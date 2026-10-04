# ATS Asset System

A private, offline-first Android app for tracking personal financial assets, portfolios, cash, and investment performance.

ATS Asset System gives you a single place to understand **what you own, how your assets are allocated, and how their value changes over time**—without connecting your bank or brokerage accounts.

## Table of Contents

- [Overview](#overview)
- [Goals](#goals)
- [Key Features](#key-features)
  - [Net Worth](#net-worth)
  - [Portfolios](#portfolios)
  - [Shared Cash Account](#shared-cash-account)
  - [Stocks and ETFs](#stocks-and-etfs)
  - [Buying](#buying)
  - [Selling](#selling)
  - [Lot-Based Accounting](#lot-based-accounting)
  - [Stock Splits](#stock-splits)
- [Gains](#gains)
  - [Realized Gains](#realized-gains)
  - [Unrealized Gains](#unrealized-gains)
- [Allocation](#allocation)
- [Market Prices](#market-prices)
- [Transaction Rules](#transaction-rules)
- [Backup and Restore](#backup-and-restore)
  - [Backup](#backup)
  - [Restore](#restore)
- [Backup Reminder](#backup-reminder)
- [Privacy and Security](#privacy-and-security)
  - [Auto-Lock](#auto-lock)
  - [Temporary Screenshot Access](#temporary-screenshot-access)
- [First Launch](#first-launch)
- [Offline Operation](#offline-operation)
- [Data Model Principles](#data-model-principles)
- [Current Scope](#current-scope)
  - [Included](#included)
  - [Not Included](#not-included)
- [Planned Features](#planned-features)
  - [Historical Performance](#historical-performance)
  - [Per-Holding Dividends](#per-holding-dividends)
  - [ETF Look-Through](#etf-look-through)
- [Design Principles](#design-principles)
- [Status](#status)
- [License](#license)

## Overview

ATS Asset System is designed for one person managing their own financial assets.

It tracks:

- A shared cash account
- Multiple stock and ETF portfolios
- Individual buy, sell, and stock-split transactions
- Per-lot cost basis and gains
- Net worth
- Asset allocation
- Portfolio and holding allocation
- Sector and geography allocation
- Realized and unrealized gains
- Backup and restore of the complete application state
- Automatic market-price updates

The application is designed to work **offline**, using the last known market prices when an internet connection is unavailable.

## Goals

The first version focuses on answering three questions:

1. **What is my total net worth?**
2. **How is my net worth allocated?**
3. **How much has each holding and portfolio gained or lost?**

Investment history is recorded from the beginning so that historical performance can be added later without losing transaction data.

## Key Features

### Net Worth

The Overall view provides a snapshot of total assets, including:

- Cash
- Stocks
- ETFs
- Individual portfolios
- Portfolio gains and losses

The first version prioritizes an accurate current snapshot over historical performance.

### Portfolios

Create and manage multiple investment portfolios.

Each portfolio has its own:

- Holdings
- Transactions
- Lots
- Realized gains

The same ticker can be held in multiple portfolios.

Portfolios can be created, renamed, and deleted. Deleting a portfolio with transactions removes its transactions and reverses their associated cash effects, subject to the application's validation rules.

Because transactions are permanent, portfolio deletion is intentionally destructive and can only be recovered by restoring a backup.

### Shared Cash Account

The application uses one shared cash account instead of tracking individual bank accounts.

Cash can change through:

- Opening balance
- Deposits
- Withdrawals
- Interest/dividends
- Adjustments
- Investment purchases
- Investment sales

Buying an investment automatically withdraws the trade total from cash.

Selling an investment automatically adds the sale proceeds to cash.

Cash moves on the trade date; settlement delays are not modeled.

### Stocks and ETFs

The first version supports:

- US stocks
- US ETFs
- NYSE
- NASDAQ
- USD-denominated instruments

Crypto, mutual funds, property, vehicles, and other asset classes are outside the current scope.

Ticker symbols are verified online when necessary.

### Buying

A buy transaction records:

- Ticker
- Portfolio
- Date
- Quantity
- Total trade amount

Trading fees are included in the total amount rather than stored as a separate field.

The price per share is calculated from the quantity and total amount.

A purchase is blocked when:

- There is insufficient cash
- The ticker cannot be verified online when verification is required

A quick review step is shown before a purchase is saved.

### Selling

Selling is based on **specific lots**.

Before selling, the user selects which lots are being sold. The application shows information such as:

- Purchase date
- Remaining quantity
- Cost per share
- Unrealized gain/loss

A sale cannot use:

- Shares from another portfolio
- Shares purchased after the sale date
- More shares than the portfolio owns

A **Sell All** shortcut fills in the complete remaining quantity.

Selling can also record a total loss with zero proceeds, allowing worthless or delisted investments to be represented accurately.

### Lot-Based Accounting

ATS Asset System uses specific-lot identification throughout the application.

Lots are used to calculate:

- Cost
- Realized gain/loss
- Unrealized gain/loss

There is no configurable default lot-selection method.

This is intended to match brokers that use specific-lot identification.

Lots are tracked independently within each portfolio.

### Stock Splits

Both forward and reverse stock splits are supported.

Fractional shares are supported, with quantities allowing up to eight decimal places.

Stock splits are entered manually.

For transactions that occurred before a split, the original traded quantity is entered and subsequent splits are applied automatically.

## Gains

### Realized Gains

Realized gains are available:

- Per sale
- Per holding
- As an overall total

The realized-gains view can be filtered by date range.

These values are informational and are **not intended to be tax calculations**.

For example, tax-specific concepts such as wash-sale adjustments and tax lots are outside the scope of the application.

### Unrealized Gains

Each holding shows:

- Total cost
- Current value
- Unrealized gain/loss

Unrealized values are calculated from individual lots rather than average cost.

## Allocation

Allocation can be viewed by:

- Asset type
- Portfolio
- Individual holding
- Sector
- Geography

The allocation can be viewed across the entire account or within individual portfolios.

Each holding has one sector and one geography in the first version.

Sector and geography values can be overridden manually.

## Market Prices

Market prices are obtained from a free external price source.

The application is designed for live or near-real-time pricing on a best-effort basis.

Each price has an associated freshness state.

If a current price cannot be retrieved:

- The last known price is used
- The price is marked as stale

The application remains usable offline using the most recently available prices.

A holding that cannot be priced is valued at cost and marked as having **no price** until a successful price update occurs.

## Transaction Rules

Transactions are intentionally treated as permanent records.

Saved transactions cannot be edited or deleted.

Mistakes are corrected by recording a reversal transaction.

A **Reverse and re-enter** workflow allows a transaction to be reversed and replaced in one confirmed operation.

Both operations must succeed together; if either is blocked, neither is saved.

Transactions may be:

- Backdated
- Future-dated

Same-day transactions are ordered by entry time.

When a transaction is backdated, the application's validation rules are applied at that point in history and to subsequent affected dates.

## Backup and Restore

The application's only import/export mechanism is its **backup-format file**.

There is deliberately no:

- CSV import
- CSV export
- Data merge
- Other data-export mechanism

### Backup

A backup contains the application's persistent configuration and financial records, including:

- Portfolios
- Transactions
- Reversal relationships
- Cash entries
- Hidden holdings
- Sector overrides
- Geography overrides
- Sector and geography lists
- Application settings

Derived data such as lots, holdings, trade cash lines, and prices is not stored directly. It is rebuilt by replaying the source records.

The backup file does not contain the device's PIN or biometric credentials.

Prices are also not stored in the backup.

### Restore

Restoring a backup is an **application initialization operation**.

It:

1. Validates the complete backup file.
2. Reports all validation errors if validation fails.
3. Makes no changes when validation fails.
4. Optionally prompts the user to create a backup of existing data.
5. Deletes the existing application data.
6. Loads the backup.
7. Rebuilds derived state by replaying the records.

The operation is all-or-nothing.

If the application already contains data, replacing it requires explicit confirmation, including a typed confirmation.

Held tickers must be verified online during restore.

## Backup Reminder

The application shows the date of the last completed backup.

If no backup exists, or the most recent backup is more than seven days old, a reminder banner is shown on the Overall screen.

The reminder:

- Appears inside the application
- Is not an Android system notification
- Can be dismissed until the next application launch

Only a successfully completed export counts as a backup.

## Privacy and Security

ATS Asset System is designed for private personal financial data.

The application:

- Stores data locally on the Android device
- Uses biometric or PIN authentication
- Locks automatically
- Blocks screenshots by default
- Blocks screen recording by default
- Hides application content from the recent-apps preview

### Auto-Lock

Available lock policies include:

- Lock whenever the app is left
- Lock after 1 minute of inactivity
- Lock after 5 minutes of inactivity
- Lock only when the phone locks or the app restarts

The strictest option is the default.

### Temporary Screenshot Access

A screenshot window can be enabled after biometric or device-PIN authentication.

The window:

- Lasts for three minutes
- Ends when the user leaves the application
- Does not survive an application restart
- Does not appear in the backup file
- Does not expose content in the recent-apps preview

## First Launch

When no financial data exists, the application provides a guided setup flow.

The initial sequence is:

1. Confirm the application lock.
2. Set the opening cash balance.
3. Create a portfolio.
4. Enter an initial purchase.

Backup-file initialization is available separately from the **More** section.

## Offline Operation

The application is designed to work without an internet connection.

Offline operation supports:

- Viewing existing financial data
- Viewing the last known market prices
- Working with locally stored transactions

Internet access is required for operations such as:

- Refreshing market prices
- Verifying previously unknown tickers
- Restoring a backup file

## Data Model Principles

The application intentionally separates **source records** from derived financial state.

The durable source of truth consists primarily of:

- Transactions
- Cash entries
- Portfolio definitions
- Configuration
- Manual classification overrides

From these records the application can rebuild:

- Holdings
- Lots
- Trade cash movements
- Current balances
- Gains
- Allocation

This approach allows the application's state to be reconstructed consistently after backup restoration.

## Current Scope

### Included

- Android phone
- Single user
- Local data storage
- PIN/biometric protection
- Offline operation
- USD
- US stocks and ETFs
- Multiple portfolios
- Shared cash account
- Manual transaction entry
- Buys and sells
- Forward and reverse stock splits
- Fractional shares
- Specific-lot accounting
- Net worth
- Allocation
- Realized gains
- Unrealized gains
- Automatic price updates
- Backup and restore
- Privacy protections
- Backup reminders

### Not Included

The following are intentionally outside the current scope:

- Liabilities
- Loans
- Mortgages
- Credit cards
- Crypto
- Property
- Vehicles
- Mutual funds
- Bank synchronization
- Broker synchronization
- CSV import/export
- Data merging
- Manual price entry
- Tax reporting
- Tax-lot reporting
- Wash-sale calculations
- Separate per-holding dividend tracking
- Separate fee tracking
- Multi-currency support
- Currency conversion
- Android system notifications
- Play Store distribution
- Multi-user accounts
- Data sharing

## Planned Features

The following capabilities are planned for later versions:

### Historical Performance

Historical net-worth and portfolio values will be reconstructed from:

- Recorded transactions
- Historical market prices

No periodic snapshots are required.

Performance is intended to show changes in total value over time, although it will not independently isolate investment returns from deposits and withdrawals.

### Per-Holding Dividends

Future versions may allow a dividend entry to optionally identify:

- Ticker
- Portfolio

The first version treats dividends as cash-only entries.

### ETF Look-Through

Future versions may break ETF allocation into the sectors and geographies represented by the ETF.

The first version assigns one sector and one geography to each holding.

## Design Principles

ATS Asset System is built around several core principles:

- **Local first** — financial data lives on the device.
- **Offline first** — the application remains useful without connectivity.
- **Explicit transactions** — investment activity is represented as individual events.
- **Specific lots** — realized and unrealized gains are based on actual lots.
- **Reconstructable state** — derived data can be rebuilt from durable records.
- **Fail safely** — invalid restores and blocked financial operations do not partially modify data.
- **Privacy by default** — sensitive financial information is hidden from screenshots and recent-app previews.
- **Minimal external dependencies** — market data uses free sources and is treated as best-effort.
- **Single-user simplicity** — the application is optimized for personal use rather than multi-user workflows.

## Status

ATS Asset System is a personal, Android-focused financial asset tracking application.

The first milestone is considered complete when:

- Real financial data can be represented
- Net worth is calculated correctly
- Allocation is displayed correctly
- Transactions update holdings and cash correctly
- Lot-based gains are calculated correctly
- Updates are fast enough for normal use
- Backup and restore work end-to-end

## License

See the repository's license information for licensing terms.
