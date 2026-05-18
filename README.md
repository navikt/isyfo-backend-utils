# isyfo-backend-common

Shared Kotlin/JVM utility library for isyfo backend services. Intended to grow with shared backend utilities over time.

Some helpers are for Ktor apps, while non-Ktor apps can use parts of the library like `EntraIdClient` and `TilgangskontrollClient`.

## What it provides

### Token providers
- `EntraIdClient` — acquires OBO and system tokens via the Nais token exchange sidecar (Texas) (recommended)
- `AzureAdClient` — acquires OBO and system tokens directly from Azure AD (for apps not yet on the Nais token sidecar (Texas))
- Both implement `OboTokenProvider`, making them interchangeable as a dependency for `TilgangskontrollClient`

### Veileder access control
- `TilgangskontrollClient` — read/write access checks against `istilgangskontroll`
- Ktor convenience helpers such as `checkVeilederTilgang(...)`

### JWT authentication
- `installJwtAuthentication()` — Ktor plugin for validating incoming Azure AD JWTs
- `getWellKnown()` — fetches the OpenID Connect discovery document at startup
- `JwtIssuer` / `JwtIssuerType` — configuration types for JWT issuers

## Adding the dependency in consumer apps

In the consumer app, add the following dependency coordinates to `build.gradle.kts`:

```kotlin
implementation("no.nav.syfo:isyfo-backend-common:<version>")
```

Also add the GitHub Packages repository so Gradle knows where to fetch it from, and provide credentials for reading packages from GitHub, for example:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/isyfo-backend-common")
        credentials {
            username = project.findProperty("githubUser") as String? ?: "x-access-token"
            password = project.findProperty("githubPassword") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

In CI, the `kotlin-build-deploy` workflow from `isworkflows` sets the environment variables `ORG_GRADLE_PROJECT_githubUser` and `ORG_GRADLE_PROJECT_githubPassword`, which makes Gradle set the corresponding Gradle project properties referenced above. In CI workflows the `GITHUB_TOKEN` is also automatically provided by GitHub Actions.

Locally, you can set the env var `GITHUB_TOKEN` (or `ORG_GRADLE_PROJECT_githubPassord`) to a GitHub personal access token with the `read:packages` scope. (You can use the same that you use with `NPM_AUTH_TOKEN` in frontend projects.) This will allow you to run Gradle tasks that need to fetch the library in consuming apps.

### Notes

- The library depends only on the SLF4J API; consuming applications should provide their own logging backend.

---

## Development

### Releasing a new version

1. Bump `version` in `build.gradle.kts`
2. Update changelog.
3. Merge PR with the changes to `main`.
4. Trigger the [Publish workflow](.github/workflows/publish.yml) manually from the GitHub Actions UI (`Run workflow`)

### Local development

Run the main validation steps locally:

```bash
./gradlew clean test
./gradlew ktlintCheck
./gradlew jar
```

#### Testing changes in a consumer app without publishing

Use `publishToMavenLocal` to install the library into your local Maven cache (`~/.m2`), then reference it from the consumer app without going through GitHub Packages:

```bash
# In this repo — publish current state to local cache
./gradlew publishToMavenLocal
```

In the consumer's `build.gradle.kts`, add `mavenLocal()` **first** in the repositories block so it takes precedence over GitHub Packages:

```kotlin
repositories {
    mavenLocal()  // picks up locally published version
    maven {
        url = uri("https://maven.pkg.github.com/navikt/isyfo-backend-common")
        credentials {
            username = project.findProperty("githubUser") as String? ?: "x-access-token"
            password = project.findProperty("githubPassword") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Remember to remove `mavenLocal()` before merging — it should not be in the final build configuration.

---

## EntraIdClient (Texas)

Uses the Nais token exchange sidecar (Texas) — no client credentials needed in the app. Texas handles caching, renewal, and communication with Entra ID.

### Setup

```kotlin
val entraIdClient = EntraIdClient()
// Reads NAIS_TOKEN_EXCHANGE_ENDPOINT and NAIS_TOKEN_ENDPOINT from environment automatically
```

### On-behalf-of tokens

```kotlin
val token = entraIdClient.getOnBehalfOfToken(
    scopeClientId = "api://<cluster>.<namespace>.<app>/.default",
    token = incomingToken,
)
```

### System tokens (M2M)

```kotlin
val token = entraIdClient.getSystemToken(
    scopeClientId = "api://<cluster>.<namespace>.<app>/.default",
)
```

### Migrating from AzureAdClient

Switching to `EntraIdClient` lets you simplify consuming apps:

- **App code**: Remove the `AzureEnvironment` config and all reading of `AZURE_APP_CLIENT_ID`, `AZURE_APP_CLIENT_SECRET`, `AZURE_APP_WELL_KNOWN_URL`. Replace `AzureAdClient(azureEnvironment)` with `EntraIdClient()` — no arguments needed.
- **Naiserator**: Remove `outbound.external: login.microsoftonline.com` — the app no longer calls Azure AD directly. Keep `azure.application.enabled: true`; the Texas sidecar still uses the app's Azure AD registration and injects `NAIS_TOKEN_EXCHANGE_ENDPOINT` and `NAIS_TOKEN_ENDPOINT` automatically.

---

## AzureAdClient

For apps not yet using the Nais token sidecar (Texas). Communicates directly with Azure AD. System tokens are cached in-memory per scope (refreshed 60 seconds before expiry). OBO tokens are not cached.

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

```kotlin
val token = azureAdClient.getSystemToken(scopeClientId = "api://my-service/.default")
```

### On-behalf-of tokens

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
val tilgangskontrollClient = TilgangskontrollClient(
    oboTokenProvider = EntraIdClient(), // or AzureAdClient(...)
    config = TilgangskontrollClientConfig(
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
val accessiblePersonIdenter: List<String>? = tilgangskontrollClient.personsVeilederHasAccessTo(
    personIdenter = listOf("12345678910", "10987654321"),
    token = incomingToken,
    callId = "call-id",
)
```

### Option C: Use the Ktor convenience helper

`checkVeilederTilgang()` wraps a route handler block — it extracts the token, calls the appropriate access check, and responds with `403 Forbidden` if access is denied.

Reading personIdent from the `nav-personident` request header:

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

Providing personIdent explicitly (e.g. when read from the request body):

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

---

## JWT authentication

### Setup

```kotlin
val wellKnown = getWellKnown(wellKnownUrl = environment.azure.appWellKnownUrl)

application.installJwtAuthentication(
    jwtIssuerList = listOf(
        JwtIssuer(
            acceptedAudienceList = listOf(environment.azure.appClientId),
            jwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
            wellKnown = wellKnown,
        )
    )
)
```

Routes are then protected using the Ktor `authenticate` block with the issuer type name:

```kotlin
authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
    get("/api/person") { ... }
}
```

---

