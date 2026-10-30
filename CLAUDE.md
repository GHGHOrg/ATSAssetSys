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

## 5. Where things are
- intent/intent.md (v52, final), spec/spec.md (v4, final, one file), spec/wireframes/, plan/plan.md, plan/setup.md.
- Spec sections: find them by heading, for example "## 4. Business rules".
  Sections 2 (concerns, C-numbers), 3 (domain), 4 (rules), 6 (views), 7 (prices), 8 (backup file), 9 (security), 11 (criteria), 14 (components, K-numbers).
- Each iteration file in plan/iterations/ lists its own spec sections, criteria and examples.

## 6. Known pitfalls
- No floating point for money or quantity (4.10).
- Never log or send amounts, tickers, quantities, notes or portfolio names (spec section 9). Never log the price-source key either: no HTTP logging, not even in debug builds; the key goes in the X-Finnhub-Token header only, never in a URL (price-source note, decision 5).
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
- The CI runner is pinned to ubuntu-26.04 (moved from ubuntu-24.04 in the dependency-upgrade spike; CI green). Do not use ubuntu-latest.
- Lint shows 3 findings, all listed on purpose, none silenced: AndroidGradlePluginVersion (AGP 9.5.0 available, gradle/libs.versions.toml:2; open item 31, since 06), NewerVersionAvailable for kotlinx-coroutines-test 1.9.0 (gradle/libs.versions.toml:17) and TypographyDashes on the date_not_a_date string in strings.xml (its line moves as strings are added; 04 saw :58) (the hyphens in the YYYY-MM-DD example). Do not silence a new notice: list it.
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

## 7. Standing checks for every iteration's screens
Criteria 9, 39 to 42, and the install-over-previous-build check (35).

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
- At the end of every iteration (its documents step), remind me of open item 33: the phone check of "Set the opening balance first" for New portfolio, due in 09, at the latest in 34. Drop this line when item 33 is closed.
- Report after creating or editing files: each file, the real git diff --stat output, and for documents the line count, byte count and SHA-256. Check that a CRLF document did not change its line endings: count the raw CR and LF bytes and run git diff --check (if a whole file shows as changed, stop). The report commands ask for my approval, about two prompts per report; that is expected.
Commits and pull requests:
- Propose one commit per step, after build, tests and lint are green. Show the message and the changed files; I approve each. Message: "Iteration NN X: what", naming the spec refs. A red step gets no commit. Keep the default commit trailer.
- Never push. I push, and I create and merge pull requests (GitHub Pull Requests extension or browser). Draft the title and description for me.
Other:
- Start sub-agents only when I ask.
- Ask one question at a time, tagged with a stage, with the options, what each means for the end user, and your leaning marked.
- Do not write security-relevant code from memory. Look up versions on the day. When a request conflicts with intent.md or spec.md, stop and offer a choice.
- Wording that spec.md does not give (messages, labels) is written by you, shown to me and approved before use (D9).
- Auto memory is off. If I correct you, propose the change as a CLAUDE.md edit.
