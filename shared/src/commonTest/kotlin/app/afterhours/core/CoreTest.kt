package app.afterhours.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CoreTest {
    private val rss = """<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:content="http://purl.org/rss/1.0/modules/content/">
<channel>
  <title>Shedunnit</title>
  <itunes:author>Caroline Crampton</itunes:author>
  <itunes:image href="https://example.com/art.jpg"/>
  <image><title>ignored</title><url>https://example.com/other.jpg</url></image>
  <description><![CDATA[<p>Golden age <b>detective</b> fiction &amp; more.</p>]]></description>
  <item>
    <title>The 9:15 train &amp; the locked room</title>
    <guid isPermaLink="false">ep-2</guid>
    <pubDate>Tue, 12 Mar 2024 10:30:00 GMT</pubDate>
    <itunes:duration>1:02:03</itunes:duration>
    <enclosure url="https://cdn.example.com/ep2.mp3" length="123" type="audio/mpeg"/>
    <content:encoded><![CDATA[<p>First line.</p><p>Second &rsquo;line&rsquo;.</p>]]></content:encoded>
  </item>
  <item>
    <title>No audio here</title>
  </item>
  <item>
    <title>Relative audio</title>
    <pubDate>2024-03-01T08:00:00+09:00</pubDate>
    <itunes:duration>3723</itunes:duration>
    <enclosure url="/audio/ep1.m4a" type="audio/mp4"/>
  </item>
</channel>
</rss>"""

    @Test fun readsAnRssFeed() {
        val info = Feeds.parse(rss, "p-shedunnit", "https://example.com/feed.xml")
        assertEquals("Shedunnit", info.title)
        assertEquals("Caroline Crampton", info.author)
        assertEquals("https://example.com/art.jpg", info.image)
        assertEquals("Golden age detective fiction & more.", info.description)
        assertEquals(2, info.episodes.size, "the item without audio is left out")
        val first = info.episodes[0]
        assertEquals("The 9:15 train & the locked room", first.title)
        assertEquals(epochMs(2024, 3, 12, 10, 30, 0), first.date)
        assertEquals(3723L, first.duration)
        assertEquals("First line.\nSecond ’line’.", first.desc)
        assertEquals(episodeId("p-shedunnit", "ep-2"), first.id)
        assertEquals("https://example.com/audio/ep1.m4a", info.episodes[1].url)
        assertEquals(epochMs(2024, 2, 29, 23, 0, 0), info.episodes[1].date)
    }

    @Test fun readsAnAtomFeed() {
        val atom = """<feed xmlns="http://www.w3.org/2005/Atom"><title>Atom show</title>
            <entry><id>urn:1</id><title>One</title><published>2023-01-02T03:04:05Z</published>
            <link rel="enclosure" href="https://x.example/1.mp3" type="audio/mpeg"/></entry></feed>"""
        val info = Feeds.parse(atom, "u-a", "https://x.example/feed")
        assertEquals("Atom show", info.title)
        assertEquals(1, info.episodes.size)
        assertEquals(epochMs(2023, 1, 2, 3, 4, 5), info.episodes[0].date)
    }

    @Test fun xmlCopesWithBrokenMarkup() {
        val doc = MiniXml.parse("<a><b x='1' y=\"2\">text<c/>more</b><unclosed><d>deep</a> trailing < stray")
        val a = assertNotNull(doc.child("a"))
        val b = assertNotNull(a.child("b"))
        assertEquals("1", b.attr("x"))
        assertEquals("textmore", b.text)
        assertTrue(a.children.any { it.name == "unclosed" })
    }

    @Test fun datesAndDurations() {
        assertEquals(epochMs(2024, 3, 12, 15, 30, 0), parseDate("Tue, 12 Mar 2024 10:30:00 EST"))
        assertEquals(epochMs(2024, 3, 12, 1, 30, 0), parseDate("Tue, 12 Mar 2024 10:30:00 +0900"))
        assertEquals(epochMs(1999, 12, 31), parseDate("31 Dec 99"))
        assertEquals(0L, parseDate("not a date"))
        assertEquals(951782400000L, epochMs(2000, 2, 29))
        assertEquals(3723L, parseDuration("01:02:03"))
        assertEquals(3723L, parseDuration("62:03"))
        assertEquals(0L, parseDuration("soon"))
        assertEquals("1:02:03", fmtClock(3723))
        assertEquals("4:05", fmtClock(245))
        assertEquals("1 h 2 min", fmtLength(3723))
        assertEquals("54 min", fmtLength(54 * 60))
    }

    @Test fun relativeDates() {
        Clock.utcOffsetMinutes = { 0 }
        val now = epochMs(2026, 10, 19, 12)
        assertEquals("Today", fmtDate(epochMs(2026, 10, 19, 1), now))
        assertEquals("Saturday", fmtDate(epochMs(2026, 10, 17, 9), now))
        assertEquals("3 Mar", fmtDate(epochMs(2026, 3, 3), now))
        assertEquals("3 Mar 2024", fmtDate(epochMs(2024, 3, 3), now))
    }

    @Test fun titlesThatBelongTogether() {
        assertTrue(related("Shedunnit", "Shedunnit: a mystery podcast"))
        assertTrue(related("The Vergecast", "Vergecast"))
        assertTrue(related("随机波动 StochasticVolatility", "随机波动"))
        assertFalse(related("Planet Money", "Song Exploder"))
        assertFalse(related("", "Anything"))
    }

    @Test fun resolvingLinks() {
        val base = "https://example.com/shows/mystery/index.html?x=1"
        assertEquals("https://example.com/feed.xml", resolveUrl(base, "/feed.xml"))
        assertEquals("https://example.com/shows/mystery/ep/1", resolveUrl(base, "ep/1"))
        assertEquals("https://example.com/shows/rss", resolveUrl(base, "../rss"))
        assertEquals("https://cdn.example.net/a.mp3", resolveUrl(base, "//cdn.example.net/a.mp3"))
        assertEquals("https://other.example/x", resolveUrl(base, "https://other.example/x"))
        assertEquals("example.com", hostOf("https://www.example.com/path"))
    }

    @Test fun findingFeedsAndEpisodesOnAWebsite() {
        val html = """<html><head><title>Mystery Hour</title>
            <link rel="alternate" type="application/rss+xml" href="/podcast.rss">
            <meta property="og:title" content="Mystery Hour &amp; Friends">
            <meta property="og:audio" content="https://cdn.example.com/latest.mp3"></head>
            <body><a href="https://podcasts.apple.com/gb/podcast/mystery-hour/id123456789">Apple</a>
            <a href="/episodes/12-the-locked-room">The locked room</a> <a href="/about">About</a>
            <a href="https://cdn.example.com/older.mp3">An older episode</a>
            <script>{"episodes":[{"title":"From the page data","audio":"https:\/\/cdn.example.com\/data.mp3"}]}</script></body></html>"""
        val base = "https://example.com/show/"
        val links = Crawler.anchors(html, base)
        assertEquals(listOf("https://example.com/podcast.rss"), Crawler.advertisedFeeds(html, base, links))
        assertEquals(123456789L, Crawler.appleId(html))
        assertEquals("Mystery Hour & Friends", Crawler.metas(html)["og:title"])
        val eps = Crawler.pageEpisodes(html, base, "Mystery Hour", "", "u-x", mutableSetOf())
        assertEquals(listOf("https://cdn.example.com/latest.mp3", "https://cdn.example.com/older.mp3", "https://cdn.example.com/data.mp3"), eps.map { it.url })
        assertEquals("An older episode", eps[1].title)
        assertEquals("From the page data", eps[2].title)
        assertEquals(listOf("https://example.com/episodes/12-the-locked-room"), Crawler.episodeLinks(links, base))
    }

    @Test fun theCatalogue() {
        assertEquals(30, Catalog.data.presets.size)
        assertEquals(7, Catalog.data.categories.size)
        assertTrue(Catalog.data.presets.all { p -> Catalog.category(p.cat) != null })
        assertNotNull(Catalog.pick("primer"))
    }

    @Test fun appleResults() {
        val json = """{"resultCount":2,"results":[{"collectionId":1,"collectionName":"Planet Money","artistName":"NPR","feedUrl":"https://feeds.npr.org/510289/podcast.xml","artworkUrl600":"https://a/600.jpg","primaryGenreName":"Business"},{"collectionId":2,"collectionName":"No feed"}]}"""
        val r = Apple.parse(json)
        assertEquals(1, r.size)
        assertEquals("Planet Money", r[0].title)
        assertEquals("https://a/600.jpg", r[0].artwork)
    }

    @Test fun stableIds() {
        assertEquals(stableId("e-", "abc"), stableId("e-", "abc"))
        assertTrue(stableId("e-", "abc") != stableId("e-", "abd"))
    }
}

class CaptionsTest {
    @Test fun webVtt() {
        val vtt = "WEBVTT\n\n00:00:01.000 --> 00:00:03.500\n<v Caroline>Welcome to the show.\n\n00:00:04.000 --> 00:00:06.000\nToday: the <i>locked</i> room &amp; more"
        val cues = Captions.parse(vtt)
        assertEquals(2, cues.size)
        assertEquals(Cue(1000, 3500, "Welcome to the show.", "Caroline"), cues[0])
        assertEquals("Today: the locked room & more", cues[1].text)
    }

    @Test fun subRip() {
        val srt = "1\r\n00:01:02,500 --> 00:01:04,000\r\nHOST: Good evening.\r\n\r\n2\r\n01:00:00,000 --> 01:00:01,000\r\nLate."
        val cues = Captions.parse(srt)
        assertEquals(Cue(62500, 64000, "Good evening.", "HOST"), cues[0])
        assertEquals(3600000L, cues[1].start)
    }

    @Test fun podcastJsonIsJoinedIntoLines() {
        val json = """{"version":"1.0.0","segments":[{"startTime":0.5,"endTime":0.9,"body":"Hello"},{"startTime":0.9,"endTime":1.3,"body":"and"},{"startTime":1.3,"endTime":2.0,"body":"welcome."},{"startTime":2.4,"endTime":3.0,"body":"Next","speaker":"Ann"}]}"""
        val cues = Captions.lines(Captions.parse(json, "transcript.json"))
        assertEquals(2, cues.size)
        assertEquals(Cue(500, 2000, "Hello and welcome."), cues[0])
        assertEquals("Ann", cues[1].speaker)
    }

    @Test fun findingTheCurrentLine() {
        val cues = listOf(Cue(0, 1000, "a"), Cue(2000, 3000, "b"), Cue(9000, 9500, "c"))
        assertEquals(0, Captions.at(cues, 500))
        assertEquals(1, Captions.at(cues, 3500))   // a short grace period after a line ends
        assertEquals(-1, Captions.at(cues, 6000))
        assertEquals(-1, Captions.at(emptyList(), 10))
    }

    @Test fun opmlRoundTrip() {
        val shows = listOf(Show("u-1", "Tom & Jerry \"Live\"", feedUrl = "https://a.example/feed?x=1&y=2", link = "https://a.example"))
        val back = Opml.read(Opml.export(shows))
        assertEquals(listOf("Tom & Jerry \"Live\"" to "https://a.example/feed?x=1&y=2"), back)
        val nested = """<opml><body><outline text="Folder"><outline text="One" xmlUrl="https://one.example/rss"/></outline><outline title="Two" xmlUrl="https://two.example/rss"/></body></opml>"""
        assertEquals(listOf("One" to "https://one.example/rss", "Two" to "https://two.example/rss"), Opml.read(nested))
    }
}
