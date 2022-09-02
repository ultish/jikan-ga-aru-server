import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   id("com.netflix.dgs.codegen") version "5.1.14"
   id("org.springframework.boot") version "2.6.2"
   id("io.spring.dependency-management") version "1.0.11.RELEASE"
   id("org.jetbrains.kotlin.plugin.allopen") version "1.6.10"
   kotlin("jvm") version "1.6.10"
   kotlin("plugin.spring") version "1.6.10"
   kotlin("kapt") version "1.6.10"
}

group = "com.ultish"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_14

repositories {
   mavenCentral()
}

dependencies {
   implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:5.1.1"))
   implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter")
//   implementation("com.netflix.graphql.dgs:graphql-dgs-pagination")
   implementation("com.netflix.graphql.dgs:graphql-dgs-subscriptions-websockets-autoconfigure:5.1.1")


   implementation("com.querydsl:querydsl-mongodb:5.0.0")
   implementation("com.querydsl:querydsl-apt:5.0.0")

   implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
   implementation("org.springframework.boot:spring-boot-starter-web")
   implementation("org.springframework.boot:spring-boot-starter")
   implementation("org.jetbrains.kotlin:kotlin-reflect")
   implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

   kapt("com.querydsl:querydsl-apt:5.0.0:general")

   testImplementation("org.springframework.boot:spring-boot-starter-test")
   testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
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

// TODO doesn't work
tasks.withType<JavaCompile> {
   options.compilerArgs.addAll(arrayOf("-parameters", "-Aquerydsl.prefix=P"))
}

tasks.withType<KotlinCompile> {
   kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
      jvmTarget = "14"
   }
   dependsOn("generateJava")
}

tasks.withType<Test> {
   useJUnitPlatform()
}
