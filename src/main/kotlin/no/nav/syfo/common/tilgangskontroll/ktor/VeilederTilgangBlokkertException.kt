package no.nav.syfo.common.tilgangskontroll.ktor

class VeilederTilgangBlokkertException(
    action: String,
    message: String = "Denied NAVIdent access to personident: $action"
) : RuntimeException(message)
