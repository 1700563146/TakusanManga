package com.manganew.reader.core.model

/**
 * Location and request metadata for one remote image.
 *
 * This model intentionally has no image bytes, file path, cache key, or local
 * storage handle. Network policy, redirect handling, sensitive-header rules,
 * response limits, and memory caching belong to the host network/image layers.
 *
 * ```
 * val image = ImageResource(
 *     url = "https://images.example.test/page-1.jpg",
 *     referer = "https://reader.example.test/",
 * )
 * ```
 *
 * [expiresAtEpochSeconds] indicates a temporary URL that must be resolved
 * again after expiry.
 */
public data class ImageResource(
    public val url: String,
    public val referer: String? = null,
    public val headers: Map<String, String> = emptyMap(),
    public val expiresAtEpochSeconds: Long? = null,
    public val mimeTypeHint: String? = null,
) {
    init {
        requireHttpUrl("image.url", url)
        if (referer != null) {
            requireHttpUrl("image.referer", referer)
        }
        requireHeaders(headers)
        require(expiresAtEpochSeconds == null || expiresAtEpochSeconds > 0) {
            "image.expiresAtEpochSeconds must be positive when present"
        }
        if (mimeTypeHint != null) {
            requireSingleLineText("image.mimeTypeHint", mimeTypeHint, 127)
            require('/' in mimeTypeHint) { "image.mimeTypeHint must contain a type and subtype" }
        }
    }
}
