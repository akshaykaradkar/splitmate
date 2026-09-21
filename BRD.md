# Business Requirements Document (BRD)

## SplitMate: Next-Generation Group Expense Management Application (Manual Collaborative Model)

**Version:** 1.1 (Refined Local-First & Mathematical Specification)  
**Target Platform:** Android (Kotlin, Jetpack Compose, Material 3 Expressive, Jetpack Room)  
**Deployment Mode (Phase 1):** Local-First Standalone with In-App Multi-Persona Simulation & Shadow Profiles

---

### 1. Executive Summary

The intersection of personal finance and group social dynamics presents a uniquely complex engineering challenge. When individuals engage in shared activities, the financial burden is rarely distributed evenly at the point of transaction, frequently resulting in one individual inadvertently acting as a micro-creditor and generating a web of informal, short-term debts. Without immediate intervention, these debts are subject to memory decay, resulting in financial inequity and profound social friction.

This document outlines the architectural and design blueprint for **SplitMate**, a highly scalable, offline-first Android application engineered to resolve these exact pain points using Google’s **Material 3 Expressive** design paradigm. Rather than relying on error-prone AI receipt scanning, this application prioritizes a **Collaborative Manual Entry** architecture optimized for speed, mathematical determinism, and group trust. The initial deployment is architected as a **local-first build with simulated multi-user / Shadow Profile collaboration** for personal and localized peer-group use before evaluating cloud synchronization and future monetization strategies.

---

### 2. Market Analysis & Competitive Dynamics

A rigorous examination of existing expense-splitting platforms reveals distinct product philosophies, monetization strategies, and critical feature gaps. An effective new entrant must seamlessly bridge the divide between applications optimized for spontaneous, offline travel and those built for long-term household tracking.

| Application | Core Philosophy & Strengths | Primary Weaknesses & Limitations | Monetization Model |
| --- | --- | --- | --- |
| **Splitwise** | High network effect, polished UI, recurring bills, direct US bank transfer integrations. | Strict daily expense caps, paywalled currency conversion, paywalled receipt scanning. | Ad-supported free tier; per-user monthly subscription (Pro). |
| **Tricount** | Frictionless onboarding, link-sharing access, completely free usage without daily caps. | Lacks recurring expenses, lacks itemized receipt scanning, removed data export features. | Completely free (subsidized by bunq ownership). |
| **Splid** | Minimalist design, strong offline mode, account-free usage, built-in free multi-currency. | No household/recurring bill layer, no automated receipt reading capabilities. | Free core; one-time paid upgrades for exports and unlimited groups. |
| **SplitterUp** | AI itemized receipt scanning, Smart re-splitting, Live table sharing, household sub-groups. | Newer application with a smaller established network, fewer third-party payment integrations. | Free promotional window; one-time purchase fee ($4.99). |
| **FamZam** | Unlimited daily expenses, offline entry, direct UPI settlement integration for Indian markets. | Lesser-known platform, lacks advanced algorithmic line-item scanning. | Completely free with zero ads and no paid tiers. |
| **Kittysplit** | Instant browser-based splitting, requires no app installation, shareable web links. | No confirmed offline mode, lacks long-term ledger tracking, no receipt scanning. | Free basic tier; "Super Kitty" unlocks premium extras. |

---

### 3. Core Functional Requirements: Collaborative Manual Entry

The application directly addresses the primary user scenario of **asymmetrical consumption** (e.g., individual D consumes nothing, while B pays the collective bill). The solution utilizes a Collaborative Manual Entry architecture optimized for low friction and instant visual feedback.

#### 3.1. Granular Manual Splitting & User-Driven Claiming

* **Bypassing Equal-Split Defaults:** When a user logs a monolithic expense (e.g., a $150 dinner), the interface bypasses the traditional "split equally" default and immediately opens the **Itemized / Custom Claim Matrix**.
* **Base Consumption Entry:** Participants enter only the **base cost** of their items (e.g., User A logs $35.00 for their meal, User B logs $50.00). Users never have to mentally calculate their share of tax, tip, or service charges.
* **In-App Persona Switcher & Direct Row-Tap Proxy Entry (Phase 1 Local-First Collaboration):**
  * **Persona Switcher Bar:** A top-level Material 3 Expressive chip selector (`Acting as: [User A] [User B] [Guest C]`) allows switching the active claiming persona on a single device to simulate multi-user collaboration or pass the phone around the table.
  * **Direct Row-Tap Editing:** Any user can also tap directly on any group member or **Shadow Profile** row in the split matrix to enter or adjust that participant's base amount via the bottom `HorizontalFloatingToolbar` NumPad and quick-add chips (`+$1`, `+$5`, `+$10`, `+$20`).

#### 3.2. Mathematical Specification: Locked Proportional Auxiliary Splitting

To prevent a participant's displayed total from shifting while other group members are still entering their items, the Tax/Tip multiplier is **locked against the receipt's Base Subtotal** from the moment the expense shell is created.

1. **Receipt Decomposition (All values stored in integer minor units / cents):**
   * Let $T$ be the **Total Receipt Amount** paid by the Payer.
   * Let $A = A_{\text{tax}} + A_{\text{tip}} + A_{\text{service}} - D_{\text{discount}}$ be the net **Auxiliary Overhead** entered by the Payer.
   * The **Base Subtotal** is fixed as:
     $$B = T - A$$
   * The **Locked Auxiliary Multiplier** $m$ is defined as:
     $$m = 1 + \frac{A}{B} = \frac{T}{B} \quad (\text{for } B > 0)$$

2. **Individual Participant Calculation:**
   * When Participant $i$ claims a base consumption amount $b_i \in [0, B]$:
     * **Auxiliary Share:** $a_i = \text{round}\!\left(b_i \times \frac{A}{B}\right)$
     * **Total Owed by Participant $i$:** $s_i = b_i + a_i = \text{round}(b_i \times m)$
   * Because $B$ and $A$ are properties of the receipt itself rather than the running sum of partial claims, Participant $i$'s total $s_i$ is **100% deterministic and invariant** the instant they input $b_i$.

3. **Exact Cent Reconciliation (Largest Remainder Method):**
   * Once 100% of the base subtotal is claimed ($\sum_{i} b_i = B$), any 1–2 cent rounding discrepancy $\Delta = T - \sum_{i} s_i$ is distributed deterministically using the **Hamilton / Largest Remainder Method** (sorting active claimants by the fractional remainder of $b_i \times \frac{T}{B}$), guaranteeing that $\sum_{i} s_i \equiv T$ to the exact cent.

#### 3.3. The "Remainder" Engine & Unassigned Balance Resolution

* **Real-Time Unassigned Balance:** As participants log their individual base amounts $b_i$, the UI prominently displays:
  * **Unassigned Base Balance:** $R_{\text{base}} = B - \sum_{i} b_i$
  * **Unassigned Total Balance (incl. proportional Tax/Tip):** $R_{\text{total}} = T - \sum_{i} s_i$
* **Payer-Held Temporary Remainder (Zero-Sum Ledger Guarantee):**
  * While an expense has an unclaimed balance ($R_{\text{base}} > 0$), the expense is marked with an **Amber Warning Badge** (`Unassigned: $XX.XX`) in the group feed.
  * To ensure the group ledger always sums to 100% and remains usable for interim balance checks, the unclaimed remainder $R_{\text{total}}$ is **temporarily held on the Payer's share** ($s_{\text{payer, effective}} = s_{\text{payer, claimed}} + R_{\text{total}}$) until claimed by peers or resolved.
* **1-Tap Remainder Resolution Action:**
  * At any point, the Payer or any participant can tap **"Split Remainder Equally"** to divide $R_{\text{base}}$ evenly across all participating members (or a filtered subset of active diners), immediately driving $R_{\text{base}} \to \$0.00$ and transitioning the expense badge from Amber (`Unassigned Remainder`) to Green (`Fully Balanced`).
* **Over-Claim Guardrail:**
  * If $\sum_{i} b_i > B$, the progress bar transitions to an **Error (`errorContainer`)** state showing `Over-claimed by $XX.XX`, prompting immediate adjustment or offering a 1-tap **"Increase Receipt Subtotal to Match"** option.

---

### 4. Resolution of Ancillary Behavioral and Technical Frictions

#### 4.1. Social Friction & Debt Account Aversion

* **Problem:** Behavioral economics reveals that individuals exhibit "Debt Account Aversion"—a desire to settle small debts quickly to reduce total accounts—yet the social awkwardness of verbally requesting repayment frequently prevents collection.
* **Solution:** The application acts as an objective, emotionless intermediary. By providing transparent itemized breakdowns (showing Base + exact proportional Tax/Tip), neutral ledger nomenclature, and 1-tap settlement recording, the software removes the emotional burden from the creditor.

#### 4.2. Frictionless Onboarding & Shadow Profiles

* **Problem:** Requiring every participant at a dinner table to install an app and register an account before logging an expense creates an insurmountable bottleneck.
* **Solution:** Authenticated/local users can create **Shadow Profiles (Guest Profiles)** with a single name/initials input (no email or account required). Through the **In-App Persona Switcher** and **Direct Row-Tap Proxy Entry**, any device holder can claim items on behalf of Shadow Profiles or pass the phone around the table so everyone can claim their share in seconds.

#### 4.3. Multi-Currency Drift

* **Problem:** Fluctuating exchange rates between the time an expense is logged internationally and settled domestically cause severe mathematical discrepancies and social confusion.
* **Solution:** Each `ExpenseEntity` stores its native `currencyCode` alongside a permanently locked `lockedExchangeRate` relative to the group's `baseCurrency` at creation time (editable offline if needed). Historical converted totals in the group's base currency remain strictly immutable regardless of subsequent exchange rate fluctuations.

#### 4.4. Group Fluidity & Smart Re-Splitting

* **Problem:** The composition of social groups is inherently fluid; manual adjustments are highly error-prone when participants join a trip late or skip activities.
* **Solution:** A **Smart Re-Splitting** algorithmic mechanism traverses the relational database and recalculates affected splits and every member's running net balance globally whenever a user is retroactively added to or excluded from an expense, maintaining cent-level accuracy without manual offset entries.

---

### 5. Algorithmic Foundation: Debt Simplification

To efficiently reconcile group balances, SplitMate models the group's financial state as a weighted directed graph $G = (V, E)$, where vertices $V$ represent group members and edges $E$ represent outstanding debts.

1. **Net Balance Calculation:** Disregard individual pairwise edges and compute the aggregate net balance $B(u)$ for every member $u \in V$ in integer cents:
   $$B(u) = \text{Total Paid by } u - \text{Total Owed by } u$$
   Because every expense (including any Payer-held temporary remainder) satisfies $\sum_{u \in V} s_u = T$, the invariant $\sum_{u \in V} B(u) \equiv 0$ holds at all times.
2. **Partitioning:**
   * **Creditors ($V_+$):** $\{ u \in V \mid B(u) > 0 \}$
   * **Debtors ($V_-$):** $\{ u \in V \mid B(u) < 0 \}$
   * **Settled ($V_0$):** $\{ u \in V \mid B(u) = 0 \}$
3. **Greedy Minimum Cash Flow (Max-Priority Queues):**
   * Initialize two Max-Priority Queues: $Q_+$ for Creditors ordered by $B(u)$, and $Q_-$ for Debtors ordered by $|B(u)|$.
   * While both $Q_+$ and $Q_-$ are non-empty:
     1. Extract maximum creditor $c = \text{extractMax}(Q_+)$ and maximum debtor $d = \text{extractMax}(Q_-)$.
     2. Create a simplified settlement transfer from $d \to c$ of amount $\tau = \min(B(c), |B(d)|)$.
     3. Update residual balances: $B'(c) = B(c) - \tau$ and $|B'(d)| = |B(d)| - \tau$.
     4. Re-insert whichever party has a non-zero residual balance back into $Q_+$ or $Q_-$.
   * **Guarantee:** Reconciles any group of $N$ members in at most $N - 1$ transactions, eliminating circular debts and intermediary pass-through payments.

---

### 6. Technical Architecture & Local-First Persistence

#### 6.1. Local Persistence Layer (Jetpack Room)

All monetary amounts are persisted as `Long` integer minor units (cents/paise) to prevent floating-point representation errors. Strict Foreign Key constraints using `onDelete = ForeignKey.CASCADE` automatically purge dependent `ExpenseEntity` and `ExpenseSplitEntity` rows when a parent `ExpenseGroupEntity` is deleted.

| Entity Table | Primary Key | Foreign Keys & Cascade Rules | Core Columns |
| --- | --- | --- | --- |
| `UserEntity` | `userId: String` | — | `name`, `avatarColorHex`, `isShadowGuest: Boolean`, `createdAt` |
| `ExpenseGroupEntity` | `groupId: String` | — | `groupName`, `baseCurrency`, `simplifyDebtsEnabled: Boolean`, `createdAt`, `syncStatus` |
| `GroupMemberCrossRef` | `(groupId, userId)` | `groupId` $\to$ `ExpenseGroupEntity` (`CASCADE`), `userId` $\to$ `UserEntity` (`CASCADE`) | `joinedAt`, `isActive: Boolean` |
| `ExpenseEntity` | `expenseId: String` | `groupId` $\to$ `ExpenseGroupEntity` (`CASCADE`), `payerId` $\to$ `UserEntity` (`RESTRICT`) | `title`, `totalAmountCents: Long`, `baseSubtotalCents: Long`, `taxAmountCents: Long`, `tipAmountCents: Long`, `currencyCode`, `lockedExchangeRate: Double`, `remainderStatus`, `timestamp`, `syncStatus` |
| `ExpenseSplitEntity` | `splitId: String` | `expenseId` $\to$ `ExpenseEntity` (`CASCADE`), `userId` $\to$ `UserEntity` (`CASCADE`) | `claimedBaseCents: Long`, `proportionalAuxCents: Long`, `totalOwedCents: Long`, `claimedByPersonaId: String`, `updatedAt` |
| `SettlementEntity` | `settlementId: String` | `groupId` $\to$ `ExpenseGroupEntity` (`CASCADE`) | `fromUserId`, `toUserId`, `amountCents: Long`, `currencyCode`, `timestamp` |

#### 6.2. Reactive State & Future Sync Readiness

* **Reactive Flows:** Room DAOs expose `Flow<GroupLedgerSnapshot>` pipelines so any claim update via the NumPad immediately recomputes the Remainder Engine progress bar, Payer-held remainder badge, and Greedy Minimum Cash Flow settlement graph in under 16ms (60fps).
* **Offline-First Sync Metadata:** Entities retain `syncStatus` (`LOCAL_ONLY`, `PENDING_SYNC`, `SYNCED`) and `updatedAt` vector timestamps so a cloud sync adapter (WorkManager + REST/Firestore) can be attached in Phase 2 without schema migrations.

---

### 7. User Interface (UI) & Design System (Material 3 Expressive)

Built entirely in **Jetpack Compose** following Google’s **Material 3 Expressive** guidelines to replace clinical banking UI with warm, tactile, low-stress interactions:

* **Dynamic Color & HCT Tonal Palettes:** Uses Android 12+ Dynamic Color (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) backed by HCT (Hue, Chroma, Tone) perceptual contrast guarantees, with a custom high-contrast M3 Expressive emerald/teal & warm-amber fallback palette.
* **In-App Persona Switcher Pill Bar:** Elevated top-bar pill carousel allowing instant switching between group members and Shadow Guests (`Acting as: Alex (You)` $\leftrightarrow$ `Sam` $\leftrightarrow$ `Priya (Guest)`), highlighting the currently active persona's row and unclaimed obligations.
* **Input-Optimized `HorizontalFloatingToolbar`:** Replaces traditional bottom navigation during expense splitting with a floating, spring-animated control surface housing:
  * Quick-increment tactile chips (`+$1`, `+$5`, `+$10`, `+$20`, `Clear`)
  * Integrated high-speed custom NumPad
  * 1-Tap **"Split Remainder Equally"** action pill when $R_{\text{base}} > 0$
* **Expressive Shapes & Typography:**
  * `16.dp` corner radius on interactive buttons and quick-chips; `20.dp`–`28.dp` on elevated expense and balance cards.
  * `DisplayLarge` (`57.sp`) / `HeadlineLarge` typography for hero balance totals and Remainder Engine readouts.
* **Dynamic Fill-State Progress Indicator:**
  * A multi-segment animated progress bar at the top of the Expense Claim Matrix showing each member's color-coded claimed share:
    * **Amber (`tertiaryContainer` / Warning):** Unassigned balance remains ($R_{\text{base}} > 0$; temporarily held on Payer).
    * **Emerald (`primaryContainer` / Balanced):** 100% claimed ($R_{\text{base}} = \$0.00$).
    * **Coral (`errorContainer` / Over-Claimed):** Claimed base exceeds receipt subtotal ($\sum b_i > B$).
* **Touch Accessibility:** Strict `48.dp × 48.dp` minimum touch targets across all persona chips, member rows, and NumPad keys.
