package no.nav.syfo.common.tilgangskontroll.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.common.azure.OboTokenProvider

import no.nav.syfo.common.http.commonConfig
import no.nav.syfo.common.testhelper.receiveBody
import no.nav.syfo.common.testhelper.respond
import no.nav.syfo.common.util.NAV_CALL_ID_HEADER
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.common.util.bearerHeader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
/**
 * Unit test for TilgangskontrollClient. azureAdClient and httpClient dependencies are mocked.
 */
class TilgangskontrollClientTest {
    private val token = "token"
    private val oboToken = "obo-token"
    private val callId = "call-id"
    private val personident = "12345678910"
    private val config = TilgangskontrollClientConfig(
        baseUrl = "isTilgangskontrollUrl",
        clientId = "dev-fss.teamsykefravr.istilgangskontroll"
    )
    private val oboTokenProvider = mockk<OboTokenProvider>()

    @BeforeEach
    fun setup() {
        coEvery {
            oboTokenProvider.getOnBehalfOfToken(any(), any())
        } returns oboToken
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `hasAccess returns true when tilgang is approved and sends expected headers to istilgangskontroll`() {
        lateinit var authorizationHeader: String
        lateinit var personidentHeader: String
        lateinit var callIdHeader: String

        // In order to intercept the headers that istilgangskontroll would be called with, this test
        // sets up httpClient and TilgangskontrollClient manually instead of using createMockClientForResponse().
        val httpClient = HttpClient(MockEngine) {
            commonConfig()
            engine {
                addHandler { request ->
                    authorizationHeader = request.headers[HttpHeaders.Authorization].orEmpty()
                    personidentHeader = request.headers[NAV_PERSONIDENT_HEADER].orEmpty()
                    callIdHeader = request.headers[NAV_CALL_ID_HEADER].orEmpty()
                    respond(Tilgang(erGodkjent = true))
                }
            }
        }

        val client = TilgangskontrollClient(
            oboTokenProvider = oboTokenProvider,
            config = config,
            httpClient = httpClient
        )

        runBlocking {
            assertTrue(client.hasAccess(callId, personident, token))
        }

        assertEquals(bearerHeader(oboToken), authorizationHeader)
        assertEquals(personident, personidentHeader)
        assertEquals(callId, callIdHeader)
    }

    @Test
    fun `hasAccess and hasWriteAccess returns true when tilgang to person is approved and user has fullTilgang`() {
        val client = createMockClientForResponse(Tilgang(erGodkjent = true, fullTilgang = true))

        runBlocking {
            assertTrue(client.hasAccess(callId, personident, token))
            assertTrue(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess returns true and hasWriteAccess returns false when tilgang to person is approved but user does not have fullTilgang`() {
        val client = createMockClientForResponse(Tilgang(erGodkjent = true, fullTilgang = false))

        runBlocking {
            assertTrue(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess and hasWriteAccess returns false when tilgang is not approved`() {
        val client = createMockClientForResponse(Tilgang(erGodkjent = false, fullTilgang = true))

        runBlocking {
            assertFalse(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess and hasWriteAccess returns false on unexpected response`() {
        val client = createMockClientForResponse(status = HttpStatusCode.InternalServerError)

        runBlocking {
            assertFalse(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess and hasWriteAccess throws when obo token request fails`() {
        coEvery {
            oboTokenProvider.getOnBehalfOfToken(any(), any())
        } returns null

        val client = createMockClientForResponse()

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                client.hasAccess(callId, personident, token)
            }
        }
        assertThrows(RuntimeException::class.java) {
            runBlocking {
                client.hasWriteAccess(callId, personident, token)
            }
        }
    }

    @Test
    fun `veilederPersonerAccess returns filtered personident list and sends expected payload`() {
        val requestedPersonidenter = listOf(personident, "10987654321")
        lateinit var authorizationHeader: String
        lateinit var callIdHeader: String
        lateinit var requestBody: List<String>

        val httpClient = HttpClient(MockEngine) {
            commonConfig()
            engine {
                addHandler { request ->
                    authorizationHeader = request.headers[HttpHeaders.Authorization].orEmpty()
                    callIdHeader = request.headers[NAV_CALL_ID_HEADER].orEmpty()
                    requestBody = request.receiveBody()
                    respond(listOf(personident))
                }
            }
        }

        val client = TilgangskontrollClient(
            oboTokenProvider = oboTokenProvider,
            config = config,
            httpClient = httpClient
        )

        val tilgang = runBlocking {
            client.veilederPersonerAccess(
                personidenter = requestedPersonidenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(listOf(personident), tilgang)
        assertEquals(bearerHeader(oboToken), authorizationHeader)
        assertEquals(callId, callIdHeader)
        assertEquals(requestedPersonidenter, requestBody)
    }

    @Test
    fun `veilederPersonerAccess returns null when istilgangskontroll responds forbidden`() {
        val client = createMockClientForResponse(status = HttpStatusCode.Forbidden)

        val tilgang = runBlocking {
            client.veilederPersonerAccess(
                personidenter = listOf(personident),
                token = token,
                callId = callId
            )
        }

        assertNull(tilgang)
    }

    private fun createMockClientForResponse(
        tilgang: Tilgang = Tilgang(erGodkjent = true),
        status: HttpStatusCode = HttpStatusCode.OK
    ): TilgangskontrollClient {
        val httpClient = HttpClient(MockEngine) {
            commonConfig()
            engine {
                addHandler {
                    if (status == HttpStatusCode.OK) {
                        respond(tilgang, status)
                    } else {
                        respondError(status)
                    }
                }
            }
        }

        return TilgangskontrollClient(
            oboTokenProvider = oboTokenProvider,
            config = config,
            httpClient = httpClient
        )
    }
}
