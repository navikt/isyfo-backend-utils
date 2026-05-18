# JWT authentication

Utilities for validating incoming Azure AD JWTs in Ktor apps.

## Setup

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
