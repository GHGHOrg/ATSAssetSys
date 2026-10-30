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
From my own knowledge, to be checked: Finnhub, Alpha Vantage, Twelve Data, Financial Modeling Prep, Polygon, Tiingo, Stooq, and the unofficial Yahoo Finance endpoints. Today's free tiers and terms are what the spike checks. All eight were checked on 2026-10-30 (sections 10.2 and 10.4 to 10.10).
Added 2026-10-30 at your request, from my own knowledge, not checked and not looked up (your decision: listed now, checked later): Alpaca (Market Data API), EODHD (EOD Historical Data), Marketstack (apilayer), Nasdaq Data Link (formerly Quandl), Tradier. Left out: IEX Cloud (a page title on alphavantage.co named its shutdown) and paid-only vendors (spec 7: free tier only).

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
Call 5, I called 2026-10-30 14:27:55 +08:00, /stock/profile2?symbol=AAPL, same key handling, nothing saved:
- {"ticker":"AAPL","name":"Apple Inc.","country":"US","currency":"USD","estimateCurrency":"USD","exchange":"NASDAQ NMS - GLOBAL MARKET","ipo":"1980-12-12","marketCapitalization":5092055.035903137,"logo":"…","shareOutstanding":14594.18,"finnhubIndustry":"Technology","phone":"14089961010","weburl":"https://www.apple.com/","floatingShare":14352.36} (the logo URL shortened here).
- My reading, not checked: for a stock, profile2 gives one industry word ("Technology"), which could be the source sector of 4.9, and the headquarters country ("US"), which could be the source geography; for an ETF it gives nothing (SPY: {}), so ETFs start as "Unclassified" plus the override (C14). The exchange text differs from the documentation sample ("NASDAQ NMS - GLOBAL MARKET" against "NASDAQ/NMS (GLOBAL MARKET)"), which supports using the list's MIC for 4.3, not this text.
Terms of service, I fetched 2026-10-30, https://finnhub.io/terms-of-service (the address you gave; a search limited to finnhub.io had not found it). Title "Terms of Service - Finnhub Stock API"; no "last updated" or effective date on the page. Quoted as the fetch returned them:
- Redistribution Rights and Personal Use: "All plan listed on Finnhub website is strictly for personal use unless explicitly stated otherwise." "Personal plan can't be used by any business even internally without a written approval." Disqualified if "You are using this data for your business or registering under your business name regardless of the industry." Restricted: to "redistribute or share access to data or derived results from the data obtained from Finnhub", except "without written approval from Finnhub".
- Intellectual Property: "All data must be deleted should your subscription to that data ends." Queries: "you agree that we can store the query in log files."
- API Limit and Access: "There is a 30 API calls/second limit on top of all plan's limit". "Your access to API will be revoked immediately should you choose to cancel your subscription."
- Failure to Comply ... and Termination: "may be terminated at any time, and without cause, by either Finnhub or the Owner and/or Subscriber"; "we may terminate your password or account or deny you access to all or part of the Service without prior notice".
- Not on the page: anything about API keys (sharing, building into an app), caching, display, or attribution.
- My reading, not legal advice: one person using the data in a personal app on their own phone fits "personal use"; the app must not share data or derived results with anyone else, so it must not send Finnhub prices out of the device: spec section 8 already keeps prices out of the backup file ("Not in the file: ... prices"), but the file does carry the sector and geography lists with their remembered source texts, which could include Finnhub's industry words, kept only by you; if you stop using Finnhub, its data kept by the app (prices, instrument data from it) must be deleted, which the app does not do today and spec 7's source change does not do either (it keeps the last price, marked stale); tickers you look up are stored in Finnhub's logs (spec 7 accepts sending tickers); access can end without notice, so the app must cope with a source that stops answering (spec 7's stale rule covers prices).

### 10.3 The MIC codes (ISO 10383; decision 4)
A web search limited to iso20022.org (only the query left this PC) found the registry's files. I downloaded two CSV files into a new folder in my scratchpad, outside the repo, and read them with Import-Csv only.
- ISO10383_MIC_NewFormat.csv (downloaded 2026-10-30 14:25:56 +08:00; HTTP Last-Modified 20 Oct 2022; 495,159 bytes; 2,553 rows; SHA-256 153968422E23BA90B61BD0A836E9D1588B8BB424CB3C83F2B6152ED32B613E18): its newest row is dated 2022-10-24, so it is an old copy and is not used below.
- ISO10383_MIC.csv (downloaded 2026-10-30 14:26:43 +08:00; HTTP Last-Modified 12 Oct 2026; 590,760 bytes; 2,889 rows; SHA-256 09A0D07B7B8348147453A2D8B18BC6638E3C5B16F749462582C9B1EDF0FD0E38). The rows for Finnhub's 7 codes:

| MIC | Operating MIC | Kind | Market name in the file | Legal entity | Country | Status | Last update | SEC name (10.1) |
|---|---|---|---|---|---|---|---|---|
| XNAS | XNAS | OPRT | NASDAQ - ALL MARKETS | NASDAQ, INC. | US | ACTIVE | 2026-04-27 | The Nasdaq Stock Market |
| XNYS | XNYS | OPRT | NEW YORK STOCK EXCHANGE, INC. | INTERCONTINENTAL EXCHANGE, INC. | US | ACTIVE | 2025-04-28 | New York Stock Exchange LLC |
| ARCX | XNYS | SGMT | NYSE ARCA | INTERCONTINENTAL EXCHANGE, INC. | US | ACTIVE | 2025-04-28 | NYSE Arca, Inc. |
| BATS | XCBO | SGMT | CBOE BZX U.S. EQUITIES EXCHANGE | (empty) | US | ACTIVE | 2018-06-25 | Cboe BZX Exchange, Inc. |
| XASE | XNYS | SGMT | NYSE AMERICAN LLC | INTERCONTINENTAL EXCHANGE, INC. | US | ACTIVE | 2025-04-28 | NYSE American LLC |
| IEXG | IEXG | OPRT | INVESTORS EXCHANGE | INVESTORS EXCHANGE LLC | US | ACTIVE | 2022-10-24 | Investors Exchange LLC |
| OOTC | FINR | SGMT | OTHER OTC | FINANCIAL INDUSTRY REGULATORY AUTHORITY, INC. | US | ACTIVE | 2024-05-27 | none: OTC, refused |

(XCBO, BATS's operating MIC: CBOE GLOBAL MARKETS INC., US, ACTIVE.) The 2022 copy showed XASE as "NYSE MKT LLC", a former name on the SEC page.
- My reading: the six non-OTC codes Finnhub reports are active US markets, each matching one of the 29 SEC names; the match of a MIC's market name to an SEC name is my reading of two different registries, not a published mapping. The other 23 SEC exchanges (options-only markets and others) did not occur in Finnhub's list on 2026-10-30.
- The downloaded copies stay in the scratchpad (not in the repo); deleting them needs your approval.

### 10.4 Twelve Data
A web search limited to twelvedata.com (only the query left this PC) found its pricing, documentation, support and refund pages; the search summary's figures were not used as facts.
Pricing page, I fetched 2026-10-30, https://twelvedata.com/pricing, quoted as the fetch returned it:
- Basic: "$0" "/month", "For testing and side projects"; "API requests: 8 (800 a day)"; "Basic plan with 8 requests per minute limit." (from the page's structured data); "800 / day".
- "Real-time US equities and ETFs", "Real-time forex market data", "Real-time crypto market data"; "Markets" row "3", which three not shown.
- Licence: "Internal non-display usage".
- "Free trial is available under the Basic plan." Whether a key is needed: not on the page.
- My reading, not checked: "non-display usage" usually means the data may not be shown on a screen to a person, which is exactly what this app does with prices; if the terms confirm that, the free plan does not fit spec 7 at all, however good its fields are. The terms decide it.
Terms of Use, I fetched 2026-10-30, https://twelvedata.com/terms ("Last updated: January 1, 2026"), found by a second search limited to twelvedata.com. Quoted as the fetch returned them; some passages came back cut off, marked "(cut off)":
- Definitions: "Non-Display Use" means any use of Data that does not involve displaying the Data to natural persons. "Internal Use" means use solely for Customer's internal business purposes and not for redistribution (cut off). "Free Trial" means evaluative use of the Platform for testing purposes prior to purchasing a subscription. "Credentials" means access controls, keys, tokens, or other authentication mechanisms necessary to access the Platform. "Display Use", "Personal Use" and "Basic": not on the page.
- 2.2(b): "Display Data to Authorized Users in accordance with this Agreement or third parties as expressly" (cut off). 2.2(e): "Redistribute or provide external display of Data only if and as expressly authorized by a" (cut off), with "(including compliance with attribution requirements)".
- Restrictions: 2.3(g) "Store or cache Data beyond permitted timeframes specified in the Documentation"; 2.3(l) "Use Free Tier data for commercial purposes"; 2.3(m) "Share Credentials or allow unauthorized access to the Platform". 4.1(c) "Maintain confidentiality of Credentials". 5.6(b) "No commercial use permitted during Free Trial". 16.2(a) "All Data must be deleted within 30 days" (on termination or expiration).
- My reading, not checked: the terms allow display to Authorized Users (2.2(b)), which does not match the pricing page's "Internal non-display usage" label for Basic; the cut-off passages and the "Documentation" that sets the caching timeframes would settle it, and neither was read. The free tier is non-commercial, which fits a personal app. Keeping prices (spec 7: last price, marked stale) depends on the caching timeframes, not read. A key built into the app on your own phone is not shared, but 4.1(c) asks to keep it confidential.
Support article "US equities market data", I fetched 2026-10-30, https://support.twelvedata.com/en/articles/9935903-us-equities-market-data (dated August 18, 2026), quoted as the fetch returned it:
- "Personal, commercial, display, non-display, use (e.g., dashboards, research, automation) is covered under standard plans."
- "All Twelve Data paid plans include access to real-time US stock market data suitable for both personal and commercial use."
- Historical and end-of-day data "is accessible starting with the Basic plan."
- Not on the page: ETFs, a table of usage per plan, whether Basic is one of the "standard plans".
- My reading, not checked: the article ties real-time US data to the paid plans and names Basic only for historical and end-of-day data, which does not match the pricing page's "Real-time US equities and ETFs" for Basic; display is covered "under standard plans", and whether the free Basic plan is one is not said. Three Twelve Data pages say three different things about the free plan; none settles display on Basic.
Support article "Commercial and personal usage", I fetched 2026-10-30 at your request, https://support.twelvedata.com/en/articles/5332349-commercial-and-personal-usage (page metadata dateModified 2026-10-22), quoted as the fetch returned it:
- "Individual plans are intended strictly for personal or internal use." Individual plans are named: "Individual plans (Grow, Pro, Ultra)"; business plans: "Business plans (Venture, Enterprise, Enterprise+)".
- "Acceptable non-commercial use cases include internal tools for personal or organizational use"; also "educational projects like student apps or academic research" and "development or testing phases of applications (non-production)".
- Individual plans "do not permit": "commercial display of data to third parties"; "redistribution of data". "Business plans allow the use of data for commercial display and internal usage, subject to exchange licensing requirements."
- Not on the page: the free Basic plan, non-display use, and a personal app or dashboard as such.
- My reading, not checked: for the paid individual plans, a personal app that shows prices only to you fits "internal tools for personal ... use" and is not "commercial display to third parties". The free Basic plan is not among the named plans in this article, so it does not settle Basic; with the pricing page's "Internal non-display usage" for Basic still unexplained, display on the free plan stays unclear.
- Outcome (your decision 2026-10-30, option A): Twelve Data is dropped as a candidate. Its free plan is unclear on display and on real-time US data (four pages, 2026-10-30, disagree); it is not shipped. No call was made to it and no key was created.

### 10.5 Financial Modeling Prep (FMP)
Two web searches limited to financialmodelingprep.com (only the queries left this PC) found mostly FMP's own how-to and blog articles, which are not used as facts, and the terms page. I could not fetch the terms: https://site.financialmodelingprep.com/terms-of-service returned HTTP 403, https://api.financialmodelingprep.com/terms-of-service returned HTTP 401.
Terms of Service, you read 2026-10-30 (https://site.financialmodelingprep.com/terms-of-service, pasted; "Last updated: August 1, 2023"). The passages that matter here, quoted:
- 2.2 Data License: a licence "to access the Services through a generally available mobile device, web browser or FMP authorized Site to view content and information, including FMP Data"; "without the prior written approval of FMP, the Customer may not distribute, publicly perform or display, lease, sell, transmit, transfer, publish, edit, copy, create derivative works from ... or otherwise make unauthorized use of the Services."
- 2.2.1 Personal Use: "strictly for their own personal, non-business and non-commercial purposes"; the Customer may not "integrate the Data or Services into any tools or applications accessible by any third parties, or use the Services to host, share, display, or provide content for others. The Customer may not copy or download any content from the Services except with the prior written approval of FMP."
- 2.2.2 Data Display: "Without a specific agreement with FMP, customers are prohibited from showcasing FMP Services or Data on platforms including but not limited to websites, blogs, software products, or applications designed for utilization by multiple individuals".
- 2.3: "Sharing your account login or API Key with any other individuals or entities, or enabling their usage of the Services, is strictly prohibited."
- 2.6.2: the Customer must comply with the Acceptable Data Use Policy at financialmodelingprep.com/acceptable-data-use-policy (not read).
- 2.8 Customer Security: "Customer affirms that all locations where FMP data is stored have implemented industry-standard physical, technical, and administrative controls ... Customer will notify FMP of the IP and domain aliases of any location where data is stored or processed."
- 6.3 Data Deletion: on termination, "Customer must delete all Data it has received from FMP ... including data cached, and sign the Data Deletion Agreement in Exhibit A."
- 10.3: "Customer will maintain a comprehensive written information security program"; 10.5: audits and, on request, security audit reports or certifications.
- Not in the terms: a plan-specific rule for the free plan, and attribution.
- My reading, not legal advice: a one-person app on your own phone is not "for multiple individuals" (2.2.2) and is not accessible to third parties (2.2.1). But the app downloads and keeps prices and instrument data (spec 7: the last price is kept and shown when a refresh fails), and 2.2.1 forbids copying or downloading content without FMP's written approval; 2.8 asks to notify FMP of every storage location; 10.3 asks for a written security programme. These go well beyond Finnhub's terms (10.2). The free plan's limits and FMP's profile fields were not looked up, because the terms already weigh against it.
- Outcome (your decision 2026-10-30, option B): FMP is dropped as a candidate and not shipped: its terms restrict copying or keeping data without FMP's written approval and add storage notices and a security programme. No key was created and no call was made. One more candidate is checked, terms first.

### 10.6 Polygon (now Massive)
Two web searches limited to polygon.io (only the queries left this PC) found its pages titled "Massive", its knowledge base and its terms pages; the search summaries' figures (Stocks Basic free: end-of-day data, 5 calls per minute) were not used as facts. Fetching https://polygon.io/individuals-terms-of-service returned "301 Moved Permanently" to https://massive.com/individuals-terms-of-service; with that redirect from polygon.io's own server, massive.com is taken as the same company's site.
Massive for Individuals Terms of Service, I fetched 2026-10-30, https://massive.com/individuals-terms-of-service ("Last Updated: July 18, 2025"), quoted as the fetch returned it:
- §2 Access Grant: "we grant you a non-exclusive, non-transferable, non-assignable, worldwide, limited right to access and use the Services" "solely for your own personal, non-commercial, and non-business purposes."
- §1: "Market Data is provided to you subject to the terms and conditions of the Market Data Terms of Service" (linked to /legal/market-data-terms-of-service).
- §3.3: "You are solely responsible for any and all activities conducted under your account, including your API keys." "For the avoidance of doubt, this includes sharing an account or API keys with others." §6.1(i): "Use or attempt to use another user's account or API keys;".
- §6.1(d)(iii): "making unauthorized copies of any content made available on or through the Services"; §6.1(h): "Sell or otherwise transfer the access granted under the Agreement or any of Massive's materials".
- §4.1: "free-of-charge Services" ("Free Services").
- Not on the page: display, apps, storing or caching, attribution, the word "Basic".
- My reading, not checked: personal, non-commercial use by you fits; display, storing and redistribution of market data are left to the Market Data Terms of Service, which decide.
Market Data Terms of Service, I fetched 2026-10-30, https://massive.com/legal/market-data-terms-of-service ("Last Updated: August 28, 2025"), quoted as the fetch returned it:
- Section 1: "exclusively for your personal, non-business, and non-commercial purposes"; "you may not use the Market Data to build an application intended for use by end users other than you".
- Section 2: "any and all Market Data is strictly for display use only"; it "may not be copied, reproduced, republished, uploaded, posted, publicly displayed" ... "to any other computer, server, website, or other medium for publication or distribution".
- Section 3: "will use the Market Data solely for your personal, non-business use".
- Section 5(d), among the things you may not do: "Use Market Data for non-display use or to create derivative works". 5(c): "to any third party or use the Market Data for business or commercial purposes". 5(a): "remove any trademarks transmitted with the Market Data".
- Section 8: "cease all use of the Market Data and delete all Market Data in your possession".
- Exchange schedules: NYSE (Schedule 2) "shall receive Market Data solely for his or her personal, non-business use", "Subscriber shall not furnish Market Data to any other person or entity."
- Not on the page: storing or caching, end-of-day data, a general attribution rule.
- My reading, not legal advice: an app built for you alone is expressly allowed ("end users other than you" is the limit), and showing prices to you is "display use". But 5(d) forbids "non-display use" and "derivative works", and the app computes market values, net worth, gains and allocation from the prices (4.8); whether that is a derivative work or non-display use is not defined on the page and could cut against the app's core purpose. With the free plan reported as end-of-day only (not checked), Massive would at best be a fallback whose terms are less clear than Finnhub's.
- Your decision 2026-10-30 (option B): Massive stays as a possible fallback, checked by calls; whether the app's computed values count as "derivative works" or "non-display use" (5(d)) stays open and is yours to accept or not at the decisions.
Key handling: a free key you registered, stored as the user environment variable MASSIVE_API_KEY on this PC (only its length, 32, was printed). A web search limited to massive.com and polygon.io found the REST quickstart (https://massive.com/docs/rest/quickstart), which per the search summary takes the key as an apiKey URL parameter or an "Authorization: Bearer" header on api.massive.com; the calls used the header, so the key was in no URL.
Batch M1, I called 2026-10-30 15:56:31 to 15:56:32 +08:00 (market closed), https://api.massive.com, nothing saved:
- /v3/reference/tickers/AAPL: "market":"stocks", "locale":"us", "primary_exchange":"XNAS", "type":"CS", "active":true, "currency_name":"usd", "sic_code":"3571", "sic_description":"ELECTRONIC COMPUTERS", address in Cupertino, CA, plus name, description, figi, market cap, employees, list date and branding URLs.
- /v3/reference/tickers/SPY: "market":"stocks", "primary_exchange":"ARCX", "type":"ETF", "currency_name":"usd"; no sic and no address.
- /v3/reference/tickers/AABB: "market":"otc", "primary_exchange":"OTC Link", "type":"CS", "currency_name":"USD".
- /v3/reference/tickers/VFIAX: HTTP 404.
- /v2/aggs/ticker/AAPL/prev: "adjusted":true, results o 348, c 348.91, h 351.43, l 347.3, vw 349.3528, t 1793304000000 (milliseconds; 2026-10-29 16:00:00 New York, the same session close Finnhub returned), v as 4.2582126e+07.
- My reading, not checked: the free key gets reference data and the previous day's prices; the ticker details give 4.3 what it needs in one call: type (CS, ETF), primary_exchange as a MIC for listed names (XNAS, ARCX, the same codes as Finnhub, so the same alias table of 10.3), market "otc" with "OTC Link" (not a MIC) for OTC names, and currency_name, whose case varies (usd, USD); a mutual fund is not found (404), as with Finnhub. For 4.9, a stock has an SIC industry and a headquarters address; an ETF has neither. Whether the free plan gives anything newer than the previous close (an intraday or real-time price) was not tried. Volume comes in exponent form, so numbers must be read as exact decimals, never as floating point (4.10).
Batch M2, I called 2026-10-30 15:58:43 to 15:58:45 +08:00, same key handling; the two paths are from my memory of Polygon's API, not from Massive's documentation:
- /v2/snapshot/locale/us/markets/stocks/tickers/AAPL: HTTP 403, no body read.
- /v2/last/trade/AAPL: HTTP 403, no body read.
- My reading, not checked: 403 (refused) rather than 404 (not found) suggests the paths exist and the free key is not entitled to them, which fits the reported end-of-day-only free plan. So on the free plan Massive gives the previous session's close, not a current price: usable as a fallback that shows yesterday's close with its time, not as the default.

### 10.7 Tiingo
A web search limited to tiingo.com (only the query left this PC) found its pricing and documentation pages; the search summary's figures were not used as facts.
Pricing page, I fetched 2026-10-30, https://www.tiingo.com/about/pricing, quoted as the fetch returned it:
- Starter, $0/month: "Unique Symbols per Month": 500; "Max Requests Per Hour": 50; "Max Requests Per Day": 1000; "Max Bandwidth Per Month": 1 GB.
- Licence row: "Internal Use Only". "Internal use means you may only use the data for your own personal use" "and you may not display or share the data with another person or organization". "For redistribution use, our pricing is explicit and predictable on our Product pages."
- The footer links "Terms of Service" at the relative path "/tos".
- Not shown by the fetch: storing data; which of "Real-time Data", "IEX Feed" and "Tiingo EOD 'Composite Prices'" the Starter plan includes.
- My reading, not checked: "your own personal use" with no display or sharing "with another person" fits an app that shows prices only to you; the limits (50 an hour, 1,000 a day, 500 symbols a month) fit a personal sweep of a few dozen tickers, not frequent refreshes.
Terms of Use: I fetched https://www.tiingo.com/tos, which returned "301 Moved Permanently" to https://app.tiingo.com/tos (a tiingo.com subdomain); that page returned HTTP 403. You read 2026-10-30 (pasted): "Tiingo Terms of Use, Version 1, Effective Date: January 16th, 2022, Last Updated Date: October 6th, 2026". The passages that matter here, quoted:
- Preamble and 9.7: "Company provides a free Starter Plan so that you can evaluate the Services before purchasing a paid subscription." "The Starter Plan is free and is intended to allow you to evaluate the Services before purchasing a paid subscription."
- 1.6(a) Starter Plans and Trial Plans: "you may not write, save, archive, back up, or otherwise retain Tiingo Data in any persistent or durable storage. You may process Tiingo Data only transiently in volatile memory or in a temporary, non-persistent cache, and only to complete the calculation or operation for which the Tiingo Data was obtained. You must permanently remove the Tiingo Data from that memory or cache immediately after the calculation or operation is completed and, in all events, before the applicable process, job, or user session ends. This prohibition applies to all storage systems ... including local devices, databases ..." The same limits apply to Derived Products: "You may not write, save, archive, back up, or otherwise retain any Derived Product in persistent or durable storage."
- 1.6(b) Paid Plans: Tiingo Data may be persisted while a paid plan is active and must be deleted when it ends or is downgraded to Starter.
- 1.6(c): "Derived Products" are "Data, results, content, or products created through the transformation, analysis, or processing of Tiingo Data"; the prohibited examples include "tables, files, feeds, APIs, databases, dashboards, charts, downloads, or query tools that display, deliver, or permit extraction of Tiingo Data or a substantially equivalent substitute".
- 7.3 Use of the Tiingo API: "All data via the API is for internal consumption only. If you are an individual, you may sign up for an Individual plan"; redistribution only on request, with "Data sourced by Tiingo" attribution.
- 2.2: "You may not share your Account or password with anyone".
- My reading, not legal advice: on the free Starter plan the app may not keep any Tiingo price on the phone, not even the last price, and may not keep values computed from it (market value, net worth). Spec 7 keeps the last price and shows it, marked stale, when a refresh fails, and spec 4.8 and the offline rule (section 9: "Works offline with last known prices") depend on it. So the Starter plan does not fit the spec. The terms also describe Starter as an evaluation plan. A paid plan would allow keeping prices while it is active; spec 7 asks for the free tier only.
- Outcome (your decision 2026-10-30, option B): Tiingo is dropped and not shipped: its free Starter plan forbids keeping any price or computed value on the phone. No key was created and no call was made. Alpha Vantage is checked next, terms first.

### 10.8 Alpha Vantage
A web search limited to alphavantage.co (only the query left this PC) found its support, documentation, premium and terms pages; the search summary's figure of 25 API requests per day for the free service was not used as a fact. I fetched https://www.alphavantage.co/terms_of_service/: it is a PDF (152.9 KB) whose text the fetch could not read; the tool saved a copy in Claude Code's session folder outside the repo, which I did not read (your decision).
Terms of Service, you read 2026-10-30 (pasted from the PDF at that address; no date shown in the paste). Quoted:
- 2 Grant of License: "Alpha Vantage grants the right to install, use, access, display and run the software on any computer or mobile device, where applicable, that you own or control, for personal, non-commercial use". "Commercial use" applies if "(i) You intend to use the Alpha Vantage Platform for any purpose that goes beyond investment analysis, research, testing, monitoring, and any other activities that are private and individual in nature", (ii) on behalf of a firm, (iii) to give others access to information "directly or indirectly", or (iv) with an affiliation to a financial adviser, broker or similar.
- 3: "a non-exclusive, non-sublicensable, non-transferable, non-assignable, revocable license to access and utilize the Alpha Vantage Platform"; effective "as of the date User clicks 'Get Free API Key'".
- 4 Use Restrictions: no reverse engineering of the platform, content or data.
- 7 Termination: "Upon termination, User's access to the Alpha Vantage Platform shall be deactivated."
- Not in the terms: storing or caching data, deleting data on termination, API keys, attribution, rate limits.
- My reading, not legal advice: the terms fit this app most plainly of all the candidates: display on a mobile device you own, for private monitoring of your own investments, is named as permitted, and nothing forbids keeping prices or computed values or asks for deletion. The open point is the limit: if the free service gives 25 requests a day (from the search, not checked), a price sweep of a few dozen tickers would use the whole day's allowance, so Alpha Vantage could serve a small portfolio or as a fallback, not frequent refreshes. Its fields (exchange, type, currency) were not looked up.
- Your decision 2026-10-30 (option A): test with a free key.
Documentation, I fetched 2026-10-30, https://www.alphavantage.co/documentation/ (only the first 100,000 of 819,311 characters were read): the key is passed as the URL parameter "apikey" (no header mentioned); GLOBAL_QUOTE: "by default, the quote endpoint is updated at the end of each trading day for all users", realtime or 15-minute delayed US data needs a premium plan; SYMBOL_SEARCH returns "the best-matching symbols and market information"; LISTING_STATUS and OVERVIEW were beyond the part read; no free request limit is stated in the part read.
Key handling: a free key you registered, stored as the user environment variable ALPHAVANTAGE_API_KEY (only its length, 16, was printed). The key had to go in the URL; the URLs were never printed, and every response was printed with the key's value replaced.
Batch AV1, I called 2026-10-30 16:34:39 to 16:34:46 +08:00, https://www.alphavantage.co/query, nothing saved:
- GLOBAL_QUOTE AAPL: "05. price": "348.9100", "08. previous close": "343.8300", "07. latest trading day": "2026-10-29" (a date, no time), open, high, low, volume, change and change percent; all values are strings.
- SYMBOL_SEARCH SPY: first match "1. symbol": "SPY", "3. type": "ETF", "4. region": "United States", "5. marketOpen": "09:30", "6. marketClose": "16:00", "7. timezone": "UTC-04", "8. currency": "USD", "9. matchScore": "1.0000"; further matches are listings in Frankfurt, London, XETRA and Paris, several in EUR.
- OVERVIEW AAPL: "Information": "Thank you for using Alpha Vantage! This is a premium endpoint. ..." (not on the free key).
- GLOBAL_QUOTE AABB (OTC): priced, "05. price": "0.0089", "07. latest trading day": "2026-10-29".
- My reading, not checked: the free key gives the end-of-day price (the same close as Finnhub and Massive) with its date but no time of day, which is less than spec 7's timestamp; prices come as strings, which suits exact decimals (4.10). The search gives a type (ETF) and a currency, but only a region ("United States"), not an exchange, so 4.3's exchange check cannot use it: an OTC name such as AABB would pass under 4.3's "if the source does not say, the ticker is accepted". Sector and country (OVERVIEW) are premium. The free limit was not reached in 4 calls and is still not shown. So Alpha Vantage's free key could at most be an end-of-day price fallback; its ticker check is weaker than Finnhub's or Massive's.

### 10.9 Stooq
A web search limited to stooq.com and stooq.pl, 2026-10-30 (only the query left this PC), found its "Free Market Data" page (https://stooq.com/db/) and quote pages with CSV file downloads, for tickers written with a market suffix (for example OSS.US, SNDK.US). Per the search summary, not fetched: "This data is intended solely for personal use"; quotes for Nasdaq, NYSE and other markets are provided by Infront; a "Terms of service" page is linked but its address and text were not shown; no API documentation appeared.
- My reading, not checked: Stooq is a website with downloadable files, not an API meant for apps; a ".US" suffix gives a country, not the exchange, type or currency that 4.3 needs; fetching its files by program, with no API offered, may be the kind of automated access its unread terms forbid.
- Outcome (your decision 2026-10-30, option B): Stooq is dropped and not shipped, without reading its terms. No call was made.

### 10.10 Yahoo Finance (unofficial endpoints)
A web search limited to yahoo.com and yahooinc.com, 2026-10-30 (only the query left this PC), found Yahoo's general terms, an API terms page and advertising and vendor terms; nothing specific to Yahoo Finance data and no official Finance API.
Yahoo Terms of Service, I fetched 2026-10-30, https://legal.yahoo.com/xw/en/yahoo/terms/otos/index.html (only the first 100,000 of 116,059 characters were read; no "last updated" date shown; the text names versions "dated May 15, 2025 or later" and "dated January 2, 2026 or later"), quoted as the fetch returned it:
- 2(d), member conduct, item 9: you may not "access or collect data, or attempt to access or collect data, from our Services using any automated means" "including but not limited to robots, spiders, scrapers, data mining tools, or data gathering or extraction tools" "for any purpose without our express, prior permission".
- 2(d), item 10: not to "use any material or content from, including without limitation any data, (a) to create any database".
- 2(e): not to "try to access them using a method other than the interface and the instructions that we provide".
- 2(i): "to use the software and APIs we may provide to you as part of the Services".
- 14.2(a)(i): "except for Yahoo Finance which is provided by Yahoo Finance LLC".
- Not on the page (part read): terms specific to market data or quotes.
- My reading, not legal advice: calling Yahoo Finance's undocumented endpoints from an app is access "using any automated means" that Yahoo has not offered as an interface or API, which 2(d) forbids "for any purpose", personal use included; keeping the prices would also be building a database from its data (item 10). Besides, such endpoints can change or stop without notice (section 8 risks).
- Outcome: Yahoo Finance is dropped and not shipped (option A: the terms were fetched first, then recorded as dropped). No call was made.

## 11. Status and notes
Outline approved 2026-10-30. Running: the US exchange list (10.1), Finnhub (10.2), the MIC codes (10.3), Twelve Data (10.4, dropped) and FMP's terms (10.5) looked up 2026-10-30; Finnhub's real-time question is open.
