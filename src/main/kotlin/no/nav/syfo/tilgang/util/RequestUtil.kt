package no.nav.syfo.tilgang.util

internal const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
internal const val NAV_PERSONIDENT_HEADER = "nav-personident"

internal fun bearerHeader(token: String) = "Bearer $token"
