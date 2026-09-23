package fr.perso.cnewsauto

import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

data class Episode(val id: String, val title: String, val date: Long, val audio: String,
    val durationMs: Long = 0, val artwork: String? = null)

object RssParser {
    fun parse(input: InputStream, showId: String): List<Episode> {
        val result = mutableListOf<Episode>()
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val handler = object : DefaultHandler() {
            var inItem = false
            var depth = 0
            var title = ""; var guid = ""; var date = ""; var url = ""
            var duration = 0L
            var artwork: String? = null
            var channelArtwork: String? = null
            var inChannelImage = false
            val channelValue = StringBuilder()
            val value = StringBuilder()
            override fun startElement(uri: String, local: String, qName: String, attributes: Attributes) {
                require(local != "DOCTYPE")
                if (!inItem && local == "image") {
                    if (uri.contains("itunes")) channelArtwork = attributes.getValue("href")?.takeIf(::validAudio)
                    else if (uri.isEmpty()) inChannelImage = true
                }
                if (!inItem && local == "url" && inChannelImage) channelValue.setLength(0)
                if (local == "item") { inItem = true; depth = 0; title = ""; guid = ""; date = ""; url = ""; duration = 0; artwork = null }
                if (inItem) {
                    depth++
                    if (depth == 2) {
                        value.setLength(0)
                        if (local == "enclosure") url = attributes.getValue("url") ?: ""
                        if (local == "image" && uri.contains("itunes")) artwork = attributes.getValue("href")?.takeIf(::validAudio)
                    }
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inItem && depth == 2) value.append(ch, start, length)
                if (!inItem && inChannelImage) channelValue.append(ch, start, length)
            }
            override fun endElement(uri: String, local: String, qName: String) {
                if (!inItem) {
                    if (local == "url" && inChannelImage) channelArtwork = channelValue.toString().trim().takeIf(::validAudio) ?: channelArtwork
                    if (local == "image") inChannelImage = false
                    return
                }
                if (depth == 2 && local == "duration" && uri.contains("itunes")) duration = parseDuration(value.toString())
                if (depth == 2 && uri.isEmpty()) when(local) {
                    "title" -> title = value.toString().trim()
                    "guid" -> guid = value.toString().trim()
                    "pubDate" -> date = value.toString().trim()
                }
                if (local == "item" && depth == 1) {
                    if (title.isNotBlank() && validAudio(url)) {
                        val key = guid.ifBlank { url.substringBefore('?') }
                        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
                        val timestamp = runCatching { ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrDefault(0)
                        result += Episode("$showId/$hash", title, timestamp, url, duration, artwork)
                    }
                    inItem = false
                }
                depth--
            }
        }
        factory.newSAXParser().parse(input, handler)
        return result.distinctBy { it.id }.sortedByDescending { it.date }.take(60).map { it.copy(artwork = it.artwork ?: handler.channelArtwork) }
    }

    fun parseDuration(text: String): Long {
        val parts = text.trim().split(':')
        if (parts.size !in 1..3) return 0
        val numbers = parts.map { it.toDoubleOrNull() ?: return 0 }
        if (numbers.any { !it.isFinite() || it < 0 } || (parts.size > 1 && numbers.drop(1).any { it >= 60 })) return 0
        val seconds = numbers.fold(0.0) { sum, number -> sum * 60 + number }
        return if (seconds <= Long.MAX_VALUE / 1000.0) (seconds * 1000).toLong() else 0
    }

    fun validAudio(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
    }.getOrDefault(false)
}
