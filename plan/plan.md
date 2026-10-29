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
  spikes/price-source.md       decision note, to be written and finished before iteration 09
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
| 13 | reverse-transaction | Reverse a buy, sell or split | 11, 12 | 4.6, crit. 53 |
| 14 | reverse-and-reenter | Reverse and re-enter, trades and cash | 13, 05 | 4.6, crit. 21, 54, 60(b) |
| 15 | hide-holdings | Hide and unhide sold-out holdings | 13 | 4.4, crit. 27, 51, 60(a) |
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
A in 11, B in 12, C in 09 (it needs a buy), D and F in 13, G in 14, E in 29. Criteria 59
and 60 are tested in the iterations whose rules they cover (integer-cent arithmetic from 02a, exact fractions from 09 and 11,
60(a) in 15, 60(b) in 14, 60(c) in 17, 60(d) in 11).

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
| spikes/price-source.md | not written |
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
| Iterations 08 to 35 | not started, files not written |

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

## 13. Out of scope for this plan
Performance over time, per-holding dividends, ETF look-through, the next intent.md items,
and the Deploy stage (review, install route, release signing), which follow Ruleset A rule 6. Deploy also carries the Licences screen (section 12).
