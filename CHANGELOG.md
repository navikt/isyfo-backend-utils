# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.0.15]

### Changed
- Added KDoc to all public API members.
- Renamed `veilederPersonerAccess()` to `personsVeilederHasAccessTo()`.
- Renamed parameter `personident` to `personIdent` in `hasAccess()` and `hasWriteAccess()`.
- Renamed parameter `personidenter` to `personIdenter` in `personsVeilederHasAccessTo()`.

## [0.0.14]

### Changed
- Renamed `HttpClientCommon.kt` to `CommonHttpClient.kt`.
- Renamed `httpClientDefault()` to `defaultHttpClient()` and `httpClientProxy()` to `proxyHttpClient()` for more idiomatic Kotlin naming (qualifier first, type second).
- Made `defaultHttpClient()`, `proxyHttpClient()`, and `commonConfig` public so consumers can use them directly.

## [0.0.13]

### Changed
- Renamed `ApplicationCall.getNAVIdent()` to `getNavIdent()` for consistent camelCase naming.
- Renamed `ApplicationCall.getBearerHeader()` to `getBearerToken()` to better reflect what it returns (the token string, not a header).
- Renamed `ApplicationCall.getPersonident()` to `getPersonIdent()` for consistent camelCase naming.

## [0.0.12]

### Added
- `OboTokenProvider` functional interface (`fun interface`) in `no.nav.syfo.common.azure`. Represents any supplier of OBO (on-behalf-of) access tokens. Accepts `scopeClientId` and `token`, returns the access token as `String?`.
- `AzureAdClient` now implements `OboTokenProvider`, so it can be passed directly to `TilgangskontrollClient` without a lambda wrapper.

### Changed
- `TilgangskontrollClient` now depends on `OboTokenProvider` instead of `AzureAdClient` directly. This decouples the client from the Azure AD implementation and makes it easier to test or substitute with a custom token provider.
- **Breaking**: `AzureAdClient.getOnBehalfOfToken()` now returns `String?` (the access token string) instead of `AzureAdToken?`. Callers that previously accessed `.accessToken` on the result must remove that property access.

## [0.0.11]

### Changed
- Renamed `ObjectMapper.configure()` extension function to `applyCommonJacksonConfig()` to avoid conflict with Jackson's built-in `configure(Feature, Boolean)` method.

## [0.0.10]

### Changed
- Renamed `ForbiddenAccessVeilederException` to `VeilederTilgangBlokkertException` for consistency with Norwegian naming conventions used elsewhere in the codebase.

## [0.0.9]

### Changed
- Renamed `ForbiddenAccessVeilederException` to `VeilederTilgangBlokkertException` (initial rename, superseded by 0.0.10).

## [0.0.8]

### Added
- `TilgangskontrollClient` with `hasAccess()`, `hasWriteAccess()`, and `veilederPersonerAccess()` methods.
- `PipelineUtil` with `checkVeilederTilgang()` and `checkVeilederTilgangWithAction()` pipeline helpers.
- `ObjectMapperConfig` (`configuredJacksonMapper()`) and `JacksonMapperConfig` (`applyCommonJacksonConfig()`).
- `AzureAdClient`, `AzureAdToken`, `AzureAdTokenResponse`, `AzureEnvironment` for Azure AD OBO token exchange.
- `RequestUtil` with common NAV header constants and helpers.
- `HttpClientCommon` with `httpClientDefault()` and `httpClientProxy()` preconfigurations.

