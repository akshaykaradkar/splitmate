# SplitMate — Bill Split & Settle (`0.00¢ Drift` Engine)

**SplitMate** is an offline-first, peer-to-peer group expense management Android application engineered with the **Stitch "Organic Tactile Financial" (Buckwheat Style)** visual identity and unified with the **4 Official Google Design Systems**:
1. **Google Material 3 (`google-material-3`)**
2. **Android Motion (`android-motion`)** — Expressive spring physics (`cubic-bezier(0.05, 0.7, 0.1, 1)`)
3. **Elements GM3 (`elements-3`)** — Semantic financial tokens
4. **Android Pixel Design System (`android-pixel-design-system`)**

---

## 📲 Direct Android APK Download (1-Tap Phone Install)

You can download and install the compiled Android APK directly onto your phone from this repository:
- **[Download `splitmate-debug.apk`](./splitmate-debug.apk)** (`17 MB`, Android 8.0+ / API 26–34, signed with APK Signature Scheme v2)

---

## ✨ Core Architecture & Mathematical Guarantees ([`BRD.md`](./BRD.md))

1. **Integer-Cent Relational SQLite Vault (`Long`)**:
   - Zero IEEE 754 floating-point drift. Every monetary value is stored and reconciled in integer cents (`$42.50` = `4250L`) inside the local 6-table Android SQLite database ([`SplitMateDatabaseHelper.kt`](./android/app/src/main/java/com/splitmate/app/SplitMateDatabaseHelper.kt)).
2. **Proportional Auxiliary Cost Distribution with Locked Multiplier ($m = T / B$)**:
   - Tax and tip are locked against the receipt's Base Subtotal ($B$), ensuring $m = T / B$ remains invariant while items are claimed ([`SplitMateMathEngine.kt`](./android/app/src/main/java/com/splitmate/app/SplitMateMathEngine.kt)).
3. **Live Remainder Engine (`R_base`)**:
   - Unclaimed base items are temporarily attributed to the Payer with a striped Terracotta/Peach alert and a 1-tap **`⚡ Split Remainder Equally`** action.
4. **Largest Remainder Method (`0.00¢ Drift`)**:
   - Deterministic penny reconciliation ensures $\sum S_i \equiv T$ down to the exact cent (`Alex (+1¢) $14.17 | Sam $14.17 | Priya $14.16`).
5. **Greedy Minimum Cash Flow Algorithm (Max-Priority Queues)**:
   - Reduces $O(N^2)$ raw cross-debts (`10 raw debts`) into at most $N - 1$ direct transfers (`2 transfers`, `-80% friction`).

---

## 🖼️ Included Stitch Screens (`stitch_splitmate_design_system/`)

1. **User Profile, Instant Guest & Cloud Sync Login**: [`splitmate_premium_guest_onboarding`](./stitch_splitmate_design_system/splitmate_premium_guest_onboarding/code.html)
2. **Animated Groups & Ledgers Hub (`+$142.50` Owed)**: [`splitmate_groups_ledgers_animated`](./stitch_splitmate_design_system/splitmate_groups_ledgers_animated/code.html)
3. **Ledgers — Net Debtor (`-$184.40` You Owe)**: [`splitmate_ledgers_you_owe_state`](./stitch_splitmate_design_system/splitmate_ledgers_you_owe_state/code.html)
4. **Ledgers — All Settled Equilibrium (`$0.00`)**: [`splitmate_ledgers_all_settled_state`](./stitch_splitmate_design_system/splitmate_ledgers_all_settled_state/code.html)
5. **Ledgers — Empty State (`$0.00 All quiet here`)**: [`splitmate_ledgers_empty_state`](./stitch_splitmate_design_system/splitmate_ledgers_empty_state/code.html)
6. **Quick Expense Logger & Calculator (`$42.50` · `0.00¢ Drift`)**: [`splitmate_quick_expense_logger_calculator`](./stitch_splitmate_design_system/splitmate_quick_expense_logger_calculator/code.html)
7. **Live Receipt Claim & Remainder Engine (`Osteria Del Sole $148.50`)**: [`live_receipt_claim_remainder_engine_buckwheat_style`](./stitch_splitmate_design_system/live_receipt_claim_remainder_engine_buckwheat_style/code.html)
8. **Greedy Debt Simplification & Settle Up (`10 → 2 Transfers`)**: [`greedy_debt_simplification_buckwheat_style`](./stitch_splitmate_design_system/greedy_debt_simplification_buckwheat_style/code.html)
