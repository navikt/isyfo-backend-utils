package no.nav.syfo.tilgang.ktor

import com.auth0.jwt.JWT
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.RoutingContext
import no.nav.syfo.tilgang.client.VeilederTilgangskontrollClient
import no.nav.syfo.tilgang.util.NAV_CALL_ID_HEADER
import no.nav.syfo.tilgang.util.NAV_PERSONIDENT_HEADER

internal const val JWT_CLAIM_AZP = "azp"
internal const val JWT_CLAIM_NAVIDENT = "NAVident"

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun ApplicationCall.getPersonIdent(): String? = this.request.headers[NAV_PERSONIDENT_HEADER]

fun ApplicationCall.getConsumerClientId(): String? =
    getBearerHeader()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }

fun ApplicationCall.getNAVIdent(): String {
    val token = getBearerHeader() ?: throw Error("No Authorization header supplied")
    return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}

fun ApplicationCall.getBearerHeader(): String? =
    this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")

suspend fun RoutingContext.checkVeilederTilgang(
    action: String,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    block: suspend () -> Unit
) {
    val personident = call.getPersonIdent()
        ?: throw IllegalArgumentException("Failed to $action: No $NAV_PERSONIDENT_HEADER supplied in request header")

    checkVeilederTilgang(
        action = action,
        personident = personident,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        requiresWriteAccess = requiresWriteAccess,
        block = block
    )
}

suspend fun RoutingContext.checkVeilederTilgang(
    action: String,
    personident: String,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    requiresWriteAccess: Boolean = false,
    block: suspend () -> Unit
) {
    val callId = call.getCallId()
    val token = call.getBearerHeader()
        ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasAccess = if (requiresWriteAccess) {
        veilederTilgangskontrollClient.hasWriteAccess(
            callId = callId,
            personIdent = personident,
            token = token
        )
    } else {
        veilederTilgangskontrollClient.hasAccess(
            callId = callId,
            personIdent = personident,
            token = token
        )
    }

    if (!hasAccess) {
        throw ForbiddenAccessVeilederException(action = action)
    } else {
        block()
    }
}
