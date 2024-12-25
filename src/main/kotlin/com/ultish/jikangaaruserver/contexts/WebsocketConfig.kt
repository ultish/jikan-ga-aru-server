package com.ultish.jikangaaruserver.contexts

import org.springframework.context.annotation.Configuration
import org.springframework.graphql.server.*
import reactor.core.publisher.Mono


@Configuration
internal class SubscriptionInterceptor : WebSocketGraphQlInterceptor {
    override fun handleConnectionInitialization(
        sessionInfo: WebSocketSessionInfo,
        connectionInitPayload: Map<String, Any>
    ): Mono<Any> {
        val headers = connectionInitPayload["headers"]
        if (headers is Map<*, *>) {
            val userId = headers["user-id"]
            println(userId)

            // using the initial connect-init connection pull out the user-id
            // header and place it in the session attributes so we can use it
            // in the intercept below
            sessionInfo.attributes.put("userIDDDDD", userId)
        }

        return Mono.just(connectionInitPayload)
    }

    /**
     * inspired by org.springframework.graphql.server.support.AbstractAuthenticationWebSocketInterceptor and org.springframework.graphql.server.webmvc.AuthenticationWebSocketInterceptor
     */
    override fun intercept(request: WebGraphQlRequest, chain: WebGraphQlInterceptor.Chain): Mono<WebGraphQlResponse> {

        if (request is WebSocketGraphQlRequest) {
            // take the attribute from connect-init and place it on the
            // extensions as websocket connections can't have extra headers.
            // this will be used by the CustomContext class to pull user-id
            val userId = request.attributes["userIDDDDD"]?.toString()
            println(userId)

            if (userId != null) {
                request.extensions.put("user-id", userId)
            }

//            chain.next(request)
//                .contextWrite { ctx -> ctx.put("user-id", userId) }

            return chain.next(request)
        } else {
            return chain.next(request)
        }
    }
}
