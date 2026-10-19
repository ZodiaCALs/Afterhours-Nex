package app.afterhours.core

import kotlin.test.*

class I18nTest {
    private fun <T> inLanguage(lang: String, block: () -> T): T { I18n.lang = lang; try { return block() } finally { I18n.lang = "en" } }

    @Test fun choosingTheLanguage() {
        assertEquals("zh-Hans", I18n.pick("system", "zh-CN")); assertEquals("ja", I18n.pick("system", "ja-JP"))
        assertEquals("en", I18n.pick("system", "fr-FR")); assertEquals("ja", I18n.pick("ja", "en-GB"))
    }
    @Test fun englishPassesThrough() = assertEquals("Listen Now", tr("Listen Now"))
    @Test fun phrasesAndSentences() = inLanguage("zh-Hans") {
        assertEquals("此生还能再见你几次？", tr("How many times do I have left to see you again in my life?"))
        assertEquals("已将“Shedunnit”添加到资料库。", tr("Added Shedunnit to your library."))
        assertEquals(" 返回", tr(" Back"))                                   // surrounding spaces are kept
        assertEquals("Midnight Mysteries", tr("Midnight Mysteries"))       // content is never translated
    }
    @Test fun nestedSentences() = inLanguage("ja") {
        assertEquals("エピソードを読み込めませんでした。サイトの応答に時間がかかりすぎました。", tr("Couldn’t load episodes. The site took too long to answer."))
    }
    @Test fun durationsAndDates() {
        inLanguage("zh-Hans") { assertEquals("1 小时 2 分钟", fmtLength(3720)); assertEquals("剩余 23 分钟", tr(fmtLength(1380) + " left")) }
        inLanguage("ja") { assertEquals("54分", fmtLength(3240)) }
        assertEquals("1 h 2 min", fmtLength(3720))
    }
}
