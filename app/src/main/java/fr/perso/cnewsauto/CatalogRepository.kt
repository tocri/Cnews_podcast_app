package fr.perso.cnewsauto

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Show(val id: String, val title: String, val rss: String)

@androidx.media3.common.util.UnstableApi
class CatalogRepository(context: Context) {
    private val history = ListeningHistory(context)
    fun lastResumeItem(): MediaItem? = history.lastId()?.takeIf { history.position(it) > 0 }
        ?.let(history::episode)?.let(::media)
    fun rootItems(): List<MediaItem> = listOfNotNull(lastResumeItem()?.let { item ->
        item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setTitle("Reprendre · ${item.mediaMetadata.title}").build()).build()
    }) + shows.map { folder(it.id, it.title) }
    fun cachedCount(id: String): Int = cache[id]?.second?.size ?: 0
    suspend fun children(parent: String, phone: Boolean): List<MediaItem> {
        if (parent == "root") return rootItems()
        val show = EpisodeNavigation.showId(parent)
        val episodes = episodes(show)
        if (phone && parent == show) return episodes.map(::media)
        val mode = EpisodeNavigation.mode(parent)
        if (mode == "periods") return (0 until EpisodeNavigation.pageCount(episodes.size)).map { page ->
            val indices = EpisodeNavigation.indices(episodes.size, "page:$page")
            fun date(index: Int) = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(episodes[index].date))
            folder("$show~page:$page", "${date(indices.first())} → ${date(indices.last())}")
        }
        val shortcuts = if (mode.isEmpty()) listOf(
            folder("$show~recent", "↑ Début · Plus récents"),
            folder("$show~oldest", "↓ Fin · Plus anciens"),
            folder("$show~periods", "Toutes les périodes")) else emptyList()
        return shortcuts + EpisodeNavigation.indices(episodes.size, mode).map { media(episodes[it]) }
    }
    fun navigationFolder(id: String): MediaItem? {
        if (!id.contains('~') || shows.none { it.id == EpisodeNavigation.showId(id) }) return null
        val mode = EpisodeNavigation.mode(id)
        val title = when {
            mode == "recent" -> "Plus récents"
            mode == "oldest" -> "Plus anciens"
            mode == "periods" -> "Toutes les périodes"
            mode.startsWith("page:") && mode.removePrefix("page:").toIntOrNull() in 0..7 -> "Épisodes par période"
            else -> return null
        }
        return folder(id, title)
    }
    val shows: List<Show> = context.assets.open("shows.json").bufferedReader().use { reader ->
        val json = JSONArray(reader.readText())
        List(json.length()) { i -> json.getJSONObject(i).let { Show(it.getString("id"), it.getString("title"), it.getString("rss")) } }
    }
    private val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<Episode>>>()
    private val locks = shows.associate { it.id to Mutex() }
    suspend fun episodes(id: String, force: Boolean = false): List<Episode> = withContext(Dispatchers.IO) {
        val mutex = locks[id] ?: throw IllegalArgumentException("Émission inconnue")
        mutex.withLock {
            val show = shows.firstOrNull { it.id == id } ?: throw IllegalArgumentException("Émission inconnue")
            val old = cache[id]
            if (!force && old != null && System.currentTimeMillis() - old.first < 300_000) return@withLock old.second
            try {
                client.newCall(Request.Builder().url(show.rss).header("User-Agent", "CnewsAutoPersonal/0.1").build()).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("RSS indisponible (${response.code})")
                    val body = response.body ?: throw IOException("Flux vide")
                    val bytes = body.byteStream().use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) { val n = input.read(buffer); if (n < 0) break; if (output.size() + n > 12_000_000) throw IOException("Flux trop volumineux"); output.write(buffer, 0, n) }
                        output.toByteArray()
                    }
                    val items = RssParser.parse(bytes.inputStream(), id)
                    if (items.isEmpty()) throw IOException("Aucun épisode audio disponible")
                    cache[id] = System.currentTimeMillis() to items
                    items
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                // A stale list remains browsable; playback still resolves its RSS enclosure.
                if (old != null && !force) old.second else throw e
            }
        }
    }
    fun folder(id: String, title: String) = MediaItem.Builder().setMediaId(id).setMediaMetadata(
        MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build()).build()
    fun media(episode: Episode): MediaItem {
        val show = shows.first { episode.id.startsWith(it.id + "/") }
        val date = if (episode.date > 0) SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(episode.date)) else ""
        val completed = history.isCompleted(episode.id)
        val position = history.position(episode.id)
        val duration = history.duration(episode.id).takeIf { it > 0 } ?: episode.durationMs
        val subtitle = listOfNotNull(date.takeIf { it.isNotEmpty() },
            if (duration > 0) PlaybackProgress.clock(duration) else "Durée indisponible",
            if (completed) "Écouté" else if (position > 0) "Reprendre à ${PlaybackProgress.clock(position)}" else null).joinToString(" · ")
        val extras = android.os.Bundle().apply {
            putLong("resume_position_ms", position)
            putInt(androidx.media3.session.MediaConstants.EXTRAS_KEY_COMPLETION_STATUS,
                if (completed) androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
                else if (position > 0) androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
                else androidx.media3.session.MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED)
            if (duration > 0) putDouble(androidx.media3.session.MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE,
                if (completed) 1.0 else (position.toDouble() / duration).coerceIn(0.0, 1.0))
        }
        return MediaItem.Builder().setMediaId(episode.id).setUri(episode.audio).setMediaMetadata(
            MediaMetadata.Builder().setTitle(episode.title).setSubtitle(subtitle).setArtist(if (completed) "🟢 Lu" else show.title).setExtras(extras)
                .setDurationMs(duration.takeIf { it > 0 }).setArtworkUri(episode.artwork?.let(android.net.Uri::parse))
                .setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE).build()).build()
    }
    suspend fun resolve(id: String): MediaItem {
        val show = id.substringBefore('/')
        require(shows.any { it.id == show }) { "Émission inconnue" }
        val episode = try {
            episodes(show).firstOrNull { it.id == id } ?: history.episode(id)
                ?: episodes(show, true).firstOrNull { it.id == id }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: IOException) { history.episode(id) ?: throw e }
        ?: throw IOException("Épisode absent du flux récent")
        history.remember(episode)
        // Pass the enclosure to ExoPlayer, never persist a redirected signed URL.
        return media(episode)
    }
    suspend fun search(query: String): List<MediaItem> {
        fun normalized(text: String) = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.FRANCE).replace(Regex("[^a-z0-9]+"), " ").trim()
        val words = normalized(query).split(' ').filter { it.isNotBlank() }
        val matches = if (words.isEmpty()) shows.take(1) else shows.filter { show -> words.all { normalized(show.title).contains(it) } }.take(3)
        return matches.flatMap { episodes(it.id).take(1).map(::media) }
    }
}
