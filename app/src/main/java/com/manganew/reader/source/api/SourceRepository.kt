package com.manganew.reader.source.api

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId
import kotlinx.coroutines.flow.Flow

/**
 * Host-owned source installation and lifecycle contract.
 *
 * UI code observes summaries instead of holding a [MangaSource] runtime. The
 * host may use [getSource] for controlled orchestration, but must keep runtime
 * lifetime and cleanup outside presentation components.
 */
@ExperimentalMangaApi
public interface SourceRepository {
    /** Observe installed source metadata in stable host order. */
    public fun observeInstalledSources(): Flow<List<InstalledSourceSummary>>

    /** Snapshot of installed sources in stable host order. */
    public suspend fun listInstalledSources(): List<InstalledSourceSummary>

    /** Return one installed source summary, or null when it is not installed. */
    public suspend fun getInstalledSource(sourceId: SourceId): InstalledSourceSummary?

    /** Enable or disable a source without deleting its configuration. */
    public suspend fun setEnabled(sourceId: SourceId, enabled: Boolean)

    /**
     * Remove one source and release source-scoped resources.
     *
     * Removing a missing source is a no-op.
     */
    public suspend fun remove(sourceId: SourceId)

    /** Run a bounded source connection test. */
    public suspend fun testConnection(sourceId: SourceId): SourceTestResult

    /**
     * Obtain the source runtime for controlled host orchestration.
     *
     * Presentation code must not retain or invoke this runtime directly.
     */
    public suspend fun getSource(sourceId: SourceId): MangaSource
}
