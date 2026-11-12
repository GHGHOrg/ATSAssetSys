package app.ghghorg.atsassets.ui

/*
 * More as the screen draws it (section 6, More; wireframe 10f; CR2, 17): the last backup line and the entries built so
 * far. Plain Kotlin: the screen picks the approved string for each.
 */

/** The entries of More, in the order of wireframe 10f; only those built so far (17 decision 1). */
enum class MoreEntry {
    NET_WORTH,

    // 19 decision 1: on the first portfolio.
    ALLOCATION,

    // 26: view 5.
    REALIZED_GAINS,
}

/** [lastBackup] is the last backup's time, null for "never backed up" (no backup until 31). */
data class MoreModel(
    val lastBackup: String?,
    val entries: List<MoreEntry>,
)

object More {
    fun of(): MoreModel = MoreModel(lastBackup = null, entries = listOf(MoreEntry.NET_WORTH, MoreEntry.ALLOCATION, MoreEntry.REALIZED_GAINS))
}
