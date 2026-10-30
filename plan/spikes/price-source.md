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

### 10.2 Finnhub
Official pages not readable by my tools (2026-10-30): a web search limited to finnhub.io found the pricing page; I fetched https://finnhub.io/pricing and https://api.finnhub.io/pricing, and both returned only the title and the tagline "Real-time stock prices, Company fundamentals, Estimates, and Alternative data." The search summary's figures were not used as facts.
Pricing page, you read 2026-10-30 (https://finnhub.io/pricing, pasted):
- Free: "$0/month"; "License: Personal Use. Terms apply"; "Limit: 60 API calls/minute".
- Fundamental Data: "Coverage: US"; "Company Profile: v2" (All-In-One: "v1 with more information").
- US Market Data: "Coverage: US"; "Websocket: 50 symbols".
- ETFs and Indices: "Coverage: US". Mutual Funds: no coverage shown for Free (All-In-One: "Global").
- Footnote: "LSE data is delayed by 15 minutes. Other international markets support end-of-day data only."
- Not shown by the paste: the rows whose cells are marks rather than text (for example quotes, "ETFs Profile", "Mutual Funds Profile", "SEC filings") came through empty in both columns, so which of them the Free plan includes is not known; whether Free US prices are real-time or delayed; the terms behind "Terms apply". The All-In-One plan ($3500/month) is out of scope (free tier only, spec 7).
Company Profile 2 page, you read 2026-10-30 (https://finnhub.io/docs/api/company-profile2, pasted):
- "Get general information of a company. You can query by symbol, ISIN or CUSIP. This is the free version of Company Profile." GET /stock/profile2?symbol=AAPL.
- Response attributes as the page words them: country "Country of company's headquarter."; currency "Currency used in company filings."; exchange "Listed exchange."; finnhubIndustry "Finnhub industry classification."; ipo; logo; marketCapitalization; name; phone; shareOutstanding; ticker "Company symbol/ticker as used on the listed exchange."; weburl.
- Sample response for AAPL: "exchange": "NASDAQ/NMS (GLOBAL MARKET)", "currency": "USD", "country": "US", "finnhubIndustry": "Technology".
- My reading, not checked by a call: there is no instrument type field, so this call alone cannot tell a stock from an ETF or a mutual fund (4.3); currency is the filing currency, not the trading currency, so it does not answer "quoted in USD" (4.3); the exchange is a free text such as "NASDAQ/NMS (GLOBAL MARKET)", not one of the SEC names of 10.1, so an alias table is needed (decision 4); finnhubIndustry is an industry, which could feed the sector (4.9), and country is the headquarters, which is not necessarily the geography the user means (4.9). The page does not say what profile2 returns for an ETF or a fund.
Quote page, you read 2026-10-30 (https://finnhub.io/docs/api/quote, pasted):
- "Get real-time quote data for US stocks. Constant polling is not recommended. Use websocket if you need real-time updates." "Real-time stock prices for international markets are supported for Enterprise clients via our partner's feed." GET /quote?symbol=AAPL.
- Response attributes: c "Current price"; d "Change"; dp "Percent change"; h "High price of the day"; l "Low price of the day"; o "Open price of the day"; pc "Previous close price".
- Sample response: "c": 261.74, "h": 263.31, "l": 260.68, "o": 261.07, "pc": 259.45, "t": 1582641000.
- My reading, not checked by a call: the price comes with a previous close (the later price-change idea, plan.md section 7); the sample has a "t" field the attribute list does not name, probably a Unix time in seconds, which would give the price timestamp spec 7 asks for; the page shows no plan marker, so whether the Free plan gets quotes, and real-time or delayed, is not shown here; prices are JSON numbers, so the app must read them as exact decimals, never as floating point (4.10).
Symbol Lookup page, you read 2026-10-30 (a screenshot of Finnhub's documentation; its address was not shown):
- "Search for best-matching symbols based on your query. You can input anything from symbol, security's name to ISIN and Cusip." GET /search?q=apple&exchange=US; arguments q (required), exchange (optional, "Exchange limit.").
- Response: count; result with description, displaySymbol, symbol, type "Security type.". Sample for "apple": AAPL, AAPL.SW, APC.BE and APC.DE, each "type": "Common Stock".
Stock Symbol page, you read 2026-10-30 (a screenshot of Finnhub's documentation; its address was not shown):
- "List supported stocks." GET /stock/symbol?exchange=US, optionally &mic=XNYS; arguments exchange (required), mic ("Filter by MIC code."), securityType ("Filter by security type used by OpenFigi standard."), currency ("Filter by currency.").
- Response attributes: currency "Price's currency. This might be different from the reporting currency of fundamental data."; description; displaySymbol; figi; isin ("only available for EU stocks and selected Asian markets. Entitlement from Finnhub is required"); mic "Primary exchange's MIC."; shareClassFIGI; symbol; symbol2; type "Security type.".
- Sample: UPOW "mic": "OTCM", AAPL "mic": "XNGS", EXCOF "mic": "OOTC"; each "currency": "USD", "type": "Common Stock".
- Neither page shows a plan marker.
- My reading, not checked by a call: the Stock Symbol list gives what 4.3 needs in one place: the price's currency (not the filing currency of profile2), the primary exchange as an ISO MIC code (XNGS for AAPL; OTCM and OOTC for OTC names), and the security type. A MIC code is a stable code, so the alias table (decision 4) could map MIC codes of the 29 exchanges rather than free-text names, and OTC MICs are refused plainly. Which type values mark an ETF or a mutual fund (OpenFIGI's list) is not shown. The US list is long: whether to fetch it once and keep it, or to look one symbol up with the search call, is a design question for 09.

## 11. Status and notes
Outline approved 2026-10-30. Running: the US exchange list looked up 2026-10-30 (section 10.1); the sources not yet checked.
