---
version: '2.0-buckwheat-expressive'
name: 'SplitMate: Organic Tactile Financial (Buckwheat Style) + GM3 Expressive'
description: 'Unified design specification combining the Stitch Organic Tactile Financial (Buckwheat-inspired) system with Google Material 3 Expressive, Android Motion, Elements GM3, and Android Pixel Design System.'
colors:
  # Buckwheat-Inspired Organic Tactile Canvas & Surface Hierarchy
  canvas-base: '#FAF6F0'
  canvas-warm: '#F7F3EC'
  canvas-sunken: '#F4EFE6'
  surface: '#FFF8F5'
  surface-container-lowest: '#FFFFFF'
  surface-container-low: '#FAF2EE'
  surface-container: '#F4ECE9'
  surface-container-high: '#EEE7E3'
  surface-container-highest: '#E8E1DD'
  border-soft: '#EDE7DF'

  # Primary: Organic Olive & Sage (Reconciled, Creditor, Primary Actions)
  primary: '#416913'
  on-primary: '#FFFFFF'
  primary-container: '#D7E8B6'
  on-primary-container: '#274700'
  sage-soft: '#D7E8B6'
  sage-medium: '#B8D98D'
  sage-deep: '#416913'
  sage-dark: '#365314'

  # Secondary: Warm Terracotta & Peach (Unassigned Remainder, You Owe, Split Remainder CTA)
  secondary: '#8F4B3A'
  on-secondary: '#FFFFFF'
  secondary-container: '#FED8C8'
  on-secondary-container: '#7A3B2A'
  peach-tint: '#FED8C8'
  peach-muted: '#FBC3B5'
  peach-accent: '#F59F89'
  terracotta-cta: '#E06B52'
  terracotta-dark: '#7C2D12'

  # Tertiary: Periwinkle & Lavender (Secondary Participants & Equal Split Badges)
  tertiary: '#525D83'
  on-tertiary: '#FFFFFF'
  tertiary-container: '#DCE3FD'
  on-tertiary-container: '#323D61'
  lavender-tint: '#EEF2FF'
  lavender-medium: '#DCE3FD'
  lilac-dark: '#3730A3'

  # Remainder & Overclaim Semantic Alerts
  butter-remainder: '#FEF08A'
  amber-remainder-text: '#713F12'
  coral-overclaim: '#FECDD3'
  coral-overclaim-text: '#881337'
  error: '#BA1A1A'
  error-container: '#FFDAD6'
  on-error-container: '#93000A'

  # Typography & Charcoal Action Contrast
  text-primary: '#23201E'
  text-secondary: '#635E59'
  text-tertiary: '#948E85'

shapes:
  card-lg: '32dp'    # Hero Bento Cards (Owed / You Owe / Equilibrium / Greedy Graph Simplifier)
  card-md: '24dp'    # Group Cards, Remainder Allocation Box, Calculator Deck
  card-sm: '18dp'    # Receipt Item Rows, Keypad Keys, Required Transfer Cards
  control-md: '16dp' # Tactile Keypad Cells, Outlined Inputs
  pill: '9999dp'     # Persona Switcher Chips, Quick-Add Pills (+$1, +$5, +$10, 1/3 Eq), Bottom Nav Active Indicator
---

# SplitMate Master Design Specification: "Organic Tactile Financial" (Buckwheat + M3 Expressive)

## 1. Executive Assessment of the Stitch Prototype (`stitch_splitmate_design_system`)

The Stitch prototype in [stitch_splitmate_design_system/](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system) is **vastly superior to a standard blue fintech UI**. Taking direct inspiration from **Buckwheat** ([buckwheat.jpg](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/buckwheat.jpg/screen.png)) and formalizing the **[Organic Tactile Financial](file:///usr/local/google/home/karadkar/splitmate/stitch_splitmate_design_system/organic_tactile_financial/DESIGN.md)** system solves the exact psychological problem identified in the BRD (*Debt Account Aversion & Social Friction*):

1. **Why the Buckwheat Earth-Tone Palette Works So Well:**
   - Standard banking blues (`#0b57d0`) and harsh clinical reds trigger financial anxiety.
   - Replacing them with **Warm Cream (`#FAF6F0`)**, **Organic Sage (`#D7E8B6` / `#365314`)**, **Warm Terracotta/Peach (`#FED8C8` / `#E06B52` / `#7C2D12`)**, **Periwinkle Mist (`#DCE3FD` / `#3730A3`)**, and **Soft Charcoal (`#23201E`)** makes splitting a dinner tab feel warm, friendly, and human.
2. **Signature UI Components Already Nailed in Your Stitch Screens:**
   - **4-Tab Bottom Navigation (`Ledgers` | `Split` | `Settle` | `Audit`):** Clean M3 pill indicator (`bg-sage-soft`) for the active destination.
   - **Hero Dual-Tone Organic Bento Card (`32dp` radius):** Dynamically adapts across all 4 states:
     1. **You Are Owed (`+$142.50`):** Soft sage-to-peach ambient blob gradient (`Across 3 active groups · 0 penny drift`).
     2. **You Owe (`-$184.40`):** Warm peach/terracotta gradient (`2 pending settlements`) with 1-tap `⚡ Pay via Venmo` CTA.
     3. **All Settled (`$0.00 Perfect Equilibrium Reached`):** Calm sage card (`0.00¢ drift certified`).
     4. **Empty State (`$0.00 All quiet here`):** Quick-starter templates (`🥗 Weekend Getaway`, `🏠 Roommate Rent & Wifi`).
   - **Live Receipt Claim & Remainder Engine (`Osteria Del Sole — $148.50`):**
     - Displays the **Locked Tax/Tip Multiplier** (`🔒 Tax 8.875% ($10.65) + Tip 18% ($17.85)` $\to$ `Multiplier m = 1.2375x` sage pill).
     - **`CLAIMING AS` Persona Switcher:** Horizontal tactile cards (`Alex (You) $52.40`, `Sam $44.10`, `Priya (Guest) $38.00`).
     - **Remainder Allocation Card:** Multi-segment progress bar with a **diagonal striped peach segment (`.striped-remainder`)** for unclaimed amounts, paired with `ⓘ Unassigned Base: $11.31 ($14.00 Final) — Temporarily attributed to Payer (Alex)` and the 1-tap terracotta button **`⚡ Split $14.00 Remainder Equally (+$4.67/ea)`**.
     - **Bottom Embedded Calculator (`Add Item to Alex`):** Shows `s = round(base × 1.2375)`, quick pills (`+$1`, `+$5`, `+$10`, `1/3 Eq`, `⌫`), a `3×4` cream tactile keypad (`#F7F3EC`), and **`⊕ Apply Custom Base to Alex`** (`#365314` olive-green primary CTA).
   - **Quick Expense Logger (`Mountain Sunrise Groceries — $42.50`):**
     - Features the 4 split mode tabs (`Equally (3)` | `Exact ($)` | `Percent (%)` | `Itemized`) and explicitly visualizes **Hamilton / Largest Remainder penny distribution** (`⚖️ Remainder Balanced (0.00¢ drift): Alex (+1¢) $14.17 | Sam $14.17 | Priya $14.16`).
   - **Greedy Debt Simplification (`Lake Tahoe Cabin`):**
     - Visualizes `10 raw debts → 2 direct transfers (-80% friction)` with a node-convergence diagram, `Required Transfers` cards (`✓ Mark Paid`, `Venmo`, `Cash App`), and the `Group Net Balances (Σ = $0.00)` table.
