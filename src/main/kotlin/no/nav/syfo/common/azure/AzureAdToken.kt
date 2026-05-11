package no.nav.syfo.common.azure

import java.io.Serializable
import java.time.LocalDateTime

data class AzureAdToken(
    val accessToken: String,
    val expires: LocalDateTime
) : Serializable

internal fun AzureAdToken.isExpired() = this.expires < LocalDateTime.now().plusSeconds(60)
