# Table of contents

- [1. Stage rules (Ruleset A)](#1-stage-rules-ruleset-a)
- [2. Question rules (Ruleset B)](#2-question-rules-ruleset-b)
- [3. Commands (Windows PowerShell, repo root)](#3-commands-windows-powershell-repo-root)
- [4. Conventions](#4-conventions)
- [5. Where things are](#5-where-things-are)
- [6. Known pitfalls](#6-known-pitfalls)
- [7. Standing checks for every iteration's screens](#7-standing-checks-for-every-iterations-screens)
- [8. Working model in Claude Code](#8-working-model-in-claude-code-decided-2026-10-28-planspikesvscode-claude-codemd-section-5)

# CLAUDE.md

Rules, commands and pitfalls for working in this repo. Read this first.
Project: ATS Asset System, a personal Android asset tracker (one user, sideloaded).

## 1. Stage rules (Ruleset A)
This project builds working software following the AI-Native SDLC playbook (https://claude.com/blog/the-ai-native-sdlc-playbook). No copy of the playbook is kept in the repo. Fetching it asks first (section 8).
These rules apply to every action in this project.

Stage order: Plan -> Design -> Build -> Test -> Deploy -> Maintain.
Each stage ends by producing a file artifact that the next stage reads:
intent.md -> spec.md -> plan.md -> code + tests -> review findings -> new intent.md.

Rules:
1. Never write code before an approved plan.md exists. If asked to skip ahead, say which artifact is missing and offer to produce it first.
2. Plan: turn my description into intent.md (problem, outcome, users, constraints, open questions). I correct it and accept it.
3. Design: produce spec.md from intent.md. Flag concerns and conflicting constraints explicitly. I approve before Build.
4. Build: produce plan.md (files, order of work, risks, proof) and wait for my approval. Keep a CLAUDE.md with commands, conventions and known pitfalls. If implementation departs from the plan, update plan.md.
5. Test: write tests with the code. Run build, tests and lint before saying anything is done, and show the real output. For bug fixes, write the failing test first. Never weaken or delete a test to make it pass.
6. Deploy: review the finished work against spec.md and plan.md (bugs, security, compliance) and report findings by severity before calling it ready. Anything risky needs my explicit approval.
7. Maintain: treat every bug or change request as a new intent.md.
8. At the start of each response, state the current stage. If a request would skip a gate, stop and tell me.
9. Be honest about anything you could not run or verify.

## 2. Question rules (Ruleset B)
- Every question asked must be tagged with a specific stage (e.g. Plan) defined by the playbook in mind.
- Do not ask questions or accept answers that are not meant for the tagged stage.
- Before doing a web search, show me the details and let me decide to proceed or not.

## 3. Commands (Windows PowerShell, repo root)
Environment (user variables, set once): JAVA_HOME = Android Studio's jbr folder; ANDROID_HOME = the Android SDK folder.
There is no local.properties file on purpose (see pitfalls).
- Build:  .\gradlew.bat :core:assemble :app:assembleDebug
- Test:   .\gradlew.bat :core:test :app:testDebugUnitTest :app:testReleaseUnitTest --tests "*BackupDueReleaseTest"
  (the filter runs only the release build's own test, so the shared unit tests don't run twice; 38a decision 1. The release build's unit tests exist through `android.onlyEnableUnitTestForTheTestedBuildType=false` in gradle.properties; 38a decision 1a.)
- Lint:   .\gradlew.bat :app:lintDebug
- Style:  .\gradlew.bat spotlessCheck (read-only; CI runs it first). .\gradlew.bat spotlessApply rewrites files: run it only when asked, and show the diff first.
- All three, forced fresh: add --rerun-tasks (use it for lint too, see pitfalls)
- Install on the phone: & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
  (adb is not on PATH on this machine. The APK path is confirmed. The install has been run on the phone:
  setup proof item 4 (2026-10-18), iteration 01 (2026-10-19) and the dependency-upgrade spike (2026-10-22).)
  Since 38a the debug APK is the development app "ATS Dev" (`app.ghghorg.atsassets.debug`, test data only; AC-65). It installs next to your daily app (`app.ghghorg.atsassets`), never over it. Your app is updated only by a signed release (38b, 38c).
- Release build: .\gradlew.bat :app:assembleRelease (unsigned, R8 on the libraries; asks through the release hook, section 8). Its checks, in Git Bash: `bash .github/scripts/release-version-check.sh [TAG]` (empty TAG: a dry run) and `DEXDUMP=/d/AndroidSdk/build-tools/36.0.0/dexdump.exe bash .github/scripts/release-class-check.sh app/build/outputs/apk/release/app-release-unsigned.apk`.
- A release, its rollback and the release notes template: plan/release.md, one command at a time.
CI (.github/workflows/ci.yml) runs the style check and then the same three steps on ubuntu-26.04 with Java 25 (Temurin); the release build's test (38a) is its own step after the unit tests. After the unit tests, also when they fail, its step "Scale test retries" prints every check HoldingsScaleTest measured again, each try, or "none" (24a). After lint it builds the release APK and runs the release class check (38b decisions 2 and 6). .github/workflows/release.yml runs on a `v*` tag, on a pull request that changes it or its scripts (a dry run), and by hand once it is on main: the version check, the release build, the class check, the APK and mapping.txt kept 90 days (38b).

## 4. Conventions
- One branch per iteration: iter-NN-name. One pull request per iteration.
- Commit messages name the iteration and the spec refs.
- Every write to transactions or cash goes through K1 (spec section 14, rule 1).
- core/ is pure Kotlin/JVM: no Android classes, no database, no network.
- K3 and K9 only read data. K4 only writes prices and instrument data. These boundaries are kept by review and checked at Deploy.
- Exact arithmetic only (spec 4.10).
- A change request (Ruleset A rule 7) gets its own intent file, intent/intent-cr-NN-short-name.md, and plan.md's iteration row links to it. intent.md stays at v52. spec.md stays one file, and each change edits it (decided 2026-11-03, CR1).
- A defect in test support (androidTest helpers, workflows) that changes no app behaviour and no spec is not a change request and needs no intent file: record it as an open item and fix it as a step of the next iteration (decided 2026-11-10, after main's screen tests failed on e83e6f4 from a font-scale reset that did not wait; open item 53).
- A new or changed screen shows only what its screen model prepares (plan.md section 3; CR2). Build the model in a builder with unit tests, plus one ViewModel wiring test per screen; the screen only picks the approved string and puts the model's values in. Don't format or work out a shown figure, text or message inside a composable. Screens built before 15a are open item 48: give one a model when you change it.
- A new or changed screen gets the screen tests that apply to it (spec AC-61; plan.md section 3): a fixed part is measured at the largest font in landscape, a layer or menu must keep MainActivity's window focus, a text field's layer must stay above the keyboard at the largest font, and a row or card whose TalkBack text carries a figure is checked for that text. Build on the helpers in app/src/androidTest/…/ScreenTestSupport.kt.
- A red screen-tests run is not merged. The tests run as 3 shard jobs (37); each keeps its own artifact, `screen-tests-shard-K-attempt-N`. Re-run only the failed shard's job: the count job `screen-tests` then uses the other shards' earlier attempt (37, step 3). Read its cause before a re-run: the run's artifact holds the results, the emulator log and any unlock screenshots, and logcat holds the ScreenTestUnlock lines. Each re-run needs your yes.
- A branch that is never merged (defects brought back, experiments) is named throw-away-NN-what, and its pull request is a draft that is closed unmerged (15b step 4).
- Refer to a record by its file and section, not by a shorthand name: "plan/spikes/screen-tests.md 11.4, series 3", not "spike F, series 3", and the same for decisions and runs. A shorthand like "spike F" or "your B" means nothing to a later reader (15b step 3).
- When you edit a document that has a table of contents at the top (spec.md, plan.md, CLAUDE.md), update the table too: add, rename or remove the entry for each heading you add, rename or remove, and check each link against its heading's anchor.
- The merge record rewrites the status, not only appends to it: the iteration file's status (line 3) says "Done" and its plan.md section 11 row starts with "done", and both name the pull request and its merge commit. Before proposing the commit, list every iteration file's line 3 and every section 11 row and compare them (found 2026-11-10: 11 said "not built"; 12, 13a, 13b and 15a did not name their merge; 16c to 21 kept "built").
- Versions are MAJOR.MINOR.PATCH (plan/iterations/38a-development-app.md, decisions 2, 2a, 2b, 2c and 2d). A tag equals the release's versionName. The first release (38c) is 0.38.0, tag v0.38.0. 1.0.0 (iteration 40, 2026-11-24, tag v1.0.0) is the first productive release, by your decision; from it on, the rules below apply as written (2.0.0 for a milestone or a breaking change, 1.1.0 for a feature, 1.0.1 for a fix). Before 1.0.0 they moved down one place: a milestone or a breaking change raised MINOR, a feature or a fix PATCH.
  - MAJOR: a big milestone (for example performance over time), or a breaking change: (a) the previous release cannot read the new backups (the backup format version rises, spec section 8); (b) keeping your data needs more than `adb install -r` (a new signing key, C21, or a change of encryption); (c) your existing records show different figures, a figure shown at another precision included (4 places instead of 8, cents dropped); (d) an Android version the app ran on is dropped (minSdk raised); (e) a feature you use is removed or replaced.
  - MINOR: a new feature, a database schema change the app migrates, a change of style only (a thousands separator, a date format).
  - PATCH: a fix.
  - The development app shows `x.y.z-dev.n`, n its versionCode (`1.0.0-dev.50`); your app shows `x.y.z`. No `-test` or `-rc` labels.

## 5. Where things are
- intent/intent.md (v52, final) and one file per change request (intent/intent-cr-NN-short-name.md; CR1 is intent-cr-01-tiny-remainders.md, CR2 intent-cr-02-screen-models.md, CR3 intent-cr-03-screen-tests.md, CR4 intent-cr-04-release-process.md, CR5 intent-cr-05-day-change-and-screens.md), spec/spec.md (v8, final, one file; acceptance criteria tagged AC-NN, code comments may still say "criterion NN"), spec/wireframes/, plan/plan.md, plan/setup.md, plan/release.md (38b).
- Spec sections: find them by heading, for example "## 4. Business rules".
  Sections 2 (concerns, C-numbers), 3 (domain), 4 (rules), 6 (views), 7 (prices), 8 (backup file), 9 (security), 11 (criteria), 14 (components, K-numbers).
- Each iteration file in plan/iterations/ lists its own spec sections, criteria and examples.
- Adapter code, the only code that knows an outside service or library's own API, sits in its own folder under app/src/main/java/app/ghghorg/atsassets/: prices/finnhub/ for Finnhub (K4: its URLs, the X-Finnhub-Token header, its fields) and data/ for Room and SQLCipher (K2). Outside prices/finnhub/, only MainActivity names Finnhub, where it builds the source; the rest of the app uses core's PriceSource and RecordStore (16a, step 2), and the screens show the source's name as `PriceSource.name` gives it (30).

## 6. Known pitfalls
- No floating point for money or quantity (4.10).
- Quantity.shown is the only place a quantity is rounded for showing, picking or judging; a quantity above 0 never shows 0 (CR1, spec 4.10). Do not round a quantity with Fraction.rounded directly.
- A buy's exact quantity is boughtQuantity (quantity × quantityRatio), not quantity: a buy re-entered with its quantity left as filled in stores the original's quantity and a ratio in the split columns (CR1). Use boughtQuantity wherever a buy's shares, cost or price are worked out or shown (14c, section 14, item 1).
- Never log or send amounts, tickers, quantities, notes or portfolio names (spec section 9). Never log the price-source key either: no HTTP logging, not even in debug builds; the key goes in the X-Finnhub-Token header only, never in a URL (price-source note, decision 5).
- The price-source key is entered in Settings and stored in the encrypted database's settings row; no key is built into the app and the build reads none (30 decision 0, replacing the price-source note's decision 5). A saved key is never shown, logged or put in a message or a model: only its last 4 characters (`PriceKey.ending`); the screen state holds a key only as the text being typed in the key entry. Only letters and digits are a key (`PriceKey.isKey`), so nothing else reaches the header.
- `FINNHUB_API_KEY` was removed as a user variable on this PC (2026-11-21); nothing in the build or CI reads it (30). If one is set again for a trial call to Finnhub, never print it or anything derived from it, not even its length: test only `-not [string]::IsNullOrEmpty($env:FINNHUB_API_KEY)` (True/False) (16a).
- No analytics or crash-reporting library.
- Docs in this repo use CRLF. Keep line endings, do not retype files.
- Claude Code's Write tool makes new files with LF. Its Edit tool kept CRLF on existing files, including multi-line inserts (02b). spotlessCheck rejects an LF Kotlin file in this working copy, so convert a new file's line breaks to CRLF right after Write. Git stores these files with LF (git ls-files --eol shows i/lf w/crlf), so git diff --check cannot see line-ending damage. After every document edit, count the raw CR and LF bytes as well (the report in section 8) and run git diff --stat.
- Claude Code's allow rules in .claude/settings.json stay inactive until its own trust dialog is accepted for the folder. VS Code's Workspace Trust does not count and the panel shows no warning; /permissions shows ALLOW (0) when this has happened. The trust is keyed on the exact path text, so E:/GHRepo/ATSAssetSys and e:/GHRepo/ATSAssetSys are two records in ~/.claude.json. To record one, run the extension's claude.exe once from that path and accept the dialog (spike vscode-claude-code, 9.6). Do not read ~/.claude.json with ConvertFrom-Json in Windows PowerShell: it rejects two keys that differ only by case.
- [UNVERIFIED] items in spec.md are not facts. Check them on the phone before relying on them.
- Message wording is D9: decided on the phone, not on paper.
- The Compose theme file in app/ is the source of truth for colors and type. Figma is only the starting point.
- Java: the project needs Java 25 (gradle/gradle-daemon-jvm.properties). core/ and app/ both compile at Java 11. The first reason (Kotlin 2.2.10 stopped at Java 24) is gone with Kotlin 2.4.21. The level stays 11 because app will use core/ from iteration 02. Revisit both together, not one.
- VS Code Kotlin extension (jetbrains.kotlin-server): after it was installed, the build failed with "jlink executable ... does not exist" under .vscode\extensions\jetbrains.kotlin-server-...\server\jbr. `.\gradlew.bat javaToolchains` showed two Java 25 installs: Android Studio's jbr (a JDK) and the extension's jbr ("Is JDK: false"). Fix that worked: `.\gradlew.bat --stop`, then rebuild. Cause is my reading, not proven: the daemon ran on the extension's Java. A property fix (org.gradle.java.installations.auto-detect=false plus .paths) is untested.
- Do not create local.properties: lint rejects the Windows path form. Use ANDROID_HOME.
- gradlew must stay LF and executable (.gitattributes, mode 100755). Do not retype it.
- In PowerShell, do not use .Split('text') to count a substring: it splits on each character.
- In PowerShell, an ordered table (`[ordered]@{}`) with number keys is indexed by position: `$t[1]` is the second entry, not key 1. Loop over a plain list of entries instead. When building held steps' commits through a temporary index, compare each commit's `git diff --cached --stat` with its step's report before committing (21: five commits got the next step's files and were reset with your yes). To hold each green step's tree: set `$env:GIT_INDEX_FILE` to a scratchpad file, `git read-tree HEAD`, `git add -A`, `git write-tree`; later commit them in order with `git read-tree <tree>` on the real index, comparing each step's staged file count with its report first (30: 3, 10, 7, 4, 26, 7, 6 files, all matched).
- In PowerShell, `git rev-parse HEAD^{tree}` is mangled: the braces become a script block and the argument turns into "-encodedCommand …". Quote it: `git rev-parse 'HEAD^{tree}'` (found 2026-11-23; quoted in 40).
- A draft GitHub Release's address reads `releases/tag/untagged-…` until it is published. Read the draft by its tag with `gh release view <tag>` (40 step 8).
- In PowerShell, read the branch with `git rev-parse --abbrev-ref HEAD`. `(git status -sb)[0]` printed "#", and `git status -sb | Select-Object -First 1` gave exit code 255 though git succeeded, because Select-Object closes the pipe (30).
- A pull request's CodeQL run is not listed by `gh run list --branch <head>`; read it with `gh pr checks <n>`. A draft pull request ran no CodeQL in 30, but draft #188 (31) showed CodeQL and Analyze (actions) passing; read `gh pr checks <n>` rather than assume either way.
- The workflows run only on a pull request or a push to main. A pushed branch with no pull request gets no runs (30).
- The CI runner is pinned to ubuntu-26.04 (moved from ubuntu-24.04 in the dependency-upgrade spike; CI green). Do not use ubuntu-latest.
- Lint shows 7 findings, all listed on purpose, none silenced: AndroidGradlePluginVersion (AGP 9.5.2 available, gradle/libs.versions.toml:2; open item 31, since 06), NewerVersionAvailable for sqlcipher-android 4.19.1 (4.20.0 available, :15; open item 43, since 12; Dependabot closed #123 on 2026-11-21 as "no longer updatable"), NewerVersionAvailable for kotlinx-serialization-json 1.12.0 (1.13.0 available, :18; since 30), TypographyDashes on the date_not_a_date string in strings.xml (its line moves as strings are added; 04 saw :58, 15b and 39 :86) (the hyphens in the YYYY-MM-DD example), and, since 2026-11-20 (seen in 36 step 1), GradleDependency for lifecycle-runtime-ktx and lifecycle-viewmodel-compose 2.11.0 (2.11.1 available, :8) and compose-bom 2026.09.00 (2026.11.01, :11). 39 cleared Spotless, kotlinx-coroutines-test and KSP. Do not silence a new notice: list it.
- Style checker: Spotless 8.12.0 with ktlint 1.8.0 (39), targets set by hand in the root build.gradle.kts. @Composable functions are exempt from ktlint's function-naming rule through an override there; the property name is supported only by its effect.
- Dependabot has one group, kotlin (pattern org.jetbrains.kotlin.*). Whether it matches the Kotlin plugins is unverified until Dependabot has run once.
- A plain `.\gradlew.bat :app:lintDebug` after a change can leave the lint report stale (seen after commit L: the report time did not move). Use `--rerun-tasks` and check the report time.
- ktlint rules that Spotless enforced in 02a (not the full list; spotlessCheck decides): imports in ASCII order with java.* last, one parameter per line when a function has two or more, lines of at most 140 characters.
- developer.android.com reference pages and cs.android.com could not be fetched by my tools. Paste the page, or check the library with javap on the cached jar.
- A version-catalog entry cannot carry the @aar suffix (found in 02a for SQLCipher); check how the dependency is declared before moving it to the catalog.
- Room 3.0.3 (androidx.room3) has no allowDataLossOnRecovery (javap, 2026-10-27), so the code does not call it and calls no fallbackToDestructiveMigration. Room 3.1.0-alpha03 (2026-10-21) adds it; consider it once 3.1 is stable.
- SQLCipher: Logger.setTarget(NoopTarget()) switches off the Java client's Logcat output only, not the JNI layer or the SQLCipher core.
- The message "Unable to strip ..." appears in every release build and names libandroidx.graphics.path.so and libsqlcipher.so (both are packaged unstripped). It is known and not silenced.
- Dependency-upgrade spike: plan/spikes/dependency-upgrade.md, run before iteration 02. Its section 9 holds the results and what was not verified.
- K1 stops at the first rule that fails. A screen that must show every refusal together asks K1 once per group of fields, with stand-ins for the other fields (the cash entry review since 04, the reversal since 05). One call hid the date refusal behind "note required" (05, phone check 5).
- MainActivity shows a cover screen whenever its window loses focus. A Compose Dialog, AlertDialog, Popup, DropdownMenu or ModalBottomSheet opens a separate window, takes the focus, and the cover and the dialog then replace each other in a loop that blocks all input (07, phone check 2). Draw dialogs and menus inside the activity window, as PortfolioNameDialog in PortfoliosScreen.kt and the ⋮ menu in PortfolioScreen.kt do. The app draws edge to edge, so such a layer needs imePadding() or the keyboard covers it (08, phone check 9a). Unit tests cannot catch either; only the phone shows it.
- Test totals: add up the per-class counts from the test result files. Never add the new tests to the previous total: 03 to 06 were written one too high that way (corrected in pull request #93).
- Windows PowerShell 5.1 splits a native command's argument at embedded double quotes. For commit messages with quotes, write the message to a file in the scratchpad and use git commit -F <file>.
- The same splitting breaks jq filters passed to `gh … --jq`: a filter can hold no string literal, and single quotes are not jq strings. Select runs by ID or by `--commit <full SHA>` (a short SHA matched nothing), or put the filter in a file (avd-memory spike, 2026-11-18: two background waits found no run).
- GPG signing can time out when its pinentry window is not answered; the window often opens behind VS Code. Nothing is committed then, and the files stay staged. Ask me to be ready, then retry. Never bypass signing.
- WebFetch cannot read pages built by JavaScript (Finnhub pricing, Nasdaq Data Link) or compressed PDFs (Alpha Vantage terms), and some sites return 403 or 401 (FMP, Tiingo terms). Ask me to paste the text.
- In Windows PowerShell 5.1, Invoke-RestMethod returns the raw text, not an object, when the JSON has keys that differ only by case (Massive's "T" and "t"). Use Invoke-WebRequest and read .Content.
- A call that must run in a time window (market hours) checks the time itself and makes no call outside it. An approval is not a clock (price-source spike, first run at 20:03 +08:00).
- Tag a question Design only when every option changes spec.md. If one option stays within the spec, the question is Build, and that option comes first (price-source spike, decision 5).
- PowerShell's HttpWebRequest and Invoke-WebRequest follow redirects by default. When measuring an API the app calls, turn that off (AllowAutoRedirect = $false, or -MaximumRedirection 0), or a redirect goes unseen (09: Finnhub's US list, phone check 3).
- Finnhub answers /stock/symbol?exchange=US with a 302 to a signed link on static2.finnhub.io. FinnhubSource follows that one redirect, only over https to *.finnhub.io and without the key. Keep it that way: Android's HTTP stack would forward the X-Finnhub-Token header on its own redirect (my reading, not checked).
- The app's uid for a Logcat check: adb shell pm list packages -U app.ghghorg.atsassets (uid:10455, your app, the release since 38c, kept by 40's `adb install -r` of 1.0.0; it was 10454 as the debug build 45). Your app is not debuggable since 38c: `run-as` is refused, and a Logcat capture by uid is the check that remains. The development app has its own uid: read it the same way with app.ghghorg.atsassets.debug. A fresh install gets a new uid (10446 in 38a, 10448 since 38b step 5's rollback rehearsal), so read it before every capture. The plain name also matches the .debug id, so read the uid on the line of the exact id. dumpsys package has no userId line on Android 17 (09 check 9).
- Before a Logcat capture, run `adb devices` and check the phone is listed. A capture started with no phone connected waits ("- waiting for device -") and records nothing (27, check 19, first capture).
- A part of a screen that stays in place (a header over a list) must give way at the largest font and in landscape. Holding detail keeps its header fixed only while it takes at most half the screen's height, and otherwise scrolls as one (10, phone check 6a). Unit tests cannot catch it; only the phone shows it. Later screens with a fixed part, such as the sell screen's header (11), should do the same.
- Phone-check instructions: name the screen each check starts on and the exact control to tap (the Buy/Sell menu's Sell and a holding's Sell return to different places), and write each check from where the previous one leaves the phone and the data it changed (11, check 3).
- When a wireframe marker and spec.md's view text can be read two ways (27: marker 4 "opens it" against view 8 "a row opens the transaction"), say so when building, not only when asked at the phone check (27 decision 4).
- Reverse and re-enter puts the new entry after every entry of the original's date, so a split entered later that day applies to it: its first quantity and picks carry those splits (14a fix). Test with such a split; on the phone every trade of 2026-10-31 and 2026-11-01 has one.
- Phone-check figures: a lot's history is every sell that ever took from it, reversed ones included, and the date each was reversed. Read the lots used on each sell's detail before working out a reversal's date rule (14a check 3).
- Exact fractions at 8 places: test with remainders that round up as well as down, and with several lots (for example 2/3, 5/3, 3/7, 1/7), not only 10/3, which rounds down. A sum of shown figures can be above or below the total shown, and every rule that compares typed quantities must use the same figures the picker shows. Before proposing a fix, try each option on all the remaining checks and on such cases (12, phone check 6).
- A stand-in value (the K1 pitfall above) is only for judging rules together. Whatever a review shows must come from the typed values, so stand in only for a field that is itself refused, and test that the review shows each typed field, an empty one included (14b, phone check 3: the review showed the stand-in note "check").
- When a field's meaning changes or a second field takes over part of its job (as boughtQuantity did for a buy's quantity in 14c), search for every read of the old field, not for one call pattern, and check each screen that shows it on the phone by its text. No test draws a screen (14c Deploy findings 1 and 2).
- Room writes app/schemas/…/N.json with LF line endings, and git then warns "LF will be replaced by CRLF the next time Git touches it". That's expected: git stores every schema file as LF (`i/lf`). Don't convert the file. Before installing a build with a new schema version, compare N.json with the one before it (each table's SQL), and stop at the unreadable-data screen if it appears; there's no backup file until 31 (15, check 1).
- Commit message files: write them with `[IO.File]::WriteAllText(path, text, (New-Object Text.UTF8Encoding $false))`. Windows PowerShell's `Out-File` and `Set-Content -Encoding utf8` add a BOM, which lands in the commit message.
- `connectedDebugAndroidTest` can pass with no test run (plan/spikes/screen-tests.md 11.4, series 1, run 20). Count the `<testcase>` entries in the result XML and treat fewer than expected as red. The screen-tests workflow does that.
- Emulator tests: turn the screen only after the test's own activity is up. Before that, the launcher holds the screen upright (plan/spikes/screen-tests.md 11.4, series 3).
- The emulator on this PC (the AVD ats-spike-37 in D:\AndroidAvd, deleted 2026-11-06 with its image; the screen tests run on GitHub only) stops with `WHPX: Unexpected VP exit code 5`: 7 times on 2026-11-04 and 05, on emulator 37.2.12 and 37.2.13, with either GPU mode, and after a Windows restart. The cause wasn't found (plan/spikes/screen-tests.md 11.8). The adb server and the emulator's virtual GPU also stalled once each. Unattended local series aren't steady here; GitHub's runner was 10 of 10.
- In PowerShell, indexing a single line of adb output with `[0]` gives its first character. Wrap the command in `@()`.
- Start the emulator detached with `cmd /c`, with `ANDROID_AVD_HOME=D:\AndroidAvd` set for that process only (plan/spikes/screen-tests.md 11.3, S9).
- A crashed Gradle daemon writes `hs_err_pid*.log` (and `replay_pid*.log`) into the repo root. Never commit them; move them to the scratchpad.
- The v2 Compose test rules redraw only on `waitForIdle` or a node access, so a check that only sleeps sees no popup. Wait with `waitForIdle` in a loop (plan/spikes/screen-tests.md 11.4, commit 57c6150; my reading, not checked in the library source).
- In a screen test, a filter chip and a bottom-bar tab can share a text and both be selectable (in landscape the bar's "Cash" tab matched the Cash chip). Match a chip by its role as well: `SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)` (pull request #178, screen tests run 1; fixed in e0a6e19).
- Cold-boot the emulator (`-no-snapshot-load`) before a local series. After about 1 h it ran about twice as slow.
- Wait a few seconds after `adb emu kill` before starting the emulator again; a start right away failed (its log file is still held).
- `gh run download <run>` fetches every attempt's artifacts, each in its own folder (seen 2026-11-20 on runs 44676146597 and 44690476652, which earlier notes said kept only the newest). Count a re-run's results per folder, not all together, or the tests of both attempts are added up. Don't save a binary download with PowerShell's `>`, which rewrites it as text.
- The system's BiometricPrompt PIN pad is a secure window. `screencap` shows it black, and `findAccessibilityNodeInfosByViewId` misses its Compose keys, so walk the node tree instead (plan/spikes/screen-tests.md 11.6, commit 039611f).
- In a GitHub Actions workflow, `$SECONDS` restarts in each step. Carry times between steps as clock time (`date +%s`) through `GITHUB_ENV`.
- On GitHub's runner, avdmanager and the emulator need the same `ANDROID_AVD_HOME`, set explicitly (plan/spikes/screen-tests.md 11.6, run 1: the emulator found no AVD).
- Gradle's per-test logcat paths in the scratchpad can pass Windows' 260-character limit, and `Get-Content` then fails. Use the Grep tool, or the `\\?\` prefix with `[IO.File]::ReadAllLines`.
- The local full run sometimes fails in `:core:compileTestFixturesKotlin`: ScaleHistories.kt cannot resolve core's own classes, and Room's KSP then fails with "references a type that is not present". The code is not at fault. Cause not found: it is not the Kotlin daemon and not a reused configuration cache entry (23b file, 13.1, step 5). The task passes alone. Read the cause, then re-run the same command unchanged; `--continue` shows every task's result in one run. CI has not shown it.
- A full run that goes green right after an intermittent KSP or testFixtures failure (open item 60) once packaged an incomplete APK: classes5.dex was 330,260 bytes against 362,904, and the phone showed "Your data cannot be read" (29 file, 13.1, step 9). Before installing, compare the APK's size and each classesN.dex's size with the previous build, and for a step that changed code, check that a name the step added is in the dex. An equal APK size alone proves nothing: the correct build of 29 step 10 was the same size to the byte (29 file, 13.1, step 9).
- A screen test that turns MainActivity relaunches it. With no auto-lock choice stored (the emulator), its onStop locks the app, and the lock prompt takes the focus. After `turn`, call `passLockAfterTurn()` (ScreenTestSupport.kt), and don't turn back (29 file, 13.1, step 10 part 1b).
- In landscape the emulator's keyboard is much lower than the phone's (phone: about 354 dp of 527 dp). A keyboard test that only checks that a field is visible can pass on the emulator while the phone fails. Check the layer's room: its bottom at its margin above the keyboard's top (29 file, 13.1, step 10 parts 1c and 1d).
- In a unit test, `ZoneOffset.UTC` prints as "Z", not "UTC", in a shown zone (the last backup line). Use `ZoneId.of("UTC")` when the expected text says "UTC" (a red run each in 31 and 32).
- Something that shows on every Overall that holds data (32's backup reminder) also shows where a screen test draws Overall directly from a state, not only in MainActivity tests, and pushes the rest down. Scroll to an item (`performScrollTo`) before tapping it (32 file, 13.1, step 5).
- A layer drawn inside a screen, above the bottom bar, with `imePadding()` loses the bar's height as well as the keyboard's. Draw layers in AppContent over the bar, as Rename's is, and consume the navigation bars' inset before `imePadding` (29 file, 13.1, step 10 part 2; open item 63).
- `R` is a PowerShell alias (Invoke-History): a helper function or script named R runs the last command instead, without an error. Don't name a helper R (33b).
- After a Gradle daemon's JVM crash (hs_err_pid*.log in the repo root), run `.\gradlew.bat --stop` before re-running: in 33b step 3, after four crashes, the next daemon read a damaged path (`file:   .gradle/...`) until stopped (open item 60).
- A lazy list (LazyColumn) draws only the items in view: a screen test reaches an item with `performScrollToNode` on the list (`hasScrollToNodeAction()`), not with `performScrollTo` on the item (33b Deploy finding 2).
- The backup writer puts three `setting` rows (`auto_lock`, `price_source`, `tab_order`) right after the `format` row, so in a test that names a line of a written file the first record is line 6 (header 1, format 2, settings 3 to 5), not line 3 as in a hand-made file without settings (BackupFile.kt; 33b).
- A screen drawn in AppContent's box above the bottom bar with `imePadding()` pads by the whole keyboard, so the bar's and the navigation strip's heights are taken twice and its room ends that far above the keyboard's top. Measure the height under the screen and consume it before `imePadding()`, as RestoreScreen does (35 step 7, decision 4); the other entry screens are open item 69.
- A keyboard screen test must not read a scrolling area's bounds: they move with its scrolled content (35 step 7, run 44015745369: -222 px). Scroll to the end (the ScrollBy semantics action) and measure the last row against the keyboard's top less the screen's padding.
- A row read by TalkBack as one sentence (`clearAndSetSemantics { contentDescription = … }`) hides its children's texts, column heads included: a screen test finds such a row by `hasContentDescription`, not by a child's text (35 step 7, run 44015745369).
- R8 can move a library class away from a package-private parent: the release then crashes at start with `IllegalAccessError … is inaccessible`, before any screen (38b step 2: Kotlin's `CollectionsKt__MutableCollectionsJVMKt`). No unit or screen test runs R8's output. Keep rules go in app/src/main/keepRules/rules.keep (there is no proguard-rules.pro); the release class check (section 3) catches this kind in CI, not a hidden method or field. After changing R8 settings or keep rules, run the release code on the phone with test data under another id, as 38b's probe build did (`initWith(release)`, `.probe`, signed with the debug key), before a release uses it.
- `dexdump` on an APK prints no class: unzip its classes*.dex first (38b decision 6).
- A manual run (`workflow_dispatch`) of a workflow starts only once the file is on main (GitHub's workflow syntax page). Prove a new workflow on its pull request instead (38b decision 4a).
- A restore needs the price key saved first: the restore checks the backup's tickers (open item 66, AC-08). In phone-check steps on a fresh install, enter the key before the restore (38b step 2).
- A test that needs an earlier tag can tag a tree, not a commit (`git write-tree`, then `git tag <name> <tree>` in a scratch repo): `git show <tag>:<file>` reads it, and nothing is signed (38b step 3).
- The release hook (section 8) asks for every command that names `assembleRelease`, so a full run with the release build asks each time. Leave the release build out of a step's full run unless the step changed it.
- The release key (`ats-release.keystore`, alias `ats-release`, certificate `CN=ATS Asset System`, SHA-256 8115d618f042c3dd9f34ee7e19c91ef8c2eec8507ac0b9a915d86c3b1182cb01) is outside the repo; its password and folder never pass through this session. `keytool` and `apksigner sign` prompt for the password, which Claude Code's shell cannot answer: you run them in your own terminal (38c decision 2). Every later release's `apksigner verify --print-certs` must show this certificate SHA-256 (C21).
- apksigner under Java 25 prints four `WARNING: A restricted method in java.lang.System has been called …` lines before it works; they are harmless (38c steps 2 and 6).
- A release needs two pull requests: the version raise first, merged, so the tag sits on main's merge commit (AC-63); then the records of the release's steps (38c section 14).
- For a job that was re-run, `gh run view --job <id> --log` and `gh api …/actions/jobs/<id>/logs` give the newest attempt's log, even for attempt 1's job id (the latter also refuses to print escape codes). Read an earlier attempt with `gh run view <run> --attempt N --log` and keep the job's lines (39 step 3).
- In Windows PowerShell 5.1, `Invoke-WebRequest -MaximumRedirection 0` throws on a 3xx ("Operation is not valid due to the current state of the object") instead of showing it. To see a redirect, use `[Net.HttpWebRequest]` with `AllowAutoRedirect = $false` and read the status and `Location` (39 step 2: services.gradle.org answered 301 to downloads.gradle.org).

## 7. Standing checks for every iteration's screens
AC-09, AC-39 to AC-42, and the install-over-previous-build check (AC-35), all on the development app "ATS Dev" with test data (AC-65; 38a). Your daily app gets AC-35 and the smoke check (AC-64) at each release.

## 8. Working model in Claude Code (decided 2026-10-28; plan/spikes/vscode-claude-code.md section 5)
These are instructions, not enforced rules. The only technical guards are the permission rules in .claude/settings.json and my user settings. Do not change either without asking.
Start mode is Edit automatically.
- No asking: deciding which files to create or edit, creating, editing and saving them, staging (git add), reading files inside the repo, .\gradlew.bat build, test, lint and spotlessCheck, git status, git diff, git log, and copying files.
- Ask first: git commit, git merge, every git push, creating or merging pull requests, web search, web fetch, deleting files or folders, git commands that discard edits or branches (git rm, clean, reset, checkout --, restore, branch -D, stash drop), moving or renaming files, reading outside the repo, every edit of intent/intent.md and spec/spec.md, updating main from the remote (git pull) and creating a branch (git checkout -b), also right after a merge, and any other shell command.
- Never read: keystore.properties, signing.properties, *.jks, *.keystore, .env, .env.*.
- The release hook (38b step 4): a PreToolUse hook in .claude/settings.json runs .claude/hooks/release-ask.sh for every Bash and PowerShell call and makes it ask when the call names `adb … install` (uninstall included), `git … tag`, `gh release` or `assembleRelease`, `bundleRelease`, `packageRelease`, even where an allow rule matches. Its prompt shows "Release command (38b hook)". Proven 2026-11-21 with five calls answered No and a control that ran without a prompt.
- For web search and web fetch: show me the tool, the query or URL and the reason in your message before you call it.
- Never suggest, and never choose, a button that saves a rule or grants the rest of the session ("Yes, allow ... for this project" or "for this session"). I press Yes or No.
Gates (Ruleset A stays in force even though nothing technical blocks you): no code before an approved iteration file and plan.md; intent.md and spec.md change only with my acceptance.
How far to go:
- Work one numbered step of the iteration file's "Order of work" at a time. After each step run build, tests and lint, show the real output and the report below, and stop until I say go.
- Before each step run git status. If there are uncommitted changes you did not make, stop, list the files and wait.
- Report after creating or editing files: each file, the real git diff --stat output, and for documents the line count, byte count and SHA-256. Check that a CRLF document did not change its line endings: count the raw CR and LF bytes and run git diff --check (if a whole file shows as changed, stop). The report commands ask for my approval, about two prompts per report; that is expected.
Commits and pull requests:
- Propose one commit per step, after build, tests and lint are green. Show the message and the changed files; I approve each. Message: "Iteration NN X: what", naming the spec refs. A red step gets no commit. Keep the default commit trailer.
- Never push. I push, and I create and merge pull requests (GitHub Pull Requests extension or browser). Draft the title and description for me.
Other:
- Start sub-agents only when I ask.
- Ask one question at a time, tagged with a stage, with the options, what each means for the end user, and your leaning marked. Each option starts with a short letter tag (A, B, C).
- For a Build choice between ways to write the code, weigh my leaning by three criteria and show them for each option: code maintainability, test run time, and APK size. Say when an option makes no real difference on a criterion, and give the measured or expected size of the difference. The leaning follows from these, plus what the option means for you as the user (decided 2026-11-03, 15a decision 1).
- Do not write security-relevant code from memory. Look up versions on the day. When a request conflicts with intent.md or spec.md, stop and offer a choice.
- Wording that spec.md does not give (messages, labels) is written by you, shown to me and approved before use (D9).
- Auto memory is off. If I correct you, propose the change as a CLAUDE.md edit.
