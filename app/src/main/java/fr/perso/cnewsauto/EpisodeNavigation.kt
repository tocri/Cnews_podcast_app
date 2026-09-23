package fr.perso.cnewsauto

/** Explicit browse folders: Android Auto does not implement callback pagination. */
object EpisodeNavigation {
    const val PAGE_SIZE = 8
    fun showId(parent: String) = parent.substringBefore('~')
    fun mode(parent: String) = parent.substringAfter('~', "")
    fun pageCount(count: Int) = (count + PAGE_SIZE - 1) / PAGE_SIZE
    fun indices(count: Int, mode: String): List<Int> = when (mode) {
        "", "recent" -> (0 until minOf(count, PAGE_SIZE)).toList()
        "oldest" -> (count - 1 downTo maxOf(0, count - PAGE_SIZE)).toList()
        else -> {
            val page = mode.removePrefix("page:").toIntOrNull()
            require(mode.startsWith("page:") && page != null && page >= 0 && page < pageCount(count))
            val start = page * PAGE_SIZE
            (start until minOf(count, start + PAGE_SIZE)).toList()
        }
    }
}
