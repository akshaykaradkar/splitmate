# EXP-2 Call Prep Guide (findings session)

Companion to Anita's EXP-2 results document (`exp2_golden_20261012`) and the EXP-2 tab. Plain-language explanations, how the script works, likely questions with answers, and what not to claim. Numbers come from the EXP-2 results document. Wherever a number is shared with EXP-1 or EXP-3, it has been cross-checked and agrees.

---

## Part 1. The picture to keep in your head

- **Same setup as EXP-1:** 5M Glance products; the **Tree-AH LARGE** index (Glance's production settings) is the test; **BruteForce** is the answer key; the same 2,000 golden queries and the same filters.
- **What's new in EXP-2:** the **RRF alpha** setting.
  - A hybrid search runs **two searches**: meaning (dense) and keywords (sparse). It then **merges** the two lists with RRF.
  - **Alpha = the weight of the meaning list** in that merge. Production uses **alpha 0.3**: 30% meaning, 70% keywords.

| Alpha | Meaning (dense) weight | Keyword (sparse) weight | In plain words |
|---|---|---|---|
| 0.0 | 0% | 100% | keywords only |
| 0.1 | 10% | 90% | |
| **0.3 (today)** | **30%** | **70%** | keyword-heavy |
| 0.5 | 50% | 50% | balanced |
| 0.7 | 70% | 30% | meaning-heavy |
| 0.9 | 90% | 10% | |
| 1.0 | 100% | 0% | meaning only |

- **The RRF formula** (same as in EXP-1): each product's score = `alpha / (60 + dense rank) + (1 − alpha) / (60 + sparse rank)`. The top scores make the final list.

### EXP-2's aim (plan)
> **Hybrid Search & RRF Tuning, 5M:** evaluate Dense, Sparse and Hybrid latency, and test RRF alpha trade-offs. **Metric:** latency and recall balance.

**EXP-2 asks four questions:**
1. **A.** How fast are dense-only, sparse-only and hybrid searches?
2. **B.** How do results change across alpha 0.0 → 1.0?
3. **C.** Is the BruteForce answer key really exact?
4. **D.** Which alpha brings more of Glance's **judged relevant** products into the results?

---

## Part 2. The Executive Summary, line by line

**1. Run validity (clean)**
- **36,000 search calls** = 2,000 queries × 9 request types (dense, sparse, and hybrid at 7 alphas) × 2 indexes, with 0 errors.
- Latency: 5 request types × 2 traffic rates, all with "client valid" (our machine kept up).
- ⚠️ **The results document says "~1.16M latency calls".** By my count it's about **0.58M measured** (5 × (14,580 + 101,700)), or about 0.65M including warm-up. Don't quote 1.16M; say "over half a million latency requests, 0 errors".

**2. Latency: the cost of real hybrid**

| Request (565 QPS) | P50 | P95 | P99 |
|---|---|---|---|
| dense only | 17.6 | 21.9 | 24.7 |
| sparse only | 15.6 | 20.7 | 24.6 |
| hybrid alpha 0.0 | 14.7 | 19.4 | 23.3 |
| hybrid alpha 1.0 | 16.4 | 19.8 | 22.5 |
| **hybrid alpha 0.3 (today)** | **23.2** | **31.1** | **36.9** |

- **Real hybrid (alpha 0.3) costs about 6–8 ms more** than a single search, because Vertex runs **both** searches and merges them.
- **Alpha 0.0 and 1.0 cost the same as a single search.**
  - Our reading: when one side has zero weight, Vertex skips it. *(Inferred from the latency and the identical results; not documented.)*
- **Traffic barely matters:** 81 → 565 QPS (7×) changes P99 only 34.5 → 36.9 ms.
- ⚠️ **Only 5 request types were load-tested:** dense, sparse, alpha 0.0, 0.3 and 1.0. **Alpha 0.5 and 0.7 were not load-tested.** They should cost about the same as 0.3 (both sides run), but that is **expected, not measured**.

**3. Recall vs BruteForce across alpha**

| Alpha | R@10 | R@100 | R@500 |
|---|---|---|---|
| 0.0 (sparse) | 0.700 | 0.544 | 0.375 |
| 0.1 | 0.694 | 0.543 | 0.373 |
| **0.3 (today)** | **0.663** | **0.553** | **0.518** |
| 0.5 | 0.664 | **0.714** | 0.629 |
| 0.7 | 0.818 | **0.822** | **0.684** |
| 0.9 | 0.877 | 0.852 | 0.661 |
| 1.0 (dense) | 0.890 | 0.856 | 0.660 |

- **The keyword search is the weak side** (R@100 0.544); meaning is strong (0.856).
- **More meaning weight means higher recall:** R@100 0.553 at 0.3 → 0.714 at 0.5 → 0.822 at 0.7.
- ⚠️ **Important nuance (expect this question):** each alpha's recall is measured against **BruteForce at the same alpha**. So "higher recall" means the fast index agrees better with the answer key. That happens because the meaning side is searched more accurately. **It does not by itself mean better products for users.** For that, look at the judged results (line 4).

**4. Search quality: judged relevant products (the real "is it better?" answer)**
- Glance's judges rated products 0–3 per query. We use the **score-3 ("highly relevant") products** that exist in our 5M data: **487 queries, 663 products**.
- **Hit@K** = share of those products found in the top K.

| Alpha | Hit@10 | Hit@50 | Hit@100 | Hit@500 |
|---|---|---|---|---|
| 0.0 (sparse) | 26.4% | 37.1% | 40.0% | 43.2% |
| **0.3 (today)** | **30.2%** | **39.5%** | **55.6%** | **76.1%** |
| 0.5 | 39.5% | 63.2% | **69.3%** | 79.2% |
| 0.7 | 37.2% | 54.5% | 66.6% | **80.3%** |
| 1.0 (dense) | 31.6% | 49.3% | 55.9% | 66.8% |

- **Alpha 0.5:** Hit@100 **+13.7 points** (55.6% → 69.3%, paired ± 1.6). Hit@50 +23.6 points.
- **Alpha 0.7:** the best Hit@500, **80.3%** (+4.2 points ± 0.9).
- **Hybrid beats both pure modes:** sparse 43%, dense 67%, hybrid 76–80% at Hit@500. Mixing helps.
- **It holds on BruteForce too:** with the exact answer key, alpha 0.5 gives Hit@100 **81.6% vs 68.2%** at 0.3. So the alpha gain is a **real relevance gain**, not an artefact of the fast index.
- **"Points" not "%":** +13.7 means percentage points (55.6 → 69.3).

**5. Is BruteForce exact? (critical for EXP-1, EXP-2 and EXP-3)**
- We checked 50 queries **offline against all 5,000,000 records**: we computed every product's exact score ourselves and compared with what BruteForce returned.

| Side | Verdict | Tie-aware @10 / @100 / @500 |
|---|---|---|
| Dense (meaning) | **EXACT** | 1.000 / 1.000 / 1.000, score error 0.0 |
| Sparse (keywords) | **NOT EXACT** | 0.916 / 0.857 / 0.748; in 5 of the 50 queries BruteForce returned fewer than 500 results (as few as 199), though 500+ valid products exist |

- **What it means:** "dense recall vs BruteForce" is recall vs perfect. "Sparse/hybrid recall vs BruteForce" is agreement with Vertex's BruteForce, not with perfect. This is a finding worth reporting to the Vertex team.

**6. Consistency checks**
- Alpha 1.0 returned **exactly the same lists** as dense-only, and alpha 0.0 **exactly the same** as sparse-only, on both indexes.
- This proves alpha behaves as documented (1 = meaning only, 0 = keywords only).

**7. List changes vs today (overlap with alpha 0.3)**
- Today's alpha 0.3 is **almost pure keyword search** at the top: at top-50 it shares **95.6%** of its results with keywords-only, and only **6.2%** with meaning-only.
- **Alpha 0.5** keeps **76.3%** of today's top-500 pool, but only **55%** of today's top-10, so the top results change a lot.
- **Alpha 0.7** keeps only **13.4%** of today's top-10.
- **Implication:** moving alpha visibly changes what users see. Validate it with Glance's relevance testing (online A/B or judge) before production.

---

## Part 3. "How did you do it?" (the script in plain words)

Script `05_exp2_hybrid_rrf_eval.py`: read-only against Vertex (searches, describe calls, datapoint reads) and GCS (reads). Dry run by default, resumable, and it records its own fingerprint per stage.

| Step | What it does | Output |
|---|---|---|
| **check** | Confirms both indexes are deployed, records their live settings, sends 1 test query to each | `index_config_live.json` |
| **queries** | Loads the same 2,000 golden queries as EXP-1 | `queries.jsonl` |
| **sweep** | Every query × **9 request types** × both indexes (36,000 calls); saves both result lists | `raw_sweep.jsonl` |
| **latency** | Open-loop load at 81 and 565 QPS for **5 request types** on the fast index | `latency_<type>_<qps>qps.csv` |
| **exact** | Offline: reads all 100 data files (5M records), computes exact dense and sparse scores for 50 queries with the same filters, compares with BruteForce | `exact_summary.json` |
| **quality** | Uses Glance's judged results (`topk_results.csv`); looks up which judged products are in the index and pass the filters; computes Hit@K per alpha | `quality_summary.json` |
| **report** | Recall, overlap, latency, exactness and quality → findings | `summary.json`, `scorecard.md` |

### Part 3A. The 9 request types, and how the code builds them

**The 9 types:** `dense`, `sparse`, and `hybrid_a0.0`, `hybrid_a0.1`, `hybrid_a0.3`, `hybrid_a0.5`, `hybrid_a0.7`, `hybrid_a0.9`, `hybrid_a1.0`. **All use the same Glance filters** (unlike EXP-1, EXP-2 has no "no filter" type).

**Step 1: the list of alphas and the switches (lines 68 and 84–86):**
```python
ALPHAS = [0.0, 0.1, 0.3, 0.5, 0.7, 0.9, 1.0]
MODES = {"dense":  dict(dense=True,  sparse=False, alpha=None),
         "sparse": dict(dense=False, sparse=True,  alpha=None)}
MODES.update({f"hybrid_a{a:.1f}": dict(dense=True, sparse=True, alpha=a) for a in ALPHAS})   # 7 hybrid types
```

**Step 2: one function builds every request (`build_request`, lines 187–206).** It works the same way as EXP-1; the only new part is that alpha comes from the request type:
```python
if m["dense"]:  dp.feature_vector = q["dense"]                       # meaning vector
if m["sparse"]: dp.sparse_embedding = SparseEmbedding(q["sparse_dims"], q["sparse_vals"])   # keyword vector
dp.restricts = [instock, status, catalog_id, retailer_domain, country]  # Glance filters, always on
query = Query(datapoint=dp, neighbor_count=500)
if m["alpha"] is not None:
    query.rrf = Query.RRF(alpha=m["alpha"])                          # merge with THIS type's alpha
```
The same request goes to the fast index and to BruteForce, so each alpha is scored against BruteForce **at the same alpha**.

**Step 3: the sweep loops over query × type × both indexes (`stage_sweep`, lines 428–473).** For dense and sparse it also saves the **scores** BruteForce returned, which the exactness check needs.

**Step 4: latency** uses the same open-loop method as EXP-1 (requests on a fixed schedule, split over processes, client checked). It covers 5 types: `dense`, `sparse`, `hybrid_a0.3`, `hybrid_a0.0`, `hybrid_a1.0` (line 96).

**Step 5: exactness check (`stage_exact`, `_exact_worker`, lines 642–890):**
- 6 processes each read one data file at a time from GCS and compute **exact dot products** for 50 queries:
  - dense with numpy (float32);
  - sparse with scipy sparse matrices.
- They apply **the same filters** as Vertex: an allow-list needs a matching token; a deny-list excludes only products that hold a denied token.
- They keep the true top 500 per query, then compare with BruteForce's list:
  - **"tie-aware"** counts a BruteForce product as correct if its true score is at least the 500th true score, so ties aren't punished;
  - they also check that the scores BruteForce returned equal our exact scores.
- **Verdict EXACT** only if tie-aware ≥ 0.999 at every K, the scores agree, and the filters agree.

**Step 6: quality (`stage_quality`, lines 974+):**
- Reads Glance's judged file.
- Asks the index which judged products exist (a read-only lookup, cached).
- Keeps those that pass the query's filters.
- Counts how many appear in each alpha's top K.
- **Paired** Δ = per-query difference vs alpha 0.3, with a standard error.

**One line for the call:** "One read-only script sends the production request with 7 different alpha values, plus dense-only and sparse-only, to both indexes. It load-tests the main request types, checks BruteForce exactness offline against all 5M records, and scores each alpha by how many of Glance's judged relevant products it returns."

---

## Part 4. Likely questions and short answers

**Q1. "Glance says they're moving 100% to hybrid. Why are you testing alpha?"** *(Vinod asked this.)*
- "100% hybrid" means **every query runs both searches**. That's already true today.
- **Alpha** is the **mix inside each hybrid query**. Today it's 30% meaning / 70% keywords.
- Glance asked in writing whether their fusion weighting "leaves quality on the table". EXP-2 answers exactly that: yes, 0.5–0.7 finds more of their judged relevant products.

**Q1a. What can "complete / 100% hybrid" mean? (it's ambiguous, so clarify it on the call)**

| Possible meaning | Is it true today? | Does EXP-2 still matter? |
|---|---|---|
| **Every query is hybrid** (sends both meaning and keyword vectors) | **Already true:** Glance confirmed more than 99.9% of traffic is hybrid at alpha 0.3 | **Yes.** Every hybrid query still needs a mix (alpha). EXP-2 shows which mix finds more relevant products. |
| **Every surface/product area moves to hybrid** (if some still use meaning-only search) | Unknown; ask Glance which surfaces aren't hybrid yet | **Yes.** EXP-2 shows hybrid beats meaning-only (Hit@500 76–80% vs 67%), and costs ~6–8 ms more per search. |
| **Every product in the index has both vectors** ("a complete hybrid index") | **Not quite:** production has 49.87M dense but 49.44M sparse vectors, so about **424,000 products have no keyword vector** and can only be found by meaning | Separate data fix (fill the missing sparse vectors). EXP-2's alpha finding still applies afterwards. |
| **Pure keywords or pure meaning** | — | No. "Hybrid" by definition mixes both. |

- **The key point:** "hybrid" says that **both** searches are used. **Alpha** says **how much each one counts**.
  - Alpha 0.3 (30% meaning / 70% keywords) **is** hybrid.
  - So is 0.5 (50/50) and 0.7 (70/30).
  - Going "100% hybrid" doesn't choose the mix. Someone still has to set alpha, and EXP-2 measures which value works best.
- **One line for the call:** "Hybrid means both searches run. Alpha is how they're weighted. Glance is already hybrid at 30/70; EXP-2 shows that a more balanced mix, 50/50 or 70/30, finds clearly more of their judged relevant products. Can you confirm what 'complete hybrid' means for you: all queries, all surfaces, or every product having both vectors?"

**Q2. "Higher alpha gives higher recall. Isn't that just because the meaning search is easier for the index?"**
- Partly, yes: recall per alpha is agreement with BruteForce at the same alpha. That's why we **also** measured judged relevant products.
- Those improve too: Hit@100 55.6% → 69.3% at 0.5, and the gain holds even on BruteForce (68.2% → 81.6%). So it's a real relevance gain.

**Q3. "Do 0.5 or 0.7 cost more latency?"**
- **Not measured.** We load-tested 0.3, 0.0, 1.0, dense and sparse.
- **Expectation:** about the same as 0.3 (~23 ms P50, ~37 ms P99 at 565 QPS), because both searches run at any alpha between 0 and 1. To be confirmed in a follow-up load test.

**Q4. "Is BruteForce really the truth?"**
- **Dense: yes, exact** (checked against all 5M records).
- **Sparse: no.** It misses some true keyword matches (0.748 at top 500) and sometimes returns fewer than 500.
- So sparse/hybrid recall is "vs BruteForce". The **judged-products** numbers don't depend on BruteForce, which is why they matter.

**Q5. "Why do alpha 0.0 and 1.0 have lower latency than 0.3?"**
- At 0.0 or 1.0 one side has zero weight. The results are identical to a single search, and so is the latency.
- So Vertex appears to skip that side. *(Inferred, not documented.)*

**Q6. "Why does R@5 dip at alpha 0.5 (0.613 vs 0.661)?"**
- At the very top, a balanced merge is sensitive to small differences between the fast index's lists and BruteForce's.
- The keyword side of the fast index finds only 37.5% of BruteForce's top 500, so products that BruteForce ranks high through both sides can be missing.
- Deeper (K ≥ 50) the gain is large. Judged-relevant products improve at every K.

**Q7. "Is the judged data biased?"**
- Yes, in favour of **today's** setting: judgements exist only for products production served, mostly at alpha 0.3. Only ~9% of judged products are in our 5M sample.
- That makes the alpha 0.5/0.7 gains **conservative**. Use it for direction, not as an absolute quality score.

**Q8. "Which alpha do you recommend?"**
- **0.5 for the best top-100** (Hit@100 69.3%); **0.7 for the best top-500** reranker pool (Hit@500 80.3%).
- Both change the top results a lot (0.7 keeps only 13% of today's top-10). Recommend an online A/B or judge test first, and confirm at 10M.

**Q9. "Does this hold on 9 shards?"**
- Alpha is applied after both searches. The weighting effect should carry over, but recall and latency must be confirmed on the 10M (2-shard) index.

**Q10. "What should Glance tell the Vertex team?"**
- BruteForce is exact for dense but **not exact for sparse** (0.748 at 500, and sometimes fewer than 500 results).
- Anyone using BruteForce as ground truth for sparse or hybrid should know this.

---

## Part 5. What NOT to claim
- ❌ "Alpha 0.5/0.7 have zero latency overhead vs 0.3": not measured (say "expected similar").
- ❌ "1.16M latency calls": about 0.58M measured (the EXP-2 plan says ~646k sent, ~581k measured).
- ❌ "Higher recall = better results" on its own: use the judged hit rates for relevance.
- ❌ Calling alpha 0.3 a "legacy" mode: it's Glance's current production setting.
- ❌ Absolute production numbers: 1 shard at 5M; confirm at 10M.

## Part 5A. This guide vs the spreadsheet (EXP-2 tab)
The recall and judged-hit numbers in this guide are **identical** to the EXP-2 tab.

> ✅ **Status (15 Oct): all fixes below are applied** in the EXP-2 and EXP2-Summary tabs (with the user's approval). The EXP2-Summary tab also now says "~581k measured load-test requests" instead of "1.16M", and its "Final Production Guidance" became "Next steps (provisional)". Its overlap columns were checked against the EXP-2 results doc and are correct (α 0.5 / 0.7 keep 55.4% / 11.1% of today's top 50, and 55.2% / 13.4% of the top 10).

The fixes that were applied:

| Spreadsheet EXP-2 tab | What it shows | Correct (this guide) | Fix in the sheet |
|---|---|---|---|
| Rows 10–16, columns C–E (R@10 / R@100 / R@500) | 21 recall values | ✅ same | — |
| Rows 10–16, column I, and rows 28–32 (judged hits) | Hit@K, paired Δ | ✅ same | Write "pts" instead of "%" for the Δ values |
| F9 header | "Single request median (ms)" | The values are **P50 at 565 QPS** | Rename F9 to "P50 at 565 QPS (ms)" |
| Rows 11, 13, 14, 15, columns F–H | Latency and "≥565" for alpha 0.1 / 0.5 / 0.7 / 0.9 | **Not load-tested** | Replace with "not load-tested" |
| B19 | "true 2-leg RRF (α=0.3–0.7) adds ~6–8 ms" | Measured at α = 0.3 only | "(measured at α=0.3)" |
| C37, C38, C40 | "zero latency overhead / identical latency / no extra latency" | Not measured | "expected similar to α=0.3 (not load-tested)" |
| B39 | "Sparse-priority legacy mode" | Current production | "Current production (sparse-heavy)" |

## Part 5B. Why these alpha values (0.0, 0.1, 0.3, 0.5, 0.7, 0.9, 1.0)?
- **It isn't "odd numbers only".** The grid covers the **whole range in steps of 0.2**, centred on 0.5:
  - **0.0 and 1.0**, the two ends: needed for the consistency check (they must equal sparse-only and dense-only);
  - **0.3**, today's production value: needed as the baseline;
  - **0.5**, the balanced middle;
  - **0.1, 0.7, 0.9**, the remaining 0.2 steps, symmetric around 0.5.
- **Why not every 0.1:** each extra alpha costs 4,000 more search calls (2,000 queries × 2 indexes) plus judged-quality work. The 0.2 grid shows the **shape of the curve** at about half the cost.
- **What the curve shows:** a smooth trend, with the best region between **0.5 and 0.7**. 0.2, 0.4, 0.6 and 0.8 would land between their neighbours.
- **To fine-tune:** a small follow-up could test **0.4, 0.5, 0.6, 0.7** (about 16,000 calls, roughly 15–20 minutes). It needs the 5M LARGE and BruteForce redeployed (both are undeployed now) or, better, it can run on the 10M index.
- **Honest note:** the plan chose this grid. It doesn't record a deeper reason than covering the range, production and the two ends.

## Part 6. Three sentences to open with
1. "EXP-2 tested the hybrid merge weight, alpha. Today Glance uses 0.3, which gives keywords 70% of the weight. In practice the top results are almost pure keyword search (95.6% the same as keywords-only)."
2. "Moving to 0.5 finds **14 points more** of Glance's judged relevant products in the top 100 (55.6% → 69.3%). 0.7 gives the best top-500 pool (80.3%). The gain holds even with exact search."
3. "Hybrid costs about 6–8 ms more than a single search. Latency stays flat from average to peak traffic. And we found that Vertex's BruteForce is exact for dense but not for sparse, which matters for how all recall numbers are read."
