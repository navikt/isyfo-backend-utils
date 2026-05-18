package no.nav.syfo.common.tilgangskontroll.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.common.http.commonConfig
import no.nav.syfo.common.testhelper.respond
import no.nav.syfo.common.token.OboTokenProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
/**
 * Unit test for the batch access endpoint in TilgangskontrollClient.
 * Tests the personsVeilederHasAccessTo() method with various scenarios including
 * successful responses, access denied, server errors, and empty lists.
 */
class VeilederPersonerAccessTest {
    private val token = "token"
    private val oboToken = "obo-token"
    private val callId = "call-id"
    private val personIdenter = listOf("12345678910", "10987654321", "11223344556")
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
    fun `personsVeilederHasAccessTo returns filtered list when access is granted`() {
        val filteredPersonidenter = listOf("12345678910", "10987654321")
        val client = createMockClientForBatchResponse(filteredPersonidenter, HttpStatusCode.OK)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(filteredPersonidenter, result)
    }

    @Test
    fun `personsVeilederHasAccessTo returns full list when all persons are accessible`() {
        val client = createMockClientForBatchResponse(personIdenter, HttpStatusCode.OK)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(personIdenter, result)
    }

    @Test
    fun `personsVeilederHasAccessTo returns empty list when no persons are accessible`() {
        val client = createMockClientForBatchResponse(emptyList(), HttpStatusCode.OK)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `personsVeilederHasAccessTo returns null when access is forbidden (403)`() {
        val client = createMockClientForBatchResponse(null, HttpStatusCode.Forbidden)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        assertNull(result)
    }

    @Test
    fun `personsVeilederHasAccessTo returns null when server returns error (500)`() {
        val client = createMockClientForBatchResponse(null, HttpStatusCode.InternalServerError)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        assertNull(result)
    }

    @Test
    fun `personsVeilederHasAccessTo returns null when server returns bad request (400)`() {
        val client = createMockClientForBatchResponse(null, HttpStatusCode.BadRequest)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        assertNull(result)
    }

    @Test
    fun `personsVeilederHasAccessTo handles empty input list`() {
        val client = createMockClientForBatchResponse(emptyList(), HttpStatusCode.OK)

        val result = runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = emptyList(),
                token = token,
                callId = callId
            )
        }

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `personsVeilederHasAccessTo calls obo token exchange before making request`() {
        val client = createMockClientForBatchResponse(personIdenter, HttpStatusCode.OK)

        runBlocking {
            client.personsVeilederHasAccessTo(
                personIdenter = personIdenter,
                token = token,
                callId = callId
            )
        }

        io.mockk.coVerify {
            oboTokenProvider.getOnBehalfOfToken(
                scopeClientId = config.clientId,
                token = token
            )
        }
    }

    @Test
    fun `personsVeilederHasAccessTo throws when obo token request fails`() {
        coEvery {
            oboTokenProvider.getOnBehalfOfToken(any(), any())
        } returns null

        val client = createMockClientForBatchResponse(personIdenter, HttpStatusCode.OK)

        val exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException::class.java) {
            runBlocking {
                client.personsVeilederHasAccessTo(
                    personIdenter = personIdenter,
                    token = token,
                    callId = callId
                )
            }
        }

        assertTrue(exception.message?.contains("Failed to request access to list of persons") ?: false)
    }

    private fun createMockClientForBatchResponse(
        response: List<String>?,
        status: HttpStatusCode
    ): TilgangskontrollClient {
        val httpClient = HttpClient(MockEngine) {
            commonConfig()
            engine {
                addHandler {
                    if (status == HttpStatusCode.OK && response != null) {
                        respond(response, status)
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
