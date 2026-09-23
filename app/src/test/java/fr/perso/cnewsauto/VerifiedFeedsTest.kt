package fr.perso.cnewsauto

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class VerifiedFeedsTest {
    @Test fun parsesVerifiedLiveFeedSnapshots() {
        val path = System.getProperty("rssFixtures")
        assumeTrue("Optional: pass -PrssFixtures=/path/to/downloaded/rss", path != null)
        val files = File(path!!).listFiles { file -> file.extension == "xml" }!!.toList()
        assertTrue(files.size >= 12)
        files.forEach { file ->
            val episodes = file.inputStream().use { RssParser.parse(it, file.nameWithoutExtension) }
            assertTrue("${file.name}: missing episodes", episodes.isNotEmpty())
            assertTrue("${file.name}: invalid date", episodes.first().date > 0)
            assertTrue(episodes.all { it.audio.startsWith("https://") && it.title.isNotBlank() })
            assertTrue("${file.name}: missing duration", episodes.first().durationMs > 0)
            assertTrue("${file.name}: missing artwork", episodes.first().artwork?.startsWith("https://") == true)
            assertEquals(episodes.size, episodes.distinctBy { it.id }.size)
        }
    }
}
