package fr.perso.cnewsauto

import android.content.ComponentName
import android.media.browse.MediaBrowser
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@androidx.media3.common.util.UnstableApi
class DeviceChecksTest {
    @Test fun carFoldersExposeBothEndsAndAllPeriods() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val connected = CountDownLatch(1)
        lateinit var browser: MediaBrowser
        instrumentation.runOnMainSync {
            browser = MediaBrowser(instrumentation.targetContext, ComponentName(instrumentation.targetContext, PlaybackService::class.java), object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() { connected.countDown() }
                override fun onConnectionFailed() { connected.countDown() }
            }, null)
            browser.connect()
        }
        fun children(id: String): List<MediaBrowser.MediaItem> {
            val loaded = CountDownLatch(1)
            var result: List<MediaBrowser.MediaItem>? = null
            instrumentation.runOnMainSync {
                browser.subscribe(id, object : MediaBrowser.SubscriptionCallback() {
                    override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) { result = children.toList(); loaded.countDown() }
                    override fun onError(parentId: String) { loaded.countDown() }
                })
            }
            assertTrue("Chargement de $id", loaded.await(45, TimeUnit.SECONDS))
            return requireNotNull(result) { "Catalogue indisponible : $id" }
        }
        try {
            assertTrue(connected.await(20, TimeUnit.SECONDS))
            assertTrue(browser.isConnected)
            val show = children(browser.root).first { it.isBrowsable }.mediaId!!
            val start = children(show)
            assertEquals(listOf("$show~recent", "$show~oldest", "$show~periods"), start.take(3).map { it.mediaId })
            assertTrue(start.drop(3).size <= 8)
            val recent = children("$show~recent")
            assertEquals(start.drop(3).map { it.mediaId }, recent.map { it.mediaId })
            val periods = children("$show~periods")
            val all = periods.flatMap { children(it.mediaId!!) }
            assertEquals(all.size, all.map { it.mediaId }.distinct().size)
            assertEquals(all.take(8).map { it.mediaId }, recent.map { it.mediaId })
            assertEquals(all.takeLast(8).reversed().map { it.mediaId }, children("$show~oldest").map { it.mediaId })
        } finally { instrumentation.runOnMainSync { browser.disconnect() } }
    }

    @Test fun historyKeepsPositionMetadataAndOldCompletionFlags() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val history = ListeningHistory(context)
        val id = "test/history-${System.nanoTime()}"
        val previousLast = history.lastId()
        try {
            val episode = Episode(id, "Test de persistance", 1, "https://sphinx.acast.com/example.mp3", 600_000, "https://example.com/cover.jpg")
            history.remember(episode)
            history.saveProgress(id, 245_000, 600_000)
            history.preferences.edit().commit() // Flush async writes before recreating the reader.
            val restored = ListeningHistory(context)
            assertEquals(245_000L, restored.position(id))
            assertEquals(600_000L, restored.duration(id))
            assertEquals(episode, restored.episode(id))
            restored.setCompleted(id, true)
            assertTrue(ListeningHistory(context).isCompleted(id))
            assertEquals(0L, restored.position(id))
            restored.setCompleted(id, false)
            assertFalse(restored.isCompleted(id))
            assertEquals(0L, restored.position(id))
        } finally {
            history.preferences.edit().remove(id).remove("position:$id").remove("duration:$id").remove("episode:$id").putString("last_id", previousLast).commit()
        }
    }

    @Test fun legacyBrowserCanBrowseWithoutOpeningPhoneActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val latch = CountDownLatch(1)
        var error: String? = null
        var count = 0
        lateinit var browser: MediaBrowser
        instrumentation.runOnMainSync {
            browser = MediaBrowser(context, ComponentName(context, PlaybackService::class.java), object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    browser.subscribe(browser.root, object : MediaBrowser.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
                            count = children.count { it.isBrowsable }
                            latch.countDown()
                        }
                        override fun onError(parentId: String) { error = "Erreur catalogue"; latch.countDown() }
                    })
                }
                override fun onConnectionFailed() { error = "Connexion refusée"; latch.countDown() }
            }, null)
            browser.connect()
        }
        try {
            assertTrue("Délai de connexion dépassé", latch.await(20, TimeUnit.SECONDS))
            assertNull(error)
            assertEquals(12, count)
        } finally { instrumentation.runOnMainSync { browser.disconnect() } }
    }
}
