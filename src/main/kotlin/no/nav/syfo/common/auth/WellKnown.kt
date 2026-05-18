package no.nav.syfo.common.auth

/** OpenID Connect discovery document fields used for JWT validation. */
data class WellKnown(
    val issuer: String,
    val jwksUri: String
)
