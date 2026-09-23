package fr.perso.cnewsauto

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.future

@UnstableApi
class PlaybackService : MediaLibraryService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: CatalogRepository
    private lateinit var session: MediaLibrarySession
    private lateinit var history: ListeningHistory
    private var historyRefresh: Job? = null
    private lateinit var carConnection: androidx.car.app.connection.CarConnection
    private var wasProjected = false
    private val carObserver = androidx.lifecycle.Observer<Int> { type ->
        val projected = type == androidx.car.app.connection.CarConnection.CONNECTION_TYPE_PROJECTION
        if (wasProjected && !projected && ::session.isInitialized) {
            session.player.pause()
            saveCurrentProgress()
        }
        wasProjected = projected
    }
    private val historyListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && ::session.isInitialized && !key.startsWith("episode:") && key != "last_id") {
            val id = key.substringAfter(':', key)
            val show = id.substringBefore('/')
            historyRefresh?.cancel()
            historyRefresh = scope.launch {
                delay(250)
                // Progress is saved every five seconds. Do not reset the car's browse
                // screen on each save; it reads fresh progress on its next visit.
                for ((controller, parents) in subscribed.toMap()) {
                    if (!isPhone(controller)) continue
                    for (parent in parents.toList()) {
                        if (parent == "root" || EpisodeNavigation.showId(parent) == show) {
                            runCatching { repository.children(parent, true) }.onSuccess {
                                session.notifyChildrenChanged(controller, parent, it.size, null)
                            }
                        }
                    }
                }
            }
        }
    }
    private fun isPhone(controller: MediaSession.ControllerInfo) = controller.packageName == packageName && controller.connectionHints.getBoolean("phone_ui", false)
    private val subscribed = mutableMapOf<MediaSession.ControllerInfo, MutableSet<String>>()
    override fun onCreate() {
        super.onCreate()
        repository = CatalogRepository(this)
        history = ListeningHistory(this)
        val player = ExoPlayer.Builder(this).setSeekBackIncrementMs(15_000).setSeekForwardIncrementMs(30_000).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_NETWORK)
        }
        session = MediaLibrarySession.Builder(this, player, Callback())
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
        carConnection = androidx.car.app.connection.CarConnection(this)
        carConnection.type.observeForever(carObserver)
        history.preferences.registerOnSharedPreferenceChangeListener(historyListener)
        player.addListener(object : Player.Listener {
            private var previousId: String? = null
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) previousId?.let { history.setCompleted(it, true) }
                previousId = mediaItem?.mediaId
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) player.currentMediaItem?.mediaId?.let { history.setCompleted(it, true) }
                if (playbackState == Player.STATE_READY) saveCurrentProgress()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { saveCurrentProgress() }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (oldPosition.mediaItem?.mediaId != newPosition.mediaItem?.mediaId) {
                    oldPosition.mediaItem?.let { item ->
                        val duration = history.duration(item.mediaId).takeIf { it > 0 } ?: item.mediaMetadata.durationMs ?: 0
                        if (oldPosition.positionMs > 0) history.saveProgress(item.mediaId, oldPosition.positionMs, duration)
                    }
                } else if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    newPosition.mediaItem?.let { item ->
                        val duration = player.duration.takeIf { it > 0 } ?: history.duration(item.mediaId)
                        history.saveProgress(item.mediaId, newPosition.positionMs, duration)
                    }
                } else saveCurrentProgress()
            }
        })
        scope.launch { while (isActive) { delay(5_000); saveCurrentProgress() } }
        scope.launch {
            while (isActive) {
                delay(300_000)
                for (id in subscribed.values.flatten().filter { it != "root" }.map(EpisodeNavigation::showId).distinct()) {
                    runCatching { repository.episodes(id, true) }.onSuccess {
                        for ((controller, parents) in subscribed.toMap()) {
                            // The car reads refreshed data on the next folder visit.
                            if (!isPhone(controller)) continue
                            for (parent in parents.toList().filter { EpisodeNavigation.showId(it) == id }) {
                                val count = repository.children(parent, true).size
                                session.notifyChildrenChanged(controller, parent, count, null)
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session
    private fun saveCurrentProgress() {
        if (!::session.isInitialized) return
        val player = session.player
        val item = player.currentMediaItem ?: return
        if (player.playbackState == Player.STATE_READY) {
            history.saveProgress(item.mediaId, player.currentPosition, player.duration)
        }
    }
    override fun onTaskRemoved(rootIntent: Intent?) { saveCurrentProgress(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() { saveCurrentProgress(); carConnection.type.removeObserver(carObserver); history.preferences.unregisterOnSharedPreferenceChangeListener(historyListener); scope.cancel(); session.player.release(); session.release(); super.onDestroy() }

    private inner class Callback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(s: MediaLibrarySession, c: MediaSession.ControllerInfo, p: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(repository.folder("root", "Émissions CNEWS"), p))

        override fun onGetChildren(s: MediaLibrarySession, c: MediaSession.ControllerInfo, parent: String, page: Int, pageSize: Int, p: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            try {
                if (page < 0 || pageSize < 1) return@future LibraryResult.ofError<ImmutableList<MediaItem>>(SessionError.ERROR_BAD_VALUE)
                val items = repository.children(parent, isPhone(c))
                val start = (page.toLong() * pageSize).coerceAtMost(items.size.toLong()).toInt()
                LibraryResult.ofItemList(items.subList(start, (start.toLong() + pageSize).coerceAtMost(items.size.toLong()).toInt()), p)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { LibraryResult.ofError<ImmutableList<MediaItem>>(SessionError.ERROR_IO) }
        }
        override fun onGetItem(s: MediaLibrarySession, c: MediaSession.ControllerInfo, id: String): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            try {
                val item = if (id == "root") repository.folder(id, "Émissions CNEWS")
                    else repository.navigationFolder(id) ?: repository.shows.firstOrNull { it.id == id }?.let { repository.folder(it.id, it.title) } ?: repository.resolve(id)
                LibraryResult.ofItem(item, null)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { LibraryResult.ofError<MediaItem>(SessionError.ERROR_BAD_VALUE) }
        }
        override fun onAddMediaItems(s: MediaSession, c: MediaSession.ControllerInfo, items: List<MediaItem>): ListenableFuture<List<MediaItem>> = scope.future {
            items.flatMap { item ->
                val query = item.requestMetadata.searchQuery
                if (query != null) repository.search(query).also { require(it.isNotEmpty()) { "Émission introuvable" } }
                else listOf(repository.resolve(item.mediaId))
            }
        }
        override fun onSetMediaItems(s: MediaSession, c: MediaSession.ControllerInfo, items: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            saveCurrentProgress()
            val resolved = items.flatMap { item ->
                item.requestMetadata.searchQuery?.let { repository.search(it) }
                    ?: listOf(repository.resolve(item.mediaId))
            }
            require(resolved.isNotEmpty()) { "Épisode introuvable" }
            val index = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(resolved.indices)
            val id = resolved[index].mediaId
            val position = if (startPositionMs == C.TIME_UNSET) history.position(id) else startPositionMs.coerceAtLeast(0)
            MediaSession.MediaItemsWithStartPosition(resolved, index, position)
        }
        override fun onPlaybackResumption(s: MediaSession, c: MediaSession.ControllerInfo): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val id = history.lastId() ?: throw IllegalStateException("Aucune écoute à reprendre")
            val item = repository.resolve(id)
            MediaSession.MediaItemsWithStartPosition(listOf(item), 0, history.position(id))
        }
        override fun onSearch(s: MediaLibrarySession, c: MediaSession.ControllerInfo, query: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> = scope.future {
            try {
                val items = repository.search(query)
                session.notifySearchResultChanged(c, query, items.size, params)
                LibraryResult.ofVoid()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { LibraryResult.ofError<Void>(SessionError.ERROR_IO) }
        }
        override fun onGetSearchResult(s: MediaLibrarySession, c: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            try {
                if (page < 0 || pageSize < 1) return@future LibraryResult.ofError<ImmutableList<MediaItem>>(SessionError.ERROR_BAD_VALUE)
                val items = repository.search(query)
                val start = (page.toLong() * pageSize).coerceAtMost(items.size.toLong()).toInt()
                LibraryResult.ofItemList(items.subList(start, (start.toLong() + pageSize).coerceAtMost(items.size.toLong()).toInt()), params)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { LibraryResult.ofError<ImmutableList<MediaItem>>(SessionError.ERROR_IO) }
        }
        override fun onSubscribe(s: MediaLibrarySession, c: MediaSession.ControllerInfo, parentId: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> = scope.future {
            try {
                val items = repository.children(parentId, isPhone(c))
                subscribed.getOrPut(c) { mutableSetOf() }.add(parentId)
                session.notifyChildrenChanged(c, parentId, items.size, params)
                LibraryResult.ofVoid()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { LibraryResult.ofError<Void>(SessionError.ERROR_IO) }
        }
        override fun onUnsubscribe(s: MediaLibrarySession, c: MediaSession.ControllerInfo, parentId: String): ListenableFuture<LibraryResult<Void>> {
            subscribed[c]?.remove(parentId)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }
        override fun onDisconnected(s: MediaSession, c: MediaSession.ControllerInfo) { subscribed.remove(c) }
    }
}
