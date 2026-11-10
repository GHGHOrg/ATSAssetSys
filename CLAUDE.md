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
- Test:   .\gradlew.bat :core:test :app:testDebugUnitTest
- Lint:   .\gradlew.bat :app:lintDebug
- Style:  .\gradlew.bat spotlessCheck (read-only; CI runs it first). .\gradlew.bat spotlessApply rewrites files: run it only when asked, and show the diff first.
- All three, forced fresh: add --rerun-tasks (use it for lint too, see pitfalls)
- Install on the phone: & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
  (adb is not on PATH on this machine. The APK path is confirmed. The install has been run on the phone:
  setup proof item 4 (2026-10-18), iteration 01 (2026-10-19) and the dependency-upgrade spike (2026-10-22).)
CI (.github/workflows/ci.yml) runs the style check and then the same three steps on ubuntu-26.04 with Java 25 (Temurin).

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
- A red screen-tests run is not merged. Read its cause before a re-run: the run's artifact holds the results, the emulator log and any unlock screenshots, and logcat holds the ScreenTestUnlock lines. Each re-run needs your yes.
- A branch that is never merged (defects brought back, experiments) is named throw-away-NN-what, and its pull request is a draft that is closed unmerged (15b step 4).
- Refer to a record by its file and section, not by a shorthand name: "plan/spikes/screen-tests.md 11.4, series 3", not "spike F, series 3", and the same for decisions and runs. A shorthand like "spike F" or "your B" means nothing to a later reader (15b step 3).
- When you edit a document that has a table of contents at the top (spec.md, plan.md, CLAUDE.md), update the table too: add, rename or remove the entry for each heading you add, rename or remove, and check each link against its heading's anchor.

## 5. Where things are
- intent/intent.md (v52, final) and one file per change request (intent/intent-cr-NN-short-name.md; CR1 is intent-cr-01-tiny-remainders.md, CR2 intent-cr-02-screen-models.md, CR3 intent-cr-03-screen-tests.md), spec/spec.md (v6, final, one file; acceptance criteria tagged AC-NN, code comments may still say "criterion NN"), spec/wireframes/, plan/plan.md, plan/setup.md.
- Spec sections: find them by heading, for example "## 4. Business rules".
  Sections 2 (concerns, C-numbers), 3 (domain), 4 (rules), 6 (views), 7 (prices), 8 (backup file), 9 (security), 11 (criteria), 14 (components, K-numbers).
- Each iteration file in plan/iterations/ lists its own spec sections, criteria and examples.
- Adapter code, the only code that knows an outside service or library's own API, sits in its own folder under app/src/main/java/app/ghghorg/atsassets/: prices/finnhub/ for Finnhub (K4: its URLs, the X-Finnhub-Token header, its fields) and data/ for Room and SQLCipher (K2). Outside prices/finnhub/, only MainActivity names Finnhub, where it builds the source with the key (and app/build.gradle.kts, which reads FINNHUB_API_KEY); the rest of the app uses core's PriceSource and RecordStore (16a, step 2).

## 6. Known pitfalls
- No floating point for money or quantity (4.10).
- Quantity.shown is the only place a quantity is rounded for showing, picking or judging; a quantity above 0 never shows 0 (CR1, spec 4.10). Do not round a quantity with Fraction.rounded directly.
- A buy's exact quantity is boughtQuantity (quantity × quantityRatio), not quantity: a buy re-entered with its quantity left as filled in stores the original's quantity and a ratio in the split columns (CR1). Use boughtQuantity wherever a buy's shares, cost or price are worked out or shown (14c, section 14, item 1).
- Never log or send amounts, tickers, quantities, notes or portfolio names (spec section 9). Never log the price-source key either: no HTTP logging, not even in debug builds; the key goes in the X-Finnhub-Token header only, never in a URL (price-source note, decision 5).
- Never print FINNHUB_API_KEY or anything derived from it, not even its length: test only `-not [string]::IsNullOrEmpty($env:FINNHUB_API_KEY)` (True/False) (16a).
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
- In PowerShell, an ordered table (`[ordered]@{}`) with number keys is indexed by position: `$t[1]` is the second entry, not key 1. Loop over a plain list of entries instead. When building held steps' commits through a temporary index, compare each commit's `git diff --cached --stat` with its step's report before committing (21: five commits got the next step's files and were reset with your yes).
- The CI runner is pinned to ubuntu-26.04 (moved from ubuntu-24.04 in the dependency-upgrade spike; CI green). Do not use ubuntu-latest.
- Lint shows 5 findings, all listed on purpose, none silenced: AndroidGradlePluginVersion (AGP 9.5.1 available, gradle/libs.versions.toml:2; open item 31, since 06), NewerVersionAvailable for kotlinx-coroutines-test 1.9.0 (gradle/libs.versions.toml:17), NewerVersionAvailable for sqlcipher-android 4.19.1 (4.20.0 available, gradle/libs.versions.toml:15; open item 43, since 12), NewerVersionAvailable for com.diffplug.spotless 8.10.4 (8.11.0 available, gradle/libs.versions.toml:12; since 15b) and TypographyDashes on the date_not_a_date string in strings.xml (its line moves as strings are added; 04 saw :58, 15b :86) (the hyphens in the YYYY-MM-DD example). Do not silence a new notice: list it.
- Style checker: Spotless 8.10.4 with ktlint 1.8.0, targets set by hand in the root build.gradle.kts. @Composable functions are exempt from ktlint's function-naming rule through an override there; the property name is supported only by its effect.
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
- GPG signing can time out when its pinentry window is not answered; the window often opens behind VS Code. Nothing is committed then, and the files stay staged. Ask me to be ready, then retry. Never bypass signing.
- WebFetch cannot read pages built by JavaScript (Finnhub pricing, Nasdaq Data Link) or compressed PDFs (Alpha Vantage terms), and some sites return 403 or 401 (FMP, Tiingo terms). Ask me to paste the text.
- In Windows PowerShell 5.1, Invoke-RestMethod returns the raw text, not an object, when the JSON has keys that differ only by case (Massive's "T" and "t"). Use Invoke-WebRequest and read .Content.
- A call that must run in a time window (market hours) checks the time itself and makes no call outside it. An approval is not a clock (price-source spike, first run at 20:03 +08:00).
- Tag a question Design only when every option changes spec.md. If one option stays within the spec, the question is Build, and that option comes first (price-source spike, decision 5).
- PowerShell's HttpWebRequest and Invoke-WebRequest follow redirects by default. When measuring an API the app calls, turn that off (AllowAutoRedirect = $false, or -MaximumRedirection 0), or a redirect goes unseen (09: Finnhub's US list, phone check 3).
- Finnhub answers /stock/symbol?exchange=US with a 302 to a signed link on static2.finnhub.io. FinnhubSource follows that one redirect, only over https to *.finnhub.io and without the key. Keep it that way: Android's HTTP stack would forward the X-Finnhub-Token header on its own redirect (my reading, not checked).
- The app's uid for a Logcat check: adb shell pm list packages -U app.ghghorg.atsassets (uid:10454). dumpsys package has no userId line on Android 17 (09 check 9).
- A part of a screen that stays in place (a header over a list) must give way at the largest font and in landscape. Holding detail keeps its header fixed only while it takes at most half the screen's height, and otherwise scrolls as one (10, phone check 6a). Unit tests cannot catch it; only the phone shows it. Later screens with a fixed part, such as the sell screen's header (11), should do the same.
- Phone-check instructions: name the screen each check starts on and the exact control to tap (the Buy/Sell menu's Sell and a holding's Sell return to different places), and write each check from where the previous one leaves the phone and the data it changed (11, check 3).
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
- Cold-boot the emulator (`-no-snapshot-load`) before a local series. After about 1 h it ran about twice as slow.
- Wait a few seconds after `adb emu kill` before starting the emulator again; a start right away failed (its log file is still held).
- `gh run download` finds only the newest attempt's artifact. Download each attempt's results before the next re-run. Don't save a binary download with PowerShell's `>`, which rewrites it as text.
- The system's BiometricPrompt PIN pad is a secure window. `screencap` shows it black, and `findAccessibilityNodeInfosByViewId` misses its Compose keys, so walk the node tree instead (plan/spikes/screen-tests.md 11.6, commit 039611f).
- In a GitHub Actions workflow, `$SECONDS` restarts in each step. Carry times between steps as clock time (`date +%s`) through `GITHUB_ENV`.
- On GitHub's runner, avdmanager and the emulator need the same `ANDROID_AVD_HOME`, set explicitly (plan/spikes/screen-tests.md 11.6, run 1: the emulator found no AVD).
- Gradle's per-test logcat paths in the scratchpad can pass Windows' 260-character limit, and `Get-Content` then fails. Use the Grep tool, or the `\\?\` prefix with `[IO.File]::ReadAllLines`.

## 7. Standing checks for every iteration's screens
AC-09, AC-39 to AC-42, and the install-over-previous-build check (AC-35).

## 8. Working model in Claude Code (decided 2026-10-28; plan/spikes/vscode-claude-code.md section 5)
These are instructions, not enforced rules. The only technical guards are the permission rules in .claude/settings.json and my user settings. Do not change either without asking.
Start mode is Edit automatically.
- No asking: deciding which files to create or edit, creating, editing and saving them, staging (git add), reading files inside the repo, .\gradlew.bat build, test, lint and spotlessCheck, git status, git diff, git log, and copying files.
- Ask first: git commit, git merge, every git push, creating or merging pull requests, web search, web fetch, deleting files or folders, git commands that discard edits or branches (git rm, clean, reset, checkout --, restore, branch -D, stash drop), moving or renaming files, reading outside the repo, every edit of intent/intent.md and spec/spec.md, and any other shell command.
- Never read: keystore.properties, signing.properties, *.jks, *.keystore, .env, .env.*.
- For web search and web fetch: show me the tool, the query or URL and the reason in your message before you call it.
- Never suggest, and never choose, a button that saves a rule or grants the rest of the session ("Yes, allow ... for this project" or "for this session"). I press Yes or No.
Gates (Ruleset A stays in force even though nothing technical blocks you): no code before an approved iteration file and plan.md; intent.md and spec.md change only with my acceptance.
How far to go:
- Work one numbered step of the iteration file's "Order of work" at a time. After each step run build, tests and lint, show the real output and the report below, and stop until I say go.
- Before each step run git status. If there are uncommitted changes you did not make, stop, list the files and wait.
- At the end of every iteration (its documents step), remind me of open item 33: the phone check of "Set the opening balance first" for New portfolio, deferred to 34 (09 decision 9). Drop this line when item 33 is closed.
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
