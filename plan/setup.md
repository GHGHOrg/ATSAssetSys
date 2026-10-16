# plan/setup.md (DRAFT, all decisions merged)

Stage: Build. Status: draft, not approved. Approved together with plan/plan.md.
Source: intent/intent.md v52 (final), spec/spec.md v4 (final), sections 9 and 14, C17.
Nothing here has been run. Tool and library versions are NOT chosen in this draft: they
are looked up and pinned on the day setup runs, never taken from memory.

## 1. Outcome
Not a user feature (a prerequisite, not an iteration). When this file is done, the
repository holds an empty Android project that:
- builds, runs its tests and passes lint, locally and in CI, with real output shown;
- installs on your phone and starts to a blank screen;
- has a safe manifest baseline (no automatic backup of app data);
- has CLAUDE.md in the repo root, so iteration 01 starts from rules, not from memory.
No screen, record, network call, storage or encryption is built here.

## 2. What exists today (to check first, not assumed)
Seen in project context: README.md, .github/dependabot.yml, intent/, spec/,
spec/wireframes/. Whether the GitHub repo exists, its default branch and what is
committed must be confirmed by you or by listing the repo.
Known mismatches to settle during setup:
- dependabot.yml targets `npm`; the chosen stack is Gradle.
- Repo-root README.md describes the app as if it were built (full pass: hunks R6 onward).
- spec/example-import.csv is stale. You remove it yourself.

## 3. Stack (decided)
- Language and UI: Kotlin, Jetpack Compose (S1, approved).
- Build: Gradle with Kotlin DSL and a version catalog for pinned versions.
- Modules (S2, approved): two to start, more added by the iteration that needs them.
  - `core/`: K1 rules engine and the K2 interface. Pure Kotlin/JVM, no Android classes
    (section 14, dependency rule 3). Exact arithmetic with arbitrary-precision integers (4.10).
  - `app/`: the Android application (K2 implementation, price service, lock, screens, wiring).
- Tests: JUnit for `core/` and `app/`. Device checks are by hand on your phone
  (criteria 18, 20, 35 to 58 are "checked by hand").
- Lint and format: Android lint plus one Kotlin style checker, chosen when setup runs
  after checking what is current.
- minSdk 33 (Android 13, spec section 9). Tested on your phone, Android 17.
- Data layer (decided: Room on SQLite, encrypted with SQLCipher):
  - K2 is an interface in `core/` (load all records, save an atomic batch, replace all
    for restore, read and write instrument data). `core/` never sees databases, keys or
    Android classes. Room and SQLCipher live in `app/`.
  - The database passphrase is random, generated on the phone and stored wrapped by a
    Keystore key (non-exportable, not tied to a biometric, section 9, C18). Neither is
    in the backup file.
  - A record and its cash line, a Reverse and re-enter pair, and a restore each run in
    one SQLite transaction (section 14, K2).
  - Records and prices are separate tables. A price refresh writes only instrument rows,
    one transaction per batch, and never touches transaction records. Lost instrument
    data is refetched; holdings show "no price" meanwhile (4.8).
  - Derived state (lots, holdings, balances) is not stored. It is rebuilt by replay (4.1),
    cached in memory and recalculated only when a record changes.
  - Migrations: Room schema versions, exported schemas committed (criterion 35).
  - The database file lives where Android's automatic backup does not reach
    (`getNoBackupFilesDir()`, in addition to the manifest rules; criterion 37).
  - Checks on the phone: criterion 36 and the update, reinstall and key-loss cases.
  - Costs accepted: a native third-party library and a dependency to keep updated.
  - Unchecked, to confirm in iteration 02: SQLCipher for Android's maintenance, licence
    and Room integration; Room's current version; native-library page-size alignment
    rules (from memory, unverified); which crypto API wraps the passphrase.
- Design source (decided: Figma for design tokens only):
  - Behaviour, layout and rules stay in spec.md and spec/wireframes. Figma never defines
    behaviour, rules or message wording (D9).
  - Figma supplies only the visual layer that section 6 leaves open: light and dark
    colors (including gain green and loss red), typography and spacing.
  - Setup adds no Figma tooling. The first token handoff happens in iteration 01.
  - The Compose theme file in `app/` is the source of truth once tokens are handed over.
    Nothing flows from the code back to Figma.
  - Copy by hand or export with a tool: decided in iteration 01 after checking what is
    current. No tool is chosen now.
  - No real amounts, tickers, notes or portfolio names in Figma files (section 9). No
    Figma token or file link in CI or the repository.
  - Contrast (4.5 to 1 text, 3 to 1 large text and outlines) is checked in both themes on
    the phone (criteria 40 and 41), whatever Figma shows.
- Not decided here, on purpose: the price source and HTTP client
  (plan/spikes/price-source.md, then iteration 09).

## 4. Repository layout
```
/
  CLAUDE.md
  README.md
  .gitignore
  .github/
    dependabot.yml             ecosystem changed to gradle
    workflows/ci.yml
  intent/  spec/  plan/        existing and planned documents, unchanged
  core/                        K1 and the K2 interface, pure Kotlin/JVM
  app/                         Android application (K2 implementation: Room and SQLCipher)
  gradle/  settings.gradle.kts  build.gradle.kts
```
`app/schemas/` appears with the first Room database (iteration 02), not in setup.

## 5. Steps (order of work)
1. Confirm the repo state (section 2). Stop and tell you if it differs.
2. Create branch `setup`. All setup work lands in one reviewed change.
3. Write `.gitignore` first, before any generated file exists (section 6).
4. Create the Gradle project with `core/` and `app/`, pinned versions. No Room or
   SQLCipher dependency yet; iteration 02 adds them with the first table.
5. Manifest baseline (S4, approved): minSdk 33; `allowBackup="false"`;
   `dataExtractionRules` pointing at a rules file that excludes all data in both
   `<cloud-backup>` and `<device-transfer>`; no analytics or crash-reporting library;
   release build not debuggable. The rules file is generic now. Iteration 02 verifies and
   tightens it against the real database location (criterion 37). Exact attributes are
   checked against Android's current pages when written.
6. One placeholder test in `core/` (S3, approved), named and commented as a placeholder,
   only to show that a test is found, run and reported. Iteration 02 replaces it with
   real K1 tests. CLAUDE.md says so, so replacing it is not "weakening or deleting a
   test" (Ruleset A rule 5).
7. Lint and style configuration, zero warnings accepted at the start.
8. CI workflow (section 7).
9. Change `dependabot.yml` to the Gradle ecosystem. Updates stay weekly, no secrets.
10. Write CLAUDE.md (section 8) with the real commands from steps 4 to 7.
11. Run the proof (section 10), show real output, then ask you to review the change.

## 6. .gitignore and key handling (C17)
- Ignore: build outputs, IDE files, `local.properties`, `*.jks`, `*.keystore`, any
  `*.properties` holding signing passwords.
- Debug builds use the default debug key on your machine.
- The release signing key is created by you, kept OUTSIDE the repo and backed up in at
  least two places. A lost key blocks updates over an installed build (C17, criterion 35).
- CI never receives the release key. Release builds belong to Deploy.
- The install route (limited-distribution account, ADB, advanced flow) is chosen later.
  Setup only proves ADB install works on your phone.

## 7. CI
- GitHub Actions, one workflow, on pull requests and pushes to the default branch.
- Steps: build, unit tests, lint. Same Gradle commands as CLAUDE.md.
- No secrets, no signing, no network calls from tests.
- Runner image, JDK and Android SDK versions are chosen and pinned when written, from
  current documentation (unverified now).

## 8. CLAUDE.md contents (outline)
1. Stage rules (Ruleset A): say the stage first; no code before an approved plan.md;
   iteration files approved before their code; update plan.md in the same commit when the
   code departs from it; run build, tests and lint and show real output before saying
   done; a bug fix starts with a failing test; never weaken or delete a test (the setup
   placeholder test is the one documented exception: iteration 02 replaces it).
2. Commands: build, test, lint, install on phone (filled from step 10).
3. Conventions: one branch per iteration (`iter-NN-name`), one pull request per
   iteration; commit messages name the iteration and spec refs; every write to
   transactions or cash goes through K1 (section 14, rule 1); boundaries beyond
   `core/` and `app/` (K3 and K9 read only, K4 writes only prices) are kept by review
   and checked at Deploy.
4. Pointer map: spec.md stays one file (S86). Section starts for 2, 3, 4, 6, 7, 8, 9,
   11, 14 and the C-, K- and criteria numbering. Each iteration file lists its own spec
   sections, criteria and examples.
5. Known pitfalls:
   - no floating point for money or quantity (4.10);
   - never log or send amounts, tickers, quantities, notes or portfolio names (section 9);
   - no analytics or crash-reporting library;
   - docs in this repo use CRLF: keep line endings, do not retype files;
   - `[UNVERIFIED]` items in spec.md are not facts: check on the phone before relying;
   - message wording is D9: decided on the phone, not on paper;
   - the theme file in `app/` is the source of truth for colors and type; Figma is only
     the starting point and never overrides the spec.
6. Standing checks on every iteration's screens: criteria 9, 39 to 42, and the
   install-over-previous-build check (35).

## 9. Risks
- Tool versions and the Android SDK setup are unchecked until setup runs.
- Manifest backup attributes behave differently across devices and Android versions
  (section 9, partly checked). Only a phone test shows the truth (criterion 37).
- ADB install may need developer options on your phone. Not tested.
- SQLCipher is a native third-party dependency: maintenance, licence, build size and
  alignment rules are unchecked. If unsuitable, the K2 interface allows another
  implementation without touching `core/`.
- A lost passphrase or key makes the data unreadable. Mitigated by the backup file as
  the only recovery (section 8, C18) and the reminder banner (section 9).
- Figma and the theme file can drift apart. Accepted: the code is the source of truth,
  and criteria 40 and 41 are checked on the phone.
- A module layout decided now may need changing when K2 to K6 arrive. Kept to two
  modules to keep that cheap.
- The HTTP client is not chosen until the price-source spike. Low risk: the price
  service sits behind an interface (section 7).

## 10. Proof (shown with real output)
1. `build`, `test` and `lint` commands from CLAUDE.md run locally, output pasted. The test
   report shows 1 test, so a vacuous pass is visible.
2. The same three steps green in a CI run, with its link.
3. The merged manifest read back, showing minSdk, `allowBackup`, `dataExtractionRules`
   and the non-debuggable release setting.
4. The debug app installed on your phone by ADB and started to a blank screen, confirmed
   by you. This is the first on-device check; nothing earlier was verified.
5. `git status` shows no key, properties or build output tracked.
6. CLAUDE.md read back against section 8.
Anything that could not be run is stated as not run.

## 11. Out of scope
Any screen, stored record, database, encryption, lock, price or network code, release
signing, the install route choice, any Figma tooling or token export, the master plan
and the iteration files.

## 12. Decisions (all resolved)
S1 stack: native Kotlin, Compose, Gradle. S2 layout: `core/` and `app/`. S3 smoke test:
one labelled placeholder. S4 manifest baseline: in setup. S5 data layer: Room with
SQLCipher. Design source: Figma for tokens only.
