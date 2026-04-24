package no.nav.syfo.tilgang.client

data class Tilgang(
    val erGodkjent: Boolean,
    val erAvslatt: Boolean = false,
    val fullTilgang: Boolean = false,
)

