package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The price source's row and key entry as Settings draws them (CR2; 30 decisions 3 and 4). */
class PriceKeyModelTest {
    @Test
    fun theRow_namesTheSource_andShowsTheKeysEnding_spacedForTalkBack() {
        assertEquals(
            PriceSourceRow(source = "Finnhub", keySet = true, keyEnding = "a1b2", spokenEnding = "a 1 b 2"),
            PriceKeys.row("Finnhub", keySet = true, ending = "a1b2"),
        )
    }

    @Test
    fun theRow_withoutAKey_hasNoEnding() {
        assertEquals(
            PriceSourceRow(source = "Finnhub", keySet = false, keyEnding = "", spokenEnding = ""),
            PriceKeys.row("Finnhub", keySet = false, ending = ""),
        )
    }

    @Test
    fun theRow_withAKeyTooShortToShow_saysOnlyThatOneIsSaved() {
        val row = PriceKeys.row("Finnhub", keySet = true, ending = "")
        assertTrue(row.keySet)
        assertEquals("", row.keyEnding)
        assertEquals("", row.spokenEnding)
    }

    @Test
    fun noEntry_isNoModel() {
        assertNull(PriceKeys.entry(null, "Finnhub", keySet = true, ending = "a1b2", saving = false))
    }

    @Test
    fun theEntry_carriesTheSource_theSavedKeysState_andWhatWasTyped() {
        val model = PriceKeys.entry(KeyEntry(text = "abc123"), "Finnhub", keySet = true, ending = "a1b2", saving = false)
        assertEquals(
            KeyEntryModel(
                source = "Finnhub",
                keySet = true,
                keyEnding = "a1b2",
                spokenEnding = "a 1 b 2",
                text = "abc123",
                refusal = null,
                busy = false,
                saveEnabled = true,
                confirmingClear = false,
            ),
            model,
        )
    }

    @Test
    fun save_isActiveOnlyWithSomethingTyped_andNotWhileBusy() {
        fun enabled(
            text: String,
            saving: Boolean,
        ) = PriceKeys.entry(KeyEntry(text = text), "Finnhub", keySet = false, ending = "", saving = saving)!!.saveEnabled
        assertFalse(enabled("", saving = false))
        assertFalse(enabled("   ", saving = false))
        assertTrue(enabled("abc", saving = false))
        assertFalse(enabled("abc", saving = true))
    }

    @Test
    fun theEntry_whileTheKeyIsCheckedOrStored_isBusy() {
        val model = PriceKeys.entry(KeyEntry(text = "abc"), "Finnhub", keySet = false, ending = "", saving = true)!!
        assertTrue(model.busy)
    }

    @Test
    fun theEntry_carriesTheRefusal_andTheClearQuestion() {
        val refused = PriceKeys.entry(KeyEntry(text = "abc", refusal = KeyRefusal.Refused(401)), "Finnhub", keySet = true, ending = "a1b2", saving = false)!!
        assertEquals(KeyRefusal.Refused(401), refused.refusal)
        assertFalse(refused.confirmingClear)
        val asking = PriceKeys.entry(KeyEntry(confirmingClear = true), "Finnhub", keySet = true, ending = "a1b2", saving = false)!!
        assertTrue(asking.confirmingClear)
    }
}
