package com.manganew.reader.parser.impl.html

import java.net.URI
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Failures returned by local HTML parsing and selector evaluation. */
internal enum class HtmlParserFailure {
    DOCUMENT_TOO_LARGE,
    INVALID_BASE_URL,
    SELECTOR_TOO_LONG,
    INVALID_SELECTOR,
    TOO_MANY_MATCHES,
}

/** Result of parsing one HTML text response. */
internal sealed interface HtmlParseResult {
    data class Success(val document: HtmlDocument) : HtmlParseResult

    data class Failure(val reason: HtmlParserFailure) : HtmlParseResult
}

/** Result of one CSS selector evaluation. */
internal sealed interface HtmlSelectionResult {
    data class Success(val elements: List<HtmlElement>) : HtmlSelectionResult

    data class Failure(val reason: HtmlParserFailure) : HtmlSelectionResult
}

/** Extraction from the first matching element. */
internal sealed interface HtmlFieldResult {
    data class Value(val value: String) : HtmlFieldResult

    data object Missing : HtmlFieldResult

    data class Failure(val reason: HtmlParserFailure) : HtmlFieldResult
}

/** Extraction from all matching elements in document order. */
internal sealed interface HtmlFieldListResult {
    data class Values(val values: List<String>) : HtmlFieldListResult

    data class Failure(val reason: HtmlParserFailure) : HtmlFieldListResult
}

/** Field extraction supported by Native JSON HTML rules. */
internal sealed interface HtmlExtraction {
    data object Text : HtmlExtraction

    data object Html : HtmlExtraction

    data class Attribute(val name: String) : HtmlExtraction

    data class AbsoluteUrl(val name: String) : HtmlExtraction

    data object Value : HtmlExtraction
}

/**
 * Parses untrusted HTML text without loading external resources.
 *
 * This class never calls network APIs. A base URL only supplies context for
 * relative link and image URL resolution.
 */
internal class HtmlDocumentParser(
    private val maxDocumentBytes: Int = 4 * 1024 * 1024,
    private val maxSelectorLength: Int = 512,
    private val maxMatches: Int = 1_000,
) {
    init {
        require(maxDocumentBytes > 0) { "maxDocumentBytes must be positive" }
        require(maxSelectorLength > 0) { "maxSelectorLength must be positive" }
        require(maxMatches > 0) { "maxMatches must be positive" }
    }

    fun parse(
        html: String,
        baseUrl: String? = null,
    ): HtmlParseResult {
        if (html.encodeToByteArray().size > maxDocumentBytes) {
            return HtmlParseResult.Failure(HtmlParserFailure.DOCUMENT_TOO_LARGE)
        }
        if (baseUrl != null && !isValidBaseUrl(baseUrl)) {
            return HtmlParseResult.Failure(HtmlParserFailure.INVALID_BASE_URL)
        }

        val document = try {
            Jsoup.parse(html, baseUrl.orEmpty())
        } catch (_: IllegalArgumentException) {
            return HtmlParseResult.Failure(HtmlParserFailure.INVALID_SELECTOR)
        }
        return HtmlParseResult.Success(
            HtmlDocument(
                document = document,
                maxSelectorLength = maxSelectorLength,
                maxMatches = maxMatches,
            ),
        )
    }

    private fun isValidBaseUrl(value: String): Boolean {
        if (value.isBlank() || value != value.trim() || value.length > MaxBaseUrlLength) {
            return false
        }
        val uri = try {
            URI(value)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return uri.isAbsolute &&
            uri.scheme.lowercase(Locale.ROOT) in setOf("http", "https") &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null
    }

    private companion object {
        const val MaxBaseUrlLength = 8 * 1024
    }
}

/** Parsed HTML document with bounded local CSS selection. */
internal class HtmlDocument internal constructor(
    private val document: Document,
    private val maxSelectorLength: Int,
    private val maxMatches: Int,
) {
    fun select(selector: String): HtmlSelectionResult {
        if (selector.length > maxSelectorLength) {
            return HtmlSelectionResult.Failure(HtmlParserFailure.SELECTOR_TOO_LONG)
        }
        if (selector.isBlank()) {
            return HtmlSelectionResult.Failure(HtmlParserFailure.INVALID_SELECTOR)
        }

        val elements = try {
            document.select(selector)
        } catch (_: IllegalStateException) {
            return HtmlSelectionResult.Failure(HtmlParserFailure.INVALID_SELECTOR)
        }
        if (elements.size > maxMatches) {
            return HtmlSelectionResult.Failure(HtmlParserFailure.TOO_MANY_MATCHES)
        }
        return HtmlSelectionResult.Success(elements.map(::HtmlElement))
    }

    fun extractFirst(
        selector: String,
        extraction: HtmlExtraction,
    ): HtmlFieldResult =
        when (val selection = select(selector)) {
            is HtmlSelectionResult.Failure -> HtmlFieldResult.Failure(selection.reason)
            is HtmlSelectionResult.Success -> {
                val element = selection.elements.firstOrNull()
                    ?: return HtmlFieldResult.Missing
                element.extract(extraction)?.let(HtmlFieldResult::Value)
                    ?: HtmlFieldResult.Missing
            }
        }

    fun extractAll(
        selector: String,
        extraction: HtmlExtraction,
    ): HtmlFieldListResult =
        when (val selection = select(selector)) {
            is HtmlSelectionResult.Failure -> HtmlFieldListResult.Failure(selection.reason)
            is HtmlSelectionResult.Success -> HtmlFieldListResult.Values(
                selection.elements.mapNotNull { it.extract(extraction) },
            )
        }
}

/** Bounded wrapper around one selected Jsoup element. */
internal class HtmlElement internal constructor(
    private val element: org.jsoup.nodes.Element,
) {
    val tagName: String
        get() = element.tagName()

    fun text(): String = element.text()

    fun html(): String = element.html()

    fun extract(extraction: HtmlExtraction): String? =
        when (extraction) {
            HtmlExtraction.Text -> text()
            HtmlExtraction.Html -> html()
            HtmlExtraction.Value -> element.`val`()
            is HtmlExtraction.Attribute -> {
                if (element.hasAttr(extraction.name)) {
                    element.attr(extraction.name)
                } else {
                    null
                }
            }
            is HtmlExtraction.AbsoluteUrl -> {
                if (!element.hasAttr(extraction.name) || element.attr(extraction.name).isBlank()) {
                    null
                } else {
                    element.absUrl(extraction.name).takeIf(String::isNotBlank)
                }
            }
        }
}
