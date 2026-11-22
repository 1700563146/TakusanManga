package com.manganew.reader.core.model

/**
 * Unique comic identity across all installed sources.
 *
 * A remote ID is only unique within its source, so [sourceId] and [remoteId]
 * must always travel together.
 */
@ExperimentalMangaApi
public data class ComicRef(
    public val sourceId: SourceId,
    public val remoteId: RemoteId,
)

/**
 * Search/list representation of a comic.
 *
 * Collections from adapters are retained in source order. Optional nullable
 * fields mean the source did not provide a reliable value.
 *
 * ```
 * val summary = ComicSummary(
 *     ref = ComicRef(SourceId("demo"), RemoteId("comic-1")),
 *     title = "Example",
 * )
 * ```
 */
@ExperimentalMangaApi
public data class ComicSummary(
    public val ref: ComicRef,
    public val title: String,
    public val cover: ImageResource? = null,
    public val subtitle: String? = null,
    public val author: String? = null,
    public val status: String? = null,
    public val latestChapter: String? = null,
) {
    init {
        requireSingleLineText("title", title, MaxTitleLength)
        requireOptionalSingleLineText("subtitle", subtitle)
        requireOptionalSingleLineText("author", author)
        requireOptionalSingleLineText("status", status)
        requireOptionalSingleLineText("latestChapter", latestChapter)
    }
}

/**
 * Detailed comic information returned by one source.
 *
 * [tags] order is preserved. [metadata] is display-only short text and must not
 * contain full HTML, chapter bodies, Base64 data, or sensitive credentials.
 */
@ExperimentalMangaApi
public data class ComicDetail(
    public val ref: ComicRef,
    public val title: String,
    public val cover: ImageResource? = null,
    public val author: String? = null,
    public val description: String? = null,
    public val tags: List<String> = emptyList(),
    public val status: String? = null,
    public val metadata: Map<String, String> = emptyMap(),
) {
    init {
        requireSingleLineText("title", title, MaxTitleLength)
        requireOptionalSingleLineText("author", author)
        requireDescription(description)
        requireOptionalSingleLineText("status", status)
        require(tags.size <= MaxTagCount) { "tags exceed $MaxTagCount entries" }
        tags.forEachIndexed { index, tag ->
            requireSingleLineText("tags[$index]", tag, MaxTagLength)
        }
        requireMetadata(metadata)
    }
}

/**
 * Chapter identity and display metadata.
 *
 * [order] is optional because some sources only provide list order. When
 * present it must be non-negative. [publishedAt] is display text and must not
 * be parsed by the reader.
 */
@ExperimentalMangaApi
public data class Chapter(
    public val sourceId: SourceId,
    public val comicId: RemoteId,
    public val chapterId: RemoteId,
    public val title: String,
    public val order: Int? = null,
    public val publishedAt: String? = null,
) {
    init {
        requireSingleLineText("chapter.title", title, MaxTitleLength)
        require(order == null || order >= 0) { "chapter.order must be non-negative" }
        requireOptionalSingleLineText("chapter.publishedAt", publishedAt)
    }
}
