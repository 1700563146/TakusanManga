package com.manganew.reader.core.model

/**
 * Ordered page of source results.
 *
 * [nextPageToken] is opaque and is passed back unchanged. It may represent a
 * page number, URL cursor, or another source-specific continuation and must
 * not be parsed by intermediate layers. [items] order is preserved exactly and
 * no repository may reorder it.
 *
 * ```
 * val page = PageResult(items = listOf(summary), nextPageToken = "cursor-2")
 * check(page.hasMore)
 * ```
 */
public data class PageResult<T>(
    public val items: List<T>,
    public val nextPageToken: String? = null,
) {
    init {
        if (nextPageToken != null) {
            require(nextPageToken.isNotBlank()) { "nextPageToken must not be blank" }
            require(nextPageToken == nextPageToken.trim()) {
                "nextPageToken must not contain surrounding whitespace"
            }
            require(nextPageToken.length <= MaxPageTokenLength) {
                "nextPageToken exceeds $MaxPageTokenLength characters"
            }
            require(nextPageToken.none(Char::isISOControl)) {
                "nextPageToken must not contain control characters"
            }
        }
    }

    /** Whether another page can be requested with [nextPageToken]. */
    public val hasMore: Boolean
        get() = nextPageToken != null
}
