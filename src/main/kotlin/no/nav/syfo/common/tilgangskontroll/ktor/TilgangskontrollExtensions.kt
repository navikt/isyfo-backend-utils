package no.nav.syfo.common.tilgangskontroll.ktor

import io.ktor.server.routing.RoutingContext
import no.nav.syfo.common.tilgangskontroll.client.TilgangskontrollClient
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.common.util.ktor.getBearerToken
import no.nav.syfo.common.util.ktor.getCallId
import no.nav.syfo.common.util.ktor.getPersonIdent

/**
 * Reads the `Nav-Personident` header from the request, checks access via [tilgangskontrollClient],
 * and executes [block] if access is granted. Throws [VeilederTilgangForbiddenException] if denied.
 *
 * This overload reads the personIdent from the `Nav-Personident` request header automatically.
 *
 * @param action Short description of the action being performed, used in error messages.
 * @param tilgangskontrollClient Client used to check access.
 * @param requiresWriteAccess If true, checks for fullTilgang (write access) rather than read access.
 * @param block The handler to execute if access is granted.
 */
suspend fun RoutingContext.checkVeilederTilgang(
    action: String,
    tilgangskontrollClient: TilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    block: suspend () -> Unit
) {
    val personIdent = call.getPersonIdent()
        ?: throw IllegalArgumentException("Failed to $action: No $NAV_PERSONIDENT_HEADER supplied in request header")

    checkVeilederTilgang(
        action = action,
        personIdent = personIdent,
        tilgangskontrollClient = tilgangskontrollClient,
        requiresWriteAccess = requiresWriteAccess,
        block = block
    )
}

/**
 * Checks veileder access for an explicitly provided [personIdent], then executes [block] if granted.
 * Throws [VeilederTilgangForbiddenException] if denied.
 *
 * Use this overload when the personIdent comes from the request body rather than the `Nav-Personident` header.
 *
 * @param action Short description of the action being performed, used in error messages.
 * @param personIdent The person's national identity number to check access for.
 * @param tilgangskontrollClient Client used to check access.
 * @param requiresWriteAccess If true, checks for fullTilgang (write access) rather than read access.
 * @param block The handler to execute if access is granted.
 */
suspend fun RoutingContext.checkVeilederTilgang(
    action: String,
    personIdent: String,
    tilgangskontrollClient: TilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    block: suspend () -> Unit
) {
    val callId = call.getCallId()
    val token = call.getBearerToken()
        ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasAccess = if (requiresWriteAccess) {
        tilgangskontrollClient.hasWriteAccess(
            callId = callId,
            personIdent = personIdent,
            token = token
        )
    } else {
        tilgangskontrollClient.hasAccess(
            callId = callId,
            personIdent = personIdent,
            token = token
        )
    }

    if (!hasAccess) {
        throw VeilederTilgangForbiddenException(action = action)
    } else {
        block()
    }
}
