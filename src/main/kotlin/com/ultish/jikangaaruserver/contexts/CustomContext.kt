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
import java.util.*


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
        var username: String? = null
        var roles: List<String>?

        var jwt: Jwt? = null
        if (principal is Jwt) {
            jwt = principal
            username = jwt.claims["username"].toString()
            roles = jwt.claims["roles"] as List<String>?
        } else {
            println("principal: $principal")
            // TODO temp check ws
//            userId = "b9d6c43e-cc6a-332b-9707-691a2b261642"
        }


        if (userId == null) {
            if (username == null) {
                // websocket is doing dodgy atm
                val securityContext = extensions?.get("TESTME")
                if (securityContext is SecurityContext) {
                    val p = securityContext.authentication.principal
                    if (p is Jwt) {
                        roles = p.claims.get("roles") as List<String>?
                        username = p.claims.get("username")
                            .toString()
                    }
                }
            }

            if (username != null) {
                userId = UUID.nameUUIDFromBytes(username.toByteArray())
                    .toString()


                if (!repository.existsById(userId)) {
                    val newUser = repository.save(EUser(username, ""))
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

