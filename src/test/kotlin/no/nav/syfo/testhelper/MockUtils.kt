package no.nav.syfo.testhelper

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.syfo.util.configuredJacksonMapper

val mapper = configuredJacksonMapper()

fun <T> MockRequestHandleScope.respond(body: T, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(
        mapper.writeValueAsString(body),
        statusCode,
        headersOf(HttpHeaders.ContentType, "application/json")
    )

suspend inline fun <reified T> HttpRequestData.receiveBody(): T =
    mapper.readValue(body.toByteArray(), T::class.java)
