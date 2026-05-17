package no.nav.syfo.common.tilgangskontroll.client

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics

private const val CALL_TILGANGSKONTROLL_PERSON_BASE = "call_tilgangskontroll_person"

val COUNT_CALL_TILGANGSKONTROLL_PERSON_SUCCESS: Counter = Counter.builder("${CALL_TILGANGSKONTROLL_PERSON_BASE}_success_count")
    .description("Counts the number of successful calls to istilgangskontroll - person")
    .register(Metrics.globalRegistry)

val COUNT_CALL_TILGANGSKONTROLL_PERSON_FAIL: Counter = Counter.builder("${CALL_TILGANGSKONTROLL_PERSON_BASE}_fail_count")
    .description("Counts the number of failed calls to istilgangskontroll - person")
    .register(Metrics.globalRegistry)

val COUNT_CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN: Counter = Counter.builder("${CALL_TILGANGSKONTROLL_PERSON_BASE}_forbidden_count")
    .description("Counts the number of forbidden calls to istilgangskontroll - person")
    .register(Metrics.globalRegistry)
