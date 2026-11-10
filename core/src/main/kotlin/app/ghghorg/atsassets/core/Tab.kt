package app.ghghorg.atsassets.core

/** The four bottom bar tabs (spec 6, navigation outline). [key] is the tab's name in spec 8's `tab_order` text. */
enum class Tab(
    val key: String,
) {
    MORE("more"),
    CASH("cash"),
    PORTFOLIOS("portfolios"),
    OVERALL("overall"),
}
