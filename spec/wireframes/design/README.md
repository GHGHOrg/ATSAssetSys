# Drawings (CR7)

Companion to spec/spec.md section 6, Visual design. Each file is a page of the Corporate Design System,
drawn in its colors, type, shapes, spacing and Lucide icons, light and dark, and accepted one at a time
(the decision in the table). The files were copied on 2026-11-28 from the pages as accepted and are
frozen: a file here is not edited again unless a change request reopens it (C29). The decisions
(D-numbers) are recorded in design/decisions.md, and the system as a whole in design/README.md.

Where a drawing and a wireframe in spec/wireframes/ differ in look, the drawing applies. Where they
differ in content or behaviour, spec.md section 6 and the wireframe apply. Every text in a drawing is a
placeholder (D9). From Build on, the token files in design/ hold the values; a drawing shows them, it
does not define them.

Each file opens in a browser with no other file. It loads Asap and Asap Condensed from Google Fonts;
offline the browser shows another font, and colors, sizes and icons are unchanged (the icons are drawn
inline from Lucide's path data). Nothing in these files depends on claude.ai. Drawn in HTML at 1 px per
dp, not rendered by Android: the phone checks of AC-73 decide what a browser can't show.

| File | Shows | Wireframe | Decision |
|------|-------|-----------|----------|
| 01-overall.html | Overall | 01 | D-38 |
| 02-net-worth.html | Net Worth (Total) | 02 | D-39 |
| 03-portfolios.html | Portfolios and a portfolio's screen | 03 | D-40 |
| 04-allocation.html | Allocation, one portfolio | 04 | D-41, D-46 |
| 05-realized-gains.html | Realized gains | 05 | D-42 |
| 06-holding-detail.html | Holding detail | 06 | D-43, D-44 |
| 07-cash.html | Cash account | 07 | D-29, D-30 |
| 08-transaction-history.html | Transaction history | 08 | D-44 |
| 09-11-hidden-holdings-about.html | Hidden holdings, About | 09, 11 | D-45 |
| 10a-buy-sell.html | Buy and sell entry | 10a | D-32 |
| 10b-split-cash-reversal.html | Cash entry, split, detail and reversal | 10b | D-31 |
| 10c-reverse-reenter.html | Reverse and re-enter | 10c | D-33 |
| 10d-portfolio-dialogs.html | Portfolio create, rename, delete | 10d | D-34 |
| 10e-backup-restore.html | Backup and restore | 10e | D-35 |
| 10f-more-settings.html | More and Settings | 10f | D-36 |
| component-catalog.html | Every component with its states | all | D-17 and on |
| remaining-screens.html | Lot picker, keyboard at 200% font, date field, lock and cover | 10a | D-18 |

Every screen drawing has the bottom bar with an icon and a name on each tab (D-37).
