# example-backup-expected.md

Results that `spec/example-backup.csv` (format version 1) should give after a restore.
Companion to the sample file (a CSV cannot hold notes). Used as the fixture for criteria 4 and 25.

How these figures were made: a throwaway script read the file the way spec.md section 8 describes
and replayed it, and the same figures were checked by hand. The app has not been built, so none of
this is output from the app. Prices are not in the file, so no market values are listed.

## Restore summary (section 8, step 5)
| Item | In the file |
|---|---|
| Portfolios | 2 (Main, Retirement, in this order) |
| Transactions | 8: 4 buys, 2 sells, 1 split, 1 reversal of a buy |
| Cash entries | 4: 3 cash rows and 1 reversal of a cash entry (trade cash lines are not counted) |
| Resulting cash balance | $9,425.00 |
| Holdings with shares | 1 (AAPL in Main) |

## After the restore
| Item | Expected |
|---|---|
| Rebuilt by the app | 7 trade cash lines (4 buys, 2 sells, 1 reversal of a buy) |
| Lowest cash balance on any date | $7,300.00 (Feb 10) |
| AAPL in Main | 16 shares, cost $1,200.00 ($75.00 per share after the 2:1 split), one lot with shares (L2). L1 is sold out. L3 is reversed to 0 shares. |
| OLDCO in Main | 0 shares, hidden. Listed in view 9 (Hidden holdings). |
| Retirement | no holdings |
| Realized gains | Mar 1 AAPL +$800.00 (cost $1,300.00); Mar 10 OLDCO -$200.00; total $600.00 |
| Realized gains, range Mar 1 to Mar 1 | $800.00 |
| Realized gains, range Apr 1 to Apr 30 | no sale |
| Net worth before the first price refresh | $10,625.00 ($9,425.00 cash + AAPL at cost, flagged "no price") |
| AAPL sector and geography | Technology, United States (the override) |
| Sector list | Diversified, Technology |
| Geography list | Global, United States |
| Remembered source text | "Information Technology" maps to Technology |
| Settings | auto-lock 5 minutes; tab order Overall, Portfolios, Cash, More; price source = the app's default |
| Last backup | "never backed up"; the Overall banner shows |
| Ticker verification (needs internet) | AAPL is verified. OLDCO is sold out, so it is not. |

## Facts the file is built to test
- The reversal of L3 (Apr 5) comes after the 2:1 split (Apr 1). It removes 4 shares (2 as traded x 2) and returns $360.00 (Example F rule, 4.6).
- The reversal of D1 (Apr 21) adds a cash entry of -$500.00 with the note of 4.6; the pair leaves the cash balance unchanged.
- A sell with zero proceeds (OLDCO, Mar 10) is a total loss: realized -$200.00, the cost of lot O1.
- The `hidden` row is what hides OLDCO. Without it OLDCO would stay in the holdings list with 0 shares.
- The file has no `price_source` row, so the default source applies.
