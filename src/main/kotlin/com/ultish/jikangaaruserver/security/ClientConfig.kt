package com.ultish.jikangaaruserver.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "client")
class ClientConfig {

    lateinit var origins: List<String>

    @NestedConfigurationProperty
    lateinit var jwt: JwtConfig
}

@Component
class JwtConfig {
    lateinit var issuer: String
}