package com.ultish.jikangaaruserver.contexts

import com.netflix.graphql.dgs.context.DgsCustomContextBuilder
import com.ultish.jikangaaruserver.entities.GraphQLEntity
import org.springframework.stereotype.Component

@Component
class CustomContextBuilder : DgsCustomContextBuilder<CustomContext> {
   override fun build(): CustomContext {
      return CustomContext(mutableListOf())
   }
}

class CustomContext(
   val entities: MutableList<GraphQLEntity<*>>,
) {

}