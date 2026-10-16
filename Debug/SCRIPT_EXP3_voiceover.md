# EXP-3 Script: ScaNN Tree-AH Algorithmic Tuning (5M)

*Read on your phone during the call. **Bold** = what to say. "👉" = where to point on the screen.*
*Order on screen: **EXP3-Summary** tab → **EXP-3** tab.*

---

## 0. The 20-second cheat sheet

- **Question:** EXP-1 showed the default search budget is too small. Can **per-request** settings, with no rebuild, win back the recall, and what does it cost in latency?
- **3 numbers to remember:**
  - **0.553 → 0.829**: hybrid Recall@100, with candidates going from 500 to 5,000 (leaves left at 5%).
  - **76.1% → 85.3%**: Glance's judged score-3 products found in the top 500 (BruteForce: 89.7%).
  - **40.1 → 55.7 ms**: P99 at 565 QPS. Still holds the peak on 2 replicas.
- **One-sentence story:** "The candidate count is the lever. 10× candidates recovers most of EXP-1's loss for about 15 ms of P99, with no rebuild. Searching more leaves adds almost nothing for hybrid and costs a lot of capacity."

---

## 1. Mini-glossary: the terms in this experiment

**The three Tree-AH knobs**
1. `leafNodeEmbeddingCount` = **7,000** products per leaf. Build-time, fixed. **Never changed** in any experiment.
2. **Leaf fraction** = share of leaves opened per query.
   - Index default: `leafNodesToSearchPercent` = 5 (percent).
   - Per request: `fraction_leaf_nodes_to_search_override` = 0.05 (fraction). It's the same setting, set in a different place.
3. **Candidates** = how many products survive to the final exact re-ranking.
   - Index default: `approximateNeighborsCount` = 500.
   - Per request: `approximate_neighbor_count`.

Plus `neighbor_count` = results returned (500, as in production).

**How a Tree-AH search works**
1. Score the query against the leaf centres; open the closest 5% of leaves (about 36 of about 714).
2. Score the products in those leaves with compressed (approximate) vectors; keep the top *approximate_neighbor_count* as candidates.
3. Re-score the candidates exactly; return the top *neighbor_count*.
- If candidates = results (500 = 500), there's no spare room: every candidate mis-ranked in step 2 is a lost result.

**Setting names on the sheet**
- `A` = approximate_neighbor_count; `F` = leaf fraction.
- `A5000_F0.05` = 5,000 candidates, 5% of leaves.
- `A500_Funset` = 500 candidates, fraction **not sent** = index default 5% = **exactly today's production request**.
- Check: sending `F0.05` explicitly gave the same dense results as `Funset` (200 of 200 queries), so the index's "5%" = 0.05.

**Recall@K**
```text
Recall@K = | Tree-AH top-K ∩ BruteForce top-K | / K      (mean over 2,000 queries)
```
- Same formula and same queries as EXP-1, which is why the `A500_Funset` row reproduces EXP-1 exactly (hybrid R@100 0.5534).

**Judged score-3 hits@K (the relevance check that doesn't use BruteForce)**
- Glance gave judgements (`topk_results.csv`): for each golden query, products rated 0–3 (3 = highly relevant).
- We keep products where every judgement for that (query, product) pair is 3, that exist in our 5M data, and that pass that query's filters. That gives **487 queries and 663 products**.
```text
hit@K for one query = (its score-3 products found in the top K) / (its score-3 products)
judged hits@K       = mean of that over the 487 queries   ("macro" average)
```
- Example:
  - query A has 2 score-3 products, 1 found in the top 500 → 0.5;
  - query B has 1, found → 1.0;
  - average = 0.75 = 75%.
- **85.3%** means: on average, 85.3% of a query's highly relevant products appear in the top 500.
- Caveat: judgements exist only for products production showed. That favours today's setting, so gains are, if anything, understated.

**Single-request median**
- One request at a time (no other traffic), 200 queries per setting in round robin; the median response time.

**P99 at 565 QPS**
- At Glance's peak rate, 99% of searches finished faster than this.
- Open-loop load, 180 s measured after a 20 s warm-up, on 2 replicas.

**Max sustainable QPS (capacity on 2 replicas)**
- Rates tried, in order: 81 → 200 → 400 → 565 → 800 QPS.
- The highest rate where **it and every lower rate** had P99 ≤ 100 ms and errors < 0.1%, with the load generator valid.
- "≥800" = it passed 800, the test cap, so the true limit is higher.
- If a rate fails (P99 > 100 ms or errors ≥ 1%), higher rates aren't run ("early stop").

**The 100 ms P99 limit** is our provisional threshold. Glance hasn't given a latency budget yet.

---

## 2. SCRIPT: screen 1, the EXP3-Summary tab

👉 **Rows 1–2 (plan row)**
> **"EXP-3 is ScaNN Tree-AH tuning. Aim: tune parameters to benchmark P95/P99 latency and recall, with the metric 'latency speed-up vs acceptable recall trade-off'. All results here are on 5M; 10M and 20M come next."**

👉 **Row 3 (yellow key finding)**
> **"In one line: raising approximate_neighbor_count from 500 to 5,000, per request and with no rebuild, lifts hybrid Recall@100 from 0.553 to 0.829, and your judged relevant products in the top 500 from 76% to 85%. It costs about 15 ms of P99 at peak. Searching more leaves mostly costs capacity."**

👉 **Row 6 (0.829 vs 0.553)**
> **"Same index, same 2,000 golden queries, same production request, same BruteForce answer key as EXP-1. We only added approximate_neighbor_count = 5,000 to the request. Recall@100 goes from 0.553 to 0.829, so most of EXP-1's loss is recovered by the candidate count alone."**

👉 **Row 7 (judged hits 85.3%)**
> **"Recall is agreement with BruteForce. To check real relevance, we used your judged products, the ones rated 3, highly relevant. Today 76.1% of them appear in the top 500; with 5,000 candidates 85.3%; and BruteForce, the ceiling, 89.7%. So we close most of the gap to the exact search, measured with your own judgements."**

👉 **Row 8 (P99 55.7 vs 40.1 ms)**
> **"The cost: at your peak of 565 QPS, P99 goes from 40.1 to 55.7 ms, about 15 ms for ten times the candidates. And it still holds 565 QPS on 2 replicas."**

👉 **Row 9 (more leaves: +0.005 / +0.009; 400 / 200 QPS)**
> **"The other knob, the share of leaves searched. Going from 5% to 10% or 20% adds only 0.005 to 0.009 of hybrid recall, but capacity drops from 565 to 400 and 200 QPS. For hybrid traffic it's not worth it."**

👉 **Row 10 (faster option)**
> **"There's also a faster option: 1,000 candidates with 2% of leaves. P99 is 34.4 ms instead of 40.1, which is 14% faster than today, and hybrid recall is better too, 0.636. The trade-off: dense-only recall drops, R@10 0.784 vs 0.890, so it suits hybrid traffic only."**

👉 **Rows 12–14 (Keep in mind)**
> **"Two caveats. approximate_neighbor_count applies per shard, and you have 9 shards, so we confirm on the 10M two-shard index. And the 100 ms limit is our placeholder until you give us a latency budget. Latency was measured on 2 replicas; autoscaling to 3 never triggered."**

👉 **Row 16 (link)** → click to the EXP-3 tab.

---

## 3. SCRIPT: screen 2, the EXP-3 detailed tab

👉 **Section 1: What we tuned (rows 2–5)**
> **"Two per-request settings on the same index, no rebuild. Candidates: 500, which is today, then 1,000, 2,000 and 5,000. Leaf fraction: 2%, 5%, which is today's index default, then 10% and 20%. That's 16 combinations, plus 2 extreme ones (50% and 100% of leaves) and today's request, so 19 settings, each on all 2,000 queries."**

👉 **Section 2: the trade-off table (rows 7–16)**
> **"Seven representative settings. Columns: the two settings, then hybrid R@10 and R@100, dense R@500, the single-request median, and P99 at your 565 QPS peak."**

Walk it top to bottom:
1. **A500_Funset (today):** R@100 **0.553**, single request 28.5 ms, P99 **40.1 ms**.
2. **A1000_F0.02 (faster):** R@100 0.636, P99 **34.4 ms**, faster than today.
3. **A1000_F0.05 → A2000_F0.05 → A5000_F0.05:** R@100 **0.643 → 0.728 → 0.829**. P99 36.5 → 41.3 → **55.7 ms**.
4. **A5000_F0.10:** R@100 0.834, but P99 **314 ms at 565: fails**.
5. **A5000_F0.20 (best recall):** R@100 0.838, dense R@500 0.957, but it **failed already at 400 QPS**.

> **"So the best balance is A5000_F0.05: it holds your peak with P99 under 60 ms and recovers most of the recall."**

👉 **Trade-off insights (rows 19–23)**: one line each:
- **Candidates lever:** "500 → 5,000 candidates: 0.553 → 0.829, for about 8–9 ms more per single request." (28.5 → 36.2 ms from today's request.)
- **Leaf fraction:** "It acts only on dense; sparse recall was identical at every fraction. It's the main latency cost: 10–20% cuts capacity to 200–400 QPS."
- **Faster option:** "A1000_F0.02 has lower P99 than today and better hybrid recall, but weaker dense-only recall."
- **Best balance:** "A5000_F0.05 holds 565 QPS at P99 55.7 ms; judged hits 85.3% vs 76.1% today, BruteForce 89.7%."
- **Higher leaves:** "+0.005 / +0.009 R@100 more, but they can't hold 565 QPS on 2 replicas."

👉 **Section 3: the R@100 grid (rows 25–32)**
> **"Rows are candidates, columns are leaf fraction. Moving down, more candidates, adds +0.28 at 5% leaves. Moving right, more leaves, adds at most +0.02. For hybrid, the candidate count is what matters."**

**Why:** sparse gets 70% of the weight, and the sparse leg depends only on the candidate count (its recall was identical at every leaf fraction).

👉 **Caveats (rows 35–36)**
> **"1 shard at 5M; UDP has 9 shards, and approximate_neighbor_count applies per shard. We confirm on the 10M index before giving production values."**

**Transition:**
> **"So the budget is the lever for quality. Next, EXP-6: can we make each search cheaper by shrinking the vectors?"**

---

## 4. "How exactly did you test it?" (code to quote)

Script: `06_exp3_tree_ah_sweep.py` (audited GO before running).
- Read-only against Vertex; dry run by default; resumable.
- Stages: `setup → check → recall → timing → depth → quality → shortlist → latency_curve → report`.

### 4.1 The grid (lines 181–188)
```python
DEFAULT_APPROX    = "500,1000,2000,5000"     # candidates
DEFAULT_FRACTIONS = "0.02,0.05,0.10,0.20"    # leaf fraction
DEFAULT_CEILING   = "5000:0.50,5000:1.00"    # two extreme cells
BASELINE_KEY      = "A500_Funset"            # today: no overrides
DEFAULT_CURVE_QPS = "81,200,400,565,800"     # load-test rates
```

### 4.2 Tree-AH request = EXP-1 production request + overrides (`tree_request`, lines 364–383)
```python
req = build_request(q, mode, DEPLOYED_TEST)   # byte-identical to EXP-1's request
pq  = req.queries[0]
if approx is not None: pq.approximate_neighbor_count = approx            # e.g. 5000
if frac   is not None: pq.fraction_leaf_nodes_to_search_override = frac  # e.g. 0.05
depth_guard(pq.neighbor_count, approx)   # refuses approx < neighbor_count
```
- Modes, the same switches as EXP-1:
  - `hybrid_prod` = dense + sparse + RRF α 0.3 + filters;
  - `dense_filtered` = dense only;
  - `sparse_filtered` = sparse only.
- Volume: 2,000 queries × 19 settings × 3 modes = **114,000 calls** to Tree-AH.

### 4.3 BruteForce never gets overrides (`bf_request` / `assert_bf_clean`, lines 386–406)
```python
req = build_request(q, mode, DEPLOYED_GT)     # plain EXP-1 request
if pq.approximate_neighbor_count or pq.fraction_leaf_nodes_to_search_override:
    raise GuardError("override on a BruteForce request: refused")
```
- **Say:** "The answer key is the same for every setting, so all 19 settings are compared fairly."

### 4.4 Recall (`recall_one`, lines 1022–1032)
```python
for k in (5, 10, 20, 50, 100, 250, 500):
    gk = gt_ids[:k]
    recall[k] = len(set(test_ids[:k]) & set(gk)) / len(gk)
# mean + standard error over queries; paired differences vs today on the same queries
```

### 4.5 Single-request timing (`stage_timing`)
```python
for q in first_200_queries:           # 1 worker, no parallel load
    for setting in settings:          # round robin, so no setting gets a "warm" advantage
        _, ms = call(client, tree_request(q, "hybrid_prod", setting))
median_ms = pct(ms, 50)               # 200 values per setting
```

### 4.6 Load curves (`open_loop_cell` + `_latency_worker`, lines 1092–1232)
```python
for rate in (81, 200, 400, 565, 800):
    nprocs = ceil(rate / 100)             # 1..7 client processes (max vCPUs - 1)
    # every process: request i is sent at t0 + i/rate, never waiting for earlier ones
    #   20 s warm-up (dropped) + 180 s measured; 10 s timeout per request
    p99 = pct([ms for ok requests after warm-up], 99)
    if p99 > 100 or error_rate >= 0.01:
        stop this setting's curve (early stop)
    sleep(60)                             # rest between rates
```
- Requests per cell at 565 QPS: 565 × 180 = **101,700** measured.

### 4.7 P99 formula (`pct`, line 238)
```python
s = sorted(ok_ms); k = (len(s) - 1) * 0.99
p99 = s[int(k)] + (s[int(k) + 1] - s[int(k)]) * (k - int(k))   # linear interpolation
```

### 4.8 Capacity rule and client validity
```python
# max sustainable QPS = highest rate where it AND every lower rate have
#   P99 <= 100 ms, errors < 0.1 %, and the load generator was valid
client_valid = (each process < 0.8 core and all processes < 75% of 8 vCPUs
                and P99 send lag < 50 ms and in-flight < 512 threads)
```
- Before the curves, `check` proved the Workbench can push 800 QPS.
- Replica count was read from Cloud Monitoring (`matching_engine/current_replicas`) before and after each cell: always 2.

### 4.9 Judged hits (`stage_quality`, lines 1906–2020)
```python
eligible[q] = {pid for (q, pid) if every judgement == 3
               and pid is in the index and passes q's filters}   # 487 queries, 663 products
for q, good in eligible.items():
    hit[K][q] = len(good & set(top_ids[q][:K])) / len(good)
macro_hit[K] = mean(hit[K].values())          # e.g. 0.853 at K=500
```
- Presence and filter tokens of each judged product come from read-only `ReadIndexDatapoints` calls.

---

## 5. Likely technical questions and answers

**Q: Do we need to rebuild the index?**
- "No. Both are per-request fields in FindNeighbors: approximate_neighbor_count and fraction_leaf_nodes_to_search_override."

**Q: Why does approx help hybrid so much more than leaves?**
- "Sparse has 70% weight, and the sparse leg's recall depends on the candidate count only; it was identical at every leaf fraction. Leaves only change which dense candidates are found."

**Q: Why is today only 0.553?**
- "Candidates equal results, 500 = 500, so there's no spare room. And the sparse side, which dominates at alpha 0.3, needs a bigger candidate pool."

**Q: Is the latency cost 15 ms or 9 ms?**
- "Single request, no load: 28.5 → 36.2 ms, about 8 ms. P99 at your 565 peak: 40.1 → 55.7 ms, about 15 ms."

**Q: What does approximate_neighbor_count do on 9 shards?**
- "It applies per shard, then results are merged. So on 9 shards the effective pool is bigger. That's exactly why we confirm on the 10M (2-shard) index before quoting production numbers."

**Q: Would more replicas fix A5000_F0.20?**
- "Probably, since capacity scales with replicas, but that's not measured. At 2 replicas it holds only 200 QPS."

**Q: Did autoscaling kick in during overload?**
- "No. Min 2 / max 3; replicas stayed at 2 in every reading. Overloaded cells showed P99 well above 100 ms and errors instead of scaling up."

**Q: Is the winner just noise?**
- "Split the 2,000 queries into two halves: both halves rank all 19 settings the same way, and the best-setting gap is 0.002. Standard errors are about 0.005."

**Q: Why fresh BruteForce answers instead of EXP-1's?**
- "Hybrid ties can come back in a different order between calls, so we fetched fresh answers at the start of this run and compared everything within one run. Today's row still reproduces EXP-1 exactly."

**Q: Can we ask for 1,000–2,000 results for re-ranking?**
- "Yes, per request. Set approximate_neighbor_count to about 2× neighbor_count. Asking for fewer candidates than results gives InvalidArgument. With 20% leaves it held 200 QPS on 2 replicas."

**Q: What about a smaller leaf size (leafNodeEmbeddingCount)?**
- "Not tested. It needs a rebuild. The product team suggests 1,000–5,000 per leaf, so it's a candidate follow-up."

**Q: Why the 100 ms limit?**
- "A placeholder. Give us your P99 budget and we'll re-read the curves against it."

**If you don't know:** "Good question. Let me check it in the raw data and get back to you today."

---

## 6. Don't say

- ❌ "Production will get 0.83": say "**on our 5M single-shard test**".
- ❌ "No extra cost": say "held your peak on the same 2 replicas in our test, with less headroom (565 vs ≥800 QPS)".
- ❌ "800 is the maximum": say "≥800, limit not reached".
- ❌ Choosing the final setting for Glance: "it depends on your latency budget and recall target".
