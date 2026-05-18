package no.nav.syfo.common.auth

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.common.http.proxyHttpClient

/**
 * Fetches and parses the OpenID Connect discovery document from [wellKnownUrl].
 * Uses a proxy-aware HTTP client. Intended to be called once at application startup.
 */
fun getWellKnown(wellKnownUrl: String): WellKnown =
    runBlocking {
        proxyHttpClient().use { client ->
            client.get(wellKnownUrl).body<WellKnownDTO>().toWellKnown()
        }
    }
