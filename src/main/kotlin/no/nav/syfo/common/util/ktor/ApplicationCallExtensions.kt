package no.nav.syfo.common.util.ktor

import com.auth0.jwt.JWT
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import no.nav.syfo.common.util.NAV_CALL_ID_HEADER
import no.nav.syfo.common.util.NAV_PERSONIDENT_HEADER

internal const val JWT_CLAIM_AZP = "azp"
internal const val JWT_CLAIM_NAVIDENT = "NAVident"

/** Returns the value of the `Nav-Call-Id` request header, used for distributed tracing. */
fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

/** Returns the value of the `nav-personident` request header, or null if not present. */
fun ApplicationCall.getPersonIdent(): String? = this.request.headers[NAV_PERSONIDENT_HEADER]

/**
 * Returns the `azp` (authorized party) claim from the incoming Bearer token, identifying the calling application.
 * Returns null if there is no Authorization header or if the claim is absent.
 */
fun ApplicationCall.getConsumerClientId(): String? =
    getBearerToken()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }

/**
 * Extracts the NAVident (veileder's employee ID) from the `NAVident` private claim in the incoming Bearer token.
 * Throws [Error] if there is no Authorization header or if the claim is missing.
 */
fun ApplicationCall.getNavIdent(): String {
    val token = getBearerToken() ?: throw Error("No Authorization header supplied")
    return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}

/**
 * Extracts the Bearer token from the `Authorization` header, stripping the `Bearer ` prefix.
 * Returns null if the header is absent.
 */
fun ApplicationCall.getBearerToken(): String? =
    this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
