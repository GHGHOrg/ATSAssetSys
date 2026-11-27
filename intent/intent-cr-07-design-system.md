# intent-cr-07-design-system.md (CR7, final)

Change request CR7, 2026-11-27: ATS takes on the Corporate Design System.
Base: intent.md v52, CR1 to CR6, spec.md v9. Discussion: the design-system interview of 2026-11-26 and 27, recorded in the Corporate Design System artifact (https://claude.ai/artifact/PHbbECbga1979aCTqDDt29, decisions.md, D-01 to D-36) and its drawing pages, listed there.

## Problem
- ATS has no look of its own. Its theme turns Android's dynamic color on (ui/theme/Theme.kt, `dynamicColor = true`), so its colors follow the wallpaper and change when the wallpaper does, and its text uses the system font (ui/theme/Type.kt, `FontFamily.Default`).
- Because the colors come from the wallpaper, the contrast rules of spec section 6 (Accessibility (4), Theme) hold only for the wallpaper the phone had when they were checked.
- I want ATS, and the apps after it, to share one look that I chose, kept in files that tools can read: the Corporate Design System.

## Outcome
1. **ATS uses the Corporate Design System** as recorded in the artifact on the day this intent is accepted: its palette, type, shape, spacing, components and icons, in light and dark (D-06 to D-19, D-24 to D-36).
2. **Its own colors only**: dynamic color is off (D-06). The palette is midnight gold, light mode "L2" (D-07, D-10). The light bar's selected-tab pill (1.96:1 against the bar) is an accepted exception (D-10).
3. **Its own fonts**: Asap and Asap Condensed, never a system font (D-11, D-12), with the figure-led type scale (D-13). The font files ship with the app.
4. **Shape and spacing**: one corner radius, 8, and tight spacing; anything tappable at least 48 dp (D-14, D-15).
5. **Errors in bronze brown** with Lucide's circle-alert; red stays for losses (D-16, D-27).
6. **Icons**: Lucide, at Lucide's own stroke of 2, shipped unchanged from a pinned release, with its licence notices (D-19, D-24 to D-26).
7. **Every screen** as drawn and accepted: the component catalog and remaining screens (D-17, D-18), Cash (D-29, D-30) and the entry flows 10a to 10f (D-31 to D-36), including Cash's card slices (`Modifier.cardSlice`).
8. **Departures from approved wireframes** (spec/wireframes/), each the catalog's disabled state (38%, solid outline) in place of a dashed outline, so one rule covers everything that can't be tapped yet:
   - wireframe 07, marker 3: unavailable chips (D-29);
   - wireframe 10d, marker 10: Delete portfolio before the name matches (D-34);
   - wireframe 10e, marker 4: Restore while offline (D-35).
9. **The design files live in the ATS repo** under design/ (tokens, the Figma set, icons), the repo leads, and Claude Code writes the Figma set into Figma (D-20 to D-23, D-26).
10. Nothing else changes: no figure, rule, stored value or backup field changes. Only how the screens look.

## Users
- Me.

## Constraints
- Contrast and touch targets as spec section 6, Accessibility, in both themes (AC-40); the design system's contrast table (decisions.md) is the starting point, checked again on the phone.
- Text follows the font size setting up to the largest; nothing is cut off (Accessibility (1), AC-61). The design system's 32 summary figure has 28 as its fallback (D-13).
- Dialogs, menus and the date picker are drawn inside the app's window, never as separate windows (CLAUDE.md pitfall).
- Wording on screens is D9: every label in the drawings is a placeholder.
- No analytics or crash-reporting library; fonts and icons are files in the app, never fetched at run time (my reading of spec section 9).
- A look only: no database schema change and no new backup format version (spec section 8; my reading).
- Each changed screen gets a screen model (CR2; open item 48) and the screen tests that apply (CR3, AC-61).
- Shipped through the release process (CR4, plan/release.md). A change of style only is MINOR: the next release is 1.3.0 (CLAUDE.md section 4; decided 2026-11-27, open question 1, A).

## Open questions (Plan)
None remaining. Both were answered on 2026-11-27:
1. Answered 2026-11-27 (A): 1.3.0, a change of style only, not a milestone. See Constraints.
2. Answered 2026-11-27 (A): the 10f drawing left out More's About row (CR5, spec 6), because wireframe 10f predates CR5. It was added to the drawing as the fifth row, after Backup and restore (D-36, amended).

## Deferred to Design
- The spec edits: section 6 (a visual identity paragraph naming the design system, Theme, Number format and color's colors), section 9 if the licence notices need a place, section 14 (K9), the acceptance criteria, and the three wireframe departures.
- Which wireframes are redrawn or annotated, and whether the drawings become spec/wireframes/ files or stay as artifacts linked from spec.md.
- Phone checks the drawings left open: the 32 figure at the largest font, a dense field with an 8-decimal quantity or a date, the bar pill around "Portfolios", the real keyboard height.

## Deferred to Build
- The iterations and their order (plan.md).
- The design/ files, the D-05 equality check's code, the Figma write through MCP.
- Font files (static or variable; about twelve files) and their size in the APK; Lucide SVGs to Android vector drawables, the stroke from the token.
- Screen models, screen tests and D9 wording.
