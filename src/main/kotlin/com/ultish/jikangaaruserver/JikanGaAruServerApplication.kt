package com.ultish.jikangaaruserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class JikanGaAruServerApplication

fun main(args: Array<String>) {
   runApplication<JikanGaAruServerApplication>(*args)
}

// Enable CORS
@Configuration
class WebConfig : WebMvcConfigurer {
   override fun addCorsMappings(registry: CorsRegistry) {
      registry.addMapping("/**")
   }
}
