# Corporate Design System

Copied into the ATS repo on 2026-11-27 from the Corporate Design System artifact, version 47 (CR7, C29). From now on this copy leads (D-22): a change starts here.

A design system for the apps made here, not for one app alone. Its first app is ATS Asset System, a personal Android asset tracker with one user, and the rules and screens below come from it. It's being set by interview, and this page records the system as it stands. Its source of truth will be token files in the repo (decision D-02). This page is a readable view of them, not the source.

Status on 2026-11-26: Plan stage. Nothing here has changed the app yet. A change to the app's look goes through a change request (intent-cr-07) and spec.md first.

## The first app it serves: ATS Asset System

- Android, Jetpack Compose, Material 3. It follows the phone's light or dark setting: one light theme, one dark theme, no theme setting (spec section 6, Theme).
- Screens are dense with money figures: net worth, gain/loss, lots, cash entries, allocation donuts.
- Wording on screens is decided on the phone and approved one message at a time (D9). The system gives no copy.

## The look today, read from the code

| Part | Today | Where |
| --- | --- | --- |
| Base colors | Since 44a step 4: the palette below, generated from design/tokens/; dynamic color removed (D-06, D-07). Before: the phone's wallpaper colors. | Theme.kt |
| Fallback colors | Since 44a step 4: none (the template purple removed) | Color.kt, Theme.kt |
| Own colors | Gain and loss, one pair per theme; the allocation slice colors | Color.kt |
| Type | Since 44a step 4: Asap and Asap Condensed on the type roles below, each Material style set from one (D-12). Before: Material 3 defaults on the system font. | Type.kt |
| Shapes, spacing | Material 3 defaults plus values set screen by screen; not inventoried yet | screens |

## The palette: Midnight gold in navy (D-07, D-08)

Ten navy shades, numbered light to dark, with gold for actions. Gain, loss and the slice colors are the app's current ones, unchanged.

| # | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Hex | #B0C4E8 | #8AABC5 | #6291B9 | #4C7CAA | #376A9E | #295C85 | #1C4F6C | #153D5A | #0E2B47 | #0A1A36 |

| Role | Light | Dark |
| --- | --- | --- |
| Page | #EBF0F8 (derived, lighter than 1) | 10 |
| Card | #DFE7F6 (1 mixed 40% with white) | 9 |
| Title band | 10, white title | none: the title sits on the page |
| Summary card | 8, white text, labels in 1 | 7, white text, labels in 1 |
| Gain / loss on the summary card | #8FE08A / #FFC2BB | #8FE08A / #FFC2BB |
| Main text | 10 | #E6ECF5 (derived) |
| Secondary text | 6 | 1 |
| Dividers between rows | 1 | 7 |
| Filled (main) button | gold #C9A227, text 10 | gold #E0BC4A, text 10 |
| Outline button | line 5, text 9 | gold line and text |
| Action and field icons (calendar, chevron-down, eye, eye-off; D-46) | 9, as the outline button's text | gold |
| Row chevrons that only show where a row leads, expand chevrons (D-46) | 6, as secondary text | 1, as secondary text |
| Icons on the title band (back arrow, ⋮, settings; D-46) | white, as the band's title | #E6ECF5, as main text |
| Error (refused field) | bronze #7A541F | sand #E2BF8D |
| Bottom bar | 10; labels 2; selected: gold label on a 7 pill | #050D1C (derived); labels 2; selected: gold label and gold line |

Cards have no border. A two-layer shadow lifts them: in light, 0 1px 3px and 0 4px 12px in swatch 10 at 26% and 16%; in dark, 0 1px 3px and 0 5px 14px in black at 62% and 48%. Rows inside a card keep their dividers.

Every pair on Overall and Holding detail was checked against spec 6 (Decisions, "Contrast check for D-07 and D-08"): 53 of 54 pass. The light bar's selected-tab pill fails 3:1 against the bar and is kept as an accepted exception, since its gold, bold label carries the selection (D-10).

Renders: [Midnight Gold Variants](https://claude.ai/artifact/FRzGkNNdWyJGG3nSemMLTh) (the chosen one is L2 with "Tint of 1"), and the earlier [Palette Directions](https://claude.ai/artifact/UfcDVwLjKpmY4TmHjbsHtk).

## Type (D-11, D-12)

Never a system font. The system uses **Asap** and **Asap Condensed**, bundled with each app.

| Width | Used for | Weights |
| --- | --- | --- |
| Asap | text and figures: titles, amounts, body, buttons | 400, 500, 700, each with its italic |
| Asap Condensed | the small, dense parts: the 1-day lines, the bottom bar's labels, the tabs inside a screen | 400, 500, 700, each with its italic |

Figures always use tabular digits, so amounts line up in columns.

The scale is figure-led (D-13): the summary card's big figure is the one large thing on a screen.

| Role | Size / line | Weight | Width |
| --- | --- | --- | --- |
| Screen title | 22 / 28 | 500 | Asap |
| Big figure (summary card) | 32 / 40 | 500 | Asap |
| Section heading | 16 / 24 | 500 | Asap |
| Body, row amounts | 16 / 24 | 400 | Asap |
| Small, secondary lines | 14 / 20 | 400 | Asap |
| Button | 14 / 20 | 500 | Asap |
| 1-day lines | 12 / 16 | 400 | Asap Condensed |
| Tabs, bottom bar labels | 14 / 20 | 500 (700 selected) | Asap Condensed |

Sizes are sp and follow the phone's font size. If the 32 figure wraps badly at the largest font on the phone, 28 is the fallback. Renders: [Typeface Options](https://claude.ai/artifact/Rk67tepT4KUGrKiTBfu7Ey) (E), [Type Scale Options](https://claude.ai/artifact/5WK4n4pez5oruPDYX5NBys) (B).

## Shape (D-14)

One corner radius, **8**, for cards, buttons and the selected tab's pill. Renders: [Corner Shape Options](https://claude.ai/artifact/9csR8YHuPs4QcYu1RJUaMa) (C).

## Spacing (D-15)

Tight, so more rows fit on a screen.

| Distance | dp |
| --- | --- |
| Screen side margin | 12 |
| Gap between cards | 8 |
| Padding inside a card and its rows | 10 |
| Smallest tappable row or touch area | 48, whatever the padding |

Renders: [Spacing Options](https://claude.ai/artifact/HYC6y4oirQucvyBp4n5xWu) (B).

## Components (D-17)

Drawn with their states, light and dark, on the [Component Catalog](https://claude.ai/artifact/PyQfKJ8reXrvVpzyB6zuy1).

| Component | Sizes and rules |
| --- | --- |
| Button | Filled (gold, the one main action), outline, text. Large 48 (Asap 16, padding 24), medium 40 (Asap 14, padding 20, default), small 32 (Asap Condensed 14, padding 12; 48 touch area, 8 dp free around it). |
| Text field | Outlined, 56 tall. Focus: 2 dp outline in navy 10 (light) or gold (dark). Refused: 2 dp bronze outline, Lucide's `circle-alert` in bronze (20 dp; 16 in a dense field), message (D-16, D-27). |
| Text field, dense | 40 tall in a 48 touch area, 120 wide, Asap 14, padding 8, label beside it, a refusal's message right-aligned under it (D-18). |
| Pick field, dense | 40 tall, Asap 14, a 20 dp chevron-down at its end, label beside it; opens its list inside the window, like the ⋮ menu (D-30; first use: the ticker filter on Cash). |
| Pick field, standard | 56 tall like the text field, label on the outline, a 24 dp chevron-down (D-31; the split's portfolio and ticker). |
| Amount field | The standard text field with a "$" before the number in the secondary text color; typed without it (D-31). |
| Ratio | Two small dense fields, at least 64 and sized to their content, with "for" between (D-31; the split). |
| One-at-a-time choice | The filter chips with the check on the picked one (D-31; cash entry type, an adjustment's direction). |
| Review and detail rows | Label-and-value rows in a card, label in secondary text; the result (cash effect, shares) a total line under a divider in medium weight; links to other entries are tappable rows with a chevron (D-31). |
| Blocked save | One card at the top of the form: 2 dp bronze outline, circle-alert, "Not saved" in medium bronze, the reason in normal text; a field's own rule stays on the field (D-16, D-27, D-31). |
| Derived value | A plain line in secondary text under the fields it comes from (price per share), never a field (D-32). |
| Price state | "no price" in secondary text in place of a gain; a stale price's age in Asap Condensed 12 under the gain (D-32). |
| Nothing to enter | A card with the reason in medium and an outline button where one exists ("Set the opening balance first") (D-32). |
| Notice banner | A card with Lucide's info icon (20) in the icon color and the text in 14, at the top of a screen; never bronze, which is for errors (D-33). |
| Original value | Under a field changed from a prefilled original, as the field's note line in regular 12, secondary ("Original: 10") (D-33). |
| Combined review | One card per part, headed in medium 16, each ending in its total line; the combined result a card of its own with one total line, uncolored (D-33). |
| Long review value | Under its label at full width instead of on the right (D-33). |
| Entry screen foot | The large full-width gold button (Review, Confirm and save); Back to edit a large outline button under it; Cancel a text button (D-31). The selected tab stays where the flow opened. |
| Date field | The standard or dense text field with a calendar icon (24 or 20) and the New York note under it; it opens the picker of its own size (D-18). The dense date field is sized to its content: at least 120, never wrapping; when a row has no room, the next field moves under it (D-28). |
| Date picker | Inside the window over a scrim. Standard: cells 40, Cancel and a confirm button. Dense: cells 28, one 48 header row (‹ month › ✕), tap a day to pick and close. Days 48 apart each way; edge to edge on narrow phones (D-18). |
| Checkbox | Standard 18 dp, 2 dp outline, Asap 16; dense 16 dp, 1.5 dp outline, Asap 14. Corner 4, the one exception to radius 8. Checked: navy 9 (light), gold (dark). Rows at least 48 (D-18). |
| Row, standard | Padding 10, body 16, as many lines as needed; at least 48 tall when tappable. |
| Row, dense | One line, Asap 14 with the gain in Asap Condensed 12 (dollars, then the percentage in brackets); 48 tall when tappable, 36 when read-only. |
| Filter chip | 32 tall with a 48 touch area; selected: navy 9 with white (light), gold with navy 10 (dark), plus a check. |
| Tabs in a screen | Asap Condensed 14 medium; selected in button-text with a 3 dp line. |
| Bottom bar | Three variants, each app picks one: text only (56), icon and text (72; in light one pill covers icon and label), icon only (56; tab names for TalkBack). 24 dp line icons (D-07, D-10, D-18). ATS uses icon and text (D-37); its light pill is one size on every tab, at least 84 wide. |
| Title band, cards | Band (light), summary card with the 32 figure, plain card: no border, shadow (D-08). |
| Dialog, ⋮ menu | Drawn inside the window over a scrim, never as a separate window; on the card tint (light) or navy 8 (dark). Menu items 48 tall. A dialog: title in 22, guidance as the field's note line, actions at the bottom right (Cancel a text button, the one main action filled gold at 40), no card inside it, a refused field shown on the field, a blocked action as the blocked card inside a dialog titled "Can't …" with Close (D-34). |
| Warning line | Medium 14 with Lucide's triangle-alert (20) in the icon color, for a consequence before it happens; never bronze (D-34). |
| Disabled button | The catalog's 38% with a solid outline, for every button that can't be tapped yet; TalkBack reads it as unavailable and a note line says why (D-34). |
| Wizard step | A line under the band in Asap Condensed 14 medium, secondary ("Step 1 of 5 · Nothing is changed until step 5."); no progress bar. The foot as an entry screen's: the step's action large gold, Cancel a large text button (D-35). |
| Progress row | 48 tall: Lucide check when done, loader-circle turning while it runs (still under reduced motion), the state in words (D-35). |
| Report list | The blocked card with the count, then a card with one row per item, dividers between, the reference (line, column) in medium (D-35). |
| Comparison table | Two right-aligned number columns under Asap Condensed 14 heads, labels in secondary (D-35). |
| Done heading | Lucide's circle-check in the icon color beside a medium 16 heading; never green, which is for gains (D-35). |
| Radio button | A 20 circle with a 2 outline; selected adds a 10 dot; both in the check color (navy 9 light, gold dark). Rows 48 tall, "default" tagged in Asap Condensed 12. A choice that only saves applies on tap (dialog with Close only); one with side effects confirms with a gold button and Cancel (D-36). |
| Icon button | 48 square, a 24 Lucide icon in the button color, no outline, a TalkBack label naming the action ("Move Cash up", "Settings"); disabled at 38% (D-36). |
| Donut | 168 across, a 30 ring, small gaps between slices; a slice of 5% or more labelled in Asap Condensed 12 medium in slice-label; rows 48 with a 16 round swatch, a slice under 5% with its percentage after the amount (D-38). |
| Settings row | In a card under a medium 14 secondary section heading: the name in 16, the current value under it in secondary 14, a chevron (D-36). |

Pressed adds a 10% layer of ink (white in dark). Disabled is 38% opacity. A lot that can't be picked: dashed outline in the secondary text color, the lot at 55%.

Screens drawn in the system: [Cash Screens](https://claude.ai/artifact/PrbnvN5217JFsweCaGkvRV) (D-30), [Cash Entry and Split](https://claude.ai/artifact/Wt5C2VUkY9TQByP1Lmyduh) (D-31), [Buy and Sell](https://claude.ai/artifact/4LDeM2aWcwCaKGsBm2URKf) (D-32), [Reverse and Re-enter](https://claude.ai/artifact/T7eEgYGjsDjsvDTojJpjbg) (D-33), [Portfolio Dialogs](https://claude.ai/artifact/EfCBaETZKt7DBXHMhjRXtY) (D-34), [Backup and Restore](https://claude.ai/artifact/VLt18yvg76WArx4moGR8eT) (D-35), [More and Settings](https://claude.ai/artifact/LFs46bhAGvY7UfdZg2vUpj) (D-36), [Overall Screen](https://claude.ai/artifact/3QYic8PcgfcgRQSxvxJSRd) (D-38), [Net Worth Screen](https://claude.ai/artifact/VAYLNxh3hRE7kgrorEfaCB) (D-39), [Portfolios Screens](https://claude.ai/artifact/JV1RRB1JvCqD5TyVuNdzZG) (D-40), [Allocation Screen](https://claude.ai/artifact/3Kd86NqSWQg2NAPsmSQUgN) (D-41), [Realized Gains Screen](https://claude.ai/artifact/4D8Pb6SoGjo5H9tsC1TAz2) (D-42), [Holding Detail Screen](https://claude.ai/artifact/GLQhsurvrXbYF1UyZx2Eey) (D-43), [Transaction History Screen](https://claude.ai/artifact/6tgGQwDs3Boc4rcowiKgzf) (D-44), [Hidden Holdings and About](https://claude.ai/artifact/RCtEh4hydcjAQjyHTLs1Ew) (D-45), [Remaining Screens](https://claude.ai/artifact/3edcZUwkggaRdoGkbSnz6C) (the lot picker, an entry screen with the keyboard at 200% font, the date field, lock and cover).

## Icons (D-19)

**Lucide**, open source: ISC, plus MIT for the icons taken from Feather; both notices ship with each app. Drawn at 24 dp with Lucide's own stroke of 2 (D-24; the `icon-stroke` token), in the label color (bottom bar) or button-text color (screens), gold when selected. Lucide has no filled forms, so a selected tab shows by color and the light pill. The SVGs sit in the repo at `design/icons/lucide/`, unchanged from a pinned Lucide release, with the licence notices; Claude Code writes them into Figma as vector components, and the same files become the Android vector drawables, so Figma and the phone show the same drawings (D-26).

| Use | Icon |
| --- | --- |
| Overall, Portfolios, Cash, More | pie-chart, briefcase, wallet, more-horizontal (in Lucide 1.72.0: chart-pie, briefcase, wallet, ellipsis) |
| Date field | calendar |
| Back, menu | chevron-left, ellipsis-vertical |
| Check, lock, refresh, settings | check, lock, refresh-cw, settings |

ATS pins Lucide 1.72.0 and uses 24 icons; design/icons/icons.json lists each with its file in that release, the name the drawings use and where it is used (D-47).

These are first picks; an app may choose other Lucide icons. Renders: [Icon Set Options](https://claude.ai/artifact/LWrVaF66trqGnYyCS8vbQB) (D).

## Rules already fixed by the spec

These come from spec.md section 6. The design system follows them and can't change them without a spec change.

- **Color meaning.** Green means a value gain or a price increase, and red means a loss or a decrease. Zero, cash figures, net worth, Investments, cost and allocation shares are never colored. Color never carries meaning alone: a sign, a word or a symbol always goes with it.
- **Slices.** An allocation slice is never green or red, and cash always has its own slice color. Every row names its slice.
- **Contrast.** Text at least 4.5:1, large text and control outlines at least 3:1, in both themes.
- **Touch targets.** At least 48 dp.
- **Font size.** Text follows Android's font size up to the largest setting. Nothing is cut off; screens scroll instead.
- **Numbers.** One fixed US format: `$10,880.00`, `−$200.00`, `+$800.00`, 1-decimal percentages (2 for the 1-day and unrealized percentages).

## Using the color tokens

- `gain` and `loss` color a gain/loss figure only, with its sign. The percentage in brackets takes the dollar figure's color.
- `slice-cash` is cash's slice in every breakdown. `slice-1` to `slice-6` go to the other slices in turn, counted without cash, so a portfolio keeps its color whether cash is listed or not.
- A percentage drawn on a slice is white in the light theme and black in the dark theme (`slice-label`).
- `gain-on-summary` and `loss-on-summary` replace `gain` and `loss` on the summary card only, where the field is dark in both themes.
- `gold` fills the one main action on a screen; `button-line` and `button-text` draw the others. In dark, `band` equals `page`, so no band shows.
- `error` (bronze brown) marks a refused field only: its 2 dp outline, its `circle-alert` icon (D-27) and its message, in medium weight. Never red, which stays for losses (D-16).

## Token files

In the ATS repository under `design/` (D-21), split by what changes with the mode. A repository of its own follows once this is done. The three token files were made in iteration 44a, step 3 (2026-11-27), and app's unit test TokenFilesTest checks them: light and dark hold the same tokens (D-04), each color's hex matches its components, every reference resolves, and the values spec 6 names are the ones here. The icons are in design/icons/ since step 3 (D-47), and the fonts in app/src/main/res/font/ with design/fonts/fonts.json and OFL-asap.txt since step 4. From step 4 the build generates the app's theme from these files (app/build.gradle.kts, DesignTokensKotlin), and ThemeTokensTest checks it. The Figma set is not made yet.

| File | Format | Holds | Figma collection |
| --- | --- | --- | --- |
| `design/tokens/color.light.tokens.json` | W3C design tokens (DTCG 2025.10) | colors and the card shadow, light | Color, mode Light |
| `design/tokens/color.dark.tokens.json` | W3C design tokens (DTCG 2025.10) | colors and the card shadow, dark | Color, mode Dark |
| `design/tokens/base.tokens.json` | W3C design tokens (DTCG 2025.10) | spacing, radius, type: the same in both modes | Base, one mode |
| `design/figma/corporate.figma.json` | the Figma set, shaped like Figma's own model, with the variable IDs | both collections with scopes and code syntax, plus text and effect styles (the shadow as a light and a dark style) | all |
| `design/icons/lucide/*.svg`, `LICENSE-lucide`, `icons.json` | SVG as Lucide ships it | the icons in use, their licences, and which icon serves where (D-26) | Icons page, vector components |

Colors are written as DTCG objects (`colorSpace` "srgb", `components` 0 to 1, a 6-digit `hex` fallback), sizes in px (dp on Android, one to one). The two sets hold the same values, and a check keeps them equal (D-04). The repo leads (D-22): a change starts in these files, and Claude Code writes the Figma set into Figma through Figma's MCP server on one prompt, every write asked first (D-23); you approve the look in Figma and the files by pull request. Claude Code writes both. When they really disagree (a different value, a missing token, a different type), it stops and you decide; neither set wins on its own (D-05). The interview record is in the Decisions section.
