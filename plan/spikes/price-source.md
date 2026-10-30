# plan/spikes/price-source.md

Stage: Build. Status: outline approved 2026-10-30 with decision 1 (section 6); running: the US exchange list looked up (section 10.1), the sources not yet checked. Branch spike-price-source.
Source: plan/plan.md section 7 (the spike's question list), spec/spec.md v4 (final): 4.3 (the ticker check), section 7 (prices), section 9 (security), concerns C1, C8 and C14; intent/intent.md v52 (final).
Facts in this note are facts of the day they were looked up; each carries its date.

How sources are labelled in this note:
- "you read": text you copied from an official page and pasted into the chat. I did not see the page.
- "I fetched": page text returned by my own search or fetch in this chat, each approved by you before it ran.
- "I called": a response to a call I made from this PC to a source, each approved by you before it ran.
- "not checked": my own knowledge or reading. It is not a fact until a look-up or call shows it.
Third-party pages (blogs, forums, comparison sites) are not used as facts.

## 1. Outcome
A decision note: which free source or sources iteration 09 uses, what they really return, and what the ticker check of spec 4.3 can rely on. No app code. Iteration 09 can start once this note is finished and merged.

## 2. Gate
This is a decision note, not an iteration. Iteration 09 cannot start until the questions in section 3 are answered, the decisions in section 6 are settled, and the results are written into section 10 (plan.md section 7; open item 17).

## 3. Questions
1. Which free source or sources: limits (calls per minute and per day), terms (use in a personal app, attribution), and whether a key is needed.
2. Per source, what a look-up returns: the price and its time, the delay, the instrument type (stock, ETF, mutual fund), the exchange name, the currency, sector, geography and the previous close.
3. The exchange names and aliases each source really reports, for AAPL, SPY, VTI, QQQ, BRK-B and one OTC ticker, plus one mutual fund and one listing not in USD to test the refusals; compared with the US exchange list taken from the SEC's list of registered national securities exchanges, looked up on the day (decision 1). Includes the risk of refusing a valid ticker and the fallback "refuse only clearly OTC or non-US names".
4. Whether the source can sit behind K4 so it can be replaced, and the list of sources the settings screen shows (iteration 30).
5. Sector and geography: if no source supplies them, iteration 23 is reduced to "Unclassified" plus the override path, and the change is recorded in plan.md.
6. The key, if a source needs one: where it would live. Spec section 9 (security) and section 8 (the backup file) say nothing about it, and section 7 assumes the app ships with its sources. A need for a key is a spec gap, flagged here and decided with you; it may need a spec change with your acceptance.

## 4. Method
- Every look-up is shown to you first and approved, one at a time: the tool, the query or URL, and the reason.
- First the official pages (the SEC's list; each candidate's own documentation, limits and terms), or pages you paste.
- Then, per candidate kept, a few real calls from this PC (curl) for the test tickers, each approved. A call that needs a key needs you to sign up and to give me the key in a way we agree; the key is never written to the repo or to this note.

## 5. Candidates (all "not checked")
From my own knowledge, to be checked: Finnhub, Alpha Vantage, Twelve Data, Financial Modeling Prep, Polygon, Tiingo, Stooq, and the unofficial Yahoo Finance endpoints. Today's free tiers and terms are what the spike checks.

## 6. Decisions (all Build)
1. The US exchange list comes from the SEC's list of registered national securities exchanges, looked up on the day (settled by you 2026-10-30, option B). Looked up 2026-10-30: 29 names under Section 6(a) (section 10.1).
2. The default source.
3. The list of sources the app ships with (spec 7, iteration 30).
4. The exchange alias table kept in plan.md (spec 4.3).
5. The key handling, or a proposed spec change.

## 7. Out of scope
No app code, no network permission in the app, no historical prices (spec 7: before the performance milestone, not before v1).

## 8. Risks
- Limits and terms can change after the check.
- An unofficial endpoint can break without notice.
- A key built into a sideloaded app can be read out of the APK.
- Tickers leave the device (accepted by spec 7: "Sending tickers to an outside source is accepted").

## 9. Proof
The raw responses for each test ticker, with the date, summarised in section 10. Nothing secret is recorded.

## 10. Results
### 10.1 The US exchange list (decision 1; question 3)
I fetched, 2026-10-30, two fetches of https://www.sec.gov/divisions/marketreg/mrexchanges.shtml ("National Securities Exchanges", "Last Reviewed or Updated: Jan. 7, 2026"), found by a web search for the SEC's own page (only the query and the URL left this PC). Names and former names as the fetch returned them from the page.
Registered under Section 6(a) as national securities exchanges: 29 names. The count matches plan.md section 7's "29-name US list in the hand-off", which is not in the repo; that the two lists are the same is my reading, not checked.

| # | Name | Former names shown on the page |
|---|---|---|
| 1 | 24X National Exchange LLC | none |
| 2 | BOX Exchange LLC | BOX Options Exchange LLC |
| 3 | Cboe BYX Exchange, Inc. | Bats BYX Exchange, Inc.; BATS Y-Exchange, Inc. |
| 4 | Cboe BZX Exchange, Inc. | Bats BZX Exchange, Inc.; BATS Exchange, Inc. |
| 5 | Cboe C2 Exchange, Inc. | none |
| 6 | Cboe EDGA Exchange, Inc. | Bats EDGA Exchange, Inc.; EDGA Exchange, Inc. |
| 7 | Cboe EDGX Exchange, Inc. | Bats EDGX Exchange, Inc.; EDGX Exchange, Inc. |
| 8 | Cboe Exchange, Inc. | none |
| 9 | Green Impact Exchange, LLC | none |
| 10 | Investors Exchange LLC | none |
| 11 | Long-Term Stock Exchange, Inc. | none |
| 12 | MEMX, LLC | none |
| 13 | Miami International Securities Exchange | none |
| 14 | MIAX Emerald, LLC | none |
| 15 | MIAX PEARL, LLC | none |
| 16 | MIAX Sapphire, LLC | none |
| 17 | MX2 LLC | none |
| 18 | Nasdaq GEMX, LLC | ISE Gemini |
| 19 | Nasdaq ISE, LLC | International Securities Exchange, LLC |
| 20 | Nasdaq MRX, LLC | ISE Mercury |
| 21 | Nasdaq PHLX LLC | NASDAQ OMX PHLX, LLC; Philadelphia Stock Exchange |
| 22 | Nasdaq Texas, LLC | Nasdaq BX, Inc.; NASDAQ OMX BX, Inc.; Boston Stock Exchange |
| 23 | The Nasdaq Stock Market | none |
| 24 | New York Stock Exchange LLC | none |
| 25 | NYSE Arca, Inc. | none |
| 26 | NYSE Texas, Inc. | NYSE Chicago, Inc.; Chicago Stock Exchange |
| 27 | NYSE American LLC | NYSE MKT LLC, NYSE Amex, NYSE Alternet US, and the American Stock Exchange |
| 28 | NYSE National, Inc. | National Stock Exchange, Inc. |
| 29 | Texas Stock Exchange LLC | none |

The page also lists 7 exchanges notice-registered under Section 6(g) for trading security futures, and 2 exempted on the basis of limited transaction volume (Arizona Stock Exchange; SWX Europe Limited), both no longer operating under the exemption. Neither group is a national securities exchange under 6(a), so neither is part of the 4.3 list.
Not shown by the fetch: which of the 29 list stocks or ETFs (several trade options only); the short names and codes a price source reports (for example "NASDAQ", "NYSE ARCA", "BATS"). Both are for the alias table (decision 4), from the calls to the sources.

## 11. Status and notes
Outline approved 2026-10-30. Running: the US exchange list looked up 2026-10-30 (section 10.1); the sources not yet checked.
