package com.manganew.reader.source.api

import com.manganew.reader.core.model.ExperimentalMangaApi

/**
 * Host-managed installation state for one validated source descriptor.
 *
 * Enabled state belongs to the host and is not controlled by the source itself.
 */
@ExperimentalMangaApi
public data class InstalledSourceSummary(
    public val descriptor: SourceDescriptor,
    public val enabled: Boolean,
)
