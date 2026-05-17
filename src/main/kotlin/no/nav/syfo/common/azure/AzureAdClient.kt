package no.nav.syfo.common.azure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import no.nav.syfo.common.http.proxyHttpClient
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AzureAdClient(
    private val azureEnvironment: AzureEnvironment,
    private val httpClient: HttpClient = proxyHttpClient()
) : OboTokenProvider {
    override suspend fun getOnBehalfOfToken(scopeClientId: String, token: String): String? = getAccessToken(
        Parameters.build {
            append("client_id", azureEnvironment.appClientId)
            append("client_secret", azureEnvironment.appClientSecret)
            append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            append("assertion", token)
            append("scope", "api://$scopeClientId/.default")
            append("requested_token_use", "on_behalf_of")
        }
    )?.toAzureAdToken()?.accessToken

    suspend fun getSystemToken(scopeClientId: String): AzureAdToken? {
        val cacheKey = "${CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX}$scopeClientId"
        val cachedToken = cache.get(key = cacheKey)
        return if (cachedToken?.isExpired() == false) {
            COUNT_CALL_AZUREAD_SYSTEM_TOKEN_CACHE_HIT.increment()
            cachedToken
        } else {
            COUNT_CALL_AZUREAD_SYSTEM_TOKEN_CACHE_MISS.increment()
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
        } catch (e: ClientRequestException) {
            log.error(
                "Client error while requesting AzureAD access token with statusCode=${e.response.status.value}",
                e
            )
            null
        } catch (e: ServerResponseException) {
            log.error(
                "Server error while requesting AzureAD access token with statusCode=${e.response.status.value}",
                e
            )
            null
        }

    companion object {
        private const val CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX = "azuread-token-system-"
        private val cache = ConcurrentHashMap<String, AzureAdToken>()
        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}
