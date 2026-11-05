# One-on-one script: Glance, Workstream 2 (WS2)

*About 6–8 minutes. First person, written to be read aloud. Only public facts about Glance; no confidential numbers (costs, Glance production figures, internal Google details).*

---

## 0. About the client (30 seconds)
"I'm deployed with Google Cloud as part of their Professional Services team, and the client I support is **Glance**.

Glance is a consumer technology company in the InMobi group, headquartered in Singapore, with large operations in India. It started as a smart lock screen platform on Android phones that shows personalised content: news, sports, entertainment and shopping. It comes pre-installed through phone makers and telecom partners. As of early 2025 it was on more than 450 million phones, with over 300 million users across India, Indonesia, Japan and the US.

Over the last two years Glance has been repositioning as an **AI-powered personal shopping platform**. Its product, Glance AI, offers personalised looks, virtual try-on and shop-the-look, on phones and now also on smart TVs, with partners like Samsung and Android TV.

Google has been an investor in Glance since 2020. In 2025 Glance and Google Cloud announced a strategic partnership to build these consumer AI experiences on Google Cloud's AI platform."

## 1. Business context (1 minute)
"Shopping on Glance depends on one core capability: **product search and recommendation**. When a user sees a look or searches for something, the system has to find the most relevant products from a catalogue of tens of millions of items in a fraction of a second, and only show products that are in stock, active and available in the user's country.

Glance runs this on Google Cloud's **Vertex AI Vector Search**. It's a hybrid setup: it combines a semantic, image-and-meaning based match with a keyword-style match, and applies business filters on top.

The catalogue is growing fast, from about 50 million products today towards around 200 million planned. So Google set up a proof-of-concept engagement with several workstreams. I work on **Workstream 2**, which is about making this search platform ready for that scale in quality, speed and cost."

## 2. Problem statement (1 minute)
"The problem was that Glance needed clear, evidence-based answers before scaling up:
- How good is the search quality today? Are users actually seeing the most relevant products?
- If the catalogue grows four to six times, will latency stay within budget at peak traffic?
- What will the infrastructure cost, and which settings give the best balance of quality, speed and cost?
- Can techniques like reducing the size of the product vectors cut cost without hurting results?

Without this, Glance risked either over-spending on infrastructure, or showing less relevant products, which directly affects shopping conversion and revenue. There was also uncertainty about how the platform splits data across machines, which drives cost."

## 3. Work and solution (2 minutes)
"My role was to design and run a structured benchmark programme on a realistic copy of Glance's catalogue, at 5, 10 and 30 million products, using real search queries and the same filters as production.

The key experiments were:
1. **A production baseline**, to measure today's search quality, latency and cost.
2. **Hybrid search tuning**: finding the best mix between the semantic and keyword parts of the search.
3. **Algorithm tuning**: testing how much search effort per query gives the best quality for the latency.
4. **Dimensionality reduction**: shrinking the product vectors from 1024 to 768 and 512 dimensions, and measuring the effect on quality, speed and build cost.
5. **Combined best configuration**: putting the winning settings together and confirming them under peak load.
6. **Capacity testing**: increasing traffic step by step to find how much load a given cluster can handle, and the cost per million searches.

How I approached it:
- Every experiment had a written plan, a runbook, and fixed pass/fail rules defined before the run.
- I built automation scripts for deployment, load testing and reporting, with safety checks so experiments could never overlap or leave expensive machines running. For long runs I added automatic shutdown once the test finished, which kept costs under control.
- Every result went through an independent review before it was reported, and I keep a single results register so the team always works from the same audited numbers.

How it helped:
- We now know exactly where quality can improve. By changing only query settings, with no extra infrastructure, the share of relevant products in the top results went up noticeably over today's configuration.
- We identified a recommended configuration and confirmed it in repeated runs.
- We found that reducing vector size saves on build cost but doesn't reduce serving machines at this scale. We traced the reason to how the platform sizes its data partitions, and we're now confirming that with Google's product team."

## 4. Expected outcome (1 minute)
"The expected benefits for Glance:
- **Better shopping relevance**: more of the right products shown to users, which should support engagement and conversion.
- **Predictable cost at scale**: clear cost per million searches and capacity per cluster, so they can plan the move to a much larger catalogue with confidence.
- **Faster, data-driven decisions**: instead of trial and error in production, they have tested settings, documented trade-offs and ready-to-use runbooks.
- **Operational efficiency**: the automation and runbooks are reusable, so their team can repeat these tests as the catalogue grows.

Next, we're planning a test of search under live catalogue updates at peak traffic, which is the last big risk before production rollout. The findings will go into a technical design document for Glance."

## 5. Key learnings (1 minute)
"Main learnings:
- **Technical:** deep, hands-on knowledge of Vertex AI Vector Search: hybrid search, approximate nearest-neighbour tuning, how data partitioning drives cost, and how to measure search quality properly against an exact reference.
- **Benchmarking discipline:** define pass/fail rules before testing, repeat important results, and separate what's measured from what's assumed. Several findings changed after a repeat run, so that discipline mattered.
- **Cost awareness:** in cloud work, every hour of running infrastructure costs money, so planning, automation and automatic shutdown are as important as the experiment itself.
- **Stakeholder communication:** turning complex technical results into simple tables and clear recommendations for the client team, and asking the product team precise, evidence-backed questions."

## 6. Challenges and mitigation (1 minute)
"A few main challenges:
1. **Machine availability:** at times the cloud region didn't have enough capacity for our large test clusters, and a deployment failed. I added retry steps to the runbooks, planned runs for quieter hours, and for the next large test we're confirming capacity with Google in advance.
2. **Results that didn't repeat:** one configuration passed once and failed in later runs. Instead of reporting the first good number, I repeated tests and only recommended what passed consistently.
3. **Long, expensive runs:** some experiments take eight hours or more. I built an automatic watcher that saves the results and shuts everything down when the test ends, even overnight, so we never paid for idle machines.
4. **Unexpected platform behaviour:** reducing vector size didn't reduce the number of machines, which went against expectations. I measured the data precisely, analysed the cause, and took a clear, data-backed question to Google's product team.
5. **Working across many parallel tasks:** I kept a detailed daily log and a single results register, so nothing gets lost and everything is ready for the final documentation.

The overall lesson: in a client engagement, being careful and transparent about what's proven and what isn't builds trust. That matters more than moving fast with numbers that aren't confirmed."

---

### Tips
- If time is short, sections 2, 3 and 4 matter most.
- Check with your PwC or Google engagement lead whether the project specifics are OK to discuss outside Google.
- Public sources for the Glance facts:
  - Google Cloud press release (Feb 2025): https://www.googlecloudpresscorner.com/2025-02-26-Glance-and-Google-Cloud-Join-Forces-to-Build-Consumer-Generative-AI-Experiences-for-Both-Smartphone-Lock-Screens-and-Ambient-TV-Screens
  - Glance on Wikipedia: https://en.wikipedia.org/wiki/Glance_(company)
