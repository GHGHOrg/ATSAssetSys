# intent-cr-06-gain-loss-percent.md (CR6, final)

Change request CR6, 2026-11-26: the unrealized gain/loss as a percentage, beside its dollar figure.
Base: intent.md v52, CR1 to CR5, spec.md v8. Discussion: the session of 2026-11-26 (not written to a file).

## Problem
- The unrealized gain/loss (spec 4.8) shows only in dollars. To judge how a holding or a portfolio is doing, I want its percentage as well, without working it out myself.
- The 1-day gain/loss already shows dollars and percent on one line (CR5); the unrealized gain/loss does not.

## Outcome
1. **Unrealized gain/loss percentage**: wherever the unrealized gain/loss shows today, its percentage shows beside it, in the same format as the dollar figure: the same sign (+ or −) and the same color (green for a gain, red for a loss, none for zero), as spec section 6, Number format and color. For Example A at $160: gain/loss $80 on a cost of $1,200 shows +$80.00 (+6.67%).
2. **Screens** (decided 2026-11-26, open question 1, A): every place the unrealized gain/loss shows today: Overall's portfolio rows, the Portfolios list, a portfolio's header and its holding rows, the Holding detail header, each lot on Holding detail's Lots tab, and each lot in the sell lot picker (spec 4.8, views 1, 3 and 6, AC-14).
3. **What it is of** (decided 2026-11-26, open question 2, A): the gain/loss ÷ cost, the return since purchase. A total (a holding over its lots, a portfolio) is its total gain/loss ÷ its total cost, not an average of its rows' percentages, so the rows' percentages do not add up to the total's. A cost of $0.00 shows no percentage.
4. **"No price" holdings** (decided 2026-11-26, open question 3, A, after a web search): a holding with "no price" (valued at cost, 4.8) shows no percentage and is left out of a total's percentage, its cost as well as its $0 gain/loss, as the 1-day percentage does (4.8). The total keeps its "no price" marker. A stale price counts as it does today. Example: a priced holding (cost $1,200, gain $80) and a "no price" holding (cost $800) give the portfolio +6.67%, not +4.00%.
5. **Decimals** (decided 2026-11-26, open question 4, A): 2, as the 1-day percentage shows ("+$80.00 (+6.67%)" over "+$16.00 (+1.27%)"); a single value, not part of a breakdown (spec section 6, Number format and color).
6. Nothing else changes: the dollar figures, the 1-day line, realized gains (view 5) and how every figure is worked out stay as they are.

## Users
- Me.

## Constraints
- Exact arithmetic, rounded once (spec 4.10).
- Colors, signs and contrast as spec section 6 (Number format and color, Accessibility); color never carries the meaning alone, and TalkBack reads the percentage in full.
- A figure is never cut off at the largest font or in landscape (Accessibility (1), AC-61).
- A derived figure only: no new stored data, so no database schema change and no new backup format version (spec section 8; my reading).
- Each changed screen gets a screen model (CR2; open item 48 for screens built before 15a) and the screen tests that apply (CR3, AC-61).
- Wording that spec.md does not give (a TalkBack label) is D9: shown to me and approved.
- Shipped through the release process (CR4, plan/release.md). A new feature: the next release is 1.2.0 (CLAUDE.md section 4).

## Open questions (Plan)
None remaining. All were answered on 2026-11-26:
1. Answered 2026-11-26 (A): see Outcome 2.
2. Answered 2026-11-26 (A): see Outcome 3.
3. Answered 2026-11-26 (A): see Outcome 4.
4. Answered 2026-11-26 (A): see Outcome 5.

## Deferred to Design
- The spec edits: 4.8 (the percentage), section 6 views 1, 3 and 6 and Number format and color, AC-14, the acceptance criteria, and the wireframes that change.
- Where the percentage sits on a narrow row at the largest font (same line or the line under it).
- A gain or loss too small to show at 2 decimals (for example $0.01 on $10,000.00): its sign and color, and whether "+0.00%" is shown.

## Deferred to Build
- The iterations and their order (plan.md).
- Screen models, screen tests and D9 wording.
