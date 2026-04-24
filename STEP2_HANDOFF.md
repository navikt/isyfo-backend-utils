# Step 2 handoff for `isyfo-check-veiledertilgang-util`

This file is the pickup point for continuing the library work in the new repo.

## Current status

Step 1 is done:
- the repo was scaffolded
- Gradle build files and wrapper were added
- initial source files were copied/adapted into the library
- CI workflows were added
- tests previously passed in the new repo

The extracted library currently includes these main areas:
- `src/main/kotlin/no/nav/syfo/tilgang/azure/`
- `src/main/kotlin/no/nav/syfo/tilgang/client/`
- `src/main/kotlin/no/nav/syfo/tilgang/http/`
- `src/main/kotlin/no/nav/syfo/tilgang/ktor/`
- `src/main/kotlin/no/nav/syfo/tilgang/util/`

## Goal for Step 2

Turn the current scaffold into a cleaner, publishable shared library by:
- tightening the public API
- removing app-specific coupling where practical
- simplifying dependencies
- adding repo metadata and docs
- validating with test/lint/local publish

---

## Step 2 checklist

- [ ] Review and clean `build.gradle.kts`
- [ ] Decide final public API for `VeilederTilgangskontrollClient`
- [ ] Reduce app-specific coupling in helpers and config types
- [ ] Decide what should stay public vs `internal`
- [ ] Add `.gitignore`
- [ ] Add `README.md`
- [ ] Add publishing metadata if needed
- [ ] Run `test`, `ktlintCheck`, and `publishToMavenLocal`

---

## Files to review first

### Build/config
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `.github/workflows/build.yml`
- `.github/workflows/publish.yml`

### Main source
- `src/main/kotlin/no/nav/syfo/tilgang/client/VeilederTilgangskontrollClient.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/client/Tilgang.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/client/VeilederTilgangConfig.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/azure/AzureAdClient.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/azure/AzureAdToken.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/azure/AzureAdTokenResponse.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/azure/AzureEnvironment.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/http/HttpClientCommon.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/ktor/PipelineUtil.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/ktor/ForbiddenAccessVeilederException.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/util/RequestUtil.kt`
- `src/main/kotlin/no/nav/syfo/tilgang/util/ObjectMapperConfig.kt`

### Tests
- `src/test/kotlin/no/nav/syfo/tilgang/client/VeilederTilgangskontrollClientTest.kt`
- `src/test/kotlin/no/nav/syfo/tilgang/ktor/PipelineUtilTest.kt`
- `src/test/kotlin/no/nav/syfo/tilgang/testhelper/MockUtils.kt`

---

## Recommended decisions for Step 2

### 1. Public API shape

Recommended public entry points:
- `VeilederTilgangskontrollClient`
- `VeilederTilgangConfig`
- `AzureAdClient`
- `AzureEnvironment`
- `Tilgang` only if consumers need raw response semantics

Recommended public methods on `VeilederTilgangskontrollClient`:
- `hasAccess(callId: String, personIdent: String, token: String): Boolean`
- `hasWriteAccess(callId: String, personIdent: String, token: String): Boolean`
- `veilederPersonerAccess(personidenter: List<String>, token: String, callId: String): List<String>?`

Keep `String` in the public API for now to avoid leaking app-domain types such as `PersonIdent`.

### 2. Visibility cleanup

Consider making these `internal` unless consumers need them:
- request header constants in `RequestUtil.kt`
- path constants inside `VeilederTilgangskontrollClient`
- HTTP client factory/config helpers in `HttpClientCommon.kt`
- JWT claim constants in `PipelineUtil.kt`

### 3. Ktor coupling

Keep the Ktor helper layer, but think of it as convenience API:
- `PipelineUtil.kt` can stay if Ktor is a deliberate part of the library
- if you want cleaner layering later, split into:
  - core client module
  - Ktor integration module

For now, a single-module Ktor-based library is acceptable.

### 4. Azure coupling

Since the decision was to include `AzureAdClient` in the library, keep it for now.
A later refinement could hide it behind a token-provider abstraction, but that is not required in Step 2.

### 5. Metrics/logging cleanup

Review whether the current library should:
- keep Micrometer support with injected `MeterRegistry`
- keep SLF4J logging only
- avoid logstash-specific constructs if not necessary

Strong recommendation:
- keep `slf4j-api`
- avoid requiring logback/logstash at runtime from the library
- if structured logging is only a nice-to-have, simplify it

---

## Build/dependency cleanup to consider

Review `build.gradle.kts` with these goals:

### Keep
- `kotlin("jvm")`
- `maven-publish`
- ktlint
- test logger
- Ktor client modules needed by `AzureAdClient` and `VeilederTilgangskontrollClient`
- Ktor server/auth dependency only if `PipelineUtil.kt` depends on it
- Jackson support used by the HTTP client
- `slf4j-api`
- test dependencies (`mockk`, `junit`, `ktor-client-mock`)

### Reconsider/remove if possible
- `logstash-logback-encoder`
- `logback-classic` from main dependencies
- any dependency only needed because of one small implementation detail

### Concrete build improvements
- switch deprecated `tasks.create("printVersion")` to `tasks.register("printVersion")`
- add `description = "..."`
- add POM metadata in `publishing {}` if this is going to be published soon

---

## Metadata to add in Step 2

### Add `.gitignore`
Recommended entries:
- `.gradle/`
- `build/`
- `.idea/`
- `*.iml`
- `out/`

### Add `README.md`
Recommended sections:
- what the library does
- who it is for
- supported stack assumptions (Kotlin/JVM, Ktor)
- dependency coordinates
- minimal usage example for:
  - `AzureAdClient`
  - `VeilederTilgangskontrollClient`
  - `checkVeilederTilgang(...)`
- note on read vs write access
- development commands

### Optional metadata
- `LICENSE`
- repo URL in publishing metadata
- package description in Gradle/POM

---

## Suggested order of work

1. Open `build.gradle.kts`
   - trim dependencies
   - change deprecated task registration
   - add `description`
   - decide publishing metadata level

2. Review `VeilederTilgangskontrollClient.kt`
   - confirm public method names and signatures
   - decide what constants/helpers should become `internal`
   - simplify logging if needed

3. Review `PipelineUtil.kt`
   - confirm Ktor-specific helper should remain in same module
   - internalize JWT/header helpers if they do not need to be public

4. Review `HttpClientCommon.kt` and `RequestUtil.kt`
   - minimize public surface
   - keep only what consumers need

5. Add `README.md` and `.gitignore`

6. Run validation

```bash
./gradlew clean test
./gradlew ktlintCheck
./gradlew jar
./gradlew publishToMavenLocal
```

---

## Behavior to preserve from the original app

When reviewing or refactoring, preserve these semantics from `isaktivitetskrav`:
- `hasAccess(...)` returns `true` only when `erGodkjent == true`
- `hasWriteAccess(...)` returns `true` only when `erGodkjent == true` and `fullTilgang == true`
- forbidden / denied access should still result in a normal access-denied outcome
- missing authorization or missing personident in Ktor route checks should still fail fast
- batch access lookup should still return the filtered list from istilgangskontroll

---

## Nice-to-have follow-up after Step 2

Once Step 2 is complete in the new repo:
- publish the library artifact
- open `isaktivitetskrav`
- replace local imports with library imports
- remove duplicated local implementation
- run the app tests to confirm behavior parity

---

## Resume prompt to use in the new repo

When opening the new repo in IntelliJ, a good continuation prompt is:

> Continue Step 2 in this repo. Start by reviewing `build.gradle.kts`, the public API in `VeilederTilgangskontrollClient`, and the Ktor helpers. Then add `README.md` and `.gitignore`, run tests and ktlint, and prepare the library for local publishing.

