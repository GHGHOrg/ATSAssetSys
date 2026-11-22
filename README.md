# ATS Asset System

ATS Asset System is a single-user Android app for tracking personal cash, stock and ETF portfolios, net worth, allocation, and lot-based investment gains. You enter transactions yourself; the app does not connect to your bank or brokerage. All app data stays on the device, and the backup file is the only supported way to transfer data in or out of the app.

The app is for personal use on Android 13 or later. It is not on Google Play; it is installed directly on the phone.

## Contents

- [Installing and updating](#installing-and-updating)
- [Getting started](#getting-started)
- [Your account and portfolios](#your-account-and-portfolios)
- [Recording investments](#recording-investments)
- [Viewing gains, history and allocation](#viewing-gains-history-and-allocation)
- [Market prices and offline use](#market-prices-and-offline-use)
- [Correcting transactions](#correcting-transactions)
- [Backups and restore](#backups-and-restore)
- [Settings](#settings)
- [Privacy, app lock and supported assets](#privacy-app-lock-and-supported-assets)

## Installing and updating

The app is installed from a signed APK file. A newer version installs over the one on your phone and keeps your data. An older version cannot be installed over a newer one, so going back to an earlier version means removing the app and restoring a backup.

Make a backup before installing an update.

Each release is recorded as a GitHub Release in this repository, with its version (for example 0.38.0) and its notes. The APK file itself is not attached there.

## Getting started

On first launch, confirm your phone's biometric or PIN to unlock the app. Then follow the setup steps:

1. Enter your opening cash balance. The app asks for this first, before a portfolio or any other entry.
2. Create one or more portfolios.
3. Record an initial purchase, if you have one.

To see market prices and to buy a ticker the app has not checked before, enter your Finnhub API key first: **More**, then the gear icon (**Settings**), then **Price source** under **Prices**. See [Market prices and offline use](#market-prices-and-offline-use).

If you already have a backup, enter the API key first, then initialize the app from the file: **More**, **Backup and restore**, **Restore from backup file**.

The bottom bar has four tabs: **Overall**, **Portfolios**, **Cash** and **More**. You can change their order in **Settings**.

## Your account and portfolios

### Overall view

The Overall view shows your net worth, with how old its prices are, each portfolio with its value and gain or loss, and the allocation of your net worth as a donut chart. Use **Allocation by** to switch the chart between asset type, portfolio, holding, sector and geography. Tap net worth, the chart or a row to open **Net Worth (Total)**.

The app tracks one shared cash account and multiple investment portfolios. Each portfolio keeps its holdings, transactions, lots, and realized gains separate. The same ticker can be held in more than one portfolio.

### Cash

Cash can be updated with an opening balance, deposits, withdrawals, interest or dividends, and adjustments. An adjustment needs a note. Investment purchases and sales also change cash automatically:

- A buy withdraws the total trade amount from cash.
- A sale adds the sale proceeds to cash.

Cash moves on the trade date. Settlement delays are not tracked. An entry that would make cash negative on its date or any later date is refused, and the message names the date and the amount.

The **Cash** tab lists every entry with the balance after it. You can narrow the list by a **From** and **To** date, by ticker, and by type (Deposit, Withdrawal, Interest/dividend, Adjustment, Buy or Sell). A line says how many entries are shown, and **Clear filters** resets them. Tap an entry to see its details.

Dividends are recorded as cash entries and are not assigned to an individual holding in this version.

### Managing portfolios

Create portfolios on the **Portfolios** tab with **New portfolio**. A portfolio's ⋮ menu offers **Rename** and **Delete**.

Deleting a portfolio removes it, its transactions and their cash effects in one step. Before you confirm, the app shows how many transactions and cash lines go with it, the realized gains that are removed, and your cash balance now and after; you then type the portfolio's name to confirm. If removing it would make cash negative on any date, the app does not delete it and explains what to add first. Deleting cannot be undone except by restoring a backup, so make a backup first if you may need the data.

A holding you have sold completely can be hidden: open it and tap **Hide** at 0 shares. It then moves to the portfolio's **Hidden holdings**, where **Unhide** brings it back. A new purchase of that ticker shows it again by itself.

## Recording investments

This version supports US stocks and ETFs listed on a US national securities exchange (for example NYSE, NYSE Arca or NASDAQ) and denominated in USD. Mutual funds are not supported. Ticker symbols are verified online with the price source when the app has not seen them before. A ticker that the price source says is a mutual fund, is listed on a non-US exchange or an OTC market, or is not quoted in USD is rejected, and the reason is shown.

Buy and Sell start from **Buy/Sell**, or from a holding's own screen.

### Buying

Enter the:

- Portfolio
- Ticker
- Trade date
- Quantity
- Total trade amount

Include trading fees in the total amount; fees are not entered separately. The app calculates the per-share price from the quantity and total. Review the purchase before saving.

A purchase is blocked if there is not enough cash on any date from the trade date on, or if a ticker that requires verification cannot be verified online.

### Selling

Sales use specific lots. Before confirming a sale, pick the lots and quantities to sell; tapping a lot picks its full remainder, and you can edit the number for part of a lot. The app shows each lot's:

- Purchase date
- Remaining quantity
- Cost per share
- Unrealized gain or loss at the latest price

Use **Sell all** to pick every available lot. A sale cannot include shares from another portfolio, shares purchased after the sale date, or more shares than are available. The review shows the lots used, their cost, the realized gain or loss and the cash effect.

Proceeds are added to cash on the trade date. You can record a total loss with zero proceeds, for example for a worthless or delisted investment. A holding sold completely stays in its portfolio at 0 shares until you hide it.

### Stock splits

Forward and reverse stock splits are entered on the holding's screen with **Split**, as a ratio of two whole numbers (for example "2 for 1"). The review shows the shares before and after; the total cost and cash do not change.

Fractional shares are supported, with quantities shown to eight decimal places. A quantity above zero never shows as 0: a tiny remainder left by a reverse split shows as 0.00000001 and can still be sold.

For a trade made before a split, enter the original traded quantity. The app applies later splits to calculate the current holding and available lots.

### Holding detail

Tap a holding to open its screen. The header shows its value, shares, cost, unrealized gain or loss and the price's age, with **Buy**, **Sell** and **Split**. Three tabs follow:

- **Transactions**: the holding's buys, sells, splits and reversals. Tap one to open its details.
- **Lots**: each lot with its date, shares left, cost per share, lot cost and unrealized gain or loss.
- **Classification**: the holding's sector and geography, and its split history.

## Viewing gains, history and allocation

### Gains

Each holding shows its total cost, current value, and unrealized gain or loss. Gains are calculated from the individual lots, not from average cost.

**Realized gains**, under **More**, shows the total realized gain with the number of sales, then one row per holding. Tap a row to see its sales, each with its date, gain or loss, quantity, proceeds and the cost of the lots used. You can filter by a **From** and **To** date. A sale cancelled by a reversal is left out.

Gain figures are informational and are not tax calculations. Tax lots, wash-sale adjustments, and tax reporting are not supported.

### Transaction history

Each portfolio has a **Transaction history**, reached from its screen. It lists the portfolio's transactions, newest first, and can be filtered by date range, ticker and type (Buy, Sell, Split or Cash). Each buy or sell shows its cash effect, and a reversal names the entry it reverses. Tap a row to open the transaction.

### Allocation

Allocation is shown at two levels:

- **Whole account (Overall and Net Worth (Total)):** the share of your net worth by asset type (cash, stocks, ETFs), portfolio, holding, sector and geography. Cash appears as its own "Cash" slice in every breakdown. **Net Worth (Total)** also lists cash and each portfolio's value, which add up to your net worth.
- **One portfolio (Allocation screen):** the share of that portfolio's value by asset type (stocks, ETFs), holding, sector and geography. Open it from a portfolio's screen or from **More**, and switch portfolio at the top. It does not include cash, because cash is one shared account.

Each holding has one sector and one geography in this version. They come from the price source where it supplies them; otherwise the holding is **Unclassified**. On the holding's **Classification** tab you can override either one by picking a value from its list, and clear the override again. You manage the lists of sectors and geographies in **Settings**.

## Market prices and offline use

The app retrieves market prices from Finnhub, using your own Finnhub API key. No key is built into the app: enter it in **Settings**, under **Prices**, **Price source**. The key is checked with Finnhub before it is saved, and afterwards the app shows only its last four characters. Without a key, the app makes no price calls, and the screens say that no key is saved.

Prices refresh by themselves when you open the app online. Pull down on Overall, a portfolio or a holding to refresh them yourself. Price updates are best-effort and may be delayed or unavailable.

Each value shows the age of its prices. If a refresh fails, the app keeps the last known price, marks it stale and shows one line saying the refresh failed. If no price is available, the holding shows "no price" and is valued at cost until a price is retrieved.

You can use the app offline to view existing records, see the last known prices, and work with locally stored transactions. An internet connection is needed to refresh prices, verify tickers the app has not seen before, and restore a backup.

## Correcting transactions

Saved transactions cannot be edited or deleted. To correct a mistake, reverse it from its details screen with **Reverse**. A reversal follows the same rules as a normal entry, can be dated on the original's date or later, and both entries stay linked. A cash entry's reversal is recorded as an opposite adjustment.

**Reverse and re-enter** lets you reverse a buy, sell, split or cash entry and enter its replacement in one confirmed operation. The form opens with the original's values, and one review shows both parts and the combined cash effect. Both steps must succeed together; if either is blocked, neither is saved.

Transactions can be backdated or future-dated. Dates follow US market time (New York, shown as ET), and transactions on the same day are ordered by entry time. When you add a backdated transaction, the app validates it at that point in history and checks the later dates it affects.

## Backups and restore

The backup file is the only supported data-transfer format. It is a versioned CSV file for whole-app backup and restore, not a general-purpose import or export. Partial imports and merges are not supported.

To prepare a backup file by hand, use the [backup template](spec/template-backup.csv) or the [sample backup](spec/example-backup.csv) and its [expected restored data](spec/example-backup-expected.md). The file's format and its checks are described in [spec/spec.md](spec/spec.md), section 8.

### Backing up

Open **More**, **Backup and restore**, then **Back up now**, and choose where Android saves the file. **More** shows the date of the last backup, or "never backed up".

A backup includes your portfolios, transactions, reversal links, cash entries, hidden holdings, classification overrides and lists, and the auto-lock, price source and tab order settings.

The app rebuilds holdings, lots, trade cash movements, balances, gains, and allocation from the saved records. Market prices, the last-backup date, your Finnhub API key and the phone's PIN or biometric credentials are not included in the backup.

The Overall screen reminds you to back up if the app holds data and no backup has been completed, or if the last completed backup is more than seven days old. The reminder appears in the app, not as an Android notification, and its **Back up now** starts a backup. Dismissing it hides it only until you next return to the app after leaving it, or restart it. Only a completed export counts as a backup.

### Restoring

Restore initializes the app from a backup file: **More**, **Backup and restore**, **Restore from backup file**. It needs an internet connection and a saved Finnhub API key, because held tickers are verified online.

The app checks the complete file first. If the check fails, it lists every error with its line and reason, which you can copy, and leaves your data unchanged. A file that passes shows a summary of what it holds.

If the app already holds data, the summary compares the counts now with the counts in the file, and the app offers to make a backup first. Replacing the data needs **RESTORE** typed to confirm.

The restore is all-or-nothing. It deletes all current data and settings, your API key included, and loads the file, so the settings stored in the file (auto-lock, price source and tab order) replace the current ones. Afterwards, enter your API key again. Holdings show "no price" until the first price refresh, and the backup reminder appears, because a restore resets the last-backup date.

## Settings

Open **Settings** with the gear icon on **More**:

- **Security:** **Auto-lock** (see below) and **Screenshot window**.
- **Navigation:** **Tab order**, to arrange the four tabs of the bottom bar.
- **Lists:** **Sectors** and **Geographies**. Add, rename or delete values; each shows how many holdings use it. A renamed value keeps matching the price source's original name. A value in use cannot be deleted.
- **Prices:** **Price source**, to enter, change or clear your Finnhub API key.

## Privacy, app lock and supported assets

ATS Asset System protects your personal financial data:

- Records are stored only on your Android device, encrypted. Android keeps the encryption key on the phone; it is not part of a backup. If the stored data can no longer be read, the app says so, and a backup file is the way to recover.
- Android cloud backup and device-to-device transfer do not include app data. Keep your backup files somewhere safe.
- Price refreshes send ticker symbols and your API key to Finnhub. The app does not send portfolio names, balances, quantities, amounts, notes, or transaction records.
- The app does not use analytics or crash-reporting services.
- The app unlocks with your phone's biometric or PIN.
- The app locks automatically. Choose when in **Settings**, **Auto-lock**: every time you leave the app (the default), after one or five minutes idle, or only when the phone locks or the app restarts.
- Screenshots and screen recording are blocked by default, and app content is hidden from the recent-apps preview.

Temporary screenshot and screen-recording access can be turned on in **Settings**, **Screenshot window**, after your biometric or PIN. It lasts three minutes, ends when you leave the app or tap **Turn off**, does not survive an app restart, and is not included in backups. The recent-apps preview stays hidden.

The app is for one user on an Android phone and supports USD cash and US stocks and ETFs. It does not support liabilities, loans, credit cards, crypto, property, vehicles, mutual funds, bank or broker synchronization, multi-currency accounts, tax reporting, or data sharing. Performance over time, per-holding dividend tracking, and ETF look-through allocation are not currently available, and Finnhub is the only price source.

## License

No license is specified in this repository.
