# plan/iterations/02a-opening-balance-stored.md

Stage: Build. Status: base approved by you 2026-10-23; decisions 1 to 3 settled the same day (section 10). Not yet approved to start the branch: that waits for the wireframes and plan.md. Nothing is built.
Source: plan/plan.md section 4 (row 02, split into 02a and 02b on 2026-10-23), intent/intent.md v52 (final), spec/spec.md v4 (final).
Section 9 is "you read": pages you pasted on 2026-10-23. I did not see the pages. "My reading" is an interpretation, not a test. Versions are re-checked on the day the branch is made.

## 1. Outcome
I unlock the app, set the opening balance (date and amount, with a review step), and Overall shows it as net worth. It is stored encrypted on the phone and is still there after a restart and after a new build is installed over the old one.

## 2. Spec refs
- spec 3 (cash entry, "app holds data"), 4.1 (order, as-of date), 4.2 (opening balance first; amounts up to 2 decimals; never 0), 4.8 (net worth is the cash balance as of today; cash only here), 4.10 (integer cents, no floating point).
- spec 6: first launch and empty state; Overall (view 1, cash only); cash entry with a review screen (wireframe 10b); date note and "ET" (C13); number format; error, empty and loading states; accessibility; theme. Wireframes 01-overall and 10b: not seen by me yet.
- spec 9: encryption at rest, no automatic copies, nothing logged; the lock of iteration 01 stays.
- spec 14: K1, K2, K3 (cash only), K9; C13, C17, C18.
- Criteria: 19, 26 (amount part), 35 (data survival), 36, 37 (partly, section 8), 38 (first look only), 9, 39 to 42.
- plan.md: K1, K2 and K3 first appear here.

## 3. Out of scope
- Bottom bar and Cash tab (02b). Deposits and later entries (03 on). Portfolios, buys, prices, backup, restore, Settings.
- The Licences screen for the SQLCipher notice: a Deploy item, decided 2026-10-23, no spec change.
- Release signing and install route (C17). Figma tokens (the template theme stays).
- Fraction type (exact quotients): no consumer until iteration 09 (price per share). plan.md says "arithmetic from 02"; this is a departure to record in plan.md (section 10).

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
2. Read wireframes 01 and 10b (you upload them). List any difference from my reading here.
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
2. Opening balance form: date prefilled in New York time with the note; 0, a third decimal and text are refused and the typed text stays.
3. A valid amount, review, confirm: Overall shows net worth, the steps are gone.
4. Force stop, restart, unlock: the balance is still there.
5. adb install -r of a newer build: the balance is still there (criterion 35).
6. Stored file copied from the debug build (run-as, from memory, unchecked): no readable amount, no "SQLite format 3" header (criterion 36).
7. Delete the wrapped-passphrase file, restart: error screen, no new empty database.
8. Uninstall and reinstall: the app opens empty.
9. Half-fill the form, switch dark mode: typed values are kept (decision 3).
10. Logcat read while entering the balance: no amount.
11. Largest font, TalkBack, light and dark theme on Overall and the form (criteria 39 to 42).
12. Iteration 01 checks 1 to 14 on the final build, as the spike did.
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
Leanings, not yet confirmed by you (I apply them unless you object): no StrongBox request; AES-256 GCM key without user authentication (C18); database opens after unlock; text date field "YYYY-MM-DD" instead of a date picker (a picker may shift dates by time zone; not checked); no navigation library yet; data_extraction_rules.xml unchanged; entity has all spec 3 cash fields; PlaceholderTest replaced.

## 11. Status and notes
Base approved, decisions settled (section 10). plan.md change log (02 split, Fraction departure, no bottom bar in 02a) is written when this file is saved and plan.md is uploaded again.
