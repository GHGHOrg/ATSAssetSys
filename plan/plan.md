# plan/plan.md

Stage: Build. Status: approved, together with plan/setup.md. Setup run 2026-10-18 (setup.md section 13). Iteration 01 is done. The dependency-upgrade spike has run and is merged (pull request 73, merge commit 7df2049). Iteration 02 is split into 02a and 02b (section 12); 02a has a base file, nothing is built. Iterations 03 onward have not started.
Source: intent/intent.md v52 (final), spec/spec.md v4 (final).
Only iteration 01 has been built, checked on the phone and merged (see its file). Where this file says "my reading", it is
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
| 02b | bottom-bar-and-cash-tab | Use the four-tab bottom bar and a Cash tab showing the balance | 02a | 6 (navigation outline, view 7), crit. 9, 39 to 42, 49 (part) |
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
  against the real database path and decides how to verify.
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
| Iteration 01 | done: built and checked on the phone 2026-10-19 (check 8 on 2026-10-20); pull request #70 merged as 8a5703e, CI green on main (your report); not-run items are listed in its file |
| Iteration 02a | base file plan/iterations/02a-opening-balance-stored.md approved 2026-10-23, decisions 1 to 3 settled; not started (wireframes 01 and 10b not yet read) |
| Iteration 02b | not started, file not written |
| Iterations 03 to 35 | not started, files not written |

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

## 13. Out of scope for this plan
Performance over time, per-holding dividends, ETF look-through, the next intent.md items,
and the Deploy stage (review, install route, release signing), which follow Ruleset A rule 6. Deploy also carries the Licences screen (section 12).
