package com.manganew.reader.network.impl

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Source-scoped, in-memory cookie storage.
 *
 * No cookie value is persisted to disk. Each client owns one instance.
 */
internal class InMemoryCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        synchronized(this.cookies) {
            cookies.forEach { cookie ->
                this.cookies.removeAll { existing ->
                    existing.name == cookie.name &&
                        existing.domain == cookie.domain &&
                        existing.path == cookie.path
                }
                if (cookie.expiresAt > now && cookie.matches(url)) {
                    this.cookies.add(cookie)
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return synchronized(cookies) {
            cookies
                .filter { it.expiresAt > now && it.matches(url) }
                .toList()
        }
    }

    fun clear() {
        synchronized(cookies) {
            cookies.clear()
        }
    }
}
