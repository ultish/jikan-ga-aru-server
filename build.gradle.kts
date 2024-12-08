import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   id("com.netflix.dgs.codegen") version "6.2.2"
   id("org.springframework.boot") version "3.4.0"
   id("io.spring.dependency-management") version "1.1.6"
   id("org.jetbrains.kotlin.plugin.allopen") version "1.9.24"
   kotlin("jvm") version "1.9.24"
   kotlin("plugin.spring") version "1.9.24"
   kotlin("kapt") version "1.9.24"
}

group = "com.ultish"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17


extra["netflixDgsVersion"] = "9.2.0"

kapt {
   correctErrorTypes = true
}

repositories {
   mavenCentral()
}
dependencyManagement {
   imports {
      // We need to define the DGS BOM as follows such that the
      // io.spring.dependency-management plugin respects the versions expressed in the DGS BOM, e.g. graphql-java
//      mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:8.7.1")
      mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:${property("netflixDgsVersion")}")
   }
}
dependencies {
   implementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter")
   implementation("org.springframework.boot:spring-boot-starter-websocket")

   // Use a newer version of QueryDSL that's compatible with Spring Boot 3.x
   implementation("com.querydsl:querydsl-mongodb:5.1.0") {
       exclude(group = "org.mongodb")
   }
   implementation("com.querydsl:querydsl-apt:5.1.0:jakarta")

   implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
   implementation("org.springframework.boot:spring-boot-starter-data-jpa")
   implementation("org.springframework.boot:spring-boot-starter-web")
   implementation("org.springframework.boot:spring-boot-starter")

   implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
   implementation("org.jetbrains.kotlin:kotlin-reflect")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")


   implementation("org.springframework.retry:spring-retry")

   // not compat with old mongodb
//   implementation( "org.springframework.boot:spring-boot-starter-actuator")
//   implementation("io.micrometer:micrometer-registry-prometheus:1.9.1")

   compileOnly("org.hibernate:hibernate-jpamodelgen:5.6.4.Final")

   kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")

   testImplementation("org.springframework.boot:spring-boot-starter-test")
   testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")

   testImplementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter-test")
}

allOpen {
   annotation("org.springframework.data.mongodb.core.mapping.Document")
}

tasks.withType<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask> {
   generateClient = true
   packageName = "com.ultish.generated"
   schemaPaths = listOf(
      "src/main/resources/schema"
   ).toMutableList()
   language = "kotlin"
}


tasks.withType<JavaCompile> {
   options.compilerArgs.addAll(arrayOf("-parameters", "-Aquerydsl.prefix=P"))
}

tasks.withType<KotlinCompile> {
   kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
      jvmTarget = "17"
   }
   dependsOn("generateJava")
}

tasks.withType<Test> {
   useJUnitPlatform()
}