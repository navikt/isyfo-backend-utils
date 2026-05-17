package no.nav.syfo.common.tilgangskontroll.client

/**
 * Configuration for [TilgangskontrollClient].
 *
 * @param baseUrl Base URL of the istilgangskontroll service (e.g. `http://istilgangskontroll`).
 * @param clientId Nais scope identifier for istilgangskontroll, used when requesting an OBO token
 * (e.g. `dev-fss.teamsykefravr.istilgangskontroll`). Nais resolves this alias to the actual Azure AD client ID.
 */
data class TilgangskontrollClientConfig(
    val baseUrl: String,
    val clientId: String
)
