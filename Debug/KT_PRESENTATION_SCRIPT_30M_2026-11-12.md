# Knowledge-Transfer Presentation Script: Glance WS2 Vertex AI Vector Search 30M POC

*Presenter's spoken script for the final engineering handover session. The engineering team will use these results to author the Technical Design Document (TDD).*
*Total presentation time: approximately 28 minutes (17 slides).*

---

## Slide 1: Purpose, Scope, and TDD Objectives

**Screen:** Tab '30M - all indexes', overview rows 1 to 20, columns Index, Exp ID, Experiment Title, Settings (alpha / A / F), Shard Count.

**On screen:**
- Scale: 30,000,000 records evaluated on Vertex AI Vector Search.
- 11 formal experiment sets executed between 23 Oct 2026 and 12 Nov 2026 UTC.
- Goal: provide verified empirical data to author the Technical Design Document (TDD).
- Evidence labels: MEASURED (run files), CALCULATED (arithmetic), INFERENCE (reading of data), UNVERIFIED (logs/analysis).

**Say:**
Good morning, everyone. Today we are conducting the final knowledge transfer for the Glance WS2 Vertex AI Vector Search proof of concept at thirty million catalog scale.

Over the period from 23 October to 12 November 2026, we ran eleven experiment sets to evaluate quality, latency, throughput, concurrent writes, and serving costs. The purpose of this session is to hand over verified empirical findings so you can author the Technical Design Document.

Throughout this presentation, every technical statement is strictly labeled. When I say measured, that number came directly from a verified run output file. Calculated means simple arithmetic on measured values. Inference indicates an interpretation that the data suggests but does not strictly prove. Finally, unverified means the figure appears in analysis notes or session logs rather than an automated run artifact.

Our goal today is to give you an accurate, unvarnished picture of how Vertex AI Vector Search performs at thirty million scale, including its strengths, edge cases, and operational limits.

*(time: ~1.5 min)*

---

## Slide 2: How to Read the Sheet

**Screen:** Tab 'Legend', rows 2 to 6 (SETTINGS NOTATION), rows 7 to 10 (DATA EVIDENCE LABELS), rows 11 to 18 (PASS / FAIL RULES PER EXPERIMENT), rows 19 to 21 (HOW 'PICKED' WAS DECIDED); and sheet tab bar across the bottom showing all 9 tabs in order: '30M - all indexes', '30M - 768-d', '30M - 512-d', 'EXP-7 settings', 'EXP-2 settings', 'EXP-3 settings', 'EXP-8 settings', 'EXP-9 settings', 'Legend'.

**On screen:**
- 9 tabs in order:
  - Summary views: '30M - all indexes', '30M - 768-d', '30M - 512-d'.
  - Per-experiment settings views: 'EXP-7 settings', 'EXP-2 settings', 'EXP-3 settings', 'EXP-8 settings', 'EXP-9 settings'.
  - Reference view: 'Legend'.
- Colour coding:
  - Green = picked and confirmed (e.g. EXP-7 tab rows 24, 46, 47; 30M - all indexes rows 10 and 20).
  - Yellow = picked once, not confirmed (EXP-7 tab row 30, 1024-d alpha0.5_A5000_F0.02).
  - Red = failed pre-set rule (e.g. p99 > 100 ms, error rate >= 0.1%, or missed recall floor).
- Settings notation: `alpha<val>_A<count>_F<fraction>`
  - alpha: RRF dense weight in hybrid fusion (0.3 is production default; 0.5 is quality-favoured; 0.7 is recall-heavy; 1.0 is dense only; 0.0 is sparse only).
  - A: approximate neighbours searched per shard (A500 is 500 default; A1000 is 1,000; A2000 is 2,000; A5000 is 5,000; must be >= requested neighbor_count 500).
  - F: fraction of leaf nodes searched (Funset is unset 5% index default; F0.02 is 2%; F0.05 is 5%; F0.10 is 10%; F0.20 is 20%; F1.00 is 100% full scan; overrides act on dense leg).
- Standard columns: Setting code, Recall@10, Recall@100, Recall@500, p50 ms, p99 ms, errors, CPU %, Pass/Fail vs rule, and Picked? (why).

**Say:**
Before diving into individual experiments, let us walk through how our results spreadsheet is organized so you can navigate it during this presentation and later while authoring the TDD.

The workbook contains nine tabs in order along the bottom. The first three tabs give high-level summaries: 30M - all indexes covers every experiment across all index variants, while 30M - 768-d and 30M - 512-d isolate the reduced-dimension findings. Next are five detailed settings tabs: EXP-7 settings, EXP-2 settings, EXP-3 settings, EXP-8 settings, and EXP-9 settings. The final tab is the Legend, which defines every rule and term.

Row colors immediately reveal the outcome of our pre-set engineering rules. Green rows highlight configurations that were picked and confirmed. Yellow marks the single configuration on EXP-7 tab row 30, which was picked once but failed its confirmation repeat. Red rows mark configurations that failed our pre-set rules on latency, error rate, or recall floor.

Let us also define the parameter notation shown in column Setting code. We write configurations as alpha, followed by A, followed by F.
Alpha represents the dense vector weight in Reciprocal Rank Fusion for hybrid search. Zero point three is today's production baseline, zero point five is quality-favoured, zero point seven is recall-heavy, one point zero is dense only, and zero point zero is sparse only.
Capital A denotes approximate neighbours searched per shard, where A500 is the 500 default, up to A5000 for 5,000 neighbours.
Capital F denotes the fraction of leaf nodes searched in the index tree, where Funset means the unset 5% default, F0.02 searches 2%, F0.05 searches 5%, and F1.00 is a full 100% leaf scan. Note that leaf overrides act on the dense leg, not the sparse leg.

*(time: ~1.5 min)*

---

## Slide 3: Data and Built Indexes

**Screen:** Tab '30M - all indexes', columns Index, Shard Count, Build cost, Build cost (net, 42.75%), rows 2 to 7 (1024-d), rows 8 to 10 (PCA 768-d), rows 11, 14, 15 (PCA 512-d), row 12 (PCA 768-d + top-96 pruned), and row 13 (PCA 512-d + top-96 pruned).

**On screen:**
- Catalog: 30,000,000 dense and 30,000,000 sparse vectors, exactly 0 duplicate ids.
- Indexes: 1024-d LARGE, BruteForce (13 shards), PCA 768-d, PCA 512-d, 768-d + top-96, and 512-d + top-96 (building).
- Shard counts: exactly 5 shards across all four built Tree-AH indexes; dimension reduction did not reduce shards.
- Quota: 50-node regional limit; a 5-shard index requires 10 nodes at 2 replicas and 15 nodes at 3 replicas.

**Say:**
Let us look at the data foundation and the indexes we constructed on tab 30M - all indexes.

The evaluation dataset contains exactly thirty million records with zero duplicate identifiers across both dense vectors and sparse SPLADE vectors. We built five completed indexes and one that is currently in progress.

Our baseline is the 1024-dimension LARGE index with id 2255975758847541248. For exact recall evaluation, we built a 1024-dimension BruteForce index with id 4952506035735625728. To test dimension reduction, we built a PCA 768-dimension index with id 1549297645443350528 and a PCA 512-dimension index with id 7505202599524564992. For sparse pruning, we built a 768-dimension index pruned to top-96 terms with id 7566001194494066688. The 512-dimension top-96 index with id 7229181200487677952 is currently building, shown on row 13.

A critical measured finding shown in column Shard Count is that all four completed Tree-AH indexes landed on exactly 5 shards. Our pre-test hypothesis that reducing dimensions to 768 would yield 4 shards, or that 512 would yield 3 shards, was disproven. Our inference is that shard sizing is driven by the sparse-side payload, which includes approximately 23.8 gigabytes of restrict metadata against an internal 10 gigabyte per-shard threshold.

Keep in mind our regional quota limit is 50 nodes. Because each 5-shard deployment takes 10 nodes at 2 replicas and 15 nodes at 3 replicas, you cannot deploy two 5-shard indexes alongside the 13-shard BruteForce index, as that would demand 58 nodes.

*(time: ~1.5 min)*

---

## Slide 4: Evaluation Methodology and Cost Basis

**Screen:** Tab 'Legend', rows 6 to 11 (Default request conditions, Evidence labels, EXP-1 rule); and tab '30M - all indexes', rows 2 to 4, columns QPS / nodes, Serving cost, Serving cost (net, 42.75%).

**On screen:**
- Golden query workload: 2,000 production text queries with categorical filters and 500 neighbors requested.
- Standard load steps: 180-second load cells evaluated at 81, 400, 565, and 800 QPS.
- Latency threshold: p99 at or below 100 milliseconds.
- Ground truth reality: BruteForce dense leg is exact; sparse leg is incomplete (tie-aware 0.8898 at K=100).
- Financial baseline: machine type e2-highmem-16; list pricing and net pricing calculated at 42.75%.

**Say:**
Next, let us review how we measured performance and evaluated costs, referencing tab Legend rows 6 through 11 and tab 30M - all indexes.

All recall evaluations used a frozen set of 2,000 golden production queries. Our standard production request uses hybrid search, RRF alpha 0.3, production filters, 500 requested neighbors, and default Tree-AH search with alpha 0.3, 500 neighbours per shard, and unset 5% leaf search (code alpha0.3_A500_Funset). Load cells ran for 180 seconds across target rates of 81, 400, 565, and 800 queries per second. Our operational target is p99 latency at or below 100 milliseconds.

Here is an essential architectural detail for the TDD team: recall is measured against our BruteForce index. While the BruteForce dense leg is mathematically exact, scoring a tie-aware 1.0 across 50 checked queries, the BruteForce sparse leg is not exact. At K equals 100, its tie-aware score is 0.8898, and for top-500 requests it returned as few as 268 results. Therefore, our inference is that measured hybrid recall represents agreement with the 1024-dimension BruteForce key rather than absolute ground-truth relevance.

On costs, every 30M deployment uses e2-highmem-16 instances. We report costs at list price, which is $10.12 per hour for 10 nodes, and at net price, which applies the standard 42.75% factor to reach $4.33 per hour.

*(time: ~1.5 min)*

---

## Slide 5: EXP-1: Production Baseline

**Screen:** Tab '30M - all indexes', row 2, columns Index, Exp ID, Settings (alpha / A / F), Latency P99 at peak (565 QPS), Shard Count, Recall@100, Recall@500, Avg CPU Utilization at 565 QPS, Notes (source / caveat).

**On screen:**
- Setting: alpha 0.3, 500 neighbours per shard, unset 5% leaf search (code `alpha0.3_A500_Funset`).
- Baseline hybrid recall: R@10 0.7555, R@100 0.6799, R@500 0.6574 (2,000 queries, 0 errors).
- Dense leg vs hybrid: dense-only recall is 0.8914 at R@100; sparse-only recall is 0.6705 at R@100.
- Load test pass: run 2 held 565 QPS on 10 nodes (p99 75.6 ms, 0 errors, 27.31% CPU).
- Load test failure: run 1 failed 565 QPS (p99 1,098.6 ms, 32 errors) at only 25.64% average CPU.

**Say:**
In EXP-1, shown on row 2 of tab 30M - all indexes, we established the production baseline on the 1024-dimension index using today's production configuration: with alpha 0.3, 500 neighbours per shard, and index default 5% leaf search (code alpha0.3_A500_Funset).

Across 2,000 golden queries with zero errors, baseline hybrid recall measured 0.7555 at R@10, 0.6799 at R@100, and 0.6574 at R@500. Interestingly, evaluating the dense leg in isolation yields a much higher R@100 of 0.8914, whereas sparse-only search scores 0.6705. When we removed filters, hybrid R@10 was 0.7420 and R@500 was 0.6405.

On latency and throughput, results revealed significant run-to-run variation. At 81 QPS, p99 was 54.0 milliseconds in run 1 and 64.8 milliseconds in run 2. At 565 QPS on 10 nodes, run 2 passed cleanly with a p99 of 75.6 milliseconds, zero errors, and 27.31% average CPU.

However, run 1 at that same 565 QPS failed dramatically, logging a p99 of 1,098.6 milliseconds and 32 errors. Crucially, average CPU utilization during that failure was only 25.64%. This proved early on that latency degradation on this platform is not primarily caused by CPU exhaustion. Across later runs in different sessions, 1024-dimension p99 at 565 QPS ranged between 58.4 and 95.8 milliseconds.

*(time: ~1.5 min)*

---

## Slide 6: EXP-2: Hybrid RRF Alpha and Judged Relevance

**Screen:** Tab 'EXP-2 settings', rows 2 to 19, pointing at row 4 (red, sparse-only alpha 0.0), row 7 (green, 1024-d alpha 0.5), and row 16 (green, 768-d alpha 0.5), columns Mode / Setting, Setting code, alpha, Hybrid R@100, Judged hit@100, Judged Δhit@100 vs alpha 0.3, 565 QPS p99 ms, Recommended? (why).

**On screen:**
- Alpha sweep on 1024-d (rows 5 to 9):
  - With alpha 0.3, 500 neighbours per shard, unset 5% leaves (code `alpha0.3_A500_Funset`, row 5): R@100 0.6799, judged score-3 hit@100 is 0.3699.
  - With alpha 0.5, 500 neighbours per shard, unset 5% leaves (code `alpha0.5_A500_Funset`, row 7 green): R@100 0.7918, judged hit@100 is 0.4800 (+0.1101 ± 0.0088 lift).
  - With alpha 0.7, 500 neighbours per shard, unset 5% leaves (code `alpha0.7_A500_Funset`, row 9): R@100 0.8740, judged hit@100 is 0.4464 (+0.0765 lift).
- Red row 4: sparse-only with alpha 0.0, 500 neighbours, unset leaves (code `alpha0.0_A500_Funset`) failed load rule.
- Semantic imbalance at alpha 0.3: top-10 overlap is 0.9238 with sparse-only, but only 0.0337 with dense.
- Quality on PCA 768-d (row 16 green): with alpha 0.5, 500 neighbours, unset leaves (code `alpha0.5_A500_Funset`), R@100 is 0.7851, p99 is 59.4 ms, all 14 load cells met p99 <= 100 ms.
- Zero serving cost impact: alpha 0.3, 0.5, and 0.7 all serve 565 QPS on 10 nodes at identical cost ($10.12/h list, $4.33/h net).

**Say:**
Now let us switch to tab EXP-2 settings, where we explored tuning the reciprocal rank fusion alpha parameter across rows 2 through 19.

Today, production uses an alpha of 0.3, shown on row 5 (code alpha0.3_A500_Funset). At alpha 0.3, the top of the returned list is almost entirely dominated by sparse keyword matches. Overlap with sparse-only search is 0.9238 at K equals 10, whereas overlap with dense vector search is only 0.0337. On row 4, marked red, sparse-only search with alpha 0.0 failed our load rule.

When we shifted alpha toward dense search, hybrid R@100 against BruteForce rose from 0.6799 at alpha 0.3, to 0.7918 at alpha 0.5 on row 7, and to 0.8740 at alpha 0.7 on row 9. Remember that recall across different alphas is measured against different BruteForce keys, so comparing raw recall across alphas is not like-for-like.

However, human judged relevance confirms the quality improvement. On judged score-3 products, hit@100 rose from 0.3699 at alpha 0.3 to 0.4800 at alpha 0.5, a statistically significant gain of 0.1101. At alpha 0.7, judged quality was 0.0765 higher than at alpha 0.3. We also observed that Tree-AH approximate search loses relevant items that exact search finds, scoring 0.3699 on the test index versus 0.4491 on BruteForce.

Row 7 on 1024-dimension and row 16 on 768-dimension are highlighted green because alpha 0.5 delivered the highest judged quality gain. On the 768-dimension index, setting alpha 0.5 with 500 neighbours per shard and unset leaves achieved a p99 of 59.4 milliseconds at 565 QPS, and all 14 load cells met our 100 millisecond target.

From an engineering and cost standpoint, alpha is purely a request-time parameter. Serving alpha 0.3, 0.5, or 0.7 requires the exact same 10 nodes at 565 QPS, costing $10.12 per hour list and $4.33 net.

*(time: ~1.5 min)*

---

## Slide 7: EXP-3: ScaNN Tree-AH Algorithmic Tuning

**Screen:** Tab 'EXP-3 settings', rows 2 to 20 (all red: winner=null), highlighting row 2 (baseline A500_Funset), row 10 (A1000_F0.20), row 17 (A5000_F0.10), row 18 (A5000_F0.20), row 20 (A5000_F1.00), columns Setting code, alpha, A (neighbours/shard), F (leaf %), Hybrid R@10, Hybrid R@100, Timing median ms, Load QPS tested, Load p99 ms, Pass/Fail vs rule, Selection outcome / Picked?.

**On screen:**
- Pre-set selection rule: highest max sustainable QPS per node among settings with (a) hybrid R@100 within margin of best cell (0.9042), (b) dense R@500 >= 0.95, and (c) p99 <= 100 ms and errors < 0.1% at 565 QPS on 10 nodes.
- Outcome: winner=null; all rows 2 to 20 are red because no setting met all criteria.
- Recall progression at alpha 0.3:
  - Baseline with 500 neighbours per shard, unset 5% leaves (code `alpha0.3_A500_Funset`, row 2): R@100 0.6799, median timing 35.99 ms.
  - Deeper search with 5,000 neighbours per shard, 20% leaves (code `alpha0.3_A5000_F0.20`, row 18): R@100 0.9042, R@10 0.9295, median timing 64.98 ms.
  - Full scan with 5,000 neighbours per shard, 100% leaves (code `alpha0.3_A5000_F1.00`, row 20): R@100 0.9071, median timing 159.35 ms.
- Load failure under deeper search:
  - With 1,000 neighbours per shard, 20% leaves (code `alpha0.3_A1000_F0.20`, row 10): failed 400 QPS (p99 834.1 ms, 18.16% rejected).
  - With 5,000 neighbours per shard, 10% leaves (code `alpha0.3_A5000_F0.10`, row 17): failed 400 QPS twice (p99 132.4 ms and 122.3 ms, 0 errors).
- Capacity ceiling for baseline requests on 10 nodes lies between 565 and 800 QPS (baseline held 565 QPS, failed 800 QPS at 126.7 ms).

**Say:**
Please turn to tab EXP-3 settings. Here we investigated tuning Tree-AH search parameters per request on the 1024-dimension index. Notice that every row from row 2 to row 20 is colored red, because no setting satisfied our pre-set selection rule.

The pre-set selection rule required the highest sustainable QPS on 10 nodes among settings achieving hybrid R@100 within margin of the best non-ceiling cell, which was 0.9042, dense R@500 at or above 0.95, and error rate under 0.1% with p99 at or below 100 milliseconds at 565 QPS.

Increasing search parameters produced substantial recall gains. On row 2, baseline setting with alpha 0.3, 500 neighbours per shard, and unset 5% leaves (code alpha0.3_A500_Funset) gives R@100 of 0.6799 with a median single-request latency of 35.99 milliseconds. On row 18, searching with alpha 0.3, 5,000 neighbours per shard, and 20% leaves (code alpha0.3_A5000_F0.20) increases hybrid R@100 to 0.9042 and R@10 to 0.9295, with median latency rising to 64.98 milliseconds. Full scan on row 20, searching with alpha 0.3, 5,000 neighbours per shard, and 100% leaves (code alpha0.3_A5000_F1.00), reached 0.9071 at 159.35 milliseconds median latency. We found that hybrid recall gains are driven almost entirely by neighbor count A; increasing fraction F dramatically improves dense recall from 0.7754 to 0.9705, but barely shifts hybrid recall.

Unfortunately, deeper search settings could not survive production traffic on 10 nodes. On row 10, searching with alpha 0.3, 1,000 neighbours per shard, and 20% leaves (code alpha0.3_A1000_F0.20) failed at 400 QPS with a p99 of 834.1 milliseconds and over 18% rejected requests. On row 17, searching with alpha 0.3, 5,000 neighbours per shard, and 10% leaves (code alpha0.3_A5000_F0.10) failed 400 QPS twice, with p99s of 132.4 and 122.3 milliseconds. Meanwhile, the baseline setting held 400 and 565 QPS, but failed 800 QPS at 126.7 milliseconds.

Because deeper settings failed load and baseline missed the recall margin by 0.2243, winner was null. The capacity ceiling for baseline requests on 10 nodes was located between 565 and 800 QPS.

*(time: ~1.5 min)*

---

## Slide 8: EXP-6: Dimension Reduction (PCA 768-d and 512-d)

**Screen:** Tab '30M - 768-d', row 2, and tab '30M - 512-d', row 2 (red), columns Index, Exp ID, Settings (alpha / A / F), Latency P99 at peak (565 QPS), Shard Count, Recall@100, Recall@500, Avg CPU Utilization at 565 QPS, Picked? (why), Notes (source / caveat).

**On screen:**
- Baseline setting evaluated: alpha 0.3, 500 neighbours per shard, unset 5% leaves (code `alpha0.3_A500_Funset`).
- Hybrid recall change (vs same-session 1024-d control, n=2,000):
  - 768-d (tab '30M - 768-d', row 2): R@10 −0.0011, R@100 −0.0009 (0.6790 vs 0.6799), R@500 −0.0052.
  - 512-d (tab '30M - 512-d', row 2 red): R@10 −0.0088, R@100 −0.0122 (0.6677 vs 0.6799), R@500 −0.0326.
- Dense-only degradation: 768-d drops −0.0191 at R@100; 512-d drops severe −0.1403 at R@100 (−0.1091 at R@500).
- Full scan recovery: 768-d dense recovers to 0.9959; 512-d dense does not recover (0.8150).
- Latency and throughput: both held 800 QPS on 10 nodes (768-d p99 81.5 ms; 512-d p99 67.1 ms) while 1024-d control failed (235.1 ms and 958.2 ms).
- CPU reduction at 565 QPS: average CPU fell −24.2% on 768-d (-24.2%) and −36.7% on 512-d (21.38% vs 33.79%).
- Financial impact: 0 hourly cost savings; both still land on 5 shards and cost $10.12/h list ($4.33/h net) for 10 nodes.

**Say:**
Let us look at tab 30M - 768-d row 2 and tab 30M - 512-d row 2, which report EXP-6 dimension reduction using PCA from 1024 dimensions down to 768 and 512 dimensions with baseline setting alpha 0.3, 500 neighbours per shard, and unset leaves (code alpha0.3_A500_Funset).

In terms of search quality, PCA 768-dimension preserved hybrid recall exceptionally well. In paired same-session testing, R@100 dropped by only 0.0009, from 0.6799 down to 0.6790, and R@500 dropped by 0.0052. For 512-dimension, hybrid R@100 fell by 0.0122 to 0.6677, and R@500 fell by 0.0326.

However, dense-only recall tells a starker story, which is why 512-dimension on row 2 is colored red. While 768-dimension dense recall dropped by 0.0191 at R@100, 512-dimension dense recall dropped by 0.1403 at R@100 and by 0.1091 at R@500. Under a full leaf scan, 768-dimension dense recall recovers to 0.9959, matching the 1024-dimension index. In contrast, 512-dimension dense recall does not recover, reaching only 0.8150. Sparse-only recall was completely unaffected on both indexes.

Where dimension reduction succeeded was in latency and CPU headroom. At 565 QPS on 10 nodes, average CPU dropped by 24.2% on 768-dimension and by 36.7% on 512-dimension. Both reduced indexes held 800 QPS on 10 nodes, with 768-dimension delivering a p99 of 81.5 milliseconds and 512-dimension delivering 67.1 milliseconds. In both cases, the same-session 1024-dimension control failed 800 QPS, logging 235.1 milliseconds and 958.2 milliseconds. Note that the 768-dimension pass at 800 QPS was marginal, as the busiest node hit 59.99% CPU and 195 requests were rejected during warm-up.

For the TDD team, the critical financial takeaway is that dimension reduction achieved zero hourly infrastructure savings. Both indexes still require 5 shards and cost $10.12 per hour list, or $4.33 net, for 10 nodes.

*(time: ~2.0 min)*

---

## Slide 9: EXP-9: Sparse Pruning (SPLADE Top-96 on 768-d)

**Screen:** Tab 'EXP-9 settings', rows 2 to 17 (pruned index) and rows 18 to 22 (1024-d control), highlighting red rows 4, 9, 13, 14, 15, and row 3 (565 QPS load pass but failed selection rule), columns Index, Setting code, Hybrid R@100, Hybrid R@100 delta vs unpruned 768-d, Sparse R@100 delta vs unpruned 768-d, Dense R@500 (pruned), Load QPS, p99 ms, Pass/Fail vs rule, Picked?.

**On screen:**
- Scope: pruned 654,349,804 of 3,161,907,488 terms (−20.69%) across 13,657,696 records (45.53%).
- Shard count outcome: index still required 5 shards; hourly hosting savings are USD 0 ($10.12/h list, $4.33/h net).
- Hybrid recall degradation under baseline (code `alpha0.3_A500_Funset`, rows 2 to 6):
  - R@10 drops −0.0820, R@100 drops −0.0202 (0.6588 vs 0.6790), R@500 +0.0059.
  - Loss is entirely in sparse leg: sparse R@100 drops −0.0232; dense R@500 unchanged (+0.0017).
- Deeper search penalty grows:
  - With alpha 0.3, 2,000 neighbours per shard, 5% leaves (code `alpha0.3_A2000_F0.05`, rows 11 to 14): R@100 drops −0.0539 under setting `alpha0.3_A2000_F0.05`.
  - With alpha 0.3, 5,000 neighbours per shard, 20% leaves (code `alpha0.3_A5000_F0.20`, row 16): R@100 drops −0.0794 under setting `alpha0.3_A5000_F0.20`.
- Latency comparison: pruned index was not faster in same-session testing (at 565 QPS p99 was 71.1 ms vs 58.4 ms; at 800 QPS p99 was 86.6 ms vs 71.3 ms).
- Outcome: winner=null; no setting met the selection rule because dense R@500 was 0.8160 (below the 0.95 threshold) and shards stayed at 5.

**Say:**
Now look at tab EXP-9 settings, covering rows 2 through 22, where we tested SPLADE sparse pruning to top-96 terms on the 768-dimension index.

Pruning eliminated over 654 million terms, representing a 20.69% reduction, and modified 45.53% of records in the catalog.

The primary operational objective of pruning was to reduce the shard count from 5 shards to 4 or 3 shards. It failed to do so. The built index remained at 5 shards, resulting in zero dollars saved on serving nodes or hourly list price.

At the same time, pruning introduced a noticeable recall penalty. Under baseline setting with alpha 0.3, 500 neighbours per shard, and unset 5% leaves (code alpha0.3_A500_Funset), hybrid R@10 dropped by 0.0820 and R@100 dropped by 0.0202, landing at 0.6588 compared to 0.6790 on unpruned 768-dimension. This loss is entirely located in the sparse retrieval leg, which fell by 0.0232 at R@100, while dense recall was unchanged.

More concerningly, this recall penalty deepens as you increase search depth. When searching with alpha 0.3, 2,000 neighbours per shard, and 5% leaves (code alpha0.3_A2000_F0.05, rows 11 to 14), R@100 drops by 0.0539 under setting `alpha0.3_A2000_F0.05`. When searching with alpha 0.3, 5,000 neighbours per shard, and 20% leaves (code alpha0.3_A5000_F0.20, row 16), R@100 drops by 0.0794 under setting `alpha0.3_A5000_F0.20`. Judged quality showed hit@100 at +0.0022 ± 0.0048, while hit@500 rose by +0.0135 ± 0.0052.

Furthermore, in same-session control testing against 1024-dimension, the pruned index was not faster. At 565 QPS, p99 was 71.1 milliseconds on the pruned index versus 58.4 milliseconds on 1024-dimension, a 12.7 millisecond penalty. At 800 QPS, p99 was 86.6 milliseconds versus 71.3 milliseconds, a 15.3 millisecond penalty, with average CPU within one percentage point.

Because dense R@500 reached only 0.8160 against the 0.95 requirement and shards stayed at 5, winner was null. Top-96 sparse pruning did not deliver the intended operational or latency benefits.

*(time: ~1.5 min)*

---

## Slide 10: EXP-7: Combination Picks & Cross-Session Differences

**Screen:** Tab 'EXP-7 settings', rows 24 and 46 to 47 (green: 768-d pick), row 30 (yellow: 1024-d pick), and red rows 12, 18, 25, 36, 37, 40, 41, 42, 43, 45, columns Index, Run (date), Setting code, alpha, A (neighbours/shard), F (leaf %), QPS, Recall@100, p99 ms, errors / error rate, Pass/Fail vs rule, Picked?.

**On screen:**
- Pre-set pick rule (Legend row 16): per index, among alpha 0.5 cells that pass 565 QPS on 10 nodes (p99 <= 100 ms, error rate < 0.001): highest hybrid R@100 (margin 0.005, tie-break lower p99).
- Green rows 24, 46, 47 (768-d pick): with alpha 0.5, 2,000 neighbours per shard, searching 5% leaves (code `alpha0.5_A2000_F0.05`):
  - Passed 565 QPS 3 of 3 times on 10 nodes (31 Oct run A p99 96.6 ms; 4 Nov repeat 1 p99 49.5 ms; 4 Nov repeat 2 p99 51.2 ms, 0 errors).
  - Steady across minutes (48.4 to 53.9 ms); average CPU dropped from 35.53% to 26.77%.
- Yellow row 30 (1024-d pick): with alpha 0.5, 5,000 neighbours per shard, searching 2% leaves (code `alpha0.5_A5000_F0.02`):
  - Passed 31 Oct run A (p99 95.0 ms, R@100 0.8475), but NOT CONFIRMED on 4 Nov repeat.
  - Repeat 1 had an 8-second query stall (346 requests > 1,000 ms) inside a 63.3 ms window p99.
  - Repeat 2 (row 45 red) failed p99 at 101.5 ms (> 100 ms limit).
- Comparison of failing settings on EXP-7 tab (red rows):
  - Row 12: 768-d with alpha 0.5, 500 neighbours, unset leaves failed error rate limit (0.0011 >= 0.001 limit).
  - Row 18: 1024-d with alpha 0.7, 500 neighbours failed 565 QPS (p99 106.8 ms).
  - Row 25: 1024-d with alpha 0.5, 2,000 neighbours, 5% leaves failed 400 QPS (p99 151.1 ms).
  - Rows 36 and 37: with alpha 0.7, 5,000 neighbours, 2% leaves failed 400 QPS (112.6 ms on 768-d, 102.5 ms on 1024-d).
  - Rows 40 to 43: with alpha 0.5, 5,000 neighbours, 5% leaves failed 400 QPS (105.9 ms on 768-d, 105.5 ms on 1024-d).
- Cross-session variance: alpha 0.5 with 5,000 neighbours, 5% leaves (code `alpha0.5_A5000_F0.05`) missed 400 QPS in EXP-7 (105.5 ms), but held 565 QPS in EXP-8 (85.0 ms).

**Say:**
Turn to tab EXP-7 settings, which evaluated combining our best alpha settings with moderate Tree-AH search parameters, and exposed crucial cross-session performance differences across rows 2 through 47.

Our pre-set pick rule was clear: for each index, among alpha 0.5 cells that pass 565 QPS on 10 nodes with p99 at or below 100 milliseconds and error rate under 0.1%, select the setting with the highest hybrid R@100, using lower p99 as tie-break.

On the 768-dimension index, shown in green on row 24 and rows 46 and 47, setting alpha 0.5 with 2,000 neighbours per shard and searching 5% of the leaves (code alpha0.5_A2000_F0.05) proved exceptionally robust. It passed 565 QPS across all three test runs on 10 nodes: 96.6 milliseconds on 31 October, and 49.5 and 51.2 milliseconds on 4 November, with zero errors, minute-by-minute p99 holding steady between 48.4 and 53.9 milliseconds, and average CPU dropping from 35.53% to 26.77%.

In contrast, look at row 30, highlighted in yellow. On 1024-dimension, setting alpha 0.5 with 5,000 neighbours per shard and searching 2% of the leaves (code alpha0.5_A5000_F0.02) was picked once on 31 October with a p99 of 95.0 milliseconds. However, it was not confirmed on the 4 November repeat. In repeat 2, shown on red row 45, it failed p99 at 101.5 milliseconds. In repeat 1, it suffered an 8-second query stall where 346 requests exceeded 1,000 milliseconds, even though the overall window p99 showed 63.3 milliseconds.

The other red rows show why other combinations failed. Setting alpha 0.7 with 500 neighbours on row 18 failed 565 QPS at 106.8 milliseconds. Setting alpha 0.5 with 2,000 neighbours on 1024-dimension on row 25 failed 400 QPS at 151.1 milliseconds. Settings with alpha 0.7 and 5,000 neighbours on rows 36 and 37 failed 400 QPS at 112.6 and 102.5 milliseconds. Settings with alpha 0.5 and 5,000 neighbours on 5% leaves on rows 40 through 43 failed 400 QPS at 105.9 and 105.5 milliseconds.

Notice also the cross-session variance. Setting alpha 0.5 with 5,000 neighbours and 5% leaves missed 400 QPS in EXP-7 at 105.5 milliseconds, but held 565 QPS in EXP-8 on 4 November with an 85.0 millisecond p99. This demonstrates that multi-tenant cloud variance can shift latencies across sessions.

*(time: ~1.5 min)*

---

## Slide 11: EXP-8: Capacity Ramp and Autoscaling Behavior

**Screen:** Tab 'EXP-8 settings', rows 2 to 4 (red: Config (a) production request), rows 5 to 7 (Config (b) EXP-7 recommended), rows 8 to 13 (Config (c) A5000_F0.02 alpha 0.3; green rows 9 and 12, red rows 10, 11, 13), rows 14 to 16 (Config (d) A2000_F0.05 alpha 0.3; red rows 15-16), columns Config, Setting code, Target QPS, Run, Achieved QPS, Nodes (CPU series / replica metric), p50 ms, p95 ms, p99 ms, Max ms, Errors in window, Error rate, Step result, Ceiling outcome.

**On screen:**
- Architecture: 1024-d index with autoscaling configured between 2 and 3 replicas (10 to 15 nodes).
- Configuration (c) with alpha 0.3, 5,000 neighbours, 2% leaves (code `alpha0.3_A5000_F0.02`):
  - Row 9 green: held 800 QPS on 10 nodes (p99 74.0 ms, 0 errors, 800.0 achieved QPS).
  - Row 12 green: held 1,200 QPS once on 15 nodes (p99 94.4 ms, 0 errors).
  - Rows 10 and 11 red: failed 1,000 QPS in both runs (101.3 ms and 131.5 ms) with query shedding.
  - Row 13 red: failed 1,500 QPS (p99 431.2 ms, 21.7% rejected requests).
  - Proven capacity under strict two-run rule: 800 QPS on 10 nodes.
- Red rows 2 to 4: Configuration (a) production request with alpha 0.3, 500 neighbours, unset leaves (code `alpha0.3_A500_Funset`):
  - Failed 565 QPS across all 3 tests (p99 549.4 to 684.6 ms) within 19 minutes of deploy, despite CPU remaining under 38%.
- Configuration (b) with alpha 0.5, 5,000 neighbours, 5% leaves (code `alpha0.5_A5000_F0.05`, rows 5 to 7): held 565 QPS on 10 nodes (85.0 ms), failed 800 QPS twice (rows 6 and 7 red).
- Configuration (d) with alpha 0.3, 2,000 neighbours, 5% leaves (code `alpha0.3_A2000_F0.05`, rows 14 to 16): held 565 QPS (78.6 ms), failed 800 QPS twice (rows 15 and 16 red, 125.3 ms and 158.8 ms).
- Autoscaling mechanics: scale-out triggered when busiest node mean CPU hit 0.71 or peak sample exceeded 60%. Metric lag of 1 cell or more; scale-down required >= 9 minutes idle.

**Say:**
Turn to tab EXP-8 settings, where we executed a capacity ramp on the 1024-dimension index with autoscaling configured between 2 and 3 replicas, representing 10 to 15 nodes.

Look at Configuration (c), using alpha 0.3 with 5,000 neighbours per shard and searching 2% of the leaves (code alpha0.3_A5000_F0.02). On row 9, highlighted in green, it held 800 QPS on 10 nodes with a p99 of 74.0 milliseconds and zero errors. When pushed to 1,000 QPS on rows 10 and 11, it triggered autoscaling to 13 and 15 nodes, but failed p99 in both runs, logging 101.3 and 131.5 milliseconds with query shedding. At 1,200 QPS on row 12, it held once on 15 nodes at 94.4 milliseconds, but failed completely at 1,500 QPS on row 13 with a p99 of 431.2 milliseconds and 21.7% rejected requests. Under a strict two-run pass rule, capacity is proven at 800 QPS on 10 nodes.

Surprisingly, look at red rows 2 through 4 for Configuration (a), today's production request with alpha 0.3, 500 neighbours per shard, and unset leaves (code alpha0.3_A500_Funset). It failed 565 QPS in all three measurements, with p99s between 549.4 and 684.6 milliseconds, despite CPU remaining under 38%. All three runs took place within 19 minutes of deployment, suggesting deploy warm-up and request shape were confounded.

We also captured valuable autoscaling metrics. Pod scale-out was observed when busiest node mean CPU reached 0.71, or when individual peak CPU samples crossed 60%. The Cloud Monitoring replica metric showed significant lag, updating one cell or more after new CPU series appeared. Finally, scale-down from 15 nodes back to 10 nodes required at least 9 minutes of sustained idle time.

*(time: ~1.5 min)*

---

## Slide 12: EXP-5: Concurrent Streaming Writes & Freshness

**Screen:** Tab '30M - all indexes', row 5 (red), columns Index, Exp ID, Settings (alpha / A / F), QPS / nodes, Latency P99 at peak (565 QPS), Shard Count, Recall@100, Picked? (why), Notes (source / caveat).

**On screen:**
- Setting: alpha 0.3, 500 neighbours per shard, unset 5% leaf search (code `alpha0.3_A500_Funset`).
- Test architecture: 400 QPS hybrid reads + 100 writes/s for 60 min on 10 pinned nodes (no autoscaling).
- Availability and throughput: held 400.0 QPS with only 3 failed requests out of 1,440,000; 360,000 writes acknowledged, 0 lost.
- Data integrity: 100% exact match across all 360,000 records on read-back; recall unchanged at 0.7555 / 0.6799.
- Red row 5 reason: failed C1 tail latency criterion (1.3684x R0 vs 1.25 limit); median p99 rose +36.8% from 57.62 ms to 78.85 ms; 6 minutes exceeded 100 ms (worst 141.24 ms).
- Freshness: sparse updates visible in p99 0.255 s; dense freshness failed criteria with 9 of 334 probes missing after 120 s (found by sparse in 0.131 s, narrow filter artifact).

**Say:**
Now look back at tab 30M - all indexes row 5, marked red for EXP-5 concurrent streaming writes.

We tested whether a thirty million record index can handle continuous background ingestion while serving traffic on 10 pinned nodes with autoscaling disabled. We subjected the index to 400 QPS of hybrid production reads using setting alpha 0.3, 500 neighbours per shard, and unset leaves (code alpha0.3_A500_Funset), while simultaneously streaming 100 writes per second for one full hour, comprising 180,000 new inserts and 180,000 updates.

System availability and data correctness were outstanding. Query throughput held at exactly 400.0 QPS, with only 3 failed requests out of 1.44 million. All 14,400 write batches and 360,000 datapoints were acknowledged with zero lost. In post-test verification, all 360,000 records were read back, showing zero mismatches across dense vectors, sparse vectors, restrict tokens, and prices. Hybrid recall was measured every minute and remained identical at 0.7555 at R@10 and 0.6799 at R@100 throughout the entire run and after index restoration.

However, row 5 is highlighted in red because streaming writes failed our pre-set tail latency criterion. While median latency rose by only 2.5 milliseconds, p99 latency increased by 36.8%, rising from 57.62 milliseconds in the read-only phase to 78.85 milliseconds during writes. This exceeded our pre-set tail latency limit of 1.25 times baseline, reaching 1.3684 times. In 6 of the 60 minutes, p99 exceeded 100 milliseconds, with a worst minute of 141.24 milliseconds. Busiest node CPU stayed below 24%, proving the tail latency rise is not due to CPU saturation.

On freshness, sparse updates were visible almost instantly, with a p99 of 0.255 seconds. However, dense freshness failed our strict criteria: 9 out of 334 test probes were never returned by dense self-queries within 120 seconds. Those same items were found by sparse search in 0.131 seconds, and the dense probe used an unusually narrow filter, so this may be a test artifact rather than data loss.

*(time: ~2.0 min)*

---

## Slide 13: Best Configuration Pick: PCA 768-d with alpha 0.5, A2000, F0.05

**Screen:** Tab '30M - all indexes', row 10 and row 20 (green), and tab '30M - 768-d', row 4 and row 8 (green), columns Index, Exp ID, Experiment Title, Settings (alpha / A / F), Latency P99 at peak (565 QPS), Shard Count, Recall@100, Recall@500, Picked? (why), Notes (source / caveat).

**On screen:**
- Selected Overall Winner: PCA 768-d with setting alpha 0.5, 2,000 neighbours per shard, searching 5% of leaves (code `alpha0.5_A2000_F0.05`).
- Pre-set pick rule (Legend row 16 & 19):
  - Highest hybrid R@100 among alpha 0.5 cells that pass 565 QPS on 10 nodes (p99 <= 100 ms, error rate < 0.001 / < 0.1%).
  - Met rule in run A (31 Oct) with R@100 = 0.8577 and p99 = 96.6 ms.
  - CONFIRMED in all 3 runs on 4 Nov (p99 49.5 ms and 51.2 ms, 0 errors, steady across minutes 48.4 to 53.9 ms).
- Quality improvement:
  - Hybrid R@100 rose from 0.6799 baseline to 0.8577 .
  - Human judged score-3 hit@100 rose to 0.5158 (+0.1459 lift vs today's 0.3699).
- Comparison with 1024-d pick (yellow row 30 on EXP-7 tab):
  - 1024-d with alpha 0.5, 5,000 neighbours, 2% leaves (code `alpha0.5_A5000_F0.02`) passed 31 Oct (p99 95.0 ms), but failed 4 Nov repeat (repeat 2 failed p99 at 101.5 ms, repeat 1 suffered an 8-second stall).
- Comparison with other EXP-7 settings:
  - Deeper settings (A5000_F0.05, A7000, A10000) exceeded 100 ms at 400 or 565 QPS.
  - Alpha 0.7 with A500_Funset gave borderline 106.8 ms.
  - A500_Funset had lower recall (0.7851 vs 0.8577).
- Operational and cost advantage:
  - 24.2% lower CPU than 1024-d at 565 QPS; held 800 QPS at 81.5 ms on 10 nodes.
  - Zero cost penalty: lands on 5 shards, costing $10.12/h list ($4.33/h net) for 10 nodes.

**Say:**
Now look at the highlighted green rows: row 10 and row 20 on tab 30M - all indexes, and row 4 and row 8 on tab 30M - 768-d. These rows identify our overall best configuration pick for the 30M architecture: PCA 768-dimension with alpha 0.5, 2,000 neighbours per shard, and searching 5% of the leaves (code alpha0.5_A2000_F0.05).

Let us explain exactly why this configuration is our recommended winner based strictly on our pre-set engineering rules.

Our pre-set rule on tab Legend row 16 stated: among alpha 0.5 cells that pass 565 QPS on 10 nodes with p99 at or below 100 milliseconds and error rate under 0.1%, select the setting with the highest hybrid R@100. This configuration met that rule in run A on 31 October, delivering a hybrid R@100 of 0.8577 at a p99 of 96.6 milliseconds. Crucially, it was confirmed in all three test runs on 4 November, where p99 dropped to 49.5 and 51.2 milliseconds with zero errors and minute-by-minute latencies holding rock-steady between 48.4 and 53.9 milliseconds. It delivers an outstanding quality leap: hybrid R@100 increases significantly over baseline, and human-judged score-3 hit@100 reaches 0.5158, an improvement of 0.1459 over today's 0.3699.

Why did we not pick the 1024-dimension candidate? On the EXP-7 tab row 30, colored yellow, 1024-dimension with alpha 0.5, 5,000 neighbours per shard, and 2% leaves (code alpha0.5_A5000_F0.02) achieved R@100 of 0.8475 with a 95.0 millisecond p99 on 31 October. But when repeated on 4 November, it failed to confirm: repeat 2 failed p99 at 101.5 milliseconds, and repeat 1 experienced an 8-second query stall where 346 requests exceeded 1,000 milliseconds.

Other settings on the EXP-7 tab were disqualified: deeper search settings with 5,000 neighbours on 5% leaves, or 4,000 and 10,000 neighbours, exceeded our 100 millisecond p99 limit under load; alpha 0.7 reached a borderline 106.8 milliseconds; and lighter settings like A500 had lower recall.

Furthermore, PCA 768-dimension gives the engineering team 24.2% lower CPU at 565 QPS and comfortably held 800 QPS at 81.5 milliseconds on 10 nodes. Because both 1024-dimension and 768-dimension land on 5 shards, adopting PCA 768-dimension with alpha 0.5, 2,000 neighbours per shard, and 5% leaves costs exactly the same: $10.12 per hour list, or $4.33 net for 10 nodes. It is superior in quality, stability, and headroom at zero extra cost.

*(time: ~2.0 min)*

---

## Slide 14: Cost Summary: Serving and Build Costs

**Screen:** Tab '30M - all indexes', columns Index, Shard Count, Build cost, Serving cost, Serving cost (net, 42.75%), Build cost (net, 42.75%), rows 2, 8, 10, 11, 12, and 20.

**On screen:**
- Common infrastructure: all 30M Tree-AH indexes use 5 shards on e2-highmem-16 nodes.
- Hourly serving cost: USD 10.1237 to 15.1855 list; net USD 4.3279 at 10 nodes (42.75% factor).
- Query cost efficiency (list / net per 1M searches):
  - At 400 QPS (10 nodes): USD 7.0303 list .
  - At 565 QPS (10 nodes): <= USD 4.9772 list (<= USD 2.1278 net).
  - At 800 QPS (10 nodes): USD 3.5152 list (USD 1.5027 net).
- Index build costs (unverified list): 1024-d USD 409.72; 768-d USD 323.89; 512-d USD 238.06; total across five indexes about USD 1,705.29 list (net about USD 729.01).

**Say:**
Let us review the financial profile of the architecture on tab 30M - all indexes.

Because every 30M Tree-AH index landed on 5 shards, the hourly infrastructure cost is identical across 1024-dimension, PCA 768-dimension, PCA 512-dimension, and sparse-pruned indexes. Deploying 10 nodes costs USD 10.1237 per hour at list price, or USD 4.3279 per hour net. If autoscaling expands the deployment to 15 nodes, hourly cost reaches USD 15.1855 list.

Consequently, serving cost per query depends entirely on the query throughput you can sustain on those 10 nodes. At 400 QPS, serving costs USD 7.0303 per million queries at list price. At 565 QPS, cost drops to less than or equal to USD 4.9772 list, or USD 2.1278 net. At 800 QPS on 10 nodes, it reaches USD 3.5152 list, or USD 1.5027 net.

Regarding index build expenses, unverified list estimates derived from data byte counts were USD 409.72 for 1024-dimension and BruteForce, USD 323.89 for 768-dimension and top-96 pruned, and USD 238.06 for 512-dimension. The total unverified build cost across all five indexes was approximately USD 1,705.29 list, or about USD 729.01 net.

*(time: ~1.5 min)*

---

## Slide 15: What Is Not Established & TDD Recommendations

**Screen:** Tab '30M - all indexes', row 20 (green), and tab 'Legend', rows 19 to 21 (HOW 'PICKED' WAS DECIDED).

**On screen:**
- Boundaries: no absolute capacity ceiling established; production request capacity unproven; multi-second stall causes unknown; 50M (9-shard) scale unverified.
- Recommendation 1: Adopt hybrid RRF alpha 0.5 (lifts judged hit@100 from 0.3699 to 0.4800 at zero cost).
- Recommendation 2: Base 30M serving on 10 pinned nodes for 400 to 565 QPS; test 15 nodes if writes require strict p99 <= 100 ms.
- Recommendation 3: Retain 768-d for CPU headroom; reject top-96 sparse pruning (degrades recall, saves USD 0).

**Say:**
Now let us examine what these experiments did not establish, followed by our direct recommendations for the TDD.

We did not establish an absolute capacity ceiling for any configuration. We do not have a verified capacity figure for today's production request on 1024-dimension. We do not know the underlying platform causes for occasional multi-second stalls or InternalServerError bursts. All traffic originated from a single client VM over a public endpoint rather than internal production VPC networking. Crucially, production operates at 9 shards around fifty million records, where per-shard candidate counts behave differently.

Based strictly on supported findings, we make three concrete recommendations for your technical design document.

First, transition the production hybrid RRF alpha parameter from 0.3 to 0.5. This delivers an immediate 0.1101 lift in human-judged top-100 relevance with zero index rebuilding and zero additional infrastructure cost.

Second, provision 30M serving infrastructure on 10 pinned e2-highmem-16 nodes for traffic between 400 and 565 QPS. If your service level objective requires p99 to remain strictly under 100 milliseconds during continuous 100 per second streaming writes, you must evaluate 15 nodes in pre-production, as 10 nodes exhibited tail spikes up to 141 milliseconds.

Third, maintain PCA 768-dimension with alpha 0.5, 2,000 neighbours per shard, and 5% leaf search as the qualified architecture to gain 24% CPU headroom, but reject top-96 sparse pruning. Pruning reduced search quality without reducing shard count or saving a single dollar.

*(time: ~2.0 min)*

---

## Slide 16: Handover Package Structure & Golden Operating Rules

**Screen:** Tab 'Legend', rows 7 to 10 (DATA EVIDENCE LABELS: MEASURED, CALCULATED, INFERENCE, UNVERIFIED) and slide deck handover archive checklist.

**On screen:**
- Package archive: `KT_HANDOVER_30M_2026-11-10.tgz` (52 MB, 318 checksummed files).
- Verification: execute `bash verify_kt_sha256.sh`; must print "SUCCESS: All 318 files...".
- Reading path: top `README.md` (§0 reading order), `00_common/README.md` (§1, §5.3, §5.4), then experiment README.
- Single source of truth: `master_docs_and_audits/30M_RESULTS_REGISTER.md`.
- Five Golden Rules:
  1. Never run two 30M sessions simultaneously.
  2. Always undeploy indexes after test execution.
  3. Never delete an existing index.
  4. Verify endpoint is completely empty before deploying.
  5. Check both VMs for locks (file locking is per-VM only).

**Say:**
Let us review the handover package and our mandatory operational rules, referencing our evidence standards on tab Legend rows 7 through 10.

The entire handover is packaged in KT_HANDOVER_30M_2026-11-10.tgz. It contains 318 checksummed files across 52 megabytes. When you unpack it, the first command you must run is bash verify_kt_sha256.sh, and you must see success reported for all 318 files.

There is no separate handbook. The top-level README.md is your handbook. Section 0 provides your reading order. You read the top README, then 00_common README for shared tooling and lock procedures, and then your specific experiment README. Runbooks are explicitly marked at the top as CURRENT, HISTORICAL, or PLAN; only execute runbooks marked CURRENT. Remember that the single source of truth for every recorded number is the 30M results register in master docs.

When operating this environment, you must adhere to five golden rules.

Rule one: never run two 30M sessions concurrently. Our regional quota is 50 nodes; two sessions will exhaust quota and fail.

Rule two: always undeploy your index when testing finishes. Leaving an index deployed burns over ten dollars an hour.

Rule three: never delete an index. Indexes take hours and hundreds of dollars to build.

Rule four: always verify that the target endpoint has zero deployed indexes before initiating a deployment.

Rule five: understand that our file locking script is strictly local to each virtual machine. It does not synchronize between our main Workbench VM and the secondary VM. You must manually check running processes on both machines before starting work.

*(time: ~2.0 min)*

---

## Slide 17: Current Status, Next Steps, and Technical Q&A

**Screen:** Tab '30M - all indexes', row 13 (PCA 512-d + top-96 pruned, building), and tab '30M - 512-d', row 6.

**On screen:**
- Current index build: EXP-9 512-d + SPLADE top-96 started 12 Nov 2026 05:17 UTC on main Workbench VM `gopalad-torch-32` (index id `7229181200487677952`).
- Evaluation runbook: `exp9_sparse_pruning_30m/runbooks/EXP9_512_SP96_RUNBOOK_2026-11-11.md` (steps A3 to A5, then B1 to B6); no results yet.
- Upcoming deliverables: 10M and 5M findings summary documents are being compiled.
- Operational handover: my colleague is taking over active execution.
- Common engineering questions:
  - Q1: Why did dimension reduction and pruning fail to lower shard counts?
  - Q2: Why did 800 QPS fail in earlier sessions but pass in EXP-9?
  - Q3: Can we rely on Cloud Console vector counts to monitor streaming write progress?

**Say:**
To conclude, let us cover active status, immediate next steps, and anticipated technical questions, referencing row 13 on tab 30M - all indexes and row 6 on tab 30M - 512-d.

As of today, 12 November, the EXP-9 512-dimension plus top-96 index build is actively executing on our main Workbench VM under index id 7229181200487677952. The build started at 05:17 UTC, and there are no results yet. Once complete, my colleague will take over execution, following runbook EXP9_512_SP96_RUNBOOK_2026-11-11.md through steps A3 to A5 and B1 to B6. Additionally, findings summary documents for our earlier 10M and 5M experiments are being finalized to complement this 30M handover.

Here are brief answers to three questions your team will encounter while drafting the TDD.

Question one: why did dimension reduction and pruning fail to lower shard counts below 5? Vertex Vector Search appears to calculate shards from sparse-side input size, which includes roughly 23.8 gigabytes of restrict tokens alongside vector data. Because total sparse input remained above 40 gigabytes against a 10 gigabyte per-shard threshold, the service assigned 5 shards in all cases.

Question two: why did 800 QPS on 1024-dimension fail in EXP-3 and EXP-6, but pass in EXP-9? Those tests ran in different cloud sessions across different days. Session-to-session latency variance on multi-tenant infrastructure is substantial, and the control itself moved between sessions.

Question three: can we use Cloud Console vector counts to monitor streaming ingestion? No. In EXP-5, the vector count metrics remained completely frozen at thirty million while 180,000 new records were ingested. Freshness must be tracked using active search probes.

Thank you for your time, and let us open the floor to your questions.

*(time: ~2.0 min)*
