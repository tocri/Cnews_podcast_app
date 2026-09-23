package fr.perso.cnewsauto

object PlaybackProgress {
    fun resumePosition(position: Long, duration: Long, completed: Boolean): Long = when {
        completed || position <= 0 -> 0
        duration > 0 -> position.coerceAtMost((duration - 1).coerceAtLeast(0))
        else -> position
    }
    fun clock(ms: Long): String {
        val seconds = ms.coerceAtLeast(0) / 1000
        return if (seconds >= 3600) "%d:%02d:%02d".format(java.util.Locale.ROOT, seconds / 3600, seconds / 60 % 60, seconds % 60)
        else "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    }
}
