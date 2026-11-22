package com.manganew.reader.parser.impl.json

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Failures returned by local JSON parsing and minimal JSONPath evaluation. */
internal enum class JsonParserFailure {
    DOCUMENT_TOO_LARGE,
    DOCUMENT_TOO_DEEP,
    INVALID_JSON,
    PATH_TOO_LONG,
    PATH_TOO_DEEP,
    INVALID_PATH,
    TOO_MANY_MATCHES,
    TYPE_MISMATCH,
}

/** Result of parsing one JSON text response. */
internal sealed interface JsonParseResult {
    data class Success(val document: JsonDocument) : JsonParseResult

    data class Failure(val reason: JsonParserFailure) : JsonParseResult
}

/** Result of evaluating one supported JSONPath expression. */
internal sealed interface JsonQueryResult {
    data class Success(val values: List<JsonValue>) : JsonQueryResult

    data class Failure(val reason: JsonParserFailure) : JsonQueryResult
}

/** Scalar or multi-value extraction from JSONPath results. */
internal sealed interface JsonFieldResult {
    data class Value(val value: String) : JsonFieldResult

    data class Values(val values: List<String>) : JsonFieldResult

    data object Missing : JsonFieldResult

    data class Failure(val reason: JsonParserFailure) : JsonFieldResult
}

/** JSON value type exposed without leaking kotlinx.serialization types. */
internal enum class JsonValueKind {
    OBJECT,
    ARRAY,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
}

/** One selected JSON value. */
internal class JsonValue internal constructor(
    private val element: JsonElement,
) {
    val kind: JsonValueKind = when (element) {
        is JsonObject -> JsonValueKind.OBJECT
        is JsonArray -> JsonValueKind.ARRAY
        JsonNull -> JsonValueKind.NULL
        is JsonPrimitive -> when {
            element.isString -> JsonValueKind.STRING
            element.content == "true" || element.content == "false" -> JsonValueKind.BOOLEAN
            else -> JsonValueKind.NUMBER
        }
    }

    val size: Int?
        get() = when (element) {
            is JsonObject -> element.size
            is JsonArray -> element.size
            else -> null
        }

    fun scalarText(): String? =
        when (kind) {
            JsonValueKind.STRING,
            JsonValueKind.NUMBER,
            JsonValueKind.BOOLEAN,
            -> (element as JsonPrimitive).content
            JsonValueKind.OBJECT,
            JsonValueKind.ARRAY,
            JsonValueKind.NULL,
            -> null
        }
}

/**
 * Parses untrusted JSON text and evaluates a deliberately small JSONPath
 * subset without network or file access.
 */
internal class JsonDocumentParser(
    private val maxDocumentBytes: Int = 4 * 1024 * 1024,
    private val maxDocumentDepth: Int = 64,
    private val maxPathLength: Int = 512,
    private val maxPathDepth: Int = 32,
    private val maxMatches: Int = 1_000,
) {
    private val json = Json {
        isLenient = false
        allowSpecialFloatingPointValues = false
    }

    init {
        require(maxDocumentBytes > 0) { "maxDocumentBytes must be positive" }
        require(maxDocumentDepth > 0) { "maxDocumentDepth must be positive" }
        require(maxPathLength > 0) { "maxPathLength must be positive" }
        require(maxPathDepth > 0) { "maxPathDepth must be positive" }
        require(maxMatches > 0) { "maxMatches must be positive" }
    }

    fun parse(source: String): JsonParseResult {
        if (source.encodeToByteArray().size > maxDocumentBytes) {
            return JsonParseResult.Failure(JsonParserFailure.DOCUMENT_TOO_LARGE)
        }
        if (exceedsNestingDepth(source, maxDocumentDepth)) {
            return JsonParseResult.Failure(JsonParserFailure.DOCUMENT_TOO_DEEP)
        }
        val root = try {
            json.parseToJsonElement(source)
        } catch (_: SerializationException) {
            return JsonParseResult.Failure(JsonParserFailure.INVALID_JSON)
        } catch (_: IllegalArgumentException) {
            return JsonParseResult.Failure(JsonParserFailure.INVALID_JSON)
        }
        return JsonParseResult.Success(
            JsonDocument(
                root = root,
                maxPathLength = maxPathLength,
                maxPathDepth = maxPathDepth,
                maxMatches = maxMatches,
            ),
        )
    }
}

/**
 * Parsed JSON document with this supported JSONPath grammar:
 *
 * - `$`
 * - `$.field`, including `_`, digits after the first character, and `-`
 * - `$['field-with-dash']` or `$["field"]` without escapes
 * - `$.items[0]`
 * - `$.items[*].field`
 *
 * Filters, recursive descent, slices, unions, negative indexes, functions,
 * and arbitrary expressions are intentionally not supported.
 */
internal class JsonDocument internal constructor(
    private val root: JsonElement,
    private val maxPathLength: Int,
    private val maxPathDepth: Int,
    private val maxMatches: Int,
) {
    fun select(expression: String): JsonQueryResult {
        val segments = when (val path = parseJsonPath(expression, maxPathLength, maxPathDepth)) {
            is JsonPathParseResult.Success -> path.segments
            JsonPathParseResult.TooLong -> {
                return JsonQueryResult.Failure(JsonParserFailure.PATH_TOO_LONG)
            }
            JsonPathParseResult.TooDeep -> {
                return JsonQueryResult.Failure(JsonParserFailure.PATH_TOO_DEEP)
            }
            JsonPathParseResult.Invalid -> {
                return JsonQueryResult.Failure(JsonParserFailure.INVALID_PATH)
            }
        }

        var current = listOf(root)
        segments.forEach { segment ->
            current = applyPathSegment(current, segment, maxMatches)
                ?: return JsonQueryResult.Failure(JsonParserFailure.TOO_MANY_MATCHES)
        }
        return JsonQueryResult.Success(current.map(::JsonValue))
    }

    fun extract(expression: String): JsonFieldResult =
        when (val query = select(expression)) {
            is JsonQueryResult.Failure -> JsonFieldResult.Failure(query.reason)
            is JsonQueryResult.Success -> {
                val values = mutableListOf<String>()
                query.values.forEach { value ->
                    when (value.kind) {
                        JsonValueKind.OBJECT,
                        JsonValueKind.ARRAY,
                        -> return JsonFieldResult.Failure(JsonParserFailure.TYPE_MISMATCH)
                        JsonValueKind.NULL -> Unit
                        else -> value.scalarText()?.let(values::add)
                    }
                }
                when (values.size) {
                    0 -> JsonFieldResult.Missing
                    1 -> JsonFieldResult.Value(values.single())
                    else -> JsonFieldResult.Values(values)
                }
            }
        }
}

private fun applyPathSegment(
    values: List<JsonElement>,
    segment: JsonPathSegment,
    maxMatches: Int,
): List<JsonElement>? {
    val matches = ArrayList<JsonElement>()
    values.forEach { value ->
        when (segment) {
            is JsonPathSegment.Field -> {
                (value as? JsonObject)?.get(segment.name)?.let(matches::add)
            }
            is JsonPathSegment.Index -> {
                (value as? JsonArray)?.getOrNull(segment.index)?.let(matches::add)
            }
            JsonPathSegment.Wildcard -> {
                val array = value as? JsonArray
                if (array != null) {
                    array.forEach { item ->
                        matches += item
                        if (matches.size > maxMatches) {
                            return null
                        }
                    }
                }
            }
        }
        if (matches.size > maxMatches) {
            return null
        }
    }
    return matches
}

private sealed interface JsonPathSegment {
    data class Field(val name: String) : JsonPathSegment

    data class Index(val index: Int) : JsonPathSegment

    data object Wildcard : JsonPathSegment
}

private sealed interface JsonPathParseResult {
    data class Success(val segments: List<JsonPathSegment>) : JsonPathParseResult

    data object TooLong : JsonPathParseResult

    data object TooDeep : JsonPathParseResult

    data object Invalid : JsonPathParseResult
}

private fun parseJsonPath(
    expression: String,
    maxPathLength: Int,
    maxPathDepth: Int,
): JsonPathParseResult {
    if (expression.length > maxPathLength) {
        return JsonPathParseResult.TooLong
    }
    if (expression.isEmpty() || expression.first() != '$') {
        return JsonPathParseResult.Invalid
    }

    val segments = mutableListOf<JsonPathSegment>()
    var cursor = 1
    while (cursor < expression.length) {
        when (expression[cursor]) {
            '.' -> {
                cursor += 1
                val start = cursor
                while (cursor < expression.length && isFieldCharacter(expression[cursor], cursor == start)) {
                    cursor += 1
                }
                if (cursor == start) {
                    return JsonPathParseResult.Invalid
                }
                segments += JsonPathSegment.Field(expression.substring(start, cursor))
            }
            '[' -> {
                val close = expression.indexOf(']', startIndex = cursor + 1)
                if (close == -1) {
                    return JsonPathParseResult.Invalid
                }
                val token = expression.substring(cursor + 1, close)
                segments += when {
                    token == "*" -> JsonPathSegment.Wildcard
                    token.isNotEmpty() && token.all(Char::isDigit) -> {
                        val index = token.toIntOrNull()
                            ?: return JsonPathParseResult.Invalid
                        JsonPathSegment.Index(index)
                    }
                    isQuotedField(token) -> JsonPathSegment.Field(token.substring(1, token.length - 1))
                    else -> return JsonPathParseResult.Invalid
                }
                cursor = close + 1
            }
            else -> return JsonPathParseResult.Invalid
        }
        if (segments.size > maxPathDepth) {
            return JsonPathParseResult.TooDeep
        }
    }
    return JsonPathParseResult.Success(segments)
}

private fun isFieldCharacter(
    character: Char,
    isFirst: Boolean,
): Boolean =
    character in 'a'..'z' ||
        character in 'A'..'Z' ||
        character == '_' ||
        (!isFirst && (character in '0'..'9' || character == '-'))

private fun isQuotedField(token: String): Boolean {
    if (token.length < 2) {
        return false
    }
    val quote = token.first()
    if ((quote != '\'' && quote != '"') || token.last() != quote) {
        return false
    }
    val content = token.substring(1, token.length - 1)
    return '\\' !in content &&
        quote !in content &&
        content.none(Char::isISOControl)
}

private fun exceedsNestingDepth(
    source: String,
    maxDepth: Int,
): Boolean {
    var depth = 0
    var inString = false
    var escaped = false
    source.forEach { character ->
        if (inString) {
            when {
                escaped -> escaped = false
                character == '\\' -> escaped = true
                character == '"' -> inString = false
            }
        } else {
            when (character) {
                '"' -> inString = true
                '{', '[' -> {
                    depth += 1
                    if (depth > maxDepth) {
                        return true
                    }
                }
                '}', ']' -> if (depth > 0) {
                    depth -= 1
                }
            }
        }
    }
    return false
}
