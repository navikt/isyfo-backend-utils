package no.nav.syfo.common.tilgangskontroll.ktor

class VeilederTilgangForbiddenException(
    action: String,
    message: String = "Denied NAVIdent access to personident: $action"
) : RuntimeException(message)
