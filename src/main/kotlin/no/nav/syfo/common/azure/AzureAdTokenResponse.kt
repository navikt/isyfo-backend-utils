package no.nav.syfo.common.azure

import java.time.LocalDateTime

internal data class AzureAdTokenResponse(
    val access_token: String,
    val expires_in: Long,
    val token_type: String
)

internal fun AzureAdTokenResponse.toAzureAdToken(): AzureAdToken {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return AzureAdToken(
        accessToken = this.access_token,
        expires = expiresOn
    )
}
