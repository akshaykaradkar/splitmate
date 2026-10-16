# EXP-2 Script: Hybrid Search & RRF Tuning (5M)

*Read on your phone during the call. **Bold** = what to say. "👉" = where to point on the screen. The sheet cells were corrected on 16 Oct to match this script.*
*Order on screen: **" EXP2-Summary"** tab → **EXP-2** tab.*

---

## 0. The 20-second cheat sheet

- **Question:** today's hybrid gives sparse 70% of the weight (α 0.3). Does that mix leave relevant products out, and what does hybrid cost in latency?
- **4 numbers to remember:**
  - **95.6%**: today's top 50 is 95.6% the same as sparse-only search, so today's hybrid ≈ keyword search.
  - **0.553 → 0.714**: hybrid Recall@100 at α 0.3 → α 0.5.
  - **55.6% → 69.3%**: Glance's judged score-3 products in the top 100, α 0.3 → α 0.5 (+13.7 points).
  - **23.2 ms P50 / 36.9 ms P99** at 565 QPS for today's hybrid. Single-leg searches take 15–18 ms P50, so hybrid costs about 6–8 ms.
- **One-sentence story:** "Today's hybrid behaves almost like keyword-only search at the top. A balanced mix brings in the dense side and finds clearly more of your relevant products, but it changes the top results a lot, so it needs user validation."

---

## 1. Mini-glossary: the terms in this experiment

**α (alpha), the RRF weight**
```text
score = α / (60 + dense_rank) + (1 − α) / (60 + sparse_rank)
```
- α = dense weight. **0.3 today** (dense 30%, sparse 70%).
- α 1.0 = dense only; α 0.0 = sparse only. We checked: α 1.0 gave **identical** lists to dense-only and α 0.0 to sparse-only, on both indexes.
- Worked example at α 0.3: dense-only #1 = 0.3/61 = **0.0049**; sparse-only #80 = 0.7/140 = **0.0050**. The dense side's best product ranks below about 80 keyword products.
- At α 0.5: both #1s = 0.5/61 = 0.0082 → the two sides alternate from rank 1.
- The formula is verified: our offline recomputation reproduced Vertex's hybrid lists exactly for 40 of 40 queries (EXP-1 audit).

**The 7 alphas tested**: 0.0, 0.1, 0.3, 0.5, 0.7, 0.9, 1.0 (both ends, today's value, then steps of 0.2). Plus dense-only and sparse-only requests.

**Recall@K at a given α**
```text
Recall@K(α) = | LARGE top-K(α) ∩ BruteForce top-K(α) | / K
```
- Each α is compared with BruteForce **at the same α**. So higher recall = the fast index agrees better with the exact answer at that α.
- It does **not** by itself mean better products. That's what judged hits show.

**Overlap with production (α 0.3)**
```text
overlap@K(α) = | top-K(α) ∩ top-K(α 0.3) | / K
```
- It measures **how much the result list changes**, not whether it's better.
- 95.6% at K = 50 (α 0.0 vs 0.3) → about 48 of 50 the same.
- α 0.5 keeps 55.2% of today's top 10 (so about 45% of the top 10 changes) and 76.3% of the top 500.

**Judged score-3 hits@K**
```text
hit@K for one query = its score-3 products found in the top K / its score-3 products
judged hits@K       = mean over the 487 queries
```
- Score-3 = rated "highly relevant" by Glance's judges, present in our 5M data and passing the query's filters: 487 queries, 663 products.
- Example: query A has 2 such products and 1 is in the top 100 → 0.5; query B has 1 and it's found → 1.0; average = 75%.
- "+13.7 pts" = percentage points (55.6% → 69.3%), not 13.7% of 55.6.
- Independent of BruteForce. Biased towards today's α (judgements exist only for products production showed), so gains are, if anything, understated.

**BruteForce exactness (the EXP-2 check other tabs refer to)**
- 50 queries; we computed exact dense and sparse scores ourselves over **all 5,000,000 records** with the same filters, then compared with BruteForce's lists.
- **Dense: EXACT.** Tie-aware 1.000 at 10 / 100 / 500, score error 0.0.
- **Sparse: NOT exact.** Tie-aware 0.916 / 0.857 / 0.748. For 5 of 50 queries BruteForce returned fewer than 500 results (as few as 199).
- So sparse/hybrid recall = "agreement with Vertex BruteForce". Judged hits don't depend on BruteForce.
- *This is in row 44 of the EXP-2 tab.*

**P50 / P95 / P99, open loop**: same as the other experiments.
- Fixed send schedule (request i at t0 + i/rate), 20 s warm-up dropped, 180 s measured, 2 replicas.
- Requests: 81 × 180 = 14,580 and 565 × 180 = 101,700 per cell.

---

## 2. SCRIPT: screen 1, the " EXP2-Summary" tab

👉 **Rows 1–2 (plan row)**
> **"EXP-2 is hybrid search and RRF tuning. Aim: evaluate dense, sparse and hybrid latency and test the RRF alpha trade-offs. The metric is the latency and recall balance. All results are on 5M; 10M and 20M come next."**

👉 **Row 3 (yellow key finding): the deep explanation.** Take it in 5 short steps, about 2 minutes.

**Step 1: what α is.**
> **"Every hybrid query runs two searches, dense for meaning and sparse for keywords, and Vertex merges the two ranked lists with Reciprocal Rank Fusion. Each product gets: alpha over 60 plus its dense rank, plus one minus alpha over 60 plus its sparse rank. Alpha is the dense weight. Today it's 0.3, so dense counts 30% and sparse 70%."**

**Step 2: why today's top 50 is "95.6% the same as keyword-only search".**
> **"Here's what 0.3 does in practice. The best dense-only product, rank 1 in the dense list and missing from the sparse list, scores 0.3 over 61, about 0.0049. A sparse-only product at rank 80 scores 0.7 over 140, 0.0050, already higher. So a product the dense side loves, but the keyword side didn't find, lands below roughly the top 80 keyword products."**
>
> **"We measured the effect directly. For each of the 2,000 queries we compared today's top 50 at alpha 0.3 with the top 50 of a keyword-only search, alpha 0. On average 95.6% are the same products, about 48 of 50. Against dense-only it's just 6.2%, about 3 of 50. So today's 'hybrid' is, at the top, almost pure keyword search. That's the 95.6% in the key finding."**

```text
overlap@50 = | top-50(α 0.3) ∩ top-50(α 0.0) | / 50   per query, averaged over 2,000 = 0.956
```

**Step 3: Recall@100, 0.553 → 0.714.**
> **"Recall@100 is: of BruteForce's exact top 100 for the same request, how many does the fast index return in its top 100? At alpha 0.3 it's 0.553, about 55 of 100. At 0.5 it's 0.714, about 71. Each alpha is compared with BruteForce at the same alpha. The gain comes from giving more weight to the dense leg, which the index searches much more accurately (dense-only 0.856 vs sparse-only 0.544)."**

**Step 4: judged score-3 Hit@100, 55.6% → 69.3% (the "is it really better?" check).**
> **"Recall tells us the fast index agrees with the exact search. It doesn't tell us the products are good. For that we used your own relevance judgements. With the golden queries you gave us topk_results.csv: for each query, products rated 0 to 3 by your judges, where 3 means highly relevant, exactly what the shopper wanted."**
>
> **"We kept the products rated 3, by every judge for that query, that also exist in our 5M test data and pass that query's filters. That leaves 487 queries and 663 highly relevant products, about 1.4 per query."**
>
> **"Hit@100 for one query is the share of its score-3 products that appear in the top 100. Then we average over the 487 queries. Today, at alpha 0.3, 55.6%. At 0.5, 69.3%. So 13.7 percentage points more of your own highly relevant products reach the top 100."**

```text
hit@100(query)  = score-3 products of the query found in the top 100 / score-3 products of the query
judged Hit@100  = mean over the 487 queries
```

*Illustrative example (not real data):*
- query A has 2 score-3 products and 1 is in the top 100 → 0.5;
- query B has 1 and it's in → 1.0;
- query C has 1 and it's missing → 0.0;
- average = (0.5 + 1.0 + 0.0) / 3 = **50%**.

> **"Two checks make this solid. It doesn't depend on BruteForce at all; it's your judges. And it holds on exact BruteForce too: Hit@100 goes 68.2% → 81.6% at alpha 0.5. One honest caveat: judgements exist only for products production already showed, mostly at 0.3. That favours today's setting, so the real gain is, if anything, larger."**

**Step 5: latency (the last sentence of the cell).**
> **"On cost: at your 565 QPS peak, today's hybrid has P50 23.2 ms and P99 36.9 ms. Alpha 0.5 runs the same two legs and the same fusion, so we expect the same latency. We measured 0.3 directly; 0.5 wasn't load-tested separately. It's a short test we'll add at 10M."**

**One-breath version (if time is short):**
> **"Today's alpha 0.3 gives keywords 70% weight, so the top 50 is 95.6% the same as keyword-only search. Moving to 0.5 lets the dense side in: recall against the exact search goes from 55% to 71%, and your own highly relevant products in the top 100 go from 56% to 69%. Same two legs, so we expect the same latency."**

👉 **Row 6 (Recall@100 0.714)**
> **"Recall vs BruteForce at the same alpha: 0.553 at alpha 0.3, 0.714 at 0.5, and 0.822 at 0.7. The more weight on dense, the better the fast index agrees with the exact answer, because the dense leg is the more accurate one."**

👉 **Row 7 (judged Hit@100 69.3%)**
> **"The real relevance check, with your judged products: in the top 100, 55.6% today, 69.3% at alpha 0.5. That's 13.7 points more. It also holds on exact BruteForce, 68.2% → 81.6%, so it's a genuine relevance gain."**

👉 **Row 7, column C (comparison):** "Production 55.6% · Dense-only 55.9% · Sparse-only 40.0%".
> **"And the comparison: dense-only finds 55.9% in the top 100 and sparse-only 40%. Hybrid at 0.5, 69.3%, beats both: mixing helps. In the top 500 it's the same story: 43% sparse-only, 67% dense-only, 76 to 80% hybrid."**

👉 **Row 8 (P50 23.2 ms, hybrid α 0.3)**
> **"Latency at your 565 QPS peak: today's hybrid has P50 23.2 ms and P99 36.9 ms, with 0 errors over 101,700 requests. Single-leg searches take 15–18 ms. So running both legs and fusing them costs about 6–8 ms. Alpha 0.3 is the only mixing alpha we load-tested; 0.5 runs the same two legs, so we expect the same, and we'll confirm it in a short test at 10M."**

Avoid "in parallel": how Vertex schedules the two legs internally isn't documented.

👉 **Row 9 (R@500 0.629)**
> **"Deeper, at the 500 results your ranker receives: 0.518 today, 0.629 at 0.5, and the peak 0.684 at 0.7."**

👉 **Row 10 (overlap)**
> **"How much would users notice? Alpha 0.5 keeps 76% of today's top-500 pool but only 55% of today's top 10, so about 45% of the top 10 changes. Today's list is 95.6% the same as keyword-only search at top 50."**

👉 **Rows 12–15 (Keep in mind)**
- Row 13: **"1 shard at 5M with 2 replicas; production runs 9 shards. We confirm on the 10M index."**
- Row 14: **"Moving from 0.3 to 0.5 replaces about 45% of today's top 10 with dense matches, so we'd validate with a user-facing relevance test before rollout."**
- Row 15: **"For transparency: we load-tested dense, sparse, alpha 0, 0.3 and 1. The other mixing alphas are expected to cost the same as 0.3, but weren't measured."**

👉 **Row 16 (link)** → click to the EXP-2 tab.

---

## 3. SCRIPT: screen 2, the EXP-2 tab

👉 **Rows 1–4 (title, context, note)**
> **"2,000 golden queries, 5M products, 1 shard on 2 replicas, baseline alpha 0.3. About 581,000 measured load-test requests, 0 errors."**

👉 **Row 4 (ℹ️ note), in one sentence:**
> **"Five request types were load-tested. Mixing alphas run both legs, so we expect them to cost the same as 0.3. Alpha 0 and 1 cost the same as a single search, and their results equal sparse-only and dense-only, so Vertex appears to skip the zero-weight leg. That last part is our inference, not documented."**

> **Tip for all three tables:** don't read every row. Say "let me point you to three cells", then name the **row** and the **column** each time, so people's eyes follow you. Each table has 3 highlights at most.

👉 **Table 1: Latency by mode and traffic rate (rows 6–17)**

*How to read it:* each row is one request type at one traffic rate. Requests = how many were measured (81 × 180 s = 14,580; 565 × 180 s = 101,700). P50 / P95 / P99 = the time within which 50 / 95 / 99% of searches finished. 0 errors everywhere.

Opening line:
> **"This table is the cost side. Five request types, at your mean of 81 and your peak of 565 searches per second. I'll point to three cells."**

- **Highlight 1: the 565 QPS block, rows 13–16, column D (P50).**
  > **"Look at the single-leg searches at peak: dense 17.6 ms, sparse 15.6, and alpha 0 and alpha 1 about 15 to 16. One leg costs 15 to 18 milliseconds."**
- **Highlight 2: row 17, hybrid_a0.3 at 565 QPS, columns D and F.**
  > **"Now today's hybrid, row 17: P50 23.2 ms, P99 36.9 ms. Running both legs and fusing them costs about 6 to 8 ms more than one leg. That's the price of hybrid, and it's small."**
- **Highlight 3: row 12 vs row 17, column F (P99 of α 0.3 at 81 vs 565).**
  > **"And traffic barely matters: P99 is 34.5 ms at 81 QPS and 36.9 at 565, seven times the traffic for 2 ms more."**

👉 **Row 18 (note under the table):** "α 0.1, 0.5, 0.7 and 0.9 were not load-tested; expected to match α 0.3."
> **"Only these five request types were load-tested. Alpha 0.1, 0.5, 0.7 and 0.9 weren't measured separately. We expect them to cost the same as 0.3, because both legs run, and we'll confirm that at 10M."**

Side note if asked: alpha 0 and alpha 1 cost the same as a single search; Vertex appears to skip the zero-weight leg (inferred, not documented).

👉 **Table 2: Recall vs BruteForce (rows 21–30)**

*How to read it:* each row is one alpha; R@K = share of BruteForce's top K (at the same alpha) that the fast index returns.

Opening line:
> **"This is the quality side, measured against the exact search. Focus on the R@100 column, and on three rows."**

- **Highlight 1: row 25, α 0.3 (today), column D: 0.5534.**
  > **"Today: 0.553. About 55 of the exact top 100 come back. That's the same number as EXP-1."**
- **Highlight 2: row 26, α 0.5, column D: 0.7135.**
  > **"At 0.5 it's 0.714, plus 16 points."**
- **Highlight 3: row 27, α 0.7, columns D and F: 0.8219 and 0.6835.**
  > **"At 0.7, 0.822, and the best R@500 of all rows, 0.684, even above dense-only, because the fused list draws on both legs."**

Optional extra line (validation):
> **"Rows 23 and 29, alpha 0 and alpha 1, are identical to EXP-1's sparse-only and dense-only rows. That confirms alpha behaves as documented: 0 is pure sparse, 1 is pure dense."**

Row 30 (key-finding note under the table) repeats the highlights. Just say **"plus 16 points at 100 and plus 11 at 500"**, then move on.

👉 **Table 3: Result list sensitivity / overlap with today (rows 33–41, key-finding note in row 42)**

*What every cell means:*
- For one query, take today's top-K list (α 0.3) and the top-K list at the row's alpha, and count the products in both. Divide by K, then average over the 2,000 queries.
- 100% = identical lists; 0% = nothing in common.
- It measures **how much users' results would change**, not whether they're better.

```text
cell(α, K) = | top-K(α) ∩ top-K(α 0.3) | / K      averaged over 2,000 queries
```

- **Columns:** **LARGE** = the lists from the fast (Tree-AH) index, i.e. what users would actually see. **BF** = the lists from exact BruteForce, the same comparison without index approximation.
- **Why both:** if the change appears in BruteForce too, it's caused by the **fusion weight itself**, not by the index. It does.
- **Row 37 (α 0.3)** is 100% everywhere, because it's compared with itself.

How to read the other rows (LARGE columns):
- **α 0.0 / 0.1:** 88–93% at top 10 and top 100 → today's list is nearly the keyword-only list.
- **α 0.5:** 55% at top 10 → about 4–5 of today's top 10 get replaced. But 76% at top 500 → most of the pool your ranker receives stays.
- **α 0.7:** 13% at top 10 → almost a completely new top 10. 52% at top 500.
- **α 0.9 / 1.0:** 5–12% at top 10 and top 100 → completely different lists (dense-only results).

Where does the "95.6%" in the key finding come from? It's the same measure at **top 50** (α 0.0 vs α 0.3). Top 50 isn't a column here; the table shows 10 / 100 / 500. So say "at top 50, 95.6%; here at top 10 and 100 it's 88% and 92%".

Opening line:
> **"This table answers: if Glance changed alpha, how different would the results look? Each cell is the share of today's list that stays the same. I'll point to three cells."**

- **Highlight 1: row 35, α 0.0 (sparse only), columns C and D: 88.1% and 92.1%.**
  > **"Today's list against a pure keyword search: 88% the same at top 10, 92% at top 100, and 95.6% at top 50. Today's hybrid is basically keyword search at the top."**
- **Highlight 2: row 38, α 0.5, columns C and E: 55.2% and 76.3%.**
  > **"At 0.5: only 55% of today's top 10 stays, so 4 to 5 of the top 10 products change, and those are the dense matches coming in. But 76% of the top 500 stays, so most of the pool your ranker sees is unchanged."**
- **Highlight 3: row 39, α 0.7, column C: 13.4%.**
  > **"At 0.7 only 13% of today's top 10 stays: practically a new top 10. That's why we say: validate with users before changing alpha."**

Closing line for the table:
> **"And the BF columns on the right show the same pattern, so this shift comes from the fusion weight itself, not from index approximation."**

Odd-looking pattern, if asked ("why is α 0.0 at 500 lower, 75.7%, than at 100, 92.1%?"):
> **"Our reading of the RRF math: deeper in today's list, positions 100 to 500, the dense leg's products do come in, because their keyword competitors run out. A keyword-only list doesn't have them, so overlap drops at 500."**

Row 42 (key-finding note) = the 95.6% (top 50) and 76.3% (top 500) you've just covered. Skip it, or point at it as the one-line summary.

👉 **Row 44 (BruteForce exactness note)**: this is what EXP1-Summary and EXP-3 mean by "checked in EXP-2".
> **"One more important check behind all our recall numbers. We computed exact scores ourselves over all 5 million records for 50 queries, with the same filters, and compared them with BruteForce. Dense is exactly right: tie-aware 1.000 at every depth, score error zero. Sparse is not fully exact: 0.916, 0.857 and 0.748 at top 10, 100 and 500, and for 5 queries BruteForce returned fewer than 500 results. So sparse and hybrid recall mean agreement with Vertex BruteForce. That's why we also use your judged products, which don't depend on BruteForce. It's also worth flagging to the Vertex team."**

👉 **What we tested (rows 47–49)**: one line:
> **"Seven alphas from 0.0, pure sparse, to 1.0, pure dense, with today's 0.3 in between, all on the same 2,000 golden queries."**

👉 **Key insights (rows 52–57)**: one line each:
- **Latency overhead (row 53):** "Single leg 15–18 ms P50; two-leg RRF adds 6–8 ms, **measured at α 0.3**." (This row is correct, so use it to back up the 'not load-tested' point.)
- **Sparse leg recall (row 54):** "Sparse is the weak leg (R@100 0.544 vs dense 0.856), which drags down α ≤ 0.3."
- **Deep recall gains (row 55):** "R@100 0.553 → 0.714 → 0.822 for α 0.3 → 0.5 → 0.7."
- **Relevance quality (row 56):** "Hit@100 55.6% → 69.3% at α 0.5; Hit@500 peaks at 80.3% at α 0.7."
- **List sensitivity (row 57):** "Today is 95.6% keyword-only at top 50; α 0.5 keeps 76% of the top-500 pool."

**BruteForce exactness** is in row 44 of the EXP-2 tab (see section 3), and the full detail is in the glossary in section 1.

**Transition:**
> **"So the mix is one lever. The other is how hard the index searches: that's EXP-3."**

---

## 4. "How exactly did you test it?" (code to quote)

Script: `05_exp2_hybrid_rrf_eval.py` (audited GO twice).
- Read-only against Vertex and GCS.
- Stages: `check → queries → sweep → latency → exact → quality → report`.

### 4.1 The 9 request types (lines 68, 84–86)
```python
ALPHAS = [0.0, 0.1, 0.3, 0.5, 0.7, 0.9, 1.0]
MODES = {"dense":  dict(dense=True,  sparse=False, alpha=None),
         "sparse": dict(dense=False, sparse=True,  alpha=None)}
MODES.update({f"hybrid_a{a:.1f}": dict(dense=True, sparse=True, alpha=a) for a in ALPHAS})
```

### 4.2 The request (`build_request`, lines 187–206)
```python
if m["dense"]:  dp.feature_vector = q["dense"]
if m["sparse"] and q["sparse_dims"]:
    dp.sparse_embedding = SparseEmbedding(q["sparse_dims"], q["sparse_vals"])
dp.restricts = [instock, status, catalog_id deny, retailer_domain deny, country]   # always on
query = Query(datapoint=dp, neighbor_count=500)
if m["alpha"] is not None:
    query.rrf = Query.RRF(alpha=m["alpha"])        # this type's alpha
```
- The same request goes to LARGE and to BruteForce, so each α is scored against BruteForce at the same α.
- Volume: 2,000 × 9 types × 2 indexes = **36,000 calls**, 0 errors.

### 4.3 Latency types (line 96)
```python
LATENCY_MODES = ["dense", "sparse", "hybrid_a0.3", "hybrid_a0.0", "hybrid_a1.0"]
DEFAULT_QPS = "81,565";  DEFAULT_DURATION_SEC = 180   # + 20 s warm-up
```
- Same open-loop harness as EXP-1: 1 client process at 81 QPS, 6 at 565, shared start time; client validity checked.

### 4.4 Overlap with α 0.3 (`stage_report`)
```python
overlap@K = len(set(ids_alpha[:K]) & set(ids_alpha03[:K])) / K     # per query, then mean
```

### 4.5 BruteForce exactness (`stage_exact` / `_exact_worker`, lines 642–890)
```python
# 6 processes, one GCS data file each (100 files, 5,000,000 records):
dense_scores  = docs_dense  @ queries_dense.T          # float32 exact dot products
sparse_scores = docs_sparse @ queries_sparse           # scipy CSR, exact
# apply the same filters (allow-list needs a matching token; deny excludes holders)
# keep the true top 500 per query, then compare with BruteForce's list:
tie_aware@K = share of BF top-K whose true score >= true K-th score (minus tolerance)
EXACT only if tie_aware >= 0.999 at every K and returned scores match
```

### 4.6 Judged hits (`stage_quality`, line 974+)
```python
eligible[q] = score-3 products of q that are in the index and pass q's filters
hit[K][q]   = len(eligible[q] & set(top_ids[q][:K])) / len(eligible[q])
macro_hit[K] = mean over queries;  paired Δ vs α 0.3 with standard error
```

---

## 5. Likely technical questions and answers

**Q: How do you know the 0.3/61 vs 0.7/140 math and the 95.6%? Did you run something?**
- **Two separate pieces of evidence, both from scripts we ran on the Workbench.**
- **(1) The fusion formula: from the EXP-1 root-cause diagnostic.**
  - The formula itself: Vertex fuses hybrid results with Reciprocal Rank Fusion, weighted by `rrf.alpha`. α 1 = dense only, 0 = sparse only, which we also confirmed: identical lists.
  - The constant 60 we checked from data, not taken on trust. For 40 golden queries we fetched the dense-only top 500, the sparse-only top 500 and the hybrid top 500 from the same index (`exp1_rca_diag.py`). Then we recomputed RRF offline with α 0.3 and constant 60 from the two leg lists (`exp1_rca_analyze.py`).
  - It reproduced Vertex's hybrid top 10 / 100 / 500 **exactly on 40 of 40 queries**. An independent audit re-ran it and confirmed (`AUDIT_REPORT_exp1_rca_verification_2026-10-08.md`).
  - The same audit counted **0.0% dense-only products in the hybrid top 10 and 0.1% in the top 50**.
  - The 0.0049 vs 0.0050 is that formula worked out by hand: 0.3/61 = 0.004918; 0.7/140 = 0.005000. The exact break-even is sparse rank 82 (0.7/142 = 0.004930).
- **(2) The 95.6%: measured in EXP-2.**
  - Script `05_exp2_hybrid_rrf_eval.py`, run `exp2_golden_20261012`.
  - The `sweep` stage sent each of the 2,000 golden queries at α 0.0, 0.1, 0.3, 0.5, 0.7, 0.9, 1.0, plus dense-only and sparse-only, to both indexes: 36,000 calls, all result lists saved.
  - The `report` stage then compares, per query, the top 50 at each α with the top 50 at α 0.3 and averages:
```python
overlap@50 = len(set(ids_alpha00[:50]) & set(ids_alpha03[:50])) / 50   # per query
mean over 2,000 queries = 0.956     # vs dense-only (α 1.0): 0.062
```
- **One line:** "The formula we verified by recomputing Vertex's hybrid output offline: an exact match on 40 of 40 queries. The 95.6% we measured directly: 2,000 queries, each sent at alpha 0.3 and at alpha 0, and we compared the two top-50 lists."

**Q: Glance is already 100% hybrid. Why test alpha?**
- "Hybrid means both legs run; alpha is how they're weighted. You asked whether the weighting 'leaves quality on the table'. At 0.3 the top results are almost pure keyword, and 0.5–0.7 finds more of your judged relevant products."

**Q: Higher recall at higher alpha: isn't that just because dense is easier for the index?**
- "Partly. Recall is agreement with BruteForce at the same alpha, and the dense leg is searched more accurately. That's why we also used your judged products, which improve too, even on exact BruteForce (68.2% → 81.6% Hit@100)."

**Q: Did alpha 0.5 / 0.7 cost more latency?**
- "Not measured. We load-tested 0.3, 0, 1, dense and sparse. We expect 0.5 and 0.7 to equal 0.3 because both legs run, and we'll confirm in a short test."

**Q: Why do alpha 0 and 1 cost the same as one search?**
- "Their results are identical to a single-leg search, and so is the latency. Vertex appears to skip the zero-weight leg. Inferred, not documented."

**Q: Why does R@5 dip at alpha 0.5 (0.613 vs 0.661)?**
- "At the very top, balanced fusion is sensitive to small differences between the fast index's leg lists and BruteForce's. From K = 20 the gain is clear, and judged hits improve at every K."

**Q: Is BruteForce the truth?**
- "Dense yes, exact against all 5M records. Sparse no: 0.748 tie-aware at 500, and sometimes fewer than 500 results. Worth flagging to the Vertex team."

**Q: Is the judged data biased?**
- "Yes, towards today's alpha: judgements exist only for products production showed, and only about 9% of judged products are in our 5M sample. So the gains at 0.5/0.7 are conservative. Use them for direction."

**Q: Which alpha do you recommend?**
- "0.5 for the best top-100, 0.7 for the best top-500 pool for your ranker. Both change the top 10 a lot, so validate with users first, and confirm at 10M."

**Q: Why three different P99s for today's request (EXP-1 50.2 ms, EXP-2 36.9 ms, EXP-3 40.1 ms)?**
- "Separate runs on different days (8, 12 and 13 Oct), same harness, same 2 replicas. P99 is the slowest 1%, so it moves run to run. Read it as about 37–50 ms at your peak, well under 100 ms."

**Q: Does this hold on 9 shards?**
- "Fusion happens after both legs, so the weighting effect should carry over. Recall and latency must be confirmed on the 10M (2-shard) index."

**Q: Why these alphas and not 0.4 / 0.6?**
- "A 0.2-step grid from today's 0.3, plus both ends for the consistency checks. A finer 0.4–0.7 sweep is a cheap follow-up."

**If you don't know:** "Good question. Let me check the raw data and come back today."

---

## 6. Don't say

- ❌ "α 0.5 has identical / zero extra latency": say "expected similar, not load-tested".
- ❌ "Vertex runs the legs in parallel": not documented.
- ❌ "1.16M calls": say "over half a million load-test requests".
- ❌ "Pure dense 67% at top 100": it's 55.9% at top 100, 66.8% at top 500.
- ❌ "Higher recall = better results" on its own: use the judged hits.
- ❌ Calling α 0.3 "legacy": it's Glance's current production setting.
