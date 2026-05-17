package no.nav.syfo.common.token.azuread

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics

private const val CALL_AZUREAD_SYSTEM_TOKEN_CACHE_BASE = "call_azuread_system_token_cache"

val COUNT_CALL_AZUREAD_SYSTEM_TOKEN_CACHE_HIT: Counter = Counter.builder("${CALL_AZUREAD_SYSTEM_TOKEN_CACHE_BASE}_hit_count")
    .description("Counts the number of cache hits for calls to Azure AD for a system token")
    .register(Metrics.globalRegistry)

val COUNT_CALL_AZUREAD_SYSTEM_TOKEN_CACHE_MISS: Counter = Counter.builder("${CALL_AZUREAD_SYSTEM_TOKEN_CACHE_BASE}_miss_count")
    .description("Counts the number of cache misses for calls to Azure AD for a system token")
    .register(Metrics.globalRegistry)
