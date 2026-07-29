package com.otaku.manayeou.data.remote

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test

class KmanaScraperTest {

    @Test
    fun resolveImgUrl_handlesProtocolRelativeAbsoluteAndRelative() {
        assertEquals("https://cdn.example.com/a.jpg", resolveImgUrl("//cdn.example.com/a.jpg"))
        assertEquals("http://cdn.example.com/a.jpg", resolveImgUrl("http://cdn.example.com/a.jpg"))
        assertEquals("https://kmana10.net/img/a.jpg", resolveImgUrl("img/a.jpg"))
        assertEquals("", resolveImgUrl(""))
    }

    @Test
    fun parseChapterListDoc_extractsDecimalChapterNumberAndExcludesFirstLink() {
        val html = """
            <div>
              <a class="first" href="/detail/123/0"><h5>처음부터</h5></a>
              <a href="/detail/123/205"><h5>205화</h5><span class="view_date_item">2024-01-01</span></a>
              <a href="/detail/123/206"><h5>205.5화</h5><span class="view_date_item">2024-01-02</span></a>
            </div>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://kmana10.net")

        val chapters = parseChapterListDoc(doc, "123")

        assertEquals(2, chapters.size)
        assertEquals("205", chapters[0].number)
        assertEquals("205.5", chapters[1].number) // ".5화" 만 잡혀 "5"로 잘리던 회귀 방지
    }

    @Test
    fun parseToonListJson_usesSrcWhenPresentAndSkipsEntriesWithoutId() {
        val body = """
            {"result":{"list":[
              {"id":"123","title":"어떤 만화","src":"//cdn.example.com/a.webp","author":"작가A"},
              {"id":"","title":"아이디 없음"}
            ]}}
        """.trimIndent()

        val series = parseToonListJson(body, "https://kmana10.net")

        assertEquals(1, series.size)
        assertEquals("123", series[0].id)
        assertEquals("https://cdn.example.com/a.webp", series[0].coverUrl)
        assertEquals("https://kmana10.net/episode/123/1/1", series[0].sourceUrl)
    }

    @Test
    fun parseToonListJson_fallsBackToIdBasedCoverWhenSrcMissing() {
        val body = """{"result":{"list":[{"id":"456","title":"검색결과"}]}}"""

        val series = parseToonListJson(body, "https://kmana10.net")

        assertEquals("https://smallimage.11toon8.com/data/toon_category/456.webp", series[0].coverUrl)
    }
}
