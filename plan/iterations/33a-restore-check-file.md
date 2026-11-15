# plan/iterations/33a-restore-check-file.md

Stage: Build. Status: approved 2026-11-16 (your A); decisions 1 to 5 settled the same day (section 11). Built 2026-11-16 (steps 1 to 6 on iter-33a-restore-check-file); not yet on a pull request.
Source: plan.md section 4 (row 33, split into 33a and 33b, your A 2026-11-15); spec section 8 (file format, records, settings keys, restore steps 2 and 5, rejection reasons and the report), 4.1 to 4.7, 4.9, 4.10, C3, C7, C13; section 14 (K1, K6, K10); spec/example-backup.csv and spec/example-backup-expected.md; the 31 file (the writer; decision 3, the quoting; decision 5, the ids and the source's word).

## 1. Outcome
Nothing on the phone changes. Core gains K6's reading half. From a file's bytes it gives one of two answers:
- the errors section 8 asks for, every one, each with its line, its column when one applies and its reason; a row that depends on a failed row names that row's line; with the counts for "N errors in M lines";
- or, for a file with no errors, what it would load: the records made through K1 as if entered by hand, the tickers still held at the end of the file (for 33b's ticker check), and the summary of section 8 step 5 (portfolios, transactions and cash entries, the cash balance, the holdings with shares).
Reasons only; the wording is 33b's (D9).

## 2. Spec refs
- Section 8, file format: UTF-8 or rejected; a byte-order mark allowed; LF or CRLF; a header with all 19 columns matched by name; an unknown or repeated column rejected; an empty-headed, empty column ignored; an all-empty row ignored but counted; cells trimmed except `note`; a cell in a column the row type does not use must be empty; the `format` row first, version 1; ids; order; dates and numbers; tickers; portfolios by name.
- Section 8, records and settings keys: each row type's rules.
- Section 8, restore step 2 (the replay in date order, then file order, each record checked as if entered by hand; every error collected; a file-level failure stops there) and step 5 (the summary's figures).
- Section 8, rejection reasons and the report: every reason; the header is line 1, as Notepad shows it; "depends on line N"; "N errors in M lines".
- 4.1 to 4.6, the rules each record is checked against, through K1; 4.7, portfolio names; 4.9 and K10, lists, remembered texts, overrides; 4.10, exact arithmetic and picks as the picker shows them.
- C3 (a check changes nothing), C7 (cascading errors).
- Criteria: AC-16's reasons (every reason in section 8, a file with no opening balance included); AC-25's reading part (spec/example-backup.csv gives the expected figures, with or without a byte-order mark, LF or CRLF); AC-59 and AC-60 where a file reaches their rules. AC-16's report screen and Copy report, and AC-08, AC-56 and AC-57, are 33b's.

## 3. Out of scope
- Every screen, Android's file screen, Copy report, the ticker check through K4, the wording (33b).
- Storing anything: the delete and load (34, 35).
- AC-25's round trip through Excel and Notepad on a real file (34's phone check; here only the byte forms are tested).

## 4. Analysis (2026-11-15, by reading the code and the spec; nothing run)
- K1 holds every rule of 4.1 to 4.6, each called with the records so far and one new record: `OpeningBalance.create`, `CashEntries.create` and `reverse`, `Transactions.buy`, `Sells.sell`, `Splits.split`, `Reversals.reverse`. Each stops at its first failing rule. Replayed in date order, then file order, the file's records come out as hand entry would have made them, trade cash lines included (section 8: rebuilt by replay).
- K1 needs an app id for each record it makes. The check is given a way to make them, as the ViewModel is, and keeps a map from file ids (compared ignoring case) to app ids.
- Hand entry refuses a portfolio before the opening balance (4.2). In a file, portfolio rows have no date and "may be anywhere", so they are checked against 4.7's name rules only and made first.
- What is not K1's is new code in core: the file level, each row's form, ids, the picks' syntax, list and setting rows, the state rows after the replay, and the dependencies. The reader must take the quoting the writer uses (31 decision 3).
- Some K1 refusals have no section 8 reason of their own, as they are the app's limits, not rules of 4.x: an amount, or a balance, beyond exact cents (`Money`'s limit); a split number above 1,000,000 (`Splits.MAX_RATIO`, 12 decision 3); a buy's `split_to` and `split_from` too large to keep as its ratio (14c decision 1) (decision 3).
- `price_source` must be "one of the sources the app ships"; core knows no source by name, so the check is given the known words (today `finnhub`).
- Size: each K1 call works over the records so far, so a file of n records costs about n² steps. The scale tests' figures at 3,000 transactions suggest a large file could take seconds (risk 2).
- No database, no Android class, no network.

## 5. Files
- core/: BackupReader.kt (new: bytes to lines and cells, the file and row checks); BackupCheck.kt (new: the replay through K1, the dependencies, the state rows, the result). K1 is called, not changed, unless a rule must be reachable on its own (then section 14 records it).
- Tests: BackupReaderTest and BackupCheckTest (new), with spec/example-backup.csv read in a test and one test per section 8 reason; a timing check.
- Documents: this file; plan.md (row 33 split into 33a and 33b, section 11, the change log).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-33a-restore-check-file from main, one pull request.
1. This file and plan.md's lines.
2. The reader: bytes to rows. UTF-8 or rejected, the byte-order mark, LF and CRLF, quoted cells, a line break in a cell, line numbers as Notepad counts them, the header, the `format` row. Tests first.
3. The row rules: each row type's columns, required and unused cells, dates, numbers, ids, tickers, the picks' syntax, cash types, list and setting rows. Tests first.
4. The replay through K1, with the dependencies; the state rows after it (hidden, override); the result's records, held tickers and summary. Tests first: spec/example-backup.csv against its expected figures; one test per section 8 reason; C7's cascade.
5. The report's counts ("N errors in M lines"), the errors' order, and the timing on a large file.
6. Documents, the pull request draft and the open item 33 reminder. No versionCode change: the app is unchanged.

## 7. Stubs
None.

## 8. Risks
1. Section 8 lists about 60 reasons; one missed is a gap in AC-16. Each has a test, listed against section 8's bullets in step 6.
2. Time: the replay is about n² (section 4). Measured in step 5 (decision 5); above 5 s on the PC I stop and ask.
3. A file reaches cases hand entry never does (ids in other case, a pick of a lot entered later on the same date, a reversal listed before its original). Each has a test.
4. Section 8 read two ways while building: listed as a question, never chosen alone; spec.md changes only with your acceptance.
5. Open item 60 (the local build's intermittent failures).
6. 33a branches from main while #190 (32) is open; it touches only core/ and the documents. plan.md's section 11 and change log meet #190's lines when the second merges.

## 9. Proof
- Unit tests only. spec/example-backup.csv gives the expected summary and figures (portfolios 2, transactions 8, cash entries 4, cash balance $9,425.00, holdings with shares 1; the lots, hidden holding, override, lists and settings of the expected-results file). Every section 8 reason has a test. The same file with a byte-order mark and with CRLF gives the same.
- No phone check: nothing on the phone changes.

## 10. Research
- None expected.

## 11. Decisions (Build, settled by you 2026-11-16)
1. A check of a clean file gives the records ready to load, made by K1 in the replay with the app's own ids, with the held tickers and the summary; 34 stores exactly those (your A). Not chosen: (B) only "no errors" and the summary, and 34 replays the file again.
2. "Depends on line N" covers three links: a row naming a failed row by id (`lot_picks`, `reverses`), a row naming a portfolio whose row failed, and a `hidden` or `override` row for a holding whose buy or sell rows failed. The error names the line of the root cause, the first failed row at the start of the chain (your A). Not chosen: (B) links by id only, naming the row pointed to.
3. The three app limits K1 refuses that section 8 does not name (an amount or balance beyond exact cents; a split number above 1,000,000; a buy's `split_to` and `split_from` too large to keep) get reasons of their own, worded in 33b; recorded in section 14 as additions to section 8's list, not a spec change (your A). Not chosen: (B) the nearest section 8 reason.
4. The report is in line order. Within a line, a row's form errors come in the file's own column order, left to right, then its rule error or "depends on line N". A row with form errors is not replayed; K1 adds at most one rule error per row. A file-level failure is reported alone (your A). Not chosen: (B) the order found, rule errors in replay (date) order.
5. One timing test: a file of 3,000 transactions with their cash entries, checked in a plain unit test; its bound set from the first measurement, generous enough not to fail by chance, and recorded here. Above 5 s on the PC I stop and ask before going on (your A). Not chosen: (B) a growth check at 1,000 and 3,000 rows like HoldingsScaleTest; (C) no timing test, one measurement by hand.

## 12. Wording to approve (D9)
- None in 33a: reasons only. 33b proposes the report's wording.

## 13. Status and notes
- Draft 2026-11-15; approved 2026-11-16 (your A), with the split of row 33 into 33a and 33b (your A, 2026-11-15).

### 13.1 Steps
- Step 1 (2026-11-16): this file, plan.md (row 33 split into 33a and 33b, row 34 building on 33b, section 11's row, the change log). Only documents changed. Branch iter-33a-restore-check-file from main at fde0080 (git pull: already up to date).
- Steps 2 to 5 were built without stops between them and committed one per step after step 5 (your instruction), each from the tree held when its full run was green (6eefc5e, 6d8ab1b, c9e9ec1, 9a2724e; 3, 3, 3 and 4 files, each matched).
- Step 2 (2026-11-16): `BackupReader` (bytes to rows) and the reasons' first part in `RestoreReport.kt`. UTF-8 through a decoder that reports bad bytes; a byte-order mark dropped; LF, CRLF and CR each end a line; quoted cells as the writer makes them (31 decision 3); header names trimmed and matched ignoring case; an unnamed column ignored while it is empty; missing columns listed in the writer's column order; the `format` row's version read as a whole number of any size. BackupReaderTest, 14 tests.
- Step 3 (2026-11-16): `BackupRows`, each row's own rules (section 8, records and settings keys): the columns each row type needs and may have, dates, numbers, signs, ids, portfolio names, cash types, tickers, splits, a buy's ratio, the picks' syntax, list and setting rows. A failed row keeps what its dependents need (line, kind, id, portfolio, ticker, cash type). The reasons for the three app limits (decision 3). BackupRowsTest, 20 tests.
- Step 4 (2026-11-16): `BackupCheck`, the replay through K1 (portfolios first, the opening balance next, then date and file order), the rules between rows (ids, names, the opening balance, picks, reversals), the state rows after the replay (hidden, override), lists and settings, and the dependent errors naming the root cause (decision 2). A clean file gives K1's records with the app's ids, the held tickers and the summary (decision 1). BackupCheckTest, 27 tests: spec/example-backup.csv against spec/example-backup-expected.md (with and without a byte-order mark, LF and CRLF), a file the writer made checked clean with its records back, AC-59's pick as the picker shows it, and the reasons.
- Step 5 (2026-11-16): the report's order (decision 4: the file as a whole first, then line order; within a line, form errors in the file's own column order, then the rule error) and the counts of "N errors in M lines". BackupReportTest, 4 tests. The timing (decision 5): 3,000 transactions with their sells' picks, checked in 1,062 ms alone, 2,230 ms and 2,405 ms in full runs on this PC (2,605 ms in step 6's); the bound is 5 s (your A, 2026-11-16: the line at which decision 5 stops to ask). 1,394 tests (65 new: 14, 20, 27 and 4), 1 skipped; lint's 6 known findings.
- Step 6 (2026-11-16): this file (these notes, 13.2, section 14), plan.md's lines, the pull request draft. Checking the reasons against section 8's bullets for 13.2 found one without its own test: a reversal whose target is another reversal. It is reached only as an id on a reversal row (section 8: a reversal has no id, so nothing can point to one); one assertion added to BackupRowsTest. No versionCode change: the app is unchanged.

### 13.2 Section 8's reasons and their tests (risk 1)
Each bullet of section 8's "Rejection reasons and the report", with the reason it gives and the test that shows it. Reader = BackupReaderTest, Rows = BackupRowsTest, Check = BackupCheckTest, Report = BackupReportTest.
- File: not UTF-8 (`NotUtf8`, Reader aFileThatIsNotUtf8…); no header (`NoHeader`, Reader anEmptyFile…); a missing, unknown or repeated column (`MissingColumn`, `UnknownColumn`, `RepeatedColumn`, Reader aMissingUnknownOrRepeatedColumn…, anUnnamedColumn…); more or fewer cells (`CellCount`, Reader aRowWithMoreOrFewerCells…); a line break in a cell (`LineBreakInCell`, Reader aLineBreakInsideACell…); no `format` row first, a version not whole or not known (`NoFormatRow`, `VersionNotWhole`, `VersionUnknown`, Reader theFirstRowAfterTheHeader…, theVersion…; `FormatNotFirst`, Rows aSecondFormatRow…).
- Every row: `row_type` missing or unknown (`RowTypeMissing`, `RowTypeUnknown`, Rows aMissingOrUnknownRowType); a required value missing (`ValueMissing`), a cell not used (`CellNotUsed`), Rows aRequiredValueMissing_andACellNotUsed.
- Dates and numbers: `DateForm`, `DateNotReal` (Rows aDateNotInTheFormOrNotReal); `NumberForm` (Rows aNumberInAFormNotAllowed); `QuantityNotPositive`, `QuantityTooManyDecimals` (Rows aQuantityOfZeroOrLess…); `AmountTooManyDecimals`, and `AmountTooLarge` (decision 3) (Rows anAmountWithMoreThan2Decimals_orTooLarge); `WrongSign`, `CashAmountZero`, `BuyTotalZero` (Rows theSigns_andTheZeros).
- Ids and portfolios: `IdBadlyFormed` (Rows aBadlyFormedId); `IdDuplicate` (Check aDuplicateId_ignoringCase); `PortfolioNameTooShort`, `PortfolioNameNoLetter` (Rows aPortfolioNameTooShort…); `PortfolioNameDuplicate`, `PortfolioNotFound` (Check aDuplicatePortfolioName_andAPortfolioNotFound).
- Cash, tickers and splits: `CashTypeUnknown`, `CashTypeTrade`, `AdjustmentWithoutNote` (Rows anUnknownCashType…); `NoOpeningBalance`, `SecondOpeningBalance`, `OpeningBalanceNotEarliest` (Check noOpeningBalance, aSecondOpeningBalance, anOpeningBalanceThatIsNotTheEarliestCashEntry); `TickerBadCharacters` (Rows aTickerWithBadCharacters); `SplitNumberNotWhole`, `SplitNumbersEqual`, and `SplitNumberTooLarge` (decision 3) (Rows aSplitNumberNotWholeAbove0…); `BuyRatioTooLarge` (decision 3) (Rows aBuysRatio_bothOrNeither). A held ticker that cannot be verified or fails 4.3's type check is 33b's (K4).
- Sells and picks: missing or badly written picks (`ValueMissing`, `PicksBadlyWritten`, Rows missingOrBadlyWrittenPicks); a pick not found, not a buy, in another portfolio (`PickNotFound`, `PickNotABuy`, `PickOtherPortfolio`; and `PickOtherTicker`, `PickTwice`, Check aPickOfAnIdNotFound…); a lot bought after the sell or entered after it the same date (`LotBoughtAfterSell`, `LotEnteredAfterSell`, Check aLotBoughtAfterTheSellDate…); more than the lot holds, picks that do not add up (`PickOverLot`, `PicksDoNotAddUp`, Check aPickOfMoreThanTheLotHolds…); picks on a row that is not a sell (`CellNotUsed`, Rows aRequiredValueMissing_andACellNotUsed).
- Reversals: the target not found, the opening balance, listed after, already reversed, a reversal dated earlier (`ReversalTargetNotFound`, `ReversalOfOpeningBalance`, `ReversalTargetListedAfter`, `AlreadyReversed`, `ReversalBeforeOriginal`, Check aReversalsTarget); another reversal (`CellNotUsed` on a reversal's id, Rows aRequiredValueMissing_andACellNotUsed, step 6); a note on a trade's reversal (`CellNotUsed`, Check aNoteOnlyOnACashReversal).
- State rows: `HiddenHoldingNotFound`, `HiddenHoldingHasShares`, `DuplicateHidden` (Check aHiddenHoldingNotFound…); `OverrideHoldingNotFound`, `OverrideValueNotOnList`, `DuplicateOverride` (Check anOverrideForAHoldingNotFound…); `OverrideBothEmpty`, `ListUnknown` (Rows anOverrideWithBothValuesEmpty…); `DuplicateListValue`, `DuplicateRememberedText`, `ListValueMatchesRememberedText`, `RememberedTextValueNotOnList` (Check theListRows); `SettingKeyUnknown`, `SettingValueUnknown` (Rows anUnknownSettingKeyOrValue); `SettingKeyTwice` (Check aSettingKeyTwice…); `TabOrderNotEachOnce` (Rows aTabOrderThatIsNotEachTabOnce).
- Rules (4.1 to 4.6): `CashNegative` with the date and the shortfall (Check cashWouldGoNegative…); `SharesNegative` (Check sharesWouldGoNegative); `SellMoreThanHeld` (Check aPickOfMoreThanTheLotHolds…); and `DatedBeforeOpeningBalance` (Check aRecordDatedBeforeTheOpeningBalance), `SplitTickerNotBought` (Check aSplitOfATickerWithNoBuy), `BalanceTooLarge` (decision 3) (Check aBalanceBeyondExactCents).
- Dependent: `DependsOn` with the root cause's line (Check dependentRows_nameTheRootCause, aFailedPortfolio…, aSplitOrHiddenRow…).
- The report's counts and order: Report theCounts…, theReport_isInLineOrder…, theFileAsAWhole_comesFirst.

## 14. Departures from the plan
1. Two files beyond section 5's: `BackupRows.kt` (each row's own rules, step 3) and `RestoreReport.kt` (the reasons and the result types), so `BackupReader.kt` and `BackupCheck.kt` each keep one job. Two more test classes: BackupRowsTest and BackupReportTest.
2. Five reasons that section 8's list does not name, besides decision 3's: `PickOtherTicker` and `PickTwice` (4.4's lot rules), `SplitTickerNotBought` (4.5), and decision 3's limits split into `AmountTooLarge` and `BalanceTooLarge` for a row's amount and the running balance. Each is an addition to section 8's list, not a spec change; 33b words them.
3. A file with no opening balance gives one error for the file as a whole and nothing is replayed; a failed opening balance row stops the replay too, and the rows after it get no dependent errors (every row would depend on it).
4. Column names are matched ignoring case and with spaces trimmed (section 8 says "matched by name").
5. A row with more or fewer cells, or with a line break in a cell, fails only that row, not the file (section 8 lists both under "File"); the rest of the file is still checked.
6. Step 4's dependent test changed its expected order in step 5, when the report was put in line order (decision 4); no expectation was weakened.
7. A split row is linked to its holding's failed buy or sell rows (decision 2's holding link), as hidden and override rows are, so a split of a holding whose buy failed says "depends on line N" rather than `SplitTickerNotBought`.
8. The timing test's bound is 5 s (your A, 2026-11-16), not a multiple of the first measurement.
9. Step 6 added one assertion to BackupRowsTest (a reversal row with an id), found while listing 13.2.
