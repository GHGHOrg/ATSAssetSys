package app.ghghorg.atsassets.core

import java.time.LocalDate

/** The six cash entry types of spec section 3. `TRADE` is made only by buys and sells. */
enum class CashType {
    OPENING_BALANCE,
    DEPOSIT,
    WITHDRAWAL,
    INTEREST_DIVIDEND,
    ADJUSTMENT,
    TRADE,
}

/** One cash entry (spec section 3). The amount is signed, in integer cents (4.10). */
data class CashEntry(
    val id: String,
    val date: LocalDate,
    val amountCents: Long,
    val type: CashType,
    val note: String,
    val enteredSeq: Long,
    val reversesId: String? = null,
    val transactionId: String? = null,
)
