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
Call 1, I called 2026-10-30 14:13:53 +08:00, GET https://finnhub.io/api/v1/stock/symbol?exchange=US with your free key in the X-Finnhub-Token header (the key is a user environment variable on this PC; it was never printed or written down). Only counts and the test tickers' rows were printed; the list was not saved.
- The free key gets the list: 31,116 rows. Fields of a row: currency, description, displaySymbol, figi, figiComposite, isin, mic, shareClassFIGI, symbol, symbol2, type.
- mic, with counts: OOTC 17,628; XNAS 5,718; XNYS 2,953; ARCX 2,807; BATS 1,685; XASE 324; IEXG 1. Only these 7 occur; OTCM from the documentation sample does not.
- currency: USD 30,869; empty 247.
- type, with counts: Common Stock 18,405; ETP 6,608; ADR 2,238; empty 800; PUBLIC 669; Equity WRT 607; Closed-End Fund 513; Unit 431; REIT 420; Right 123; NVDR 52; Foreign Sh. 43; GDR 43; MLP 27; Preference 22; Ltd Part 21; Royalty Trst 20; CDI 20; Stapled Security 17; NY Reg Shrs 10; Receipt 8; Open-End Fund 5; SDR 5; Dutch Cert 3; Savings Share 2; PRIVATE 2; Tracking Stk 1; Canadian DR 1.
- Test tickers: AAPL mic XNAS, Common Stock, USD; SPY mic ARCX, ETP, USD; VTI mic ARCX, ETP, USD; QQQ mic XNAS, ETP, USD; BRK.B mic XNYS, Common Stock, USD; BRK-B not in the list (Finnhub writes the class with a dot); VFIAX (a mutual fund) not in the list.
- My reading, not checked: ETFs are typed "ETP"; open-end (mutual) funds are almost absent (5 rows, VFIAX missing), so in practice a mutual fund would be refused as "not found" rather than as "a mutual fund", which changes the reason 4.3's message names; AAPL reports XNAS here while the documentation sample shows XNGS; the 6 non-OTC MIC codes fit 6 of the 29 SEC names (XNAS The Nasdaq Stock Market, XNYS New York Stock Exchange LLC, ARCX NYSE Arca, Inc., BATS Cboe BZX Exchange, Inc., XASE NYSE American LLC, IEXG Investors Exchange LLC), to be confirmed against the ISO MIC list; OOTC is the OTC market and is refused; types such as ADR, Closed-End Fund, REIT and the 800 empty types pass 4.3 as written (not a mutual fund, US exchange, USD) and are a question for the spec's "stocks and ETFs" scope, not for this note to decide.
Batch 2, I called 2026-10-30 14:18:23 to 14:18:31 +08:00 (02:18 New York, market closed), six calls with the same key handling; raw responses printed, nothing saved.
- a) The US list again; the first OTC row by symbol with type Common Stock: AABB, mic OOTC, USD, "ASIA BROADBAND INC". Used as the OTC test ticker.
- b) /quote?symbol=AAPL: {"c":348.91,"d":5.08,"dp":1.4775,"h":351.43,"l":347.3,"o":348,"pc":343.83,"t":1793304000}. t = 2026-10-29 16:00:00 New York, the close of the previous session; 37,109 s before the call.
- c) /quote?symbol=AABB: {"c":0.0089,"d":-0.0005,"dp":-5.3191,"h":0.0095,"l":0.0083,"o":0.0092,"pc":0.0094,"t":1793232000}. t = 2026-10-28 20:00:00 New York (midnight UTC); 109,109 s before the call.
- d) /quote?symbol=BRK.B: {"c":541.96,"d":1.62,"dp":0.2998,"h":548.7694,"l":541.8,"o":545.295,"pc":540.34,"t":1793304000}. The dot form works for quotes too.
- e) /stock/profile2?symbol=SPY: {} (empty).
- f) /stock/profile2?symbol=AABB: "exchange":"OTC MARKETS", "currency":"", "finnhubIndustry":"N/A", "country":"US"; also fields the documentation does not list ("estimateCurrency", "floatingShare").
- My reading, not checked: the free key gets quotes, including for an OTC name; t is the time of the last price in Unix seconds, so spec 7's timestamp is available; whether free quotes are real-time or delayed cannot be told outside market hours and needs one call during a session; profile2 is empty for an ETF, so the sector and geography of ETFs cannot come from it (C14 expected this); profile2's exchange is free text ("OTC MARKETS") and its currency can be empty, so 4.3 is better served by the list's mic, type and currency; prices come with up to 4 decimals (548.7694, 0.0089), so the stored price must be an exact decimal with more than 2 places (4.10).
Batch 3, I called 2026-10-30 14:21:59 to 14:22:01 +08:00 (market closed), at your question whether ETF quotes work; same key handling, raw responses printed, nothing saved.
- /quote?symbol=SPY: {"c":769.36,"d":-2.95,"dp":-0.382,"h":770.76,"l":766.62,"o":768.79,"pc":772.31,"t":1793304000}.
- /quote?symbol=VTI: {"c":378.24,"d":-0.93,"dp":-0.2453,"h":378.93,"l":376.83,"o":377.5,"pc":379.17,"t":1793304000}.
- /quote?symbol=QQQ: {"c":715.34,"d":-10.27,"dp":-1.4154,"h":718.07,"l":711.62,"o":716.54,"pc":725.61,"t":1793304000}.
- Each t = 2026-10-29 16:00:00 New York, the close of the previous session. So the free key prices ETFs on NYSE Arca and Nasdaq, although the quote page names "US stocks"; real-time or delayed is still not shown.
Batch 4, I called 2026-10-30 14:23:01 to 14:23:06 +08:00 (market closed), at your request to try SQQQ (a leveraged inverse ETF); same key handling, nothing saved.
- The US list: SQQQ mic XNAS, type ETP, USD, "PROSHARES ULTRAPRO SHORT QQQ".
- /quote?symbol=SQQQ: {"c":37.83,"d":1.55,"dp":4.2723,"h":38.4,"l":37.42,"o":37.645,"pc":36.28,"t":1793304000}; t = 2026-10-29 16:00:00 New York.
- A leveraged inverse ETF is typed ETP like any other ETF and passes 4.3 as written; the list does not tell leveraged or inverse ETFs apart.

## 11. Status and notes
Outline approved 2026-10-30. Running: the US exchange list looked up 2026-10-30 (section 10.1); the sources not yet checked.
