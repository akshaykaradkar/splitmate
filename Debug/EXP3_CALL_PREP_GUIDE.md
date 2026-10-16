# EXP-3 Call Prep Guide (findings session, presenting from the spreadsheet)

You'll **present the EXP-3 tab of the spreadsheet**. This guide follows that tab row by row, in the same order and with the same numbers. Behind it: the EXP-3 results document (`EXP3_RESULTS_ANALYSIS_exp3_golden_20261012_gt.html`), the audited script `06_exp3_tree_ah_sweep.py`, and the integrity audit (`docs/audits/AUDIT_REPORT_exp3_final_integrity.md`).

---

## Part 1. The picture to keep in your head

**The library picture.** 5 million products, sorted by Tree-AH into **folders (leaves)** of similar items: about 714 folders of ~7,000 products.

| Step of a search | Setting | Glance today |
|---|---|---|
| 1. Open the closest few **folders** | `fraction_leaf_nodes_to_search_override` ("leaf fraction") | **5%** of folders (~36) |
| 2. From them, keep a **shortlist** of the most promising products | `approximate_neighbor_count` ("candidates", "approx") | **500** |
| 3. Rank the shortlist carefully and return the **results** | `neighbor_count` | **500** results |

- **Both settings can be changed per search request.** No new index is needed. That's what EXP-3 does.
- **The same request, the same 2,000 golden queries, and the same BruteForce answer key** as EXP-1. Only these two settings change.

### How to read a setting name (e.g. `A5000_F0.05`, `A500_Funset`)
The names are short codes the script uses for each combination. The sheet now also shows them in plain words, in the two columns next to "Setting".

| Part of the name | Means | Example |
|---|---|---|
| **A** + number | **A**pproximate neighbour count = how many **candidates** go on the shortlist | `A5000` = 5,000 candidates |
| **F** + number | **F**raction of the index's folders (leaves) searched | `F0.05` = 5%, `F0.20` = 20% |
| **F unset** ("Funset") | The fraction override is **not set**, so the index uses its own default, **5%** | `A500_Funset` = today |
| **N** + number (depth tests only) | **N**eighbour count = how many **results** are returned | `N1000_A2000_F0.20` = 1,000 results, 2,000 candidates, 20% |

| Setting | Candidates kept | Share of index searched | In plain words |
|---|---|---|---|
| `A500_Funset` | 500 (index default, not overridden) | 5% (index default, not overridden) | **Today's production request**, nothing changed |
| `A1000_F0.02` | 1,000 | 2% | Bigger shortlist, smaller search: **faster** |
| `A1000_F0.05` | 1,000 | 5% | Double the shortlist |
| `A2000_F0.05` | 2,000 | 5% | 4× the shortlist |
| `A5000_F0.05` | 5,000 | 5% | 10× the shortlist: **best balance** |
| `A5000_F0.10` | 5,000 | 10% | 10× shortlist, 2× the search |
| `A5000_F0.20` | 5,000 | 20% | 10× shortlist, 4× the search: **best recall** |

**Why "Funset" and not "F0.05"?**
- Glance's production code **doesn't send** a leaf-fraction value, so the index uses its built-in 5%.
- To measure **exactly** what production does, the baseline sends **no override at all**. "unset" says precisely that.
- We also tested `A500_F0.05` (5% sent explicitly). It returned the **same dense results** as `A500_Funset` (200 of 200 queries identical), which confirms the index's "5%" equals a fraction of 0.05. So `Funset` and `F0.05` mean the same search; the name only records whether the value was sent.
- **If asked, say:** "'unset' means we didn't override anything: it's literally today's production request."

### EXP-3's aim (plan)
> **ScaNN Tree-AH Algorithmic Tuning, 5M:** tune parameters to benchmark P95/P99 latency and recall: **latency speed-up vs acceptable recall trade-off.**

---

## Part 2. Presenting the EXP-3 tab, section by section

### Row 1: context (italic)
*"2,000 golden queries · test index = Glance's production config on 5M products (1 shard, 2 replicas) · ground truth = BruteForce · request = Glance production; only the two per-request search settings change."*
- **Say:** "Same setup as EXP-1. We changed only two settings in the search request, nothing in the index."

### Section 1, What We Tuned (rows 2–5)
- **Candidates (approx):** 500 (today) / 1,000 / 2,000 / 5,000.
- **Leaf fraction:** 2% / 5% (today) / 10% / 20%.
- 4 × 4 = 16 combinations, plus 2 "extreme" ones (50%, 100% of folders) and today's default = **19 settings**, each on all 2,000 queries.
- **Say:** "Two knobs: how big the shortlist is, and how much of the index is searched."

### Section 2, Latency Speed-up vs. Recall Trade-off (rows 7–16, columns A–K) ⭐ the main table
| Setting | Candidates kept | Share of index searched | Role | Hybrid R@10 | Hybrid R@100 | Dense R@500 | Single request (ms) | P99 at 565 QPS (ms) | Max QPS (2 replicas) | Judged hits @500 |
|---|---|---|---|---|---|---|---|---|---|---|
| A500_Funset | 500 (default) | 5% (default) | **Today** | 0.6630 | 0.5534 | 0.6596 | 28.46 | 40.1 | ≥800 | 76.1% |
| A1000_F0.02 | 1,000 | 2% | **Faster** | 0.7191 | 0.6361 | 0.6989 | 25.95 | 34.4 | ≥800 | 77.5% |
| A1000_F0.05 | 1,000 | 5% | More candidates | 0.7334 | 0.6429 | 0.8045 | 27.78 | 36.5 | 565 | 80.1% |
| A2000_F0.05 | 2,000 | 5% | Middle | 0.7899 | 0.7276 | 0.8427 | 30.31 | 41.3 | 565 | 83.3% |
| A5000_F0.05 | 5,000 | 5% | **Best balance** | 0.8600 | 0.8289 | 0.8500 | 36.22 | 55.7 | 565 | 85.3% |
| A5000_F0.10 | 5,000 | 10% | More leaves | 0.8725 | 0.8343 | 0.9168 | 40.12 | 314.4 (fails) | 400 | 86.3% |
| A5000_F0.20 | 5,000 | 20% | Best recall | 0.8817 | 0.8377 | 0.9570 | 48.20 | fails (failed at 400 QPS) | 200 | 87.7% |

**How to read each column:**
- **Candidates kept / Share of index searched:** the two settings in plain words (the "Setting" code says the same thing in short).
- **Hybrid R@10 / R@100:** share of BruteForce's top 10 / top 100 that the setting also finds, for the production hybrid request.
- **Dense R@500:** the meaning-only search, deep down to 500. It shows what more folders buy.
- **Single request (ms):** median time of one search with no other traffic.
- **P99 at 565 QPS:** at Glance's **peak traffic**, 99% of searches were faster than this.
- **Max QPS (2 replicas):** the highest traffic it held, with P99 ≤ 100 ms (provisional limit) and < 0.1% errors. **"≥800"** = we stopped at 800 without reaching a limit.
- **Judged hits @500:** of Glance's "highly relevant" judged products in our 5M data, the share found in the top 500.

**Walk it top to bottom:**
1. **Today:** fast (P99 40 ms at peak, ≥800 QPS), but finds only 55% of the true top 100.
2. **Faster:** A1000_F0.02 is *faster* than today and *better* for hybrid, but weaker for meaning-only searches.
3. **More candidates at 5% folders** (A1000 → A2000 → A5000): hybrid R@100 climbs 0.64 → 0.73 → **0.83**, each still holding Glance's 565 QPS peak.
4. **Best balance:** A5000_F0.05 has hybrid R@100 **0.829** (vs 0.553), judged hits **85.3%** (vs 76.1%), and P99 **55.7 ms** at peak.
5. **More folders** (10%, 20%): little extra hybrid recall (+0.005 / +0.009), and they **can't hold the peak** on 2 replicas.

### Trade-off Insights (rows 18–23): what each line says
| Row | Insight | Say it like this |
|---|---|---|
| 19 | Candidates are the main lever for hybrid | "Shortlist 500 → 5,000 lifts hybrid R@100 from 0.553 to 0.829, for about 9 ms per request." |
| 20 | Leaf fraction acts only on dense, and costs capacity | "More folders help only the meaning search, and cut capacity to 200–400 QPS." |
| 21 | Faster option | "A1000_F0.02: lower P99 than today and better hybrid, but weaker meaning-only recall (0.784 vs 0.890 at R@10)." |
| 22 | Best balance | "A5000_F0.05 holds the 565 peak at P99 55.7 ms and finds 85.3% of judged-relevant products vs 76.1% today." |
| 23 | More leaves aren't worth it for hybrid | "+0.005 / +0.009 R@100 more, but they can't hold 565 QPS." |

### Section 3, Hybrid R@100 grid (rows 25–32)
| approx \ fraction | 2% | 5% | 10% | 20% |
|---|---|---|---|---|
| 500 | 0.5478 | **0.5534** (today) | 0.5564 | 0.5581 |
| 1000 | 0.6361 | 0.6429 | 0.6465 | 0.6487 |
| 2000 | 0.7197 | 0.7276 | 0.7320 | 0.7349 |
| 5000 | 0.8199 | **0.8289** | 0.8343 | 0.8377 |

- **How to read it:** move **down** (more candidates) and recall jumps (+0.28). Move **right** (more folders) and it barely moves (at most +0.02).
- **Say:** "For Glance's hybrid traffic, the shortlist size is what matters, not how much of the index we search."
- **Why:** alpha 0.3 gives the keyword (sparse) side 70% weight. The keyword side depends on the shortlist and isn't affected by folders at all: its recall was identical at every leaf fraction.

### Section 4, Recommendation (rows 34–39)
| Goal | Setting | Measured on 5M |
|---|---|---|
| Faster for hybrid traffic | approx 1000, fraction 0.02 | P99 −14% at 565 QPS, −39% at 800 QPS; hybrid R@100 +0.083; dense R@10 −0.106 |
| **Best balance at production peak** | **approx 5000, fraction 5% (unset)** | Hybrid R@100 0.553 → 0.829; judged hits 76.1% → 85.3%; P99 55.7 ms at 565 QPS; capacity 565 QPS on 2 replicas |
| Highest recall | approx 5000, fraction 0.20 | Hybrid R@100 0.838, dense R@500 0.957; capacity only 200 QPS on 2 replicas |
| Deeper result lists | neighbor_count = N, approx = 2 × N | Works per request; 200 QPS on 2 replicas at N = 1,000–2,000 with 20% leaves |

- **Say:** "If Glance wants more quality at the same cost, it's **5,000 candidates, folders left at 5%**. It's provisional until the 10M test and their latency budget."
- **Deeper lists:** for re-ranking or brand boosting, Glance can ask for 1,000–2,000 results per request. Set candidates to twice the results; asking for more results than candidates gives an error (InvalidArgument 400).

### Caveats (rows 41–45)
- **1 shard at 5M:** production has 9 shards, and candidates count **per shard**. Confirm on 10M (2 shards).
- **The P99 limit of 100 ms is provisional:** Glance hasn't given a latency budget. "≥800" = the test cap.
- **Sparse/hybrid recall is vs BruteForce,** whose keyword search isn't fully exact (EXP-2).
- **Stable results:** the 2,000 queries split into two halves rank all 19 settings the same way (gap 0.002). The winner isn't luck.

---

## Part 3. "How did you do it?" (the script in plain words)

Script `06_exp3_tree_ah_sweep.py`: read-only against Vertex, dry run by default, resumable. It was audited GO before running, and every stage records the script fingerprint.

| Step | What it does | Main output |
|---|---|---|
| **setup** | Loads the 2,000 golden queries (the same file as EXP-1, checked by sha256) and fetches **fresh BruteForce answers** for them | `queries.jsonl`, `gt/base.jsonl` |
| **check** | Confirms the deployments and live settings; small probes (e.g. approx 200 for 500 results must fail); re-asks BruteForce 50 queries; proves our load machine can push 800 QPS | `check.json` |
| **recall** | 2,000 queries × 19 settings × 3 modes (hybrid, dense-only, sparse-only) to the fast index = 114,000 calls; compared with the BruteForce answers | `cells/` |
| **timing** | One request at a time, 200 queries per setting: the single-request median | `timing.json` |
| **depth** | 1,000 / 2,000 / 5,000 results, with BruteForce at the same depth | depth tables |
| **quality** | Glance's judged products: Hit@K per setting | `quality.json` |
| **shortlist** | Picks which settings get the expensive load test, by fixed rules | `shortlist.json` |
| **latency_curve** | Load at 81 → 200 → 400 → 565 → 800 QPS, 3 minutes each, 60 s rest; stops a setting early when P99 > 100 ms or errors ≥ 1% | `latency/` |
| **report** | Everything → findings | `scorecard.md`, `summary.json` |

### Part 3A. How a "setting" becomes a request (the key code)

**1. The grid (lines 181–188):**
```python
DEFAULT_APPROX    = "500,1000,2000,5000"       # candidates
DEFAULT_FRACTIONS = "0.02,0.05,0.10,0.20"      # share of folders
DEFAULT_CEILING   = "5000:0.50,5000:1.00"      # two extreme cells
BASELINE_KEY      = "A500_Funset"              # today: no overrides
DEFAULT_CURVE_QPS = "81,200,400,565,800"       # load-test rates
```

**2. The fast-index request = the EXP-1 request + the two overrides (`tree_request`, lines 364–383).** It reuses EXP-1's `build_request` unchanged (so the default cell is byte-identical to EXP-1), then sets only the overrides the setting asks for:
```python
req = build_request(q, mode, DEPLOYED_TEST)        # exactly the EXP-1 production request
if approx is not None: pq.approximate_neighbor_count = approx            # e.g. 5000
if frac   is not None: pq.fraction_leaf_nodes_to_search_override = frac  # e.g. 0.05
depth_guard(...)   # refuses approx < neighbor_count (Vertex would reject it anyway)
```

**3. The BruteForce request never gets overrides (`bf_request` / `assert_bf_clean`, lines 386–406).** It's the plain EXP-1 request. A guard refuses to send anything with overrides to BruteForce, so the answer key always stays the same.

**4. Recall:** the same formula as EXP-1 (`|fast top-K ∩ BruteForce top-K| / K`, averaged over 2,000 queries). That's why today's row matches EXP-1 exactly (hybrid R@100 0.5534).

**5. The load test:** the same open-loop method as EXP-1/EXP-2 (fixed schedule, split over processes, client checked), plus **early stop**. When a rate fails, higher rates aren't run, which is why A5000_F0.20 shows "fails (failed at 400 QPS)".

**One line for the call:** "We kept Glance's exact production request and changed only two per-request settings: shortlist size and share of the index searched. That's 19 combinations on 2,000 queries against the same BruteForce answer key. Then we load-tested the promising ones at Glance's traffic levels up to 800 searches per second."

---

## Part 4. Likely questions and short answers

| Question | Answer |
|---|---|
| "Do we need to rebuild the index?" | **No.** Both are per-request settings in the query. |
| "Which setting should we use?" | "For hybrid traffic, **5,000 candidates with folders left at 5%** is the best balance on 5M. We'll confirm on 10M, and we need your latency budget to finalise." |
| "Why is today only 55%?" | "The shortlist (500) equals the number of results (500), so there's no spare room. And the keyword side, which gets 70% weight, needs a bigger shortlist." |
| "Why do more folders barely help?" | "Folders only change the meaning (dense) search. The keyword side's recall was identical at every leaf fraction, and it dominates hybrid at alpha 0.3." |
| "Will this need more machines?" | "At your current peak it held on the same 2 replicas in our test, with less headroom above peak (565 vs ≥800 QPS). We'll check on multi-shard before advising." |
| "What does ≥800 mean?" | "We tested up to 800 searches per second and it never hit its limit. The true maximum is higher." |
| "Is 5M representative of 50M?" | "Each production shard holds about 5.5M products, so 5M is a good per-shard model. Shard count changes how candidates add up, which is why 10M (2 shards) is next." |
| "Can we go faster than today?" | "Yes: A1000_F0.02 was faster under load (P99 34 vs 40 ms at peak) and better for hybrid, but weaker for meaning-only searches." |
| "What about 1,000–2,000 results for re-ranking?" | "It works per request: set candidates to twice the results. With 20% folders it held 200 QPS on 2 replicas." |
| "Is your answer key exact?" | "Exact for meaning (dense). For keywords even BruteForce is approximate (EXP-2), so sparse/hybrid numbers are 'compared with BruteForce'." |
| "Why fresh BruteForce answers instead of EXP-1's?" | "Hybrid ties come back in a varying order between calls, so we fetched fresh answers at the start of this run and compared everything within one run. The default row still reproduces EXP-1 exactly." |
| "How do we know the winner isn't luck?" | "Split the queries in two halves: same ranking of all 19 settings on both, and the best setting differs by only 0.002." |
| "What does the product team recommend?" | "About 10× candidates relative to results. That's exactly our A5000 finding. They also suggest smaller folders (1,000–5,000 products), which needs a rebuild and is the one thing we haven't tested yet." |
| A question you don't know | "Good question. Let me check it against the data and come back to you today." |

---

## Part 5. Consistency with the spreadsheet, and what not to claim

**The guide and the sheet use the same numbers.** The EXP-3 tab was filled directly from the EXP-3 results document, and every number in Part 2 is copied from the tab. One thing to know:
- The results document's tables write "800" as max QPS for today and A1000_F0.02; **the sheet writes "≥800"**. Use "≥800" ("didn't reach its limit"). It's the more accurate wording.

**What not to claim:**
- ❌ "Production will get 83%". Say "**on our 5M single-shard test**".
- ❌ "No extra cost". Say "held at your peak on the same 2 replicas in our test, with less headroom".
- ❌ "800 is the maximum". Say "≥800, limit not reached".
- ❌ Picking the final setting for Glance. Say "it depends on your latency budget and recall target".

---

## Part 6. Three sentences to open with
1. "EXP-3 kept Glance's production setup and changed only two per-request settings, the shortlist size and the share of the index searched, across 19 combinations on 2,000 real queries."
2. "The shortlist is the lever. Going from 500 to 5,000 candidates lifts hybrid recall@100 from 0.55 to 0.83, and judged relevant products in the top 500 from 76% to 85%. It still holds your 565 searches-per-second peak at P99 56 ms, with no new index."
3. "Searching more of the index mostly helps the meaning-only search and costs a lot of capacity. These are 5M single-shard results, and we'll confirm them on the 10M test."
