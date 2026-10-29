package app.ghghorg.atsassets.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

/** Creating a portfolio (spec 4.7, criteria 26 and 55; iteration 07). */
class PortfoliosTest {
    private val opening =
        listOf(
            CashEntry(
                id = "ob",
                date = LocalDate.parse("2026-01-01"),
                amountCents = 100_000L,
                type = CashType.OPENING_BALANCE,
                note = "",
                enteredSeq = 1L,
            ),
        )

    private val main = Portfolio(id = "p1", name = "Main", createdSeq = 1L)

    private fun create(
        typedName: String,
        existing: List<Portfolio> = emptyList(),
        cashEntries: List<CashEntry> = opening,
    ): PortfolioResult = Portfolios.create(existing, cashEntries, "new", typedName)

    private fun created(result: PortfolioResult): Portfolio = (result as PortfolioResult.Created).portfolio

    private fun refusal(result: PortfolioResult): PortfolioRefusal = (result as PortfolioResult.Refused).reason

    @Test
    fun aValidName_isCreatedWithTheIdAndTheFirstSeq() {
        assertEquals(Portfolio(id = "new", name = "Main", createdSeq = 1L), created(create("Main")))
    }

    @Test
    fun theNameIsSavedTrimmed_withItsCasingAndInnerSpacesKept() {
        assertEquals("my  Main", created(create("  my  Main  ")).name)
    }

    @Test
    fun aNewPortfolioComesAfterTheExistingOnes() {
        val second = Portfolio(id = "p2", name = "Second", createdSeq = 2L)
        assertEquals(3L, created(create("Third", listOf(main, second))).createdSeq)
    }

    @Test
    fun theListIsInCreationOrder_whateverTheStoredOrder() {
        val second = Portfolio(id = "p2", name = "Second", createdSeq = 2L)
        assertEquals(listOf(main, second), Portfolios.inCreationOrder(listOf(second, main)))
    }

    @Test
    fun withoutAnOpeningBalance_creationIsRefused() {
        assertEquals(PortfolioRefusal.NoOpeningBalance, refusal(create("Main", cashEntries = emptyList())))
    }

    @Test
    fun theMissingOpeningBalanceIsReportedBeforeTheName() {
        assertEquals(PortfolioRefusal.NoOpeningBalance, refusal(create("", cashEntries = emptyList())))
    }

    // Criterion 55: "投資" (2 characters) is refused, "投資信託" (4) is accepted.
    @Test
    fun fewerThanThreeCharacters_isRefused() {
        assertEquals(PortfolioRefusal.TooShort, refusal(create("投資")))
        assertEquals(PortfolioRefusal.TooShort, refusal(create("ab")))
        assertEquals(PortfolioRefusal.TooShort, refusal(create("")))
        assertEquals("投資信託", created(create("投資信託")).name)
    }

    @Test
    fun outerSpacesDoNotCount_innerSpacesDo() {
        assertEquals(PortfolioRefusal.TooShort, refusal(create("  ab  ")))
        assertEquals(PortfolioRefusal.TooShort, refusal(create("     ")))
        assertEquals("a b", created(create("a b")).name)
    }

    // An emoji is one code point but two UTF-16 chars: "a😀" is 2 characters.
    @Test
    fun theLengthIsCountedInCodePoints() {
        assertEquals(PortfolioRefusal.TooShort, refusal(create("a😀")))
        assertEquals("a😀b", created(create("a😀b")).name)
    }

    // Criterion 55: "123" and "---" are refused.
    @Test
    fun aNameWithNoLetter_isRefused() {
        assertEquals(PortfolioRefusal.NoLetter, refusal(create("123")))
        assertEquals(PortfolioRefusal.NoLetter, refusal(create("---")))
        assertEquals(PortfolioRefusal.NoLetter, refusal(create("😀😀😀")))
        assertEquals(PortfolioRefusal.NoLetter, refusal(create("1 2 3")))
    }

    @Test
    fun lettersOfAnyScriptCount() {
        assertEquals("かなカ", created(create("かなカ")).name)
        assertEquals("한국어", created(create("한국어")).name)
        assertEquals("Ελλ", created(create("Ελλ")).name)
        assertEquals("a12", created(create("a12")).name)
    }

    @Test
    fun theLengthIsCheckedBeforeTheLetter() {
        assertEquals(PortfolioRefusal.TooShort, refusal(create("12")))
    }

    @Test
    fun aMatchingName_isRefusedAndNamesTheOtherPortfolio() {
        assertEquals(PortfolioRefusal.Duplicate("Main"), refusal(create("Main", listOf(main))))
        assertEquals(PortfolioRefusal.Duplicate("Main"), refusal(create("MAIN", listOf(main))))
        assertEquals(PortfolioRefusal.Duplicate("Main"), refusal(create(" main ", listOf(main))))
    }

    @Test
    fun repeatedInnerSpacesMatch_butRemovedSpacesDoNot() {
        val account = Portfolio(id = "p2", name = "Main account", createdSeq = 2L)
        assertEquals(PortfolioRefusal.Duplicate("Main account"), refusal(create("main   ACCOUNT", listOf(account))))
        assertEquals("Mainaccount", created(create("Mainaccount", listOf(account))).name)
    }

    // My reading (07 file, risks): case is folded through upper case, so "ß" matches "SS".
    @Test
    fun theNameKeyFoldsCaseAndSpaces() {
        assertEquals(Portfolios.nameKey("Straße"), Portfolios.nameKey("STRASSE"))
        assertEquals(Portfolios.nameKey("Main account"), Portfolios.nameKey("\tmain\n account "))
        assertNotEquals(Portfolios.nameKey("Main account"), Portfolios.nameKey("Mainaccount"))
    }

    @Test
    fun theLetterIsCheckedBeforeTheDuplicate() {
        val dashes = Portfolio(id = "p2", name = "---", createdSeq = 2L)
        assertEquals(PortfolioRefusal.NoLetter, refusal(create("---", listOf(dashes))))
    }
}
