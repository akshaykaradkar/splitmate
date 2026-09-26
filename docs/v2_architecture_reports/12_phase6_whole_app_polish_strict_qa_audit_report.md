# Phase 6 Whole-App Polish Pass: Strict QA & R8 Guardrail Audit (Report 12)

> **Auditor**: `v2_principal_qa_and_r8_guardrail_auditor` (read-only; no source files modified, nothing committed)
> **Baseline**: [11_WHOLE_APP_MATERIAL_GENUX_AND_ANDROID_ARCHITECTURE_AUDIT.md](file:///usr/local/google/home/karadkar/splitmate/docs/v2_architecture_reports/11_WHOLE_APP_MATERIAL_GENUX_AND_ANDROID_ARCHITECTURE_AUDIT.md) (5 P1 · 8 P2 · 4 P3) + `GEMINI.md` rules
> **Date**: 2026-09-26

---

## 0. Execution Constraints (read first)

> [!WARNING]
> **The build/test gate was NOT executed.** Both `git status/diff` and the mandated
> `gradle testDebugUnitTest assembleRelease` command were blocked: the terminal permission prompt timed out with no user response.
> This audit is therefore a **static source audit of the current working tree**. I compared the code against each claim and the Report 11 line references.
> I could not produce a diff against HEAD, so I could not prove "light mode unchanged" by diff. I inferred it from the `if (isDark) … else <light value>` branches.
> I could not confirm "BUILD SUCCESSFUL, 0 `e:`/`w:`". The verdict below is **provisional** until someone runs that command once.

---

## 1. Per-Item Verification Table

| ID | Claim | Evidence (current tree) | Result |
| :- | :--- | :--- | :---: |
| **P1-1** | Aurora dark-adaptive container/border/accent/blob alpha; status pill dark text | [AuroraEnergySurface.kt:L99-L113](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AuroraEnergySurface.kt#L99-L113): `resolvedBaseContainer` → `#24201C`, `resolvedRestingBorder` → `#38312B`, `resolvedAccentTint` → `#B5DC86`/`#C7D2FE`, blob scale `0.12f`. Pill text `GM3DarkPrimaryText` ([L310](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AuroraEnergySurface.kt#L310)). `SplitMateTheme.isDark` is `mutableStateOf` ([SplitMateAppComposable.kt:L96](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L96)), so it recomposes. Light branch keeps `#F7F3EC` / `#EDE7DF` / `#416913` / `#3730A3`. Hero `#FAF6F0` on `#24201C` ≈ 14:1. | **PASS** |
| **P1-2** | Log & Split CTA `#D7E8B6`/`#1E2F08` in dark; hero label contrast; chip touch bounds | [QuickExpenseAndGuideScreens.kt:L1575-L1586](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1575-L1586): dark CTA is sage/olive (≈11:1), light stays `#23201E`/white, disabled stays SageSurface/SageText. Hero label `#D6CEC4` in dark ([L1265](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1265)). `minimumInteractiveComponentSize` at L853, L896, L958, L1039. | **PASS** |
| **P1-3** | Flight/PNR dark chrome + dark disabled CTA | `FlightPassTokens` dark getters ([Flight:L75-L108](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L75-L108)). Dark disabled CTA `#2E2A25` + `#B8B0A4` (≈6.5:1) at [L963-L964](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L963-L964) and [L997](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L997). `TactilePaperPassTokens` dark getters ([PNR:L78-L119](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L78-L119)), dark disabled CTA [L648-L649](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L648-L649), Switch Group tint fix [L542](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L542). Light values match Report 11 tokens. | **PASS** (see N-P3-6) |
| **P1-4** | Flight preview delegates to `splitEquallyZeroDrift` with VM-mirrored payer/member resolution | [Flight:L711-L727](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L711-L727): same member filter as the VM, payer fallback `selected → currentUser → first`, and the same `currentUserId`. Matches [SplitMateViewModel.kt:L695-L711](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L695-L711). `totalAirfarePaise` is passed as `Long` cents on commit ([L949](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L949)). **Preview == persisted on the create path.** | **PASS** (edge case N-P3-7) |
| **P1-5** | `ACTION_SEND` `text/plain` on MainActivity | [AndroidManifest.xml:L68-L72](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/AndroidManifest.xml#L68-L72) has `DEFAULT` category and `mimeType="text/plain"`. The handler in [MainActivity.kt:L79-L89](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/MainActivity.kt#L79-L89) is guarded by the SM2 token check, handles `singleTop` `onNewIntent`, and uses the consumed-flag. | **PASS** (UX note N-P3-9) |
| **P2-1** | Transit deck pill touch bounds | [AnimatedTransitDeckHeroCard.kt:L609](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AnimatedTransitDeckHeroCard.kt#L609), L920 | **PASS** (N-P3-3) |
| **P2-2** | Flight/PNR top bar & chip touch bounds with padding compensation | PNR: 20/14 → 17/11 padding compensates for the 42 → 48 dp growth ([PNR:L485-L487](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L485-L487)), so there is no visual shift. Flight: L840, L868, L1149, L1186. | **PASS** (N-P3-3 for Flight Switch Group) |
| **P2-3** | QuickExpense chips | L853/896/958/1039 (see P1-2) | **PASS** (info I-1) |
| **P2-4** | UPI VPA chips + manual settle ≥48dp | [UpiExpressPaymentSheet.kt:L1039](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/UpiExpressPaymentSheet.kt#L1039), [L1192](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/UpiExpressPaymentSheet.kt#L1192) | **PASS** |
| **P2-5** | Classic v1.x touch bounds | [SplitMateAppComposable.kt](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt) L1346, L1521, L1554, L2014, L2048, L3605, L4873, L4905 | **PASS** (N-P3-4) |
| **P2-6** | Labelled `PNR:` regex in VM | [SplitMateViewModel.kt:L663-L669](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L663-L669) and [L1181-L1183](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L1181-L1183) use `\bPNR:\s*([A-Za-z0-9]{6,10})\b`. This matches the `"PNR: $pnr"` part emitted by [formatTravelExpenseTitle](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/pnr/PnrTicketModels.kt#L205). Priority is bracket → labelled → 10-digit. | **PASS** (N-P3-2) |
| **P2-7** | Two-pass QR decode ≤1600px, recycle, OOM catch | [UpiExpressPaymentSheet.kt:L191-L264](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/UpiExpressPaymentSheet.kt#L191-L264): `inJustDecodeBounds`, power-of-two `inSampleSize`, streams closed via `use`, `recycle()` in `finally`, `OutOfMemoryError` caught. Runs on `Dispatchers.IO` (L561). Worst case is 1600² × 4 B ≈ 10 MB `IntArray`. | **PASS** |
| **P2-8** | Adaptive Traveler/Member, Trip/Group Spend | [TripHomeScreen.kt:L472-L492](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/TripHomeScreen.kt#L472-L492), [L1250](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/TripHomeScreen.kt#L1250); singular/plural handled. | **PASS** (N-P3-5) |
| **P3-1** | `tnum` on monetary text | Flight L773 (LocalTextStyle provider), L991, L1398, L1440, L2420, L2537. PNR L668, L746, L1095, L1478, L1565-L1591, L1881-L1902. | **PASS** |
| **P3-2** | Dark periwinkle settings icon circle | [OnboardingAndSettingsScreens.kt:L815-L816, L872-L873](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt#L815-L816): `#282552`/`#DCE3FD` | **PASS** (N-P3-8) |
| **P3-3** | NavHost KDoc | [SplitMateAppNavHost.kt:L146-L180](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/navigation/SplitMateAppNavHost.kt#L146-L180) documents the primary vs optional root clearly. | **PASS** |
| **P3-4** | Remove `FlightSplitMember.shareAmount` | [Flight:L407-L418](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L407-L418) now has only `shareAmountPaise: Long`. | **PASS** |
| **TL** | AutoMirrored icons + `@Suppress` hygiene | Every `Icons.AutoMirrored.*` use has a matching `automirrored.rounded.*` import. `@Suppress("UNUSED_PARAMETER"/"UNUSED_VARIABLE")` is scoped to declarations (TripHome L334-346, L2581; Onboarding L416, L444; QuickExpense L291). | **PASS** (N-P3-1) |
| **Rules** | Integer cents / 0.00 drift / zero emojis / Buckwheat palette | No `Double` money math was added. An emoji scan (`\p{Emoji_Presentation}`) over `app/src/main` found **0 hits**. The only pictographic-class glyphs are text arrows (`↗`), which predate this pass. No new cold-blue tokens. | **PASS** |
| **Build** | `gradle testDebugUnitTest assembleRelease`, 0 `e:`/`w:` | **Not executed** (permission timeout) | **UNVERIFIED** |

**Tally**: 17 of 17 remediation items PASS on static review. The build gate is UNVERIFIED.

---

## 2. New Findings

### P0: none
### P1: none

### P2
- **N-P2-1: QuickExpense `+1p` remainder badge disagrees with the persisted split (pre-existing, but it breaks the "preview == persisted" rule).**
  - Where: [QuickExpenseAndGuideScreens.kt:L1096](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/QuickExpenseAndGuideScreens.kt#L1096).
  - What happens: the badge and coin catch go to `selectedMemberIds.firstOrNull()`, which is Set insertion order, and only one member is marked. The commit path `commitQuickEqualExpense` → `splitEquallyZeroDrift` orders **payer-first, then memberId**, and gives `+1` to the first `remainder` members (up to N−1).
  - Result: for ₹100.00 ÷ 3 paid by B, the UI marks A while Room credits B.
  - Fix: apply the same approach as P1-4. Derive the badge set from `SplitMateMathEngine.splitEquallyZeroDrift(...).filter { it.plusOneCent }`.

### P3
- **N-P3-1: Latent deprecation warning.**
  - Where: [SplitMateTheme.kt:L896](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateTheme.kt#L896) still uses `Icons.Rounded.ReceiptLong`.
  - Why it matters: this API is deprecated in material-icons 1.6.x, which BOM 2024.04.01 pulls in. It will emit a `w:` line on a clean or non-incremental build and break the "0 `w:`" gate.
  - Fix: switch to `Icons.AutoMirrored.Rounded.ReceiptLong`.
- **N-P3-2: PNR duplicate lookup uses substring matching.**
  - Where: [SplitMateViewModel.kt:L672-L676](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateViewModel.kt#L672-L676).
  - What happens: the lookup against existing titles is `exp.title.contains(detectedPnr)`. With the new 6-10 character alphanumeric label regex, a false hit silently *overwrites* an unrelated expense through `editExistingExpense`.
  - Fix: use a `\b…\b` word-boundary matcher, preferably anchored to `PNR:\s*`.
- **N-P3-3: Touch-bound growth without compensation (small layout shift).**
  - Flight "Switch Group": `.padding(end = 8.dp).minimumInteractiveComponentSize().size(40.dp)` ([Flight:L866-L869](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L866-L869)) widens the action slot from 48 to 56 dp.
  - Transit deck pills ([L608-L611](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/components/AnimatedTransitDeckHeroCard.kt#L608-L611), L920): the row grows from 38 to 48 dp inside the fixed hero card.
  - Check both on the smallest supported width and font scale 1.3.
- **N-P3-4: Header row grows about 24dp.**
  - Where: [SplitMateAppComposable.kt:L3604-L3606](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/SplitMateAppComposable.kt#L3604-L3606).
  - What happens: the 24dp `ⓘ` toggle now reserves 48dp of layout, so the "Trip Settlement Summary" title row gets taller.
  - Fix: add `Modifier.offset`/negative padding compensation, or keep 24dp visuals inside a 48dp `Box` that overlaps the row.
- **N-P3-5: `isTravelGroup` defaults to travel.**
  - Where: [TripHomeScreen.kt:L476](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/TripHomeScreen.kt#L476) uses `?: true`, and the default `iconName` is `"Flight"` everywhere (RoomEntities L29, create dialog L2846, sync fallback L1500).
  - Result: a household group created without picking an icon still reads "Travelers / Trip Spend". This is acceptable, but document it.
- **N-P3-6: Dark enabled CTAs have low container contrast against the canvas.**
  - Flight `#282552` ([L960](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L960)) and PNR `ForestTop #264010` ([L646](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/PnrExpenseReviewScreen.kt#L646)) sit on `#141311` at about 1.3:1 and 1.6:1 (WCAG 1.4.11 non-text contrast).
  - The labels are legible, but this is the same situation P1-2 fixed on QuickExpense. Consider the sage inversion (`#D7E8B6`/`#1E2F08`) for consistency.
- **N-P3-7: Flight edit path does not fully mirror the preview.**
  - Where: [Flight:L938-L945](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/FlightExpenseReviewScreen.kt#L938-L945) → `editExistingExpense`.
  - Difference 1: it resolves members from `existing.groupId`, and the payer via `newPayerId.ifBlank { existing.payerId }` with no current-user fallback.
  - Difference 2: when `existingFlightExpenseInGroup` comes from the `existingFlightMatchAnyGroup` cross-group fallback (L561), the preview uses active-group members while persistence uses the other group's members.
  - This is rare, but the `+1p` placement can differ.
- **N-P3-8: Cobalt blue in light mode (pre-existing).** The light `SettingsRowItem` tint is `#244896` on `#E8EDFB` ([Onboarding:L815-L816](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/ui/screens/OnboardingAndSettingsScreens.kt#L815-L816)). That is outside the Stitch Periwinkle token (`#3730A3` on `#EEF2FF`).
- **N-P3-9: Share target shows no feedback.** Because of P1-5, SplitMate now appears for *every* `text/plain` share. Sharing text without an SM2 token opens the app with no feedback ([MainActivity.kt:L79-L89](file:///usr/local/google/home/karadkar/splitmate/android/app/src/main/java/com/splitmate/app/MainActivity.kt#L79-L89)). Consider a status banner saying "No SplitMate sync code found".

### Informational
- **I-1:** M3 `Surface(onClick = …)` already applies `minimumInteractiveComponentSize()` internally. On chips with no fixed `size()`, the added modifiers (P2-3, UPI L1039, Classic filter chips) are harmless no-ops. They matter only where an outer `.size(<48.dp)` was present (PNR/Flight top bar, `ⓘ` toggle).

---

## 3. Regression Checks

| Check | Result |
| :--- | :--- |
| Light mode unchanged | Every dark change sits behind an `if (isDark)` branch, and the light branches keep the Report 11 values. Not proven by diff (git blocked). |
| Layout shift | None on PNR (compensated). Small growth on Flight Switch Group, transit pills, and the `ⓘ` row (N-P3-3/4). |
| Math parity (Flight preview == persisted) | **Holds on the create path.** Rare divergence on the edit path (N-P3-7). QuickExpense badge mismatch (N-P2-1). |
| Integer cents | Preserved. All new math uses `Long` paise. |
| Compile hygiene (static) | Imports resolve. Suppress annotations are scoped. One latent deprecation (N-P3-1). **Build not run.** |

---

## 4. Final Verdict

**PASS WITH FINDINGS (provisional).**
- All 17 Report 11 remediation items check out on static review, with 0 P0 and 0 P1 regressions.
- New findings: 1 P2 and 9 P3.
- The verdict becomes final only after someone runs `/usr/local/google/home/karadkar/splitmate/.android-sdk/gradle-8.7/bin/gradle testDebugUnitTest assembleRelease` once from `android/` and confirms **BUILD SUCCESSFUL** with 0 `e:`/`w:` lines.
- Run a clean build (`--rerun-tasks` or a `clean` beforehand) so N-P3-1 is not hidden by incremental compilation.
