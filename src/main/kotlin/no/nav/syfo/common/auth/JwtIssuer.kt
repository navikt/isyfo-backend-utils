package no.nav.syfo.common.auth

/**
 * Describes a trusted JWT issuer used to configure Ktor's JWT authentication.
 *
 * @param acceptedAudienceList The audience values accepted for tokens from this issuer.
 * @param jwtIssuerType A named identifier for this issuer, used as the Ktor authentication provider name.
 * @param wellKnown The OpenID Connect discovery document for this issuer.
 */
data class JwtIssuer(
    val acceptedAudienceList: List<String>,
    val jwtIssuerType: JwtIssuerType,
    val wellKnown: WellKnown
)

/** Named identifiers for supported JWT issuers. Used as Ktor authentication provider names. */
enum class JwtIssuerType {
    INTERNAL_AZUREAD
}
