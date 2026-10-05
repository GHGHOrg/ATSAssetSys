# ATS Asset System

ATS Asset System is an Android app for tracking personal cash, stock and ETF portfolios, net worth, and investment gains. Enter transactions yourself; the app does not connect to your bank or brokerage. Your financial records are stored locally on your device.

## Contents

- [Getting started](#getting-started)
- [Your account and portfolios](#your-account-and-portfolios)
- [Recording investments](#recording-investments)
- [Viewing gains and allocation](#viewing-gains-and-allocation)
- [Market prices and offline use](#market-prices-and-offline-use)
- [Correcting transactions](#correcting-transactions)
- [Backups and restore](#backups-and-restore)
- [Privacy and supported assets](#privacy-and-supported-assets)

## Getting started

On first launch, follow the setup flow to:

1. Choose how the app is locked.
2. Enter your opening cash balance.
3. Create a portfolio.
4. Record an initial purchase, if you have one.

If you already have a backup, initialize the app from that file using the option in **More**.

## Your account and portfolios

### Overall view

The Overall view summarizes your net worth, cash, stocks, ETFs, portfolios, and gains. Use it for a current snapshot of your assets and to see how they are allocated.

The app tracks a single shared cash account and multiple investment portfolios. Each portfolio keeps its holdings, transactions, lots, and realized gains separate. The same ticker can be held in more than one portfolio.

### Cash

Cash can be updated with an opening balance, deposits, withdrawals, interest or dividends, and adjustments. Investment purchases and sales also change cash automatically:

- A buy withdraws the total trade amount from cash.
- A sale adds the sale proceeds to cash.

Cash moves on the trade date. Settlement delays are not tracked.

Dividends are recorded as cash entries and are not assigned to an individual holding in this version.

### Managing portfolios

Create portfolios to keep different groups of investments separate. You can rename or delete a portfolio. Deleting one that contains transactions removes those records and reverses their cash effects, subject to validation. This is destructive; transactions are permanent, so make a backup first if you may need to recover the data.

## Recording investments

This version supports US stocks and ETFs listed on the NYSE or NASDAQ and denominated in USD. Ticker symbols are verified online when necessary.

### Buying

Enter the:

- Ticker
- Portfolio
- Trade date
- Quantity
- Total trade amount

Include trading fees in the total amount; fees are not entered separately. The app calculates the per-share price from the quantity and total. Review the purchase before saving.

A purchase is blocked if there is not enough cash or if a ticker that requires verification cannot be verified online.

### Selling

Sales use specific lots. Before confirming a sale, select the lots and quantities to sell. The app shows each lot's:

- Purchase date
- Remaining quantity
- Cost per share
- Unrealized gain or loss

Use **Sell All** to fill in the complete remaining quantity. A sale cannot include shares from another portfolio, shares purchased after the sale date, or more shares than are available.

Proceeds are added to cash on the trade date. You can record a total loss with zero proceeds, for example for a worthless or delisted investment.

### Stock splits

Forward and reverse stock splits are entered manually. Fractional shares are supported, with quantities recorded to up to eight decimal places.

For a trade made before a split, enter the original traded quantity. The app applies subsequent splits to calculate the current holding and available lots.

## Viewing gains and allocation

### Gains

Each holding shows its total cost, current value, and unrealized gain or loss. Gains are calculated from the individual lots, not from average cost.

Realized gains are available per sale, per holding, and as an overall total. You can filter realized gains by date range.

Gain figures are informational and are not tax calculations. Tax lots, wash-sale adjustments, and tax reporting are not supported.

### Allocation

View allocation across the whole account or within an individual portfolio. Available breakdowns include:

- Asset type
- Portfolio
- Holding
- Sector
- Geography

Each holding has one sector and one geography in this version. You can override these classifications manually.

## Market prices and offline use

The app retrieves market prices from a free external source. Price updates are best-effort and may be delayed or unavailable.

If a refresh fails, the app uses the last known price and marks it stale. If no price is available, the holding is valued at cost until a price is retrieved.

You can use the app offline to view existing records, see the last known prices, and work with locally stored transactions. An internet connection is needed to refresh prices, verify previously unknown tickers, and restore a backup.

## Correcting transactions

Saved transactions cannot be edited or deleted. To correct a mistake, record a reversal. **Reverse and re-enter** lets you reverse a transaction and enter its replacement in one confirmed operation. Both steps must succeed together; if either is blocked, neither is saved.

Transactions can be backdated or future-dated. Transactions on the same day are ordered by entry time. When you add a backdated transaction, the app validates it at that point in history and checks the subsequent affected dates.

## Backups and restore

The app's backup-format file is the only supported way to import or export data. CSV import/export and data merging are not available.

### Backing up

A backup includes your portfolios, transactions, reversal relationships, cash entries, hidden holdings, classification overrides and lists, and app settings.

The app rebuilds holdings, lots, trade cash movements, balances, gains, and allocation from the saved records. Market prices and PIN or biometric credentials are not included in the backup.

The Overall screen reminds you to back up if no backup has been completed or the last completed backup is more than seven days old. The reminder appears in the app, not as an Android notification. Dismissing it hides it until the next app launch. Only a completed export counts as a backup.

### Restoring

Restore initializes the app from a backup file and replaces existing financial data. If you already have data, consider exporting a backup before continuing.

The restore process validates the complete file first. If validation fails, the app reports the errors and leaves existing data unchanged. Replacing existing data requires explicit confirmation, including typed confirmation. Held tickers must be verified online during restore.

## Privacy and supported assets

ATS Asset System is designed for personal financial data:

- Records are stored locally on your Android device.
- The app supports biometric or PIN authentication.
- The app locks automatically; available policies include locking when you leave, after one or five minutes of inactivity, or when the phone locks or the app restarts. Locking whenever you leave is the default.
- Screenshots and screen recording are blocked by default, and app content is hidden from the recent-apps preview.

Temporary screenshot access can be enabled after authentication. It lasts three minutes, ends when you leave the app, does not persist after an app restart, and is not included in backups.

This version is for one user on an Android phone. It supports USD cash and US stocks and ETFs. It does not support liabilities, loans, credit cards, crypto, property, vehicles, mutual funds, bank or broker synchronization, multi-currency accounts, tax reporting, or data sharing. Historical performance and per-holding dividends are not currently available.

## License

See the repository's license information for licensing terms.
