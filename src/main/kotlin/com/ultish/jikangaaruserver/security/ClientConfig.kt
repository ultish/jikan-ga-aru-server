package com.ultish.jikangaaruserver.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "client")
class ClientConfig {

    lateinit var origins: List<String>
}