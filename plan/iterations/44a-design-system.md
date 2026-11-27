# plan/iterations/44a-design-system.md

Stage: Build. Status: approved 2026-11-27 (your A); step 1 built and committed (0d3b6aa); step 2 built (research, section 10); step 3 part 1 built (the three token files and TokenFilesTest, shown red first: 6 of 6 failed on the missing files); step 3 part 2 built (Lucide 1.72.0's 24 icons, LICENSE-lucide, icons.json, the 24 vector drawables, IconFilesTest shown red first: 5 of 5 failed on the missing files; D-47). The release is 44b (plan.md row 44b), drafted when 44a is merged.
Source: plan.md section 4 (row 44a); intent/intent-cr-07-design-system.md (CR7, final); spec/spec.md v10 (with the date-picker amendment, pull request #250): C27 to C29, section 6 (Visual design, Accessibility, Theme, Number format and color, Date fields with the date picker, every view and entry flow, the navigation outline), section 14 (K9), AC-40, AC-61 (b) and (e), AC-62, AC-70, AC-73; spec/wireframes/design/ (the accepted drawings); design/README.md and design/decisions.md (D-01 to D-46). "My reading" is an interpretation, not a test.

## 1. Outcome
Every screen of the development app looks as drawn: navy and gold in light and dark, never the wallpaper's colors; Asap and Asap Condensed everywhere; Lucide icons, and a bottom bar with an icon and a name on each tab; one disabled look; a calendar icon on every date field that opens a date picker, with typing kept. No figure, rule, stored value or backup field changes. The release 1.3.0 is iteration 44b.

## 2. Spec refs
- C27 (the licences of Asap and Lucide on About), C28 (the light bar's pill), C29 (the drawings in the repo).
- Section 6: Visual design (1) to (7); Accessibility (4) (icons at 3 to 1, the pill); Theme (dynamic color off); Number format and color (gain, loss, summary-card and error colors); Date fields (the date picker); More and About (the two rows); the navigation outline (the bar's icons).
- Section 14: K9 takes every color, font, size, shape, spacing and icon from the theme built from the token files.
- AC-40 (wallpaper), AC-61 (b) for the picker and (e) for every changed screen, AC-62, AC-70, AC-73.
- Standing checks: AC-09, AC-35, AC-39 to AC-42 on ATS Dev (CLAUDE.md section 7).

## 3. Out of scope
- The release and the version raise (44b).
- Writing the Figma set into Figma (design system D-23); the figma-workflow spike (draft pull request #196).
- D9 wording beyond the new labels (the calendar icon, the pickers' days and buttons).
- Dependabot pull requests.

## 4. Analysis (2026-11-27, by reading the code)
- The theme today is three files in ui/theme/. Theme.kt turns dynamic color on (`dynamicColor = true`, :75) and keeps the template's purple fallback (:15 to :35); Color.kt holds the gain, loss and slice colors; Type.kt sets only bodyLarge, on the system font.
- 51 colors, sizes, weights, shapes and icons are set outside the theme, in 15 files (BottomBar.kt, HoldingDetailScreen.kt, NetWorthScreen.kt and others). K9 now says a screen sets none of its own.
- 27 screen files; those built before 15a have no screen model (open item 48).
- Not in the app today: a date picker, icons in the bar, the card shadow, the disabled state with a solid outline.
- No stored data changes: the database stays at version 15 and the backup format at 1, so no migration.

## 5. Files
- design/: tokens/color.light.tokens.json, tokens/color.dark.tokens.json, tokens/base.tokens.json (DTCG 2025.10); icons/lucide/*.svg, LICENSE-lucide, icons.json; README.md updated.
- app/: ui/theme/ rewritten from the tokens; res/font/ (Asap, Asap Condensed, the OFL licence); res/drawable/ (the icons as vector drawables); shared components in ui/ (buttons, fields, chips, rows, cards, band, layers, checkbox, radio, icon button, blocked card and the catalog's other parts); the date picker; every screen file; strings.xml for new labels; About's two rows (C27).
- Tests: a token unit test (light and dark hold the same names, D-04; the values equal spec 6); a theme unit test against the tokens; the AC-61 (e) helpers in ScreenTestSupport.kt and (e) checks per screen; the picker's model and its (b) and (e) tests; models for the screens that get one.
- Documents: this file; plan.md (sections 2, 3, 4, 11 and 12); CLAUDE.md (the theme pitfall; section 5 for design/); open-items.md where an item is found.

## 6. Order of work
Each step ends with spotlessCheck, build, tests, lint and `:app:compileDebugAndroidTestKotlin`, all with --rerun-tasks, then the report and a stop. One commit per step, proposed after a green run.
1. This file; plan.md (sections 2 and 3, rows 44a and 44b, section 11, the change log); CLAUDE.md's theme pitfall and section 5; the dates of 2026-11-27 corrected in design/ and spec/wireframes/ (section 13).
2. Research, each web search or fetch shown and asked first: the Asap release (static or variable files, their sizes, OFL); the pinned Lucide release, its licence and SVGs; how the SVGs become vector drawables. Never from memory. Results recorded; the decisions are yours.
3. design/ token and icon files; the token unit test written first and shown red.
4. The theme from the tokens: dynamic color off, the purple removed; the palette, the Asap type scale, shapes, and the gain, loss, summary, error, slice and icon-role colors; its unit test first. A Build choice: Kotlin generated from the JSON at build time, or written by hand and checked against the JSON by the test (weighed by maintainability, test time and APK size).
5. The bottom bar with icons (D-37), and the AC-61 (e) helpers proven on it first (text style through the text layout, an icon by its name, sampled pixels). Their [UNVERIFIED] parts are settled here; if a helper does not work on the runner, I stop and you decide.
6. The shared components from the catalog, each with its states; the disabled state.
7. The date picker (spec 6, Date fields): the calendar icon on every date field with typing kept; the standard picker (a confirm button and Cancel) and the dense picker (the filters' From and To; a tap picks and closes); drawn inside the window; its model; an AC-61 (b) focus test and (e) checks; its TalkBack labels (D9, shown for approval).
8. The screens in five groups, each with screen models where missing (open item 48), the drawing, (e) checks, and the existing AC-61 tests kept: (a) Overall, Net Worth, Allocation; (b) Portfolios, a portfolio, Holding detail, Hidden holdings; (c) Cash, Transaction history, Realized gains; (d) buy, sell and the lot picker, split, cash entry, detail, reversal, Reverse and re-enter; (e) the portfolio dialogs, Backup and restore, More, Settings, lists, tab order, About (C27), lock and cover.
9. The phone check on ATS Dev installed over `1.2.0-dev.53`: AC-73 against the drawings, light and dark, with the date pickers; AC-40 with a changed wallpaper; the drawings' open checks (the 32 figure at the largest font, a dense field, the pill around "Portfolios", the real keyboard); the standing checks; Logcat by uid. Then the APK size before and after, the Deploy review against the spec refs, and the pull request draft.

## 7. Stubs
None planned.

## 8. Risks
1. One very large pull request (your decision, 2026-11-27): each step is its own commit, so the review can go commit by commit.
2. The AC-61 (e) helpers are [UNVERIFIED] on API 37 (spec AC-61): step 5 proves them before any screen relies on them.
3. Values set outside the theme: every `Color(0x…)`, `.sp`, `FontWeight` and `RoundedCornerShape` outside ui/theme/ is searched for in each group (K9).
4. Fonts at the largest size: the 32 figure falls back to 28 if it wraps badly (D-13); fixed parts must still give way (AC-61 (a)).
5. APK size: about twelve font files and the icons; measured in step 2 and at the end.
6. Dark cards stand off the page only faintly (1.20 to 1, design/decisions.md); noted, not a rule.
7. The date picker is a new control on about nine screens and twelve date fields: every existing date test still passes with typing unchanged.

## 9. Proof
- Commands: spotlessCheck, `.\gradlew.bat :core:assemble :app:assembleDebug`, the test command of CLAUDE.md section 3, `:app:lintDebug`, all with --rerun-tasks, and `:app:compileDebugAndroidTestKotlin`. Test totals from the result files; the screen tests counted from the artifacts on the pull request.
- Before installing: the APK's and each classesN.dex's size against the previous build (CLAUDE.md, open item 60).
- The phone check of step 9.

## 10. Research
Step 2, run 2026-11-27 (your yes): `gh api` read only on github.com, and one web search. Nothing about you or the project was sent.
1. Asap (google/fonts, ofl/asap and ofl/asapcondensed): Asap ships as two variable files, `Asap[wdth,wght].ttf` (376,764 bytes) and `Asap-Italic[wdth,wght].ttf` (426,484 bytes), with a width axis from 75 to 125 and a weight axis from 100 to 900; version 3.002, from Omnibus-Type/Asap at commit 2de32f2 (2025-05-23); OFL.txt 4,383 bytes. Asap Condensed ships as 16 static files of 106,820 to 120,208 bytes; the six the type scale needs (400, 500 and 700, each with its italic) add up to 688,596 bytes. [UNVERIFIED] whether Asap Condensed looks the same as the variable font at width 75; and that Compose sets a variable font's axes on API 33 (my memory: `FontVariation`, API 26 and up), checked in step 4.
2. Lucide (lucide-icons/lucide): a release every weekday; the latest is 1.82.0 (published 2026-11-27, today); 1.72.0 was published 2026-11-11. Each release has `lucide-icons-<version>.zip` (1,615,346 bytes for 1.82.0) with the SVGs. The repo's LICENSE: ISC ("Copyright (c) 2026 Lucide Icons and Contributors"), then MIT ("Copyright (c) 2013-present Cole Bemis") for the icons derived from Feather, listed by name; that list includes calendar, check, the chevrons, info, lock, more-horizontal, more-vertical and x.
3. SVG to vector drawable (one web search, "convert SVG to Android vector drawable command line"): vd-tool (npm; built from Android Studio's own converter), svg2vectordrawable and svg2vdandroid (npm), svg_to_avd (Dart), svg-to-compose (Kotlin, writes Compose code). Node 26.2.0 and npm 11.13.0 are on this PC. None was installed. Sources: https://www.npmjs.com/package/vd-tool, https://github.com/Ashung/svg2vectordrawable, https://www.npmjs.com/package/svg2vdandroid, https://pub.dev/documentation/svg_to_avd/latest/, https://github.com/rafaeltonholo/svg-to-compose/blob/main/README.md.

## 11. Decisions (Build)
- 0. Two iterations for CR7, 44a (everything) and 44b (the release) (your B, 2026-11-27).
- 1. The date picker is built in 44a, after spec v10 was amended (your B, 2026-11-27; pull request #250).
- 3. Lucide 1.72.0 (2026-11-11) is pinned, not 1.82.0 (published the day of step 2): the icons used are the same in both, and 1.72.0 had 16 days of use first (your A, 2026-11-27).
- 4. The SVGs become vector drawables through vd-tool (npm; Android Studio's own converter), run once with npx; its XML is kept in res/drawable/ and a unit test checks that every icon in icons.json has its SVG and its drawable (your A, 2026-11-27). The design system does not name a Lucide version (D-24, D-26): the drawings' icons were drawn from memory, so the release file wins where a path differs (AC-73); D-47 records the pin.
- 5. The 24 drawables stay in step 3; lint's 24 UnusedResources notices (one per icon, until a screen draws it) are listed in each step's report and in CLAUDE.md's lint line, not silenced, and fall to 0 by step 8 (your A, 2026-11-27).
- Open from step 2: 2. which font files ship (variable Asap with or without the italics; Asap Condensed as its own files or as Asap at width 75). Asked before step 4's fonts.
- Open from step 4: the way of building the theme.

## 12. Wording to approve (D9)
In step 7 and step 8: the calendar icon's TalkBack label, the pickers' day and button labels, About's two rows; shown before use.

## 13. Notes
- The date 2026-11-28 was written by mistake for 2026-11-27 in design/decisions.md (D-38 to D-46), design/README.md, spec/wireframes/README.md and spec/wireframes/design/README.md; corrected in step 1. The frozen drawing pages in spec/wireframes/design/ keep it in their captions (C29: not edited).
