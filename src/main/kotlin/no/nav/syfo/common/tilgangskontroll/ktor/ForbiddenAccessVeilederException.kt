package no.nav.syfo.common.tilgangskontroll.ktor

class ForbiddenAccessVeilederException(
    action: String,
    message: String = "Denied NAVIdent access to personident: $action"
) : RuntimeException(message)
