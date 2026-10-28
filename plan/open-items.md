# plan/open-items.md

Stage: Build. Created 2026-10-28 (plan/spikes/vscode-claude-code.md, decision D4).
One list of what is still open. When an item is closed, change its status here with the date and the evidence. It does not replace the iteration files, which keep the full detail.

| # | Item | Source | Status |
|---|---|---|---|
| 1 | ats.db hash changed once (b01e0538... to e3988592..., all database files stamped 16:14) while db_passphrase.bin was hidden. Not reproduced in two later hidden-blob starts | 02a file, 13.3 check 7 | open, unexplained |
| 2 | A normal start changes the hash and times of all four database files at open (e3988592... to cc77983a... at 16:32, to 74c6522e... at 16:40). Cause not shown | 02a file, 13.3 | open, unexplained |
| 3 | The release build's mapping.txt lists no app.ghghorg.atsassets.data class, yet the release classes.dex defines them with their original names (APK Analyzer) | 02a file, 13.2 | open, unexplained |
| 4 | The release app was never run (the unsigned APK cannot be installed). Whether Room finds AtsDatabase_Impl at run time is not shown | 02a file, 15.1 | open |
| 5 | Criterion 37: backup and device-transfer test | plan.md change log 2026-10-28 | deferred; iteration chosen when the 02b file is written |
| 6 | TalkBack check, including the TalkBack part of criterion 39 | same | deferred, same |
| 7 | Check 8 (no screen lock) on the 02a build | same | deferred, same |
| 8 | Not run in 02a: newer-build install-over (criterion 35 only partly checked), key survival and lock removal, Keystore KeyInfo level, the -journal file, contrast, a wrong-key test | 02a file, 13.5 | open |
| 9 | The balance sum has no overflow guard | 02a file, 15.3 | iteration 03 must add it |
| 10 | The note rule rejects only \n and \r | 02a file, 15.3 | known limit |
| 11 | Net worth does not refresh at midnight New York until the next load or save | 02a file, 15.3 | known limit |
| 12 | The ViewModel keeps data in memory while the app is locked | 02a file, 15.3 | known limit |
| 13 | data_extraction_rules.xml is still the generic baseline | 02a file, 15.2 | tighten in a later iteration |
| 14 | Unproven: insert via rawQuery, a possible open cursor after a statement closes, Long binding in the driver, the journal-mode path (read, not run), Room 3 failed-open behaviour, atomicity of multi-row saves. Before iteration 03: you paste CashEntryDao_Impl.kt and the Room schema 1.json | 02a file, 15.1 | open |
| 15 | libsqlcipher.so is packaged unstripped (the strip message names it) | 02a file, 15.1 | known, not silenced |
| 16 | Wording (D9): the gap under an empty message line, the date note's error color, the comma key shown but refused, "Close the app and open it again" may not help when the blob is lost | 02a file, 15.4 | decide on the phone |
| 17 | Price-source spike (plan/spikes/price-source.md) must finish before iteration 09. After 02b I ask you: paste official pages, or approve searches | plan.md section 7 | not written |
| 18 | Install route (C17), criterion 57, the Licences screen for SQLCipher and the D2 revisit at Deploy, Figma tokens, iteration 21 must keep the cover-screen behaviour | plan.md section 8 | later |
| 19 | Room 3.1: consider allowDataLossOnRecovery() once 3.1 is stable (3.0.3 has no such method) | CLAUDE.md pitfalls | later |
| 20 | Dependabot pull requests #67 (Gradle wrapper 9.8.1) and #68 (core-ktx, already applied on main) | dependency-upgrade note 9.9 | open, status not checked since |
| 21 | Remote branch docs/readme-user-guide | hand-off | unchecked |
| 22 | The Gradle configuration-cache "unknown location" message, and the LF to CRLF warning on commit | hand-off | unexplained, harmless so far |
| 23 | The Project instructions in claude.ai are not changed by the spike. The repo's CLAUDE.md is the source for Claude Code, and the Project text can go stale | spike note 3.1, D2 | known |
