# plan/plan.md

Stage: Build. Status: approved, together with plan/setup.md. Setup run 2026-10-18 (setup.md section 13). Iteration 01 is done. The dependency-upgrade spike has run and is merged (pull request 73, merge commit 7df2049). Iteration 02 is split into 02a and 02b (section 12). Iteration 02a is done: built and checked on the phone; pull request #79 merged as 68c9015, CI green on the pull request and on main (your reports); see its file. A spike to set up Claude Code in VS Code ran between 02a and 02b (section 7); pull request #81 merged as 0c2b49e, CI green on main (your report). 02b is done: built, checked on the phone, pull request #83 merged as 0a20873; iterations 03 onward have not started.
Source: intent/intent.md v52 (final), spec/spec.md v4 (final).
Iteration 01 is built, checked on the phone and merged (see its file). 02a is built, checked on the phone and merged (see its file). Where this file says "my reading", it is
an interpretation of spec.md that you approve, not a test.

## 1. Purpose and gate
Ruleset A rule 1: no code before an approved plan.md. This file and plan/setup.md are
approved together. After that:
1. plan/setup.md is run (a prerequisite, not an iteration).
2. Each iteration has its own file in plan/iterations/. It is approved by you before
   its code starts (section 5).
3. If implementation departs from a plan file, that file is updated in the same commit
   (Ruleset A rule 4).

## 2. Files
```
plan/
  plan.md                      this file
  setup.md                     repo, stack, CI, manifest baseline, CLAUDE.md
  spikes/price-source.md       decision note, run 2026-10-30 (summary in its section 11); finished: pull request #98 merged as 5cc77da, before 09
  spikes/dependency-upgrade.md decision note, run after iteration 01 (results in its section 9); finished: pull request 73 merged as 7df2049, before 02
  spikes/vscode-claude-code.md decision note, run after iteration 02a and before 02b (results in its section 9); finished: pull request #81 merged as 0c2b49e
  open-items.md                one list of what is still open; updated when an item closes (spike vscode-claude-code, D4)
  iterations/01-... to 35-...  one file per iteration (section 4)
CLAUDE.md                      repo root, created by setup.md
```

## 3. Technical decisions (from setup.md, approved)
- Native Kotlin, Jetpack Compose, Gradle (Kotlin DSL, version catalog).
- Two modules at the start: `core/` (K1 rules engine and the K2 interface, pure
  Kotlin/JVM, no Android classes) and `app/` (everything Android). More modules are
  added by the iteration that needs them.
- Data layer: Room on SQLite with SQLCipher. K2 is an interface in `core/`. Records and
  prices are separate tables. Derived state is rebuilt by replay and cached in memory.
- Exact arithmetic everywhere in K1 (4.10): arbitrary-precision integers, no floating point.
- Screens draw from screen models (CR2, 15a): a screen shows only figures, texts and messages that a plain Kotlin model prepares (no Compose, no resources), and picks the approved string (strings.xml, D9) to put them in. Each model is built by a builder (ScreenModels.kt, or its own file) and unit tested; one ViewModel test per screen checks that the model reaches the state. Applies to new and changed screens from 15a on; the screens built before are listed in open item 48.
- minSdk 33. Manifest baseline in setup (no automatic backup of app data).
- Figma for design tokens only. The Compose theme file is the source of truth.
- Tool and library versions are looked up and pinned when used, never taken from memory.

## 4. Iterations
Rules for the list:
- Each iteration adds one end-user capability, with the least new functionality and the
  least effort that still gives you something you can use.
- The order follows spec dependencies (section 6). Iteration numbers are stable. If one
  is split or merged later, plan.md records it in the change log (section 12).
- "Spec refs" are the starting reading list. The iteration file confirms them.
- Criteria numbers are my reading and are not run.

| # | File | After this you can... | Depends on | Spec refs |
|---|---|---|---|---|
| 01 | unlock-app | Unlock with biometric or PIN; screenshots and recents blocked | setup | 9, crit. 6, 20, 35 |
| 02a | opening-balance-stored | Set the opening balance; it is stored encrypted and Overall shows it as net worth | 01, upgrade spike | 3, 4.1, 4.2, 4.8, 4.10, crit. 19, 26, 35, 36, 37 |
| 02b | bottom-bar-and-cash-tab | Use the four-tab bottom bar and a Cash tab showing the balance | 02a, Claude Code spike | 6 (navigation outline, view 7), crit. 9, 39 to 42, 49 (part) |
| 03 | deposit-withdrawal | Add deposits and withdrawals; overdrafts blocked | 02 | 4.2, crit. 42, 53 |
| 04 | interest-adjustment | Add interest/dividend and adjustment entries | 03 | 4.2, crit. 53 |
| 05 | reverse-cash-entry | Reverse a cash entry | 04 | 4.6 (cash), crit. 53 |
| 06 | cash-filters-date-type | Filter the cash list by date range and type | 02 | crit. 9, 29, 49 |
| 07 | create-portfolio | Create portfolios | 02 | 4.7, crit. 26, 55 |
| 08 | rename-portfolio | Rename a portfolio | 07 | 4.7, crit. 55 |
| 09 | buy-stock | Buy a stock (verified ticker, review, cash line, lot) | 07, price spike | 4.1, 4.3, crit. 15, 30, 31, 52 |
| 10 | holding-detail | Open a holding (header, Transactions, Lots) | 09 | view 6, crit. 48 |
| 11 | sell-with-lots | Sell with lot picking and Sell all | 09 | 4.4, crit. 14, 52, 59 |
| 12 | split | Record a stock split | 09 | 4.5, crit. 53 |
| 13a | transaction-detail | Open a buy, sell or split from Holding detail or a cash trade line | 11, 12 | view 7, 10b frames G and H, crit. 29, 53 |
| 13b | reverse-transaction | Reverse a buy, sell or split | 13a | 4.6, crit. 53 |
| 14a | reverse-and-reenter-trades | Reverse and re-enter a buy or sell | 13b | 4.6, crit. 21, 54, 60(b) |
| 14b | reverse-and-reenter-split-cash | Reverse and re-enter a split or cash entry | 14a, 05 | 4.6, 10c frame E, crit. 21 |
| 14c | tiny-remainders | Sell out a leftover too small to show; re-enter a buy after a same-date split with its exact shares ([CR1](../intent/intent-cr-01-tiny-remainders.md)) | 14a, 12 | CR1, 3, 4.4, 4.6, 4.10, Examples H and I, crit. 59(e), 59(f) |
| 15 | hide-holdings | Hide and unhide sold-out holdings | 13, 14c | 4.4, crit. 27, 51, 60(a) |
| 15a | screen-models | What three screens show is built as a tested model; the convention for every screen from 16 on ([CR2](../intent/intent-cr-02-screen-models.md); open item 48) | 15 | CR2, views 6 and 7, 4.6, crit. 48, 53, 54 |
| 16 | live-prices | See prices, their age, stale marker, unrealized gain/loss | 09, price spike | 7, 4.8, crit. 5, 41, 42 |
| 17 | net-worth-screen | Net Worth (Total) with composition | 16 | 4.8, crit. 34, 44 |
| 18 | allocation-net-worth | Allocation by asset type, portfolio, holding (Overall, Net Worth) | 17 | crit. 32, 43 |
| 19 | allocation-portfolio | Per-portfolio Allocation screen | 18 | crit. 33, 46 |
| 20 | settings-autolock | Settings screen with the four auto-lock choices | 01 | 9, crit. 6, 58 |
| 21 | screenshot-window | Temporary 3-minute screenshot window | 20 | 9, crit. 20, 58 |
| 22 | tab-order | Reorder the bottom tabs | 20 | crit. 13, 58 |
| 23 | sector-geography-source | Sector and geography from the source, "Unclassified", breakdowns | 16, 18 | 4.9, C14, crit. 12 |
| 24 | sector-geography-override | Override a holding's sector and geography | 23 | 4.9, crit. 22 |
| 25 | manage-lists | Add, rename, delete list values; remembered source texts | 24 | 4.9, crit. 22, 58 |
| 26 | realized-gains | Realized gains with date filter | 11, 13 | view 5, crit. 11, 23, 47 |
| 27 | transaction-history | Per-portfolio history with filters | 13 | view 8, crit. 10, 50 |
| 28 | cash-ticker-filter | Cash ticker filter, Buy/Sell chips, reversal labels | 06, 13 | view 7, crit. 49 |
| 29 | delete-portfolio | Delete a portfolio with summary, typed confirmation, blocked message | 04, 07, 13 | 4.7, crit. 28, 55 |
| 30 | price-source-setting | Change the price source (sweep, offline warning) | 16, 20 | 7, crit. 7, 58 |
| 31 | backup-export | Back up now; "Last backup" line | 02 to 30 (every record type) | 8, 9, crit. 18 |
| 32 | backup-reminder | Overall banner when a backup is due | 31 | 9, crit. 18 |
| 33 | restore-check-report | Check a hand-made file, get the error report and Copy report | 31, 09 | 8, crit. 16 |
| 34 | restore-to-empty-app | Restore a valid file onto an empty app | 33 | 8, crit. 4, 8, 25, 56, 57 |
| 35 | restore-over-data | Restore onto an app that holds data | 34 | 8, crit. 17, 56 |

### Examples as automated tests (K1, crit. 2; my reading)
A in 11, B in 12, C in 09 (it needs a buy), D and F in 13, G in 14a, H and I in 14c, E in 29. Criteria 59
and 60 are tested in the iterations whose rules they cover (integer-cent arithmetic from 02a, exact fractions from 09 and 11,
60(a) in 15, 60(b) in 14a, 60(c) in 17, 60(d) in 11).

### Where components first appear (my reading)
| Component | First built | Grows in |
|---|---|---|
| K1 rules engine | 02a | 03 to 05, 09, 11 to 15, 29, 33 to 35 |
| K2 storage | 02a | every iteration that adds a record type |
| K3 valuation | 02a (cash only) | 09, 16 to 19, 23, 26 |
| K4 price service | 09 (verify only) | 16, 23, 30, 33 to 35 |
| K5 entry flows | 03 | 04 to 05, 07 to 09, 11 to 15, 29 |
| K6 backup and restore | 31 | 32 to 35 |
| K8 app lock | 01 | 20, 21, 34 (file screens, crit. 57) |
| K9 screens | 01 | every iteration |
| K10 classification | 23 | 24, 25 |

## 5. How an iteration runs
1. I draft the iteration file from the template below and show it. You approve or change it.
   Nothing is created on disk before you say so.
2. Branch `iter-NN-name`, one pull request per iteration.
3. Tests are written with the code. A bug fix starts with a failing test.
4. Build, tests and lint are run and the real output is shown (Ruleset A rule 5).
5. Platform behaviour is checked by hand on your phone where the spec says so. The result
   is written in the iteration file as "checked on the phone" or "not checked".
6. The iteration file's status is updated, and plan.md's status table with it.
7. A short review against the iteration's spec refs is done before it is called done.
   The full Deploy review (Ruleset A rule 6) comes after the last iteration.

### Iteration file template
- Outcome: one sentence, in your terms.
- Spec refs: sections, rules, examples and criteria to read.
- Out of scope: what is deliberately not in this iteration.
- Files: expected new and changed files.
- Order of work: numbered steps.
- Stubs: anything faked and where it is replaced.
- Risks: including unverified spec items this iteration touches.
- Proof: exact commands and checks, and what is checked by hand on the phone.
- Decisions needed from you.
- Status and notes.

An iteration that needs more than one capability to reach its outcome is split before it
starts, not after.

## 6. Why this order (my reading of spec.md)
- Cash comes first: nothing else can be entered before the opening balance (4.2), and it
  works offline with no prices.
- A buy needs only ticker verification (4.3), not live prices, because a holding with no
  price is valued at cost (4.8). So buying (09) comes before prices (16).
- Sell, split and reversals follow the buy. Hide needs a sold-out holding and the
  unhide-by-reversal rule (4.4), so it follows reversals.
- Value screens follow prices. Sector and geography follow allocation.
- Realized gains and history are late (D2, C2): they follow the core buy, sell and correct
  features. Portfolio deletion follows reversals and adjustments (Example E).
- Backup and restore are last: section 8 rejects unknown row types, so every record type
  must exist, and the restore verifies held tickers through K4.

## 7. Spike: price source (plan/spikes/price-source.md)
Not an iteration, because it gives you no feature. It is a decision note, and iteration 09
cannot start until it is finished. It must answer, with checks done on the day, not from memory:
- which free source, its limits and terms;
- whether it supplies instrument type (stock, ETF, mutual fund), exchange, currency, and
  sector and geography (decides how 4.3 works and what iteration 23 can do);
- the exchange names and aliases it actually reports (the 29-name US list in the hand-off,
  plus the tickers named in section 7 of the spec), and the wrong-rejection risk with the
  "reject only clearly OTC or non-US names" fallback;
- whether it gives a previous close (the later price-change idea);
- whether it can be replaced behind the K4 interface;
- the source list the settings screen will show (iteration 30).
If the source does not supply sector and geography, iteration 23 is reduced to
"Unclassified" plus the override path, and the change is recorded here.

### Exchange table (spec 4.3; price-source spike decision 4)
The exchanges the ticker check accepts: the 29 national securities exchanges registered under Section 6(a),
as the SEC page listed them on 2026-10-30 (price-source note 10.1), with their former names from that page
and the MIC codes Finnhub reports (note 10.3). A name is matched ignoring case and spaces as in 4.9. Any other
name or code, OOTC (OTC) included, is refused. The same table is in core/ (TickerCheck.EXCHANGES, iteration 09).

| SEC name | Aliases (former names; MIC code) |
|---|---|
| 24X National Exchange LLC | none |
| BOX Exchange LLC | BOX Options Exchange LLC |
| Cboe BYX Exchange, Inc. | Bats BYX Exchange, Inc.; BATS Y-Exchange, Inc. |
| Cboe BZX Exchange, Inc. | Bats BZX Exchange, Inc.; BATS Exchange, Inc.; BATS |
| Cboe C2 Exchange, Inc. | none |
| Cboe EDGA Exchange, Inc. | Bats EDGA Exchange, Inc.; EDGA Exchange, Inc. |
| Cboe EDGX Exchange, Inc. | Bats EDGX Exchange, Inc.; EDGX Exchange, Inc. |
| Cboe Exchange, Inc. | none |
| Green Impact Exchange, LLC | none |
| Investors Exchange LLC | IEXG |
| Long-Term Stock Exchange, Inc. | none |
| MEMX, LLC | none |
| Miami International Securities Exchange | none |
| MIAX Emerald, LLC | none |
| MIAX PEARL, LLC | none |
| MIAX Sapphire, LLC | none |
| MX2 LLC | none |
| Nasdaq GEMX, LLC | ISE Gemini |
| Nasdaq ISE, LLC | International Securities Exchange, LLC |
| Nasdaq MRX, LLC | ISE Mercury |
| Nasdaq PHLX LLC | NASDAQ OMX PHLX, LLC; Philadelphia Stock Exchange |
| Nasdaq Texas, LLC | Nasdaq BX, Inc.; NASDAQ OMX BX, Inc.; Boston Stock Exchange |
| The Nasdaq Stock Market | XNAS |
| New York Stock Exchange LLC | XNYS |
| NYSE Arca, Inc. | ARCX |
| NYSE Texas, Inc. | NYSE Chicago, Inc.; Chicago Stock Exchange |
| NYSE American LLC | NYSE MKT LLC; NYSE Amex; NYSE Alternet US; American Stock Exchange; XASE |
| NYSE National, Inc. | National Stock Exchange, Inc. |
| Texas Stock Exchange LLC | none |

The SEC page writes NYSE American's former names as one phrase ("NYSE MKT LLC, NYSE Amex, NYSE Alternet US,
and the American Stock Exchange"); splitting it into four names is my reading.

### Spike: dependency upgrade (plan/spikes/dependency-upgrade.md)
Added 2026-10-18. Not an iteration. It runs after iteration 01 and before iteration 02, which
cannot start until it is finished. Reason: Android lint reports 8 library-version notices on the
versions Android Studio generated (Kotlin 2.2.10, Compose BOM 2026.02.01, AndroidX libraries),
and those notices stay until this spike. It must answer, with checks done on the day:
- which newer versions to move to, and whether AGP 9.4.1 and Gradle 9.6.0 support them
  (for example the Kotlin version lint reports);
- which Compose BOM and AndroidX versions work together;
- whether the Java 11 target of core/ and app/ still builds;
- which Kotlin style checker to use (deferred from setup step 7) and whether it supports the
  chosen Kotlin version;
- whether Dependabot's Gradle updates need grouping so AGP and Kotlin move together;
- whether the CI runner moves from ubuntu-24.04 to ubuntu-26.04.
Its proof includes re-running iteration 01's phone checks, because iteration 01 is built on the
old versions.
Run 2026-10-21 to 2026-10-22 on branch spike-dependency-upgrade. The results, departures and what
was not verified are in the note's section 9. In short (your reports): Kotlin 2.4.21, 0 lint notices,
Spotless with ktlint 1.8.0, runner ubuntu-26.04, Dependabot group kotlin (unverified until it runs),
and all 14 phone checks of iteration 01 passed.
Merged as 7df2049 (pull request 73); CI on main reported green by you.

### Spike: Claude Code in VS Code (plan/spikes/vscode-claude-code.md)
Added 2026-10-28. Not an iteration. It runs after iteration 02a and before 02b, whose file cannot start until it is finished. Reason: the work moves from claude.ai (Project instructions, Project memory, chat history) to Claude Code in VS Code, and those are not visible there. It must answer, with checks done on the day:
- what must be installed and signed in (VS Code, the extension, the CLI, Git for Windows, the account);
- how the CLI installs on Windows and whether the piped-script route is acceptable;
- where the rules live (CLAUDE.md, user-level file, `.claude/rules`) and how Ruleset A and B get into them;
- what Claude Code may do without asking, and how Manual mode is pinned so Auto cannot start by accident;
- whether web search, web fetch, shell commands, file edits and git push prompt, and which rules guard them;
- where the settings files live on Windows and which wins;
- whether anything is saved outside the repo (auto memory);
- whether the hand-off works as written.
Its proof includes an inventory of everything that lives only in the Project or in chat, each item with a destination you approved.
Run 2026-10-28. The results and what was not verified are in the note's section 9. In short (your reports): checks 1 to 20 passed; the allow rules work only after Claude Code's own trust dialog is accepted for the folder.
Merged as 0c2b49e (pull request #81); CI on main reported green by you.

## 8. Cross-cutting risks
- Real data is unprotected until iteration 31: you can enter real data from 09, but there is
  no backup until 31 and no way to load a prepared file until 34. This is a risk, not a spec change.
- Iterations 02, 09, 16 and 33 to 35 are the heaviest. Each is split before starting if
  its file shows more than one capability.
- Install route (C17): the route is chosen later. Re-check Google's pages then.
  The release signing key stays outside the repo and is backed up (setup.md section 6).
- Keystore (C18): key survival across updates, reinstall and Android upgrades, and the
  lost-key case, are phone tests in iteration 02 and later (crit. 35, 36).
- Backup and device-transfer exclusions (crit. 37): iteration 02 tightens the generic rules
  against the real database path and decides how to verify. Not tested in 02a (deferred by you 2026-10-28); data_extraction_rules.xml is still the generic baseline.
- Secure window and recents preview (crit. 20): the order of setting and clearing the flags
  is planned and tested in iterations 01 and 21.
- Auto-lock and Android's file screens (crit. 57): decided in 31 and 34, tested on the phone.
- Iteration 01 is built on the library versions Android Studio generated. The dependency-upgrade
  spike may break it, so the spike's proof re-runs 01's phone checks. Re-run 2026-10-22: all 14
  checks passed (your report; see the note's section 9).
- Unverified spec items stay unverified until their iteration checks them on the phone.
  The list is in spec.md ([UNVERIFIED] and [PARTLY CHECKED] markers).
- Message wording is D9: shown and approved on the phone in each iteration.
- SQLCipher and other libraries: maintenance and licence are checked in iteration 02.

## 9. Definition of done (one for every iteration)
An iteration is done only when all of these are true and shown:
1. The outcome in its file works on your phone, confirmed by you.
2. Build, tests and lint are green, with the real output shown. Nothing was weakened,
   skipped or deleted to get there.
3. Every spec ref in the iteration file is either implemented, or listed as not done with
   a reason you accepted.
4. The standing checks on its screens are done by hand on the phone: date note and "ET"
   suffix (crit. 9), accessibility, theme, number and color format, error and empty
   states (crit. 39 to 42), and a new build installs over the previous one without losing
   data (crit. 35).
5. Nothing logs or sends amounts, tickers, quantities, notes or portfolio names (section 9).
6. Anything not run or not verified is stated in the iteration file.
7. plan.md and the iteration file match what was actually built.

## 10. CLAUDE.md
Created by setup.md (its section 8 holds the outline): stage rules, commands, conventions,
the spec pointer map, known pitfalls and the standing checks. Each iteration updates it only
when it adds a command, a convention or a pitfall.

## 11. Status
| Item | Status |
|---|---|
| plan.md | approved |
| setup.md | approved, run 2026-10-18 on branch setup (pull request #64, merged) |
| spikes/price-source.md | run 2026-10-30 on branch spike-price-source; thirteen candidates checked, decisions 1 to 7 settled (Finnhub only, key built in). Results in the note's sections 6, 10 and 11. Pull request #98 merged as 5cc77da, CI green on the pull request and on main (1m 47s) (your reports) |
| spikes/dependency-upgrade.md | run 2026-10-21 to 2026-10-22 on branch spike-dependency-upgrade; pull request 73 merged as 7df2049, CI green on main (your report). Results in the note's section 9. Dependabot group unverified until it has run once |
| spikes/vscode-claude-code.md | run 2026-10-28, checks 1 to 20 passed; pull request #81 merged as 0c2b49e, CI green on main (run 40663927506, your report). Results in the note's section 9 |
| Iteration 01 | done: built and checked on the phone 2026-10-19 (check 8 on 2026-10-20); pull request #70 merged as 8a5703e, CI green on main (your report); not-run items are listed in its file |
| Iteration 02a | done: built 2026-10-24 to 2026-10-28 (commits A to M plus the file update a1b9a2c); local run: spotlessCheck, debug build, 74 tests and lint as listed in its file; phone checks done by you 2026-10-27 and 2026-10-28; criterion 37, TalkBack and check 8 deferred by you (see change log); pull request #79 merged as 68c9015 on 2026-10-28, CI green on the pull request and on main (your reports) |
| Iteration 02b | done: built 2026-10-28 (steps 1 to 6); phone checks 2026-10-28 to 2026-10-29 all yes; 98 tests; pull request #83 merged as 0a20873, CI green on the pull request and on main (2m 24s) (your reports) |
| Iteration 03 | done: built 2026-10-29 (steps 1 to 6); phone checks 2026-10-29 all yes; 132 tests; pull request #85 merged as 8d25892, CI green on the pull request and on main (1m 42s) (your reports) |
| Iteration 04 | done: built 2026-10-29 (steps 1 to 5); phone checks 2026-10-29 all yes; 154 tests; pull request #87 merged as 5424a6e, CI green on the pull request and on main (2m 17s) (your reports) |
| Iteration 05 | done: built 2026-10-29 (steps 1 to 5 and one fix); phone checks 2026-10-29 all yes; 183 tests; pull request #89 merged as c1ad96d, CI green on the pull request and on main (2m 25s) (your reports) |
| Iteration 06 | done: built 2026-10-29 (steps 1 to 5 and one change); phone checks 2026-10-29 all yes; 213 tests; pull request #91 merged as 0d5e2f4, CI green on the pull request and on main (1m 51s) (your reports) |
| Iteration 07 | done: built 2026-10-30 (steps 1 to 6 and one fix); phone checks 2026-10-30 all yes, check 10 not run (open item 33); 243 tests; pull request #94 merged as 1eb7220, CI green on the pull request and on main (2m 19s) (your reports) |
| Iteration 08 | done: built 2026-10-30 (steps 1 to 6 and one fix); phone checks 2026-10-30 all yes; 266 tests; pull request #96 merged as 936c7e2, CI green on the pull request and on main (2m 31s) (your reports) |
| Iteration 09 | done: built 2026-10-31 (steps 1 to 10 and two fixes); phone checks 2026-10-31 all yes; 388 tests; pull request #100 merged as e31c558, CI green on the pull request and on main (3m 25s) (your reports) |
| Iteration 10 | done: built 2026-10-31 (steps 1 to 6 and one fix); phone checks 2026-10-31 all yes; 402 tests; pull request #102 merged as 43a2c7f, CI green on the pull request and on main (2m 24s) (your reports) |
| Iteration 11 | done: built 2026-10-31 to 2026-11-01 (steps 1 to 6); phone checks 2026-10-31 to 2026-11-01 all yes; 453 tests; pull request #104 merged as e689af9, CI green on the pull request and on main (2m 16s) (your reports) |
| Iteration 12 | done: built 2026-11-01 (steps 1 to 6 and one fix); phone checks 2026-11-01 all yes after the fix; 495 tests; pull request #106 merged as 08198bd, CI green on the pull request and on main (2m 51s) (your reports) |
| Iteration 13a | done: built 2026-11-01 (steps 1 to 4); phone checks 2026-11-01 all yes; 503 tests; pull request #108 merged as 07dd814, CI green on the pull request and on main (2m 5s) (your reports) |
| Iteration 13b | done: built 2026-11-01 to 2026-11-02 (steps 1 to 6); phone checks 2026-11-02 all yes; 527 tests; pull request #110 merged as e5edb0e, CI green on the pull request and on main (2m 21s) (your reports) |
| Iteration 14a | done: built 2026-11-02 (steps 1 to 6, one fix and one Deploy fix); phone checks 2026-11-02 all yes after the fix; 564 tests; pull request #112 merged as bcd5e3b, CI green on the pull request and on main (2m 59s) (your reports) |
| Iteration 14b | done: built 2026-11-02 to 2026-11-03 (steps 1 to 6, one fix and one Deploy fix); phone checks 2026-11-02 to 2026-11-03 all yes after the fix; 594 tests; pull request #114 merged as 7229ef3, CI green on the pull request and on main (2m 26s) (your reports) |
| Iteration 14c | done: built 2026-11-03 (steps 1 to 6, one Deploy fix); phone checks 2026-11-03 all yes, rechecked after the fix; 611 tests; pull request #116 merged as ae4ec04, CI green on the pull request and on main (3m 7s) (your reports) |
| Iteration 15 | done: built 2026-11-03 (steps 1 to 6, two tests added at Deploy); phone checks 2026-11-03 all yes; 630 tests; database version 6; pull request #117 merged as d7630a8, CI green on the pull request and on main (2m 56s) (your reports) |
| Iteration 15a | built 2026-11-03 (steps 1 to 6, one Deploy fix); phone checks 2026-11-03 all yes (the screens read the same before and after), rechecked after the fix; 649 tests; pull request #120 merged as 2411066, CI green on the pull request and on main (2m 9s) (your reports) |
| Iterations 15 to 35 | not started, files not written |

## 12. Change log
Each departure from this plan is recorded here with its date and the reason.
- 2026-10-18: added the spike plan/spikes/dependency-upgrade.md between iterations 01 and 02.
  Iteration 02 now depends on it. In the iteration table, "spike" in rows 09 and 16 is renamed
  "price spike". Reason: the library-version lint notices and the deferred Kotlin style checker
  (setup.md section 13).
- 2026-10-22: the dependency-upgrade spike ran. Departures from its own plan are in the note's
  section 9.8: the style checker was added to CI (a spotlessCheck step, approved by you) and the
  runner moved to ubuntu-26.04. Iteration 02 still depends on the spike, which is finished when
  its pull request is merged.
- 2026-10-22: pull request 73 was merged as 7df2049 and CI on main was reported green by you. The spike
  is finished and iteration 02 no longer waits on it. The status lines above were updated. The original
  question list in section 7 is left as written, as the plan's record.
- 2026-10-23: iteration 02 split into 02a (opening-balance-stored) and 02b (bottom-bar-and-cash-tab),
  as section 5 requires when an iteration holds more than one capability. Reason: 02 held the opening
  balance entry, the encrypted data layer (Room, SQLCipher, Keystore), the backup exclusions, and the
  bottom bar with the Cash tab. In the Depends-on column of other rows, "02" means both parts.
- 2026-10-23: 02a has no bottom bar (the spec says it is always visible; temporary until 02b) and no
  Create a portfolio button (it arrives with 07). The exact-fraction type is not built in 02a: nothing
  uses it before 09 (price per share), so "arithmetic from 02" is now integer cents from 02a and exact
  fractions from 09 and 11.
- 2026-10-23: data layer decisions for 02a (settled by you): Room 3.0.3 (androidx.room3) with
  SQLCipherDriver, with the K2 interface in core/ using suspend functions; if Room 3 fails on the branch
  I stop and show you before any fall-back to Room 2.8.5. Typed form values are kept by a ViewModel and
  the app still re-locks on recreation. Versions come from pages you pasted on 2026-10-23.
- 2026-10-23: the SQLCipher licence (BSD-style, three conditions, the binary one applies if the app is
  passed on) is a Deploy item: a Licences screen, written then as a spec addition. Spec v4 is unchanged.
- 2026-10-24: wireframes 01 and 10b, their README and the theme files were read for 02a, and you approved my reading (02a file, section 12). Effect on 02a: an optional one-line Note field on the opening balance form; the allocation donut, price lines, backup banner, Buy/Sell button and "Restore is in More only" line of wireframe 01 are left to later iterations (18, 16, 32, 09, 02b); a future-dated opening balance is allowed. Spec v4 and the wireframes are unchanged.
- 2026-10-27: 02a added two files the plan did not list, ui/AppContent.kt and data/SqlCipherOpener.kt, plus closeForm() with a Back button. Details in the 02a file, section 14.
- 2026-10-27: Room 3.0.3's builder has no allowDataLossOnRecovery (checked with javap), so 02a does not call it and calls no destructive-migration method. Room 3.1.0-alpha03 (2026-10-21) adds it; a later version should consider it once 3.1 is stable.
- 2026-10-28: 02a has no bottom bar and shows the Overall screen only (as the 2026-10-23 entry says); the 02a file section 14 records the other departures from its plan (the "locked" wording, the wrapping note field, the review layout).
- 2026-10-28: you deferred three 02a checks to later iterations: criterion 37 (backup and device-transfer test), the TalkBack check (including the TalkBack part of criterion 39), and check 8 on this build. Section 8 and the 02a row of the iteration table still list criterion 37 for 02a. The iteration that picks them up is left undecided until the 02b file is written (your decision 2026-10-28).
- 2026-10-28: pull request #79 (iteration 02a) was merged as 68c9015. CI on the pull request (3 checks) and on main (the run for 68c9015, 2m21s) were reported successful by you. The status lines above were updated. The 02a file's sections 13 to 15 are left as written at build time.
- 2026-10-28: added the spike plan/spikes/vscode-claude-code.md between iterations 02a and 02b. Iteration 02b now depends on it. Reason: the project moves from claude.ai to Claude Code in VS Code, and the Project instructions, memory and chat history are not visible there.
- 2026-10-28: the spike vscode-claude-code settled its decisions D1 to D14 (its section 5). Effects on the plan: the working model for Claude Code is written into CLAUDE.md section 8 (Edit automatically; ask before commit, merge, push, deletions, web search and web fetch; one numbered step at a time); the permission rules are in .claude/settings.json; plan/open-items.md holds the open items; the Ruleset A and B texts are now in CLAUDE.md sections 1 and 2.
- 2026-10-28: the spike vscode-claude-code was run (results in its section 9; checks 1 to 18 and 20 passed, check 19 waits for the pull request). Effects on the plan: CLAUDE.md sections 6 and 8 gained the trust and line-ending pitfalls and the rule about buttons that save a rule; plan/open-items.md has items 24 to 29; the D12 report is accepted as asking about twice per step; 02b still waits for the merge.
- 2026-10-28: check 19 of the spike vscode-claude-code passed (pull request #81, CI green, no key files tracked). Iteration 02b still waits for the merge.
- 2026-10-28: pull request #81 (spike vscode-claude-code) was merged as 0c2b49e. CI on main (run 40663927506, 1m52s) was reported green by you. The spike is finished and iteration 02b no longer waits on it. The status lines above were updated.
- 2026-10-28: the 02b file was approved with decisions 1 to 7. The "Restore is in More only" line on Overall moves from 02b to 34. Of the checks deferred from 02a, TalkBack and check 8 are in 02b; criterion 37 waits for a later iteration, chosen when its file is written. Icons in the bottom bar are a future idea (open item 30).
- 2026-10-29: 02b built and checked on the phone. Decisions 8 (same-date order in the Cash list) and 9 (unlock keeps an open form) were added. The bar uses selectable text items instead of Material's NavigationBarItem. Criterion 35's install-over is checked with a newer build (versionCode 2). TalkBack and check 8 are done for the current screens; criterion 37 is still deferred.
- 2026-10-29: pull request #83 (iteration 02b) was merged as 0a20873. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 0a20873, 2m 24s) were reported successful by you. The status lines above were updated. The 02b file's sections 13 and 14 are left as written at build time.
- 2026-10-29: pull request #84 (docs: the 02b merge record) was merged as 5e46dae, CI green on the pull request and on main (your reports). The 03 file was approved with decisions 1 to 4. Example C is tested cash-only in 03 (a withdrawal stands in for the buy, which arrives in 09). Open item 14's multi-row check moves to 09.
- 2026-10-29: 03 built and checked on the phone. Decision 5 (the balance is judged after every entry) was added. One 02a ViewModel test was replaced, because a second opening balance can no longer be started (your decision; 03 file, section 14). Open item 9 (overflow guard) is closed. Criterion 35's install-over is checked again (versionCode 3).
- 2026-10-29: pull request #85 (iteration 03) was merged as 8d25892. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 8d25892, 1m 42s) were reported successful by you. The status lines above were updated. The 03 file's sections 13 and 14 are left as written at build time.
- 2026-10-29: pull request #86 (docs: the 03 merge record) was merged as 00156bf, CI green on the pull request and on main (2m 2s) (your reports). The 04 file was approved with decisions 1 to 4.
- 2026-10-29: 04 built and checked on the phone. Two 03 tests were narrowed, because Interest/dividend and Adjustment are now allowed (your decisions; 04 file, section 14). Criterion 35's install-over is checked again (versionCode 4).
- 2026-10-29: pull request #87 (iteration 04) was merged as 5424a6e. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 5424a6e, 2m 17s) were reported successful by you. The status lines above were updated. The 04 file's sections 13 and 14 are left as written at build time.
- 2026-10-29: pull request #88 (docs: the 04 merge record) was merged as dc2549e, CI green on the pull request and on main (2m 10s) (your reports). The 05 file was approved with decisions 1 to 4. The Cash rows gain the note line of wireframe 07 marker 4, which 02b did not build.
- 2026-10-29: 05 built and checked on the phone. A defect found in phone check 5 (the reversal showed only the note error when the date was also wrong) was fixed in 05 with failing tests first (your decision; 05 file, section 14). Criterion 35's install-over is checked again (versionCode 5).
- 2026-10-29: pull request #89 (iteration 05) was merged as c1ad96d. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for c1ad96d, 2m 25s) were reported successful by you. The status lines above were updated. The 05 file's sections 13 and 14 are left as written at build time.
- 2026-10-29: pull request #90 (docs: the 05 merge record) was merged as da9f26e, CI green on the pull request and on main (1m 54s) (your reports). The 06 file was approved with decisions 1 to 4.
- 2026-10-29: 06 built and checked on the phone. After phone check 12 the date filters were changed to digits only with the dashes shown (your request; decision 1 changed; 06 file, section 14). A new lint notice, AGP 9.5.0 available, is listed as open item 31. Criterion 35's install-over is checked again (versionCode 6).
- 2026-10-29: pull request #91 (iteration 06) was merged as 0d5e2f4. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 0d5e2f4, 1m 51s) were reported successful by you. The status lines above were updated. The 06 file's sections 13 and 14 are left as written at build time.
- 2026-10-29: pull request #92 (docs: the 06 merge record) was merged as 1c4b85c, CI green on the pull request and on main (2m 4s) (your reports). 2026-10-30: the 07 file was approved with decisions 1 to 4. The phone check of "Set the opening balance first" for New portfolio moves to 09 (open item 33).
- 2026-10-30: the test totals of 03 to 06 were 1 too high: 03 added 34 tests to 02b's 98, written as 133 instead of 132, and 04 to 06 added their new tests to that figure. Corrected to 132, 154, 183 and 213 in the status lines above and in the four files' local-run lines. The per-class counts were right, and no test was removed (git check of @Test lines at each merge). The 03 file's departure 2 now names the renamed test.
- 2026-10-30: pull request #93 (docs: the test-count fix) was merged as 8ee5394, CI green on the pull request and on main (1m 29s) (your reports); main was merged into iter-07-create-portfolio (2d50683).
- 2026-10-30: 07 built and checked on the phone. The first schema change (database version 2, a Room auto migration adding the portfolios table) was checked by installing over 06's build with data (criterion 35, versionCode 7). A defect found in phone check 2 (a dialog window took the focus and the focus cover hid the app in a loop) was fixed in 07 by drawing the dialog inside the activity window, with no failing test possible off the device (your decision; 07 file, section 14). A Logcat check for the portfolio names was added (07 file, section 13.3).
- 2026-10-30: pull request #94 (iteration 07) was merged as 1eb7220. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 1eb7220, 2m 19s) were reported successful by you. The status lines above were updated. The 07 file's sections 13 and 14 are left as written at build time.
- 2026-10-30: pull request #95 (docs: the 07 merge record) was merged as 53a97b8, CI green on the pull request and on main (1m 33s) (your reports). The 08 file was approved with decisions 1 to 3; its decision 3 replaces 07 decision 1 (the portfolio rows become tappable).
- 2026-10-30: 08 built and checked on the phone. Rename is the first update of a stored record (the name of one portfolio row by id). A defect found in phone check 9a.2 (the keyboard covered the in-window dialog at the largest font, in New portfolio from 07 too) was fixed in 08 with imePadding, with no failing test possible off the device (your decision; 08 file, section 14). Criterion 35's install-over is checked again (versionCode 8).
- 2026-10-30: pull request #96 (iteration 08) was merged as 936c7e2. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 936c7e2, 2m 31s) were reported successful by you. The status lines above were updated. The 08 file's sections 13 and 14 are left as written at build time.
- 2026-10-30: pull request #97 (docs: the 08 merge record) was merged as efdec84, CI green on the pull request and on main (2m 7s) (your reports). The price-source spike's outline was approved (plan/spikes/price-source.md). The "29-name US list in the hand-off" named in section 7 is not in the repo; the spike takes the US exchange list from the SEC's list of registered national securities exchanges instead (your decision 1). A source that needs a key is a spec gap (sections 8 and 9), flagged in the note.
- 2026-10-30: the price-source spike was run (note sections 6, 10 and 11). Effects on the plan: the app ships with Finnhub only (decision 3); the exchange table of 4.3 is fixed here, built from the 29 SEC names with the MIC codes XNAS, XNYS, ARCX, BATS, XASE and IEXG as aliases, and any other code is refused (decision 4; the table itself is written with iteration 09); the Finnhub key is built in at build time from FINNHUB_API_KEY, with no spec change (decision 5); Finnhub's deletion term is met by App initialization (decision 6). Iteration 23 is not reduced for stocks; ETFs start as "Unclassified" plus the override. Iteration 30 has only one source to change to (open item 36). Open item 17 is closed; items 35 to 38 are new.
- 2026-10-31: pull request #98 (price-source spike) was merged as 5cc77da. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 5cc77da, 1m 47s) were reported successful by you. The spike is finished and iteration 09 no longer waits on it. The status lines above were updated. Dependabot has opened branches for the Gradle wrapper 9.8.2 and core-ktx 1.19.1 (open item 20).
- 2026-10-31: pull request #99 (docs: the price-source spike merge record) was merged as 5978482, CI green on the pull request and on main (1m 38s) (your reports). The 09 file was approved with decisions 1 to 9. The phone check of open item 33 moves from 09 to 34 (decision 9). Open item 37 is settled by decision 6; open item 38 is closed.
- 2026-10-31: 09 built and checked on the phone. The first network access: Finnhub's list comes as a redirect to a signed link on its file host, which the app follows once, without the key, over https to finnhub.io only (defect found in phone check 3, fixed with failing tests first; 09 file, section 14). A second defect from phone check 7 (a field error hid the cash rule on the review) was fixed the same way. Decision 8 was amended in step 6 (six check-failure messages, the status suffix), and decisions 10 (the buy date as the Cash filters) and 11 (portfolio chips) were added in step 8. The exchange table of 4.3 is in section 7 above. Database version 3 adds the transactions and instruments tables. Criterion 35's install-over is checked again (versionCode 9).
- 2026-10-31: pull request #100 (iteration 09) was merged as e31c558. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for e31c558, 3m 25s) were reported successful by you; CI builds without FINNHUB_API_KEY, as planned. The status lines above were updated. The 09 file's sections 13 and 14 are left as written at build time.
- 2026-10-31: pull request #101 (docs: the 09 merge record) was merged as c372de8, CI green on the pull request and on main (2m 40s) (your reports). The 10 file was approved with decisions 1 to 5: only the Transactions and Lots tabs (Classification in 23), only Buy in the header (Sell in 11, Split in 12), and Transactions rows and the Cash buy lines open nothing until 13 (09's "10 or 27" becomes 13).
- 2026-10-31: 10 built and checked on the phone. A defect found in phone check 6a (at the largest font the fixed header hid the list) was fixed in 10: the header stays in place while it takes at most half the screen's height, and the whole screen scrolls as one when it is taller, at the largest font and in landscape (your decisions; 10 file, section 14). No failing test was possible off the device. Criterion 35's install-over is checked again (versionCode 10).
- 2026-10-31: pull request #102 (iteration 10) was merged as 43a2c7f. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 43a2c7f, 2m 24s) were reported successful by you. The status lines above were updated. The 10 file's sections 13 and 14 are left as written at build time.
- 2026-10-31: pull request #103 (docs: the 10 merge record) was merged as 385f3fb, CI green on the pull request and on main (2m 30s) (your reports). The 11 file was approved with decisions 1 to 3: the ticker on Sell is chosen with chips (not the wireframe's dropdown, which opens a separate window), Sell sits beside Buy in Holding detail's header and returns to the holding, and a sell allocation's exact quantity is stored as a reduced fraction in one text column (table sell_allocations, schema version 4).
- 2026-11-01: 11 built and checked on the phone, with no defect found. Database version 4 adds the sell_allocations table; a sell, its cash line and its allocations are written as one action. My instructions for phone check 3 did not say where to start, so it ran from the holding's Sell; the checks after it were rewritten with their start named (11 file, section 14). "1 shares" stays until 12, which takes singular and plural "share" (open item 41). Criterion 35's install-over is checked again (versionCode 11).
- 2026-11-01: pull request #104 (iteration 11) was merged as e689af9. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for e689af9, 2m 16s) were reported successful by you. The status lines above were updated. The 11 file's sections 13 and 14 are left as written at build time.
- 2026-11-01: pull request #105 (docs: the 11 merge record) was merged as dde7568, CI green on the pull request and on main (1m 54s) (your reports). The 12 file was approved with decisions 1 to 4: the Split screen chooses with chips like Sell, a split at 0 shares is allowed, each ratio number is a whole number from 1 to 1,000,000, and "share" is used only for a quantity shown as exactly 1 (open item 41). Split stays a header button as view 6 and criterion 48 say; a ⋮ menu for it is open item 42.
- 2026-11-01: 12 built and checked on the phone. Database version 5 adds two columns to transactions (splitTo, splitFrom). A defect found in phone check 6 (Sell all after a 1 for 3 split was refused as more than held, because remainders rounded up at 8 places add to more than the exact holding) was fixed with failing tests first: "more than held" compares with the lots' remainders at 8 places (your decision; 12 file, section 14). Open item 41 is closed by 12; open item 44 (a leftover too small to show) is new and due before 15. Criterion 35's install-over is checked again (versionCode 12).
- 2026-11-01: pull request #106 (iteration 12) was merged as 08198bd. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 08198bd, 2m 51s) were reported successful by you. The status lines above were updated. The 12 file's sections 13 and 14 are left as written at build time.
- 2026-11-01: pull request #107 (docs: the 12 merge record) was merged as c5dad53, CI green on the pull request and on main (2m 50s) (your reports). Iteration 13 split into 13a (transaction-detail) and 13b (reverse-transaction), as section 5 requires when an iteration holds more than one capability (your decision): viewing a buy, sell or split, reached from Holding detail and from a cash trade line (criterion 29), and reversing one (4.6). In the Depends-on column of rows 14, 15, 26 and 27, "13" means 13b, which depends on 13a. The 13a file was approved with decisions 1 to 3: a sell's detail shows the lots used and the realized gain as on its review, a buy's detail shows what is left of its lot, and Back goes one step back.
- 2026-11-01: 13a built and checked on the phone, with no defect found and no schema change. Holding detail's rows and the Cash tab's trade lines open the transaction (criterion 29); a trade line's detail is titled with its buy or sell and links back to it, keeping 05's "Back" button (your decision; 13a file, section 14). Criterion 35's install-over is checked again (versionCode 13).
- 2026-11-01: pull request #108 (iteration 13a) was merged as 07dd814. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 07dd814, 2m 5s) were reported successful by you. The status lines above were updated. The 13a file's sections 13 and 14 are left as written at build time.
- 2026-11-01: pull request #109 (docs: the 13a merge record) was merged as 460d3b6, CI green on the pull request and on main (2m 39s) (your reports). The 13b file was approved with decisions 1 to 3: a reversal is stored as a REVERSAL holding only its link, date and entry order, with every figure worked out from the original; Reverse is on the original's detail only; a refused buy reversal names the earliest sell not yet reversed.
- 2026-11-02: 13b built and checked on the phone, with no defect found and no schema change. A reversal is a REVERSAL row holding its link; Examples D and F hold on the phone. A lot's remainder for a pick is now the least it holds on the sell date or later, since a reversal can raise a lot again. A split's reversal applies its inverse ratio to every lot placed before it, as 4.6 is written (your decision; 13b file, section 14). Steps 1 to 6 were committed together on your request, so the intermediate commits do not build alone. Criterion 35's install-over is checked again (versionCode 14).
- 2026-11-02: pull request #110 (iteration 13b) was merged as e5edb0e. CI on the pull request and on main (the run for e5edb0e, 2m 21s) were reported successful by you. The status lines above were updated. The 13b file's sections 13 and 14 are left as written at build time.
- 2026-11-02: pull request #111 (docs: the 13b merge record) was merged as 8d2769f, CI green on the pull request (3 checks) and on main (2m 49s) (your reports). The 13b status row now starts with "done:", which #111 left out. Iteration 14 split into 14a (reverse-and-reenter-trades) and 14b (reverse-and-reenter-split-cash) before it started, as section 5 requires (your decision): Reverse and re-enter on a buy or sell, which carries the pair check, the combined review and every criterion figure (21, 54, 60(b) in part), and then on a split or cash entry, reusing them. 60(b)'s restore, hidden and Realized gains parts wait for 34, 15 and 26. The 14a file was approved with decisions 1 to 4: the split; after the save the app returns to the original's detail; only a refusal about the reversal date opens the combined review; Change opens an inline date field there.
- 2026-11-02: 14a built and checked on the phone, with no schema change. A pair is a reversal and a normal buy or sell saved in one database transaction; Example G and criterion 54 hold on the phone, with a new buy dated before its reversal. Decision 5 was added after step 2: on a shared date the cash replay judges the pair as one point. A defect found while working out phone check 2 was fixed with failing tests first: a re-entered entry's first quantity now carries the splits entered later on the original's date, as its picks do (your decision B; 14a file, section 14, item 11; open item 46 for the 8-place limit). Criterion 35's install-over is checked again (versionCode 15).
- 2026-11-02: the 14a Deploy review found one Medium bug, fixed on the branch with a failing test first (your decision A): Confirm on the combined review could refuse an unfinished reversal date without showing why (14a file, section 14, item 12). The other findings are covered by open items 45 and 46.
- 2026-11-02: pull request #112 (iteration 14a) was merged as bcd5e3b. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for bcd5e3b, 2m 59s) were reported successful by you. The status lines above were updated. The 14a file's sections 13 and 14 are left as written at build time.
- 2026-11-02: pull request #113 (docs: the 14a merge record) was merged as c509d66, CI green on the pull request (3 checks) and on main (2m 56s) (your reports). The 14b file was approved with decision 1: in a cash entry's Reverse and re-enter, the reversal's note is the pre-filled one and cannot be edited; a plain Reverse still lets it be edited.
- 2026-11-03: 14b built and checked on the phone, with no schema change. A split pair is two transactions saved in one database transaction, its lots checked once with both splits applied; a cash pair is two cash entries saved as one batch and judged in one replay, so CashEntries.kt was split into drafts and a replay. A defect found in phone check 3 was fixed with failing tests first: the combined review showed a stand-in note in place of the typed one (your decision A; 14b file, section 14, item 6). Criterion 35's install-over is checked again (versionCode 16).
- 2026-11-03: the 14b Deploy review found one High bug, fixed on the branch with a failing test first and rechecked on the phone (your decision A): a split re-entered on another ticker or portfolio could be saved while its reversal left the original holding short (14b file, section 14, item 8). A stale comment (Low) was fixed with it.
- 2026-11-03: pull request #114 (iteration 14b) was merged as 7229ef3. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 7229ef3, 2m 26s) were reported successful by you. The status lines above were updated. The 14b file's sections 13 and 14 are left as written at build time.
- 2026-11-03: pull request #115 (docs: the 14b merge record) was merged as 071d490, CI green on the pull request (3 checks) and on main (2m 1s) (your reports). Open items 44 and 46 were taken up before 15 as change request CR1 (intent/intent-cr-01-tiny-remainders.md; your decisions): from now on each change request has its own intent file, intent/intent-cr-NN-short-name.md, linked from plan.md, and intent.md stays at v52. spec.md v5: a leftover too small to show is shown and picked as 0.00000001 (4.4, 4.10), and an unchanged re-entered buy keeps the original's exact quantity through `quantity_ratio` (3, 4.6, section 8). Iteration 14c added; 15 now depends on it.
- 2026-11-03: 14c built and checked on the phone, with no schema change: a buy's quantity ratio uses the existing split columns. Examples H and I hold on the phone (V sold out to exactly 0 after a 1 for 7; MA re-entered after a 1 for 3 gives exactly 1 share after a 3 for 1). Departure: three screens show a buy's exact shares, which section 4 of the 14c file did not expect (14c file, section 14). Open items 44 and 46 closed.
- 2026-11-03: the 14c Deploy review found one Medium and one Low finding, both fixed on the branch and rechecked on the phone (your decision A): the Transactions row and the reversal card's TalkBack text showed a buy's stored quantity instead of its exact shares (14c file, section 14, items 9 and 10). No test draws a screen: a CLAUDE.md pitfall was added and open item 47 opened (your decision A).
- 2026-11-03: pull request #116 (iteration 14c) was merged as ae4ec04. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for ae4ec04, 3m 7s) were reported successful by you. The branch was deleted locally; origin no longer has it, nor iter-14b or docs/plan-14b-merge (git branch -r after git fetch --prune, 2026-11-03). The 15 file was approved; it adds the first schema change since 09 (a hidden_holdings table, database version 6).
- 2026-11-03: 15 built and checked on the phone. The migration 5 to 6 ran on the phone's database with the data kept. Hide is offered only at exactly 0 shares counting every entry, and a buy, a sell's reversal or a trade pair that gives a hidden holding shares unhides it in the same database transaction (15 decisions 1 and 2). Correction: the last schema change before 15 was in 12, not 09 (15 file, section 14, item 2).
- 2026-11-03: the 15 Deploy review found one Low finding: two tests that step 4 named were missing (unhiding by a pair; a failed Unhide). Added on the branch after the code, both green on their first run, and shown to fail when the code they guard is broken (15 file, section 14, items 8 and 9; your decision A). No code changed.
- 2026-11-03: pull request #117 (iteration 15) was merged as d7630a8. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for d7630a8, 2m 56s) were reported successful by you. The branch was deleted locally; origin no longer has it (git branch -r after git fetch --prune). The status lines above were updated. The 15 file's sections 13 and 14 are left as written at build time.
- 2026-11-03: pull request #118 (docs: the screen tests discussion) was merged as 1e2806f, CI green on the pull request (3 checks) and on main (2m 58s) (your reports); the branch is gone from origin. It records the discussion of open item 47 in plan/discussions/screen-tests.md, with nothing decided; item 47 stays open. Open item 45 gained your report of a slower Cash tab and cold start (your decision A).
- 2026-11-03: pull request #119 (docs: the 15 merge record and open item 45) was merged as 65250c7, CI green on the pull request (3 checks) and on main (2m 28s) (your reports); the branch is gone from origin (git branch -r after git fetch --prune).
- 2026-11-03: open item 47 taken up in part as change request CR2 (intent/intent-cr-02-screen-models.md; your decisions): screen models with no new library, covering new and changed screens from 16 on and three screens where defects were found; open item 48 records the existing screens not covered. CR2 Design: no spec change (section 14 is technology-neutral and K9's boundary is unchanged); criterion 42 keeps its phone check (your A). Iteration 15a added; the 15a file was approved.
- 2026-11-03: 15a built and checked on the phone, with no schema change and no change to what is shown. Three screens now draw from tested models (Holding detail's Transactions rows, the combined review, the cash entry form's messages); the models are built in builders, not in AppViewModel (15a decision 1, after comparing maintainability, test run time and APK size; the three criteria now apply to every Build choice about code, CLAUDE.md section 8). The screen model convention is in section 3 and CLAUDE.md. Release APK +32,784 bytes.
- 2026-11-03: the 15a Deploy review found three Low findings (your decision A): the share-or-shares choice for a list of lots moved from the screen into the model, with a failing test first, and was rechecked on the phone; a test was added for a review whose part 1 has no effect yet; the cash rule's figures, still formatted by a helper shared with the reversal screen, were added to open item 48 (15a file, section 14, items 9 to 11).
- 2026-11-03: pull request #120 (iteration 15a, 11 commits) was merged as 2411066. CI on the pull request (3 checks: build, Analyze (actions), CodeQL) and on main (the run for 2411066, 2m 9s) were reported successful by you. The branch is gone from origin (git branch -r after git fetch --prune). The status lines above were updated. The 15a file's sections 13 and 14 are left as written at build time.
- 2026-11-04: pull request #121 (docs: the 15a merge record) was merged as 6c23750, CI green on the pull request (3 checks) and on main (3m 11s) (your reports); the branch is gone from origin (git branch -r after git fetch --prune).
- 2026-11-04: the rest of open item 47 decided (your F, Plan): a spike for level C only (instrumented tests on an emulator, plan/spikes/screen-tests.md, to be written and approved first), then change request CR3 from its results; level B only if level C cannot run on every change. Running on every change is tried first with a self-hosted runner on your PC, under the conditions in plan/discussions/screen-tests.md, section 8.

## 13. Out of scope for this plan
Performance over time, per-holding dividends, ETF look-through, the next intent.md items,
and the Deploy stage (review, install route, release signing), which follow Ruleset A rule 6. Deploy also carries the Licences screen (section 12).
