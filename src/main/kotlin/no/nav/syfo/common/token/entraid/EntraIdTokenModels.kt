package no.nav.syfo.common.token.entraid

import com.fasterxml.jackson.annotation.JsonProperty

internal data class EntraIdTokenExchangeRequest(
    @JsonProperty("identity_provider") val identityProvider: String,
    val target: String,
    @JsonProperty("user_token") val userToken: String
)

internal data class EntraIdTokenRequest(
    @JsonProperty("identity_provider") val identityProvider: String,
    val target: String
)

internal data class EntraIdTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("token_type") val tokenType: String
)
