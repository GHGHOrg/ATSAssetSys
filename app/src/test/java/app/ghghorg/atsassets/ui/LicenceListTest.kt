package app.ghghorg.atsassets.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

// 41 decision 1 (CR5, AC-70): About's list kept by hand, checked against the release build's runtime libraries, which
// the build's runtimeGroups task writes (app/build.gradle.kts). A Dependabot update that adds a library turns this red.
class LicenceListTest {
    private fun runtimeGroups(): Set<String> {
        val path = System.getProperty("atsassets.runtimeGroups")
        assertNotNull("The build passes the runtime groups' file (runtimeGroups task)", path)
        return File(path!!).readLines().filter { it.isNotBlank() }.toSet()
    }

    private val listed = Licences.ALL.flatMap { it.groups }

    @Test
    fun everyGroupTheReleaseShips_isListed_andNoOther() {
        val runtime = runtimeGroups()
        assertEquals("Shipped by the release, missing from About", emptySet<String>(), runtime - listed.toSet())
        assertEquals("Listed in About, not shipped by the release", emptySet<String>(), listed.toSet() - runtime)
    }

    @Test
    fun eachGroup_isInOneRowOnly() {
        assertEquals(listed.size, listed.toSet().size)
    }

    @Test
    fun theRows_areInOrderByName_ignoringCase() {
        val names = Licences.ALL.map { it.name }
        assertEquals(names.sortedWith(String.CASE_INSENSITIVE_ORDER), names)
    }
}
