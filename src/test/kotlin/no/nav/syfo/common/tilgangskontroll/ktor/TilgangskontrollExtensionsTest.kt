package no.nav.syfo.common.tilgangskontroll.ktor

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.common.tilgangskontroll.client.TilgangskontrollClient
import no.nav.syfo.common.util.NAV_CALL_ID_HEADER
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.common.util.bearerHeader
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TilgangskontrollExtensionsTest {
    private val action = "read aktivitetskrav"
    private val callId = "123"
    private val personident = "12345678910"
    private val token = "token"

    private val tilgangskontrollClient = mockk<TilgangskontrollClient>()

    @Test
    fun `calls hasAccess and executes block when read access is granted`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(NAV_PERSONIDENT_HEADER, personident)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )
        var blockCalled = false

        coEvery {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        } returns true

        runBlocking {
            routingContext.checkVeilederTilgang(
                action = action,
                tilgangskontrollClient = tilgangskontrollClient
            ) {
                blockCalled = true
            }
        }

        assertTrue(blockCalled)
        coVerify(exactly = 1) {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        }
        coVerify(exactly = 0) {
            tilgangskontrollClient.hasWriteAccess(any(), any(), any())
        }
    }

    @Test
    fun `calls hasWriteAccess and executes block when write access is granted`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(NAV_PERSONIDENT_HEADER, personident)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )
        var blockCalled = false

        coEvery {
            tilgangskontrollClient.hasWriteAccess(callId, personident, token)
        } returns true

        runBlocking {
            routingContext.checkVeilederTilgang(
                action = action,
                tilgangskontrollClient = tilgangskontrollClient,
                requiresWriteAccess = true
            ) {
                blockCalled = true
            }
        }

        assertTrue(blockCalled)
        coVerify(exactly = 1) {
            tilgangskontrollClient.hasWriteAccess(callId, personident, token)
        }
        coVerify(exactly = 0) {
            tilgangskontrollClient.hasAccess(any(), any(), any())
        }
    }

    @Test
    fun `throws forbidden and does not execute block when read access is denied`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(NAV_PERSONIDENT_HEADER, personident)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )
        var blockCalled = false

        coEvery {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        } returns false

        assertThrows(VeilederTilgangForbiddenException::class.java) {
            runBlocking {
                routingContext.checkVeilederTilgang(
                    action = action,
                    tilgangskontrollClient = tilgangskontrollClient
                ) {
                    blockCalled = true
                }
            }
        }

        assertFalse(blockCalled)
        coVerify(exactly = 1) {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        }
    }

    @Test
    fun `throws illegal argument when authorization header is missing`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(NAV_PERSONIDENT_HEADER, personident)
            }
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                routingContext.checkVeilederTilgang(
                    action = action,
                    tilgangskontrollClient = tilgangskontrollClient
                ) {}
            }
        }

        coVerify(exactly = 0) {
            tilgangskontrollClient.hasAccess(any(), any(), any())
        }
        coVerify(exactly = 0) {
            tilgangskontrollClient.hasWriteAccess(any(), any(), any())
        }
    }

    @Test
    fun `throws illegal argument when personident header is missing`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                routingContext.checkVeilederTilgang(
                    action = action,
                    tilgangskontrollClient = tilgangskontrollClient
                ) {}
            }
        }

        coVerify(exactly = 0) {
            tilgangskontrollClient.hasAccess(any(), any(), any())
        }
        coVerify(exactly = 0) {
            tilgangskontrollClient.hasWriteAccess(any(), any(), any())
        }
    }

    @Test
    fun `explicit personident - calls hasAccess and executes block when read access is granted`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )
        var blockCalled = false

        coEvery {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        } returns true

        runBlocking {
            routingContext.checkVeilederTilgang(
                action = action,
                personident = personident,
                tilgangskontrollClient = tilgangskontrollClient
            ) {
                blockCalled = true
            }
        }

        assertTrue(blockCalled)
        coVerify(exactly = 1) {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        }
    }

    @Test
    fun `explicit personident - calls hasWriteAccess and executes block when write access is granted`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )
        var blockCalled = false

        coEvery {
            tilgangskontrollClient.hasWriteAccess(callId, personident, token)
        } returns true

        runBlocking {
            routingContext.checkVeilederTilgang(
                action = action,
                personident = personident,
                tilgangskontrollClient = tilgangskontrollClient,
                requiresWriteAccess = true
            ) {
                blockCalled = true
            }
        }

        assertTrue(blockCalled)
        coVerify(exactly = 1) {
            tilgangskontrollClient.hasWriteAccess(callId, personident, token)
        }
    }

    @Test
    fun `explicit personident - throws forbidden when access is denied`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
                append(HttpHeaders.Authorization, bearerHeader(token))
            }
        )
        var blockCalled = false

        coEvery {
            tilgangskontrollClient.hasAccess(callId, personident, token)
        } returns false

        assertThrows(VeilederTilgangForbiddenException::class.java) {
            runBlocking {
                routingContext.checkVeilederTilgang(
                    action = action,
                    personident = personident,
                    tilgangskontrollClient = tilgangskontrollClient
                ) {
                    blockCalled = true
                }
            }
        }

        assertFalse(blockCalled)
    }

    @Test
    fun `explicit personident - throws illegal argument when authorization header is missing`() {
        val routingContext = routingContextWithHeaders(
            headers = Headers.build {
                append(NAV_CALL_ID_HEADER, callId)
            }
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                routingContext.checkVeilederTilgang(
                    action = action,
                    personident = personident,
                    tilgangskontrollClient = tilgangskontrollClient
                ) {}
            }
        }

        coVerify(exactly = 0) {
            tilgangskontrollClient.hasAccess(any(), any(), any())
        }
    }

    private fun routingContextWithHeaders(headers: Headers): RoutingContext {
        val routingRequest = mockk<RoutingRequest>()
        every { routingRequest.headers } returns headers

        val routingCall = mockk<RoutingCall>()
        every { routingCall.request } returns routingRequest

        val routingContext = mockk<RoutingContext>()
        every { routingContext.call } returns routingCall
        return routingContext
    }
}
