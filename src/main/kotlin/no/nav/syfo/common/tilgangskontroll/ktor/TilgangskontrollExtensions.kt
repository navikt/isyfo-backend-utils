package no.nav.syfo.common.tilgangskontroll.ktor

import io.ktor.server.routing.RoutingContext
import no.nav.syfo.common.tilgangskontroll.client.TilgangskontrollClient
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.common.util.ktor.getBearerHeader
import no.nav.syfo.common.util.ktor.getCallId
import no.nav.syfo.common.util.ktor.getPersonident

suspend fun RoutingContext.checkVeilederTilgang(
    action: String,
    tilgangskontrollClient: TilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    block: suspend () -> Unit
) {
    val personident = call.getPersonident()
        ?: throw IllegalArgumentException("Failed to $action: No $NAV_PERSONIDENT_HEADER supplied in request header")

    checkVeilederTilgang(
        action = action,
        personident = personident,
        tilgangskontrollClient = tilgangskontrollClient,
        requiresWriteAccess = requiresWriteAccess,
        block = block
    )
}

suspend fun RoutingContext.checkVeilederTilgang(
    action: String,
    personident: String,
    tilgangskontrollClient: TilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    block: suspend () -> Unit
) {
    val callId = call.getCallId()
    val token = call.getBearerHeader()
        ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasAccess = if (requiresWriteAccess) {
        tilgangskontrollClient.hasWriteAccess(
            callId = callId,
            personident = personident,
            token = token
        )
    } else {
        tilgangskontrollClient.hasAccess(
            callId = callId,
            personident = personident,
            token = token
        )
    }

    if (!hasAccess) {
        throw VeilederTilgangBlokkertException(action = action)
    } else {
        block()
    }
}
