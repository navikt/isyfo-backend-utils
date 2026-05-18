package no.nav.syfo.common.auth

internal data class WellKnownDTO(
    val authorization_endpoint: String,
    val issuer: String,
    val jwks_uri: String,
    val token_endpoint: String
)

internal fun WellKnownDTO.toWellKnown() = WellKnown(
    issuer = this.issuer,
    jwksUri = this.jwks_uri
)
