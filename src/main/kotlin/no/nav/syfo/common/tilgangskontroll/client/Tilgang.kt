package no.nav.syfo.common.tilgangskontroll.client

internal data class Tilgang(
    val erGodkjent: Boolean,
    val erAvslatt: Boolean = false,
    val fullTilgang: Boolean = false
)
