# SplitMate Project Rules & Design System Mandate

When writing or modifying any Android (Kotlin / Jetpack Compose / Room) or Web UI code in this repository, you MUST strictly adhere to:

1. **Primary Visual Identity — Stitch "Organic Tactile Financial" (Buckwheat Style)**:
   - **Canonical Stitch Design System**: [stitch_splitmate_design_system/organic_tactile_financial/DESIGN.md](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/organic_tactile_financial/DESIGN.md)
   - **Reference Screen Prototypes (`code.html` & `screen.png`)**:
     - **Guest Onboarding**: [splitmate_premium_guest_onboarding](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/splitmate_premium_guest_onboarding/code.html)
     - **Groups & Ledgers (Owed / You Owe / All Settled / Empty States)**:
       - [splitmate_groups_ledgers_buckwheat_style](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/splitmate_groups_ledgers_buckwheat_style/code.html)
       - [splitmate_ledgers_you_owe_state](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/splitmate_ledgers_you_owe_state/code.html)
       - [splitmate_ledgers_all_settled_state](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/splitmate_ledgers_all_settled_state/code.html)
       - [splitmate_ledgers_empty_state](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/splitmate_ledgers_empty_state/code.html)
     - **Live Receipt Claim & Remainder Engine**: [live_receipt_claim_remainder_engine_buckwheat_style](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/live_receipt_claim_remainder_engine_buckwheat_style/code.html)
     - **Quick Expense Logger & Calculator (0.00¢ Drift)**: [splitmate_quick_expense_logger_calculator](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/splitmate_quick_expense_logger_calculator/code.html)
     - **Greedy Debt Simplification & Settle Up**: [greedy_debt_simplification_buckwheat_style](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/greedy_debt_simplification_buckwheat_style/code.html)
   - **Color Palette Mandate (Buckwheat Earth & Pastel Tones, NOT Cold Corporate Blue)**:
     - Canvas Base: `#FAF6F0` / `#FCF9F4` (`#FFF8F5`), Canvas Warm: `#F7F3EC`, Canvas Sunken: `#F4EFE6`, Card Surface: `#FFFFFF` with `1px` `#EDE7DF` soft border.
     - Primary Olive/Sage (`#416913` / `#365314` deep olive, `#D7E8B6` / `#DCE9B9` soft sage container).
     - Secondary Terracotta/Peach (`#E06B52` / `#EE8564` coral-terracotta CTA, `#FED8C8` / `#FCE3D7` peach container, `#7C2D12` dark terracotta text) for Unassigned Remainder alerts, `Split Remainder Equally` CTA, and `You Owe` states.
     - Tertiary Periwinkle/Lavender (`#EEF2FF` / `#DCE3FD` container, `#3730A3` text).
     - Soft Charcoal (`#23201E`) for primary typography and dark action pills (`+ New Group`, `✓ Mark Paid`, `Start Splitting →`).

2. **The 4 Official Google Design Systems (`design_systems/` & `.agents/skills/`)**:
   - **1. Google Material 3 (`google-material-3`)**: [design_systems/google-material-3/DESIGN.md](file:///usr/local/google/home/karadkar/splitmate/design_systems/google-material-3/DESIGN.md)
   - **2. Android Motion (`android-motion`)**: [design_systems/android-motion/DESIGN.md](file:///usr/local/google/home/karadkar/splitmate/design_systems/android-motion/DESIGN.md)
   - **3. Elements GM3 (`elements-3`)**: [design_systems/elements-3/DESIGN.md](file:///usr/local/google/home/karadkar/splitmate/design_systems/elements-3/DESIGN.md)
   - **4. Android Pixel Design System (`android-pixel-design-system`)**: [design_systems/android-pixel-design-system/DESIGN.md](file:///usr/local/google/home/karadkar/splitmate/design_systems/android-pixel-design-system/DESIGN.md)

3. **Business & Mathematical Requirements ([BRD.md](file:///usr/local/google/home/karadkar/splitmate/BRD.md))**:
   - Store all monetary amounts in integer cents (`Long`), lock the Proportional Auxiliary Multiplier ($m = T / B$) against the receipt's Base Subtotal, hold any unassigned remainder temporarily on the Payer with the striped/peach Remainder banner + 1-tap `Split Remainder Equally` action, reconcile penny rounding via Largest Remainder (`0.00¢ drift`), and simplify group debts via Greedy Minimum Cash Flow (Max-Priority Queues).
