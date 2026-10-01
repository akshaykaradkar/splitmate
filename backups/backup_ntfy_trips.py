#!/usr/bin/env python3
"""
SplitMate server-side backup (READ-ONLY).

Pulls everything SplitMate has cached on ntfy.sh for your trips and saves it locally:
  - splitmate_v2_idx_<phone>   -> list of your groups (trips)
  - splitmate_v2_code_<CODE>   -> join-code -> groupId pointer
  - splitmate_v2_grp_<groupId> -> the trip ledger (members, expenses, splits, settlements)
  - splitmate_v2_plan_<groupId>-> v2.3.4 plan manifest (usually empty before v2.3.4)
  - splitmate_v2_u_<phone>     -> your profile record

Only HTTP GETs are made. Nothing is ever published, so the live trips cannot be affected.

Usage:
  python3 backup_ntfy_trips.py --phone 98XXXXXXXX
  python3 backup_ntfy_trips.py --code ABC123 --code XYZ789
  python3 backup_ntfy_trips.py --group <groupId>
"""
import argparse
import base64
import datetime as dt
import gzip
import json
import os
import re
import sys
import urllib.request

NTFY = "https://ntfy.sh"
GZ = "SMGZ:"
UA = {"User-Agent": "SplitMate-Backup/1.0 (read-only)"}


def get(url, timeout=20):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read().decode("utf-8", "replace")


def sanitize(key):
    return re.sub(r"[^a-zA-Z0-9_-]", "_", key)


def decode_payload(raw):
    raw = (raw or "").strip()
    if raw.startswith(GZ):
        try:
            raw = gzip.decompress(base64.b64decode(raw[len(GZ):])).decode("utf-8")
        except Exception:
            pass
    try:
        return json.loads(raw)
    except Exception:
        return raw


def fetch_topic(topic, outdir):
    """Returns list of decoded payloads (oldest -> newest) and writes raw + decoded files."""
    try:
        body = get(f"{NTFY}/{topic}/json?poll=1&since=all")
    except Exception as e:
        print(f"  ! {topic}: fetch failed ({e})")
        return []
    events = []
    for line in body.splitlines():
        if not line.strip():
            continue
        try:
            ev = json.loads(line)
        except Exception:
            continue
        if ev.get("event") != "message":
            continue
        content = ev.get("message", "")
        att = (ev.get("attachment") or {}).get("url")
        if att:
            try:
                content = get(att)
            except Exception as e:
                print(f"  ! {topic}: attachment expired/unreachable ({e}); keeping inline text")
        events.append({
            "id": ev.get("id"),
            "time": ev.get("time"),
            "time_iso": dt.datetime.fromtimestamp(ev.get("time", 0), dt.timezone.utc).isoformat(),
            "had_attachment": bool(att),
            "payload": decode_payload(content),
        })
    events.sort(key=lambda e: e["time"] or 0)
    with open(os.path.join(outdir, f"{topic}.raw.jsonl"), "w") as f:
        f.write(body)
    with open(os.path.join(outdir, f"{topic}.decoded.json"), "w") as f:
        json.dump(events, f, indent=2, ensure_ascii=False)
    print(f"  - {topic}: {len(events)} message(s)")
    return [e["payload"] for e in events]


def find_group_ids(obj, found):
    """Collect groupId values from any decoded payload shape."""
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in ("groupId", "group_id", "gid", "g") and isinstance(v, str) and len(v) > 3:
                found.add(v)
            else:
                find_group_ids(v, found)
    elif isinstance(obj, list):
        for v in obj:
            find_group_ids(v, found)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--phone", action="append", default=[])
    ap.add_argument("--code", action="append", default=[])
    ap.add_argument("--group", action="append", default=[])
    ap.add_argument("--out", default=None)
    a = ap.parse_args()
    if not (a.phone or a.code or a.group):
        ap.error("give at least one --phone, --code or --group")

    stamp = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
    outdir = a.out or os.path.join(os.path.dirname(os.path.abspath(__file__)), f"ntfy_backup_{stamp}")
    os.makedirs(outdir, exist_ok=True)
    print(f"Backing up to {outdir}")

    group_ids = set(a.group)
    for phone in a.phone:
        p = re.sub(r"\D", "", phone)[-10:]
        print(f"Phone {p[:2]}******{p[-2:]}")
        fetch_topic(f"splitmate_v2_u_{p}", outdir)
        for payload in fetch_topic(f"splitmate_v2_idx_{p}", outdir):
            find_group_ids(payload, group_ids)
    for code in a.code:
        c = code.strip().upper()
        print(f"Join code {c}")
        for payload in fetch_topic(f"splitmate_v2_code_{c}", outdir):
            find_group_ids(payload, group_ids)

    summary = {"created": stamp, "groups": {}}
    for gid in sorted(group_ids):
        print(f"Trip {gid}")
        ledgers = fetch_topic(f"splitmate_v2_grp_{sanitize(gid)}", outdir)
        fetch_topic(f"splitmate_v2_plan_{sanitize(gid)}", outdir)
        latest = next((l for l in reversed(ledgers) if isinstance(l, dict)), None)
        summary["groups"][gid] = {
            "ledger_messages": len(ledgers),
            "latest_top_level_keys": sorted(latest.keys()) if latest else [],
        }
        if latest:
            with open(os.path.join(outdir, f"TRIP_{sanitize(gid)}_latest_ledger.json"), "w") as f:
                json.dump(latest, f, indent=2, ensure_ascii=False)

    with open(os.path.join(outdir, "SUMMARY.json"), "w") as f:
        json.dump(summary, f, indent=2)
    print(json.dumps(summary, indent=2))
    if not group_ids:
        print("No trips found on the server (ntfy.sh only caches messages for a limited time).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
