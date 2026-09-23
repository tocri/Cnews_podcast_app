package fr.perso.cnewsauto

import org.junit.Assert.*
import org.junit.Test

class EpisodeNavigationTest {
    @Test fun newestAndOldestAreImmediatelyAccessible() {
        assertEquals((0..7).toList(), EpisodeNavigation.indices(60, "recent"))
        assertEquals((59 downTo 52).toList(), EpisodeNavigation.indices(60, "oldest"))
        assertEquals(emptyList<Int>(), EpisodeNavigation.indices(0, "oldest"))
        assertEquals(listOf(2, 1, 0), EpisodeNavigation.indices(3, "oldest"))
    }
    @Test fun periodsCoverEveryEpisodeExactlyOnceIncludingShortLastPage() {
        for (count in listOf(0, 1, 8, 9, 59, 60)) {
            val pages = (0 until EpisodeNavigation.pageCount(count)).map { EpisodeNavigation.indices(count, "page:$it") }
            assertEquals((0 until count).toList(), pages.flatten())
            assertTrue(pages.all { it.size <= 8 })
        }
    }
    @Test fun folderIdsRetainShowIdentity() {
        assertEquals("frontieres", EpisodeNavigation.showId("frontieres~page:3"))
        assertEquals("page:3", EpisodeNavigation.mode("frontieres~page:3"))
    }
    @Test fun invalidPagesAreRejected() {
        for (mode in listOf("page:-1", "page:8", "page:2147483647", "invalid")) {
            assertThrows(IllegalArgumentException::class.java) { EpisodeNavigation.indices(60, mode) }
        }
    }
}
