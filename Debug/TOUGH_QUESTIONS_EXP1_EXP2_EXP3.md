# Tough questions: EXP-1, EXP-2, EXP-3 (5M)

How to use this sheet: each answer is **2–3 lines you can say**, followed by **the number that proves it**. All numbers come from our runs: EXP-1 `exp1_golden_20261008`, EXP-2 `exp2_golden_20261012` (Anita's analysis document), EXP-3 `exp3_golden_20261012_gt`. All runs used the 5M test index (1 shard) and Glance's 2,000 golden queries.

**Two terms used throughout:**
- **Recall@K** = of the **true** best K products (from BruteForce, which checks every product), how many the fast index found.
- **Judged "good" hit rate** = of the products Glance's judges scored 3 ("exact match") for a query, how many we return in the top K.

---

## General

**Q1. Why should we trust these numbers?**
Every script was audited before it ran, every run had zero errors, and the results reproduce across experiments.
- EXP-3's default setting gives exactly EXP-1's numbers: hybrid R@10 0.663, R@100 0.553, R@500 0.518.
- Repeat queries differ by at most 0.00001 recall.
- The test client was never the bottleneck.

**Q2. What did these experiments cost?**
Vector Search has **no per-query charge**; you pay for machines and builds.
- Our 5M deployments: ~$5.2/hour.
- Copying the 30M data: ~$9.
- Each 10M index build: ~$135 (45.07 GiB × $3/GiB; confirm in billing).

---

## EXP-1: production baseline vs exact search

**Q3 (Vinod). "BruteForce has exactly the same 5M data. Comparing against it, recall should be very high. Why is it ~0.55?"**
Same data, **different way of searching**:
- BruteForce checks every product.
- Glance's Tree-AH index takes a shortcut: it searches 5% of the tree and keeps only **500 candidates** for 500 results.

Recall measures what that shortcut misses. It's not a data problem: the same index, told to search more, reaches dense R@100 **0.997** (EXP-3, 5,000 candidates, 100% of leaves).

**Q4. Then why is hybrid so much lower than dense?**
The keyword (sparse) leg is the weak one, and alpha 0.3 gives it 70% of the weight.
- EXP-1 R@100: dense **0.856**, sparse **0.544**, hybrid **0.553**.
- EXP-3: sparse rises to **0.835** with 5,000 candidates, so the shortlist size is what limits it.

**Q5. Is it because the test index has only 1 shard?**
Partly, but that's the secondary reason. Candidates count **per shard**: our test has 1 × 500, production about 9 × 500. So production likely does better than our 1-shard number. The main reason is the shortcut plus the small shortlist (Q3–Q4), and 10M (2 shards) will show the shard effect.

**Q6. Your first results showed 0.88. Why 0.55 now?**
The first run used **product vectors as queries**: searching for items that are already in the index, which is easy. Golden queries are **real user text**, and their best matches are spread across more of the tree. Provisional hybrid R@10 was 0.878; golden hybrid R@10 is 0.663.

**Q7. Is BruteForce really an exact answer key?**
- **Dense: yes.** EXP-2 part C checked it against our own exact calculation over all 5M products: tie-aware recall 1.0.
- **Sparse: no.** Even BruteForce's keyword search is approximate: 0.916 / 0.857 / 0.748 at K = 10 / 100 / 500, with scores and filters matching exactly.

So sparse and hybrid recall is "vs BruteForce", and we also report the judged "good" hit rate, which doesn't depend on BruteForce.

**Q8. Are the filters (in stock, active, deny lists, country) causing the low recall?**
No. The same request without filters gives almost the same recall: hybrid R@100 **0.528** without filters vs **0.553** with them (EXP-1).

**Q9. Does it matter that the 5M sample may not contain the right products for some queries?**
Not for recall. BruteForce searches the same 5M, so recall compares like with like. It matters for the judged hit rate, which is why that uses only judged products present in the 5M (628 products, 487 queries).

**Q10. Is `neighbor_count` really 500 in production?**
Yes. Glance confirmed 500 on the UDP index, never changed per request. The index default candidates is also 500 (UDP profile), so candidates = results, with no spare room.

**Q11. How fast was the baseline?**
P99 **46 ms** at 81 QPS (Glance's average) and **50 ms** at 565 QPS (Glance's peak), 0 errors, on 2 replicas (EXP-1, production request).

---

## EXP-2: hybrid and RRF alpha

**Q12 (Vinod). "Glance said they're moving 100% to hybrid. Why are we tuning alpha?"**
"100% hybrid" means **every query runs both legs**; Glance confirmed over 99.9% of searches already do. Alpha is the **weight between the two legs inside each hybrid query**, so alpha 0.5 is still 100% hybrid.

Glance asked us in writing (`search-retrieval-description.md`, item 2): "*whether the fusion weighting … are leaving quality on the table*". EXP-2 answers exactly that.

**Q13. What does alpha 0.3 mean?**
In the merge (RRF), the dense leg gets 30% of the weight and keywords 70%. Alpha 1.0 = dense only, 0.0 = sparse only. EXP-2 confirmed both ends behave exactly like single-leg search (overlap 1.0).

**Q14. What did EXP-2 find?**
Alpha 0.3 leaves quality on the table:

| alpha | Hybrid R@100 vs BruteForce | Judged "good" @100 | Judged "good" @500 |
|---|---|---|---|
| 0.3 (production) | 0.553 | 55.6% | 76.1% |
| 0.5 | 0.714 | 69.3% (+13.7 pts) | 79.2% |
| 0.7 | 0.822 | 66.6% (+11.0 pts) | 80.3% |

**Q15. Why does a higher alpha help?**
At 0.3 the result list is dominated by the weaker keyword leg: at K = 50, hybrid shares **95.6%** of its results with pure sparse and only **6.2%** with pure dense. Higher alpha brings the stronger meaning-based results into the top ranks.

**Q16. Isn't recall vs BruteForce unfair, since BruteForce's sparse leg isn't exact?**
That's why we also use the **judged "good" hit rate**, which uses Glance's own human judgements and not BruteForce. It improves too: +13.7 points at the top 100 for alpha 0.5.

**Q17. Does hybrid cost latency compared with one leg?**
Yes, a little. Single-leg searches run at 15–19 ms P50 and 22–28 ms P99. Hybrid alpha 0.3 runs at **23–24 ms P50 and 34.5–36.9 ms P99** at 81 and 565 QPS: about +6–8 ms P50 and +10 ms P99, because both legs run and are merged.

**Q18. So should Glance switch to alpha 0.5?**
It's promising, but not final. It's 5M, 1 shard, and their ranker sits after retrieval, so it needs confirming on 10M and in their own A/B. EXP-3 shows a second fix for the same weak keyword leg (more candidates), so the best answer may combine both.

---

## EXP-3: Tree-AH query-time tuning

**Q19. Do we need to rebuild the index to change these settings?**
**No.** `approximate_neighbor_count` (candidates), `fraction_leaf_nodes_to_search_override` (leaves) and `neighbor_count` (results) are all **per-request** fields in the query.

**Q20. What exactly did you tune?**
- Candidates: 500 / 1,000 / 2,000 / 5,000.
- Share of the tree searched: 2% / 5% / 10% / 20%, plus 50% and 100% as ceilings.
- Results: 500 / 1,000 / 2,000 / 5,000.

Each was measured for recall (2,000 queries) and for P50 / P95 / P99 at 81 / 200 / 400 / 565 / 800 QPS.

**Q21. What did you find?**
- **Candidates drive hybrid recall:** R@100 0.553 → 0.643 → 0.728 → **0.829** for 500 → 1,000 → 2,000 → 5,000.
- **Leaves only affect the dense leg** (sparse recall is identical at every leaf fraction) and cost a lot of time.

**Q22. Which setting do you recommend?**
Provisionally, for hybrid traffic: **5,000 candidates, leaves left at the default 5%**.
- Hybrid R@100 0.553 → **0.829**; judged "good" @500 76.1% → **85.3%**.
- P99 **55.7 ms at 565 QPS** on the same 2 replicas.

Final values depend on Glance's latency budget and recall target, and on confirmation on 10M.

**Q23. Will that need more machines?**
In our test it held Glance's peak (565 QPS) on the same 2 replicas. It leaves less headroom: today's setting held 800 QPS, the tuned one fails between 565 and 800. Multi-shard behaviour needs checking on 10M before advising on capacity.

**Q24. Why not just search more of the tree for better recall?**
It mainly helps the dense leg and costs capacity:
- 5,000 candidates at 20% gives only +0.009 hybrid R@100 over 5%;
- but it falls to **200 QPS** on 2 replicas, against 565.

**Q25. Can we make search *faster* than today?**
Yes. **1,000 candidates, 2% of leaves**:
- P99 34.4 vs 40.1 ms at 565 QPS, and 38.2 vs 63.1 ms at 800 QPS;
- hybrid R@100 **higher** (0.636 vs 0.553);
- dense R@10 lower (0.784 vs 0.890), so it's not for meaning-only searches.

**Q26. What about 1,000 or 2,000 results for re-ranking or brand boosting?**
It works per request. Set candidates to **twice** the number of results; candidates below results is an error (`InvalidArgument 400`). Recall at that depth: R@1000 0.664, R@2000 0.699. It costs capacity: 200 QPS on 2 replicas with 20% of leaves.

**Q27. Is a 5M, 1-shard test representative of 50M on 9 shards?**
Per shard, yes: each production shard holds about 5.5M products and sees every query, so per-replica load is comparable. But candidates count per shard, so recall and merge cost will differ. That's why the 10M (2-shard) index is next.

**Q28. Did you just pick the setting that looks best on these 2,000 queries?**
We checked. We split the queries in two halves by a hash of the query id:
- all 19 settings rank in the same order on both halves;
- the best setting on half A loses only **0.002** R@100 on half B.

**Q29. Why did EXP-3 use fresh BruteForce answers instead of EXP-1's?**
A safety check re-queried BruteForce and found hybrid result **order** varies between calls. Products with tied fused scores come back in a different order; the sets are identical. Dense and sparse matched 100%. We fetched fresh answers (about 6,000 calls) to be strict.

---

## When you don't know

> "Good question. I don't want to guess. Let me check it against the data and come back to you today."

That's normal and professional on a POC. Then bring the question to me and we'll answer it with the data.
