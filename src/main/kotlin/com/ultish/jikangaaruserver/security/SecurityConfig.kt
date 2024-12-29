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
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.socket.config.annotation.EnableWebSocket


@EnableMethodSecurity
@Configuration
@EnableWebSecurity
@EnableWebSocket
class SecurityConfig /*: WebSocketConfigurer*/ {
    @Value("\${client.origins}")
    lateinit var origins: String

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
                    .requestMatchers(
                        "/graphiql",
                        "/graphql",
                        "/graphql/**",  // Allow WebSocket handshake paths
                        "/ws/**"              // Additional WebSocket paths if needed
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val o = this.origins;
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(o) // Add your frontend origins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Origin",
                "Accept",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "user-id",
                "Sec-WebSocket-Protocol",     // Required for WebSocket
                "Sec-WebSocket-Version",
                "Sec-WebSocket-Key"
            )
            allowCredentials = true
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/graphql", configuration)
        }
    }

    @Bean
    fun jwtDecoder(
        @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
        jwlSetUri: String
    ): JwtDecoder {
        return NimbusJwtDecoder.withJwkSetUri(jwlSetUri)
            .build()
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