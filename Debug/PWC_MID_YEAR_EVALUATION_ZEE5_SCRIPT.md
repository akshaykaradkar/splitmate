# 🎙️ PwC Mid-Year Evaluation 1-on-1 Script — ZEE5 AI Search Engagement

**Engineer:** Akshay Karadkar (AI Engineer — PwC xWF deployed at Google Cloud)  
**Client Account:** ZEE5 (Zee Entertainment Enterprises Ltd.)  
**Project Name:** `ZEE5-Conversational-Media-Search` (Agentic Multimodal Voice & Text Discovery Platform)  
**Google Tech Lead:** Arjun Vijayvargiya (Cloud Engineer, Application Development, Google)  
**Key Recognition:** 🏆 **2026 Q3 Google AI Awards — SWEETY Silver Award Winner**  

---

## 🗣️ Opening Introduction (20 Seconds)

> *"Hi [Manager's Name], thanks for setting up this mid-year connect. Deployed as an AI Engineer at Google Cloud, I have been working on a flagship generative AI initiative for **ZEE5**, called **ZEE5 Conversational Media Search**. It has been an incredible engagement—not only did we successfully deliver and deploy the platform under tight timelines, but our project was also selected as a **SWEETY Silver Award Winner in the 2026 Q3 Google AI Awards**, and we received formal appreciation from our Google Tech Lead, Arjun Vijayvargiya. Let me walk you through the engagement across the six agenda points."*

---

## 1. Business Context (Client Overview & Project Background)

> *"To start with the client and business context: **ZEE5** is the flagship OTT streaming platform of Zee Entertainment Enterprises. It is one of India’s largest digital entertainment platforms, serving over **100+ million monthly active users** across **190+ countries** with content in **12+ Indian regional languages** (including Hindi, Marathi, Tamil, Telugu, Bengali, Kannada, and Malayalam).*
> 
> *In the highly competitive OTT streaming space, viewer retention and watch-time depend directly on how effortlessly a user can discover content. ZEE5 partnered with Google Cloud to replace their traditional, static search experience with an **Agentic, Multimodal Conversational Voice and Text Search Platform**—allowing millions of users to discover movies, web series, and even specific video scenes through natural, human-like conversation in their own native language."*

---

## 2. Problem Statement (The Gap & Business Impact)

> *"There were three critical business and technical gaps we needed to solve:*
> * **1. Limitations of Keyword Search:** *Traditional search only works when a user knows the exact movie title or actor name. If a user searched by a famous dialogue, a visual scene description (like 'the forest tiger fight scene'), or an abstract mood (like 'show me a patriotic military movie'), legacy search failed and caused viewer drop-off.*
> * **2. Complex Indian Multilingual Behavior:** *Indian viewers frequently mix spoken languages and content preferences—for example, speaking in Hindi or Hinglish to ask for a Marathi comedy or a Telugu action movie, or using regional voice accents that standard search bars cannot handle.*
> * **3. Balancing AI Relevance with Commercial Merchandising:** *ZEE5’s business and editorial teams needed a way to dynamically promote trending releases, cross-sell companion shows, and salvage searches for out-of-catalog movies (like 'Sholay' or 'Pathaan') without breaking AI search accuracy."*

---

## 3. Work and Solution (Approach, Key Actions & Ownership)

> *"Working closely with our Google Tech Lead, **Arjun Vijayvargiya**, and my teammate Mandar, I took end-to-end ownership of several core AI engineering pillars from POC to Cloud Run production:*
> * **1. Media Ingestion & Deep Scene Analysis Engine:** *We architected an automated multimodal video ingestion pipeline using **Gemini 3.1 Pro** and **Vertex AI Multilingual Embeddings (`text-multilingual-embedding-002`)**. It dissects full-length movies and shows into timestamped scene chunks—extracting visual summaries, character interactions, emotions, genres, and iconic dialogues—and indexes 768-dimension vectors into **Google Cloud Spanner Vector DB**.*
> * **2. Real-Time Bi-Directional Voice Agent (`Gemini Live API`):** *As Arjun highlighted in his feedback, we proactively spearheaded the POC and full integration of the cutting-edge **Gemini Live Audio API** using the **Google Agent Development Kit (ADK)**. We engineered low-latency bi-directional audio streaming over WebSockets, natural barge-in interruption handling, and **Two-Track Language Decoupling**—so if a user asks in Hindi for a Marathi movie, the agent retrieves the Marathi movie while replying fluently in Hindi.*
> * **3. Hybrid Search, Pluggable Ranking & Merchandising Studio:** *We built a 4-tier hybrid retrieval engine in Cloud Spanner (combining Exact Index match, Native Full-Text Search, Regex array filtering, and ANN Cosine Vector Search) paired with a 3-factor composite ranking algorithm (Semantic Similarity + Viewership Telemetry + Recency Decay) and an interactive **Ranking & Merchandising Studio**.*
> * **4. Engineering Rigor & 5,500+ Golden Query Certification:** *Beyond feature development, we took full ownership of software quality—writing comprehensive automated test suites spanning **5,500+ golden test cases** across text, live audio, phonetic typos, and multi-turn conversations, and managing stable deployments on **Google Cloud Run**."*

---

## 4. Expected Outcome (Business Impact, Award & Lead Feedback)

> *"This initiative delivered tangible impact for the client and strong recognition for our team:*
> * **Customer & Business Impact:** *Viewers can now discover titles and jump directly to playable, timestamped scene clips in under a second across Indian languages. At the same time, ZEE5’s editorial team can configure promotional campaigns and ranking weights in real time with **sub-millisecond (`<0.01ms`) overhead**.*
> * **2026 Q3 Google AI Award Winner:** *Our project, **`ZEE5-Conversational-Media-Search`**, was honored as a **SWEETY Silver Award Winner in the 2026 Q3 Google AI Awards**—recognizing our innovation in multimodal ingestion, scene-level intelligence, and conversational AI.*
> * **Commendation from Google Tech Lead:** *Arjun Vijayvargiya shared formal feedback with our leadership noting our technical depth, high ownership under demanding timelines and ambiguity, our initiative in leading the Gemini Live Audio POC, and our calm composure in triaging bugs and maintaining deployment stability."*

---

## 5. Key Learnings (Skills, Insights & Best Practices)

> *"This engagement significantly expanded my technical and consulting toolkit:*
> * **Agentic AI & Hybrid Vector Architecture:** *Deep hands-on mastery of **Google ADK**, multi-agent orchestration (`RootSupervisorAgent`, `IntentSubAgent`, `QueryIntentAgent`), and **Cloud Spanner Hybrid Vector + Full-Text Search**.*
> * **Real-Time Multimodal Audio Engineering:** *End-to-end tuning of **Gemini Live Audio WebSockets**, PCM audio framing, echo/interruption guards, and Indic script anchoring to eliminate hallucinations.*
> * **Enterprise Search Quality & Guardrails:** *Learning how to combine pure AI vector similarity with real-world business telemetry (views, completion rate, recency decay) while enforcing the **'Golden Rule'**—ensuring commercial boosts never override an exact movie title search."*

---

## 6. Challenges and Mitigation (Problems Faced & Lessons Learned)

> *"We faced three major technical challenges under tight customer timelines, and here is how we resolved them:*
> * **Challenge 1 — Cross-Lingual Drops & Voice Prefixes in the 5,500+ Benchmark:** *During our large-scale regression run, we discovered that hard SQL language filters were blocking cross-lingual actor queries (for example, searching for a Hindi actor while the user's preferred UI language was Bengali), and natural conversational voice prefixes (like 'Mujhe dekhna hai RRR') were bypassing exact merchandising triggers.*
>   * **Mitigation:** *Rather than using brittle hardcoding, we replaced hard SQL language exclusions with a **Soft Language Affinity Ranking bonus (+0.04)** and engineered a **Multilingual Voice Prefix Normalizer** with fuzzy sequence matching. This drove our overall benchmark pass rate above **94%+** while preserving **100% Exact Title protection**.*
> * **Challenge 2 — Multi-Turn Context Loss on Category Pivots:** *When users asked a broad query like 'snake scenes' and followed up in Turn 2 with 'in web series', stateless queries lost the original topic.*
>   * **Mitigation:** *We implemented **Dialogue State Tracking and Contextual Query Rewriting (CQR)** in the session manager to carry forward active thematic entities across turns seamlessly.*
> * **Challenge 3 — Maintaining Sub-Second Latency with Live Merchandising Rules:** *Querying the database for active promotional rules on every voice turn risked adding latency.*
>   * **Mitigation:** *We built an **In-Memory Micro-Cache** with a background Cloud Spanner sync daemon, reducing campaign lookup latency to **under 0.01 milliseconds**.*
> * **Lesson Learned:** *The biggest takeaway was that enterprise GenAI succeeds when cutting-edge models are paired with deterministic guardrails, rigorous automated regression testing, and clean systems engineering."*

---

## 🎯 Closing Wrap-Up (15 Seconds)

> *"Overall, working on the ZEE5 account has been a career highlight—both in terms of the deep GenAI engineering we delivered and the recognition we earned with the **Google Q3 SWEETY Silver Award** and **Arjun’s feedback**. I’m happy to dive deeper into any part of the architecture or take your feedback!"*

---

## 📎 Appendix: Verbatim Recognition & Feedback (For Reference During Call)

### 🏆 1. 2026 Q3 Google AI Awards — SWEETY Silver Award
> *"It gives me immense pride to share that our project, **ZEE5-Conversational-Media-Search**, has been selected as a **SWEETY Silver Award Winner in the 2026 Q3 Google AI Awards!** 🎉*  
> *This recognition highlights our team's exceptional innovation, technical excellence, and the real impact we've driven. Every milestone we achieved—from architecting the data ingestion pipeline to perfecting the scene-level metadata analysis—was only possible because of everyone's collective dedication, brilliance, and seamless collaboration."*  
> — **Arjun Vijayvargiya** (Google Tech Lead)

### ✉️ 2. Formal Feedback from Arjun Vijayvargiya (Google Tech Lead) to PwC Leadership
> *"Hi Anish,*  
> *I am writing to share feedback for Mandar Pardakhe and Akshay Karadkar based on our close collaboration on the ZEE5 Search AI platform initiative. Throughout this engagement, both performed exceptionally well as AI Engineers and Developers, demonstrating strong technical competence, high ownership, and agility in building complex GenAI capabilities.*  
> *Operating under demanding customer timelines and high ambiguity, Mandar and Akshay proved to be dependable, proactive problem solvers. They contributed substantially to the core technical deliverables, including building the media ingestion engine and developing multimodal semantic search across both audio and text modalities. Their technical depth and curiosity were evident as they actively spearheaded multiple POCs, notably exploring and integrating the cutting-edge Audio Live Gemini API module.*  
> *Equally commendable has been their commitment to software quality and engineering rigor. Beyond feature development, they took ownership of writing comprehensive regression tests, hands-on triaging and resolving critical platform bugs, and ensuring smooth, stable deployments within the customer environment. Their calm composure and troubleshooting abilities under pressure ensured the platform remained reliable and scalable.*  
> *Their collaborative mindset, rapid turnaround times, and solid execution have been pivotal to the success of this engagement. Mandar and Akshay delivered immense engineering value to the project as contractor developers, and it has been a pleasure working alongside them."*  
> — **Arjun Vijayvargiya**, Cloud Engineer, Application Development (`arjunvijay@google.com`)
