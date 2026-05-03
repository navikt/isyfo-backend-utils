# isyfo-backend-utils

Shared Kotlin/JVM utility library for isyfo backend services. Intended to grow with shared backend utilities over time.

The library has no opinion on web framework — non-Ktor apps can use `AzureAdClient` and `VeilederTilgangskontrollClient` directly. The Ktor helpers are opt-in.

## What it provides

### Azure AD
- `AzureAdClient` — Azure AD token exchange (system tokens and OBO tokens), usable with any downstream service

### Veileder access control
- `VeilederTilgangskontrollClient` — read/write access checks against `istilgangskontroll`
- Ktor convenience helpers such as `checkVeilederTilgang(...)`

## Adding the dependency

Add the following dependency coordinates to your `build.gradle.kts`:

```kotlin
implementation("no.nav.syfo:isyfo-backend-utils:0.0.4")
```

---

## AzureAdClient

### Setup

```kotlin
val azureEnvironment = AzureEnvironment(
    appClientId = "client-id",
    appClientSecret = "client-secret",
    appWellKnownUrl = "https://login.microsoftonline.com/<tenant>/v2.0/.well-known/openid-configuration",
    openidConfigTokenEndpoint = "https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token",
)

val azureAdClient = AzureAdClient(azureEnvironment = azureEnvironment)
```

### System tokens

System tokens are cached per scope to reduce unnecessary calls to Azure AD:
- A cached token is reused if it's still valid
- Tokens are considered expired when less than 60 seconds of lifetime remain
- Cache is in memory and does not persist across restarts

```kotlin
val token = azureAdClient.getSystemToken(scopeClientId = "api://my-service/.default")
```

### On-behalf-of tokens

OBO tokens are not cached — each call results in a new token exchange. This is intentional: OBO tokens are user-scoped and short-lived, making caching add complexity without meaningful benefit.

```kotlin
val token = azureAdClient.getOnBehalfOfToken(
    scopeClientId = "api://my-service/.default",
    token = incomingToken,
)
```

---

## Veileder tilgangskontroll

### Access semantics

- `hasAccess(...)` returns `true` only when `erGodkjent == true`
- `hasWriteAccess(...)` returns `true` only when `erGodkjent == true && fullTilgang == true`
- Forbidden access from `istilgangskontroll` is treated as a normal access-denied outcome

### Setup

```kotlin
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

`checkVeilederTilgang()` wraps a route handler block — it extracts the token, calls the appropriate access check, and responds with `403 Forbidden` if access is denied.

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

Required request headers when using the Ktor helper:
- `Authorization: Bearer <token>`
- `Nav-Call-Id`
- `nav-personident` (if not providing personident as argument)

---

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

