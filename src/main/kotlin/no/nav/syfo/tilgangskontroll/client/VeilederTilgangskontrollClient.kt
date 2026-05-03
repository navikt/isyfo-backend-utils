package no.nav.syfo.tilgangskontroll.client

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
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import no.nav.syfo.azure.AzureAdClient
import no.nav.syfo.http.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class VeilederTilgangskontrollClient(
    private val azureAdClient: AzureAdClient,
    private val config: VeilederTilgangConfig,
    private val httpClient: HttpClient = httpClientDefault(),
    meterRegistry: MeterRegistry = Metrics.globalRegistry
) {
    private val tilgangskontrollPersonUrl = "${config.baseUrl}$TILGANGSKONTROLL_PERSON_PATH"
    private val tilgangskontrollBrukereUrl = "${config.baseUrl}$TILGANGSKONTROLL_BRUKERE_PATH"

    private val countSuccess = Counter.builder(CALL_TILGANGSKONTROLL_PERSON_SUCCESS)
        .description("Counts the number of successful calls to istilgangskontroll - person")
        .register(meterRegistry)
    private val countFail = Counter.builder(CALL_TILGANGSKONTROLL_PERSON_FAIL)
        .description("Counts the number of failed calls to istilgangskontroll - person")
        .register(meterRegistry)
    private val countForbidden = Counter.builder(CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN)
        .description("Counts the number of forbidden calls to istilgangskontroll - person")
        .register(meterRegistry)

    private suspend fun getTilgang(callId: String, personIdent: String, token: String): Tilgang? {
        val onBehalfOfToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = config.clientId,
            token = token
        )?.accessToken
            ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        return try {
            val tilgangResponse = httpClient.get(tilgangskontrollPersonUrl) {
                header(HttpHeaders.Authorization, bearerHeader(onBehalfOfToken))
                header(NAV_PERSONIDENT_HEADER, personIdent)
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            countSuccess.increment()
            tilgangResponse.body<Tilgang>()
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                countForbidden.increment()
            } else {
                handleUnexpectedResponseException(e.response, callId)
                countFail.increment()
            }
            null
        }
    }

    suspend fun hasAccess(callId: String, personIdent: String, token: String): Boolean {
        return getTilgang(callId, personIdent, token)?.erGodkjent ?: false
    }

    suspend fun hasWriteAccess(callId: String, personIdent: String, token: String): Boolean {
        return getTilgang(callId, personIdent, token)?.let {
            it.erGodkjent && it.fullTilgang
        } ?: false
    }

    suspend fun veilederPersonerAccess(
        personidenter: List<String>,
        token: String,
        callId: String
    ): List<String>? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = config.clientId,
            token = token
        )?.accessToken
            ?: throw RuntimeException("Failed to request access to list of persons: Failed to get OBO token")

        return try {
            val response: HttpResponse = httpClient.post(tilgangskontrollBrukereUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(personidenter)
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

    companion object {
        private val log = LoggerFactory.getLogger(VeilederTilgangskontrollClient::class.java)

        private const val TILGANGSKONTROLL_PERSON_PATH = "/api/tilgang/navident/person"
        private const val TILGANGSKONTROLL_BRUKERE_PATH = "/api/tilgang/navident/brukere"

        private const val CALL_TILGANGSKONTROLL_PERSON_BASE = "tilgangskontroll_person"
        private const val CALL_TILGANGSKONTROLL_PERSON_SUCCESS = "${CALL_TILGANGSKONTROLL_PERSON_BASE}_success_count"
        private const val CALL_TILGANGSKONTROLL_PERSON_FAIL = "${CALL_TILGANGSKONTROLL_PERSON_BASE}_fail_count"
        private const val CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN = "${CALL_TILGANGSKONTROLL_PERSON_BASE}_forbidden_count"
    }
}
