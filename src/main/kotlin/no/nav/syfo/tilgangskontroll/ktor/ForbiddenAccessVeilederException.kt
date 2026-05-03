package no.nav.syfo.tilgangskontroll.ktor

class ForbiddenAccessVeilederException(
    action: String,
    message: String = "Denied NAVIdent access to personident: $action"
) : RuntimeException(message)
