package fr.perso.cnewsauto

import org.junit.Assert.*
import org.junit.Test

class PlaybackProgressTest {
    @Test fun resumeSurvivesUnknownDurationButDoesNotRestartCompletedEpisodeAtEnd() {
        assertEquals(420_000L, PlaybackProgress.resumePosition(420_000, 0, false))
        assertEquals(420_000L, PlaybackProgress.resumePosition(420_000, 3_600_000, false))
        assertEquals(0L, PlaybackProgress.resumePosition(420_000, 3_600_000, true))
        assertEquals(0L, PlaybackProgress.resumePosition(-1, 3_600_000, false))
    }
    @Test fun changedDurationClampsTheSavedPositionInsideTheNewTimeline() {
        assertEquals(599_999L, PlaybackProgress.resumePosition(650_000, 600_000, false))
    }
    @Test fun readableTimesIncludeHours() {
        assertEquals("0:00", PlaybackProgress.clock(0))
        assertEquals("7:04", PlaybackProgress.clock(424_000))
        assertEquals("1:34:09", PlaybackProgress.clock(5_649_000))
    }
}
