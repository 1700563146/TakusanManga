package com.manganew.reader.source.api

import com.manganew.reader.core.model.Chapter
import com.manganew.reader.core.model.ComicDetail
import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.ComicSummary
import com.manganew.reader.core.model.ImageResource
import com.manganew.reader.core.model.PageResult

/**
 * Stable source contract consumed by repositories and eventually third-party
 * adapters.
 *
 * All operations are suspend functions so callers can cancel work. Implementors
 * must propagate cancellation instead of converting it to a normal failure.
 * Methods receiving a [ComicRef] or [Chapter] must reject entities whose source
 * ID differs from [descriptor].
 *
 * Implementations must not expose request/session objects, Android Context,
 * parser implementations, or plugin runtime handles through this interface.
 */
public interface MangaSource {
    /** Metadata and network permissions for this source instance. */
    public val descriptor: SourceDescriptor

    /**
     * Search this source.
     *
     * [keyword] is expected to be trimmed and bounded by the caller.
     * [pageToken] is opaque and must be passed back unchanged.
     */
    public suspend fun search(
        keyword: String,
        pageToken: String? = null,
    ): PageResult<ComicSummary>

    /** Load a comic whose [ComicRef.sourceId] belongs to this source. */
    public suspend fun getDetail(ref: ComicRef): ComicDetail

    /** Load chapters for a comic belonging to this source. */
    public suspend fun getChapters(ref: ComicRef): List<Chapter>

    /** Load image locations for a chapter belonging to this source. */
    public suspend fun getPages(chapter: Chapter): List<ImageResource>

    /** Run a bounded connection/capability test without downloading content lists. */
    public suspend fun testConnection(): SourceTestResult

    /** Release source-scoped resources. Implementations must be idempotent. */
    public suspend fun close()
}
