# plan/iterations/33a-restore-check-file.md

Stage: Build. Status: approved 2026-11-16 (your A); decisions 1 to 5 settled the same day (section 11). Step 1 (documents) done; steps 2 to 6 not built.
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

## 14. Departures from the plan
- None yet.
