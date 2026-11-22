package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.DeletionCheck
import app.ghghorg.atsassets.core.Money
import app.ghghorg.atsassets.core.PortfolioDeletion
import java.time.LocalDate

/**
 * The Delete dialog as the screen draws it (wireframe 10d frames D and E; CR2; 29): the portfolio's name, the summary's
 * counts and figures (the realized gain signed, its sign also its color, section 6; the balances on the latest entry
 * date, 4.8), the name as typed, whether Delete portfolio is active, and the state of the save.
 */
data class DeleteDialogModel(
    val name: String,
    val transactionCount: Int,
    val tradeLineCount: Int,
    val realizedGain: String,
    val spokenRealizedGain: SpokenFigure,
    val balanceNow: String,
    val spokenBalanceNow: SpokenFigure,
    val balanceAfter: String,
    val spokenBalanceAfter: SpokenFigure,
    val typedName: String,
    val deleteEnabled: Boolean,
    val saving: Boolean,
    val saveFailed: Boolean,
)

/**
 * The blocked message (frame F; 4.7): the first date as YYYY-MM-DD, the largest shortfall, and for a deletion that can
 * be fixed the day before the first date as the example (Example E: "for example Feb 9"); [cannotDelete] when the
 * first date is the opening balance's.
 */
data class DeleteBlockedModel(
    val cannotDelete: Boolean,
    val date: String,
    val shortfall: String,
    val exampleDate: String?,
)

/** Builds the two models from the state (29). */
object PortfolioDeletions {
    /** The gain rounded to cents once (4.10), so its sign, and its color, are the shown figure's: $0.00 has none. */
    fun dialog(
        dialog: DeleteDialog,
        saving: Boolean,
    ): DeleteDialogModel {
        val summary = dialog.summary
        val gainCents = summary.realizedGain.rounded(2).movePointRight(2).longValueExact()
        return DeleteDialogModel(
            name = summary.portfolio.name,
            transactionCount = summary.transactionCount,
            tradeLineCount = summary.tradeLineCount,
            realizedGain = signedAmount(gainCents),
            spokenRealizedGain = SpokenFigure.ofAmount(gainCents),
            balanceNow = Money.format(summary.balanceNowCents),
            spokenBalanceNow = SpokenFigure.ofBalance(summary.balanceNowCents),
            balanceAfter = Money.format(summary.balanceAfterCents),
            spokenBalanceAfter = SpokenFigure.ofBalance(summary.balanceAfterCents),
            typedName = dialog.typedName,
            deleteEnabled = !saving && PortfolioDeletion.nameMatches(summary.portfolio, dialog.typedName),
            saving = saving,
            saveFailed = dialog.save == SaveRefusal.SAVE_FAILED,
        )
    }

    /** The message for a blocked check, null for anything else. */
    fun blocked(check: DeletionCheck?): DeleteBlockedModel? = when (check) {
        is DeletionCheck.Blocked -> message(false, check.firstDate, check.largestShortfallCents)
        is DeletionCheck.CannotDelete -> message(true, check.firstDate, check.largestShortfallCents)
        else -> null
    }

    private fun message(
        cannotDelete: Boolean,
        date: LocalDate,
        shortfallCents: Long,
    ) = DeleteBlockedModel(
        cannotDelete = cannotDelete,
        date = date.toString(),
        shortfall = Money.format(shortfallCents),
        exampleDate = if (cannotDelete) null else date.minusDays(1).toString(),
    )
}
