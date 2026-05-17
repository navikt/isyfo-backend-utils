package no.nav.syfo.common.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Creates a pre-configured [ObjectMapper] instance with common isyfo Jackson settings applied.
 *
 * Equivalent to `jacksonObjectMapper().applyCommonJacksonConfig()`.
 * Use this when you need a standalone mapper, e.g. for serializing/deserializing in non-Ktor contexts.
 */
fun configuredJacksonMapper() = jacksonObjectMapper().applyCommonJacksonConfig()

/**
 * Applies common isyfo Jackson configuration to this [ObjectMapper]:
 * - Registers [JavaTimeModule] for Java 8 date/time type support (e.g. [java.time.LocalDateTime])
 * - Ignores unknown JSON properties on deserialization
 * - Serializes dates as ISO-8601 strings rather than timestamps
 */
fun ObjectMapper.applyCommonJacksonConfig() = this.apply {
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
