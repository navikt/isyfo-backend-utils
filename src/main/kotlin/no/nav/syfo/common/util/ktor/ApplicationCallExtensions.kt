package no.nav.syfo.common.util.ktor

import com.auth0.jwt.JWT
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import no.nav.syfo.common.util.NAV_CALL_ID_HEADER
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER

internal const val JWT_CLAIM_AZP = "azp"
internal const val JWT_CLAIM_NAVIDENT = "NAVident"

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun ApplicationCall.getPersonIdent(): String? = this.request.headers[NAV_PERSONIDENT_HEADER]

fun ApplicationCall.getConsumerClientId(): String? =
    getBearerToken()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }

fun ApplicationCall.getNavIdent(): String {
    val token = getBearerToken() ?: throw Error("No Authorization header supplied")
    return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}

fun ApplicationCall.getBearerToken(): String? =
    this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
