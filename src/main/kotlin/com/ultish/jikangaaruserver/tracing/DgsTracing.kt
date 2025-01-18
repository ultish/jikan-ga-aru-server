package com.ultish.jikangaaruserver.tracing

import io.micrometer.observation.ObservationRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.observation.GraphQlObservationInstrumentation


@Configuration
class RegisterGraphQlObservations {

    /**
     * Magical bean that gives me graphql tracing (from spring-graphql)
     */
    @Bean
    fun graphQlObservationInstrumentation(observationRegistry: ObservationRegistry): GraphQlObservationInstrumentation {
        return GraphQlObservationInstrumentation(observationRegistry)
    }

//    @Bean
//    fun gqlJavaErrors(): GraphQLJavaErrorInstrumentation {
//        // TODO not sure what this does, but it gets registered??
//        return GraphQLJavaErrorInstrumentation()
//    }

}