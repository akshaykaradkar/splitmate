# EXP-1 Script: Production Baseline (5M)

*Read on your phone during the call. **Bold** = what to say. "👉" = where to point on the screen.*
*Order on screen: **EXP1-Summary** tab → **EXP-1** tab.*

---

## 0. The 20-second cheat sheet

- **Question:** how close does Glance's current production search get to an exact search?
- **3 numbers to remember:**
  - **0.553**: hybrid Recall@100 today (about 55 of the exact top 100 come back).
  - **0.856 vs 0.544**: dense-only vs sparse-only. Hybrid follows the weaker sparse side.
  - **1.000**: with maximum search effort, the same index matches BruteForce exactly, so the data is fine and the default search budget is too small.
- **One-sentence story:** "Today's search is fast, but it returns only about half of the exact top 100. The cause is the small default search budget and the 70% sparse weight, not the data or the filters."

---

## 1. Mini-glossary: terms you may need to explain

**Dense vector**
- 1,024 numbers from Glance's CLIP model; captures the *meaning* of a product or query.
- Score between query and product = dot product (vectors are unit length, so this equals cosine similarity).

**Sparse vector**
- SPLADE keyword weights: about 100 non-zero entries out of a 30,522-word vocabulary.
- Score = sum over shared words of (query weight × product weight).
- Example: query "everyday earbuds for commutes and workouts" has 82 entries (ear 1.89, everyday 1.73, workout 1.58…).

**Hybrid search and RRF**
- Vertex runs the dense search and the sparse search, then merges the two ranked lists with Reciprocal Rank Fusion (RRF):
```text
score = α / (60 + dense_rank) + (1 − α) / (60 + sparse_rank)
```
- A product missing from one list gets 0 from that side. **α = 0.3 today** → dense 30%, sparse 70%.
- Worked example at α 0.3:
  - #1 in dense only → 0.3 / 61 = **0.00492**
  - #82 in sparse only → 0.7 / 142 = **0.00493** (beats dense #1)
  - So a dense-only #1 lands below about 82 sparse-only products.
- We verified this formula offline: it reproduces Vertex's hybrid lists exactly for 40 of 40 test queries (EXP-1 audit).

**Tree-AH (ScaNN): the fast index**
- Splits the products into **leaves** (clusters).
  - `leafNodeEmbeddingCount` = 7,000 products per leaf (build-time; never changed). At 5M that's about 714 leaves.
  - `leafNodesToSearchPercent` = 5 → open the closest 5% of leaves (about 36).
  - `approximateNeighborsCount` = 500 → keep 500 candidates from those leaves, then re-rank them and return `neighbor_count` = 500 results.
- Per-request versions (overrides): `fraction_leaf_nodes_to_search_override` (0.05 = 5%) and `approximate_neighbor_count`. Glance's production code sends **neither**.

**BruteForce**
- Compares the query with every product: no shortcut. It's our **answer key** (ground truth).
- Dense side: **exact** (EXP-2 checked against all 5M records, score error 0.0).
- Sparse side: **not fully exact** (it finds 91.6% / 85.7% / 74.8% of the true top 10 / 100 / 500). So sparse and hybrid recall mean "agreement with BruteForce".

**Recall@K**
```text
Recall@K = | fast top-K ∩ BruteForce top-K | / K      (averaged over 2,000 queries)
```
- Example: BruteForce's top 100 for "white sneakers women"; the fast index returns 55 of those 100 in its own top 100 → R@100 = 0.55.
- Order inside the top K doesn't matter; only membership does.

**Standard error (SE)**
- SE = standard deviation of the per-query recall ÷ √2000. Here SE ≈ 0.004–0.007.
- So 0.553 really means 0.553 ± about 0.01 (95% range).

**Golden queries**
- 2,000 real Glance text searches, with Glance's own CLIP text vectors and SPLADE vectors.
- All 2,000 have both vectors (sparse: 19–146 words, median 53).

**Filters (always on)**
- `instock` = true, `status` = active, `catalog_id` NOT IN 4 test catalogs, `retailer_domain` NOT IN 3 retailers, `country` = query country (both upper and lower case).
- Copied from Glance's `VertexMatchingEngineProvider.java`.

**QPS**: queries per second. **81** = UDP mean, **565** = UDP peak (Glance's UDP profile, Match calls, 24 h).

**P50 / P95 / P99**
- Sort all measured response times; P99 = the time below which 99% of searches finished.
- Example: of 101,700 searches at 565 QPS, P99 50.2 ms → about 1,017 searches were slower than 50.2 ms.
- Computed with linear interpolation between the two nearest sorted values (code in section 4).

**Open-loop load test**
- Request *i* is sent at `t0 + i / rate`, whether or not earlier ones have finished, just like real users.
- A closed loop (wait, then send) would hide queueing and make latency look better than it is.

**Shard / replica**
- **Shard** = a slice of the index; every query visits every shard. 5M fits in 1 LARGE shard; UDP has 9 shards for 49.9M.
- **Replica** = a copy of a shard on its own machine (e2-highmem-16). We ran min 2 / max 3, like UDP; it stayed at 2.

---

## 2. SCRIPT: screen 1, the EXP1-Summary tab

👉 **Rows 1–2 (blue header and plan row)**
> **"EXP-1 is the production baseline. The aim, from our plan: establish the as-is baseline against exact Brute-Force ground truth, measured with Recall@K. Our test index uses your UDP production configuration: LARGE shard, leaf 7000, 5% of leaves searched, 500 candidates, on 5 million of your products."**

👉 **Row 3 (yellow key finding)**
> **"The short answer: today's search is fast, but it returns only about half of the exact top 100. And the cause is the small default search budget, not the data."**

👉 **Row 6 (Hybrid Recall@100 = 0.553)**
> **"We sent your 2,000 golden queries with your exact production request: hybrid, alpha 0.3, 500 results, your filters. We sent each one to the production-config index and to BruteForce. Of BruteForce's top 100, the production setup returns about 55. That's Recall@100 of 0.553."**

👉 **Row 7 (Dense 0.856 · Sparse 0.544, hybrid 0.553)**
> **"To see which side loses products, we also sent each query dense-only and sparse-only. Dense alone finds 86 of the top 100, sparse alone 54. Hybrid lands at 55, right next to the weaker sparse side, not in between. That's because alpha 0.3 gives sparse 70% of the weight."**

👉 **Row 8 (maximum search effort: Dense 1.000 · Hybrid 0.853)**
> **"Then the key test. On 40 queries we asked the same index to search 100% of the leaves and keep 5,000 candidates. Dense recall became exactly 1.000, a perfect match with BruteForce. So the data, the index and our test code are correct. The loss comes only from the default search budget."**

👉 **Rows 10–12 (Keep in mind)**
> **"Two caveats: this is 1 shard at 5M while UDP runs 9 shards, so we confirm on 10M. And sparse and hybrid recall are measured against BruteForce, which is exact for dense but not fully exact for sparse. We checked that in EXP-2."**

👉 **Row 14 (link)**: *"Let me show you how we measured it."* → click to the EXP-1 tab.

---

## 3. SCRIPT: screen 2, the EXP-1 detailed tab

👉 **Row 1 (context line)**
> **"Setup: 2,000 golden queries, test index with your production config on 5M products, ground truth BruteForce, and your production request."**

👉 **Section 1: Recall vs BruteForce by request mode (rows 5–9)**
> **"Four versions of the same request:**
> - **hybrid_prod: exactly what production sends;**
> - **dense_filtered: dense vector only, with filters;**
> - **sparse_filtered: sparse vector only, with filters;**
> - **hybrid_nofilter: production, but with the filters switched off."**

Point at the numbers:
- **hybrid_prod:** R@50 **0.591**, R@100 **0.553**, R@500 **0.518**.
- **dense_filtered:** 0.872 → 0.856 → 0.660. Strong at the top, weaker deep.
- **sparse_filtered:** 0.601 → 0.544 → 0.375. Weak throughout.
- **hybrid_nofilter:** 0.528 at R@100, about the same as with filters.

> **"Two things to notice. Dense drops at 500 because the index keeps 500 candidates for 500 results, so there's no spare room. And removing the filters doesn't help, so the filters aren't the cause."**

👉 **Recall insights (rows 12–15)**: read the four labels and their one-line text:
- **Dense leg** → no spare room at depth.
- **Sparse leg** → weak throughout.
- **Hybrid mode** → sits next to sparse.
- **Filter impact** → filters change little.

👉 **Section 2: Root-cause diagnostics (rows 17–25)**
> **"To find the cause, we took 40 queries (a fixed random sample, seed 7) and sent the same request with three search budgets: the default (5% of leaves, 500 candidates), Override A (20%, 2,000) and Override B (100%, 5,000). That's 480 calls, 0 errors."**
- Dense: **0.834 → 0.964 → 1.000** at R@100.
- Sparse: **0.555 → 0.735 → 0.846**.
- Hybrid: **0.561 → 0.745 → 0.853**.
> **"Row 25: the default column, 0.561, matches the full 2,000-query run, 0.553, so the 40-query sample is representative."**

**What "override" means (if asked):** "A value sent inside one search request that replaces the index default for that request only. Nothing is rebuilt."

👉 **Root causes (rows 27–31)**
> - **"Search budget: with Override B dense reaches 1.000, so data and setup are correct."**
> - **"Cause 1: the default searches only 5% of leaves and keeps 500 candidates for 500 results, which is too small for text queries."**
> - **"Cause 2: the sparse leg improves with more candidates, 0.555 → 0.735 → 0.846, but stays below dense."**
> - **"Cause 3: alpha 0.3 gives sparse 70% of the weight, so hybrid follows the weaker sparse side. That's what EXP-2 tests."**

**Transition to the next experiment:**
> **"So there are two levers: the mix (alpha, EXP-2) and the search budget (EXP-3). Let's look at the budget."**

---

## 4. "How exactly did you test it?" (code to quote)

Script: `04_exp1_baseline_eval.py`.
- It only **reads** from Vertex: FindNeighbors and describe calls, never any change.
- Stages: `check → sample → recall → latency → report`.
- Dry run by default; resumable; every stage records the script sha256.

### 4.1 The 4 modes = 4 switches (lines 71–76)
```python
MODES = {
  "hybrid_prod":     dict(dense=True,  sparse=True,  rrf=True,  filters=True),
  "dense_filtered":  dict(dense=True,  sparse=False, rrf=False, filters=True),
  "sparse_filtered": dict(dense=False, sparse=True,  rrf=False, filters=True),
  "hybrid_nofilter": dict(dense=True,  sparse=True,  rrf=True,  filters=False),
}
```

### 4.2 One function builds every request (`build_request`, lines 161–181)
```python
dp = IndexDatapoint()
if m["dense"]:  dp.feature_vector = q["dense"]                 # 1024 floats
if m["sparse"] and q["sparse_dims"]:
    dp.sparse_embedding = IndexDatapoint.SparseEmbedding(
        dimensions=q["sparse_dims"], values=q["sparse_vals"])
if m["filters"]:
    dp.restricts = [instock IN [true], status IN [active],
                    catalog_id NOT IN [4 ids], retailer_domain NOT IN [3 domains],
                    country IN [UPPER, lower]]
query = FindNeighborsRequest.Query(datapoint=dp, neighbor_count=500)
if m["rrf"]: query.rrf = FindNeighborsRequest.Query.RRF(alpha=0.3)
# no approximate_neighbor_count, no leaf override -> same as Glance's Java code
```
- **Say:** "The **same request object** goes to the Tree-AH deployment and to the BruteForce deployment; only the deployed index id differs."
- Dense-only = sparse switched off; sparse-only = dense switched off; RRF only applies when both are present.

### 4.3 The recall loop (`stage_recall`)
```python
for q in queries:                      # 2,000
  for mode in MODES:                   # x 4  = 8,000 requests
    for deployed in (TREE_AH, BRUTEFORCE):   # x 2 = 16,000 calls
        ids, ms = call(client, build_request(q, mode, deployed))
    write {qid, mode, test_ids, gt_ids} to raw_recall.jsonl
```
- 8 calls in parallel, up to 3 retries, every error recorded. Result: 0 errors, 0 missing; BruteForce returned ≥500 for every query.

### 4.4 The recall formula (same in EXP-3 / EXP-6: `recall_one`)
```python
for K in (5, 10, 20, 50, 100, 250, 500):
    gk = gt_ids[:K]
    recall[K] = len(set(test_ids[:K]) & set(gk)) / len(gk)
# then mean and standard error over the 2,000 queries, per mode
```

### 4.5 The load test (`_latency_worker`, open loop)
```python
n_total = int(rate * (20 + 180))          # 20 s warm-up + 180 s measured
for i in range(pidx, n_total, nprocs):    # each process sends every nprocs-th request
    sched = t_start + i / rate            # fixed schedule, like real users
    sleep_until(sched)
    pool.submit(fire, i, sched)           # don't wait for earlier responses
# fire(): t0 = perf_counter(); client.find_neighbors(req, timeout=10 s);
#         record service_ms = (end - t0)*1000, send_lag = t0 - sched, code
```
- One client process per 100 QPS: 1 process at 81 QPS, 6 at 565. All processes share one start time.
- Request = **hybrid_prod** (the production request), cycling through the 2,000 golden queries.
- Measured requests: 81 × 180 = **14,580**; 565 × 180 = **101,700**. 0 errors.

### 4.6 How P99 is computed (`pct`)
```python
meas = [r for r in rows if r.sched_offset_s >= 20]      # drop warm-up
ok   = [r.service_ms for r in meas if r.code == "OK"]
def pct(vals, p):                       # linear interpolation
    s = sorted(vals); k = (len(s) - 1) * p / 100
    f, c = int(k), min(int(k) + 1, len(s) - 1)
    return s[f] + (s[c] - s[f]) * (k - f)
p99 = pct(ok, 99)
```

### 4.7 "Client valid": our load generator wasn't the bottleneck
- Each client process used < 0.8 core, and all of them together < 75% of the Workbench's 8 vCPUs.
- Requests left on time (send lag small).
- In-flight requests stayed below the thread pool size (512 per process).
- If any check fails, the number is flagged, not reported as server latency.

### 4.8 The 40-query diagnostic (Override A / B)
```python
req = build_request(q, mode, TREE_AH)                     # production request
req.queries[0].fraction_leaf_nodes_to_search_override = 0.20   # A: 20%  (B: 1.0)
req.queries[0].approximate_neighbor_count = 2000               # A: 2,000 (B: 5,000)
# BruteForce gets the plain request; every returned id re-scored with an exact dot product
```

---

## 5. Likely technical questions and answers

**Q: Same data in both indexes, so why isn't recall 1.0?**
- "Same data, different search method. Tree-AH opens 5% of leaves and keeps 500 candidates; BruteForce checks everything. With 100% of leaves and 5,000 candidates the same index matched BruteForce exactly on dense (1.000)."

**Q: Is BruteForce really exact?**
- "Dense yes: checked offline against all 5M records, score error 0. Sparse no: it misses some true matches (0.748 at top 500). So sparse and hybrid recall are 'agreement with BruteForce', and we also use your judged products, which don't depend on BruteForce."

**Q: How do you know your request equals production?**
- "Built from your `VertexMatchingEngineProvider.java` and `hybrid-search-sample.json`: alpha 0.3, neighbor_count 500, the same always-on filters plus country, no overrides."
- Known gaps:
  - the conditional gender and category filters aren't applied (both indexes alike);
  - `per_crowding_attribute_neighbor_count` isn't set (your value is unknown; we asked).

**Q: Why golden queries and not product vectors?**
- "Products are easy queries, because they already sit in the index (dense recall 0.977 in an early run). Your text queries are harder and they're what users send, so they give the honest baseline."

**Q: Why does dense recall drop from 0.856 at 100 to 0.660 at 500?**
- "approximate_neighbor_count (500) equals neighbor_count (500). Every candidate that's slightly mis-ranked by the approximate scoring falls off the end, so the deep part of the list suffers most."

**Q: How does Vertex fuse dense and sparse?**
- "RRF with constant 60: alpha over (60 + dense rank) plus 1 − alpha over (60 + sparse rank), over each side's top-500. We reproduced Vertex's hybrid output offline exactly for 40 of 40 queries."

**Q: Why does hybrid sit next to sparse?**
- "At alpha 0.3 a dense-only #1 scores 0.3/61 = 0.0049. Every sparse-only product down to rank 82 scores more (0.7/142 = 0.0049+). Only 3.7% of BruteForce's hybrid top 100 are dense-only products."

**Q: 1 shard vs your 9 shards: does it hold?**
- "One UDP shard holds about 5.5M products, so our 5M single shard is a good per-shard model. But candidates are collected per shard and then merged, so we confirm on the 10M (2-shard) index."

**Q: Are the latency numbers what production sees?**
- "They're measured from a Workbench in us-central1 over the public endpoint, on 1 shard with 2 replicas, with our load generator checked. Treat them as relative: there's headroom. Your network path and 9-shard fan-out can differ."

**Q: Why 81 and 565 QPS?**
- "Your UDP profile: Match calls mean 81.4, peak 564.8 per second over 24 hours."

**Q: Did autoscaling kick in?**
- "Min 2 / max 3 like UDP. It never added the third replica during our 3-minute tests, so the numbers are on 2 replicas, the conservative case."

**Q: What's the standard error?**
- "About 0.004–0.007 on recall over 2,000 queries, so differences above about 0.01 are real."

**Q: Is the code correct?**
- "Three checks:
  - With full effort the index reproduces BruteForce dense exactly, which would be impossible with wrong code or data.
  - Returned scores equal exact dot products.
  - An independent audit recomputed the diagnostic numbers."

**Q: Why didn't you test the v3 config?**
- "The plan says 'v3', but UDP replaced v3 as the production profile (Glance's 8 Oct profile), so we copied UDP's settings: LARGE, leaf 7000, 5%, approx 500."

**If you don't know:** "Good question. Let me check it against the raw data and come back to you today."

---

## 6. Don't say

- ❌ "Production recall is 55%": say "**on our 5M single-shard test**".
- ❌ "BruteForce is perfect": it's exact for **dense** only.
- ❌ "Filters cause the loss": they don't (0.528 without them).
- ❌ Absolute production latency: our numbers are relative.
