package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OpeningBalanceTest {
    private val jan1 = LocalDate.of(2026, 1, 1)

    private fun existingOpening(): CashEntry = CashEntry(
        id = "old",
        date = jan1,
        amountCents = 1_000_000L,
        type = CashType.OPENING_BALANCE,
        note = "",
        enteredSeq = 1L,
    )

    private fun create(
        existing: List<CashEntry> = emptyList(),
        date: LocalDate = jan1,
        amountCents: Long = 1_000_000L,
        note: String = "",
    ): OpeningBalanceResult = OpeningBalance.create(existing, "new", date, amountCents, note)

    private fun refused(reason: OpeningBalanceError): OpeningBalanceResult = OpeningBalanceResult.Refused(reason)

    @Test
    fun create_emptyApp_returnsTheOpeningBalanceEntry() {
        val expected =
            CashEntry(
                id = "new",
                date = jan1,
                amountCents = 1_000_000L,
                type = CashType.OPENING_BALANCE,
                note = "",
                enteredSeq = 1L,
                reversesId = null,
                transactionId = null,
            )
        assertEquals(OpeningBalanceResult.Created(expected), create())
    }

    @Test
    fun create_keepsTheNoteAsTyped() {
        val result = create(note = "  from my broker, 2026 ")
        val entry = (result as OpeningBalanceResult.Created).entry
        assertEquals("  from my broker, 2026 ", entry.note)
    }

    @Test
    fun create_dateInTheFuture_isAccepted() {
        val result = create(date = LocalDate.of(2099, 1, 1))
        assertTrue(result is OpeningBalanceResult.Created)
    }

    @Test
    fun create_whenOneExists_isRefusedAsAlreadyExists() {
        assertEquals(
            refused(OpeningBalanceError.ALREADY_EXISTS),
            create(existing = listOf(existingOpening())),
        )
    }

    @Test
    fun create_whenOneExistsAndAmountIsZero_reportsAlreadyExistsFirst() {
        assertEquals(
            refused(OpeningBalanceError.ALREADY_EXISTS),
            create(existing = listOf(existingOpening()), amountCents = 0L),
        )
    }

    @Test
    fun create_zeroOrNegativeAmount_isRefused() {
        assertEquals(refused(OpeningBalanceError.AMOUNT_NOT_POSITIVE), create(amountCents = 0L))
        assertEquals(refused(OpeningBalanceError.AMOUNT_NOT_POSITIVE), create(amountCents = -1L))
    }

    @Test
    fun create_noteWithLineBreak_isRefused() {
        assertEquals(refused(OpeningBalanceError.NOTE_HAS_LINE_BREAK), create(note = "a\nb"))
        assertEquals(refused(OpeningBalanceError.NOTE_HAS_LINE_BREAK), create(note = "a\rb"))
    }

    @Test
    fun appHoldsData_noEntries_isFalse() {
        assertFalse(CashRules.appHoldsData(emptyList()))
    }

    @Test
    fun appHoldsData_withAnEntry_isTrue() {
        assertTrue(CashRules.appHoldsData(listOf(existingOpening())))
    }
}
