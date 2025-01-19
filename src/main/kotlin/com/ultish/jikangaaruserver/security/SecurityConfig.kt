package com.ultish.jikangaaruserver.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@EnableMethodSecurity
@Configuration
@EnableWebSecurity
//@EnableWebSocket
class SecurityConfig(private val clientConfig: ClientConfig) /*: WebSocketConfigurer*/ {

    /**
     * also see https://github.com/Netflix/dgs-framework/issues/1294 for example setup
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val resolver = DefaultBearerTokenResolver()
        resolver.setAllowUriQueryParameter(true)

        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() } // duno?
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("subscriptions", "graphiql", "graphql", "actuator/**")
                    .permitAll()
//                    .requestMatchers(
//                        "/graphql",
//                    )
//                    .authenticated()
                    .anyRequest()
                    .permitAll()

            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

        // Enable Security Context propagation
//        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {

        val configuration = CorsConfiguration().apply {
            allowedOrigins = clientConfig.origins // Add your frontend origins
            allowedMethods = listOf("*")
            allowedHeaders = listOf("*")
//            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
//            allowedHeaders = listOf(
//                "Authorization",
//                "Content-Type",
//                "Origin",
//                "Accept",
//                "X-Requested-With",
//                "Access-Control-Request-Method",
//                "Access-Control-Request-Headers",
//                "Access-Control-Allow-Origin",
//                "user-id",
//                "Sec-WebSocket-Protocol",     // Required for WebSocket
//                "Sec-WebSocket-Version",
//                "Sec-WebSocket-Key"
//            )
            allowCredentials = false
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/graphql", configuration)
        }
    }

    @Bean
    fun jwtDecoder(
        @Value("\${client.jwt.issuer}") issuer: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
        jwlSetUri: String
    ): JwtDecoder {
        val defaultVal = JwtValidators.createDefaultWithIssuer(issuer)
        val jwtVal = NimbusJwtDecoder.withJwkSetUri(jwlSetUri)
            .build()
        jwtVal.setJwtValidator(defaultVal)
        return jwtVal
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(CustomJwtGrantedAuthoritiesConverter())
        return converter
    }

    /**
     * Parses the JWT string and fetches the roles out of it
     */
    class CustomJwtGrantedAuthoritiesConverter : Converter<Jwt, Collection<GrantedAuthority>> {
        override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
            val authorities = mutableListOf<GrantedAuthority>()
            val roles = jwt.getClaim<List<String>>("roles")
            roles?.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_$role"))
            }
            return authorities
        }
    }


}
