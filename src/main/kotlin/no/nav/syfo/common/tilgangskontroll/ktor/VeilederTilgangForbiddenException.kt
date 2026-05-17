package no.nav.syfo.common.tilgangskontroll.ktor

/**
 * Thrown when a veileder is denied access to a person by istilgangskontroll.
 * Typically caught by a Ktor status page handler and mapped to a 403 response.
 */
class VeilederTilgangForbiddenException(
    action: String,
    message: String = "Denied NAVIdent access to personident: $action"
) : RuntimeException(message)
