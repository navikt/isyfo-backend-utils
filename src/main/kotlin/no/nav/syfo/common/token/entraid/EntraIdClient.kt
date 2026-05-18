package no.nav.syfo.common.token.entraid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.syfo.common.http.defaultHttpClient
import no.nav.syfo.common.token.OboTokenProvider
import org.slf4j.LoggerFactory

/**
 * OBO token provider using the Nais token exchange sidecar (Texas) for Entra ID (formerly Azure AD).
 *
 * Instead of communicating directly with Azure AD, this client calls the local Texas sidecar
 * at [tokenExchangeEndpoint] and [tokenEndpoint]. Texas handles token caching, renewal,
 * and communication with Entra ID — no client credentials are needed in the app.
 *
 * @param tokenExchangeEndpoint OBO token exchange endpoint URL. Defaults to `NAIS_TOKEN_EXCHANGE_ENDPOINT` env var.
 * @param tokenEndpoint M2M (client credentials) token endpoint URL. Defaults to `NAIS_TOKEN_ENDPOINT` env var.
 * @param httpClient HTTP client to use. Defaults to [defaultHttpClient].
 */
class EntraIdClient(
    private val tokenExchangeEndpoint: String = System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
    private val tokenEndpoint: String = System.getenv("NAIS_TOKEN_ENDPOINT"),
    private val httpClient: HttpClient = defaultHttpClient()
) : OboTokenProvider {
    /**
     * Exchanges the veileder's incoming token for an OBO token scoped to the given [scopeClientId].
     *
     * @param scopeClientId The Nais scope identifier for the target API, e.g. `api://<cluster>.<namespace>.<app>/.default`.
     * @param token The veileder's incoming Bearer token (without the "Bearer " prefix).
     * @return The OBO access token string, or null on error.
     */
    override suspend fun getOnBehalfOfToken(scopeClientId: String, token: String): String? =
        try {
            val response = httpClient.post(tokenExchangeEndpoint) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    EntraIdTokenExchangeRequest(
                        identityProvider = IDENTITY_PROVIDER,
                        target = scopeClientId,
                        userToken = token
                    )
                )
            }
            response.body<EntraIdTokenResponse>().accessToken
        } catch (e: ClientRequestException) {
            log.error(
                "Client error while exchanging OBO token with EntraId via Texas (NAIS_TOKEN_EXCHANGE_ENDPOINT): statusCode=${e.response.status.value}",
                e
            )
            null
        } catch (e: ServerResponseException) {
            log.error(
                "Server error while exchanging OBO token with EntraId via Texas (NAIS_TOKEN_EXCHANGE_ENDPOINT): statusCode=${e.response.status.value}",
                e
            )
            null
        }

    /**
     * Acquires an M2M (client credentials) token scoped to the given [scopeClientId].
     * Token caching is handled by Texas (Nais sidecar).
     *
     * @param scopeClientId The Nais scope identifier for the target API, e.g. `api://<cluster>.<namespace>.<app>/.default`.
     * @return The access token string, or null on error.
     */
    suspend fun getSystemToken(scopeClientId: String): String? =
        try {
            val response = httpClient.post(tokenEndpoint) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    EntraIdTokenRequest(
                        identityProvider = IDENTITY_PROVIDER,
                        target = scopeClientId
                    )
                )
            }
            response.body<EntraIdTokenResponse>().accessToken
        } catch (e: ClientRequestException) {
            log.error(
                "Client error while acquiring system token from Nais token endpoint: statusCode=${e.response.status.value}",
                e
            )
            null
        } catch (e: ServerResponseException) {
            log.error(
                "Server error while acquiring system token from Nais token endpoint: statusCode=${e.response.status.value}",
                e
            )
            null
        }

    companion object {
        private const val IDENTITY_PROVIDER = "entra_id"
        private val log = LoggerFactory.getLogger(EntraIdClient::class.java)
    }
}
