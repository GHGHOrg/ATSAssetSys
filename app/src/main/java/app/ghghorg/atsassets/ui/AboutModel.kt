package app.ghghorg.atsassets.ui

/*
 * About as the screen draws it (CR5, 41; spec section 6, More and Settings entry; wireframe 11; AC-70; CR2). Plain
 * Kotlin: the screen picks the app's name, the approved strings and each licence's text, and puts these values in.
 */

/** One library row: its name, its licence, and whether it is open to show the licence's text in place. */
data class AboutRowModel(
    val name: String,
    val licence: Licence,
    val open: Boolean,
)

/** The version as the app reports it (x.y.z, or x.y.z-dev.n in the development app) and the rows in [Licences] order. */
data class AboutModel(
    val version: String,
    val rows: List<AboutRowModel>,
)

object About {
    /** [open] holds the names of the rows opened on this visit; all start closed (wireframe 11 marker 3). */
    fun of(
        version: String,
        open: Set<String>,
    ): AboutModel = AboutModel(
        version = version,
        rows = Licences.ALL.map { AboutRowModel(it.name, it.licence, it.name in open) },
    )
}
