package com.manganew.reader.feature.source

import com.manganew.reader.source.api.InstalledSourceSummary
import com.manganew.reader.source.api.SourceCapability
import com.manganew.reader.source.api.SourceFormat
import java.net.URI

internal fun InstalledSourceSummary.toUiModel(): PrototypeSource = PrototypeSource(
    id = descriptor.id.value,
    name = descriptor.name,
    host = URI(descriptor.baseUrl).host.orEmpty(),
    format = descriptor.format.displayName(),
    version = descriptor.version,
    enabled = enabled,
    capabilities = descriptor.capabilities
        .map(SourceCapability::displayName)
        .sorted(),
)

private fun SourceFormat.displayName(): String = when (this) {
    SourceFormat.NATIVE_JSON -> "Native JSON"
    SourceFormat.LEGADO_JSON_SUBSET -> "Legado JSON"
    SourceFormat.NATIVE_JS -> "Native JS"
    SourceFormat.BREEZE_JS_COMPAT -> "Breeze JS"
}

private fun SourceCapability.displayName(): String = when (this) {
    SourceCapability.SEARCH -> "搜索"
    SourceCapability.DETAIL -> "详情"
    SourceCapability.CHAPTERS -> "章节"
    SourceCapability.PAGES -> "页面"
    SourceCapability.EXPLORE -> "探索"
    SourceCapability.ADVANCED_IMAGE -> "高级图片"
}
