package app.ghghorg.atsassets.ui

/*
 * The open-source libraries the app ships and their licences (CR5, 41; spec section 6, About; wireframe 11; AC-70).
 * Kept by hand (41 decision 1, your A): LicenceListTest compares every group here with the release build's runtime
 * libraries, so a library added or removed without this list turns the build red. Each licence's text is a raw resource
 * taken from the library's own files, never typed by hand.
 */

/** A licence the app's libraries use; the screen picks its approved name and its raw text for each. */
enum class Licence {
    APACHE_2_0,
    SQLCIPHER,
}

/** One row of About: a library project, the Maven groups it ships under, and its licence. */
data class LibraryLicence(
    val name: String,
    val groups: Set<String>,
    val licence: Licence,
)

object Licences {
    /** In the order About shows them: by name, ignoring case. */
    val ALL: List<LibraryLicence> =
        listOf(
            LibraryLicence("AndroidX Activity", setOf("androidx.activity"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Annotation", setOf("androidx.annotation"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX AppCompat", setOf("androidx.appcompat"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Arch Core", setOf("androidx.arch.core"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Autofill", setOf("androidx.autofill"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Biometric", setOf("androidx.biometric"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Collection", setOf("androidx.collection"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Concurrent", setOf("androidx.concurrent"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Core", setOf("androidx.core"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX CursorAdapter", setOf("androidx.cursoradapter"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX CustomView", setOf("androidx.customview"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX DrawerLayout", setOf("androidx.drawerlayout"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Emoji2", setOf("androidx.emoji2"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Fragment", setOf("androidx.fragment"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Graphics", setOf("androidx.graphics"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Interpolator", setOf("androidx.interpolator"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Lifecycle", setOf("androidx.lifecycle"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Loader", setOf("androidx.loader"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX NavigationEvent", setOf("androidx.navigationevent"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX ProfileInstaller", setOf("androidx.profileinstaller"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Room", setOf("androidx.room3"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX SavedState", setOf("androidx.savedstate"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX SQLite", setOf("androidx.sqlite"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Startup", setOf("androidx.startup"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Tracing", setOf("androidx.tracing"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX VectorDrawable", setOf("androidx.vectordrawable"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX VersionedParcelable", setOf("androidx.versionedparcelable"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX ViewPager", setOf("androidx.viewpager"), Licence.APACHE_2_0),
            LibraryLicence("AndroidX Window", setOf("androidx.window"), Licence.APACHE_2_0),
            LibraryLicence("Guava ListenableFuture", setOf("com.google.guava"), Licence.APACHE_2_0),
            LibraryLicence("JetBrains Java Annotations", setOf("org.jetbrains"), Licence.APACHE_2_0),
            LibraryLicence(
                "Jetpack Compose (Animation, Foundation, Material, Material 3, Runtime, UI)",
                setOf(
                    "androidx.compose.animation",
                    "androidx.compose.foundation",
                    "androidx.compose.material",
                    "androidx.compose.material3",
                    "androidx.compose.runtime",
                    "androidx.compose.ui",
                ),
                Licence.APACHE_2_0,
            ),
            LibraryLicence("JSpecify", setOf("org.jspecify"), Licence.APACHE_2_0),
            LibraryLicence("Kotlin standard library", setOf("org.jetbrains.kotlin"), Licence.APACHE_2_0),
            LibraryLicence("kotlinx.coroutines and kotlinx.serialization", setOf("org.jetbrains.kotlinx"), Licence.APACHE_2_0),
            LibraryLicence("SQLCipher for Android", setOf("net.zetetic"), Licence.SQLCIPHER),
        )
}
