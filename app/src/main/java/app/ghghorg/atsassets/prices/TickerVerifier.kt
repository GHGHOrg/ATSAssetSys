package app.ghghorg.atsassets.prices

import app.ghghorg.atsassets.core.Instrument
import app.ghghorg.atsassets.core.Lookup
import app.ghghorg.atsassets.core.PriceSource
import app.ghghorg.atsassets.core.RecordStore
import app.ghghorg.atsassets.core.TickerCheck
import app.ghghorg.atsassets.core.TickerRefusal
import app.ghghorg.atsassets.core.TickerVerdict

/** Why the ticker check of 4.3 refused or could not be made (09 decisions 6 and 8). Reasons only; the wording is D9. */
sealed interface CheckRefusal {
    /** The ticker was never verified and the device is offline (4.3). */
    data object Offline : CheckRefusal

    data object NoKey : CheckRefusal

    /** No connection or a timeout ([status] null), or a status other than 200, 401, 403 and 429. */
    data class NoAnswer(
        val status: Int?,
    ) : CheckRefusal

    data class KeyRefused(
        val status: Int,
    ) : CheckRefusal

    data object TooManyCalls : CheckRefusal

    /** Status 200 with a body the app cannot read: a content problem. */
    data object Unreadable : CheckRefusal

    /** The source answered and the check of 4.3 refuses the ticker (not found, mutual fund, exchange, currency). */
    data class Ticker(
        val reason: TickerRefusal,
    ) : CheckRefusal
}

sealed interface Verification {
    data class Verified(
        val instrument: Instrument,
    ) : Verification

    data class Refused(
        val reason: CheckRefusal,
    ) : Verification
}

/**
 * K4 (spec 4.3, 7; criterion 15): checks a ticker that is not verified yet, and on success writes the instrument as
 * verified, so it is never checked again (D6). Only K4 writes instruments (CLAUDE.md conventions).
 */
object TickerVerifier {
    /**
     * [ticker] is trimmed and in capitals. A verified ticker passes at once, offline too. Otherwise offline is refused
     * without asking the source, and an online look-up decides through TickerCheck. Store failures propagate.
     */
    suspend fun verify(
        ticker: String,
        instruments: List<Instrument>,
        online: Boolean,
        source: PriceSource,
        store: RecordStore,
    ): Verification {
        val result = check(ticker, instruments, online, source)
        if (result is Verification.Verified && instruments.none { it.ticker == ticker && it.verified }) {
            store.saveInstrument(result.instrument)
        }
        return result
    }

    /**
     * As [verify], but nothing is stored: a newly verified ticker comes back as an instrument only. The restore check uses
     * it, so the app is unchanged until the restore itself (33b decision 2; C3).
     */
    suspend fun check(
        ticker: String,
        instruments: List<Instrument>,
        online: Boolean,
        source: PriceSource,
    ): Verification {
        instruments.firstOrNull { it.ticker == ticker && it.verified }?.let { return Verification.Verified(it) }
        if (!online) return Verification.Refused(CheckRefusal.Offline)
        val refusal =
            when (val lookup = source.lookUp(ticker)) {
                is Lookup.Found -> {
                    when (val verdict = TickerCheck.verdict(lookup.listing)) {
                        is TickerVerdict.Accepted -> {
                            return Verification.Verified(Instrument(ticker = ticker, assetType = verdict.assetType, verified = true))
                        }

                        is TickerVerdict.Refused -> CheckRefusal.Ticker(verdict.reason)
                    }
                }

                Lookup.NotFound -> CheckRefusal.Ticker(TickerRefusal.NotFound)

                Lookup.Unreadable -> CheckRefusal.Unreadable

                is Lookup.KeyRefused -> CheckRefusal.KeyRefused(lookup.status)

                Lookup.TooManyCalls -> CheckRefusal.TooManyCalls

                is Lookup.NoAnswer -> CheckRefusal.NoAnswer(lookup.status)

                Lookup.NoKey -> CheckRefusal.NoKey
            }
        return Verification.Refused(refusal)
    }
}
