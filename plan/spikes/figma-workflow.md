# plan/spikes/figma-workflow.md

Stage: Build. Status: written 2026-11-17; discovery done the same day (section 0); approved by you 2026-11-18 as written, before the look-ups (section 5), which may still correct sections 4, 5 and 8. Nothing in it has run.
Source: your request of 2026-11-17 (four points), your choice C (how far Figma reaches is decided by this spike), your answers in the discovery (section 0), five web searches on 2026-11-17, plan/setup.md section 3 (design source), spec/spec.md section 6, CLAUDE.md, open item 18, the theme files in app/ (read 2026-11-17).
Section 5's look-ups are not done yet. Every claim about Figma, its MCP server, Code Connect or a plugin in sections 4, 5 and 8 is "my reading" until section 5 is filled.

How sources are labelled in this note:
- "I fetched": page text returned by my own search or fetch in this session, with the date.
- "you pasted": text you pasted (pages my tools cannot read).
- "your answer": your answer to a discovery question (section 0.3).
- "my reading": my interpretation. Not a fact until a look-up or the run shows it.
- "not checked": not looked at.

## Table of contents
- [0. Discovery](#0-discovery)
- [1. Outcome](#1-outcome)
- [2. Gate](#2-gate)
- [3. Where things stand](#3-where-things-stand)
- [4. Three approaches](#4-three-approaches)
- [5. Questions to look up](#5-questions-to-look-up)
- [6. Criteria for the choice](#6-criteria-for-the-choice)
- [7. Decisions](#7-decisions)
- [8. The run](#8-the-run)
- [9. Results](#9-results)
- [10. Cleanup and follow-up](#10-cleanup-and-follow-up)

## 0. Discovery
Done 2026-11-17 to sharpen sections 1 and 6 (your idea); the other sections follow from them. Three sources: the repo, outside practice (my five searches and the four practices you pasted), and your answers; the repo and outside practice are of equal weight (your B: the repo's way so far is not assumed to be the best).

### 0.1 The repo's history (read)
- R1: UI defects showed only on the phone: dialogs taking the window's focus (07), the keyboard covering a layer or field (08, 29, 35), a fixed header at the largest font in landscape (10, 11). Their fixes were partly visual (where things sit) and partly mechanism (insets, windows).
- R2: wording is decided on the phone (D9); the gain/loss and slice colors were worked out by hand for contrast (11, 18).
- R3: a screen shows only what its screen model prepares (CR2); screen tests run on every pull request (AC-61).

### 0.2 Outside practice and trends (I fetched, 2026-11-17; five searches, mostly vendor or practitioner blogs, not standards)
- P1 Fidelity: a handoff is judged by the gap between the design and what is built, and by the back-and-forth it takes; a falling count of design QA findings is the usual proxy (app.uxcel.com, design-development handoff evaluation).
- P2 One source of truth with a chosen sync direction (Figma to code, code to Figma, or both), each platform's output generated from it, and a naming contract between Figma names and code names (designsystemscollective.com, the 2025/2026 Figma Variables playbook; levelup.gitconnected.com, Figma-to-code token pipeline; figma.com resource library, design tokens).
- P3 AI agents read structured design data rather than screenshots; their output still needs review; the 2026 trend is two-way (design to code and code to design), and teams with tokens and named components first gain the most (blog.bytebytego.com; engineering.monday.com; chatprd.ai on the Figma team's workflow; figma.com resource library, MCP for designers).
- P4 Raw generated output is graded before any hand-fixing, for example on accessibility (kunalganglani.com, design-to-code tools benchmark 2026).
- P5 Drift (the shipped UI moving away from the design) is caught by a report-only comparison of Figma tokens with code tokens after each Figma change; review is tiered: automated checks for objective rules, people for judgment (magicpatterns.com, design system maintenance; designsystemproblems.pages.dev, design system code review).
- P6 Android: practice points to a generated Material 3 theme instead of hex values copied by hand; no authoritative 2026 guidance on a Compose handoff was found (urlaunched.com, managing a design system in Android apps).

### 0.3 Your answers (2026-11-17, each Build)
- D1 How often (your B): regular screen work; most iterations add or change a screen, so the handoff runs about weekly.
- D2 Who owns the design (your model, from option C): Figma is the one source of truth. A fix found on the phone is made in Figma by Claude Code, you approve it in Figma, and the approved design flows to code. Of the four kinds of fix (table below), kinds 1 and 2 and the visible result of kind 3 go through Figma; kind 3's mechanism and kind 4 stay in code and spec.
- D3 Kind 3's states (your A): each screen in Figma has its largest-font, landscape and keyboard-up frames as well as the normal one.
- D4 Seamless (your A): after your approval in Figma, one prompt to Claude Code brings the change into code; nothing is exported, copied or moved by hand. Time is measured, not limited.
- D5 Weighing (your A): hard rules first, then the ranked criteria with fidelity first (section 6).

| Kind of fix | Examples in the repo | Through Figma? |
|---|---|---|
| 1. Visual values: color, dimensions, spacing, type | gain/loss and slice colors for contrast (11, 18) | yes |
| 2. Placement: where components sit, what is fixed and what scrolls | Holding detail's header fixed only up to half the screen (10); layers over the bottom bar (29) | yes |
| 3. Behaviour under conditions: keyboard, largest font, landscape, focus | the restore field under the keyboard (35); dialogs taking focus (07) | the visible result yes (D3); the mechanism (insets, windows) no |
| 4. Wording | every message (D9) | no |

### 0.4 Practices you pasted (2026-11-17), translated to Android, and your answers
- P7 Component docs and live playgrounds (Storybook-like): design variants map to live code stories where states are checked without the whole app. Android: Compose previews (shown in Android Studio, not VS Code; my reading) or screenshot tests rendered on the JVM (my reading: Roborazzi or Paparazzi). D6 (your A): screenshot tests are the playground; every component variant and screen state has one, run in CI and locally, and its images are compared with Figma's frames.
- P8 State coverage beyond the happy path: empty, error, network failure, long strings and loading are designed before code. The spec already names many of these per screen (AC-39 to AC-42, the wireframes' empty states and blocked messages). D7 (your A): every state the spec names for a screen has a frame in the normal condition, and the main state also has the three condition frames (D3); the screenshot tests cover the combinations.
- P9 Context-first handoff: the "why" travels with the design. Here the why lives in intent.md and spec.md, and setup.md forbids Figma links in the repo. D8 (your A): each frame is named by its spec view and state (for example "V06 Holding detail / empty") and carries an annotation with its spec sections and criteria; Claude Code's handoff reads the frame, then those spec parts, and states the criteria before writing code. Figma never holds the reasons.
- P10 Accessibility and token contracts in CI. D9 (your A): three gates, hard rule H8: a contrast test of every token pair in light and dark (WCAG 4.5:1 text, 3:1 large text and outlines; exact, pure JVM); a token contract test (the theme matches the approved token source); Compose's accessibility checks (touch targets, labels) in the screen tests. Findings on existing screens are recorded as open items, never silenced. The phone check stays.

## 1. Outcome
An Android native UI workflow for this repo that works, shown by a run. It passes when every point holds:
1. Figma is the one source of truth for the UI's look and placement (D2). A design change starts in Figma, and a fix found on the phone is made in Figma, not only in code.
2. What Figma gives is for Android native UI (your rule): Android frames, Material 3 components, color roles and type styles, dp and sp; code reaches the app only as Jetpack Compose with Material 3.
3. Each screen in Figma has a frame for every state the spec names for it (empty, error or refused, loading, offline or stale, long strings), and its main state also has largest-font, landscape and keyboard-up frames (D3, D7). Each frame is named by its spec view and state and annotated with its spec refs (D8).
4. The IDE stays VS Code with the Claude Code extension; no second IDE is needed.
5. Claude Code reads the approved design from Figma, or from a repo file Figma produced, and can make a fix in Figma (D2). No value is retyped.
6. Seamless (D4): after your approval in Figma, one prompt to Claude Code brings the change into code, with no export, copy or file move by hand. The time from your approval to the phone is measured and reported.
7. Nothing reaches the app unapproved: your approval in Figma, then your commit approval, as today.
8. The phone matches the approved Figma frames (fidelity, P1), and differences between Figma and code are reported (drift, P5).
9. Every component variant and screen state can be seen without the phone or the app, in VS Code: screenshot tests render them on the JVM, locally and in CI, and their images are compared with the frames (D6).
10. The handoff carries the context: Claude Code reads the frame's spec refs and states the criteria before writing code (D8).
11. Contrast, the token contract and Compose's accessibility checks run as pull-request gates (D9).
The spike proposes three approaches (section 4), tries them, and ends with your choice of a level (decision F5), judged by section 6.

## 2. Gate
This is a decision note, not an iteration. It gives no app feature, and no app code from it is merged.
It blocks nothing in iterations 32 to 35. The Figma part of open item 18 (the first token handoff) moves here.
Branch: spike-figma-workflow (from main at 9d952cb), one pull request, merged by you.
Trial code goes only on throw-away branches (CLAUDE.md section 4): each has a draft pull request that is closed unmerged.
Order: discovery (section 0, done), the look-ups (section 5), the note's approval, decisions F1 to F4 and F6, the run (section 8), then F5.
Level 3, if chosen, needs a change request (intent/intent-cr-04-...) and a spec.md section 6 edit, accepted by you at the Design gate, before any Build work.

## 3. Where things stand
- plan/setup.md section 3, "Design source": Figma is for design tokens only (light and dark colors including gain green and loss red, typography, spacing). Behaviour, layout and rules stay in spec.md and spec/wireframes. Figma never decides wording (D9). The Compose theme file is the source of truth once tokens are handed over. Nothing flows from the code back to Figma. No real amounts, tickers, notes or portfolio names in Figma files (spec section 9). No Figma token or file link in CI or the repository. Contrast is checked on the phone (AC-40, AC-41) whatever Figma shows.
- spec.md section 6: layout and navigation come from the approved wireframes in spec/wireframes/ (15 HTML files, all approved; the index is spec/wireframes/README.md). Their colors and wording are placeholders.
- CLAUDE.md section 6 and plan.md section 3: the Compose theme file in app/ is the source of truth; Figma is only the starting point.
- The code (read 2026-11-17): Theme.kt still holds the template's purple schemes and `dynamicColor = true`, so the app's main colors come from the wallpaper. Type.kt is the template's (bodyLarge only). Color.kt's gain and loss pairs (11) and allocation slice colors (18) are the app's own, with their contrast worked out in comments.
- The first token handoff was deferred in 01 and 02a and never happened (open item 18).
- Conflicts with section 1, to settle after F5 (setup.md and CLAUDE.md are Build documents; spec.md only at level 3): Figma as the source of truth (outcome 1) replaces "the theme file is the source of truth"; Claude Code making fixes in Figma (outcome 5) changes "nothing flows from the code back to Figma", though no code flows back, only Claude Code's edits to the design; placement in Figma (kind 2) reaches into what spec.md section 6 gives the wireframes.

## 4. Three approaches
All three are my reading, to be corrected by section 5. Each is judged by section 6; one that cannot meet the outcome's Figma-first fix loop (outcome 1 and 5) for its own scope says so in section 9.

| | Approach 1: token file in the repo | Approach 2: live Figma link, visual layer | Approach 3: Figma as the screen source |
|---|---|---|---|
| What Figma defines | Variables only: colors (light and dark, gain/loss, slices), type scale, spacing | Variables, plus how components look (a holding row, a card, a button) | Whole screens with their four frames (D3): Figma frames replace the HTML wireframes as the layout source |
| How it reaches the code | The Variables are exported to a JSON file in the repo (by a plugin, or by a script calling the REST API, which Claude Code can run in one prompt). A generator (a Gradle task or a script) writes the theme's Kotlin | Claude Code in VS Code connects to Figma's MCP server. It reads a selected component's design and writes the component code. Code Connect may map Figma components to the app's composables. Tokens: read live through MCP, or kept on approach 1's file (decided by the run, step 3) | The same MCP link, used per screen. Claude Code builds Compose screens from frames, with Code Connect for the shared parts |
| A fix found on the phone (D2) | a token fix: Claude Code changes the Variable in Figma (needs write access, Q6), you approve, re-export | a token or component fix in Figma, the same way | any kind 1, 2 or visible kind 3 fix in Figma, the same way |
| Wireframes | stay | stay | frozen or retired; spec.md section 6 points to Figma |
| Live connection to Figma | none for reading if the plugin route is used; the REST route and Figma fixes need one | yes: a sign-in or token on this PC, never in the repo | yes |
| Gates touched | setup.md and CLAUDE.md text only | as 1, plus .claude/settings.json (MCP permission rules, asked first) | as 2, plus a change request and a spec.md section 6 edit (Design gate) |
| Risks to check | the export tool's upkeep; the generator is code to keep; placement (kind 2) stays outside Figma | Compose output quality (Figma's code may be web-first); the screen-model rule (CR2); the plan or seat cost; what is sent to Figma | all of 2, plus your design time for four frames per screen; the approved wireframes are replaced |

How the approaches relate (your question, 2026-11-17):
- Scope nests: 1 is inside 2, and 2 is inside 3. What Figma defines grows from tokens, to tokens and components, to whole screens, and the gates touched grow with it.
- The mechanism nests only from 2 to 3: both use the MCP link. Approach 1 is a file pipeline, so approach 2 covers 1's scope without containing 1's pipeline.
- So approach 2 has two token routes: live through MCP, or approach 1's file kept for the tokens, with MCP only for components. The file route keeps the theme reproducible from git without Claude Code or a Figma connection. The run measures both (section 8, step 4).
- The choice (F5) is therefore a level: up to 1, up to 2 (with its token route), or up to 3. The trials build on each other (section 8, your A).
- Only level 3 covers outcome 1 for placement (kind 2) in full; at levels 1 and 2, placement stays with the wireframes and the code. Section 9 states this against each level.

The Android native rule (outcome 2) in practice (my reading, checked by Q9):
- the Figma file uses Android frames (the phone's size in dp, portrait and landscape), Material 3 components and Material 3 color roles and type styles, not web or iOS ones;
- sizes are dp and text sizes sp; no px, rem or CSS values reach the code;
- tokens map onto the Compose theme (MaterialTheme's ColorScheme, Typography, and the app's own gain/loss and slice colors);
- code reaches the app only as Jetpack Compose with Material 3; no React, HTML, CSS or Tailwind is kept, even as a step in between.

Also the same in all three (D6 to D9): screenshot tests as the playground, state and condition frames, frames named and annotated with spec refs, and the three CI gates. They do not depend on the level, so the follow-up adopts them whichever level is chosen.

The same in all three: wording is decided on the phone (D9); contrast is checked on the phone (AC-39 to AC-42); a screen shows only what its screen model prepares (CR2); screen tests apply (AC-61); no real data in Figma; turning `dynamicColor` off is a visible change you approve.

## 5. Questions to look up
Each web search or fetch is shown to you (tool, query or URL, reason) and asked first. Pages my tools cannot read (built by JavaScript) are pasted by you.

| # | Question | For | Finding | Source |
|---|---|---|---|---|
| Q1 | Figma's MCP server: local (desktop app) or remote; Windows; the plan and seat it needs; how Claude Code in VS Code connects; its tools | 2, 3 | | not checked |
| Q2 | What code it gives for Android: Jetpack Compose, or web only; how it fits the screen-model rule (CR2) | 2, 3 | | not checked |
| Q3 | Exporting Variables: plugins, the REST Variables API and its plan limits, the file format (DTCG); options for a generator | 1 | | not checked |
| Q4 | Code Connect for Jetpack Compose: does it exist; what mapping this app's components costs | 2, 3 | | not checked |
| Q5 | The Figma for VS Code extension: what it adds beyond MCP | all | | not checked |
| Q6 | Claude Code writing to Figma (the fix loop, D2): what exists (for example the use_figma beta named in P3), on which plans, and what it can change (Variables, components, frames) | all | | not checked |
| Q7 | Security (spec section 9): where the credential lives; what MCP sends where; permission rules for MCP tools | all | | not checked |
| Q8 | Costs: plan or seat, and your time per change | all | | not checked |
| Q9 | Android native in Figma: a current Material 3 design kit for Figma, Android frame presets, how Figma Variables map to Material 3 color roles and type styles, and how each tool keeps dp and sp | all | | not checked |
| Q10 | Approval in Figma (outcome 7): Figma's review features (branching and merging, version history, comments) on your plan, and what record an approval leaves that the repo can name | all | | not checked |
| Q11 | Drift reports (outcome 8, P5): a tool or script that compares Figma's Variables or frames with the code and only reports | all | | not checked |
| Q12 | Screenshot tests for Compose on the JVM (D6): current libraries (for example Roborazzi, Paparazzi), their versions on the day, Windows and CI, comparing an image with a Figma frame, and their test-time cost | all | | not checked |
| Q13 | Compose's accessibility checks in instrumented tests (D9): the current API, what it checks (touch targets, labels, contrast), and how it fits the screen tests | all | | not checked |
| Q14 | Figma annotations (D8): what an annotation can hold, on your plan, and whether MCP returns it to Claude Code | all | | not checked |

## 6. Criteria for the choice
Applied to each level and shown side by side in section 9 (your A, D5).

Hard rules: a level that breaks one is out, whatever else it scores.
- H1 The Android native rule (outcome 2).
- H2 No real data in Figma (spec section 9).
- H3 The Figma credential never in the repo or CI.
- H4 Nothing reaches the app without your approval in Figma and your commit approval (outcome 7).
- H5 Build, tests, lint, spotlessCheck and the screen tests green.
- H6 The phone checks kept: AC-39 to AC-42, AC-61, and kind 3's states on the phone.
- H7 D9: wording decided on the phone.
- H8 The three CI gates (outcome 11, D9) green: contrast of every token pair, the token contract, Compose's accessibility checks.

Ranked criteria, in this order:
1. Fidelity (outcome 8, P1): the phone and the screenshot tests' images match the approved Figma frames; measured by the differences found at the run's phone check and in the images (D6).
2. The one-prompt handoff (outcome 6): hand actions counted from your approval in Figma to code; zero exports, copies or moves is the pass.
3. Figma stays the source of truth (outcomes 1 and 5): the fix loop works for the level's scope, and drift is reported (P5).
4. Raw output quality before any fixing (P4): Material 3 and dp/sp kept, accessibility, CR2.
5. Cost: money (plan or seat) and your design time per change (state and condition frames, D7); code maintainability, test run time (the screenshot tests and the accessibility checks included) and APK size (CLAUDE.md section 8), with "no real difference" said where it holds.
6. Time from your approval to the phone: measured, no limit (D4).

## 7. Decisions
Asked one at a time after the look-ups, each tagged with its stage.

| # | Decision | Stage | Answer |
|---|---|---|---|
| F1 | Your Figma plan and seat, and whether to change it | Build | |
| F2 | Where the Figma credential lives on this PC (never the repo) | Build | |
| F3 | MCP permission rules: ask on every call, or allow read-only tools; writing to Figma always asks (any change to .claude/settings.json asked first) | Build | |
| F4 | The trials' branch names (throw-away-figma-1 from main, -2 from -1, -3 from -2 proposed) | Build | |
| F6 | What counts as your approval in Figma, and how the repo names it (from Q10) | Build | |
| F5 | The final choice: up to level 1, up to level 2 (with its token route: live through MCP or approach 1's file), or up to level 3 | Build if level 1 or 2 (the options within the spec first); level 3 then needs a change request and the Design gate | |

## 8. The run
One numbered step at a time; after each, the results and a stop until you say go.
The trials build on each other (your A, 2026-11-17): trial 2 starts from trial 1's branch and trial 3 from trial 2's, and each measures only what it adds.
1. You set up a Figma test file with no real data, built to outcome 2: Variables for the colors (light and dark, gain/loss, slices), one type scale and spacing; one component (a holding row); one screen (Overall, from wireframe 01) with a frame for each state the spec names for it (at least the empty state and the backup reminder) and, for its main state, the largest-font, landscape and keyboard-up frames (D3, D7); every frame named and annotated with its spec refs (D8).
2. Level 1 trial: export the Variables, generate the theme, then run build, tests, lint and spotlessCheck. Install on the phone; you check contrast. Count the hand actions and measure the time from your approval to the phone.
3. Fix loop, level 1: you name a token defect seen on the phone (or one is planted); Claude Code fixes the Variable in Figma (if Q6 allows); you approve it in Figma (F6); one prompt brings it to code; the phone is checked again.
4. Level 2 trial, on top of trial 1: connect the MCP server, show its tool list, read the test file. Build the holding row; review the code against CR2 and grade it before any fixing (criterion 4); try Code Connect if Q4 allows. Tokens both ways: read the Variables live through MCP and compare the theme it writes with trial 1's; count the hand actions for each route. Repeat the fix loop for a component defect.
5. Level 3 trial, on top of trial 2: build Overall from its four frames through MCP; compare the phone with each frame (fidelity) and with wireframe 01; the screen tests run on the trial's draft pull request (each re-run asked). Repeat the fix loop for a placement defect (kind 2). Measure your design time and the build time.
6. Drift: run the report (Q11) after a Figma change that is not yet in code, and check it names the difference.
7. Checks across the levels: screenshot tests for the holding row's variants and Overall's states, their images compared with the frames (D6); the contrast and token contract tests and Compose's accessibility checks on the trial's draft pull request (D9), with their test time measured; the handoff prompt shown to state the frame's spec refs and criteria before code (D8).
8. Fill the results table (section 9) and ask F5.

## 9. Results
Filled by the run: the hard rules passed or failed, the ranked criteria for each level, the measured hand actions and times, and what was not verified.

## 10. Cleanup and follow-up
- The throw-away branches and their draft pull requests are closed unmerged.
- The Figma test file is kept or deleted by you.
- After F5: the follow-up is named here but not written: an iteration file for level 1 or 2 (with setup.md and CLAUDE.md updates), or a change request for level 3.
