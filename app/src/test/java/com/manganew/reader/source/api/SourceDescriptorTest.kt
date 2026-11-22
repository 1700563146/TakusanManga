package com.manganew.reader.source.api

import com.manganew.reader.core.model.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceDescriptorTest {
    @Test
    fun validHttpsDescriptorExposesExactHostsAndCapabilities() {
        val descriptor = descriptor()

        assertEquals("https://example.test", descriptor.baseUrl)
        assertEquals(
            setOf("example.test", "images.example.test"),
            descriptor.allowedHosts,
        )
        assertEquals(false, descriptor.allowHttp)
    }

    @Test
    fun httpRequiresExplicitPermission() {
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(baseUrl = "http://example.test")
        }

        val allowed = descriptor(
            baseUrl = "http://example.test",
            allowHttp = true,
        )
        assertEquals("http://example.test", allowed.baseUrl)
    }

    @Test
    fun baseHostMustBeDeclaredExactly() {
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(allowedHosts = setOf("images.example.test"))
        }
    }

    @Test
    fun rejectsWildcardUppercasePortInvalidHostsAndBlankCapabilities() {
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(allowedHosts = setOf("*.example.test"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(allowedHosts = setOf("Example.Test"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(allowedHosts = setOf("example.test:443"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(allowedHosts = setOf("127.0.0.1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(allowedHosts = setOf("example..test"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(capabilities = emptySet())
        }
    }

    @Test
    fun rejectsEmbeddedCredentialsAndNonPositiveAdapterVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(baseUrl = "https://user:pass@example.test")
        }
        assertThrows(IllegalArgumentException::class.java) {
            descriptor(adapterApiVersion = 0)
        }
    }

    private fun descriptor(
        baseUrl: String = "https://example.test",
        allowedHosts: Set<String> = setOf("example.test", "images.example.test"),
        capabilities: Set<SourceCapability> = setOf(
            SourceCapability.SEARCH,
            SourceCapability.DETAIL,
        ),
        allowHttp: Boolean = false,
        adapterApiVersion: Int = 1,
    ) = SourceDescriptor(
        id = SourceId("demo-source"),
        name = "Demo Source",
        version = "1.0.0",
        format = SourceFormat.NATIVE_JSON,
        adapterApiVersion = adapterApiVersion,
        baseUrl = baseUrl,
        allowedHosts = allowedHosts,
        capabilities = capabilities,
        allowHttp = allowHttp,
    )
}
