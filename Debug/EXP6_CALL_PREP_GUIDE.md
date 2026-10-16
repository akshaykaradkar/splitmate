# EXP-6 Call Prep Guide (findings session, presenting from the spreadsheet)

You'll **present the EXP-6 tab of the spreadsheet**. This guide follows that tab row by row, with the same numbers. It also explains **how we shrank the vectors (PCA)**, **how we know 768 keeps 99.998% of the information**, and **how the code works**. Behind it:
- the findings document `docs/EXP6_RESULTS_ANALYSIS_exp6_pca768_20261014.md`;
- the scripts `07_exp6_scale_down_vectors.py` (data) and `08_exp6_reduced_dim_eval.py` (evaluation), both audited GO;
- the independent integrity audit `docs/audits/AUDIT_REPORT_exp6_results_integrity.md`, which recomputed every number from raw data.

---

## Part 1. The picture to keep in your head

- **Each product is a list of 1024 numbers** (the CLIP "dense" vector) that describes what it looks like and means.
- **EXP-6 asks:** if we describe each product with **768 numbers instead of 1024**, do searches still find the same products? And does it get faster or cheaper?
- **Smaller vectors mean** less data to store and compare. That can mean faster searches, smaller indexes and, at scale, fewer machines.
- **What stays the same:** the keyword (sparse) vectors, the filters, the request (alpha 0.3, 500 results), the index settings (LARGE, leaf 7000, 5%, approx 500) and the 2,000 golden queries. **Only the dense vector size changes.**

### EXP-6's aim (plan)
> **Dimensionality Reduction, 5M:** 1024d → 768d (/512d): **latency vs recall against exact 1024-d results.** *(512-d was dropped from scope; 768 only.)*

---

## Part 2. How we shrank the vectors (PCA), in plain words

### The idea: find the directions that matter
- Think of 5 million products as points in a **1024-direction space**.
- **They don't use all 1024 directions equally.** Almost all the variation between products lies in fewer directions; the rest is close to empty.
- **PCA (Principal Component Analysis)** finds those directions and **ranks them by importance**. Keeping the top 768 keeps almost everything.
- **Analogy:** photographing a 3-D object from the best angle. If the object is nearly flat, a 2-D photo from the right side loses almost nothing. PCA finds the "right side", in 1024 dimensions.

### What we did, step by step
| Step | What happens | Why |
|---|---|---|
| 1. **Sample** | Take **200,000** products: 2,000 from each of the 100 data files, picked by a fixed hash of the product id, servable (active and in stock) only | Enough to learn the directions, and repeatable (same sample every run) |
| 2. **Learn the directions (fit)** | Compute how the 200,000 vectors spread across the 1024 directions, then rank the directions | This is the PCA "model": a 1024 × 1024 table of directions plus their importance |
| 3. **Measure what 768 keeps** | Share of the total "spread" held by the top 768 directions → **0.99998** | Tells us before building anything whether 768 is enough |
| 4. **Shrink every product** | For all **5,000,000** products: keep the top 768 directions, then **re-normalise to length 1** | Same shape as the original (unit length), so the index's dot-product scoring works as before |
| 5. **Shrink the queries the same way** | The 2,000 golden queries go through the **same frozen model** | Queries and products must live in the same 768-d space |
| 6. **Keep everything else identical** | Only the `embedding` numbers in each record are replaced. Sparse vectors, filters, crowding tags and ids stay **byte-for-byte unchanged** | So any change in results comes only from the 768-d vectors |
| 7. **Verify** | All 5M records: same ids and order, 768 numbers each, length 1, every other byte unchanged, recomputed vectors match | Proves the data step did exactly what it should |

### Why PCA and not just "cut off the last 256 numbers"?
- **Cutting off numbers ("truncation")** only works for models trained to put the most important information first (so-called Matryoshka models). CLIP isn't trained that way, so its information is spread over all 1024 numbers.
- **PCA rotates first, then keeps the top 768.** It's the mathematically best linear way to keep the most information with 768 numbers.

### Why "uncentered" PCA?
- Standard PCA first subtracts the average vector. We **didn't**, on purpose.
- **Vertex scores products by dot product.** Subtracting an average would change those scores in a different way for each product, and so change the ranking.
- Uncentered PCA keeps the **dot products themselves** as close as possible, which is what search ranking depends on.

---

## Part 3. How we know 768 keeps 99.998% of the information

**Short answer:** the PCA fit measures it directly. Each direction comes with an **importance number** (its "eigenvalue"): how much of the products' total spread lies along that direction. Sum the importance of the top 768 and divide by the total:

> **explained variance (768) = (importance of the top 768 directions) ÷ (importance of all 1024) = 0.99998**

Our measured values (Phase A fit, 13 Oct):

| Numbers kept | Information kept (explained variance) |
|---|---|
| 64 | 62.2% |
| 128 | 73.0% |
| 256 | 85.0% |
| 512 | 97.5% |
| **768** | **99.998%** |

**What 99.998% means in practice:**
- Because the vectors are unit length, it also means that after shrinking, a product vector keeps on average **99.998% of its length**: only 0.002% is lost.
- Geometrically, the shrunk vector points almost exactly the same way as the original: on average **0.26 degrees or less** of difference (√0.00002 ≈ 0.0045 radians; an average over products, not a guarantee for each one).
- **Hint for the call (a hypothesis, not verified):** the last 256 directions are nearly empty. That suggests the embedding model may have a ~768-wide core, which is worth asking Glance which model produces the vectors.

**Three cautions to say out loud:**
1. **"Information kept" isn't "recall".** It was measured on **product (image) vectors**. The real test is the recall on Glance's **text** queries, which is what the EXP-6 tab shows (Part 4).
2. **The model is frozen.** The same model must be used for every query and every product update.
3. **Independent check:** the audit confirmed that query pairs keep their similarity (correlation 0.99985 between 768-d and 1024-d cosine similarities), and that one fixed linear map reproduces every shrunk query.

**The code that computes it** (`07_exp6_scale_down_vectors.py`, `fit_pca_model`, lines 745–771):
```python
x_train = np.stack(train_list).astype(np.float64)          # 200,000 x 1024 sample
matrix_to_fit = x_train                                     # uncentered (no mean subtracted)
cov = (matrix_to_fit.T @ matrix_to_fit) / x_train.shape[0]  # 1024 x 1024 "spread" matrix
eig_vals, eig_vecs = np.linalg.eigh(cov)                    # directions + their importance
chosen_comp = eig_vecs[:, ::-1].T                           # directions, most important first
chosen_vals = eig_vals[::-1]                                # importance, largest first
explained = {d: sum(chosen_vals[:d]) / sum(chosen_vals) for d in (64, 128, 256, 512, 768)}
```
- The directions, importances and sample ids are saved with checksums in `pca_manifest.json`. Its fingerprint `7bb912d1…` is recorded in both the product files and the query files, which proves they used the same model.

---

## Part 4. Presenting the EXP-6 tab, section by section

### Row 1: context (italic)
*"2,000 golden queries (shrunk to 768-d with the same model) · test index = Glance's production config on 5M products at 768-d (1 shard, 2 replicas) · ground truth = 1024-d BruteForce answers (same as EXP-3) · request = Glance production."*
- **Say:** "Same setup and same answer key as EXP-1 and EXP-3. Only the dense vectors are smaller."

### Section 1, What We Did (rows 2–6)
- PCA, uncentered, fitted on 200,000 products; re-normalised; sparse, filters and crowding unchanged (sparse results identical to 1024-d for all 2,000 queries).
- 768 keeps **99.998%** (512 would keep 97.5%).
- The same model shrinks the queries; all 5M products verified; the index is identical except for the dimension.

### Section 2, Recall: 1024-d vs 768-d (rows 8–17) ⭐ the quality table
**Δ = 768 − 1024, paired per query.** Colour key (row 18):
- 🟩 green = 768-d better;
- 🟨 yellow = about the same (|Δ| < 0.005);
- 🟥 red = 768-d worse.

| Setting | Mode | R@50 1024 | R@50 768 | Δ R@50 | R@100 1024 | R@100 768 | Δ R@100 | R@500 1024 | R@500 768 | Δ R@500 |
|---|---|---|---|---|---|---|---|---|---|---|
| A500_Funset (today) | Hybrid | 0.5911 | 0.5900 | 🟨 −0.0012 | 0.5534 | 0.5527 | 🟨 −0.0007 | 0.5181 | 0.5053 | 🟥 −0.0128 |
| A1000_F0.02 | Hybrid | 0.6726 | 0.6722 | 🟨 −0.0004 | 0.6361 | 0.6356 | 🟨 −0.0005 | 0.5829 | 0.5807 | 🟨 −0.0022 |
| A2000_F0.05 | Hybrid | 0.7529 | 0.7528 | 🟨 −0.0001 | 0.7276 | 0.7276 | 🟨 0.0000 | 0.6958 | 0.6948 | 🟨 −0.0010 |
| A5000_F0.05 (best balance) | Hybrid | 0.8461 | 0.8462 | 🟨 +0.0002 | 0.8289 | 0.8292 | 🟨 +0.0002 | 0.8023 | 0.8022 | 🟨 −0.0001 |
| A5000_F0.20 | Hybrid | 0.8525 | 0.8527 | 🟨 +0.0002 | 0.8377 | 0.8371 | 🟨 −0.0006 | 0.8255 | 0.8253 | 🟨 −0.0001 |
| A500_Funset (today) | Dense only | 0.8720 | 0.8451 | 🟥 −0.0269 | 0.8559 | 0.8108 | 🟥 −0.0451 | 0.6596 | 0.5664 | 🟥 −0.0933 |
| A5000_F1.00 (search everything) | Dense only | 0.9982 | 0.9961 | 🟨 −0.0021 | 0.9972 | 0.9949 | 🟨 −0.0023 | 0.9909 | 0.9853 | 🟥 −0.0056 |

(Setting codes: A = candidates, F = share of the index searched; `Funset` = not overridden = today. See the EXP-3 guide.)

**Walk it top to bottom:**
1. **Today's request, hybrid:** almost no change at R@50 and R@100. A small loss at depth 500 (−0.013, the only red hybrid cell).
2. **Better settings** (A2000 and up): 768 **matches** 1024 (differences of 0.0000–0.0002).
3. **Dense-only at today's default:** the one real loss, growing with depth (−0.027 / −0.045 / −0.093).
   - **Why:** today's shortlist (500) is too small, and at 768-d slightly more true neighbours fall off it before the final ranking.
   - With a bigger shortlist the gap disappears (A5000_F0.05 dense R@100: 0.8717 vs 0.8715).
4. **Search everything:** the most shrinking alone can cost is **0.2–0.6 points**. That's an upper bound on the PCA-only loss.
- **Why hybrid barely moves:** the keyword side (70% of the weight) is unchanged, so 94.8% of the hybrid results are the same as at 1024-d. The changes sit at ranks 401–500.

### Section 3, Latency and Capacity: 1024-d vs 768-d (rows 19–26)
Colours: 🟩 768-d faster or holds more, 🟥 768-d slower or fails, no colour = same or not run.

| Setting | Single request ms 1024 / 768 | P99 @565 QPS 1024 / 768 | P99 @800 QPS 1024 / 768 | Max QPS 1024 / 768 |
|---|---|---|---|---|
| A500_Funset (today) | 28.46 / **23.49** 🟩 | 40.1 / **32.1** 🟩 | 63.1 / **33.4** 🟩 | ≥800 / ≥800 |
| A1000_F0.02 | 25.95 / **22.48** 🟩 | 34.4 / **32.6** 🟩 | 38.2 / **30.5** 🟩 | ≥800 / ≥800 |
| A2000_F0.05 | 30.31 / **25.85** 🟩 | 41.3 / **34.4** 🟩 | 161.3 (fails) / **38.9** 🟩 | 565 / **≥800** 🟩 |
| A5000_F0.05 (best balance) | 36.22 / **30.81** 🟩 | 55.7 / **46.9** 🟩 | 505.0 (fails) / **57.2** 🟩 | 565 / **≥800** 🟩 |
| A5000_F0.20 | 48.20 / **39.20** 🟩 | not run (failed at 400) / 891.7 (fails) 🟥 | not run / not run | 200 / **400** 🟩 |

**New columns J–L: "768-d faster by" (%)**
- Formula: **(1024-d − 768-d) ÷ 1024-d**. Higher = bigger improvement. Example A500_Funset single request: (28.46 − 23.49) ÷ 28.46 = **17.5%**.

| Setting | Single request | P99 @565 QPS | P99 @800 QPS |
|---|---|---|---|
| A500_Funset (today) | **17.5%** | **20.0%** | **47.1%** |
| A1000_F0.02 | 13.4% | 5.2% | 20.2% |
| A2000_F0.05 | 14.7% | 16.7% | n/a (1024-d failed, 768-d held) |
| A5000_F0.05 (best balance) | 14.9% | 15.8% | n/a (1024-d failed, 768-d held) |
| A5000_F0.20 | 18.7% | n/a (both fail) | not run |

- **Why "n/a" where 1024-d failed:** a failed run is overload, not a slow answer, so a % would be misleading. The honest statement is "1024-d failed at 800 QPS, 768-d held it".
- **Be careful with the 47.1%:** 1024-d P99 at 800 QPS (63.1 ms) was one run on another day; at 565 QPS the gain is 20%. Quote it as "up to about 20% at the production peak", not "47% faster".
- **Single requests about 5 ms faster** (13–19% across the 5 sheet settings; up to 23% at the diagnostic "search everything" setting A5000_F1.00, which isn't on the sheet): fewer numbers to compare per product.
- **More headroom:** the best-balance setting **A5000_F0.05 held 800 QPS at 768-d** (P99 57 ms), where 1024-d failed at 800.
- **A5000_F0.20 still can't hold the 565 QPS peak** on 2 replicas.
- ⚠️ **Indicative:** the 1024-d numbers come from EXP-3, a different day and deployment. Recall is compared like for like; latency isn't.

### Insights (rows 28–34): one line each
| Row | Say it like this |
|---|---|
| 29 | "Hybrid quality at today's request is almost unchanged at R@50 and R@100, with a small loss at 500." |
| 30 | "The meaning-only search loses more at today's small shortlist; with 2,000+ candidates the gap almost disappears." |
| 31 | "Shrinking alone costs very little: searching everything, dense recall is 0.996 / 0.995 / 0.985 vs 0.998 / 0.997 / 0.991." |
| 32 | "Single requests about 5 ms faster (13–19%, columns J–L); the balanced setting held 800 QPS at 768-d, where 1024-d failed." |
| 33 | "A5000_F0.20 still can't hold your 565 peak on 2 replicas." |
| 34 | "Judged relevant products in the top 500 unchanged within noise: 76.6% vs 76.1% today, 86.3% vs 85.3% at A5000_F0.05." |

### Section 4, Conclusion (rows 36–38)
- ✅ **Can claim:** at today's request, 768-d finds almost the same products (small loss at 500). With more candidates it matches 1024-d. It was faster, with more headroom under load (indicative).
- ❌ **Cannot claim:** cost, memory or node savings (not measured; still 1 shard); "no recall loss" at today's request; numbers for the 9-shard production index.

### Caveats (rows 40–44)
- 1 shard at 5M; confirm at 10M.
- Recall is like for like; latency is across days (indicative).
- **The same frozen PCA model must be applied to every query and every product update.** A refit means re-projecting everything and rebuilding.
- Sparse/hybrid recall is vs BruteForce (sparse approximate). Every number was independently recomputed.

---

## Part 5. "How did you do it?" (the code in plain words)

### Script 1: `07_exp6_scale_down_vectors.py` (the data step), run on the Workbench
| Stage | What it does | Code |
|---|---|---|
| **fit** | Samples 200,000 products, learns the PCA directions, saves the model with checksums, prints the explained variance | `fit_pca_model` (lines 673–813) |
| **queries** | Shrinks the 2,000 golden queries with the same model and writes a provenance file (source sha, model sha, 768) | `project_queries` (line 1271) |
| **project** | Streams all 100 files (5M products) from the bucket, shrinks each vector, writes 100 new files to `embedding-datasets-pca768/` | `_transform_single_file` (line 841) |
| **verify** | Re-reads all 5M records and checks them line by line against the originals | `_verify_single_file` (line 1151) |

**The shrink itself** (`project_vectors`, lines 617–631):
```python
projected = x @ components[:768].T                       # keep the top 768 directions
norms = np.linalg.norm(projected, axis=-1, keepdims=True)
return projected / norms                                 # re-normalise to length 1
```
**Only the embedding numbers are replaced** (`splice_vector_bytes`, lines 606–614): the script finds the `"embedding": [...]` part of each record and swaps in the 768 numbers. Every other byte (id, sparse, filters, crowding tag) is copied as is.

**Safety built in** (the audit found and fixed issues before the run):
- dry run by default;
- never overwrites the source or an existing file;
- a crash leaves no half-written files;
- resumable;
- one model per run (fingerprint recorded);
- verify checks every line.

### Script 2: `09` / `10`: build and deploy the 768 index
- The same LARGE settings as the 1024-d index, only `dimensions: 768` and the new data folder.
- Built with STREAM_UPDATE: 3 h 41 m; 5,000,000 dense + 5,000,000 sparse vectors, 1 shard.
- Deployed on the same machines as EXP-3 (e2-highmem-16, 2–3 replicas), then **undeployed after the test** (the index is kept).

### Script 3: `08_exp6_reduced_dim_eval.py`, the evaluation (a copy of the audited EXP-3 script)
**Key idea:** the 768 index gets the **shrunk** query; BruteForce gets the **original 1024-d** query. So recall answers "how close is the 768 index to the exact 1024-d answer?"

```python
def tree_request(q, mode, setting):                          # line 457
    if len(q["dense"]) != TEST_DENSE_DIM:                    # guard: the 768 index must get a 768-d query
        raise GuardError("send the projected query file to the reduced index")
    req = build_request(q, mode, DEPLOYED_TEST)              # the EXP-1 production request
    # + approx / leaf-fraction overrides for this setting (same as EXP-3)
```
- **BruteForce answers are reused from EXP-3** (the same 1024-d answer key the 1024-d results were scored against). They were checked first: same queries, same request, and a live re-check of 50 queries × 3 modes against the deployed BruteForce.
- **The check stage refuses to run** unless the live 768 index and deployment match the expected settings (768, LARGE, 1 shard, e2-highmem-16 2/3, 5M/5M), and the queries were shrunk with the **same model** as the products (`7bb912d1…`).
- **Same stages as EXP-3:** recall (36,000 calls, 0 errors) → timing → quality → shortlist → load curves (81–800 QPS) → report. The report also compares each number with EXP-3's 1024-d results, query by query.

**One line for the call:** "We learned the 768 most important directions from 200,000 products (PCA), shrank all 5 million products and the queries with that same frozen model, and built an identical index at 768-d. Then we ran exactly the EXP-3 tests against the same 1024-d answer key."

---

## Part 6. Likely questions and short answers

| Question | Answer |
|---|---|
| "How did you reduce the dimensions?" | "PCA: we found the 768 directions that hold almost all the variation between products, using 200,000 products, and projected every product and query onto them, then re-normalised." |
| "How do you know 768 keeps 99.998%?" | "The PCA fit measures it: the top 768 directions hold 99.998% of the total variance. On average a product vector keeps 99.998% of its length, about a quarter of a degree of difference. And the recall tests on real queries confirm it." |
| "Why not just drop the last 256 numbers?" | "CLIP doesn't put its most important information first, so cutting would lose information. PCA rotates first, then keeps the most important 768." |
| "Why uncentered PCA?" | "Search ranks by dot product. Uncentered PCA preserves dot products best; centering would change scores product by product." |
| "Is 99.998% the same as recall?" | "No. It's measured on product vectors. Recall on your text queries is the real test: about the same as 1024-d, with a small loss only at depth 500 at today's settings." |
| "Why does dense-only lose 9 points at R@500?" | "At today's small shortlist of 500, slightly more true neighbours drop off before the final ranking at 768-d. With 2,000+ candidates the gap disappears." |
| "Does it save money?" | "Not measured. At 5M both indexes are 1 shard. The data is smaller (roughly 20% fewer bytes per product, an estimate), so at 50M it could mean fewer shards or cheaper builds, but we haven't measured that." |
| "What would we have to change in production?" | "Apply the same frozen PCA model to every query and every product update, and rebuild the index at 768-d. A model refit means re-projecting everything." |
| "Text queries vs image products?" | "The PCA was learned on product vectors and applied to text queries. That's why we measured recall on your real text queries, and they behave the same." |
| "Why 768 and not 512?" | "Scope decision: 768 only. 512 keeps 97.5% of the variance; it would need its own test." |
| "Did you rebuild the index?" | "Yes, a new 768-d index from the shrunk data: 3 h 41 m, same settings. It's undeployed now but kept." |
| "Why is P99 at 81 QPS higher than at 800?" | "A small constant trickle of slow requests (1–2 per second, cause unverified) matters more at low traffic. Compare P95, or P99 at ≥200 QPS." |
| "How do we know the numbers are right?" | "An independent audit recomputed every number from the raw results (126 recall values, 54 paired comparisons, latency, quality): all reproduce." |

---

## Part 7. Consistency with the spreadsheet, and what not to claim

**The guide and the sheet use identical numbers.** The EXP-6 tab was filled from the audited scorecard. R@50 was recomputed from the raw data and matches both scorecards.

**What not to claim:**
- ❌ Cost, memory or node savings (not measured).
- ❌ "No recall loss" at today's request (−0.013 at R@500; dense-only −0.09).
- ❌ "Higher max QPS" where both runs show "≥800" (both hit the test cap).
- ❌ That A5000_F0.20 is ready for peak traffic.
- ❌ Numbers for the 9-shard production index.

---

## Part 8. Three sentences to open with
1. "EXP-6 tested whether Glance's product vectors can be shrunk from 1024 to 768 numbers. We used PCA, which keeps 99.998% of the information, and applied the same model to the queries."
2. "On your 2,000 real queries, search quality is essentially unchanged: hybrid R@50 and R@100 are the same, with a small loss only at depth 500 at today's settings. With more candidates, 768 matches 1024."
3. "Searches were about 5 ms faster and the balanced setting held 800 searches per second. Cost savings weren't measured, and production would need the same shrink model on every query and update."
