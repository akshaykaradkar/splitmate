# EXP-6 Script: Dimensionality Reduction 1024 → 768 (5M)

*Read on your phone during the call. **Bold** = what to say. "👉" = where to point on the screen.*
*Order on screen: **EXP6-Summary** tab → **EXP-6** tab.*

---

## 0. The 20-second cheat sheet

- **Question:** if each product's dense vector has 768 numbers instead of 1,024, is search faster, and do we lose quality?
- **4 numbers to remember:**
  - **99.998%**: variance (information) kept by PCA at 768.
  - **0.553 vs 0.553**: hybrid Recall@100 at the production request (R@500: 0.505 vs 0.518).
  - **13–19%**: faster single requests (23.5 vs 28.5 ms at today's request).
  - **≥800 vs 565 QPS**: capacity of the best-balance setting (5,000 candidates), 768-d vs 1024-d.
- **One-sentence story:** "Shrinking to 768 keeps essentially all the information, finds the same products at today's request, and makes every search 13–19% faster. The cost saving can't show at 5M because both indexes are one shard."
- **No "Keep in mind" on this summary tab**, so say the caveats out loud (section 2, last block).

---

## 1. Mini-glossary: the terms in this experiment

**Dimension**: the number of values in a dense vector. Glance's CLIP vectors have 1,024.

**PCA (Principal Component Analysis)**
- Finds the directions along which the products differ most, ranked by importance.
- Keeping the top 768 directions keeps almost everything.
- Analogy: photographing a nearly flat object from the best side. A 2-D photo loses almost nothing.

**How 99.998% is computed (explained variance)**
- Each PCA direction has an importance number (eigenvalue) = how much of the products' total spread lies along it.
```text
explained_variance(768) = sum of the top-768 eigenvalues / sum of all 1,024 eigenvalues = 0.99998
```
- Measured values: 64 → 62.2%, 128 → 73.0%, 256 → 85.0%, 512 → 97.5%, **768 → 99.998%**.
- Meaning: a unit-length product vector keeps on average 99.998% of its length after shrinking. Its direction changes by about 0.26° or less on average (√0.00002 ≈ 0.0045 rad).

**Uncentered PCA**
- We did **not** subtract the average vector before PCA.
- Vertex ranks by **dot product**. Uncentered PCA preserves dot products best; centering would shift each product's score differently.

**Re-normalise**
- After projecting to 768, every vector is scaled back to length 1.
- So dot product still equals cosine, and the index settings (DOT_PRODUCT_DISTANCE, norm NONE) stay valid.

**Why not just drop the last 256 numbers (truncation)?**
- That only works for "Matryoshka" models trained to put important information first. CLIP isn't, so its information is spread over all 1,024 numbers.
- PCA rotates first, then keeps the top 768.

**Recall@K vs the 1024-d answer key**
```text
Recall@K = | 768-d index top-K ∩ 1024-d BruteForce top-K | / K      (mean over 2,000 queries)
```
- The 768 index gets the **shrunk** query; BruteForce gets the **original 1024-d** query.
- So recall answers: "how close is the 768 index to the exact 1024-d answer?"

**Δ (delta), paired per query**
- Δ = (768-d recall − 1024-d recall), computed for each query, then averaged.
- It's more precise than subtracting the two averages, which is why a Δ can differ by 0.0001 from subtracting the rounded cells.

**Colours on the EXP-6 tab**
- Recall Δ: 🟨 yellow = about the same (|Δ| < 0.005), 🟥 red = 768-d worse, 🟩 green = 768-d better.
- Latency: 🟩 green = more than 5% faster or holds more QPS; 🟥 red = slower or fails.

**Faster by %**
```text
faster_by = (1024-d time − 768-d time) / 1024-d time
```
- Example, today's request: (28.46 − 23.49) / 28.46 = **17.5%**.

**Single request / P99 / max QPS**: same definitions as EXP-3.
- Single request = median of one-at-a-time requests.
- P99 = 99% of searches faster than this, under open-loop load.
- Max QPS = highest rate where it and every lower rate held P99 ≤ 100 ms and errors < 0.1%, on 2 replicas. "≥800" = limit not reached at the 800 cap.

---

## 2. SCRIPT: screen 1, the EXP6-Summary tab

👉 **Rows 1–2 (plan row)**
> **"EXP-6 is dimensionality reduction. Plan aim: compress 1024d to 768d or 512d and benchmark latency vs recall, with the main interest in cost and latency, traded off against Recall@K. We tested 768-d on 5M; 512 was dropped from scope."**

👉 **Row 3 (yellow key finding)**
> **"In one line: PCA from 1024 to 768 keeps 99.998% of the variance, gives the same hybrid Recall@100 at your production request, and makes single requests 13 to 19% faster. Cost saving can't be measured at 5M."**

👉 **Row 6 (99.998%)**
> **"First, how much information do 768 numbers keep? We fitted PCA on 200,000 of your products. The top 768 directions hold 99.998% of the variance; for reference, 512 would keep 97.5%. So before building anything, we knew almost nothing is lost."**

👉 **Row 7 (0.553 vs 0.553)**
> **"Then the real test on your 2,000 text queries, against the exact 1024-d BruteForce answers. Hybrid Recall@100 is 0.553 at 768-d and 0.553 at 1024-d. The only loss is deep in the list: R@500 0.505 vs 0.518."**

👉 **Row 8 (23.5 vs 28.5 ms)**
> **"Speed: a single search at today's request takes 23.5 ms instead of 28.5. Across the five settings we tested, it's 13 to 19% faster, because there are fewer numbers to compare per product."**

👉 **Row 9 (P99 32.1 / 46.9 vs 40.1 / 55.7)**
> **"Under your 565 QPS peak: P99 32 ms instead of 40 at today's settings, and 47 instead of 56 at the 5,000-candidate setting. That's 16 to 20% lower."**

👉 **Row 10 (≥800 vs 565)**
> **"And headroom: with 5,000 candidates, the 1024-d index maxed out at 565 QPS, while 768-d held 800 without reaching its limit. So the higher-recall setting from EXP-3 becomes cheaper to run at 768."**

👉 **Row 11 (cost: not measured)**
> **"Cost: not measured. At 5M both indexes fit in one shard, so the node count is the same. Savings in memory or shard count would only show at larger scale, 10M and up."**

👉 **Caveats to say out loud** (no "Keep in mind" on this tab):
> **"Two things to keep in mind. The 1024-d latency comes from the EXP-3 run on a different day, so the speed comparison is indicative; recall is compared query by query. And to use this in production, the same frozen PCA model has to be applied to every query and every product update."**

👉 **Row 13 (link)** → click to the EXP-6 tab.

---

## 3. SCRIPT: screen 2, the EXP-6 detailed tab

👉 **Section 1: What we did (rows 2–6)**
> **"Uncentered PCA fitted on 200,000 products. Every vector is shrunk from 1024 to 768 and re-normalised to length 1. Sparse vectors and filters are untouched, and sparse results were identical to 1024-d for all 2,000 queries. The same model shrinks the queries. All 5 million products were verified. And the 768 index has the same settings as the 1024 test index; only the dimension differs."**

👉 **Section 2: Recall 1024 vs 768 (rows 8–17)**
> **"Each row shows R@50, R@100 and R@500 at 1024 and 768, and the paired difference. Yellow means about the same, red means worse."**

Walk it top to bottom:
1. **Today's request (A500_Funset), hybrid:** Δ −0.001 at R@50 and R@100, both yellow. R@500 **−0.013**, red: the only red hybrid cell.
2. **A1000_F0.02, A2000_F0.05:** all yellow (−0.0022 to 0.0000).
3. **A5000_F0.05 (best balance), A5000_F0.20:** all yellow, Δ about 0.0002. 768 **matches** 1024.
4. **Dense-only at today's default:** **−0.027 / −0.045 / −0.093**, red. That's the real loss.
5. **Dense-only, search everything (A5000_F1.00):** only −0.002 / −0.002 / −0.006.

> **"Two key points. First, the dense-only loss at today's default comes from the small candidate pool, not from PCA: with all leaves searched the loss is at most 0.6 points. That's the upper bound on what PCA alone costs. Second, hybrid barely moves because the sparse side, with 70% weight, is unchanged."**

👉 **Section 3: Latency and capacity (rows 19–26)**
> **"Same settings, 1024 vs 768, on 2 replicas. The green cells are where 768 is faster or holds more."**
- **Single request:** 28.46 → **23.49 ms** (17.5% faster), and 13–19% across all five.
- **P99 at 565:** 40.1 → **32.1 ms** (20%) today; 55.7 → **46.9 ms** (15.8%) best balance.
- **P99 at 800:** at A2000 and A5000_F0.05, **1024-d failed and 768-d held** (38.9 and 57.2 ms). That's why those cells say "n/a" instead of a %.
- **Max QPS:** A2000 and A5000_F0.05: 565 → **≥800**; A5000_F0.20: 200 → 400.
- **A5000_F0.20 at 565:** red. It still fails at 768 (891.7 ms).

**If they point at L22 (47.1%):** "That's at 800 QPS, from one 1024-d run on another day. At the 565 peak the gain is 20%, so I'd quote up to about 20%."

👉 **Insights (rows 29–33)**: one line each:
- "Hybrid at today's request almost unchanged at R@50 / R@100; −0.013 at 500."
- "Dense-only loses more at the default because too few candidates survive; with 2,000+ candidates the gap disappears."
- "Shrinking alone costs very little: 0.996 / 0.995 / 0.985 vs 0.998 / 0.997 / 0.991 with every leaf searched."
- "About 5 ms faster per request; the balanced setting held 800 QPS at 768-d where 1024-d failed."
- "A5000_F0.20 still can't hold 565 on 2 replicas."

👉 **Caveats (rows 36–38)**
> **"1 shard at 5M; confirm on 10M. And production needs the same frozen PCA model on every query and every product update. A refit means re-projecting all vectors and rebuilding the index."**

**Close:**
> **"Summary across the three: EXP-1 found the gap, EXP-3 shows the candidate count closes most of it per request, and EXP-6 shows we can make each search cheaper without losing quality. Next we confirm all of it on the 10M two-shard index."**

---

## 4. "How exactly did you test it?" (code to quote)

**Scripts, all audited GO before running:**
- `07_exp6_scale_down_vectors.py`: data step, stages `fit → queries → project → verify`.
- `09_exp6_build_index.sh` / `10_exp6_deploy_index.sh`: build and deploy.
- `08_exp6_reduced_dim_eval.py`: evaluation, a copy of the audited EXP-3 script.

### 4.1 Fit the PCA model (`fit_pca_model`, lines 745–771)
```python
x = np.stack(sample).astype(np.float64)     # 200,000 x 1024, servable products,
                                            # 2,000 per file by a fixed hash of the id
cov = (x.T @ x) / x.shape[0]                # uncentered: no mean subtracted
eig_vals, eig_vecs = np.linalg.eigh(cov)    # directions + importance
components = eig_vecs[:, ::-1].T            # most important first
vals = eig_vals[::-1]
explained = {d: vals[:d].sum() / vals.sum() for d in (64, 128, 256, 512, 768)}
# saved with checksums -> pca_manifest.json (fingerprint 7bb912d1...)
```

### 4.2 Shrink one batch of vectors (`project_vectors`, lines 617–631)
```python
projected = x @ components[:768].T                    # 1024 -> 768
return projected / np.linalg.norm(projected, axis=-1, keepdims=True)   # back to length 1
```

### 4.3 Replace only the embedding in each record (`splice_vector_bytes`)
- Finds the `"embedding": [...]` part of each JSON line and swaps in the 768 numbers.
- id, sparse_embedding, restricts, numeric_restricts and crowding_tag are copied **byte for byte**.
- Streamed GCS → GCS over the 100 files; never overwrites the source; a crash leaves no half-written files.

### 4.4 Verify (`_verify_single_file`)
- All 5,000,000 records: same ids in the same order, 768 numbers each, unit length, every other byte unchanged.
- A sample of vectors is re-projected and compared.
- Result: VERIFY PASSED, records = 5,000,000.

### 4.5 Build and deploy
- Same metadata as the 1024-d LARGE index except `dimensions: 768` and the data folder.
- STREAM_UPDATE; built in **3 h 41 m**; 5,000,000 dense + 5,000,000 sparse; 1 shard.
- Deployed on e2-highmem-16, min 2 / max 3 (same as EXP-3), then **undeployed after the test**. The index is kept.

### 4.6 Evaluation: 768 query to the 768 index, 1024 answer key
```python
def tree_request(q, mode, setting):              # 08_exp6_reduced_dim_eval.py, line 457
    if len(q["dense"]) != 768:                   # guard: must be the projected query
        raise GuardError("send the projected query file to the reduced index")
    req = build_request(q, mode, DEPLOYED_TEST)  # same production request as EXP-1/3
    # + approx / leaf-fraction overrides of the setting (same as EXP-3)
```
- **Ground truth reused from EXP-3:** the 1024-d BruteForce answers for the original queries. It was validated first:
  - same query file (sha256);
  - same request text;
  - a live re-check of 50 queries × 3 modes.
- The `check` stage refused to start unless the live index matched (768, LARGE, leaf 7000, 5%, approx 500, STREAM_UPDATE), and the queries were shrunk with the **same model** as the products (fingerprint 7bb912d1…).
- Grid: A500_Funset, A1000_F0.02, A2000_F0.05, A5000_F0.05, A5000_F0.20 + diagnostic A5000_F1.00; 3 modes → **36,000 calls, 0 errors**.
- Latency: the same open-loop harness as EXP-1/3, at 81 / 200 / 400 / 565 / 800 QPS, 180 s each after a 20 s warm-up, with early stop.

### 4.7 The paired Δ
```python
d = [recall_768[q][K] - recall_1024[q][K] for q in queries_in_both]
delta = mean(d)          # what the Δ columns show
```

---

## 5. Likely technical questions and answers

**Q: How exactly did you reduce the dimension?**
- "Uncentered PCA on 200,000 servable products; projection onto the top 768 components; re-normalised to unit length. The same frozen matrix is applied to the queries."

**Q: How do you know 768 keeps 99.998%?**
- "From the PCA eigenvalues: the top 768 hold 99.998% of the total variance, measured on product vectors. And recall on your real text queries confirms it."

**Q: Is 99.998% the same as recall?**
- "No. It's measured on product vectors. Recall on your text queries is the real test, and it's essentially unchanged at the top 100."

**Q: The PCA was learned on image (product) vectors but the queries are text. Is that a problem?**
- "That's the modality gap risk, which is exactly why we measured recall on your 2,000 text queries. The audit also found that query-pair cosine similarities keep a 0.99985 correlation between 768-d and 1024-d."

**Q: Why does dense-only lose 9 points at R@500 at today's settings?**
- "At 500 candidates for 500 results, a few more true neighbours drop out before the exact re-ranking at 768-d. With 2,000+ candidates the gap is zero, and searching everything shows PCA alone costs at most 0.6 points."

**Q: Why is hybrid unaffected?**
- "The sparse side, with 70% weight, is unchanged (identical results for all 2,000 queries), and 94.8% of the hybrid results are the same as at 1024-d. The changes sit at ranks 401–500."

**Q: Why uncentered and not standard PCA?**
- "Dot-product ranking. Subtracting a mean changes each product's score differently; uncentered PCA preserves dot products."

**Q: Why not OPQ / quantization instead?**
- "Tree-AH already uses its own quantised scoring internally. This experiment tested reducing the input dimension itself. Other compression wasn't in scope."

**Q: What changes in production?**
- "Multiply every query embedding by the fixed 1024 × 768 matrix and re-normalise. That's tiny compute, about 0.8 million multiply-adds. Do the same for every product upsert, and rebuild the index at 768-d. A model refit means re-projecting everything."

**Q: Does it save money?**
- "Not measured. At 5M both are one shard. The vectors are 25% shorter (768 vs 1024 floats), so at your 50M scale it could mean fewer shards or nodes. That's a hypothesis to test at 10M+."

**Q: Is the latency comparison fair?**
- "Same settings, same client machine and harness, same rates, both on 2 replicas. But the 1024-d curves ran on a different day and deployment, so we call it indicative. Recall is compared query by query against the same answer key."

**Q: Why is P99 at 81 QPS higher than at 800 for today's setting (if they see it in the raw numbers)?**
- "A small constant trickle of slow requests weighs more at low traffic (cause unverified). P95, or P99 at 200 QPS and above, shows the clean picture."

**Q: Why not 512?**
- "A scope decision: 768 only. 512 keeps 97.5% of the variance; it would need its own build and test."

**Q: How long did the rebuild take?**
- "3 h 41 m for 5M at 768-d (the 1024-d LARGE took about 4 h)."

**Q: How do we know the numbers are right?**
- "An independent audit recomputed every number from the raw results: 126 recall values, 54 paired comparisons, latency and quality. All reproduce."

**If you don't know:** "Good question. Let me check it against the raw data and come back today."

---

## 6. Don't say

- ❌ Cost, memory or node savings (not measured).
- ❌ "No recall loss" at today's request (R@500 −0.013; dense-only −0.09).
- ❌ "47% faster" (that's one 800 QPS point from another day; say "up to about 20% at peak").
- ❌ That A5000_F0.20 is ready for peak traffic (it fails at 565).
- ❌ Numbers for the 9-shard production index.
