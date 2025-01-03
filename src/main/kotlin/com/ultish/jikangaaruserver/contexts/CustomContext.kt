package com.ultish.jikangaaruserver.contexts

import com.netflix.graphql.dgs.context.DgsCustomContextBuilderWithRequest
import com.ultish.jikangaaruserver.entities.EUser
import com.ultish.jikangaaruserver.entities.GraphQLEntity
import com.ultish.jikangaaruserver.users.UserRepository
import com.ultish.jikangaaruserver.users.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest


@Component
class CustomContextBuilder : DgsCustomContextBuilderWithRequest<CustomContext> {

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var repository: UserRepository

    override fun build(extensions: Map<String, Any>?, headers: HttpHeaders?, webRequest: WebRequest?): CustomContext {
        val principal = SecurityContextHolder.getContext()
            .authentication
            .principal

        var userId = headers?.getFirst("user-id")
        var name: String = "unknown"

        var jwt: Jwt? = null
        if (principal is Jwt) {
            jwt = principal
            userId = jwt.claims["sub"].toString()
            name = jwt.claims["name"].toString()
        } else {
            println("principal: $principal")
        }


        if (userId == null) {
            // websocket is doing dodgy atm
            val securityContext = extensions?.get("TESTME")
            if (securityContext is SecurityContext) {
                val p = securityContext.authentication.principal
                if (p is Jwt) {
                    userId = p.claims["sub"].toString()
                    name = p.claims["name"].toString()
                }
            }

            if (userId != null) {
                if (!repository.existsById(userId)) {
                    val newUser = repository.save(EUser(userId, name, ""))
                    println(newUser)
                }
            }
        }

        return CustomContext(mutableListOf(), userId, jwt)
    }
}

class CustomContext(
    val entities: MutableList<GraphQLEntity<*>>, val userId: String?, val jwt: Jwt?
) {

}

