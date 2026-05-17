package no.nav.syfo.common.token

fun interface OboTokenProvider {
    suspend fun getOnBehalfOfToken(scopeClientId: String, token: String): String?
}
