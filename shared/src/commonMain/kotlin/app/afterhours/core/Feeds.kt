package app.afterhours.core

object Feeds {
    val AUDIO_EXT = Regex("\\.(mp3|m4a|m4b|aac|ogg|oga|opus|wav|flac)(\\?|#|$)", RegexOption.IGNORE_CASE)

    fun looksLikeFeed(text: String): Boolean {
        val head = text.take(6000).lowercase()
        return "<rss" in head || "<feed" in head || ("<channel" in head && "<item" in text.lowercase())
    }

    /** Read an RSS or Atom feed. Episodes without audio are left out. */
    fun parse(text: String, sid: String, feedUrl: String): FeedInfo {
        val doc = MiniXml.parse(text)
        val rss = doc.children.firstOrNull { it.name.equals("rss", true) || it.name.endsWith("RDF", true) }
        val atom = doc.children.firstOrNull { it.name.equals("feed", true) }
        return when {
            rss != null -> rss(rss.child("channel") ?: rss, rss, sid, feedUrl)
            atom != null -> atom(atom, sid, feedUrl)
            else -> throw FetchException("That link doesn’t look like a podcast feed.")
        }
    }

    private fun rss(ch: XmlNode, root: XmlNode, sid: String, base: String): FeedInfo {
        val image = ch.child("itunes:image")?.attr("href")?.takeIf { it.isNotBlank() } ?: ch.child("image")?.childText("url") ?: ""
        val items = ch.all("item").ifEmpty { root.all("item") }
        val seen = HashSet<String>()
        val episodes = items.mapNotNull { item ->
            val enclosure = item.child("enclosure")
            var url = enclosure?.attr("url").orEmpty()
            if (url.isBlank()) url = item.all("media:content").firstOrNull { it.attr("type").startsWith("audio") || AUDIO_EXT.containsMatchIn(it.attr("url")) }?.attr("url").orEmpty()
            if (url.isBlank()) return@mapNotNull null
            url = resolveUrl(base, url.trim())
            if (!seen.add(url)) return@mapNotNull null
            val guid = item.childText("guid")
            val desc = item.child("content:encoded")?.text ?: item.child("description")?.text ?: item.childText("itunes:summary")
            Episode(
                id = episodeId(sid, guid.ifBlank { url }), sid = sid,
                title = clean(item.childText("title")).ifBlank { "Untitled episode" }, url = url,
                date = parseDate(item.childText("pubDate").ifBlank { item.childText("dc:date") }),
                duration = parseDuration(item.childText("itunes:duration")),
                image = item.child("itunes:image")?.attr("href").orEmpty(),
                desc = stripHtml(desc), guid = guid,
                transcript = item.all("podcast:transcript").firstOrNull()?.attr("url").orEmpty(),
            )
        }
        return FeedInfo(
            title = clean(ch.childText("title")), author = clean(ch.childText("itunes:author").ifBlank { ch.childText("author") }),
            image = image.trim(), description = stripHtml(ch.child("description")?.text ?: ch.childText("itunes:summary")),
            link = ch.childText("link"), episodes = episodes,
        )
    }

    private fun atom(feed: XmlNode, sid: String, base: String): FeedInfo {
        val episodes = feed.all("entry").mapNotNull { e ->
            val link = e.all("link").firstOrNull { it.attr("rel") == "enclosure" || it.attr("type").startsWith("audio") }?.attr("href") ?: return@mapNotNull null
            val url = resolveUrl(base, link)
            val id = e.childText("id")
            Episode(
                id = episodeId(sid, id.ifBlank { url }), sid = sid, title = clean(e.childText("title")).ifBlank { "Untitled episode" }, url = url,
                date = parseDate(e.childText("published").ifBlank { e.childText("updated") }),
                desc = stripHtml(e.child("content")?.text ?: e.childText("summary")), guid = id,
            )
        }
        val home = feed.all("link").firstOrNull { it.attr("rel").isEmpty() || it.attr("rel") == "alternate" }?.attr("href").orEmpty()
        return FeedInfo(clean(feed.childText("title")), clean(feed.child("author")?.childText("name")), feed.childText("logo").ifBlank { feed.childText("icon") },
            stripHtml(feed.childText("subtitle")), home, episodes)
    }
}
