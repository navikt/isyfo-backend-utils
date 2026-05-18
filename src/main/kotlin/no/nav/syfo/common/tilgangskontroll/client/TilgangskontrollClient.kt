package no.nav.syfo.common.tilgangskontroll.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.syfo.common.http.defaultHttpClient
import no.nav.syfo.common.token.OboTokenProvider
import no.nav.syfo.common.util.NAV_CALL_ID_HEADER
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.common.util.bearerHeader
import org.slf4j.LoggerFactory

/**
 * Client for istilgangskontroll — the isyfo service that checks what access a veileder has to a given person.
 *
 * Uses an [OboTokenProvider] to exchange the veileder's incoming token for an OBO token scoped to istilgangskontroll
 * before making requests.
 *
 * @param oboTokenProvider Supplies OBO tokens for the veileder's token. Pass an [no.nav.syfo.common.token.azuread.AzureAdClient]
 * directly, or wrap a custom token source in a lambda: `{ scopeClientId, token -> ... }`.
 * @param config Base URL and Nais scope identifier for istilgangskontroll.
 * @param httpClient HTTP client to use. Defaults to [defaultHttpClient]. Override in tests with a mock engine.
 */
class TilgangskontrollClient(
    private val oboTokenProvider: OboTokenProvider,
    private val config: TilgangskontrollClientConfig,
    private val httpClient: HttpClient = defaultHttpClient()
) {
    private val tilgangskontrollPersonUrl = "${config.baseUrl}$TILGANGSKONTROLL_PERSON_PATH"
    private val tilgangskontrollBrukereUrl = "${config.baseUrl}$TILGANGSKONTROLL_BRUKERE_PATH"

    private suspend fun getTilgang(callId: String, personIdent: String, token: String): Tilgang? {
        val onBehalfOfToken = oboTokenProvider.getOnBehalfOfToken(
            scopeClientId = config.clientId,
            token = token
        ) ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        return try {
            val tilgangResponse = httpClient.get(tilgangskontrollPersonUrl) {
                header(HttpHeaders.Authorization, bearerHeader(onBehalfOfToken))
                header(NAV_PERSONIDENT_HEADER, personIdent)
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_TILGANGSKONTROLL_PERSON_SUCCESS.increment()
            tilgangResponse.body<Tilgang>()
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                COUNT_CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN.increment()
            } else {
                handleUnexpectedResponseException(e.response, callId)
                COUNT_CALL_TILGANGSKONTROLL_PERSON_FAIL.increment()
            }
            null
        }
    }

    private fun handleUnexpectedResponseException(
        response: HttpResponse,
        callId: String
    ) {
        log.error(
            "Error while requesting access to person from istilgangskontroll: statusCode={}, callId={}",
            response.status.value,
            callId
        )
    }

    /**
     * Returns true if the veileder has read access to the given person.
     *
     * @param callId Forwarded to istilgangskontroll as the `Nav-Call-Id` request header for tracing across services.
     * @param personIdent The person's national identity number (fødselsnummer).
     * @param token The veileder's incoming Bearer token (without the "Bearer " prefix).
     */
    suspend fun hasAccess(callId: String, personIdent: String, token: String): Boolean {
        return getTilgang(callId, personIdent, token)?.erGodkjent ?: false
    }

    /**
     * Returns true if the veileder has write access (fullTilgang) to the given person.
     * Returns false if the veileder does not have access to the person, or if the veileder does not have fullTilgang.
     *
     * @param callId Forwarded to istilgangskontroll as the `Nav-Call-Id` request header for tracing across services.
     * @param personIdent The person's national identity number (fødselsnummer).
     * @param token The veileder's incoming Bearer token (without the "Bearer " prefix).
     */
    suspend fun hasWriteAccess(callId: String, personIdent: String, token: String): Boolean {
        return getTilgang(callId, personIdent, token)?.let {
            it.erGodkjent && it.fullTilgang
        } ?: false
    }

    /**
     * Returns the subset of [personIdenter] that the veileder has access to.
     * Returns null on error or if access is forbidden entirely.
     *
     * @param personIdenter List of national identity numbers (fødselsnummer) to check.
     * @param token The veileder's incoming Bearer token (without the "Bearer " prefix).
     * @param callId Forwarded to istilgangskontroll as the `Nav-Call-Id` request header for tracing across services.
     */
    suspend fun personsVeilederHasAccessTo(
        personIdenter: List<String>,
        token: String,
        callId: String
    ): List<String>? {
        val oboToken = oboTokenProvider.getOnBehalfOfToken(
            scopeClientId = config.clientId,
            token = token
        ) ?: throw RuntimeException("Failed to request access to list of persons: Failed to get OBO token")

        return try {
            val response: HttpResponse = httpClient.post(tilgangskontrollBrukereUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(personIdenter)
            }
            response.body<List<String>>()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                log.warn("Forbidden to request access to list of person from istilgangskontroll")
                null
            } else {
                log.error("Error while requesting access to list of person from istilgangskontroll: ${e.message}", e)
                null
            }
        } catch (e: ServerResponseException) {
            log.error("Error while requesting access to list of person from istilgangskontroll: ${e.message}", e)
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TilgangskontrollClient::class.java)

        private const val TILGANGSKONTROLL_PERSON_PATH = "/api/tilgang/navident/person"
        private const val TILGANGSKONTROLL_BRUKERE_PATH = "/api/tilgang/navident/brukere"
    }
}
