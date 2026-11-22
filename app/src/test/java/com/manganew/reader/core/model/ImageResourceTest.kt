package com.manganew.reader.core.model

import java.io.File
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageResourceTest {
    @Test
    fun acceptsAbsoluteHttpImageLocationAndRequestMetadata() {
        val resource = ImageResource(
            url = "https://images.example.test/page-1.jpg?token=abc",
            referer = "https://reader.example.test/chapter/1",
            headers = mapOf("Accept" to "image/*"),
            expiresAtEpochSeconds = 1_800_000_000,
            mimeTypeHint = "image/jpeg",
        )

        assertFalse(resource.url.startsWith("file:"))
    }

    @Test
    fun rejectsRelativeFileAndCredentialBearingUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageResource("/images/page-1.jpg")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageResource("file:///tmp/page-1.jpg")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageResource("https://user:password@example.test/page-1.jpg")
        }
    }

    @Test
    fun rejectsUnsafeHeaderValuesAndInvalidExpiry() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageResource(
                url = "https://images.example.test/page-1.jpg",
                headers = mapOf("X-Test" to "safe\r\nInjected: true"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageResource(
                url = "https://images.example.test/page-1.jpg",
                expiresAtEpochSeconds = 0,
            )
        }
    }

    @Test
    fun modelHasNoByteArrayFileOrPathField() {
        val fields = ImageResource::class.java.declaredFields

        assertFalse(fields.any { it.type == ByteArray::class.java })
        assertFalse(fields.any { File::class.java.isAssignableFrom(it.type) })
        assertFalse(fields.any { Path::class.java.isAssignableFrom(it.type) })
        assertFalse(fields.any { it.name.lowercase() in setOf("bytes", "path", "file") })
    }
}
