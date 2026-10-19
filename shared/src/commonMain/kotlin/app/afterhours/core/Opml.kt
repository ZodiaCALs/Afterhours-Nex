package app.afterhours.core

/** OPML, the file podcast apps use to move a list of shows between them. */
object Opml {
    private fun esc(s: String) = s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")

    fun export(shows: List<Show>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<opml version=\"2.0\">\n  <head><title>AfterhoursNex</title></head>\n  <body>\n")
        for (s in shows) if (s.feedUrl.isNotBlank()) {
            append("    <outline type=\"rss\" text=\"").append(esc(s.title)).append("\" title=\"").append(esc(s.title))
                .append("\" xmlUrl=\"").append(esc(s.feedUrl)).append("\" htmlUrl=\"").append(esc(s.link)).append("\"/>\n")
        }
        append("  </body>\n</opml>\n")
    }

    /** The (title, feed address) pairs in an OPML file, in order, including nested folders. */
    fun read(text: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        fun walk(n: XmlNode) {
            for (c in n.children) {
                if (c.name.equals("outline", true)) {
                    val url = c.attr("xmlUrl").trim()
                    if (url.startsWith("http")) out.add(c.attr("title").ifBlank { c.attr("text") }.ifBlank { url } to url)
                }
                walk(c)
            }
        }
        walk(MiniXml.parse(text))
        return out.distinctBy { it.second }
    }
}
