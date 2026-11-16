package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.RestoreError
import app.ghghorg.atsassets.core.RestoreReason
import app.ghghorg.atsassets.core.RestoreSummary
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.prices.CheckRefusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// 33b step 4: the wizard's models (CR2; section 8, rejection reasons and the report; wireframe 10e frames A to F): every
// reason to its sentence and values, the count line, the places, the second round, the file problems, step 2, the summary,
// Restore on Backup and restore, and the copied text equal to the shown text (C16).
class RestoreModelTest {
    private val date = LocalDate.parse("2026-02-10")

    private val held = BigDecimal("1.66666667")

    private fun w(
        text: RestoreText,
        vararg args: String,
    ) = RestoreWords(text, args.toList())

    // Risk 1: one row per reason of section 8 and 33a's additions, with the values each sentence takes.
    private val everyReason: List<Pair<RestoreReason, RestoreWords>> =
        listOf(
            RestoreReason.NotUtf8 to w(RestoreText.NOT_UTF8),
            RestoreReason.NoHeader to w(RestoreText.NO_HEADER),
            RestoreReason.MissingColumn("ticker") to w(RestoreText.MISSING_COLUMN, "ticker"),
            RestoreReason.UnknownColumn("price") to w(RestoreText.UNKNOWN_COLUMN, "price"),
            RestoreReason.UnknownColumn("") to w(RestoreText.UNNAMED_COLUMN),
            RestoreReason.RepeatedColumn("date") to w(RestoreText.REPEATED_COLUMN, "date"),
            RestoreReason.NoFormatRow to w(RestoreText.NO_FORMAT_ROW),
            RestoreReason.VersionNotWhole to w(RestoreText.VERSION_NOT_WHOLE),
            RestoreReason.VersionUnknown("2") to w(RestoreText.VERSION_UNKNOWN, "2", "1"),
            RestoreReason.CellCount(18, 19) to w(RestoreText.CELL_COUNT, "18", "19"),
            RestoreReason.LineBreakInCell to w(RestoreText.LINE_BREAK_IN_CELL),
            RestoreReason.RowTypeMissing to w(RestoreText.ROW_TYPE_MISSING),
            RestoreReason.RowTypeUnknown("dividend") to w(RestoreText.ROW_TYPE_UNKNOWN, "dividend"),
            RestoreReason.FormatNotFirst to w(RestoreText.FORMAT_NOT_FIRST),
            RestoreReason.ValueMissing to w(RestoreText.VALUE_MISSING),
            RestoreReason.CellNotUsed to w(RestoreText.CELL_NOT_USED),
            RestoreReason.DateForm to w(RestoreText.DATE_FORM),
            RestoreReason.DateNotReal to w(RestoreText.DATE_NOT_REAL),
            RestoreReason.NumberForm to w(RestoreText.NUMBER_FORM),
            RestoreReason.QuantityNotPositive to w(RestoreText.QUANTITY_NOT_POSITIVE),
            RestoreReason.QuantityTooManyDecimals to w(RestoreText.QUANTITY_TOO_MANY_DECIMALS),
            RestoreReason.AmountTooManyDecimals to w(RestoreText.AMOUNT_TOO_MANY_DECIMALS),
            RestoreReason.AmountTooLarge to w(RestoreText.AMOUNT_TOO_LARGE),
            RestoreReason.WrongSign to w(RestoreText.WRONG_SIGN),
            RestoreReason.CashAmountZero to w(RestoreText.CASH_AMOUNT_ZERO),
            RestoreReason.BuyTotalZero to w(RestoreText.BUY_TOTAL_ZERO),
            RestoreReason.IdBadlyFormed to w(RestoreText.ID_BADLY_FORMED),
            RestoreReason.PortfolioNameTooShort to w(RestoreText.PORTFOLIO_NAME_TOO_SHORT),
            RestoreReason.PortfolioNameNoLetter to w(RestoreText.PORTFOLIO_NAME_NO_LETTER),
            RestoreReason.CashTypeUnknown to w(RestoreText.CASH_TYPE_UNKNOWN),
            RestoreReason.CashTypeTrade to w(RestoreText.CASH_TYPE_TRADE),
            RestoreReason.AdjustmentWithoutNote to w(RestoreText.ADJUSTMENT_WITHOUT_NOTE),
            RestoreReason.TickerBadCharacters to w(RestoreText.TICKER_BAD_CHARACTERS),
            RestoreReason.SplitNumberNotWhole to w(RestoreText.SPLIT_NUMBER_NOT_WHOLE),
            RestoreReason.SplitNumbersEqual to w(RestoreText.SPLIT_NUMBERS_EQUAL),
            RestoreReason.SplitNumberTooLarge to w(RestoreText.SPLIT_NUMBER_TOO_LARGE, "1,000,000"),
            RestoreReason.BuyRatioTooLarge to w(RestoreText.BUY_RATIO_TOO_LARGE),
            RestoreReason.PicksBadlyWritten to w(RestoreText.PICKS_BADLY_WRITTEN),
            RestoreReason.OverrideBothEmpty to w(RestoreText.OVERRIDE_BOTH_EMPTY),
            RestoreReason.ListUnknown to w(RestoreText.LIST_UNKNOWN),
            RestoreReason.SettingKeyUnknown to w(RestoreText.SETTING_KEY_UNKNOWN),
            RestoreReason.SettingValueUnknown to w(RestoreText.SETTING_VALUE_UNKNOWN),
            RestoreReason.TabOrderNotEachOnce to w(RestoreText.TAB_ORDER_NOT_EACH_ONCE),
            RestoreReason.IdDuplicate(1234) to w(RestoreText.ID_DUPLICATE, "1,234"),
            RestoreReason.PortfolioNameDuplicate(3) to w(RestoreText.PORTFOLIO_NAME_DUPLICATE, "3"),
            RestoreReason.PortfolioNotFound to w(RestoreText.PORTFOLIO_NOT_FOUND),
            RestoreReason.NoOpeningBalance to w(RestoreText.NO_OPENING_BALANCE),
            RestoreReason.SecondOpeningBalance(4) to w(RestoreText.SECOND_OPENING_BALANCE, "4"),
            RestoreReason.OpeningBalanceNotEarliest to w(RestoreText.OPENING_BALANCE_NOT_EARLIEST),
            RestoreReason.PickNotFound("T9") to w(RestoreText.PICK_NOT_FOUND, "T9"),
            RestoreReason.PickNotABuy("S1") to w(RestoreText.PICK_NOT_A_BUY, "S1"),
            RestoreReason.PickOtherPortfolio("T2") to w(RestoreText.PICK_OTHER_PORTFOLIO, "T2"),
            RestoreReason.PickOtherTicker("T3") to w(RestoreText.PICK_OTHER_TICKER, "T3"),
            RestoreReason.PickTwice("T1") to w(RestoreText.PICK_TWICE, "T1"),
            RestoreReason.LotBoughtAfterSell("T4") to w(RestoreText.LOT_BOUGHT_AFTER_SELL, "T4"),
            RestoreReason.LotEnteredAfterSell("T5") to w(RestoreText.LOT_ENTERED_AFTER_SELL, "T5"),
            // Quantities as the lot picker shows them (4.4, 4.10): exact fractions at 8 places, rounding up and down.
            RestoreReason.PickOverLot("T1", BigDecimal("0.66666667")) to w(RestoreText.PICK_OVER_LOT, "T1", "0.66666667"),
            RestoreReason.PicksDoNotAddUp(BigDecimal("12.5"), BigDecimal("10")) to w(RestoreText.PICKS_DO_NOT_ADD_UP, "12.5", "10"),
            RestoreReason.ReversalTargetNotFound("C7") to w(RestoreText.REVERSAL_TARGET_NOT_FOUND, "C7"),
            RestoreReason.ReversalOfOpeningBalance to w(RestoreText.REVERSAL_OF_OPENING_BALANCE),
            RestoreReason.ReversalTargetListedAfter(20) to w(RestoreText.REVERSAL_TARGET_LISTED_AFTER, "20"),
            RestoreReason.AlreadyReversed(12) to w(RestoreText.ALREADY_REVERSED, "12"),
            RestoreReason.ReversalBeforeOriginal(date) to w(RestoreText.REVERSAL_BEFORE_ORIGINAL, "2026-02-10"),
            RestoreReason.HiddenHoldingNotFound to w(RestoreText.HOLDING_NOT_FOUND),
            RestoreReason.HiddenHoldingHasShares to w(RestoreText.HIDDEN_HOLDING_HAS_SHARES),
            RestoreReason.OverrideHoldingNotFound to w(RestoreText.HOLDING_NOT_FOUND),
            RestoreReason.OverrideValueNotOnList to w(RestoreText.OVERRIDE_VALUE_NOT_ON_LIST),
            RestoreReason.DuplicateListValue(30) to w(RestoreText.DUPLICATE_LIST_VALUE, "30"),
            RestoreReason.DuplicateRememberedText(31) to w(RestoreText.DUPLICATE_REMEMBERED_TEXT, "31"),
            RestoreReason.DuplicateHidden(32) to w(RestoreText.DUPLICATE_HIDDEN, "32"),
            RestoreReason.DuplicateOverride(33) to w(RestoreText.DUPLICATE_OVERRIDE, "33"),
            RestoreReason.ListValueMatchesRememberedText(34) to w(RestoreText.LIST_VALUE_MATCHES_REMEMBERED_TEXT, "34"),
            RestoreReason.RememberedTextValueNotOnList to w(RestoreText.REMEMBERED_TEXT_VALUE_NOT_ON_LIST),
            RestoreReason.SettingKeyTwice(40) to w(RestoreText.SETTING_KEY_TWICE, "40"),
            RestoreReason.CashNegative(date, 20_000L) to w(RestoreText.CASH_NEGATIVE, "2026-02-10", "$200.00"),
            RestoreReason.SharesNegative(date) to w(RestoreText.SHARES_NEGATIVE, "2026-02-10"),
            RestoreReason.SellMoreThanHeld(date, held) to w(RestoreText.SELL_MORE_THAN_HELD, "2026-02-10", "1.66666667"),
            RestoreReason.DatedBeforeOpeningBalance(date) to w(RestoreText.DATED_BEFORE_OPENING_BALANCE, "2026-02-10"),
            RestoreReason.SplitTickerNotBought to w(RestoreText.SPLIT_TICKER_NOT_BOUGHT),
            RestoreReason.BalanceTooLarge to w(RestoreText.BALANCE_TOO_LARGE),
            RestoreReason.DependsOn(14) to w(RestoreText.DEPENDS_ON, "14"),
        )

    @Test
    fun everyReason_hasItsSentence_andValues() {
        for ((reason, expected) in everyReason) assertEquals(reason.toString(), expected, Restores.reason(reason))
    }

    // Every kind of RestoreReason (each nested class) is in the table above (the `when` in Restores.reason is exhaustive; this keeps
    // the table complete too).
    @Test
    fun theTable_coversEveryKindOfReason() {
        val type = RestoreReason::class.java
        val kinds = type.declaredClasses.filter { type.isAssignableFrom(it) }.map { it.simpleName }.toSet()
        val covered = everyReason.map { it.first.javaClass.simpleName }.toSet()
        assertEquals(kinds, covered)
    }

    // Section 8: "N errors in M lines", the singular forms, and the file as a whole, which is no line.
    @Test
    fun theCountLine() {
        assertEquals(w(RestoreText.ERRORS_IN_LINES, "3", "2"), Restores.countLine(3, 2))
        assertEquals(w(RestoreText.ERRORS_IN_LINES, "1,204", "1,100"), Restores.countLine(1204, 1100))
        assertEquals(w(RestoreText.ERROR_IN_LINE), Restores.countLine(1, 1))
        assertEquals(w(RestoreText.ERRORS_IN_LINE, "2"), Restores.countLine(2, 1))
        assertEquals(w(RestoreText.ERROR_IN_FILE), Restores.countLine(1, 0))
        assertEquals(w(RestoreText.ERRORS_IN_FILE, "3"), Restores.countLine(3, 0))
    }

    // The header is line 1; the column when one applies; the file as a whole.
    @Test
    fun thePlaces() {
        val quantity = RestoreError(14, "quantity", RestoreReason.QuantityNotPositive)
        assertEquals(w(RestoreText.PLACE_LINE_COLUMN, "14", "quantity"), Restores.line(quantity).place)
        assertEquals(w(RestoreText.PLACE_LINE, "21"), Restores.line(RestoreError(21, null, RestoreReason.DependsOn(14))).place)
        assertEquals(w(RestoreText.PLACE_FILE), Restores.line(RestoreError(null, null, RestoreReason.NotUtf8)).place)
    }

    // Wireframe 10e frame E, with the first round's errors in the order the check gives.
    @Test
    fun theReport_ofTheFirstRound() {
        val errors =
            listOf(
                RestoreError(14, "quantity", RestoreReason.QuantityNotPositive),
                RestoreError(17, "lot_picks", RestoreReason.DependsOn(14)),
                RestoreError(21, null, RestoreReason.CashNegative(date, 20_000L)),
            )
        val report = Restores.report(RestoreWizard(step = RestoreStep.REPORT, errors = errors), "Finnhub")
        assertEquals(w(RestoreText.ERRORS_IN_LINES, "3", "3"), report.count)
        assertEquals(errors.map(Restores::line), report.lines)
    }

    // Section 8, restore step 3; wireframe 10e marker 12: the second round names each ticker and the source.
    @Test
    fun theReport_ofTheSecondRound() {
        val wizard =
            RestoreWizard(
                step = RestoreStep.REPORT,
                tickerErrors = listOf(TickerError("XYZ", TickerRefusal.NotFound), TickerError("VFIAX", TickerRefusal.MutualFund)),
            )
        val report = Restores.report(wizard, "Finnhub")
        assertEquals(w(RestoreText.TICKER_ERRORS, "2"), report.count)
        assertEquals(
            listOf(
                RestoreReportLine(w(RestoreText.PLACE_TICKER, "XYZ"), w(RestoreText.TICKER_NOT_FOUND, "Finnhub")),
                RestoreReportLine(w(RestoreText.PLACE_TICKER, "VFIAX"), w(RestoreText.TICKER_MUTUAL_FUND, "Finnhub")),
            ),
            report.lines,
        )
        val euro = TickerError("ABC", TickerRefusal.NotUsd("EUR"))
        val one = Restores.report(RestoreWizard(step = RestoreStep.REPORT, tickerErrors = listOf(euro)), "Finnhub")
        assertEquals(w(RestoreText.TICKER_ERROR), one.count)
        assertEquals(w(RestoreText.TICKER_NOT_USD, "Finnhub", "EUR"), one.lines.single().reason)
        val otc = Restores.tickerReason(TickerRefusal.NotUsExchange("OOTC"), "Finnhub")
        assertEquals(w(RestoreText.TICKER_NOT_US_EXCHANGE, "Finnhub", "OOTC"), otc)
    }

    // Decision 4, and a file that could not be read or checked: one error, the file as a whole.
    @Test
    fun theReport_ofAFileProblem() {
        val expected =
            mapOf(
                RestoreFileProblem.TOO_LARGE to RestoreText.FILE_TOO_LARGE,
                RestoreFileProblem.NOT_READ to RestoreText.FILE_NOT_READ,
                RestoreFileProblem.CHECK_FAILED to RestoreText.FILE_CHECK_FAILED,
            )
        for ((problem, text) in expected) {
            val report = Restores.report(RestoreWizard(step = RestoreStep.REPORT, fileProblem = problem), "Finnhub")
            assertEquals(w(RestoreText.ERROR_IN_FILE), report.count)
            assertEquals(listOf(RestoreReportLine(w(RestoreText.PLACE_FILE), w(text))), report.lines)
        }
    }

    // C16: the copied text is the shown text: the count line, then "<place>: <reason>" for each error, one per line.
    @Test
    fun theReportText_isTheCountLine_thenEachError() {
        val report =
            Restores.report(
                RestoreWizard(
                    step = RestoreStep.REPORT,
                    errors =
                    listOf(
                        RestoreError(14, "quantity", RestoreReason.QuantityNotPositive),
                        RestoreError(21, null, RestoreReason.CashNegative(date, 20_000L)),
                    ),
                ),
                "Finnhub",
            )
        val say: (RestoreWords) -> String = { "${it.text}(${it.args.joinToString()})" }
        assertEquals(
            listOf(
                "ERRORS_IN_LINES(2, 2)",
                "REPORT_LINE(PLACE_LINE_COLUMN(14, quantity), QUANTITY_NOT_POSITIVE())",
                "REPORT_LINE(PLACE_LINE(21), CASH_NEGATIVE(2026-02-10, \$200.00))",
            ).joinToString("\n"),
            Restores.reportText(report, say),
        )
        assertEquals(Restores.lineText(report.lines[0], say), Restores.reportText(report, say).lines()[1])
    }

    // Decision 3: each problem of the source has its line.
    @Test
    fun eachSourceProblem_hasItsLine() {
        val expected =
            mapOf(
                CheckRefusal.Offline to RestoreText.SOURCE_OFFLINE,
                CheckRefusal.NoKey to RestoreText.SOURCE_NO_KEY,
                CheckRefusal.NoAnswer(503) to RestoreText.SOURCE_NO_ANSWER,
                CheckRefusal.NoAnswer(null) to RestoreText.SOURCE_NO_ANSWER,
                CheckRefusal.KeyRefused(401) to RestoreText.SOURCE_KEY_REFUSED,
                CheckRefusal.TooManyCalls to RestoreText.SOURCE_TOO_MANY_CALLS,
                CheckRefusal.Unreadable to RestoreText.SOURCE_UNREADABLE,
            )
        for ((problem, text) in expected) assertEquals(problem.toString(), w(text), Restores.sourceProblem(problem))
    }

    // Wireframe 10e frames C and D.
    @Test
    fun steps1And2() {
        val none = Restores.screen(RestoreWizard(), "Finnhub")
        assertNull(none.fileName)
        assertTrue(none.canChoose)
        assertFalse(none.canCheck)
        assertTrue(none.backEnabled)
        val choosing = Restores.screen(RestoreWizard(fileName = "b.csv", choosing = true), "Finnhub")
        assertFalse(choosing.canChoose)
        assertFalse(choosing.canCheck)
        assertFalse(choosing.backEnabled)
        val chosen = Restores.screen(RestoreWizard(fileName = "b.csv"), "Finnhub")
        assertTrue(chosen.canCheck)
        assertEquals("b.csv", chosen.fileName)
        val reading = Restores.screen(RestoreWizard(step = RestoreStep.CHECKING), "Finnhub")
        assertFalse(reading.readingDone)
        assertNull(reading.progress)
        val round = RestoreWizard(step = RestoreStep.CHECKING, phase = CheckPhase.VERIFYING, verified = 1, toVerify = 3)
        val verifying = Restores.screen(round, "Finnhub")
        assertTrue(verifying.readingDone)
        assertEquals(w(RestoreText.VERIFY_PROGRESS, "2", "3"), verifying.progress)
        val stopped = Restores.screen(round.copy(verified = 3, sourceProblem = CheckRefusal.NoKey), "Finnhub")
        assertEquals(w(RestoreText.VERIFY_PROGRESS, "3", "3"), stopped.progress)
        assertEquals(w(RestoreText.SOURCE_NO_KEY), stopped.sourceProblem)
        assertNull(stopped.report)
    }

    // Section 8, restore step 5; AC-56's figures for spec/example-backup.csv (decision 1: the file's counts only).
    @Test
    fun theSummary() {
        val summary = RestoreSummary(portfolios = 2, transactions = 8, cashEntries = 4, cashBalanceCents = 942_500L, holdingsWithShares = 1)
        val model = Restores.screen(RestoreWizard(step = RestoreStep.SUMMARY, summary = summary), "Finnhub")
        assertEquals(RestoreSummaryModel("2", "8", "4", "$9,425.00", "1"), model.summary)
        assertNull(model.report)
    }

    // Wireframe 10e frame A, marker 4; AC-08: offline, Restore is inactive and a line says why; it needs no data.
    @Test
    fun restoreOnBackupAndRestore() {
        val moment = Instant.parse("2026-10-04T03:00:00Z")
        fun screen(
            holdsData: Boolean,
            stage: BackupStage?,
            online: Boolean,
        ) = Backups.screen(moment, ZoneOffset.UTC, false, holdsData, stage, outcome = null, saving = false, online = online)
        val online = screen(holdsData = false, stage = null, online = true)
        assertTrue(online.canRestore)
        assertFalse(online.restoreOffline)
        val offline = screen(holdsData = true, stage = null, online = false)
        assertFalse(offline.canRestore)
        assertTrue(offline.restoreOffline)
        val exporting = screen(holdsData = true, stage = BackupStage.CHOOSING, online = true)
        assertFalse(exporting.canRestore)
        assertFalse(exporting.restoreOffline)
    }
}
