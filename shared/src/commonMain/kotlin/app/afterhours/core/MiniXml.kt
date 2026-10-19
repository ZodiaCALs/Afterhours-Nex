package app.afterhours.core

/** A forgiving XML reader for feeds: it never throws, and it copes with the broken markup real feeds contain. */
class XmlNode(val name: String, val attrs: Map<String, String> = emptyMap()) {
    val children = mutableListOf<XmlNode>()
    private val buf = StringBuilder()
    val text: String get() = buf.toString()
    internal fun addText(s: String) { buf.append(s) }
    fun child(n: String): XmlNode? = children.firstOrNull { it.name.equals(n, ignoreCase = true) }
    fun all(n: String): List<XmlNode> = children.filter { it.name.equals(n, ignoreCase = true) }
    fun childText(n: String): String = child(n)?.text?.trim() ?: ""
    fun attr(n: String): String = attrs[n] ?: attrs.entries.firstOrNull { it.key.equals(n, ignoreCase = true) }?.value ?: ""
}

object MiniXml {
    private val ATTR = Regex("([^\\s=/]+)\\s*(?:=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?")

    fun parse(src: String): XmlNode {
        val root = XmlNode("#document")
        val stack = ArrayDeque<XmlNode>()
        stack.addLast(root)
        val n = src.length
        var i = 0
        while (i < n) {
            val lt = src.indexOf('<', i)
            if (lt < 0) { stack.last().addText(decodeEntities(src.substring(i))); break }
            if (lt > i) stack.last().addText(decodeEntities(src.substring(i, lt)))
            when {
                src.startsWith("<!--", lt) -> { val e = src.indexOf("-->", lt + 4); i = if (e < 0) n else e + 3 }
                src.startsWith("<![CDATA[", lt) -> {
                    val e = src.indexOf("]]>", lt + 9)
                    stack.last().addText(src.substring(lt + 9, if (e < 0) n else e))
                    i = if (e < 0) n else e + 3
                }
                src.startsWith("<?", lt) -> { val e = src.indexOf("?>", lt + 2); i = if (e < 0) n else e + 2 }
                src.startsWith("<!", lt) -> { val e = src.indexOf('>', lt + 2); i = if (e < 0) n else e + 1 }
                src.startsWith("</", lt) -> {
                    val e = src.indexOf('>', lt + 2)
                    val name = src.substring(lt + 2, if (e < 0) n else e).trim()
                    val at = stack.indexOfLast { it.name.equals(name, ignoreCase = true) }
                    if (at > 0) while (stack.size > at) stack.removeAt(stack.lastIndex)
                    i = if (e < 0) n else e + 1
                }
                else -> {
                    val e = tagEnd(src, lt + 1)
                    val raw = src.substring(lt + 1, e)
                    val selfClosing = raw.endsWith("/")
                    val body = if (selfClosing) raw.dropLast(1) else raw
                    val nameEnd = body.indexOfFirst { it.isWhitespace() }.let { if (it < 0) body.length else it }
                    val name = body.substring(0, nameEnd)
                    if (name.isEmpty() || !(name[0].isLetter() || name[0] == '_')) {
                        stack.last().addText("<")   // a stray "<" in text
                        i = lt + 1
                    } else {
                        val node = XmlNode(name, attrs(body.substring(nameEnd)))
                        stack.last().children.add(node)
                        if (!selfClosing) stack.addLast(node)
                        i = e + 1
                    }
                }
            }
        }
        return root
    }

    /** The '>' that ends a tag, skipping quoted attribute values. */
    private fun tagEnd(s: String, from: Int): Int {
        var quote = '\u0000'
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (quote != '\u0000') { if (c == quote) quote = '\u0000' }
            else if (c == '"' || c == '\'') quote = c
            else if (c == '>') return i
            i++
        }
        return s.length
    }

    private fun attrs(s: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in ATTR.findAll(s)) {
            val g = m.groups
            out[m.groupValues[1]] = decodeEntities(g[3]?.value ?: g[4]?.value ?: g[5]?.value ?: "")
        }
        return out
    }
}
