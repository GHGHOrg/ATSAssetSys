# plan/iterations/15c-performance.md

Stage: Build. Status: approved 2026-11-07 (your A), amended the same day (the app-start timing test, your A). Decisions 1 to 5 settled 2026-11-07 (section 11). In build: step 1 merged (#137), steps 2 to 7 done, step 8's documents written and the phone check done, all yes (section 13); the pull request is next.
Source: plan.md section 4 (row 15c), open item 45, spec 4.1, 4.4 to 4.6, 4.8, 4.10 and section 6 (views 6 and 7, Error, empty and loading states (3)), C19. "My reading" is an interpretation, not a test.

## 1. Outcome
Saves, the lot picker, Holding detail and the detail screens stay quick as records grow, and every figure, refusal and message stays exactly the same.

## 2. Spec refs
- 4.1 (the replay order), 4.4, 4.5, 4.6, 4.8 and 4.10. No new rule and no new criterion.
- The examples, AC-59 and AC-60 are covered by the existing tests, which must pass unchanged.
- Section 6, state (3): "the app never waits": every figure from stored data shows at once.
- Section 9: the timing test uses test data and the throwaway test PIN only.
- C19: the emulator is not the phone.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The Cash tab rework: its own iteration, 15d (decision 4).
- Moving work off the main thread (decision 5).
- Prices (16a).
- Any wording, database or schema change.
- The other screens of open item 48.

## 4. Analysis (2026-11-06, by reading; step 2 measures it)
Notation:
- n: all transactions.
- S: the sells and reversals among them.
- B: all buys.
- h and b: one holding's transactions and its buys.
- A: the sell allocations.
- H: the hidden holdings.
- m: the cash entries.

### 4.1 core/ (the calculation layer)
- `splitFactor` scans all n transactions: O(n). `quantityAt` calls it for the buy and for each of the S sells and reversals of every holding: O(S·n) ≈ O(n²).
- `lots` and `holdings`: B × O(n²) = O(n³). `lotsOf` works out every lot in the app and then filters: O(n³).
- `firstNegative` (each split, reversal and re-entry save): O(h·b·n²), up to O(n⁴).
- `Sells.lotChoices`: O(b·h·n²), up to O(n⁴). `Sells.sell` calls it on Review and on save.
- `Hiding.offered` and `Hiding.unhiddenBy`: one `holdings(LocalDate.MAX)` call per hidden holding, O(H·n³).
- Smaller costs:
  - `Sells.pieces`: O(A·n).
  - `Splits.effect` and the split part of `Reversals.effect`: O(b·n²).
  - `originalOf` and `reversalOf`: O(n) each.

### 4.2 The data layer (RoomRecordStore.kt)
- `loadHidden` reads every transaction a second time at start.
- `checkHidden` adds a third O(n³) holdings call.
- The rest is lean: plain `SELECT … ORDER BY` loads, linear checks, and saves that update the state in memory with no reload.

### 4.3 The app's screen state (AppViewModel.kt)
- No `Dispatchers` at all: everything runs on the main thread. `valued()` makes two O(n³) calls on every load and save.
- Computed properties of AppState run again on every read, and every keystroke makes a new AppState:
  - Sell: `sellLotChoices` runs on each keystroke, and inside `toggleLot`, `onSellDateChange` and `sellAll`.
  - Holding detail: `hideOffered` once, plus `holdingLots` read twice.
  - Transaction detail: `transactionLot`.
  - Reversal and Reverse and re-enter: the effect and the pieces.

### 4.4 The screens
- The Cash tab (CashScreen.kt, lines 72 to 91):
  - three sorts per redraw
  - a search of every transaction per row, twice
  - every row drawn at once in a non-lazy `Column`

  My reading is that this, not the core, is what makes the Cash tab feel slow. It is left to 15d (decision 4); AppStartTimingTest's Cash figure shows how much remains after 15c.
- No problem found: the cash replay, Fraction's greatest-common-divisor per operation, the portfolio rules, the database loads, and the holdings lists on Portfolios and a portfolio's screen.

### 4.5 Example sizes
| Size | Call | Steps now |
|---|---|---|
| ~300 transactions (10 holdings × 30) | one `holdings` call | ≈ 5.4 million |
| ~300 transactions | `valued()` (two `holdings` calls) | ≈ 11 million |
| ~300 transactions | the sell lot list of one holding | ≈ 16 million per keystroke |
| 1,000 transactions | one `holdings` call | ≈ 200 million |

After the fix, each of these is a few thousand steps or fewer.

### 4.6 Target classes after the rewrite
| Function | Now | After |
|---|---|---|
| `holdings`, `lots`, `netWorth` | O(n³) | O(n log n + A) |
| `lotsOf`, `pieces` | O(n³), O(A·n) | O(h log h + A) |
| `firstNegative` | O(h·b·n²) | O(h + A) |
| `lotChoices`, `Splits.effect`, `Reversals.effect` | O(b·h·n²), O(b·n²) | O(b·h) |
| `Hiding.offered`, `unhiddenBy` | O(H·n³) | O(H·h) |

## 5. Files
- core/, Holdings.kt:
  - a per-holding ledger, built once for a transaction list: the transactions grouped by holding in the 4.1 order, an id map, the running split factor at each place, and each lot's quantity through one replay
  - `splitFactor`, `quantityAt`, `firstNegative`, `lots`, `holdings` and `lotsOf` use the ledger
  - the public functions keep their signatures; the internal ones may change, and no test calls them (checked 2026-11-06)
- core/, the callers: Sells.kt (`lotChoices`, `pieces`), Splits.kt (`effect`), Reversals.kt (`effect`, `sharesRefusal`), Reentries.kt, Hiding.kt (`offered` and `unhiddenBy` read one holding's shares) and Valuation.kt.
- The data layer:
  - RoomRecordStore.kt: `loadHidden` takes the transactions already read, and `checkHidden` makes one pass.
  - RecordStore.kt: `loadHidden`'s signature changes to match.
- The screen state, AppViewModel.kt:
  - the derived values become fields of AppState, set when their inputs change: `holdings`, `netWorth`, the shown holding's lots, Hide offered, the sell lot list and the transaction's lot
  - the actions read these fields
  - no composable changes
- Build:
  - core/build.gradle.kts adds the `java-test-fixtures` plugin, a Gradle built-in, not a new library
  - app/build.gradle.kts adds `androidTestImplementation(testFixtures(project(":core")))`
  - the exact form is checked on the day, and a departure is recorded in section 14
- Tests (section 9):
  - core/src/testFixtures/…/ScaleHistories.kt (the fixed recipe, shared by both timing tests)
  - core/src/test/…/HoldingsReference.kt (decision 1)
  - core/src/test/…/HistoryGenerator.kt (random histories for the comparison)
  - core/src/test/…/HoldingsComparisonTest.kt
  - core/src/test/…/HoldingsScaleTest.kt
  - app/src/androidTest/…/AppStartTimingTest.kt
  - in app/src/test, the kept-field tests and the "same object" tests
- Documents:
  - plan.md: the 15c and 15d rows, and the 16 row split into 16a, 16b and 16c
  - plan.md section 11 and the change log
  - open-items.md: items 40, 45 and 47
  - this file

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Steps 2 to 8 run on branch iter-15c-performance from main, once this file is merged.
1. This file and the plan.md and open-items.md lines, on docs/iter-15c-plan, merged first.
2. The test tools and the "before" figures, on today's code:
   - ScaleHistories (test fixtures), HistoryGenerator and HoldingsReference
   - HoldingsComparisonTest: reference against today's code, which must be equal from the start; this proves the comparison itself
   - HoldingsScaleTest's "before" run, once, with `ATS_SCALE_OLD=1`: the table at 100, 300 and 1,000 transactions, capped by estimate at 60 s
   - the new-code part is not asserted yet, since today's code would fail its bounds; it is switched on in step 7
   - AppStartTimingTest: you push once, and its run on the pull request gives the "before" app-start figures, from logcat in the artifact (gh, read only)
3. The core ledger in Holdings.kt: the existing tests unchanged and green, and the comparison test equal.
4. The core callers, with the same proof.
5. The data layer: `loadHidden` and `checkHidden`.
6. The screen state: the kept fields, each with its kept-field test and its "same object" test.
7. The "after" figures:
   - HoldingsScaleTest's new-code part switched on: its table at 100, 300, 1,000 and 3,000 transactions, the ratio bounds asserted, and the time limit set from the measured figures (decision 2)
   - the unit suite's run time, before and after
   - AppStartTimingTest's run on the pull request gives the "after" figures
8. Documents: results, the phone check, open item 45 and the pull request draft.

## 7. Stubs
None.

## 8. Risks
- The replay is rewritten, and every save rule uses it. A wrong result could let a bad save through or refuse a good one. The guards are section 9, part 1, layers 1 and 2.
- Kept fields can go stale. A field not refreshed when one of its inputs changes would show an old figure. The guards: each field is set in one place, from one function, plus section 9, part 1, layer 3.
- The comparison is only as good as the generator. A case the generator never makes is compared only through the existing tests. The guard: the mix is listed in section 9, and its counts per kind are printed with each run.
- Timing noise. CI machines are shared, and their timing varies more than this PC's. The ratio bounds have wide margins for that reason. A failing ratio is re-run once before it is treated as real, and that re-run needs your yes, as with screen tests. Absolute times on one machine are a guide, not a fact.
- One call stays O(n²). The sell lot list on one big holding stays O(n²) after the rewrite: each lot's "least it holds from the sell date on" needs a look at every later place. That is far below today's O(n⁴), and section 9 bounds it separately.
- AppStartTimingTest is not a true cold start. The test process cannot kill the app's process without killing itself, so each run is a new screen in an already running process, with Java's compiler warm. It does include what 15c changes: the database read, `loadHidden`, `valued()` and building Overall and Cash.
- The unlock is left out of the timing. The PIN entry through the system's PIN pad takes several seconds and varies, so timing starts when the unlock is accepted.
- The emulator is not the phone (C19). AppStartTimingTest's figures compare before and after on one kind of machine; they do not say how fast your phone is. The phone check by feel stays.
- AppStartTimingTest's data could leak into other tests. It writes about 300 transactions into the emulator's app database. It clears the database before and after, so the other screen tests see the same app as now. If clearing fails, the test fails, and the cause is in the artifact.
- The screen-tests run gets longer: about 30 to 60 s after 15c; the "before" run longer, perhaps minutes.
- The Cash tab keeps its own cause until 15d.
- The main thread stays (decision 5): with the fix, a save costs about n log n, which my reading says is not noticeable.
- The unit test run gets longer: the comparison runs today's slow code on small histories, and the scale test's new-code part aims at about 5 s at most. Step 7 reports the real figures.

## 9. Tests and proof

### Part 1: Correctness, "every figure stays exactly the same"
1. The existing 649 tests, unchanged.
   - They cover the worked examples A to I, splits and reverse splits, reversals, tiny remainders (CR1), Hide, and the ViewModel flows (120 tests).
   - At the end, `git diff main -- core/src/test app/src/test app/src/androidTest` shows only new files and added lines, with no existing line changed or deleted.
2. Old against new on generated histories (HoldingsComparisonTest; decision 1).
   - The reference: HoldingsReference.kt keeps today's versions of every function the rewrite touches: `lots`, `holdings`, `lotsOf`, `firstNegative`, `lotChoices`, `pieces`, the two `effect` functions, `Hiding.offered` and `unhiddenBy`.
   - The histories: HistoryGenerator, with a fixed seed so every run is the same and a failure can be replayed. It builds a few hundred histories of 20 to 60 transactions through the real K1 functions (buy, sell with picks, split, reversal), so each is a history you could have entered. The mix:
     - several portfolios and tickers
     - splits and reverse splits (for example 1 for 3 and 3 for 7)
     - reversals of buys, sells and splits
     - quantities whose remainders round up as well as down (2/3, 5/3, 3/7, 1/7)
     - several lots per holding
   - Histories that should be refused: some are broken on purpose, such as a split that leaves a later sell short, or a buy reversed before its sell is reversed, so the refusals get compared too.
   - The check: for every history, at every date in it, old and new must be exactly equal (`Fraction` equality, never approximate). That covers each lot's remaining quantity and cost, each holding's shares and cost, `firstNegative`'s place, lot and amount, every lot choice with its reason, every piece, every effect, and Hide offered and unhidden-by.
3. Kept fields follow their inputs (app/src/test). For each kept field, a test changes every input in turn and checks the field equals a fresh calculation afterwards:
   - each kind of save
   - a new date typed
   - another portfolio or ticker
   - hide and unhide
   - a new day
4. The screens: the screen tests, 6 of 6, then 7 of 7 with AppStartTimingTest. On the phone, after installing over the previous build (AC-35), Overall, a portfolio, Holding detail (header, Transactions, Lots), the Sell lot picker with one lot picked, and a transaction detail read the same as the screenshots you take before the install.

### Part 2: Performance, "it's faster"
1. HoldingsScaleTest: what it times. Only core/'s calculation functions, called the way the app calls them. No screen and no database.

   | # | Call | Like the app's… |
   |---|---|---|
   | 1 | `Holdings.holdings(transactions, lastDate)` | `valued()` on every load and save |
   | 2 | `Valuation.netWorth(entries, transactions, lastDate)` | `valued()`, its second O(n³) call |
   | 3 | `Holdings.lots(transactions, lastDate)` | the transaction detail's lot |
   | 4 | `Holdings.lotsOf(…, the largest holding, lastDate)` | Holding detail's Lots tab |
   | 5 | `Holdings.firstNegative(…, the largest holding)` | each split, reversal and re-entry save |
   | 6 | `Sells.lotChoices(…, the largest holding, lastDate, next seq)` | the sell lot list |
   | 7 | `Hiding.offered` (one holding) and `Hiding.unhiddenBy` (5 hidden holdings) | Holding detail's Hide and a buy's save |
   | 8 | `Splits.effect` and `Reversals.effect` on the largest holding's last split | the split and reversal reviews |
   | 9 | `Sells.pieces` on the last sell | the sell review and the transaction detail |

2. HoldingsScaleTest: the steps.
   1. Build the histories (not timed). ScaleHistories uses a fixed recipe, not random, and does not go through K1's save functions (on today's code, building 1,000 transactions that way would itself take O(n⁴)).
      - Valid by construction: the recipe keeps its own count of each lot's remaining shares, so a sell never takes more than a lot holds.
      - The cycle of 10, per holding:
        - 5 buys, with quantities like 3, 2.5 and 0.33333333
        - 3 sells, each taking part of the oldest one or two lots
        - 1 split, alternating 3 for 2 and 1 for 3
        - 1 reversal, in turn of a sell, a split, or a buy with no sells from it
      - One opening balance large enough for every buy.
      - Two shapes at each size n:
        - Spread: n / 30 holdings of about 30 transactions each, the usual case.
        - One big holding: all n transactions in one holding, the worst case for calls 4, 5, 6, 8 and 9.
   2. Warm up (not timed). Each call runs 3 times first, so Java's just-in-time compiler has done its work.
   3. Time each call.
      - Each timed block repeats the call until at least 50 ms have passed, then divides by the number of calls.
      - The block runs 5 times, and the median is kept.
      - Every result is kept in a variable the test reads, so no call is skipped as unused.
   4. Sizes: 100, 300 and 1,000 transactions, plus 3,000 for the new code only.
   5. Write the table: for each call, shape and size, the median time and the growth ratio to the next size. It goes to the test's standard output (in the result XML) and to `core/build/scale/holdings-scale.txt`, and from there into section 13.
   6. Assert (new code only, every normal run, CI included). The growth ratio (the time at 3,000 ÷ the time at 1,000) stays under its bound:

      | Call | Target after the rewrite | Ratio for 3× the transactions | Bound |
      |---|---|---|---|
      | 1, 2, 3, 7 (both shapes) | O(n log n) | ≈ 3.5 | under 8 |
      | 4, 5, 9 (spread) | O(h + A), not growing with n | ≈ 1 | under 8 |
      | 4, 5, 9 (one big holding) | O(n) | ≈ 3 | under 8 |
      | 6, 8 (one big holding) | O(b·h) = O(n²) | ≈ 9 | under 20 |
      | today's code, for comparison | O(n³) / O(n⁴) | ≈ 27 / ≈ 81 | would fail |

      Under decision 2, each call at 3,000 also stays under a fixed time, about 10 times what step 7 measures, written into the test in step 7.
3. The "before" run (step 2, once).
   - It is switched on by the environment variable `ATS_SCALE_OLD=1`, which Gradle passes to the test; without it, this part is skipped.
   - It times today's code (HoldingsReference) at 100, 300 and 1,000 transactions.
   - The cap: before each size, the time is estimated from the size before times the expected growth. If the estimate is over 60 s, the size is skipped and recorded as "skipped, estimated ≈ X s".
   - The table goes into section 13, next to the "after" table.
4. App start on the emulator (AppStartTimingTest, app/src/androidTest; decision 3). It runs on GitHub's emulator in the screen-tests workflow, never on the phone.
   1. Clear the app's database and key files.
   2. Seed about 300 transactions with their cash lines (ScaleHistories' spread shape) through the app's RoomRecordStore. Test data only (spec section 9).
   3. Open the app: launch MainActivity and unlock it with the test PIN (ScreenTestSupport's `unlockApp`).
   4. Time "load and Overall": from the unlock being accepted until Overall shows its net worth.
   5. Tap Cash right away: time from the tap until the Cash list shows its first row.
   6. Repeat steps 3 to 5 three times, closing the screen between runs, and keep the median of each.
   7. Log the two medians and the three runs under the tag ScreenTestTiming (times only: no amounts, tickers or names). The workflow already keeps each test's logcat in its artifact.
   8. Clear the database again.

   It asserts only that each screen appears within 120 s; a slower screen is recorded as "over 120 s". The "before" figures come from step 2's pull request run and the "after" ones from step 7's. Both go into section 13.
5. "Computed once, not per keystroke" (app/src/test):
   - typing in Sell's quantity, total and a pick leaves the sell lot list in the state as the same object (`assertSame`)
   - a change of date, portfolio or ticker gives a new one
   - the same is tested for Holding detail's lots and Hide offered
6. On the phone, by feel (decision 3): opening the app, the first Overall, the Cash tab and typing in Sell feel the same or quicker than before the install.
7. Commands:
   - spotlessCheck, build, the unit tests, lint and `:app:compileDebugAndroidTestKotlin`, all with --rerun-tasks
   - the screen tests on the pull request, with their count
   - the unit suite's run time before (1.2 s on 2026-11-06) and after
   - the standing checks AC-09 and AC-39 to AC-42
   - no benchmarking library (JMH) is added: the timing is hand-written in JUnit

## 10. Research
None, since no library is added. The `java-test-fixtures` form for an Android module's androidTest is checked on the day (section 5, Build).

## 11. Decisions
1. Settled 2026-11-07 (your A): the reference is today's code kept as a test-only copy (HoldingsReference.kt), compared on generated histories. Not chosen: (B) the existing tests only; (C) a saved snapshot.
2. Settled 2026-11-07 (your A): the time limit: each call at 3,000 transactions also stays under about 10 times the "after" time, as well as its ratio bound. Not chosen: (B) the ratio bounds only.
3. Settled 2026-11-07 (your A, the amendment): the app start is timed by AppStartTimingTest on GitHub's emulator, before and after, plus a check by feel on the phone. Not chosen: hand-timing with a stopwatch on the phone.
4. Settled 2026-11-07 (your A): the Cash tab rework (a screen model, one sort, a lazy list; CR2 and AC-61 apply) is its own iteration, 15d, after 15c's measurements. Not chosen: (B) inside 15c.
5. Settled 2026-11-07 (your A): the main thread stays; with the rewrite, a load or save costs about n log n. Not chosen: (B) valuation on a background thread.

## 12. Wording to approve (D9)
None, since no text the app shows changes. The ScreenTestTiming log lines are for the test artifact only.

## 13. Status and notes
- Draft 2026-11-06, revised 2026-11-07 (the analysis; the tests and proof; the HoldingsScaleTest design), approved 2026-11-07 (your A), amended 2026-11-07 (AppStartTimingTest; your A).
- 16 is split into 16a, 16b and 16c (your A), and 15c runs before 16a (your A); 15d follows 15c (decision 4).
- The before and after tables are added in steps 2 and 7.
- Step 1 merged in pull request #137 (02b24d5, 2026-11-07). Steps 2 to 8 on iter-15c-performance.

### 13.1 Step 2: the code before 15c, timed (2026-11-07, this PC, JVM)
HoldingsScaleTest with ATS_SCALE_OLD=1, on HoldingsReference (main at 02b24d5). Median ms per call; the growth is over the size before.

| Call | Spread 100 | Spread 300 | Spread 1,000 | One holding 100 | One holding 300 | One holding 1,000 |
|---|---|---|---|---|---|---|
| 1 holdings | 3.7 | 54.7 (×15.0) | 1,822 (×33.3) | 8.0 | 280.6 (×35.1) | 26,886 (×95.8), timed once |
| 2 netWorth | 3.4 | 53.8 (×15.8) | 1,824 (×33.9) | 8.0 | 283.3 (×35.4) | 26,813 (×94.6), timed once |
| 3 lots | 3.3 | 53.3 (×16.1) | 1,823 (×34.2) | 8.0 | 281.2 (×35.4) | 26,851 (×95.5), timed once |
| 4 lotsOf | 3.4 | 54.2 (×16.1) | 1,820 (×33.6) | 8.0 | 279.4 (×35.1) | 26,875 (×96.2), timed once |
| 5 firstNegative | 13.8 | 50.4 (×3.6) | 531.3 (×10.5) | 223.9 | skipped, estimated about 18 s per call | skipped |
| 6 lotChoices | 1.3 | 5.7 (×4.3) | 61.7 (×10.9) | 8.0 | 282.6 (×35.2) | skipped, estimated about 35 s per call |
| 7 Hide (one holding, 5 hidden) | 14.2 | 335.0 (×23.6) | 11,340 (×33.8) | 15.9 | 560.4 (×35.3) | skipped, estimated about 21 s per call |
| 8 effects | 1.7 | 8.6 (×4.9) | 87.7 (×10.2) | 14.0 | 538.0 (×38.3) | skipped, estimated about 20 s per call |
| 9 pieces | 0.010 | 0.014 (×1.5) | 0.041 (×2.9) | 0.013 | 0.042 (×3.3) | 0.342 (×8.2) |

Reading: from 300 to 1,000 transactions (3.3 times as many), calls 1 to 4 and 7 grow about 34 times on the spread shape (cubic) and about 96 times on one holding; section 4's analysis holds. One load or save of the app calls 1 and 2: about 110 ms together at 300 spread transactions on this PC, before the phone's slower processor, and loadHidden adds a third call at start. "Timed once": three runs would pass the 60 s cap, so the warm-up's time stands alone.

Repeat run (2026-11-07, same code, data and seed; your request): every timed row within −7.5% to +0.5% of the first, 40 of 47 within 4%, the same 7 rows skipped; the growth ratios agree to within 4%. The first run's figures stand above.

App start before 15c (AppStartTimingTest on GitHub's emulator, pull request #138, screen-tests run 42267711272 on 64416aa, 2026-11-06 UTC; "Tests run: 9 of 9", 4m 33s; 300 transactions, spread):

| Run | Unlock to Overall's net worth | Tap on Cash to its rows | The test's own time |
|---|---|---|---|
| 1 | 6,202 ms | 2,975 ms | 19.3 s |
| 2 | 5,379 ms | 1,385 ms | 12.3 s |
| 3 | 2,431 ms | 627 ms | 6.9 s |
| median | 5,379 ms | 1,385 ms | |

Reading: each run is faster than the one before, not scattered. My reading: the warm-up of Android's runtime, which compiles the app's code as it runs and keeps its caches between runs in the same process. The median is therefore mostly run 2, so step 7 compares each run with its counterpart (run 1 is the nearest to a first open) as well as the medians. The medians are worked out from the three run lines: the test's own median line is written after the class, outside the logcat Gradle keeps (section 14, item 5).

### 13.2 Step 2: the tests added
- HoldingsComparisonTest: 200 histories, 6,985 transactions (3,577 buys; 1,359 sells, 967 with a partial pick; 331 splits and 321 reverse splits; 959 reversals of buys, 280 of sells, 158 of splits; 330 refused reversals and 3 refused splits as candidates). Equal throughout: today both sides are the same code, which proves the comparison. It takes 13.9 s, because both sides run the code before 15c.
- HoldingsScaleTest: the recipe check (both shapes valid at 100 transactions) runs every time, in 0.4 s; the "before" timing runs only with ATS_SCALE_OLD=1.
- AppStartTimingTest: compiled; it runs on the pull request's screen-tests run, not on this PC (section 3).
- Unit tests: 652 (649 unchanged, plus 3), 0 failures, 1 skipped (the "before" timing without its switch); the suite's time 22 s, from 1.2 s, almost all of it the comparison on the code before 15c.

### 13.3 Steps 3 to 6: the rewrite (2026-11-07)
Each step passed spotlessCheck, the build, the unit tests (0 failures, 1 skipped), lint (the 5 listed findings, report time checked) and the androidTest compile, all with --rerun-tasks. HoldingsComparisonTest was equal on all 200 histories at every step. No existing line of the test folders changed (`git diff main` on them removes no line).
- Step 3 (2c119ac), the core ledger: Holdings.kt gains the internal class Ledger, built once per transaction list. Its indexes: an id map that keeps the first transaction with each id; each holding's transactions; each holding's splits with a running product, looked up by binary search; and what touches each lot, from every holding. `quantityAt`, `splitFactor`, `firstNegative`, `lots`, `holdings` and `lotsOf` use it, with unchanged signatures. `firstNegative` checks each lot only at the first place of its holding after each of the lot's own entries. A split factor not above 0 falls back to the old checks. 652 tests; the comparison 9.3 s, the suite 14.9 s.
- Step 4 (8a9376e), the callers share one ledger: `Sells.lotChoices` (through `Ledger.leastHeld`), `Sells.pieces`, `Splits.effect` and `Reversals.effect` build one ledger each; `Reversals.sharesRefusal` finds each sell's reversal in one pass; `Hiding.offered` and `unhiddenBy` read a holding's shares from its own lots. Section 14, item 6. 652 tests; the suite 10.9 s.
- Step 5 (fa3b4b6), the data layer reads the transactions once: RecordStore gains `loadHidden(transactions)`, by default `loadHidden()`, so the test stores are unchanged; RoomRecordStore overrides it, and `checkHidden` uses the new `Hiding.sharesAfterEveryEntry(transactions, holdings)`, one ledger. AppViewModel's start loads in the same order and passes the transactions it read. Not reached by any unit test: RoomRecordStore's `loadHidden(transactions)` on the real database. AppStartTimingTest runs it with no hidden holding (green on #138, 13.4); the phone check runs it with your hidden holdings. 652 tests; the suite 13.6 s.
- Step 6 (ac8a0a2), the kept fields: `holdings`, `netWorth`, `holdingLots`, `hideOffered`, `sellTickers`, `sellLotChoices` and `transactionLot` are fields of AppState, each worked out by one function in KeptFields.kt. Every write to the screen state goes through KeptStateFlow, which calls `refreshed()`: a field is worked out again only when one of its own inputs changed, and otherwise stays the same object; a second call changes nothing. A state built directly (the screen tests) gets the same values from the fields' defaults. `valued()` sets today only, and net worth comes from the holdings already worked out (`Valuation.netWorthOf`), one ledger instead of two. `onSellDateChange` refreshes before it reads the new date's lot list. No composable changed. New tests: KeptFieldsTest (6: each of the 12 inputs changed in turn moves its field and gives a fresh calculation; typing leaves every field the same object) and KeptFieldsFlowTest (12: each kind of save, a new sell date, ticker and portfolio, Hide and Unhide, a new day, typing). Two deliberate faults (Hide offered not following the hidden list; the date change not refreshing) each failed the new tests and an existing one, and were taken out. 670 tests; the suite 9.9 s. Section 14, item 7.

### 13.4 Step 7: the code after 15c, timed (2026-11-07)
HoldingsScaleTest's new-code part (d844201), this PC, JVM. Median ms per call; the growth is from 1,000 to 3,000 transactions.

| Call | Spread 100 | Spread 300 | Spread 1,000 | Spread 3,000 | One holding 100 | One holding 300 | One holding 1,000 | One holding 3,000 |
|---|---|---|---|---|---|---|---|---|
| 1 holdings | 0.259 | 0.564 | 2.012 | 6.207 (×3.1) | 0.188 | 0.601 | 2.210 | 7.117 (×3.2) |
| 2 netWorth | 0.179 | 0.538 | 1.929 | 5.853 (×3.0) | 0.188 | 0.608 | 2.200 | 7.261 (×3.3) |
| 3 lots | 0.157 | 0.478 | 1.692 | 5.090 (×3.0) | 0.172 | 0.551 | 1.973 | 6.348 (×3.2) |
| 4 lotsOf | 0.068 | 0.096 | 0.238 | 0.632 (×2.7) | 0.172 | 0.557 | 1.987 | 6.447 (×3.2) |
| 5 firstNegative | 0.075 | 0.103 | 0.240 | 0.641 (×2.7) | 0.201 | 0.637 | 2.285 | 7.035 (×3.1) |
| 6 lotChoices | 0.071 | 0.107 | 0.247 | 0.670 (×2.7) | 0.174 | 0.582 | 1.990 | 6.267 (×3.1) |
| 7 Hide (one holding, 5 hidden) | 0.247 | 0.378 | 0.686 | 1.480 (×2.2) | 0.359 | 1.149 | 4.130 | 13.177 (×3.2) |
| 8 effects | 0.095 | 0.169 | 0.478 | 1.306 (×2.7) | 0.214 | 0.710 | 2.636 | 8.334 (×3.2) |
| 9 pieces | 0.011 | 0.016 | 0.048 | 0.155 (×3.2) | 0.012 | 0.031 | 0.101 | 0.337 (×3.3) |

Reading: every growth from 1,000 to 3,000 lies between 2.2 and 3.3, under the bounds of 8 and 20; calls 6 and 8 on one holding grow like the others, below the O(n²) section 8 expected. Against 13.1 at 1,000 transactions: `holdings` 1,822 ms to 2.0 ms on the spread shape and 26,886 ms to 2.2 ms on one holding; Hide 11,340 ms to 0.69 ms. The test now asserts the growth bounds and, for each call and shape, a fixed time at 3,000 of about 10 times these figures, rounded up (decision 2; HoldingsScaleTest's LIMITS_AT_3000).

Suite times (summed from the per-class result files): 1.2 s before 15c (2026-11-06), 22 s after step 2, 32.1 s now, of which HoldingsComparisonTest 11.2 s (its reference side still runs the code before 15c) and HoldingsScaleTest 19.4 s (section 14, item 8). 671 tests, 0 failures, 1 skipped.

App start after 15c (AppStartTimingTest on GitHub's emulator, 300 transactions, spread), two runs on pull request #138, both "Tests run: 9 of 9":
- run A: screen-tests run 42329781018 on d844201 (5m 41s)
- run B: screen-tests run 42331382465 on 49e0a68 (5m 39s), with the CPU lines (section 14, item 10)

| Run | Unlock to Overall: before / A / B | Tap on Cash to its rows: before / A / B | The test's own time: before / A / B |
|---|---|---|---|
| 1 | 6,202 / 3,078 / 4,820 ms | 2,975 / 6,046 / 6,751 ms | 19.3 / 21.5 / 24.3 s |
| 2 | 5,379 / 3,341 / 2,823 ms | 1,385 / 3,353 / 4,077 ms | 12.3 / 14.6 / 13.9 s |
| 3 | 2,431 / 1,598 / 2,028 ms | 627 / 1,302 / 2,384 ms | 6.9 / 8.4 / 10.9 s |
| median | 5,379 / 3,078 / 2,823 ms | 1,385 / 3,353 / 4,077 ms | |

The six other screen tests, which time nothing, took 32.4 s together before, and 41.6 s (A) and 41.7 s (B): both runners were about 29% slower than the "before" one.

Run B, CPU time in each phase (ms):

| Run | Phase | Wall time | App process CPU | Main thread CPU | Garbage collection |
|---|---|---|---|---|---|
| 1 | Load and Overall | 4,820 | 2,804 | 900 | 326 |
| 1 | Cash | 6,751 | 5,169 | 4,500 | 727 |
| 2 | Load and Overall | 2,823 | 2,155 | 480 | 0 |
| 2 | Cash | 4,077 | 3,111 | 2,770 | 519 |
| 3 | Load and Overall | 2,028 | 2,086 | 380 | 136 |
| 3 | Cash | 2,384 | 2,396 | 2,190 | 100 |

Reading:
- Load and Overall is quicker in every run, on slower runners: run 1 by 22 to 50%, the median by 43 to 48%. Its main thread now uses 380 to 900 ms; the rest of the process's CPU is the database read and decryption on other threads.
- Cash is about twice as slow in every run. It is the app's own work on the main thread (67 to 92% of its wall time): the Cash screen's drawing, which 15c did not change and which reads no kept field. Its per-row search of every transaction (CashScreen.kt, lines 91 to 94) is section 4.4's cause, left to 15d (decision 4).
- In run A, unlock to Cash rows stayed about the same as before (9,124 against 9,177 ms in run 1): the time saved before Overall was spent after the tap.
- My reading, not proven: Android's runtime compiles code as it runs; the slow valuation before 15c ran the same list searches millions of times during the load, so they were compiled before Cash was tapped, and now the Cash screen runs them first. Proving it would need a profiler trace or the CPU lines on the code before 15c; not done (your A, 2026-11-07), since 15d removes the per-row search either way.
- On the phone: Overall should come sooner; Cash may feel the same or slower until 15d. The phone check decides.

### 13.5 Step 8: the phone check (2026-11-07, your results)
The debug APK built at 10:46 from the app code of ac8a0a2 (nothing under app/src/main changed after it), installed with `adb install -r` over the build already on the phone: "Success". No schema change. Before the install, after a force stop, you took screenshots of Overall, a portfolio's screen, Holding detail (header and Transactions, then Lots), the Sell lot picker with one lot picked, a buy's transaction detail and Hidden holdings, and noted how Overall, Cash and typing in Sell felt.
- 10. The app opened after the install with your data and the same net worth (AC-35): yes.
- 11. Overall after a second force stop, by feel: the same as before.
- 12. Cash, by feel: a tiny bit slower than before, as section 13.4 leads to expect; left to 15d.
- 13. A portfolio, Holding detail (header, Transactions, Lots), the Sell lot picker and the transaction detail read the same as the screenshots; typing in Sell felt the same: yes.
- 14. Hidden holdings lists each hidden holding (step 5's loadHidden on your real data): yes.
- 15 to 18. AC-39 (the largest font), AC-40 (dark), AC-09 (the date note on Sell) and AC-42 (network off): yes.

My reading of 11: with your data, the calculation was a small part of the start; most of it is the database read and decryption (13.4, the CPU table).

## 14. Departures from the plan
1. ScaleHistories (step 2): the splits alternate 3 for 2 and 2 for 3, not 3 for 2 and 1 for 3, and each sell takes all of the oldest lot with shares and half of the next, not part of the oldest one or two. With the plan's recipe a 1,000-transaction holding carries fractions of hundreds of digits (2 to the power 150 and beyond), so the timing would measure long-number arithmetic, not the calculations. The comparison's random histories keep 1 for 3, 3 for 7 and partial picks.
2. AppStartTimingTest (step 2): three test methods (run1 to run3) sharing one seeded database, not one test that opens MainActivity three times: the v2 compose test API in use has no empty rule to launch the activity by hand. The screen-tests count becomes 9 with it, not 7. ScreenTestSupport.kt gains unlockAppTimed() as added lines only: unlockApp() returns only after the data has loaded.
3. HoldingsScaleTest's cap (step 2): it counts a call's whole measurement (warm-up and runs) against 60 s, not one call, and a call that would pass it with three runs is timed once. The first "before" run, with the cap on one call only, was stopped after about 13 minutes (your A) and run again; it then took about 5 minutes. A progress line per call and size goes to core/build/scale/progress.txt.
4. HoldingsComparisonTest's time (step 2): 13.9 s with both sides on the code before 15c, against the 1 to 3 s estimated in section 8. Step 7 measures it with the new code on one side.
5. AppStartTimingTest's median line (step 2): it is logged after the class, and Gradle keeps logcat only while each test runs, so the line is in no saved file; the step 2 medians are worked out from the three run lines. Step 7 logs the medians at the end of run 3 instead (a change to this iteration's own new test). Section 9's comparison is also read run by run, since the runs warm up (section 13.1).
6. The core callers (step 4): Reentries (startingPicks, startingQuantity, keptRatio) and Valuation.netWorth are unchanged, against section 5's list. Each makes a single call to Holdings, which since step 3 builds one ledger per call, so a shared ledger would save nothing. Sells.lotChoices goes further than section 4.6: the least a lot holds from the sell on is decided at the sell's place and at the first place of the holding from each of the lot's later entries on, not at every later place (Ledger.leastHeld), so it is O(b × the lot's entries), not O(b·h).
7. The screen state (step 6): one refresh on every write (KeptStateFlow and `refreshed()`), not a call per action, since about 40 actions change an input; each field records the inputs it was worked out from (AppState.keptFrom). `sellTickers` is kept too, beyond section 5's list. `Valuation.netWorth` is no longer unchanged (against item 6): it calls the new `Valuation.netWorthOf(entries, holdings, today)`, with the same result, so `valued()` builds one ledger. PREVIEW_ID became internal, for KeptFields.kt.
8. HoldingsScaleTest's time (step 7): the new-code part takes about 19 s on every run, CI included, not the "about 5 s at most" of section 8. The method of section 9 (five blocks of at least 50 ms for each of 72 calls, shapes and sizes) cannot go below about 18 s. Kept as planned (your A, 2026-11-07).
9. AppStartTimingTest's medians (step 7): logged by whichever run finishes third, not by run3 itself, since JUnit 4 does not promise the methods' order.
10. AppStartTimingTest's CPU lines and a second push (step 7): Cash took about twice as long in run A, so the test logs each phase's process, main-thread and garbage-collection time (49e0a68; your B, 2026-11-07), and you pushed a second time, against section 6's "you push once". Results in 13.4; the Cash tab stays with 15d (your A).
