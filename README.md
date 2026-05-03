# isyfo-backend-utils

Shared Kotlin/JVM utility library for iSyfo backend services. Currently focused on veileder access control via `istilgangskontroll`, but intended to grow with other shared backend utilities over time.

## What it provides

### Veileder access control
- `AzureAdClient` — Azure AD token exchange (system tokens and OBO tokens)
- `VeilederTilgangskontrollClient` — read/write access checks against `istilgangskontroll`
- Ktor convenience helpers such as `checkVeilederTilgang(...)`

## Intended consumers

This library is intended for Kotlin/JVM services in the isyfo domain that run on NAV infrastructure. It has no opinion on web framework — non-Ktor apps can use `AzureAdClient` and `VeilederTilgangskontrollClient` directly. The Ktor helpers are opt-in.

## Adding the dependency

Add the following dependency coordinates to your `build.gradle.kts`:

```kotlin
implementation("no.nav.syfo:isyfo-backend-utils:0.0.3")
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

## Usage

### Setup (required)

Set up the client.

```kotlin
val azureEnvironment = AzureEnvironment(
    appClientId = "client-id",
    appClientSecret = "client-secret",
    appWellKnownUrl = "https://login.microsoftonline.com/<tenant>/v2.0/.well-known/openid-configuration",
    openidConfigTokenEndpoint = "https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token",
)

val azureAdClient = AzureAdClient(azureEnvironment = azureEnvironment)

val tilgangskontrollClient = VeilederTilgangskontrollClient(
    azureAdClient = azureAdClient,
    config = VeilederTilgangConfig(
        baseUrl = "https://istilgangskontroll",
        clientId = "dev-fss.teamsykefravr.istilgangskontroll",
    ),
)
```

### Option A: Check access directly

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

### Option B: Check batch access

```kotlin
val accessiblePersonidenter: List<String>? = tilgangskontrollClient.veilederPersonerAccess(
    personidenter = listOf("12345678910", "10987654321"),
    token = incomingToken,
    callId = "call-id",
)
```

### Option C: Use the Ktor convenience helper

The `checkVeilederTilgang()` Ktor helper can wrap any block of code that requires a specific access level. It can be used to wrap code in route handlers. It handles extracting the personident from the request header (if not provided as argument, see below), calling the appropriate access check method, and responding with `403 Forbidden` if access is denied.

Reading personident from the `nav-personident` request header:

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

Providing personident explicitly (e.g. when read from the request body):

```kotlin
route("/api") {
    post("/person") {
        val requestDTO = call.receive<RequestDTO>()
        checkVeilederTilgang(
            action = "write person",
            personident = requestDTO.personident,
            veilederTilgangskontrollClient = tilgangskontrollClient,
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
- `nav-personident` (if not providing personident as argument)

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

On-behalf-of (OBO) tokens obtained via `getOnBehalfOfToken()` are not cached. Each call results in a new token exchange with Azure AD. This is intentional — OBO tokens are user-scoped and short-lived, making caching add complexity without meaningful benefit.

## Development

### Releasing a new version

1. Bump `version` in `build.gradle.kts` and update the version in the `README.md` dependency coordinates
2. Commit and push to `main`
3. The [Publish workflow](.github/workflows/publish.yml) automatically runs lint, tests, and publishes the new version to GitHub Packages

### Local development

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

