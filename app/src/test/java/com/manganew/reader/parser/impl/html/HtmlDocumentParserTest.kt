package com.manganew.reader.parser.impl.html

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlDocumentParserTest {
    private val parser = HtmlDocumentParser()
    private val fixture = requireNotNull(
        javaClass.getResource("/fixtures/html/catalog.html"),
    ).readText()

    @Test
    fun extractsFieldsFromFixedFixtureWithoutNetworkAccess() {
        val document = parseFixture()

        assertEquals(
            HtmlFieldResult.Value("First Comic"),
            document.extractFirst(".comic-item .title", HtmlExtraction.Text),
        )
        assertEquals(
            HtmlFieldListResult.Values(listOf("First Comic", "Second Comic", "Third Comic")),
            document.extractAll(".comic-item .title", HtmlExtraction.Text),
        )
    }

    @Test
    fun preservesDocumentOrderForMultipleMatches() {
        val document = parseFixture()

        val selection = document.select(".comic-item") as HtmlSelectionResult.Success
        assertEquals(3, selection.elements.size)
        assertEquals("article", selection.elements.first().tagName)
        assertEquals(
            listOf("/comic/one", "https://cdn.example.test/comic/two", "javascript:void(0)"),
            (document.extractAll(
                ".comic-item .title",
                HtmlExtraction.Attribute("href"),
            ) as HtmlFieldListResult.Values).values,
        )
    }

    @Test
    fun distinguishesMissingElementsAndEmptyAttributes() {
        val document = parseFixture()

        assertEquals(
            HtmlFieldResult.Missing,
            document.extractFirst(".does-not-exist", HtmlExtraction.Text),
        )
        assertEquals(
            HtmlFieldResult.Value(""),
            document.extractFirst(
                ".comic-item:nth-child(2) .cover",
                HtmlExtraction.Attribute("data-src"),
            ),
        )
        assertEquals(
            HtmlFieldResult.Missing,
            document.extractFirst(
                ".comic-item:nth-child(3) .cover",
                HtmlExtraction.Attribute("data-src"),
            ),
        )
        assertEquals(
            HtmlFieldResult.Value("demo"),
            document.extractFirst("input.keyword", HtmlExtraction.Value),
        )
        assertEquals(
            HtmlFieldResult.Value(""),
            document.extractFirst("input.empty-keyword", HtmlExtraction.Value),
        )
    }

    @Test
    fun resolvesRelativeUrlsAgainstExplicitBaseAndKeepsAbsoluteUrls() {
        val document = parseFixture()

        assertEquals(
            HtmlFieldResult.Value("https://example.test/catalog/images/cover-one.jpg"),
            document.extractFirst(
                ".comic-item:first-child .cover",
                HtmlExtraction.AbsoluteUrl("data-src"),
            ),
        )
        assertEquals(
            HtmlFieldResult.Value("https://cdn.example.test/comic/two"),
            document.extractFirst(
                ".comic-item:nth-child(2) .title",
                HtmlExtraction.AbsoluteUrl("href"),
            ),
        )
        assertEquals(
            HtmlFieldResult.Missing,
            document.extractFirst(
                ".comic-item:nth-child(2) .cover",
                HtmlExtraction.AbsoluteUrl("data-src"),
            ),
        )
    }

    @Test
    fun rejectsInvalidAndOversizedSelectors() {
        val document = parseFixture()

        assertEquals(
            HtmlSelectionResult.Failure(HtmlParserFailure.INVALID_SELECTOR),
            document.select("article["),
        )
        assertEquals(
            HtmlSelectionResult.Failure(HtmlParserFailure.SELECTOR_TOO_LONG),
            document.select(".".repeat(513)),
        )
    }

    @Test
    fun limitsMatchCountsAndDocumentSize() {
        val boundedParser = HtmlDocumentParser(maxDocumentBytes = 128, maxMatches = 2)
        val tooLarge = boundedParser.parse("<p>${"x".repeat(128)}</p>")
        assertEquals(
            HtmlParseResult.Failure(HtmlParserFailure.DOCUMENT_TOO_LARGE),
            tooLarge,
        )

        val document = (HtmlDocumentParser(maxMatches = 2).parse(
            "<i></i><i></i><i></i>",
        ) as HtmlParseResult.Success).document
        assertEquals(
            HtmlSelectionResult.Failure(HtmlParserFailure.TOO_MANY_MATCHES),
            document.select("i"),
        )
    }

    @Test
    fun rejectsInvalidBaseUrl() {
        assertEquals(
            HtmlParseResult.Failure(HtmlParserFailure.INVALID_BASE_URL),
            parser.parse("<a href='/demo'>Demo</a>", "file:///tmp/page.html"),
        )
        assertEquals(
            HtmlParseResult.Failure(HtmlParserFailure.INVALID_BASE_URL),
            parser.parse(
                "<a href='/demo'>Demo</a>",
                "https://user:secret@example.test/catalog/",
            ),
        )
    }

    @Test
    fun parsesScriptContentWithoutExecutingIt() {
        val document = parseFixture()

        assertEquals(HtmlFieldResult.Value(""), document.extractFirst("script", HtmlExtraction.Text))
        val scriptHtml = document.extractFirst("script", HtmlExtraction.Html)
        assertTrue(scriptHtml is HtmlFieldResult.Value)
        assertTrue(
            (scriptHtml as HtmlFieldResult.Value).value.contains(
                "http://127.0.0.1:9/must-not-be-requested",
            ),
        )
    }

    private fun parseFixture(): HtmlDocument =
        when (
            val result = parser.parse(
                html = fixture,
                baseUrl = "https://example.test/catalog/2026/",
            )
        ) {
            is HtmlParseResult.Success -> result.document
            is HtmlParseResult.Failure -> error("fixture parse failed: ${result.reason}")
        }
}
