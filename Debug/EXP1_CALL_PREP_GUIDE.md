# EXP-1 Call Prep Guide (findings session)

Companion to `EXP1_RESULTS_ANALYSIS_exp1_golden_20261008.html`. Plain-language explanations, how the script works, and likely questions with answers. All numbers come from the EXP-1 scorecard, the RCA and its audit.

---

## Part 1. The picture to keep in your head

- **The warehouse:** 5 million Glance products. Each product has two "fingerprints":
  - a **dense** vector (CLIP, 1024 numbers): the *meaning* of the product;
  - a **sparse** vector (SPLADE, about 100 weighted keywords): its *important words*.
- **Two copies of the same 5M products, searched two ways:**

| Index | How it searches | Role in EXP-1 |
|---|---|---|
| **Tree-AH, LARGE** (Glance's production settings: leaf 7000, search 5%, approx 500) | **Shortcut:** groups products into ~714 "boxes" (leaves), opens only the closest ~36 (5%), keeps 500 candidates, ranks them | **The thing we test** = "as-is" production behaviour |
| **BruteForce** | **No shortcut:** compares the query with every product | **The answer key** |

- **One query, sent identically to both.** We compare the two result lists.
  - **Recall@K** = of the answer key's top K products, how many did the fast index also return in its top K?
  - Example: R@100 = 0.553 means 55 of the 100 correct products were found.

---

## Part 1A. What's inside a sparse vector, and what "weak" means

### A real example from Glance's data
**One product** from the 5M data (id `56181de0-…`, a Japanese product from zozo.jp: knee-high boots, brand "lace ladies"):
- **Dense vector:** 1024 numbers, e.g. `[-0.0231, 0.0191, -0.0054, 0.0065, -0.0176, …]`. This describes what the product looks like and means; it isn't readable by humans.
- **Sparse vector (SPLADE):** only **153 non-zero entries** out of 30,522 possible words. Stored as two lists:
```text
"sparse_embedding": {
  "dimensions": [100, 999, 1010, 1011, 1012, 1718, 1720, ...],   <- word numbers (token ids)
  "values":     [0.592, 0.621, 0.688, 0.321, 0.536, 1.272, ...]   <- how important each word is
}
```
- **Each "dimension" is a word number** in a 30,522-word vocabulary. Translated back to words, the most important entries are:

| Word (token) | Weight | Word (token) | Weight |
|---|---|---|---|
| lace | 1.89 | boot | 1.07 |
| knee | 1.62 | japanese | 0.94 |
| boots | 1.39 | knees | 0.90 |
| foot | 1.21 | lacey | 0.90 |
| japan | 1.18 | ##wear | 0.95 |
| high | 1.16 | (Japanese katakana pieces: ハ, ##ク, ##ッ, ##サ, ##ス …) | ~1.0–1.3 |

- So the sparse vector says: "this product is about **lace, knee, high, boots, footwear, Japan**". SPLADE also **adds related words** that aren't literally in the title (e.g. "knees", "lacey", "boot").

**One golden query:** `q0001`, "everyday earbuds for commutes and workouts", **82 non-zero entries**:

| Word (token) | Weight | Word (token) | Weight |
|---|---|---|---|
| ear | 1.89 | gym | 1.25 |
| everyday | 1.73 | ears | 0.97 |
| workout | 1.58 | helmet | 0.80 |
| ##mute (from "commutes") | 1.57 | device | 0.80 |
| ##bu (from "earbuds") | 1.49 | phone | 0.58 |
| exercise | 1.33 | training / daily | 0.69 / 0.68 |

- Long words are split into pieces ("earbuds" → "ear" + "##bu" …; "commutes" → "com" + "##mute" …).
- SPLADE **expands** the query with related words: exercise, gym, training, device, phone.
- **The sparse score** between a query and a product = multiply the weights of the words they share, then add up. For this query and the boots product it's 1.66 (shared words like "gym", "shoe", "gear"): a weak match, as you'd expect.

*How the words were decoded:* the 30,522-entry vocabulary is the standard BERT-uncased vocabulary that SPLADE models use. Glance didn't state the exact model; the decoding is confirmed because the boots product decodes to "lace / knee / boots" and Japanese characters for a Japanese product.

### What "weak" means: three different things, none of them "wrong data"
| When we say… | It means | Evidence | Is the data wrong? |
|---|---|---|---|
| **"The sparse leg is weak"** (EXP-1) | The **fast index's keyword search** (Tree-AH shortcut) finds only **54%** of BruteForce's keyword top-100 (R@100 0.544), vs 86% for the meaning search | EXP-1 table | **No.** It's the *search shortcut* that loses products, not the vectors. With a bigger shortlist the same index reaches 0.846 (40-query diagnostic) and 0.83 (EXP-3). |
| **"BruteForce's sparse isn't exact"** (EXP-2) | Even the BruteForce index **doesn't return every true keyword match**. Against our own exact calculation over all 5M records, it had 91.6% / 85.7% / 74.8% of the true top 10 / 100 / 500, and sometimes fewer than 500 results | EXP-2 exactness check (50 queries, all 5M records) | **No.** The scores BruteForce returned **exactly matched** our own calculation from the same data (error 0.0), and its filters were correct. It's BruteForce's keyword *retrieval* that's incomplete. The data in the index equals our files. |
| **"Keywords alone find fewer relevant products"** (EXP-2 quality) | Using **only** the keyword vectors, Glance's judged relevant products in the top 500 are 43%, vs 67% with meaning only and 76–80% with hybrid | EXP-2 judged hits | **No.** That's how good the keyword *model* is for Glance's queries. It's a property of SPLADE vs CLIP, not an error. Hybrid beats both. |

### So were the later experiments run on a "wrong index"? No.
- **The indexes hold exactly Glance's data.** The data was verified before building (all 5M records), and BruteForce's returned scores equal our exact calculation from the same files.
- **Dense results are measured against exact truth** (BruteForce dense = 100% exact).
- **Sparse and hybrid recall are measured against BruteForce's keyword search**, which is slightly incomplete. So those numbers mean "agreement with BruteForce", not "agreement with perfect". We say this in every report.
- **The comparisons are still fair.** Every experiment compares settings against **the same** BruteForce answers (alpha 0.3 vs 0.5; 500 vs 5,000 candidates; 1024 vs 768). An imperfection in the answer key affects all settings alike, so "which setting is better" stays valid.
- **An independent check agrees:** Glance's **judged relevant products** don't use BruteForce at all, and they move in the same direction:
  - EXP-2: alpha 0.5 → Hit@100 55.6% → 69.3%;
  - EXP-3: 5,000 candidates → Hit@500 76.1% → 85.3%.
- **What we can't say:** the exact absolute keyword recall against a perfect search. It could be somewhat higher or lower than the numbers shown.

**One line for the call:** "The sparse vectors are fine; they're Glance's SPLADE keyword weights, stored exactly. 'Weak' means two things: the fast index's keyword search loses candidates with today's small shortlist, and even BruteForce's keyword search isn't perfectly exhaustive. That's why we also check against Glance's own relevance judgements, which point the same way."

---

## Part 2. The Executive Summary, line by line

**1. Aim**
> *Establish the as-is baseline of Glance's production index configuration against exact BruteForce ground truth. Metric: Recall@K.*

- **Plain words:** "Before changing anything, how good is Glance's current setup compared with a perfect search?"
- **"As-is"** = we copied Glance's exact production index settings and their exact production request. Nothing was tuned.

**2. Run validity (clean)**
> *2,000 queries × 4 modes on both indexes, 0 errors, 0 missing; BruteForce returned ≥500 results for every query; latency client valid at both rates.*

- **2,000 queries:** Glance's own golden queries (real text searches, with their CLIP and SPLADE embeddings).
- **× 4 modes × 2 indexes** = 16,000 search calls. **0 errors and 0 missing** means every query was scored.
- **BruteForce returned ≥500 every time** means the answer key was always complete, so no query had a short answer list.
- **"Client valid"** means our load-testing machine kept up. The latency numbers measure Vector Search, not our laptop/Workbench.

**3. Headline**
> *Production request: R@10 0.663, R@100 0.553, R@500 0.518.*

- The fast index finds **about 6–7 of the correct top 10, and about 55 of the correct top 100.** (i.e. about 66% at top 10 and 55% at top 100.)
- This is **the baseline number** every later experiment tries to improve.

**4. The two legs differ a lot**
> *Dense R@100 0.856, sparse R@100 0.544. Hybrid follows the weaker sparse leg.*

- We also sent **dense-only** and **sparse-only** versions of each query, to test each half separately.
- The **meaning** search is fairly good (0.856); the **keyword** search is weak (0.544).
- **Hybrid (0.553) sits right next to sparse**, not between the two. Line 8 explains why.

**5. Filters are not the cause**
> *Without filters, hybrid R@100 = 0.528.*

- **Filters** = in stock, active, country, blocked catalogs and retailers.
- With them removed, recall is about the same (0.528 vs 0.553). So the filters are **not** what loses products.

**6. Latency is good**
> *P99 45.9 ms at 81 QPS (UDP mean) and 50.2 ms at 565 QPS (UDP peak), 0 errors.*

- **81 and 565 QPS** are Glance's real production traffic: the average and the peak (from their UDP metrics).
- **P99** = 99% of searches finish faster than this.
- 7× more traffic adds only ~4 ms. **Speed isn't the problem; quality is.**

**7. Root cause (40-query diagnostic)**
> *The default search budget (5% of leaves, 500 candidates for 500 results) is too small for real text queries; with a larger budget the same index reaches dense R@100 1.000 and hybrid R@100 0.853.*

- We took 40 queries and asked the **same index** again with bigger budgets, which can be set per request:

| Budget | Leaves searched | Candidates | Dense R@100 | Hybrid R@100 |
|---|---|---|---|---|
| Default (today) | 5% | 500 | 0.834 | 0.561 |
| Override A | 20% | 2,000 | 0.964 | 0.745 |
| Override B | 100% | 5,000 | **1.000** | **0.853** |

- **Why this matters:** at Override B the index found **exactly** the BruteForce dense answers.
  - So the data, the index and our test code are correct.
  - The loss comes **only** from the shortcut size (the "budget"), which can be changed per request (that was **EXP-3**).
- **"500 candidates for 500 results"** = Glance asks for 500 results and the index keeps only 500 candidates, so there is no spare room. One wrong candidate means one missing result.

**What "override" means**
- The index has **built-in defaults**, set when it was built: search **5%** of the folders (leaves) and keep **500** candidates.
- An **override** is a value sent **inside one search request** that replaces the default **for that request only**. Two fields do this:
  - `fraction_leaf_nodes_to_search_override`: share of folders searched;
  - `approximate_neighbor_count`: candidates kept.
- The index itself doesn't change and nothing is rebuilt. The next request without overrides uses the defaults again.
- **Override A** = 20% of folders + 2,000 candidates (4× the default budget). **Override B** = 100% of folders + 5,000 candidates (search everything, a big shortlist).
- **Why we used them in EXP-1:** as a **diagnostic test**, not as a recommendation.
  - If giving the same index a bigger budget makes it match BruteForce, the data and code are fine and the budget is the problem.
  - Override B matched BruteForce exactly on dense (1.000).
- **EXP-3** then tested the budget properly (19 combinations, plus speed under load) to find a setting Glance could actually use.
- **One line:** "An override is a per-request setting that tells the index to search more than its default, without rebuilding anything. We used two of them to prove the loss comes from the default budget."

**8. Why hybrid tracks sparse**
> *With alpha 0.3, a rank-1 dense-only product is out-scored by every sparse-only product down to rank 82; only 3.7% of BruteForce's hybrid top-100 are dense-only.*

- **RRF** merges the two lists. Each product gets: `alpha / (60 + its dense rank) + (1 − alpha) / (60 + its sparse rank)`.
- **With alpha 0.3** (30% dense, 70% sparse):
  - the **#1 dense** product (not in the sparse list) scores 0.3 / 61 = **0.00492**;
  - the **#82 sparse** product (not in the dense list) scores 0.7 / 142 = **0.00493**, slightly higher.
- So even the best meaning-match ranks below 81 keyword-matches. The final top 100 is **mostly sparse products** (only 3.7% dense-only).
- That is why hybrid quality follows the weaker sparse leg. Testing other alphas was **EXP-2**.

**9. Quality impact**
> *Of the products Glance's judges rated score 3, the production setting finds 76.0% in its top 500, vs 89.5% for BruteForce.*

- Glance gave us **judged results**: products rated 0–3 for each golden query (3 = highly relevant).
- Of the "3"-rated products that exist in our 5M data, the production setting finds **76%** in its top 500; a perfect search finds **89.5%**.
- So the recall loss is **real**: relevant products go missing. It is not just a number.

---

## Part 3. "How did you do it?" (the script in plain words)

Script `04_exp1_baseline_eval.py` runs in **5 steps**. It only **reads** from Vertex (searches and describe calls); it never changes an index.

| Step | What it does | Output |
|---|---|---|
| **1. check** | Confirms both indexes are deployed on the endpoint, records their **live settings** (shards, leaf size, 5%, approx 500…), sends 1 test query to each | `index_config_live.json` |
| **2. sample** | Loads Glance's 2,000 golden queries (dense + sparse + country) and validates each one | `queries.jsonl` |
| **3. recall** | For each query × 4 modes: sends the **identical request** to the fast index and to BruteForce, saves both result lists | `raw_recall.jsonl` (16,000 calls) |
| **4. latency** | Sends the production request at a **fixed rate** (81, then 565 QPS) for 3 minutes each, records each request's time | `latency_81qps.csv`, `latency_565qps.csv` |
| **5. report** | Computes Recall@K and P50/P95/P99, writes the findings | `summary.json`, `scorecard.md` |

**The request (built the same way as Glance's Java code, `VertexMatchingEngineProvider.java`):**
- dense vector + sparse vector, **RRF alpha 0.3**, **500 results**;
- filters: instock = true, status = active, catalog_id NOT IN 4 test catalogs, retailer NOT IN 3 retailers, country = the query's country;
- **no** approximate-neighbour or leaf overrides, because Glance's code doesn't set them.

**The 4 modes:** see Part 3A below for the full explanation and the code.

### Part 3A. The 4 modes: what they are, and how the code builds them

**What a "mode" is:** one **version of the same search request**. For every golden query we send 4 versions, each keeping or dropping one part of the request, so we can see **where** quality is lost.

**Example query:** "everyday earbuds for commutes and workouts" (country: India). Glance turns it into:
- a **dense** vector (CLIP): the **meaning** of the text;
- a **sparse** vector (SPLADE): the **important words** ("earbuds", "workouts", …).

| # | Mode | What is sent | In plain words | Why we ran it |
|---|---|---|---|---|
| 1 | **hybrid_prod** | dense + sparse, merged with **RRF alpha 0.3**, **with** filters | **Exactly what Glance's live system sends** | ⭐ Headline: "how good is production?" |
| 2 | **dense_filtered** | dense only, with filters (no sparse, so no RRF/alpha) | **Meaning search alone** | Is the meaning half losing products? |
| 3 | **sparse_filtered** | sparse only, with filters (no dense, so no RRF/alpha) | **Keyword search alone** | Is the keyword half losing products? |
| 4 | **hybrid_nofilter** | dense + sparse, RRF alpha 0.3, **without** filters | Production with the filters switched off | Are the filters causing the loss? |

**Filters** (modes 1–3) = Glance's always-on filters:
- in stock = true;
- status = active;
- catalog not in 4 test catalogs;
- retailer not in 3 blocked retailers;
- country = the query's country.

**RRF / alpha** only applies when both dense and sparse are sent. With one part there is only one result list, so there is nothing to merge. (EXP-2 confirmed this: alpha 1.0 gives exactly the dense-only results, and alpha 0.0 exactly the sparse-only results.)

**What the 4 modes told us (R@100):**

| Mode | R@100 | Conclusion |
|---|---|---|
| 1. hybrid_prod | **0.553** | Production quality is low |
| 2. dense_filtered | 0.856 | The meaning half is fairly good |
| 3. sparse_filtered | 0.544 | The keyword half is weak, and hybrid sits right next to it |
| 4. hybrid_nofilter | 0.528 | About the same as mode 1, so the filters are not the cause |

**Arithmetic:** 2,000 queries × 4 modes = **8,000 requests**. Each goes to **both** indexes = **16,000 search calls**. Each (query, mode) gives one recall value.

#### How the code makes the 4 modes (script `04_exp1_baseline_eval.py`)

**Step 1: one small table of switches (lines 71–76).** Each mode is just four on/off switches:
```python
MODES = {
    "hybrid_prod":     dict(dense=True,  sparse=True,  rrf=True,  filters=True),
    "dense_filtered":  dict(dense=True,  sparse=False, rrf=False, filters=True),
    "sparse_filtered": dict(dense=False, sparse=True,  rrf=False, filters=True),
    "hybrid_nofilter": dict(dense=True,  sparse=True,  rrf=True,  filters=False),
}
```

**Step 2: one function builds every request from those switches (`build_request`, lines 161–181).** It starts with an empty query and adds each part only if its switch is on:
```python
def build_request(q, mode, deployed_id):
    m = MODES[mode]
    dp = IndexDatapoint()
    if m["dense"]:                       # add the meaning vector
        dp.feature_vector = q["dense"]
    if m["sparse"] and q["sparse_dims"]: # add the keyword vector
        dp.sparse_embedding = IndexDatapoint.SparseEmbedding(dimensions=q["sparse_dims"], values=q["sparse_vals"])
    if m["filters"]:                     # add Glance's filters + the query's country
        dp.restricts = [...instock, status, catalog_id, retailer_domain..., country(UPPER, lower)]
    query = FindNeighborsRequest.Query(datapoint=dp, neighbor_count=500)
    if m["rrf"]:                         # merge the two lists with alpha 0.3
        query.rrf = FindNeighborsRequest.Query.RRF(alpha=0.3)
    return FindNeighborsRequest(index_endpoint=ENDPOINT, deployed_index_id=deployed_id, queries=[query])
```
- **Same function, same query, different switches:** that is the whole difference between the 4 modes. Nothing else changes (same 500 results, same endpoint).
- **`deployed_id`** is the only other input. The function is called once with the production-config index and once with BruteForce, so **both get a byte-identical request**.
- There are **no `approximate_neighbor_count` or leaf overrides**, exactly as in Glance's Java code.

**Step 3: the recall stage loops over every query × every mode × both indexes (`stage_recall`, `work()`).**
```python
todo = [(q, m) for q in queries for m in MODES]           # 2,000 x 4 = 8,000 pairs
def work(item):
    q, m = item
    for tag, did in (("test", DEPLOYED_TEST), ("gt", DEPLOYED_GT)):   # production-config index, then BruteForce
        ids, ms = call(client, build_request(q, m, did))               # same request to both
        rec[f"{tag}_ids"] = ids
    write one line to raw_recall.jsonl: {qid, mode, test_ids, gt_ids, ...}
```
- It runs 8 searches in parallel, retries a failed call up to 3 times, and records any error. Nothing is silently dropped.
- Every result line carries its **mode name**. That is how the results stay separated.

**Step 4: the report groups the results by mode (`stage_report`).** For each line it computes recall and files it under that line's mode:
```python
t = test_ids;  g = gt_ids                                  # the two result lists
for K in (5, 10, 20, 50, 100, 250, 500):
    recall = len(set(t[:K]) & set(g[:K])) / len(g[:K])     # share of BruteForce's top-K found
    per_mode[mode][f"recall@{K}"].append(recall)
# then mean and standard error per mode -> the 4 rows of the results table
```

**One line for the call:** "Each mode is four on/off switches (dense, sparse, RRF, filters) that one function uses to build the request. The same request goes to both indexes, and every result is tagged with its mode, so the report can score the production request and the three diagnostics separately."

**Recall formula (one line):** `Recall@K = |fast top-K ∩ BruteForce top-K| / K`, averaged over the 2,000 queries.

**Latency method ("open loop"):**
- Requests are sent on a **fixed schedule** (e.g. 565 per second), whether or not earlier ones have finished, just like real users.
- The load is split over several processes, so our machine isn't the bottleneck. We check that it kept up ("client valid").
- The first 20 seconds are warm-up and are not counted.

**Safety:** dry run by default, resumable (a crash continues where it stopped), and every run records the script's fingerprint (sha256) and settings, so it is reproducible.

---

## Part 4. Likely questions and short answers

**Q1. "BruteForce has an exact copy of the same 5M data. Why is recall so low, shouldn't they match?"** *(Vinod asked this.)*
- Same data, **different search method**. BruteForce checks every product; Tree-AH checks only ~5% of the index and keeps 500 candidates.
- **Proof:** with the full budget (Override B), the same Tree-AH index matched BruteForce **exactly** on dense (R@100 1.000). So the data is identical; only the shortcut loses products.

**Q2. "Is BruteForce really exact?"**
- **Dense: yes.** EXP-2 checked offline against all 5M records: 100% exact.
- **Sparse: not fully.** Tie-aware 0.916 / 0.857 / 0.748 at K = 10 / 100 / 500. So sparse and hybrid recall are "compared with BruteForce", not "compared with perfect".

**Q3. "Production has 9 shards; you tested 1. Does this hold?"**
- 5M fits in 1 LARGE shard; production has ~5.5M per shard. So **one test shard ≈ one production shard**.
- But candidates are collected **per shard**, and production also merges 9 shards. **The 10M (2-shard) run confirms the multi-shard effect.** We say "direction, not final production numbers".

**Q4. "Why golden queries, not product vectors?"**
- An earlier provisional run used product vectors as queries, and dense recall was 0.977. Products are "easy" queries, because they already sit in the index.
- **Real text queries are harder** (mean best-match similarity only 0.329), and they are what Glance users send. So golden queries are the honest baseline.

**Q5. "Is your request really the same as production?"**
- Yes: built from Glance's Java code and their sample payload (alpha 0.3, 500 results, same filters, no overrides).
- **Known gaps** (in the caveats):
  - the gender filter (production adds it conditionally) and category deny lists are not applied, which affects test and answer key equally;
  - `per_crowding_attribute_neighbor_count` was not set (production value unknown, asked Glance).

**Q6. "Are the latency numbers what Glance sees?"**
- They're measured from a Workbench in us-central1 over the public endpoint, on 1 shard with 2 replicas. Glance's network path and 9-shard fan-out can differ.
- Treat them as **relative** ("latency has headroom"), not exact production latency.

**Q7. "Why 81 and 565 QPS?"**
- They're Glance's real production traffic from their UDP metrics: mean 81.4, peak 564.8 QPS.

**Q8. "What would you change?"**
- Two per-request levers, no rebuild needed:
  - **more candidates** (EXP-3: approx 5,000 at 5% leaves lifts hybrid R@100 0.553 → 0.829 and still holds 565 QPS);
  - **alpha** (EXP-2: 0.5–0.7 lifts recall and judged hits).
- Both still to be confirmed at 10M.

**Q9. "Why is quality 76.0% here but 76.1% in EXP-3?"**
- A slightly different set of eligible judged products: EXP-1 has 491 queries / 679 products, EXP-2/EXP-3 have 487 / 663, after a stricter filter check. Same conclusion.

**Q10. "How do I know your code is correct?"**
- Override B dense = 1.000 vs BruteForce (an impossible result if the code or data were wrong).
- Returned scores match exact dot products (error < 1e-7 dense).
- An independent audit re-computed the RCA numbers (`AUDIT_REPORT_exp1_rca_verification_2026-10-08.md`).
- Every run records the script fingerprint and settings.

---

## Part 4A. This guide vs the spreadsheet (EXP-1 tab)
Every number on the EXP-1 tab is **identical** to this guide (same source: the EXP-1 scorecard and RCA):
- the recall table: R@50 / R@100 / R@250 / R@500 for the 4 modes;
- the 40-query diagnostic table;
- the insights and root causes.

Two differences to know when presenting from the sheet:

| Point | Sheet (EXP-1 tab) | This guide | What to say |
|---|---|---|---|
| Depths shown | R@50, R@100, R@250, R@500 | Also quotes R@10 (0.663) | When pointing at the sheet, lead with **R@50 0.591 and R@100 0.553**. R@10 0.663 is in the full results doc. |
| Latency and judged quality | Not on the tab | P99 45.9 / 50.2 ms; judged hits 76.0% vs 89.5% | Quote them from the results doc. The EXP-3 tab shows 76.1% for the same setting (slightly different eligible set, 487 vs 491 queries; same conclusion). |

## Part 5. Three sentences to open with

1. "EXP-1 measured Glance's current production settings, on 5 million of their products, against a perfect search: about **55–66% recall** at the depths Glance uses, with **good latency** (P99 about 50 ms at peak)."
2. "The loss isn't the data or the filters. It's the **search budget**: 5% of the index and only 500 candidates for 500 results. With a bigger budget, the same index matches the perfect dense search exactly."
3. "Hybrid follows the weaker keyword side because **alpha 0.3 gives it 70% of the weight**. That led to EXP-2 (alpha) and EXP-3 (budget)."
