package no.nav.syfo.tilgang.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.tilgang.azure.AzureAdClient
import no.nav.syfo.tilgang.azure.AzureAdToken
import no.nav.syfo.tilgang.http.commonConfig
import no.nav.syfo.tilgang.testhelper.respond
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Unit test for the batch access endpoint in VeilederTilgangskontrollClient.
 * Tests the veilederPersonerAccess() method with various scenarios including
 * successful responses, access denied, server errors, and empty lists.
 */
class VeilederPersonerAccessTest {
    private val token = "token"
    private val oboToken = "obo-token"
    private val callId = "call-id"
    private val personidenter = listOf("12345678910", "10987654321", "11223344556")
    private val config = VeilederTilgangConfig(
        baseUrl = "isTilgangskontrollUrl",
        clientId = "dev-fss.teamsykefravr.istilgangskontroll"
    )
    private val azureAdClient = mockk<AzureAdClient>()

    @BeforeEach
    fun setup() {
        coEvery {
            azureAdClient.getOnBehalfOfToken(any(), any())
        } returns AzureAdToken(
            accessToken = oboToken,
            expires = LocalDateTime.now().plusHours(1)
        )
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `veilederPersonerAccess returns filtered list when access is granted`() {
        val filteredPersonidenter = listOf("12345678910", "10987654321")
        val client = createMockClientForBatchResponse(filteredPersonidenter, HttpStatusCode.OK)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(filteredPersonidenter, result)
    }

    @Test
    fun `veilederPersonerAccess returns full list when all persons are accessible`() {
        val client = createMockClientForBatchResponse(personidenter, HttpStatusCode.OK)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(personidenter, result)
    }

    @Test
    fun `veilederPersonerAccess returns empty list when no persons are accessible`() {
        val client = createMockClientForBatchResponse(emptyList(), HttpStatusCode.OK)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `veilederPersonerAccess returns null when access is forbidden (403)`() {
        val client = createMockClientForBatchResponse(null, HttpStatusCode.Forbidden)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        assertNull(result)
    }

    @Test
    fun `veilederPersonerAccess returns null when server returns error (500)`() {
        val client = createMockClientForBatchResponse(null, HttpStatusCode.InternalServerError)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        assertNull(result)
    }

    @Test
    fun `veilederPersonerAccess returns null when server returns bad request (400)`() {
        val client = createMockClientForBatchResponse(null, HttpStatusCode.BadRequest)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        assertNull(result)
    }

    @Test
    fun `veilederPersonerAccess handles empty input list`() {
        val client = createMockClientForBatchResponse(emptyList(), HttpStatusCode.OK)

        val result = runBlocking {
            client.veilederPersonerAccess(
                personidenter = emptyList(),
                token = token,
                callId = callId
            )
        }

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `veilederPersonerAccess calls obo token exchange before making request`() {
        val client = createMockClientForBatchResponse(personidenter, HttpStatusCode.OK)

        runBlocking {
            client.veilederPersonerAccess(
                personidenter = personidenter,
                token = token,
                callId = callId
            )
        }

        io.mockk.coVerify {
            azureAdClient.getOnBehalfOfToken(
                scopeClientId = config.clientId,
                token = token
            )
        }
    }

    @Test
    fun `veilederPersonerAccess throws when obo token request fails`() {
        coEvery {
            azureAdClient.getOnBehalfOfToken(any(), any())
        } returns null

        val client = createMockClientForBatchResponse(personidenter, HttpStatusCode.OK)

        val exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException::class.java) {
            runBlocking {
                client.veilederPersonerAccess(
                    personidenter = personidenter,
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
    ): VeilederTilgangskontrollClient {
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

        return VeilederTilgangskontrollClient(
            azureAdClient = azureAdClient,
            config = config,
            httpClient = httpClient
        )
    }
}
