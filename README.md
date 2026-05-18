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

---

## Instrumentation

### Logging

The library uses SLF4J for logging and does not depend on any specific logging backend. Consuming apps own the binding (e.g. Logback). Structured log arguments use `logstash-logback-encoder`'s `StructuredArguments` — these are emitted as top-level JSON fields when the logstash encoder is active, making them queryable by field name in Kibana/Loki.

### Metrics

The library registers Micrometer counters on `Metrics.globalRegistry`. For these counters to appear in Prometheus scraping, the consuming app's `PrometheusMeterRegistry` must be wired into the global registry early in startup:

```kotlin
Metrics.addRegistry(METRICS_REGISTRY)
```

Without this, library counters will not be visible at the `/metrics` endpoint and will not be scraped.

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

## Package documentation

- [Token providers](docs/token.md) — `EntraIdClient` (Texas) and `AzureAdClient`
- [Veileder tilgangskontroll](docs/tilgangskontroll.md) — `TilgangskontrollClient` and Ktor helpers
- [JWT authentication](docs/auth.md) — `installJwtAuthentication`, `getWellKnown`, `JwtIssuer`
