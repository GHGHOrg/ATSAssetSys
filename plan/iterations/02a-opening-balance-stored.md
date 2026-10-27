# plan/iterations/02a-opening-balance-stored.md

Stage: Build. Status: done: built on branch iter-02a-opening-balance-stored (commits A to M and three document commits); pull request #79 merged as 68c9015 on 2026-10-28, CI green on the pull request and on main (your reports). Base approved by you 2026-10-23, decisions 1 to 3 settled the same day (section 10), wireframe reading approved 2026-10-24 (section 12). Local checks and the phone checks done are in section 13. Not done, deferred by you on 2026-10-28: criterion 37, TalkBack (the rest of check 11 and the TalkBack part of criterion 39) and check 8 on this build. Sections 13 to 15 were written before the push; the pull request and CI results are in section 11. Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
Source: plan/plan.md section 4 (row 02, split into 02a and 02b on 2026-10-23), intent/intent.md v52 (final), spec/spec.md v4 (final).
Section 9 is "you read": pages you pasted on 2026-10-23. I did not see the pages. "My reading" is an interpretation, not a test. Versions are re-checked on the day the branch is made.

## 1. Outcome
I unlock the app, set the opening balance (date, amount and an optional note, with a review step), and Overall shows it as net worth. It is stored encrypted on the phone and is still there after a restart and after a new build is installed over the old one.

## 2. Spec refs
- spec 3 (cash entry, "app holds data"), 4.1 (order, as-of date), 4.2 (opening balance first; amounts up to 2 decimals; never 0), 4.8 (net worth is the cash balance as of today; cash only here), 4.10 (integer cents, no floating point).
- spec 6: first launch and empty state; Overall (view 1, cash only); cash entry with a review screen (wireframe 10b); date note and "ET" (C13); number format; error, empty and loading states; accessibility; theme. Wireframes 01-overall and 10b: read 2026-10-24; differences in section 12.
- spec 9: encryption at rest, no automatic copies, nothing logged; the lock of iteration 01 stays.
- spec 14: K1, K2, K3 (cash only), K9; C13, C17, C18.
- Criteria: 19, 26 (amount part), 35 (data survival), 36, 37 (partly, section 8), 38 (first look only), 9, 39 to 42.
- plan.md: K1, K2 and K3 first appear here.

## 3. Out of scope
- Bottom bar and Cash tab (02b). Deposits and later entries (03 on). Portfolios, buys, prices, backup, restore, Settings.
- The Licences screen for the SQLCipher notice: a Deploy item, decided 2026-10-23, no spec change.
- Release signing and install route (C17). Figma tokens (the template theme stays).
- Fraction type (exact quotients): no consumer until iteration 09 (price per share). plan.md says "arithmetic from 02"; this is a departure to record in plan.md (section 10).
- Parts of wireframe 01 that a later iteration builds (section 12, items 1 to 4): the allocation donut and dropdown (18), the price lines (16), the backup banner (32), Net Worth (Total) on tap (17), the Buy/Sell button (09) and the "Restore is in More only" line (02b).

## 4. Files
New, core/ (package app.ghghorg.atsassets.core, under core/src/main/kotlin/...):
- Money.kt: typed text to integer cents (digits and a dot only, no float), cents to "$10,000.00" (US style, section 6).
- CashEntry.kt: id, date, amount in cents, type, note, entered_seq, optional reverses and transaction ids (spec 3).
- OpeningBalance.kt (K1): the rule of 4.2 and the balance on a date. Valuation.kt (K3): net worth, cash only.
- MarketDate.kt: today in New York from a Clock (C13).
- RecordStore.kt: the K2 interface (load cash entries, save entries atomically).
New tests, core/src/test/kotlin/...: MoneyTest, OpeningBalanceTest, BalanceTest, MarketDateTest. PlaceholderTest.kt is replaced.
New, app/ (under app/src/main/java/app/ghghorg/atsassets/):
- data/DatabaseKey.kt (Keystore key, wrapped passphrase), data/AtsDatabase.kt (Room, one table), data/RoomRecordStore.kt (K2 implementation).
- ui/AppViewModel.kt, ui/OverallScreen.kt, ui/OpeningBalanceScreen.kt.
- app/schemas/.../1.json (generated, committed). app/src/test/...: AppViewModelTest with a fake RecordStore.
Changed: MainActivity.kt (Unlocked branch shows the app; database opens only after unlock), app/build.gradle.kts, build.gradle.kts (root), gradle/libs.versions.toml, strings.xml, CLAUDE.md, plan/plan.md.
Unchanged on purpose: AndroidManifest.xml (no INTERNET permission yet) and data_extraction_rules.xml (it already excludes every domain; my reading: nothing to narrow, the work is to check it).

## 5. Order of work
1. Branch iter-02a-opening-balance-stored from the updated main.
2. Done 2026-10-24: wireframes 01 and 10b, their README and the theme files read; the differences are in section 12 and are approved.
3. Baseline: build, tests, lint, spotlessCheck. Run the 16 KB check on the current debug APK (section 8) so any change is visible later.
4. Write the core/ tests first (Money, OpeningBalance, Balance, MarketDate), see them fail, then implement. Replace PlaceholderTest in the same commit (CLAUDE.md exception).
5. Add dependencies (versions in section 9, re-checked that day): KSP, Room 3 with its Gradle plugin, SQLCipher, lifecycle ViewModel, project(":core"). Build and lint. If the build fails, stop and show it (decision 1 fall-back).
6. Data layer in app/: Keystore AES key, random passphrase, wrapped blob in getNoBackupFilesDir(), database file in the same folder, System.loadLibrary("sqlcipher"), SQLCipher logging switched off. All Keystore and database work off the main thread.
7. RoomRecordStore. Unreadable data (blob or key lost) shows a plain error screen and never creates a new database over the old one.
8. AppViewModel with a fake store in tests: form values kept, messages for refused input, save then Overall.
9. Screens: Overall (guided empty state, or net worth), opening balance form, review, confirm. Back returns one screen.
10. MainActivity: open the database only after unlock; keep the cover screen of iteration 01 unchanged.
11. Build, tests, lint, spotlessCheck, release build. Push, read CI.
12. Phone checks (section 8). Write results here.
13. Update CLAUDE.md and plan.md in the same pull request.

## 6. Stubs
- No bottom bar until 02b. Spec says it is always visible; this is a temporary departure.
- With cash and no portfolio, spec shows "No portfolios yet" and a Create a portfolio button. The button is absent until 07.
- The unreadable-data screen has no recovery (restore is iteration 34). Wording is D9, decided on the phone.

## 7. Risks
- Room 3.0.3 and SQLCipher 4.19.1 may not build or run together here. Neither source states a tested pairing. Kotlin 2.4 turned annotations in metadata on by default (spike note); effect on Room 3 unknown.
- KSP: no source says 2.3.12 works with Kotlin 2.4.21 or with AGP 9.4.1's built-in Kotlin. Try 2.3.12, fall back to 2.3.10 (the quickstart's pairing for 2.4.21).
- Room 3 may need a higher Java target than 11. If so, that is the trigger to revisit the Java level of core/ and app/ together (CLAUDE.md).
- The README says SQLCipher logs to Logcat by default. I cannot tell if it ever prints row data. Logging is switched off and Logcat is read during entry (criteria 38, 36).
- R8 is on for release. SQLCipher and Room may need keep rules. A release build can be built, but it cannot be installed (no signing key until Deploy), so R8's effect on the data layer stays unseen.
- 16 KB pages: SQLCipher ships native libraries. Alignment of 4.19.1 is unknown. The phone's page size is not recorded.
- Keystore: key survival across update, uninstall and Android upgrade is unverified (C18); the AES and GCM code is written from memory, not from a page.
- Data stays in memory (ViewModel) while the app is locked. It is not on screen and not on disk in the clear. My reading: acceptable.
- Storage cannot be unit tested on the JVM (native library, Android). It is checked by hand; CI has no emulator.
- Room 3.0.3 is 2 days old (2026-10-21). Spike decision D2 set no minimum-age rule.
- A lost opening-balance form on theme change is the iteration 01 limit (re-lock on recreation); decision 3.
- Dynamic color is always on (minSdk 33), so the colors come from the wallpaper and one phone cannot show that contrast holds. Check 11 is by eye only (section 12, item 10).
- A future-dated opening balance is allowed (section 12, item 7). Net worth shows $0.00 until that date while the app already holds data. A test covers the rule; the phone behaviour is check 13.

## 8. Proof
Commands (PowerShell, repo root), real output shown:
- .\gradlew.bat :core:assemble :app:assembleDebug
- .\gradlew.bat :core:test :app:testDebugUnitTest, then the test count command. The report must show the new tests and no placeholder.
- .\gradlew.bat :app:lintDebug (0 findings; a new notice is listed, not silenced). .\gradlew.bat spotlessCheck.
- .\gradlew.bat :app:assembleRelease (unsigned; R8 step runs).
- 16 KB: alignment of every .so in the debug APK, before and after SQLCipher, by Android's zipalign and objdump method. The PowerShell commands are not seen yet; you paste that tab first.
- The merged manifest read back: allowBackup, dataExtractionRules, not debuggable for release.
- CI green on the pull request (your report).
By hand on the phone (Android 17), each result written back as "checked on the phone" or "not checked":
1. First launch after unlock shows the three steps; 2 and 3 are locked.
2. Opening balance form: date prefilled in New York time with the note; 0, a third decimal, text, a "$", a comma and a minus are refused and the typed text stays. The Note field takes one line only.
3. A valid amount, review, confirm: Overall shows net worth, the steps are gone.
4. Force stop, restart, unlock: the balance is still there.
5. adb install -r of a newer build: the balance is still there (criterion 35).
6. Stored file copied from the debug build (run-as, from memory, unchecked): no readable amount, no "SQLite format 3" header (criterion 36).
7. Delete the wrapped-passphrase file, restart: error screen, no new empty database.
8. Uninstall and reinstall: the app opens empty.
9. Half-fill the form, switch dark mode: typed values are kept (decision 3).
10. Logcat read while entering the balance: no amount.
11. Largest font, TalkBack, light and dark theme on Overall and the form (criteria 39 to 42), by eye only: the colors follow the wallpaper.
12. Iteration 01 checks 1 to 14 on the final build, as the spike did.
13. Opening balance dated in the future: Overall shows net worth $0.00 and the normal screens (the guide has ended).
Criterion 37's phone test (no transfer, no cloud backup): method not decided. Android's backup testing page is not read; you paste it before this check.

## 9. Research (you read, 2026-10-23)
- Room 3: stable 3.0.3 (2026-10-21); 3.0.0 was 2026-07-01; alpha 3.1.0-alpha03. Artifacts androidx.room3:room3-runtime and room3-compiler (ksp). Plugin id androidx.room3 with room3 { schemaDirectory(...) }, schemas checked in. KSP only, Kotlin only, coroutine APIs, no SupportSQLite: a SQLiteDriver is required. Room 2: 2.8.5 (2026-09-09).
- SQLCipher: net.zetetic:sqlcipher-android:4.19.1@aar plus androidx.sqlite:sqlite:2.7.0 (README, no date). Room 3 route: SQLCipherDriver(password bytes, null, null) with setDriver. Room 2 route: SupportOpenHelperFactory. System.loadLibrary("sqlcipher") by hand. API 23 and up. Licence: BSD-style, three conditions; the binary condition applies only if the app is passed on (Deploy item).
- KSP: 2.3.12 (Sep 10, minimum AGP 8.12.0), 2.3.11 (Aug 4). Quickstart pairs Kotlin 2.4.21 with 2.3.10; plugin id com.google.devtools.ksp.
- 16 KB: native code means the libraries must be aligned (LOAD segments 2**14). Lint and APK Analyzer flag misaligned libraries.
- Keystore: AndroidKeyStore with KeyGenerator; not on the main thread; authorizations fixed at generation; user authentication is opt-in; StrongBox optional (Android 9), slower. androidx.security.crypto is fully deprecated; the replacement named is KeyGenerator with AndroidKeyStore. We use that.
- Not read: SQLCipher core and LibTomCrypt licences; the PowerShell alignment commands; an AES and GCM code sample; Android's backup testing page; which KSP and androidx.sqlite versions Room 3.0.3 expects; lifecycle ViewModel artifacts and versions.

## 10. Decisions (all [Build])
Settled by you on 2026-10-23:
1. Room generation: (a) Room 3.0.3 with SQLCipherDriver. If it fails to build or run on the branch, I stop and show you before any fall-back to Room 2.8.5 with SupportOpenHelperFactory.
2. K2 and coroutines: (a) suspend functions in the K2 interface; K1 stays plain functions. I believe declaring suspend needs no extra library in core/ (unchecked; the branch build shows it).
3. Typed values across recreation: (a) a ViewModel keeps the form text and the app still re-locks on recreation, so iteration 01 check 12 stays valid. The text is in memory only and is lost if Android kills the app in the background.
Leanings, not yet confirmed by you (I apply them unless you object): no StrongBox request; AES-256 GCM key without user authentication (C18); database opens after unlock; text date field "YYYY-MM-DD" instead of a date picker (a picker may shift dates by time zone; not checked); no navigation library yet; data_extraction_rules.xml unchanged; entity has all spec 3 cash fields; PlaceholderTest replaced. New message wording that the wireframes do not cover is written by me and shown to you before use (D9).

## 11. Status and notes
Done: built (commits A to M), checked locally and on the phone as stated in section 13. Pull request #79 merged as 68c9015 on 2026-10-28. CI on the pull request: 3 checks passed. CI on main for 68c9015: success, 2m21s (spotlessCheck, build, tests and lint). Both by your report. Left for later by you on 2026-10-28: criterion 37, TalkBack, and check 8 on this build.

## 12. Wireframe and theme reading (approved by you 2026-10-24)
Files read, from your uploads of 2026-10-24. The hashes are from tool output. No earlier hash existed for them, so they identify what I read; they do not prove it equals the repo copy.
| File | What | Lines | Bytes | SHA-256 |
|---|---|---|---|---|
| 01-overall.html | wireframe 1 | 118 | 9701 | 44b797ebcd8ca1be2bd4d4c5ad41a8ebcfea40346d3d6d48e1b9ffc8e29f6caa |
| 10b-split-cash-reversal.html | wireframe 10b | 233 | 21045 | 889ef1fb9e5b31e7fc99f59dc3568372811ecc55942e7baf29000dd297b93a5e |
| README.md | wireframes README | 26 | 1997 | 0b34112de77fd1973d338ea1f1d845cc9fdb41821ccfd8e5f3f869b9f203dd96 |
| Color.kt | ui/theme | 11 | 286 | 1d3887a7b30a8b3a52b30d9d20ea7dd209d8b6010c14ddc81645d8353edba260 |
| Theme.kt | ui/theme | 58 | 1636 | bf58aa7e18cbed2bc61a83743bd88f7bd8e4eb8821e9073297a9e52a687913cc |
| Type.kt | ui/theme | 36 | 1038 | 9fa4fb82a688583b190798cd7004fed6f5c4d84b87fab67efca388e2cb6fed21 |

Left out of 02a, because a later iteration builds it:
1. Allocation donut and "Allocation by" dropdown (wireframe 01, marker 3): iteration 18. 02a shows net worth only, so criterion 24 (100% cash) is not in 02a.
2. "Prices updated" and "refresh failed" lines (markers 2 and 5): there are no prices yet.
3. Backup banner (marker 1, iteration 32), tap net worth for Net Worth (Total) (marker 2, 17), Buy/Sell button (marker 6, 09), "Restore is in More only" line (marker 8, More arrives in 02b).
4. Bottom bar: 02b (stub, section 6).

What you see or type:
5. Note: the opening balance form has an optional Note (wireframe 10b, frame C), one line only, because a line break would break the backup file (spec section 8).
6. Typed amount: "10000.00" is accepted. "$10,000.00", a comma and a minus are refused (spec section 8 number rules), although the wireframe field shows the "$" form.
7. Future date: a future-dated opening balance is allowed. Net worth shows $0.00 until that date (4.1, 4.8).
8. Review screen: the layout of frame D (Type, Date with "ET", Amount with its sign, Note, Cash effect; Confirm and save; Back to edit; "Nothing is saved until you confirm.").
9. Wording: messages the wireframes do not cover (a non-number amount, an empty or impossible date, "$" or a comma) are written by me and shown to you before use (D9). Frame J gives "Amount: cannot be 0. Up to 2 decimals." and "Not saved. The opening balance already exists."

Limits:
10. Theme: Theme.kt sets dynamicColor to true by default and minSdk is 33, so the colors come from the wallpaper. The purple palette in Color.kt is never used. Contrast (criteria 39 to 42) cannot be shown by one phone. 02a keeps the theme unchanged. Gain green and loss red are decided with the Figma token handoff, not here.
11. Frame C of wireframe 10b draws no bottom bar, although the wireframe says the bar is always visible. This is raised again when 02b is written; nothing changes here.

## 13. Build results (2026-10-28)
Everything below is your pasted output or your report. I did not see the phone. "My reading" is not a test.

### 13.1 Commits
Branch iter-02a-opening-balance-stored, all local at the time of writing (pull request #79 later merged them as 68c9015, see section 11). Newest first: M a0f7d63, L 21aa2ee, K 82c1b3b, J cf32e73, I 2e0258a, H 6161a27, G 6e5bdd9, F 00f9be5, E c3928f8, D 1edbfbf, C 875fd98, B a5c13a3, A 1b26895. main is 55a1e7a.

### 13.2 Local run
- On commit M's files (2026-10-27): spotlessCheck green; debug build green (39 tasks); forced test run (30 tasks): 74 tests, 0 failures (BalanceTest 8, MarketDateTest 10, MoneyTest 15, OpeningBalanceTest 9, AppLockTest 12, AppViewModelTest 20), fresh timestamps.
- Lint: 2 findings, both listed, neither silenced. NewerVersionAvailable for kotlinx-coroutines-test 1.9.0 (1.11.0 available) at gradle/libs.versions.toml:17; kept on purpose, it is the same coroutines core that Room 3 resolves. TypographyDashes at strings.xml:31 (date_not_a_date): the hyphens in YYYY-MM-DD are the format the field accepts, and an en dash would show a character you cannot type. Quoting the dates did not silence it. Not taken: tools:ignore, dropping the example date.
- Run on 2026-10-28 on commit M's code (the later commits changed documents only): release build (52 tasks, R8 ran; libandroidx.graphics.path.so and libsqlcipher.so packaged unstripped); 16 KB check on the debug APK: all 8 .so files (4 ABIs, 2 libraries) show align 2**14.
- R8 on the release APK (2026-10-28): the release build was green (52 tasks, 23 executed). mapping.txt (32,064,470 bytes, same time as the release APK) has no line for AtsDatabase_Impl or for any app.ghghorg.atsassets.data class; I did not find why. classes.dex of the release APK contains the names as text. Android Studio's APK Analyzer (your screenshot) lists, under app.ghghorg.atsassets.data in the release classes.dex, with their original names: AtsDatabase, AtsDatabase_Impl (1 inner class), CashEntryDao_Impl (4 inner classes), CashEntryRow, DatabaseKey, RoomRecordStore (2 inner classes), RoomRecordStoreKt, SqlCipherOpener (1 inner class). The 16 KB check read the debug APK only. Not shown: that the release app runs, because an unsigned APK cannot be installed.

### 13.3 Phone results (Android 17, debug builds)
- 1 First launch: three steps, 2 and 3 locked. Yes.
- 2 Form: date prefilled in New York time with its note; 0, 1.234, abc, $100, 1,000 and -5 refused, typed text kept. The amount keyboard shows a comma key, and a comma is refused by rule. An empty message line leaves a gap under a field (cosmetic). The date note takes the error color when an error shows.
- 3 Review and confirm: the review shows type, date with ET, amount, note, cash effect, Back to edit and "Nothing is saved until you confirm"; Back keeps the text. After 12345.67, Overall shows $12,345.67, no steps, "No portfolios yet".
- 4 Force stop and restart: the balance is still there.
- 5 adb install -r of the same APK over itself: the balance is still there. This is weaker than a newer build with a higher version code, so criterion 35 is not fully checked.
- 6 Stored file (run-as): ats.db 16,384 bytes, ats.db-shm 32,768, ats.db-wal 0, ats.db.lck 0, db_passphrase.bin 93 bytes (my arithmetic: 1 + 12 + 64 + 16, not checked in the file). The first 16 bytes look random, not "SQLite format 3". grep counts were 0 for zzznote, cash_entries (in ats.db and ats.db-shm) and OPENING_BALANCE. The "SQLite format" search was split by my shell and searched "SQLite" alone (0). The 12345 count is weak: the amount is stored as integer cents. Criterion 36 is partly checked: not every page, and not the update, reinstall and key-loss cases. A later inspection must also search ats.db-wal (a fresh database had 16,512 bytes there).
- 7 Lost blob (db_passphrase.bin renamed): the screen reads "Your data cannot be read. The stored data could not be opened. Nothing was changed or deleted. Close the app and open it again. If this stays, the only way back is a restore from a backup file, which this version does not have yet." No way to create a balance, and no new blob appeared. Two later hidden-blob starts (16:27 and the repeat) kept the database hash, size and times unchanged. Renaming the blob back returned the balance, twice. Open: before the first hidden-blob start the ats.db hash changed (b01e0538... to e3988592...) and all four database files were stamped 16:14. I could not explain it and did not reproduce it. Also seen: a normal start changes the hash and the time of all four files at open (e3988592... to cc77983a... at 16:32, 74c6522e... at 16:40), then nothing changes through a wait and a force stop; cause not shown. Wording point: "Close the app and open it again" may not help when the blob is lost.
- 8 Uninstall and reinstall: the app opens empty with the three steps. The folder has a new owner id, a new ats.db (4,096 bytes, hash adc99292...) and a new blob.
- 9 Dark mode while the form is open: the form reopens with the typed values (after the re-lock).
- 10 Logcat since logcat -c, before the first launch: no line matches 12345, 1234567 or zzznote. It would not match the formatted "12,345.67", and the buffer may have rolled over. The JNI layer and SQLCipher core logging are not switched off by the Java setting. Evidence, not proof.
- 13 Future date: 2027-01-15 accepted; Overall shows $0.00 with the normal screens, no guide and "No portfolios yet"; the same after a force stop and restart.

### 13.4 Largest font and iteration 01 checks (you ran them on the final build)

Largest font, defects you found and the fixes:
- "(locked)" on the Overall steps: at the largest font the screen read "Enter a buy locked". Commit K sets each locked step as a wrapping two-part row, with "(locked)" in the secondary color, right-aligned. You confirmed three steps, with step 1 active and steps 2 and 3 marked "(locked)".
- Keyboard covering fields: the keyboard covered the selected field and the page would not scroll (criterion 39). Commit L makes the form scroll and adds imePadding(). You reported all four checks yes; the gap under a field is normal, not a defect.
- Long note and review screen: you asked for the note to wrap inside its field, and for the review rows and the two buttons to flow as responsive two-column rows. Commit M does this, with a Done key instead of a line-break key. After M you reported: keyboard shows the check mark, no line-break key; all fine except the note text, which you wanted left-aligned. I changed that in M's final form, and you then confirmed all yes, the "none" row looks okay, and the net worth card looks okay.

Iteration 01 checks on the final build (your report): checks 1-7 all yes; check 9 (same APK installed over itself) the $500.00 was still there; checks 10-14 all yes. Check 8 (no screen lock) was not re-run.

### 13.5 Not run, not checked, or deferred

Deferred by you on 2026-10-28:
- criterion 37 (backup and device-transfer test)
- the TalkBack check, which includes the TalkBack part of criterion 39
- check 8 (no screen lock) on this build

Not run:
- CI, and any pull request
- the release APK on a device
- the merged-manifest read-back
- installing a newer build over this one (criterion 35 is only partly checked)
- the key-survival and lock-removal check
- the Keystore KeyInfo security level
- the -journal file check
- atomicity of multi-row saves
- contrast measurement
- a wrong-key test (opening the database with a wrong passphrase)

## 14. Departures from the plan as approved (2026-10-28)
Items 1 to 9 come from the build and your phone reports. They are not in sections 1 to 12.
1. Extra file ui/AppContent.kt: shows the screen the ViewModel names (loading, unreadable, Overall, form, review).
2. Extra file data/SqlCipherOpener.kt: loads SQLCipher, switches off the Java client library's Logcat output, and builds Room with SQLCipherDriver. A LinkageError from the native library becomes UnreadableDataException.
3. closeForm() and a Back button on the form (commits G and H). The Back button wording is new and was not in the plan.
4. Guided steps 2 and 3 show "(locked)", as wireframe 01 does, and not the "Set the opening balance first" message I drafted. That message is kept for taps on buttons that arrive later. Your phone check at the largest font showed "Enter a buy locked" reading as one phrase. Commit K: "(locked)" sits in a FlowRow, right-aligned, and wraps under the title when it does not fit.
5. The form scrolled under the keyboard at the largest font. Commit L: imePadding() before verticalScroll. Found by you on the phone.
6. The note field wraps its text and shows a Done key (commit M). Section 8 item 2 and section 12 item 5 say "one line only". spec.md does not say that, and its rule that a note has no line break (section 8) is unchanged; a keyboard that still inserts one gets the existing refusal.
7. The review rows and the two review buttons use FlowRow. The note value is left-aligned (commit M).
8. The review check uses a probe call of OpeningBalance.create with the id "check", a stand-in amount of 1 cent and today's date, so all refusals show together.
9. Room 3.0.3's builder has no allowDataLossOnRecovery (javap on the cached jar, 2026-10-27), so the call is not made, and no fallbackToDestructiveMigration method is called. Room 3.1.0-alpha03 (2026-10-21, your paste) adds allowDataLossOnRecovery() and setPreparedStatementCacheSize(Int). If the app moves to Room 3.1 or later, decide whether to call it. It would let Room recover from open or corruption failures at the cost of data, which conflicts with the rule that unreadable data shows the error screen and never creates a new database over the old one (spec 9, C18). It needs an explicit decision and a new intent first. The 3.1 default is assumed off (an opt-in method), not verified. Source: your paste.

## 15. New risks, research, known limits and wording points

### 15.1 New risks (none proven either way)
- Inserting through rawQuery is unproven. The code was read, not run on the insert path.
- A cursor may stay open after its statement is closed. This has not been shown either way.
- A Long binding in the driver was not seen in source.
- The journal-mode path was read in source, not run.
- What Room 3 does when the open fails is unknown. Phone check 7 shows the screen and the unchanged file, not Room's internal behavior.
- There is no proof of atomicity for multi-row saves until iteration 03 or 09.
- R8 may remove AtsDatabase_Impl. In the release APK it is present with its original name (13.2). Whether Room finds it when the release app runs is not shown, and mapping.txt not listing the data classes is unexplained.
- libsqlcipher.so is not stripped. The strip message names it, and it is left as is.

### 15.2 Research notes
- Room 3.1.0-alpha03 (2026-10-21) adds allowDataLossOnRecovery and setPreparedStatementCacheSize(Int). Room 3.0.3 has neither. A later version should consider allowDataLossOnRecovery() once 3.1 is stable.
- SQLCipher logging has three areas: the Java client, the JNI layer and the SQLCipher core pragmas. Logger.setTarget(NoopTarget()) covers only the first.
- The backup-testing page you gave was fetched, along with the cloud and device-to-device scripts. The phone state is Backup Manager disabled, the Google transport selected, and the local transport parameters null. Both scripts run bmgr enable true and do not undo it, so criterion 37 must run that cleanup afterwards.
- data_extraction_rules.xml is still the generic baseline (752 bytes). It needs tightening in a later iteration.

### 15.3 Known limits
- The balance sum has no overflow guard. Iteration 03 must add one.
- The note rule rejects only \n and \r.
- Net worth does not refresh at midnight New York until the next load or save.
- The ViewModel keeps data in memory while the app is locked.

### 15.4 Wording and design points for D9
- A gap shows under a field with an empty message.
- The date note takes the error color when an error shows.
- The amount keyboard shows a comma key that the rule refuses.
- "Close the app and open it again" may not help when the blob is lost.
