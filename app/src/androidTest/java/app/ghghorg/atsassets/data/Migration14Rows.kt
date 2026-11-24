package app.ghghorg.atsassets.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The rows both migration tests put into a version 14 database, and what version 15 must read back (41 step 5; AC-71):
 * MigrationFrom14Test builds the file by hand from 14.json, MigrationFrom14HelperTest with Room's MigrationTestHelper.
 * One place for both, so equal results compare the same input and the same expectation.
 */
internal object Migration14Rows {
    val PASSPHRASE = ByteArray(32) { 7 }

    /** One cash entry and one instrument with a stale price and a profile, as 1.0.0 stores them. */
    fun insert(connection: SQLiteConnection) {
        connection.execSQL(
            "INSERT INTO cash_entries (id, date, amountCents, type, note, enteredSeq, reversesId, transactionId) " +
                "VALUES ('o1', '2026-01-01', 960000, 'OPENING_BALANCE', '', 1, NULL, NULL)",
        )
        connection.execSQL(
            "INSERT INTO instruments (ticker, assetType, verified, price, priceTime, priceStale, sourceSector, " +
                "sourceGeography, profileAsked) VALUES ('AAPL', 'STOCK', 1, '348.9100', 1793304000, 1, 'Technology', 'US', 1)",
        )
    }

    val CASH = listOf(CashEntryRow("o1", "2026-01-01", 960_000L, "OPENING_BALANCE", "", 1L, null, null))

    // Every column kept, and no previous close until the first refresh (AC-71).
    val INSTRUMENTS =
        listOf(
            InstrumentRow(
                ticker = "AAPL",
                assetType = "STOCK",
                verified = true,
                price = "348.9100",
                priceTime = 1_793_304_000L,
                priceStale = true,
                pricePreviousClose = null,
                sourceSector = "Technology",
                sourceGeography = "US",
                profileAsked = true,
            ),
        )
}
