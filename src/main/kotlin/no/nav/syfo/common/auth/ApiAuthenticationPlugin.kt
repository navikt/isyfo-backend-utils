package no.nav.syfo.common.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("no.nav.syfo.common.auth")

/**
 * Installs Ktor's JWT [Authentication] plugin, configuring one provider per entry in [jwtIssuerList].
 * Each provider is named after its [JwtIssuerType] and validates audience against [JwtIssuer.acceptedAudienceList].
 */
fun Application.installJwtAuthentication(jwtIssuerList: List<JwtIssuer>) {
    install(Authentication) {
        jwtIssuerList.forEach { jwtIssuer ->
            configureJwt(jwtIssuer)
        }
    }
}

private fun AuthenticationConfig.configureJwt(jwtIssuer: JwtIssuer) {
    val jwkProvider = JwkProviderBuilder(URL(jwtIssuer.wellKnown.jwksUri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    jwt(name = jwtIssuer.jwtIssuerType.name) {
        verifier(
            jwkProvider = jwkProvider,
            issuer = jwtIssuer.wellKnown.issuer
        )
        validate { credential ->
            val hasExpectedAudience = jwtIssuer.acceptedAudienceList.any { aud ->
                credential.payload.audience.contains(aud)
            }
            if (hasExpectedAudience) {
                JWTPrincipal(credential.payload)
            } else {
                log.warn(
                    "Auth: Unexpected audience for jwt {}, {}",
                    StructuredArguments.keyValue("issuer", credential.payload.issuer),
                    StructuredArguments.keyValue("audience", credential.payload.audience)
                )
                null
            }
        }
    }
}
