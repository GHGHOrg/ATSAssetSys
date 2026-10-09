# ATS Asset System

ATS Asset System is a single-user Android app for tracking personal cash, stock and ETF portfolios, net worth, allocation, and lot-based investment gains. You enter transactions yourself; the app does not connect to your bank or brokerage. All app data stays on the device, and the backup file is the only supported way to transfer data in or out of the app.

The app is for personal use on Android 13 (API 33) or later and is intended to be installed directly on a phone.

## Contents

- [Getting started](#getting-started)
- [Your account and portfolios](#your-account-and-portfolios)
- [Recording investments](#recording-investments)
- [Viewing gains and allocation](#viewing-gains-and-allocation)
- [Market prices and offline use](#market-prices-and-offline-use)
- [Correcting transactions](#correcting-transactions)
- [Backups and restore](#backups-and-restore)
- [Privacy, app lock and supported assets](#privacy-app-lock-and-supported-assets)

## Getting started

On first launch, confirm your phone's biometric or PIN to unlock the app. Then follow the setup steps:

1. Enter your opening cash balance. The app asks for this first, before a portfolio or any other entry.
2. Create one or more portfolios.
3. Record an initial purchase, if you have one.

You can change the auto-lock setting later in **Settings** under **More**. By default the app locks every time you leave it.

If you already have a backup, initialize the app from that file using the option in **More**.

## Your account and portfolios

### Overall view

The Overall view summarizes your net worth, cash, stocks, ETFs, portfolios, and gains. Use it as a current snapshot of your assets and to see how they are allocated.

The app tracks one shared cash account and multiple investment portfolios. Each portfolio keeps its holdings, transactions, lots, and realized gains separate. The same ticker can be held in more than one portfolio.

### Cash

Cash can be updated with opening balance, deposits, withdrawals, interest or dividends, and adjustments. Investment purchases and sales also change cash automatically:

- A buy withdraws the total trade amount from cash.
- A sale adds the sale proceeds to cash.

Cash moves on the trade date. Settlement delays are not tracked.

Entry dates are calendar dates using the US market timezone (New York); transactions on the same date are ordered by entry time. The app stores dates as selected, without converting time zones.

Dividends are recorded as cash entries and are not assigned to an individual holding in this version.

### Managing portfolios

Create portfolios to keep different groups of investments separate. You can rename or delete a portfolio. Deleting one that contains transactions removes those records and reverses their cash effects, subject to validation. This is destructive; transactions are permanent, so make a backup first if you may need to recover the data.

The same ticker can be held in multiple portfolios. Each portfolio has its own holdings and transaction history. Fully sold holdings can be hidden from the regular holdings list and viewed or unhidden later.

## Recording investments

This version supports US stocks and ETFs listed on a US national securities exchange (for example NYSE, NYSE Arca or NASDAQ) and denominated in USD. Mutual funds are not supported. Ticker symbols are verified online when necessary. A ticker that the price source says is a mutual fund, is listed on a non-US exchange or OTC market, or is not quoted in USD is rejected, and the reason is shown.

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

Proceeds are added to cash on the trade date. You can record a total loss with zero proceeds, such as for a worthless or delisted investment.

### Stock splits

Forward and reverse stock splits are entered manually. Fractional shares are supported, with quantities recorded to up to eight decimal places.

For a trade made before a split, enter the original traded quantity. The app applies subsequent splits to calculate the current holding and available lots. The holding detail includes split history so omissions are easy to spot.

## Viewing gains and allocation

### Gains

Each holding shows its total cost, current value, and unrealized gain or loss. Gains are calculated from the individual lots, not from average cost.

Realized gains are available per sale, per holding, and as an overall total. You can filter realized gains by date range.

Gain figures are informational and are not tax calculations. Tax lots, wash-sale adjustments, and tax reporting are not supported.

### Allocation

Allocation is shown at two levels:

- **Whole account (Net Worth screen):** the share of your net worth by asset type (cash, stocks, ETFs), portfolio, holding, and sector or geography. Cash appears as its own "Cash" slice in every breakdown.
- **One portfolio (Allocation screen):** the share of that portfolio's value by asset type (stocks, ETFs), holding, and sector or geography. It does not include cash, because cash is one shared account.

Each holding has one sector and one geography in this version. You can override these classifications manually. You can manage the sector and geography lists in Settings. Classifications not supplied by the price source appear as **Unclassified** unless you choose an override.

## Market prices and offline use

The app retrieves market prices from a free external source. Price updates are best-effort and may be delayed or unavailable. Each price shows its timestamp and stale state when the last refresh is older than expected or the source fails.

If a refresh fails, the app keeps the last known price and marks it stale. If no price is available, the holding is valued at cost until a price is retrieved.

You can use the app offline to view existing records, see the last known prices, and work with locally stored transactions. An internet connection is needed to refresh prices, verify previously unknown tickers, and restore a backup.

You can choose the active price source in Settings.

## Correcting transactions

Saved transactions cannot be edited or deleted. To correct a mistake, record a reversal. **Reverse and re-enter** lets you reverse a transaction and enter its replacement in one confirmed operation. Both steps must succeed together; if either is blocked, neither is saved.

To undo a mistake, you can reverse a transaction or cash entry. A reversal follows the same validation rules as a normal transaction and can be used to correct buys, sells, splits, and cash entries.

Transactions can be backdated or future-dated. Transactions on the same day are ordered by entry time. When you add a backdated transaction, the app validates it at that point in history and checks the subsequent affected dates.

## Backups and restore

The backup file is the only supported data-transfer format. It is a versioned UTF-8 CSV file for whole-app backup and restore, not a general-purpose CSV import/export feature. An app initialization or restore deletes all current settings and data before loading the file. Partial imports and merge operations are not supported.

To prepare a backup file, use the [backup template](spec/template-backup.csv) or review the [sample backup](spec/example-backup.csv) and its [expected restored data](spec/example-backup-expected.md). See the [backup file format and validation rules](spec/spec.md). Do not use [example-import.csv](spec/example-import.csv); it uses an older format that is no longer supported.

### Backing up

A backup includes your portfolios, transactions, reversal relationships, cash entries, hidden holdings, classification overrides and lists, and app settings.

The app rebuilds holdings, lots, trade cash movements, balances, gains, and allocation from the saved records. Market prices, the last-backup date, and the phone's PIN or biometric credentials are not included in the backup.

The Overall screen reminds you to back up if the app holds data and no backup has been completed, or if the last completed backup is more than seven days old. The reminder appears in the app, not as an Android notification. Dismissing it hides it only until you next return to the app after leaving it, or restart it. Only a completed export counts as a backup.

### Restoring

Restore initializes the app from a backup file. It validates the complete file first. If validation fails, the app reports the errors and leaves existing data unchanged. Replacing existing data requires explicit confirmation, including typed confirmation. Held tickers must be verified online during restore.

The restore process is all-or-nothing. If the file is valid, the app deletes the current data and settings, loads the file, and the settings stored in the file (auto-lock, price source, and tab order) replace the current ones. A restore resets the last-backup date so the backup reminder appears afterwards. If you already have data, the app offers to export a backup first.

## Privacy, app lock and supported assets

ATS Asset System protects your personal financial data:

- Records are stored locally on your Android device.
- App records are encrypted at rest. Android keeps the encryption key; it is not included in a backup. If the key is lost, restore your data from a backup file.
- Android cloud backup and device-to-device transfer do not include app data. Keep exported backup files somewhere safe.
- Price refreshes require internet. The app sends ticker symbols and price-source request details to the selected external price source. It does not send portfolio names, balances, quantities, notes, or transaction records.
- The app does not use analytics or crash-reporting services.
- The app supports biometric or PIN authentication.
- The app locks automatically; available policies include locking when you leave, after one or five minutes of inactivity, or when the phone locks or the app restarts. Locking whenever you leave is the default.
- Screenshots and screen recording are blocked by default, and app content is hidden from the recent-apps preview.

Temporary screenshot and screen-capture access can be enabled after authentication. It lasts three minutes, ends when you leave the app, does not persist after an app restart, and is not included in backups.

The app is for one user on an Android phone and supports USD cash and US stocks and ETFs. It does not support liabilities, loans, credit cards, crypto, property, vehicles, mutual funds, bank or broker synchronization, multi-currency accounts, tax reporting, or data sharing. Historical performance and per-holding dividends are not available.

Performance over time, per-holding dividend tracking, and ETF look-through allocation are not currently available.

## License

Licensing terms are not specified in this repository.
