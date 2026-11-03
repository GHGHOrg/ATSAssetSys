package app.ghghorg.atsassets.ui

import app.ghghorg.atsassets.core.AmountError
import app.ghghorg.atsassets.core.CashEntryRefusal
import app.ghghorg.atsassets.core.ReentryRefusal

/*
 * The cash entry form's messages at Review as a screen model (CR2; 15a decision 1): which messages show and where,
 * worked out and unit tested; the screen only chooses the approved string (D9). In its own file, not ScreenModels.kt,
 * so 15a's steps 3 and 4 could be committed apart (15a file, section 14).
 */

/** The note field's message: a line break is shown in place of "note required" (04, 05). */
enum class NoteMessage {
    LINE_BREAK,
    REQUIRED,
}

/** A message below the form, in red. */
sealed interface CashFormMessage {
    data class Save(
        val refusal: SaveRefusal,
    ) : CashFormMessage

    /** K1's cash rule; while [replacing], it names the replacement (14a wording 1). */
    data class Cash(
        val refusal: CashEntryRefusal,
        val replacing: Boolean,
    ) : CashFormMessage

    /** The pair's own rule while a cash entry is replaced (14b). */
    data class Replace(
        val refusal: ReentryRefusal,
    ) : CashFormMessage
}

/**
 * Every refusal at Review shows together, each where it belongs: the date, amount and note under their fields, the rest
 * below the form (05 phone check 5: one refusal must not hide another).
 */
data class CashFormMessages(
    val date: DateRefusal?,
    val amount: AmountError?,
    val note: NoteMessage?,
    val below: List<CashFormMessage>,
)

object CashForm {
    /** The messages for [refusals]; while [replacing], the pair's [replaceRule] follows the cash rule. */
    fun messages(
        refusals: Refusals,
        replacing: Boolean,
        replaceRule: ReentryRefusal?,
    ): CashFormMessages {
        val note =
            when {
                refusals.noteHasLineBreak -> NoteMessage.LINE_BREAK
                refusals.noteRequired -> NoteMessage.REQUIRED
                else -> null
            }
        val below =
            listOfNotNull(
                refusals.save?.let { CashFormMessage.Save(it) },
                refusals.cash?.let { CashFormMessage.Cash(it, replacing) },
                replaceRule?.takeIf { replacing }?.let { CashFormMessage.Replace(it) },
            )
        return CashFormMessages(refusals.date, refusals.amount, note, below)
    }
}

/** The cash entry form's messages, as the screen draws them (decision 1: built by [CashForm], read here). */
val AppState.cashFormMessages: CashFormMessages
    get() = CashForm.messages(refusals, replacing = replacedCashEntry != null, replaceRule = replaceRefusals.rule)
