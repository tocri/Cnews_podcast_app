package fr.perso.cnewsauto

import android.content.Context
import org.json.JSONObject

/** Stable RSS episode identifiers survive catalogue refreshes and app upgrades. */
class ListeningHistory(context: Context) {
    val preferences = context.getSharedPreferences("listening_history", Context.MODE_PRIVATE)
    fun isCompleted(id: String) = preferences.getBoolean(id, false)
    fun duration(id: String) = preferences.getLong("duration:$id", 0)
    fun position(id: String) = PlaybackProgress.resumePosition(preferences.getLong("position:$id", 0), duration(id), isCompleted(id))
    fun lastId(): String? = preferences.getString("last_id", null)
    fun saveProgress(id: String, position: Long, duration: Long) {
        if (!id.contains('/') || position < 0) return
        val editor = preferences.edit().putLong("position:$id", position).putString("last_id", id)
        if (duration > 0) editor.putLong("duration:$id", duration)
        editor.apply()
    }
    fun remember(episode: Episode) {
        // Only retain the RSS enclosure, never a resolved signed CDN URL.
        if (episode.audio.contains("Expires=", true) || episode.audio.contains("Signature=", true)) return
        val json = JSONObject().put("id", episode.id).put("title", episode.title).put("date", episode.date)
            .put("audio", episode.audio).put("duration", episode.durationMs).put("artwork", episode.artwork ?: "")
        preferences.edit().putString("episode:${episode.id}", json.toString()).apply()
    }
    fun episode(id: String): Episode? = runCatching {
        val json = JSONObject(preferences.getString("episode:$id", null) ?: return null)
        Episode(id, json.getString("title"), json.getLong("date"), json.getString("audio"),
            json.optLong("duration"), json.optString("artwork").takeIf { it.isNotBlank() })
    }.getOrNull()
    fun setCompleted(id: String, completed: Boolean) {
        if (id.contains('/') && isCompleted(id) != completed) {
            preferences.edit().putBoolean(id, completed).putLong("position:$id", 0).apply()
        }
    }
}
