package fr.perso.cnewsauto

import org.junit.Assert.*
import org.junit.Test

class RssParserTest {
    @Test fun durationFormatsAndInvalidValues() {
        assertEquals(5_649_000L, RssParser.parseDuration("1:34:09"))
        assertEquals(5_649_000L, RssParser.parseDuration("5649"))
        assertEquals(89_000L, RssParser.parseDuration("01:29"))
        assertEquals(0L, RssParser.parseDuration(""))
        assertEquals(0L, RssParser.parseDuration("NaN"))
        assertEquals(0L, RssParser.parseDuration("-5"))
        assertEquals(0L, RssParser.parseDuration("1:78:22"))
    }
    @Test fun artworkUsesEpisodeThenChannelWithoutLeakingPreviousEpisodeImage() {
        val xml = """<rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
            <item><title>First</title><guid>a</guid><itunes:duration>01:02:03</itunes:duration><itunes:image href="https://example.com/episode.jpg"/><enclosure url="https://example.com/a.mp3"/></item>
            <item><title>Second</title><guid>b</guid><enclosure url="https://example.com/b.mp3"/></item>
            <itunes:image href="https://example.com/channel.jpg"/>
            </channel></rss>"""
        val items = parse(xml)
        assertEquals("https://example.com/episode.jpg", items[0].artwork)
        assertEquals("https://example.com/channel.jpg", items[1].artwork)
        assertEquals(3_723_000L, items[0].durationMs)
        assertEquals(0L, items[1].durationMs)
    }
    @Test fun standardRssImageFallbackAndUnsafeEpisodeImage() {
        val xml = """<rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel><image><url>https://example.com/cover.jpg</url></image>
            <item><title>Episode</title><itunes:image href="file:///private.jpg"/><enclosure url="https://example.com/a.mp3"/></item></channel></rss>"""
        assertEquals("https://example.com/cover.jpg", parse(xml).single().artwork)
    }
    private fun parse(xml: String) = RssParser.parse(xml.byteInputStream(), "test")
    @Test fun handlesNamespacesCdataDatesAndStableIds() {
        val xml = """<rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel><item><title><![CDATA[Test & débat]]></title><itunes:title>Incorrect</itunes:title><guid>abc</guid><pubDate>Wed, 16 Sep 2026 10:59:33 GMT</pubDate><enclosure url="https://sphinx.acast.com/media.mp3?a=1&amp;b=2"/></item></channel></rss>"""
        val item = parse(xml).single()
        assertEquals("Test & débat", item.title)
        assertTrue(item.date > 0)
        assertEquals("https://sphinx.acast.com/media.mp3?a=1&b=2", item.audio)
        assertEquals(item.id, parse(xml.replace("a=1", "a=9")).single().id)
    }
    @Test fun rejectsUnsafeAudioAndDeduplicates() {
        val item = "<item><title>A</title><guid>same</guid><enclosure url=\"https://sphinx.acast.com/a.mp3\"/></item>"
        assertEquals(1, parse("<rss><channel>$item$item</channel></rss>").size)
        assertFalse(RssParser.validAudio("file:///etc/test"))
        assertFalse(RssParser.validAudio("http://host/a.mp3"))
        assertTrue(parse("<rss><channel><item><title>No audio</title></item></channel></rss>").isEmpty())
    }
    @Test fun sortsNewestAndLimitsToSixty() {
        val items = (1..70).joinToString("") { "<item><title>Episode $it</title><guid>$it</guid><pubDate>Wed, 16 Sep 2026 10:${"%02d".format(it % 60)}:00 GMT</pubDate><enclosure url=\"https://sphinx.acast.com/$it.mp3\"/></item>" }
        val parsed = parse("<rss><channel>$items</channel></rss>")
        assertEquals(60, parsed.size)
        assertTrue(parsed.zipWithNext().all { it.first.date >= it.second.date })
    }
    @Test fun externalEntitiesAreNotExpanded() {
        val xml = """<!DOCTYPE rss [<!ENTITY external SYSTEM "file:///does-not-exist">]><rss><channel><item><title>&external;</title><enclosure url="https://sphinx.acast.com/a.mp3"/></item></channel></rss>"""
        assertTrue(parse(xml).isEmpty())
    }
}
