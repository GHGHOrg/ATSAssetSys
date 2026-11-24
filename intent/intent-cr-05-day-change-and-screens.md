# intent-cr-05-day-change-and-screens.md (CR5, final)

Change request CR5, 2026-11-24: a 1-day gain/loss, four smaller screen changes and an About screen, after a first use of the app with my real data (1.0.0).
Base: intent.md v52, CR1 to CR4, spec.md v7. Discussion: the session of 2026-11-24 (not written to a file).

## Problem
- The app shows only the unrealized gain/loss since purchase. Spec 4.8 says on purpose: "no 1-day change is shown". With real data I want to see how the day went.
- Holding detail opens on Transactions, and its tabs are Transactions, Lots, Classification. I look at the lots more often than at the transactions.
- A lot shows its cost and its unrealized gain/loss, but not what it is worth now.
- Overall shows net worth with cash in it. The value of my investments alone (net worth without cash) is not shown anywhere as one figure; Net Worth (Total) lists each portfolio's value, but not their sum.
- Nothing in the app shows which version is installed. The About screen was dropped before 1.0.0 (plan/iterations/40-release-1.0.0.md, line 4) and comes back here.

## Outcome
1. **1-day gain/loss** (change #1): the change in value since the previous market close, shown wherever the unrealized gain/loss shows today (decided 2026-11-24, open question 1, A): Overall (net worth and each portfolio row; net worth's line sits in the card Overall shares with Net Worth (Total), so that screen shows it too), the Portfolios list, a portfolio's header and its holding rows, and the Holding detail header. Lots get none. Broker style (decided 2026-11-24, open question 2, A): a share held at the previous close counts from that close, a share bought today (New York date, C13) counts from its buy price per share, and a share sold today is left out (its result is a realized gain, view 5). Stale and missing prices as for the unrealized gain/loss (decided 2026-11-24, open question 2a, A): a stale price gives the day figure from its last known price and previous close, with the stale marker; a "no price" holding shows no day figure and adds $0 to the totals; a total that includes either carries the marker it carries today.
2. **Holding detail tabs** (change #2): Lots and Transactions swap places, so the tabs are Lots, Transactions, Classification. It opens on Lots, and the tab is still not saved (decided 2026-11-24, open question 3, A).
3. **A lot's current value** (change #3): each lot shows its remaining quantity × the latest price, with the stale or "no price" flag as today (4.8). On Holding detail's Lots tab only, beside the lot cost; the sell lot picker stays as it is (decided 2026-11-24, open question 4, A).
4. **Net worth without cash** (change #4): the sum of all portfolio values (4.8), shown as its own line under net worth in the net worth card, which Overall and Net Worth (Total) share (NetWorthCard), so both screens show it. Its value only, no gain/loss of its own. Net Worth (Total)'s composition list stays as it is, with no subtotal (decided 2026-11-24, open question 5, A2, after a mock). Its name is D9.
5. **About** (change #5): a screen in the More tab that shows the app's name and the installed version (for example "ATS Asset System, 1.0.0", or "ATS Dev, 1.0.0-dev.50"), and the open-source libraries the app uses with their licences (decided 2026-11-24, open question 6, C). A render of the screen is shown to me for approval before it is built.

## Users
- Me.

## Constraints
- Free price source only (intent v52). The 1-day change needs the previous close from the source. **[UNVERIFIED]** That Finnhub's /quote answer carries the previous close (my reading: a field `pc`) is not checked; the app reads only `c` and `t` today (FinnhubSource.kt).
- Exact arithmetic (spec 4.10); colors and signs as spec section 6, Number format and color (it already says a later change since the previous close uses the same colors).
- Prices are not in the backup file (spec section 8), so my reading is that the backup format version does not change. A stored previous close would be a database schema change that the app migrates.
- Each new or changed screen gets a screen model (CR2) and the screen tests that apply (CR3, AC-61).
- Wording that spec.md does not give is D9: shown to me and approved.
- Shipped through the release process (CR4, plan/release.md). New features: the next release is 1.1.0 (CLAUDE.md section 4).

## Open questions (Plan)
None remaining. All were answered on 2026-11-24:
1. Answered 2026-11-24 (A): see Outcome 1.
2. Answered 2026-11-24 (A): see Outcome 1.
2a. Answered 2026-11-24 (A): see Outcome 1.
3. Answered 2026-11-24 (A): see Outcome 2.
4. Answered 2026-11-24 (A): see Outcome 3.
5. Answered 2026-11-24 (A2): see Outcome 4.
6. Answered 2026-11-24 (C): see Outcome 5.
7. Answered 2026-11-24 (A): one change request for all five, built as several iterations. A release can follow any of them, when I decide (CR4); new features raise MINOR (1.1.0, then 1.2.0 if released in parts).

## Deferred to Design
- The spec edits: 4.8 (the 1-day change and net worth without cash), section 3 (Instrument: previous close), section 6 views 1, 2, 3 and 6, the More entry and the navigation outline (About), section 7 (what the quote carries), the acceptance criteria, and the wireframes that change, the About screen's render among them (Outcome 5).

## Deferred to Build
- Checking the previous close in the source's answer (a call shown and approved first).
- The schema migration and its test.
- The iterations and their order (plan.md).
- Screen models, screen tests and D9 wording.
- How the licence list is made (a Gradle plugin or a hand-kept list), and its effect on the APK size (CLAUDE.md section 8, the three criteria).
