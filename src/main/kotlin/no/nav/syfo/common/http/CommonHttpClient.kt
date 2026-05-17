package no.nav.syfo.common.http

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.engine.apache5.Apache5EngineConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.syfo.common.util.applyCommonJacksonConfig
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner
import java.net.ProxySelector

/**
 * Base [HttpClient] configuration shared by all HTTP clients in the isyfo ecosystem.
 *
 * Installs:
 * - JSON content negotiation via Jackson with [applyCommonJacksonConfig]
 * - Automatic retry (up to 2 retries) on non-client errors (i.e. not 4xx responses), with a 500ms constant delay
 * - [expectSuccess] = true, so non-2xx responses throw [io.ktor.client.plugins.ResponseException]
 *
 * Use this as a base when configuring a custom [HttpClient] with a mock engine in tests.
 */
val commonConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {
    install(ContentNegotiation) {
        jackson { applyCommonJacksonConfig() }
    }
    install(HttpRequestRetry) {
        retryOnExceptionIf(2) { _, cause ->
            cause !is ClientRequestException
        }
        constantDelay(500L)
    }
    expectSuccess = true
}

// Extends commonConfig with proxy routing via the JVM system proxy settings (e.g. HTTPS_PROXY env var set by Nais).
// SystemDefaultRoutePlanner consults ProxySelector.getDefault() per request, routing internet-bound traffic
// through the Nav outbound proxy while intra-cluster traffic (matching NO_PROXY) goes direct.
internal val proxyConfig: HttpClientConfig<Apache5EngineConfig>.() -> Unit = {
    this.commonConfig()
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

/**
 * Creates an [HttpClient] for intra-cluster calls (no proxy).
 *
 * Use this for calls to other services within the same cluster, such as PDL, dokarkiv, or istilgangskontroll.
 * Applies [commonConfig].
 */
fun defaultHttpClient() = HttpClient(Apache5, commonConfig)

/**
 * Creates an [HttpClient] for internet-bound calls, routed through the Nav outbound proxy.
 *
 * Use this for calls that need to reach external endpoints, such as the Azure AD token endpoint
 * or the OpenID Connect well-known configuration URL.
 * Applies [commonConfig] plus proxy routing via [java.net.ProxySelector].
 */
fun proxyHttpClient() = HttpClient(Apache5, proxyConfig)
