package com.manganew.reader.parser.impl.json

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonDocumentParserTest {
    private val parser = JsonDocumentParser()
    private val fixture = requireNotNull(
        javaClass.getResource("/fixtures/json/catalog.json"),
    ).readText()

    @Test
    fun parsesObjectsAndArraysFromFixedFixture() {
        val document = parseFixture()

        val comic = document.select("$.comic") as JsonQueryResult.Success
        assertEquals(JsonValueKind.OBJECT, comic.values.single().kind)
        assertEquals(5, comic.values.single().size)

        val items = document.select("$.items") as JsonQueryResult.Success
        assertEquals(JsonValueKind.ARRAY, items.values.single().kind)
        assertEquals(3, items.values.single().size)
    }

    @Test
    fun readsDotBracketIndexAndWildcardPaths() {
        val document = parseFixture()

        assertEquals(
            JsonFieldResult.Value("Demo Comic"),
            document.extract("$.comic.title"),
        )
        assertEquals(
            JsonFieldResult.Value("Demo Comic"),
            document.extract("$[\"comic\"]['title']"),
        )
        assertEquals(
            JsonFieldResult.Value("/covers/demo.jpg"),
            document.extract("$.comic.cover-image"),
        )
        assertEquals(
            JsonFieldResult.Value("/covers/demo.jpg"),
            document.extract("$['comic']['cover-image']"),
        )
        assertEquals(
            JsonFieldResult.Value("Second"),
            document.extract("$.items[1].title"),
        )
        assertEquals(
            JsonFieldResult.Values(listOf("First", "Second")),
            document.extract("$.items[*].title"),
        )
        assertEquals(
            JsonFieldResult.Values(listOf("1", "2", "3")),
            document.extract("$.items[*].order"),
        )
    }

    @Test
    fun distinguishesMissingNullAndTypeMismatch() {
        val document = parseFixture()

        assertEquals(JsonFieldResult.Missing, document.extract("$.comic.missing"))
        assertEquals(JsonFieldResult.Missing, document.extract("$.comic.nullable"))
        assertEquals(
            JsonFieldResult.Failure(JsonParserFailure.TYPE_MISMATCH),
            document.extract("$.comic"),
        )
        assertEquals(
            JsonFieldResult.Failure(JsonParserFailure.TYPE_MISMATCH),
            document.extract("$.items[0].tags"),
        )
    }

    @Test
    fun readsPrimitiveRootAndBooleanValues() {
        val document = parseFixture()

        assertEquals(JsonFieldResult.Value("true"), document.extract("$.enabled"))
        val rootArray = (JsonDocumentParser().parse("[10,20]") as JsonParseResult.Success).document
        assertEquals(JsonFieldResult.Value("20"), rootArray.extract("$[1]"))
    }

    @Test
    fun rejectsUnsupportedOrInvalidJsonPathExpressions() {
        val document = parseFixture()

        listOf(
            "comic.title",
            "$..title",
            "$.items[-1]",
            "$.items[0:2]",
            "$.items[?(@.order > 1)]",
            "$.*",
            "$['bad\\'escape']",
        ).forEach { expression ->
            assertEquals(
                expression,
                JsonQueryResult.Failure(JsonParserFailure.INVALID_PATH),
                document.select(expression),
            )
        }
    }

    @Test
    fun enforcesPathAndMatchLimits() {
        val parser = JsonDocumentParser(maxPathLength = 32, maxPathDepth = 3, maxMatches = 2)
        val document = (parser.parse("""{"a":{"b":{"c":{"d":1}}},"items":[1,2,3]}""") as JsonParseResult.Success).document

        assertEquals(
            JsonQueryResult.Failure(JsonParserFailure.PATH_TOO_LONG),
            document.select("$.${"a".repeat(40)}"),
        )
        assertEquals(
            JsonQueryResult.Failure(JsonParserFailure.PATH_TOO_DEEP),
            document.select("$.a.b.c.d"),
        )
        assertEquals(
            JsonQueryResult.Failure(JsonParserFailure.TOO_MANY_MATCHES),
            document.select("$.items[*]"),
        )
    }

    @Test
    fun rejectsInvalidAndOversizedDocuments() {
        assertEquals(
            JsonParseResult.Failure(JsonParserFailure.INVALID_JSON),
            parser.parse("{\"broken\":"),
        )
        val bounded = JsonDocumentParser(maxDocumentBytes = 16)
        assertEquals(
            JsonParseResult.Failure(JsonParserFailure.DOCUMENT_TOO_LARGE),
            bounded.parse("""{"value":"this is too large"}"""),
        )
        assertEquals(
            JsonParseResult.Failure(JsonParserFailure.DOCUMENT_TOO_DEEP),
            JsonDocumentParser(maxDocumentDepth = 4).parse("[[[[[0]]]]]"),
        )
    }

    @Test
    fun preservesArrayOrderForMultipleMatches() {
        val document = parseFixture()

        assertEquals(
            JsonFieldResult.Values(listOf("1", "2", "3")),
            document.extract("$.items[*].order"),
        )
        val matrix = document.select("$.matrix[*]") as JsonQueryResult.Success
        assertEquals(2, matrix.values.size)
        assertEquals(JsonValueKind.ARRAY, matrix.values.first().kind)
    }

    private fun parseFixture(): JsonDocument =
        when (val result = parser.parse(fixture)) {
            is JsonParseResult.Success -> result.document
            is JsonParseResult.Failure -> error("fixture parse failed: ${result.reason}")
        }
}
