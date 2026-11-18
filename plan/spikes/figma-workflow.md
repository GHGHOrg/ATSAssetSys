# plan/spikes/figma-workflow.md

Stage: Build. Status: written 2026-11-17; discovery done the same day (section 0); approved by you 2026-11-18 as written, before the look-ups (section 5). Look-ups and decisions F1 to F4 and F6 done 2026-11-18; sections 4 and 8 corrected from them; the corrections approved by you 2026-11-18. Nothing in it has run.
Source: your request of 2026-11-17 (four points), your choice C (how far Figma reaches is decided by this spike), your answers in the discovery (section 0), five web searches on 2026-11-17, plan/setup.md section 3 (design source), spec/spec.md section 6, CLAUDE.md, open item 18, the theme files in app/ (read 2026-11-17).
Section 5's look-ups were done 2026-11-18 from search summaries (eleven searches) and your pastes (Figma's pricing page, a forum reply); sections 4 and 8 are corrected from them; what they could not answer stays "not checked".

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
| How it reaches the code | The Variables reach a JSON file in the repo: by a community plugin such as a DTCG exporter (a click), or read by Claude Code through the MCP server's Variables tool or a use_figma Plugin API script (one prompt). The REST API is Enterprise only (Q3). A generator (a Gradle task or a script) writes the theme's Kotlin; Material Theme Builder can make the Material 3 Variables in Figma and export Compose theme files, also a click (Q9) | Claude Code in VS Code connects to Figma's MCP server. It reads a selected component's design and writes the component code. Code Connect may map Figma components to the app's composables. Tokens: read live through MCP, or kept on approach 1's file (decided by the run, step 4) | The same MCP link, used per screen. Claude Code builds Compose screens from frames, with Code Connect for the shared parts |
| A fix found on the phone (D2) | a token fix: Claude Code changes the Variable in Figma (needs write access, Q6), you approve, re-export | a token or component fix in Figma, the same way | any kind 1, 2 or visible kind 3 fix in Figma, the same way |
| Wireframes | stay | stay | frozen or retired; spec.md section 6 points to Figma |
| Live connection to Figma | none if the plugin route is used; the one-prompt routes and Figma fixes need the remote MCP server (F2) | yes: a sign-in or token on this PC, never in the repo | yes |
| Gates touched | setup.md and CLAUDE.md text only | as 1, plus .claude/settings.json (MCP permission rules, asked first) | as 2, plus a change request and a spec.md section 6 edit (Design gate) |
| Risks to check | the export tool's upkeep; the generator is code to keep; placement (kind 2) stays outside Figma | Compose output quality: the MCP server answers React unless Preferences > Language is Android > Compose, and React fails H1 (Q2, your B); Code Connect on Professional: one library and one framework (Q8); the screen-model rule (CR2); what is sent to Figma | all of 2, plus your design time for four frames per screen; the approved wireframes are replaced |

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
- Figma's design context (your B, 2026-11-18): Figma's Preferences > Language is set to Android > Compose (Q2). If the MCP server still answers with React, HTML, CSS or Tailwind, that level fails hard rule H1; React-shaped context is not used even as transient input. Levels 2 and 3 then remain only through Variables and Code Connect's Compose mappings.

Also the same in all three (D6 to D9): screenshot tests as the playground, state and condition frames, frames named and annotated with spec refs, and the three CI gates. They do not depend on the level, so the follow-up adopts them whichever level is chosen.

The same in all three: wording is decided on the phone (D9); contrast is checked on the phone (AC-39 to AC-42); a screen shows only what its screen model prepares (CR2); screen tests apply (AC-61); no real data in Figma; turning `dynamicColor` off is a visible change you approve.

## 5. Questions to look up
Each web search or fetch is shown to you (tool, query or URL, reason) and asked first. Pages my tools cannot read (built by JavaScript) are pasted by you.

Look-ups done 2026-11-18: ten web searches (L1 to L10), each shown and approved. "Search summary" means the search tool's summary of the pages, not page text I opened; official Figma or Android pages are named where the summary cites them. Nothing here is checked by a run.

| # | Question | For | Finding | Source |
|---|---|---|---|---|
| Q1 | Figma's MCP server: local (desktop app) or remote; Windows; the plan and seat it needs; how Claude Code in VS Code connects; its tools | 2, 3 | Two servers. Remote (recommended by Figma): hosted at mcp.figma.com/mcp, no desktop app, on all plans and seats, metered: Starter gets limited calls, a Dev or Full seat on Professional or Organization up to 200 read calls a day (Enterprise 600). Desktop: needs the desktop app and a Dev or Full seat on a paid plan, local address, cannot write to the canvas. Claude Code adds either with `claude mcp add --transport http`; a Claude Code plugin exists; VS Code has a setup article | L1, search summary: help.figma.com "Guide to the Figma MCP server" (32132100833559), "VS Code and Figma: Set up the MCP server" (39890361040535); mcp.so guide |
| Q2 | What code it gives for Android: Jetpack Compose, or web only; how it fits the screen-model rule (CR2) | 2, 3 | Its design context comes as React and Tailwind by default. Forum reports: asking for Kotlin or Compose still returned JSX with Tailwind; with Code Connect set up for Compose, get_design_context still returned React. No official statement found. A community server (figma-android-mcp) outputs Compose. So Compose is written by Claude Code from React-shaped context, not given by Figma: a question for the Android native rule (section 4). Later reply in thread 43110 (you pasted, 2026-11-18; a forum user, found by accident, not Figma's documentation): the MCP server answers in the language set in Figma's Preferences > Language (CSS, iOS, or Android > Compose or XML), on the remote and the desktop app; the ClientFramework and ClientLanguage parameters are used only for logging. Not checked: the run tries it (section 8, step 4) | L2, search summary: forum.figma.com threads 43110 and 53081; pkgstats figma-android-mcp; your paste of thread 43110 |
| Q3 | Exporting Variables: plugins, the REST Variables API and its plan limits, the file format (DTCG); options for a generator | 1 | The Variables REST API needs a Full seat in an Enterprise organization. On any plan, Variables are read through the Plugin API: by a plugin (for example TokensBruecke, DTCG 2025.10 JSON, "Download JSON") or by a script run inside Figma. So approach 1's "script calling the REST API" works only on Enterprise; on other plans the export is a plugin click, or a Plugin API script run through use_figma (Q6; my reading, not checked) | L3, search summary: developers.figma.com REST API "variables"; forum.figma.com 51451; TokensBruecke README |
| Q4 | Code Connect for Jetpack Compose: does it exist; what mapping this app's components costs | 2, 3 | Exists: Figma's docs list Jetpack Compose as a Code Connect target (developers.figma.com/docs/code-connect/compose); one component can map to several frameworks. But reports say the MCP server did not return the Compose mapping (Q2). The mapping cost is not found | L2, search summary: developers.figma.com Code Connect; help.figma.com 23920389749655; forum threads |
| Q5 | The Figma for VS Code extension: what it adds beyond MCP | all | Inspect files, comments and their notifications, change tracking, and code auto-complete from the selected layer, inside VS Code. It gives Claude Code nothing: the agent's link is the MCP server. No 2026 changelog found | L4, search summary: help.figma.com "Figma for VS Code" (15023121296151) |
| Q6 | Claude Code writing to Figma (the fix loop, D2): what exists (for example the use_figma beta named in P3), on which plans, and what it can change (Variables, components, frames) | all | use_figma (write to canvas): open beta since 2026-03-24, free during the beta, later a usage-based paid feature (no rates or date). Runs JavaScript through the Plugin API, so it can create and change native Figma content, Variables included (my reading). Remote server only, through listed clients, Claude Code among them. Professional, Education, Organization and Enterprise plans; writing outside your drafts needs a Full seat (sources differ on Dev seats). No images or custom fonts yet. Figma advises installing its skills for the client | L5, search summary: developers.figma.com "write to canvas", "tools and prompts"; help.figma.com 39216419318551 |
| Q7 | Security (spec section 9): where the credential lives; what MCP sends where; permission rules for MCP tools | all | The remote server signs in only through Figma's OAuth, started by the client; no personal access token (a feature request asks for one). Only clients in Figma's MCP catalog may connect; Claude Code is one. Claude Code keeps and refreshes the token itself, reportedly in the user-level file ~/.claude.json (a bug report about it not being written), outside the repo; `/mcp` > figma > revoke clears it, and `claude mcp remove` drops it. Design data passes from Figma's hosted service to Claude Code and so to Anthropic. The desktop server stays local but cannot write (Q1), so the fix loop needs the remote one. Not checked by a run | L11 (2026-11-18), search summary: forum.figma.com 57343 and 55680; claudeissues.com 35957; mcp.so guide |
| Q8 | Costs: plan or seat, and your time per change | all | Needed for the full outcome: a Professional plan or higher with a Full seat (writing outside drafts, Q6; up to 200 read calls a day, Q1). Branch review needs a Full seat in an Organization or Enterprise plan (Q10). use_figma becomes paid after the beta. Figma's prices not found: its pricing page is built by JavaScript, so you paste it | L1, L5, L6; prices not checked |
| Q9 | Android native in Figma: a current Material 3 design kit for Figma, Android frame presets, how Figma Variables map to Material 3 color roles and type styles, and how each tool keeps dp and sp | all | Material Theme Builder (a Figma plugin and a web tool) makes the Material 3 palette as Figma Variables and styles, and exports Jetpack Compose Kotlin (Theme.kt, Color.kt). The Material 3 Design Kit for Figma is built to work with it (a listing shows an update in April 2026). Open issues report it breaking Variable modes. Android frame presets and dp/sp handling not found | L10, search summary: github.com material-foundation/material-theme-builder; m3.material.io blog; material.io blog on the design kit |
| Q10 | Approval in Figma (outcome 7): Figma's review features (branching and merging, version history, comments) on your plan, and what record an approval leaves that the repo can name | all | Branch review: a reviewer approves the whole branch or requests changes (no partial approval); an "Approved" badge shows; anyone who can edit the main file can merge. Branching needs a Full seat in an organization or workspace (Organization or Enterprise; the plan tiers not confirmed on an official pricing page). On Professional, version history and comments remain (not looked up). What record the repo could name (a version or branch name) is not found | L6, search summary: help.figma.com "Review branch changes" (5693123873687), "Merge branch into main file" (5691189138839) |
| Q11 | Drift reports (outcome 8, P5): a tool or script that compares Figma's Variables or frames with the code and only reports | all | No established product; community tools only (Claude skills such as design-system-governance, report-only; DesignxCode MCP server, its Variables API part needs Organization or Enterprise). Their upkeep and security are not checked. The token contract test (D9) already compares the token source with the theme in CI, which covers token drift (my reading) | L7, search summary: mcpmarket and claudskills listings; pkgstats designxcode-mcp-server |
| Q12 | Screenshot tests for Compose on the JVM (D6): current libraries (for example Roborazzi, Paparazzi), their versions on the day, Windows and CI, comparing an image with a Figma frame, and their test-time cost | all | Three JVM options, no emulator: Roborazzi (on Robolectric; works with Compose previews; runs in the normal test task), Paparazzi (on LayoutLib; single-frame; reported to break on AGP or Compose updates; a separate task), and Google's Compose Preview Screenshot Testing. Versions, Windows, CI time and comparison with a Figma frame not found: looked up on the day the trial starts | L8, search summary: Roborazzi docs; droidcon course; proandroiddev |
| Q13 | Compose's accessibility checks in instrumented tests (D9): the current API, what it checks (touch targets, labels, contrast), and how it fits the screen tests | all | `enableAccessibilityChecks()` from androidx.compose.ui:ui-test-accessibility wraps Espresso's AccessibilityValidator. It checks content labels, touch target size (48 by 48 dp), low contrast and implementation. Reported inconclusive on Robolectric, so it belongs in the emulator screen tests, not the JVM screenshot tests. Version and how failures are thrown not found | L9, search summary: proandroiddev; dev.to; a skill page (secondary) |
| Q14 | Figma annotations (D8): what an annotation can hold, on your plan, and whether MCP returns it to Claude Code | all | Not found whether the MCP server returns Dev Mode annotations. A 2024 forum report: a branch with only annotation changes could not be merged; no fix found. Open | L6, search summary: forum.figma.com 84411 |

Figma's plans and prices (you pasted figma.com/pricing and its "What is a seat?" text, 2026-11-18; USD per seat per month):

| | Starter | Professional | Organization | Enterprise |
|---|---|---|---|---|
| Full seat | free | $20 monthly, $16 billed annually | $55, annual only | $90, annual only |
| Dev seat | – | $15 monthly, $12 annually | $25 | $35 |
| Collab seat | – | $5 monthly, $3 annually | $5 | $5 |
| AI credits (Full seat) | 150 a day, up to 500 a month | 3,000 a month | 3,500 | 4,250 |
| Figma MCP server | 20 tool calls a month | 200 a day, 10 a minute | 200 a day, 15 a minute | 600 a day, 20 a minute |
| Variables / Variable modes | yes / none | yes / 10 modes | yes / 20 modes | yes / unlimited |
| Annotations; Dev Mode "View annotations and measurements" | no | yes | yes | yes |
| Code Connect | no | one active library and one framework | unlimited | unlimited |
| Dev Mode: compare changes, component playground, VS Code extension | no | yes | yes | yes |
| Dev Mode "Improved handoff experience" (Ready for dev) | no | on canvas only | yes | yes |
| Plugins in Dev Mode / private plugins | no / no | community plugins only / no | yes / yes | yes / yes |
| Version history | 30 days | yes | yes | yes |
| Branching and merging | no | no | yes | yes |
| Webhooks | no | yes | yes | yes |
| REST API for variables | no | no | no | yes |

Seats: only the Full seat can edit Figma Design files (and includes Dev Mode); Dev and Collab seats only view and comment there. Free viewers can inspect and copy CSS, iOS and Android code.

What the prices answer:
- Q8: one Full seat is needed, since only it edits designs (yours and Claude Code's fixes, Q6). Professional: $192 a year (annual) or $240 (monthly); Organization $660 a year; Enterprise $1,080 a year before any sales terms. Whether MCP calls or use_figma draw on AI credits is not shown.
- Starter cannot run the workflow: 20 MCP calls a month, and no Variable modes, so no light and dark themes in Variables.
- Q10 on Professional: no branching. What is there: unlimited version history, Dev Mode "compare changes", and "Ready for dev" marked on the canvas. An approval record would be a named version or Ready for dev (my reading, to be tried).
- Q14: annotations and their view in Dev Mode come with Professional; whether MCP returns them stays open.
- Q3: on Professional, community plugins (such as a DTCG exporter) run; the Variables REST API is Enterprise only, as found.
- Q4: Professional's Code Connect allows one framework, which is enough for Compose only.
- Webhooks on Professional could signal a design change (my reading; not needed for D4's one prompt).

What the look-ups change (my reading, for you to decide at F1 and later):
- Figma's plan is now the main decision. The fix loop (D2, Q6) needs a paid plan with a Full seat; approval by branch review (Q10) needs Organization or Enterprise; reading Variables by REST (approach 1's script) needs Enterprise.
- Approach 1 without Enterprise has a hand step (a plugin click), which breaks D4 unless the Variables are read through MCP or use_figma; Material Theme Builder's Compose export is a candidate generator, but it is also a click.
- Figma's MCP server does not give Compose by default: Claude Code writes Compose from React-shaped context (Q2). A forum reply says Figma's Preferences > Language > Android > Compose makes it answer in Compose; the run tries it. Whether React-shaped context passes the Android native rule (outcome 2) if the setting fails is yours to decide.
- Accessibility checks go in the emulator screen tests (Q13); screenshot tests on the JVM (Q12).
- Still open: Q7 (sign-in and token storage), Q8 (prices, to be pasted), Q14 (annotations through MCP), Q12's versions on the day.

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
| F1 | Your Figma plan and seat, and whether to change it | Build | 2026-11-18: you have no Figma account yet (your D); the plan and seat are chosen in this spike, from Q8's prices. Chosen 2026-11-18 (your A): Professional, one Full seat, billed monthly ($20 a month), so it can be cancelled after any month; approval is recorded as a named version or Ready for dev (F6), not by branch review |
| F2 | Where the Figma credential lives on this PC (never the repo) | Build | 2026-11-18 (your A): Figma's remote MCP server, connected to Claude Code directly (not through a VS Code extension), added at user scope (`claude mcp add --scope user --transport http figma https://mcp.figma.com/mcp`, the exact form checked on the day); OAuth sign-in in Claude Code; the token only in your user files; nothing Figma-related in the repo. After the spike, `/mcp` > figma > revoke, unless F5 keeps Figma. The Figma for VS Code extension is optional, for you only, and no part of the workflow |
| F3 | MCP permission rules: ask on every call, or allow read-only tools; writing to Figma always asks (any change to .claude/settings.json asked first) | Build | 2026-11-18 (your A): in your user settings, ~/.claude/settings.json (C:\Users\koebi\.claude\settings.json), outside the repo: `permissions.allow` lists Figma's read tools (design context, Variables, screenshot, metadata, Code Connect map), `permissions.ask` lists use_figma, so every write to Figma prompts. The real tool names are read from `/mcp` on the day; I show the exact text and you paste it (as in plan/spikes/vscode-claude-code.md section 4, step 4). Not checked: whether user-level allow rules need the folder-trust dialog; the first read call shows it |
| F4 | The trials' branch names (throw-away-figma-1 from main, -2 from -1, -3 from -2 proposed) | Build | 2026-11-18 (your A): throw-away-figma-1 from main, throw-away-figma-2 from -1, throw-away-figma-3 from -2; the spike's name stands in for NN (CLAUDE.md section 4). Each has its own draft pull request, so CI and the screen tests run per level; all three are closed unmerged at cleanup |
| F6 | What counts as your approval in Figma, and how the repo names it (from Q10) | Build | 2026-11-18 (your A): both. You mark the approved frames Ready for dev (what), then save a named version, for example `approved 2026-11-20 V06 empty, V06 largest font` (when). Claude Code's handoff takes only Ready-for-dev frames; the commit message and the iteration file quote the version's name, never a link. Not checked: whether MCP shows Claude Code the Ready-for-dev status and the version names; the run tries it |
| F5 | The final choice: up to level 1, up to level 2 (with its token route: live through MCP or approach 1's file), or up to level 3 | Build if level 1 or 2 (the options within the spec first); level 3 then needs a change request and the Design gate | |

## 8. The run
One numbered step at a time; after each, the results and a stop until you say go.
The trials build on each other (your A, 2026-11-17): trial 2 starts from trial 1's branch and trial 3 from trial 2's, and each measures only what it adds.
1. You set up Figma (Professional, one Full seat, monthly; F1) with Preferences > Language set to Android > Compose (Q2). You add the remote MCP server at user scope and sign in (F2), and paste the permission rules I show you (F3). Then a test file with no real data, built to outcome 2: Variables for the colors (light and dark, gain/loss, slices), one type scale and spacing; one component (a holding row); one screen (Overall, from wireframe 01) with a frame for each state the spec names for it (at least the empty state and the backup reminder) and, for its main state, the largest-font, landscape and keyboard-up frames (D3, D7); every frame named and annotated with its spec refs (D8).
2. Level 1 trial, on throw-away-figma-1 (F4): get the Variables into the repo's JSON file two ways, by a community plugin (a click) and by one prompt through MCP or use_figma, counting the hand actions of each; generate the theme; then run build, tests, lint and spotlessCheck. Install on the phone; you check contrast. Count the hand actions and measure the time from your approval to the phone.
3. Fix loop, level 1: you name a token defect seen on the phone (or one is planted); Claude Code fixes the Variable in Figma through use_figma (Q6; it prompts, F3); you approve it in Figma: Ready for dev and a named version (F6); check whether MCP shows Claude Code that status and name; one prompt brings it to code; the phone is checked again.
4. Level 2 trial, on throw-away-figma-2, on top of trial 1: connect the MCP server, show its tool list, read the test file. First check the language of its design context: Compose passes; React, HTML, CSS or Tailwind fails H1 for levels 2 and 3, and the trial goes on only with Variables and Code Connect's Compose mappings (section 4, your B). Build the holding row; review the code against CR2 and grade it before any fixing (criterion 4); try Code Connect if Q4 allows. Tokens both ways: read the Variables live through MCP and compare the theme it writes with trial 1's; count the hand actions for each route. Repeat the fix loop for a component defect.
5. Level 3 trial, on throw-away-figma-3, on top of trial 2: build Overall from its four frames through MCP; compare the phone with each frame (fidelity) and with wireframe 01; the screen tests run on the trial's draft pull request (each re-run asked). Repeat the fix loop for a placement defect (kind 2). Measure your design time and the build time.
6. Drift: run the report (Q11) after a Figma change that is not yet in code, and check it names the difference.
7. Checks across the levels: screenshot tests for the holding row's variants and Overall's states, their images compared with the frames (D6); the contrast and token contract tests and Compose's accessibility checks on the trial's draft pull request (D9), with their test time measured; the handoff prompt shown to state the frame's spec refs and criteria before code (D8).
8. Fill the results table (section 9) and ask F5.

## 9. Results
Filled by the run: the hard rules passed or failed, the ranked criteria for each level, the measured hand actions and times, and what was not verified.

## 10. Cleanup and follow-up
- The throw-away branches and their draft pull requests are closed unmerged.
- The Figma test file is kept or deleted by you.
- After F5: the follow-up is named here but not written: an iteration file for level 1 or 2 (with setup.md and CLAUDE.md updates), or a change request for level 3.
