package com.ultish.jikangaaruserver.contexts

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.server.*
import org.springframework.graphql.server.support.AbstractAuthenticationWebSocketInterceptor
import org.springframework.graphql.server.support.AuthenticationExtractor
import org.springframework.graphql.server.support.BearerTokenAuthenticationExtractor
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import reactor.core.publisher.Mono
import reactor.util.context.ContextView

/**
 * Don't need this after going to JWT tokens
 */
//@Configuration
//internal class SubscriptionInterceptor : WebSocketGraphQlInterceptor {
//
//
//    override fun handleConnectionInitialization(
//        sessionInfo: WebSocketSessionInfo,
//        connectionInitPayload: Map<String, Any>
//    ): Mono<Any> {
//        val headers = connectionInitPayload["headers"]
//        if (headers is Map<*, *>) {
//            val userId = headers["user-id"]
//            println(userId)
//
//            // using the initial connect-init connection pull out the user-id
//            // header and place it in the session attributes so we can use it
//            // in the intercept below
//            sessionInfo.attributes.put("userIDDDDD", userId)
//        }
//
//        return Mono.just(connectionInitPayload)
//    }
//
//    /**
//     * inspired by org.springframework.graphql.server.support.AbstractAuthenticationWebSocketInterceptor and org.springframework.graphql.server.webmvc.AuthenticationWebSocketInterceptor
//     */
//    override fun intercept(request: WebGraphQlRequest, chain: WebGraphQlInterceptor.Chain): Mono<WebGraphQlResponse> {
//        if (request is WebSocketGraphQlRequest) {
//            // take the attribute from connect-init and place it on the
//            // extensions as websocket connections can't have extra headers.
//            // this will be used by the CustomContext class to pull user-id
//            val userId = request.attributes["userIDDDDD"]?.toString()
//            println(userId)
//
//            if (userId != null) {
//                request.extensions.put("user-id", userId)
//            }
//
//            return chain.next(request)
//        } else {
//            return chain.next(request)
//        }
//    }
//
//
//}

class Test(
    val issuerUri: String,
    val authenticationExtractor: AuthenticationExtractor
) : AbstractAuthenticationWebSocketInterceptor(authenticationExtractor) {


    private val authenticationAttribute = javaClass.name + ".AUTHENTICATION"

    override fun intercept(request: WebGraphQlRequest, chain: WebGraphQlInterceptor.Chain): Mono<WebGraphQlResponse> {
        if (request !is WebSocketGraphQlRequest) {
            return chain.next(request)
        }
        val attributes: Map<String, Any> = request.sessionInfo.attributes
        val securityContext = attributes[authenticationAttribute] as SecurityContext?
        val contextView = getContextToWrite(securityContext!!)
        request.extensions.put("TESTME", securityContext)
        return chain.next(request)
            .contextWrite(contextView)
    }

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val authMgr = JwtReactiveAuthenticationManager(ReactiveJwtDecoders.fromIssuerLocation(this.issuerUri))
        return authMgr.authenticate(authentication)
    }

    override fun getContextToWrite(securityContext: SecurityContext): ContextView {

        println(securityContext)
//        val key = SecurityContext::class.java.name // match SecurityContextThreadLocalAccessor key

//        return Context.of(key, securityContext)

        return ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))

    }

}

@Configuration
class WSConfigurer {

    /**
     * the magic sauce to wire up web socket JWTs
     */
    @Bean
    fun graphqlWssInterceptor(
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
        issuerUri: String
    ): WebSocketGraphQlInterceptor {
        return Test(issuerUri, BearerTokenAuthenticationExtractor())
//        return AuthenticationWebSocketInterceptor(
//            BearerTokenAuthenticationExtractor(),
//            JwtReactiveAuthenticationManager(ReactiveJwtDecoders.fromIssuerLocation(issuerUri))
//        )
    }
}
