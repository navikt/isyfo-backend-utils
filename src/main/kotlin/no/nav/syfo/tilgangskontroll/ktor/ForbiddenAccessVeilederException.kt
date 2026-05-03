package no.nav.syfo.tilgangskontroll.ktor

class ForbiddenAccessVeilederException(
    action: String,
    message: String = "Denied NAVIdent access to personIdent: $action"
) : RuntimeException(message)
