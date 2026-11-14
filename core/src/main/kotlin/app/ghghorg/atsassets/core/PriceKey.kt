package app.ghghorg.atsassets.core

/**
 * The price source's API key (30; spec section 9: a request detail of the source). The app never shows a key in full
 * and never logs it; a screen gets [ending] only.
 */
object PriceKey {
    /** How many of a saved key's last characters are shown (30 decision 4). */
    const val SHOWN = 4

    /**
     * Letters a to z in either case and digits 0 to 9 only, and at least one: the key travels in a request header, where
     * a space, a line break or any other character could change the request (30, risk 3). The build's rule until 30.
     */
    fun isKey(text: String): Boolean = text.isNotEmpty() && text.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }

    /**
     * The last [SHOWN] characters of [key], the only part of a saved key that is shown. A key of [SHOWN] characters or
     * fewer shows nothing, as its ending would be the whole key.
     */
    fun ending(key: String): String = if (key.length > SHOWN) key.takeLast(SHOWN) else ""
}
