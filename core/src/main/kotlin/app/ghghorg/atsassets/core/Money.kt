package app.ghghorg.atsassets.core

/** Why a typed amount was refused (spec 4.2 and the number rules of section 8). */
enum class AmountError {
    EMPTY,
    BAD_FORMAT,
    TOO_MANY_DECIMALS,
    ZERO,
    TOO_LARGE,
}

sealed interface AmountResult {
    data class Cents(
        val value: Long,
    ) : AmountResult

    data class Refused(
        val reason: AmountError,
    ) : AmountResult
}

/** STUB (iteration 02a, commit A). Commit B implements it. */
object Money {
    fun parseCents(text: String): AmountResult = TODO("commit B")

    fun format(cents: Long): String = TODO("commit B")
}
