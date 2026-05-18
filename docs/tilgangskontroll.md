# Veileder tilgangskontroll

Client for checking veileder access via `istilgangskontroll`.

## Access semantics

- `hasAccess(...)` returns `true` only when `erGodkjent == true`
- `hasWriteAccess(...)` returns `true` only when `erGodkjent == true && fullTilgang == true`
- Forbidden access from `istilgangskontroll` is treated as a normal access-denied outcome

## Setup

```kotlin
val tilgangskontrollClient = TilgangskontrollClient(
    oboTokenProvider = EntraIdClient(), // or AzureAdClient(...)
    config = TilgangskontrollClientConfig(
        baseUrl = "https://istilgangskontroll",
        clientId = "dev-fss.teamsykefravr.istilgangskontroll",
    ),
)
```

## Usage

### Using the Ktor convenience helper

`checkVeilederTilgang()` can wrap a route handler block — it extracts the token, calls the appropriate access check, and responds with `403 Forbidden` if access is denied.

Overload that reads personIdent from the `nav-personident` request header:

```kotlin
route("/api") {
    get("/person") {
        checkVeilederTilgang(
            action = "read person",
            veilederTilgangskontrollClient = tilgangskontrollClient,
        ) {
            call.respond(HttpStatusCode.OK)
        }
    }
}
```

Overload that takes personIdent as a parameter explicitly (e.g. when read from the request body):

```kotlin
route("/api") {
    post("/person") {
        val requestDTO = call.receive<RequestDTO>()
        checkVeilederTilgang(
            action = "write person",
            personIdent = requestDTO.personIdent,
            veilederTilgangskontrollClient = tilgangskontrollClient,
            requiresWriteAccess = true,
        ) {
            call.respond(HttpStatusCode.Created)
        }
    }
}
```

Required request headers when using the Ktor helper:
- `Authorization: Bearer <token>`
- `Nav-Call-Id`
- `nav-personident` (if not providing personident as argument)

### Check if veileder has read or write access to a person

```kotlin
val hasReadAccess = tilgangskontrollClient.hasAccess(
    callId = "call-id",
    personIdent = "12345678910",
    token = incomingToken,
)

val hasWriteAccess = tilgangskontrollClient.hasWriteAccess(
    callId = "call-id",
    personIdent = "12345678910",
    token = incomingToken,
)
```

### Check if veileder has access to multiple persons in one call

```kotlin
val accessiblePersonIdenter: List<String>? = tilgangskontrollClient.personsVeilederHasAccessTo(
    personIdenter = listOf("12345678910", "10987654321"),
    token = incomingToken,
    callId = "call-id",
)
```

