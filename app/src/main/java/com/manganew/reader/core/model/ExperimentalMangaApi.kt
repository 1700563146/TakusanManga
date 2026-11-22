package com.manganew.reader.core.model

/**
 * Marks source/domain contracts that are still Draft and may change before a
 * stable public SDK release.
 */
@RequiresOptIn(
    message = "This source API is Draft and may change before the first stable SDK.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalMangaApi
