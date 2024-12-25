package com.ultish.jikangaaruserver.contexts

import com.netflix.graphql.dgs.context.DgsCustomContextBuilderWithRequest
import com.ultish.jikangaaruserver.entities.GraphQLEntity
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest


@Component
class CustomContextBuilder : DgsCustomContextBuilderWithRequest<CustomContext> {
    override fun build(extensions: Map<String, Any>?, headers: HttpHeaders?, webRequest: WebRequest?): CustomContext {
        var userId = headers?.getFirst("user-id")
        if (userId == null) {
            userId = extensions?.get("user-id")
                ?.toString()
        }
        return CustomContext(mutableListOf(), userId)
    }
}

class CustomContext(
    val entities: MutableList<GraphQLEntity<*>>, val userId: String?
) {

}

