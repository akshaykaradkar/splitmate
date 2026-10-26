# 10M findings presentation: speaking script, coaching notes and tough Q&A

*For the 30-minute call with Glance. Matches the live sheet "10M-EXP-Findings" (tabs: Setup · 5M vs 10M · Summary & takeaways · Combination & best config). Every number is on the sheet or in the audited 10M findings. Things that are no longer on the sheet (some caveats, the cheaper option, next steps, asks) are in the script so you say them out loud.*

---

## Part A. Five things to do better than last time

1. **Lead with the answer.** Give the recommendation in the first two minutes, then show the evidence.
2. **Never guess.** Last time "was PCA only for sparse?" got a quick "yes", and it was dense only. If you're not 100% sure: *"Let me confirm that and send it after the call."* That sounds stronger than a wrong yes.
3. **One number, then what it means.** "About two thirds of the best 100 results", not "0.640 recall@100 vs BruteForce".
4. **Say the caveats yourself, early and calmly.** Said first, they sound like rigour, not weakness.
5. **Stop and ask.** After each tab: *"Any questions before I move on?"* Talk less than 60% of the time.

The script is about 18 minutes of speaking, which leaves about 10 minutes for questions.

---

## Part B. Before the call (2 minutes)
- Summary & takeaways F7: typo "reuslts" → "results".
- The sheet mixes "image / keyword leg" and "dense / sparse leg". Say it once at the start (it's in the opening below).

---

## Part C. Speaking script

### 0. Opening (1 min)
> "Thanks everyone for joining. So today I'll take you through the 10M results, and how they compare with the 5M ones we shared last week.
>
> Let me give you the short answer first. At 10M we see pretty much the same picture as at 5M, just a bit better. Today's search request finds about two thirds of the best 100 results. With two small changes in the request, and no rebuild, we get that up to about 89%. And we find about 22 more highly relevant products in every 100. The cost is roughly 16 milliseconds more for the slowest 1% of requests at peak. We also tried shrinking the vectors to 768. Quality stayed the same, but we didn't get any speed or node saving at 10M.
>
> Quick note on words: when I say 'dense', I mean the image-text vector, and 'sparse' is the keyword vector. I'll use both.
>
> I'll go through the setup, then 5M versus 10M, then a one-page summary, and then the combined test and the configuration we'd recommend. Please jump in any time."

### 1. Tab "Setup" (3 min)
> "So, first the setup. We used 9.9 million of your products, basically the first third of the 30 million set you gave us. About 26% of them aren't servable, meaning inactive or out of stock, which is close to what you see in production. Part of that we did on purpose: we marked 11.2% as inactive to match your share.
>
> The index is exactly your production configuration. Tree-AH, leaf size 7,000, 5% of the index searched per request, 500 candidates per shard, streaming updates, and the same e2-highmem-16 machines.
>
> At 10M it ended up with 2 shards, with 2 replicas each, so 4 serving nodes. At 5M it was 1 shard and 2 nodes. You have 9 shards in production. So 10M is closer to one of your production shards, but it's not the full production layout yet. I'll come back to that.
>
> We also built a second index with the same products, where we shortened the dense vectors from 1024 to 768 numbers using PCA. And just to be clear on this: we only reduced the dense vectors. The keyword vectors weren't touched at all.
>
> For the 'right answer' we use a full-scan BruteForce index. For the dense side it's exact. For the keyword side it's close but not exact: it finds about 84% of the true top 100. So when I say recall today, I mean how much we agree with that full scan.
>
> Same 2,000 golden queries as before, and your production request: hybrid, RRF alpha 0.3, 500 results, with your filters.
>
> And for latency, we send a fixed number of requests per second: 81, which is your average, 565, your peak, and also 400 and 800. Each one runs for 3 minutes after a 2-minute warm-up. P99 just means 99% of requests finish within that time. We've been using 100 milliseconds as a working limit until you tell us your actual budget."
>
> *(Pause.)* "Any questions on the setup before I move on?"

### 2. Tab "5M vs 10M" (8 min)

**EXP-1 baseline**
> "Okay, EXP-1 is simply today's request, nothing changed. At 5M it found about 55% of the true top 100. At 10M it's about 64%. And it's higher at every depth: top 10, top 100, top 500.
>
> Now you might ask why it's higher with more data. Our best explanation, and I want to be clear it's a hypothesis, is that the 500 candidates are collected per shard. 10M has 2 shards, so in total it's looking at about 1,000 candidates. And when we gave 5M 1,000 candidates on its one shard, it came out almost exactly the same as 10M.
>
> On latency, it holds your peak with zero errors at both scales. The 37.9 milliseconds at 10M is a re-measurement. The very first 10M reading wasn't usable because one serving node was much busier than the other three. Also, P99 moves around by about 10 milliseconds from one run to the next, even for the same request. So I wouldn't read 50 versus 38 as a real difference between 5M and 10M."

**EXP-2 RRF alpha**
> "EXP-2 is about RRF alpha, so how much weight goes to the dense side versus the keyword side. Today it's 0.3, which means 70% keyword.
>
> What we saw is that as alpha goes up, recall against the full scan goes up too: about 0.64 at 0.3, 0.76 at 0.5, and 0.85 at 0.7. Same pattern at 5M. Mostly that tells us the dense side is searched more accurately than the keyword side.
>
> But the more interesting part for relevance is this. Glance already grades search results with its relevance judge, 0 to 3. For each test query we took the products graded 3, highly relevant, and checked how many of them our search puts in the top 100. Today it's about half, 0.49. With alpha 0.5 it's about two thirds, 0.64. With the best config, which I'll show at the end, it's 0.71. And alpha 0.5 came out on top at 5M as well.
>
> I should say it's a small sample, about 750 queries, and only products production already showed were graded. So I'd treat it as a strong direction, not proof.
>
> The trade-off is that alpha 0.5 changes about half of what users see in the top 10. That's why we'd suggest an online A/B test before rolling it out."

**EXP-3 search effort**
> "EXP-3 looks at how hard each request searches. No rebuild, just settings in the request: how many candidates per shard, and how much of the index to scan.
>
> And the candidate count is really the main lever here. With today's 500 candidates we're at 0.64. With 2,000 we get to 0.80, and that held 800 requests per second. With 5,000 at today's 5%, we get to 0.90, and that held your peak.
>
> I want to be upfront about that last one though. It held the peak only just, at 99 milliseconds, and one earlier run actually failed at 400 requests per second. Single runs close to the limit are a bit noisy. That's why the combined test I'll show later matters, because there the same setting held the peak at 64 milliseconds."

**EXP-6 768-d**
> "EXP-6 is the dimension reduction, 1024 down to 768 numbers.
>
> For today's request, recall didn't change at all: 0.640 versus 0.640. When you look at the dense side on its own, it does lose a bit of depth with only 500 candidates, but with 2,000 or more candidates that loss goes away.
>
> One thing I want to correct from 5M. Back then, 768 looked 13 to 19% faster, but that was comparing runs on different days. At 10M we measured both indexes in the same session, and 768 wasn't faster: about 50 versus 48 milliseconds at peak, and a bit slower per single request. The day-to-day variation is simply bigger than the effect. And we didn't save any nodes either; it's still 2 shards and 4 nodes.
>
> So if 768 has a benefit, it's more about memory and index size at a much bigger scale, not speed. That's something we'll check at 30M."

**Same vs changed**
> "So, in one line: the conclusions are the same at both scales. Recall is just higher at 10M, and the 768 speed gain didn't hold up in a fair test."
>
> *(Pause.)* "Any questions before I go to the summary?"

### 3. Tab "Summary & takeaways" (4 min)
> "This page is basically everything on one screen, one line per experiment.
>
> EXP-1: today we miss about a third of the true top 100.
>
> EXP-2: alpha 0.5 finds more of the highly relevant products, about 64 versus 49 out of every 100, but it changes half of the top 10.
>
> EXP-3: the number of candidates per shard is the lever, up to about 0.90 with 5,000.
>
> EXP-6: 768 keeps recall, but no speed or node saving at 10M.
>
> And EXP-7, the green row, is everything together. The best result we got is keeping 1024, alpha 0.5, 5,000 candidates per shard, and today's 5% of the index. That gives about 22 more highly relevant products in every 100, for roughly 16 milliseconds more at P99.
>
> A few things I'd keep in mind. First, our index has 2 shards and you have 9. The candidate setting works per shard, so the right number really needs to be confirmed on a bigger index before going to production. The 30M index is already built for exactly that. Second, each load level was measured once, and P99 moves by about 10 milliseconds between runs. Third, if you ever go with 768, the same projection has to run on every query and every product update.
>
> And two more that aren't on the sheet: the full-scan answer isn't exact for keywords, so recall means agreement with it; and the 100 milliseconds is our working assumption until we hear your latency budget."

### 4. Tab "Combination & best config" (4 min)
> "And the last page, the combined test, EXP-7. Here we ran both indexes and every combination in one session on 22 October, so all the latencies here are directly comparable.
>
> Let me walk you through it step by step. Today we're at 0.64 recall, 49 highly relevant out of every 100, and 48 milliseconds at peak.
>
> Step one, move alpha to 0.5: recall goes to 0.76, highly relevant goes up by 15, and we're at 60 milliseconds.
>
> Step two, add 5,000 candidates per shard: recall 0.89, highly relevant up by another 7, so 71, and 64 milliseconds.
>
> Step three, switch to 768: quality doesn't change, and it gets slower, 80 milliseconds.
>
> So the configuration we'd recommend is: keep 1024, alpha 0.5, 5,000 candidates per shard, and keep 5% of the index searched. These are all per-request settings in your search call, so no rebuild. Zero errors, on the same 4 nodes. And the choice is quite robust: it comes out on top whether we look at the top 10, 50, 100 or 500.
>
> The one thing to watch is that about half of today's top 10 changes, which again is why we'd A/B test it.
>
> Next on our side: we'll run this configuration and the key settings on the 30M index. It's already built, with 5 shards, which gets us closer to your 9. There's also a cheaper option with 2,000 candidates, which we want to repeat once to be sure."

### 5. Close and asks (2 min)
> "So to wrap up. Today's request leaves a real recall gap. Two per-request settings close most of it, at a small latency cost. And 768 isn't worth it at this scale.
>
> Three things that would really help us: your P99 latency budget, your recall target if you have one, and the RRF alpha you're using live today. And for the read-and-write test we're planning on 30M, your real write pattern: peak writes per second, and roughly how many inserts, updates and deletes.
>
> That's it from my side. Happy to take questions."

---

## Part D. Tough questions, with conversational answers

### About the results
1. **"Was PCA applied to the sparse vectors?"**
   > "No, only the dense ones. We reduced the image-text vectors from 1024 to 768, and left the keyword vectors exactly as they were. You can actually see that in the results: the keyword results were identical on both indexes for all 2,000 queries. PCA works on fixed-length vectors, and a sparse vector only stores its non-zero keyword weights, about 110 per product, so it doesn't really apply there."
2. **"Why is 768 not faster? Fewer numbers should be faster."**
   > "You'd think so. But comparing vectors is only a small part of a request. Tree-AH already scores with compressed vectors, and every hybrid request also does the keyword search, the fusion, the filtering and the network round trip. Making the dense vector 25% smaller barely moves the total. Where it could help is memory and size at very large scale, which we'll look at on 30M."
3. **"At 5M you said 768 was faster. Which is right?"**
   > "The 10M one. At 5M we compared runs from different days, and day-to-day variation is about 10 milliseconds, which is bigger than the effect. At 10M we measured both in the same session, so that's the fair comparison."
4. **"Why is recall higher at 10M? More data should be harder."**
   > "That surprised us too. Our hypothesis is that candidates are collected per shard. 10M has 2 shards, so 500 per shard is about 1,000 in total, and 1,000 candidates at 5M gave almost exactly the 10M numbers. We haven't proven it yet; 30M with 5 shards will tell us."
5. **"So with 9 shards, production already looks at 4,500 candidates. Is recall better in production than what you measured?"**
   > "Probably, if that per-shard effect holds. And that's exactly why we don't want to copy 5,000 straight into production. We want to confirm it closer to 9 shards first, which is the 30M test."
6. **"Is BruteForce really the truth?"**
   > "For the dense side, yes, it's exact. For the keyword side it's close: about 91% of the true top 10 and 84% of the top 100. We checked that by computing exact scores over all 9.9 million products for a sample of 50 queries. That's why we also show the highly relevant numbers from your own judge."
7. **"What exactly is 'highly relevant found in the top 100'?"**
   > "Glance already grades search results with its relevance judge, 0 to 3. For each test query we took the products graded 3, highly relevant, and checked how many of them our search puts in the top 100. Today it's about half, 0.49. With alpha 0.5 it's about two thirds, 0.64. With the best config, 0.71."
   - *If they push:* "It's an average share of products, not a share of queries. About 750 queries have such products, and only results production already served were graded, so it's a direction, not proof."
8. **"Who graded score 3? Humans?"**
   > "No, it's your production LLM judge's grade, 0 to 3, on the results production served. We used it as it came."
9. **"Why alpha 0.5 and not 0.7? 0.7 has higher recall."**
   > "Good question. Recall at each alpha is measured against a different answer, so they're not really comparable. If you look at the highly relevant products, 0.5 comes out on top. And 0.7 changes about 90% of today's top 10 and had a higher P99 at peak."
10. **"Why not the setting with the highest recall, 5,000 candidates and 20% of the index?"**
    > "It did reach 0.91, but it only held 81 requests per second on 4 nodes and failed at 400. So it just doesn't work at your peak."
11. **"How confident are you in these latency numbers?"**
    > "Each load level was measured once, 3 minutes after a warm-up, and P99 moves about 10 milliseconds between runs. So we only compare settings within one session, and the recommendation comes from the combined test where everything ran together."
12. **"The baseline at 565 QPS was 3.6 seconds. Is that a Vertex problem?"**
    > "What we saw was one serving node much busier than the other three, while our load generator was healthy. We re-measured the same request on the same nodes three times and got 38, 51 and 38 milliseconds. We don't know the cause, and we haven't seen it again since."
13. **"Did autoscaling kick in?"**
    > "Yes. The deployment is set to 2 to 3 replicas per shard, and we saw extra pods appear in a few cells. We flagged those, and the recommended setting ran on 4 nodes."
14. **"How representative are the 2,000 golden queries?"**
    > "They're your own sample from production traffic, 2 to 20 September: about 1,200 US and 800 India, spread across country, quality and query length, and they deliberately include about 580 poorly performing queries. We didn't apply the gender and category deny lists, but that's the same for both the test and the ground truth."
15. **"What does this cost us?"**
    > "Same nodes. The recommended setting ran on the same 4 nodes with zero errors. What we haven't checked yet is how much head-room it leaves above 565, and we'll look at that on 30M."

### Code and implementation
16. **"Where do we change this in our code?"**
    > "In the FindNeighbors query your retrieval service builds. You already set the RRF alpha there, so that goes to 0.5, and you add `approximate_neighbor_count` of 5,000 on the query. The leaf fraction stays at the index default of 5%, so no override needed. And `neighbor_count` stays at 500. No rebuild."
17. **"Does approximate_neighbor_count have to be at least neighbor_count?"**
    > "Yes, otherwise the request gets rejected. 5,000 is well above 500, so that's fine."
18. **"Do the restricts or crowding change?"**
    > "No, same filters. We didn't set `per_crowding_attribute_neighbor_count`; if you use it in production, just tell us the value and we'll include it."
19. **"How would PCA fit into our pipeline?"**
    > "You'd have one fixed projection matrix, applied in two places: in the query encoder before the search call, and in the ingestion job before every upsert. Plus a new 768 index and a new query cache key, because the cached 1024 vectors wouldn't match. But since it didn't give us anything at 10M, we wouldn't recommend it right now."
20. **"Could we use a smaller CLIP model instead of PCA?"**
    > "It's possible. We actually noticed a 768 FashionSigLIP path in your ingestion code. That would mean re-embedding the catalogue and the queries, and judging quality with relevance data, because the full-scan answer would come from a different model. Can I ask what's driving it: cost, latency or quality? We'll come back with options."
21. **"Will streaming updates behave the same?"**
    > "We haven't tested writes yet. That's the read-and-write test we're planning on 30M. We've already checked our write quotas, and what would help is your real write pattern so we can size it properly."

### If you don't know
> "That's a good question, and I don't want to guess. Let me confirm and send it to you after the call."
