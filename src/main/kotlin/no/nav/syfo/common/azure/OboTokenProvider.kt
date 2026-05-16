package no.nav.syfo.common.azure

fun interface OboTokenProvider {
    suspend fun getOnBehalfOfToken(scopeClientId: String, token: String): String?
}
