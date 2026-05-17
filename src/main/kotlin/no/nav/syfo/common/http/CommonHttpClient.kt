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

internal val proxyConfig: HttpClientConfig<Apache5EngineConfig>.() -> Unit = {
    this.commonConfig()
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

fun defaultHttpClient() = HttpClient(Apache5, commonConfig)
fun proxyHttpClient() = HttpClient(Apache5, proxyConfig)
