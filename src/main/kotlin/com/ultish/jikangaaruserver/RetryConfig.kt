package com.ultish.jikangaaruserver

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

@Configuration
@EnableRetry
class RetryConfig {


    @Value("\${spring.data.mongodb.uri}")
    private lateinit var mongoUri: String

    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()
        retryTemplate.setRetryPolicy(SimpleRetryPolicy().apply {
            maxAttempts = Integer.MAX_VALUE // Keep retrying indefinitely
        })
        retryTemplate.setBackOffPolicy(ExponentialBackOffPolicy().apply {
            initialInterval = 1000 // Initial retry interval in milliseconds
            maxInterval = 10000 // Maximum retry interval in milliseconds
            multiplier = 2.0 // Multiplier for exponential backoff
        })
        return retryTemplate
    }

    @Bean
    fun mongoTemplate(retryTemplate: RetryTemplate): MongoTemplate {
        return retryTemplate.execute<MongoTemplate, RuntimeException> {
            val factory = SimpleMongoClientDatabaseFactory(mongoUri)
            MongoTemplate(factory)
        }
    }
}