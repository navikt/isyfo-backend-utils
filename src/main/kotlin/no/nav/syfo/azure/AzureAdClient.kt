package no.nav.syfo.azure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import no.nav.syfo.http.httpClientProxy
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AzureAdClient(
    private val azureEnvironment: AzureEnvironment,
    private val httpClient: HttpClient = httpClientProxy()
) {
    suspend fun getOnBehalfOfToken(scopeClientId: String, token: String): AzureAdToken? = getAccessToken(
        Parameters.build {
            append("client_id", azureEnvironment.appClientId)
            append("client_secret", azureEnvironment.appClientSecret)
            append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            append("assertion", token)
            append("scope", "api://$scopeClientId/.default")
            append("requested_token_use", "on_behalf_of")
        }
    )?.toAzureAdToken()

    suspend fun getSystemToken(scopeClientId: String): AzureAdToken? {
        val cacheKey = "${CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX}$scopeClientId"
        val cachedToken = cache.get(key = cacheKey)
        return if (cachedToken?.isExpired() == false) {
            cachedToken
        } else {
            val azureAdTokenResponse = getAccessToken(
                Parameters.build {
                    append("client_id", azureEnvironment.appClientId)
                    append("client_secret", azureEnvironment.appClientSecret)
                    append("grant_type", "client_credentials")
                    append("scope", "api://$scopeClientId/.default")
                }
            )
            azureAdTokenResponse?.let { token ->
                token.toAzureAdToken().also {
                    cache[cacheKey] = it
                }
            }
        }
    }

    private suspend fun getAccessToken(
        formParameters: Parameters
    ): AzureAdTokenResponse? =
        try {
            val response: HttpResponse = httpClient.post(azureEnvironment.openidConfigTokenEndpoint) {
                accept(ContentType.Application.Json)
                setBody(FormDataContent(formParameters))
            }
            response.body<AzureAdTokenResponse>()
        } catch (e: ResponseException) {
            handleUnexpectedResponseException(e)
            null
        }

    private fun handleUnexpectedResponseException(responseException: ResponseException) {
        log.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
    }

    companion object {
        private const val CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX = "azuread-token-system-"
        private val cache = ConcurrentHashMap<String, AzureAdToken>()
        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}
