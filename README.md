# isyfo-backend-utils

Shared Kotlin/JVM library for checking veileder access to persons through `istilgangskontroll`.

## What it provides

The library includes:
- `AzureAdClient` for Azure AD token exchange
- `VeilederTilgangskontrollClient` for read/write access checks against `istilgangskontroll`
- Ktor convenience helpers such as `checkVeilederTilgang(...)`

## Intended consumers

This library is intended for Kotlin/JVM services that:
- run on NAV infrastructure
- use Ktor on the server side
- need to call `istilgangskontroll`
- already manage their own logging backend

## Dependency coordinates

```kotlin
implementation("no.nav.syfo:isyfo-backend-utils:0.0.2")
```

## Public API

Primary entry points:
- `AzureEnvironment`
- `AzureAdClient`
- `VeilederTilgangConfig`
- `VeilederTilgangskontrollClient`
- Ktor helpers in `no.nav.syfo.tilgang.ktor`

### Access semantics

- `hasAccess(...)` returns `true` only when `erGodkjent == true`
- `hasWriteAccess(...)` returns `true` only when `erGodkjent == true && fullTilgang == true`
- forbidden access from `istilgangskontroll` is treated as a normal access-denied outcome

## Minimal usage

### 1. Create Azure AD client

```kotlin
val azureEnvironment = AzureEnvironment(
    appClientId = "client-id",
    appClientSecret = "client-secret",
    appWellKnownUrl = "https://login.microsoftonline.com/<tenant>/v2.0/.well-known/openid-configuration",
    openidConfigTokenEndpoint = "https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token",
)

val azureAdClient = AzureAdClient(azureEnvironment = azureEnvironment)
```

### 2. Create tilgang client

```kotlin
val tilgangClient = VeilederTilgangskontrollClient(
    azureAdClient = azureAdClient,
    config = VeilederTilgangConfig(
        baseUrl = "https://istilgangskontroll",
        clientId = "dev-fss.teamsykefravr.istilgangskontroll",
    ),
)
```

### 3. Check access directly

```kotlin
val hasReadAccess = tilgangClient.hasAccess(
    callId = "call-id",
    personIdent = "12345678910",
    token = incomingToken,
)

val hasWriteAccess = tilgangClient.hasWriteAccess(
    callId = "call-id",
    personIdent = "12345678910",
    token = incomingToken,
)
```

### 4. Check batch access

```kotlin
val accessiblePersonidenter: List<String>? = tilgangClient.veilederPersonerAccess(
    personidenter = listOf("12345678910", "10987654321"),
    token = incomingToken,
    callId = "call-id",
)
```

### 5. Use the Ktor convenience helper

Reading personident from the `nav-personident` request header:

```kotlin
route("/api") {
    get("/person") {
        checkVeilederTilgang(
            action = "read person",
            veilederTilgangskontrollClient = tilgangClient,
        ) {
            call.respond(HttpStatusCode.OK)
        }
    }
}
```

Providing personident explicitly (e.g. when read from the request body):

```kotlin
route("/api") {
    post("/person") {
        val requestDTO = call.receive<RequestDTO>()
        checkVeilederTilgang(
            action = "write person",
            personident = requestDTO.personident,
            veilederTilgangskontrollClient = tilgangClient,
            requiresWriteAccess = true,
        ) {
            call.respond(HttpStatusCode.Created)
        }
    }
}
```

Set these request headers before using the Ktor helper:
- `Authorization: Bearer <token>`
- `Nav-Call-Id`
- `nav-personident`

## Token Management

### System Token Caching

`AzureAdClient` automatically caches system tokens obtained via the `getSystemToken()` method to reduce unnecessary calls to Azure AD. This is useful when your service needs to authenticate with other services.

**How it works:**
- Tokens are cached per scope (client ID)
- A cached token is reused if it's still valid
- Tokens are considered expired when less than 60 seconds of lifetime remain
- Cache is stored in memory; it does not persist across application restarts

**Example:**
```kotlin
// First call fetches from Azure AD
val token1 = azureAdClient.getSystemToken(scopeClientId = "api://my-service/.default")

// Second call reuses cached token (no Azure AD call)
val token2 = azureAdClient.getSystemToken(scopeClientId = "api://my-service/.default")
```

### On-Behalf-Of Tokens

On-behalf-of (OBO) tokens obtained via `getOnBehalfOfToken()` are not cached. Each call results in a new token exchange with Azure AD, ensuring that access control changes (e.g., revoked permissions) are reflected immediately.

## Development

Run the main validation steps locally:

```bash
./gradlew clean test
./gradlew ktlintCheck
./gradlew jar
./gradlew publishToMavenLocal
```

## Notes

- The library depends only on the SLF4J API; consuming applications should provide their own logging backend.
- The bundled Ktor helpers are convenience APIs. If needed later, the Ktor integration can be split into a separate module.

