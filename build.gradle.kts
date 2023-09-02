import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
   id("com.netflix.dgs.codegen") version "6.0.2"
   id("org.springframework.boot") version "3.1.3"
   id("io.spring.dependency-management") version "1.1.3"
   id("org.jetbrains.kotlin.plugin.allopen") version "1.9.10"
   kotlin("jvm") version "1.9.10"
   kotlin("plugin.spring") version "1.9.10"
   kotlin("kapt") version "1.9.10"
}

group = "com.ultish"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
   mavenCentral()
}
dependencyManagement {
   imports {
      // We need to define the DGS BOM as follows such that the
      // io.spring.dependency-management plugin respects the versions expressed in the DGS BOM, e.g. graphql-java
      mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:latest.release")
   }
}
dependencies {
   // using dependency management instead
//   implementation(platform("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:5.1.1"))
   implementation("com.netflix.graphql.dgs:graphql-dgs-spring-boot-starter")
//   implementation("com.netflix.graphql.dgs:graphql-dgs-pagination")
//   implementation("com.netflix.graphql.dgs:graphql-dgs-subscriptions-websockets-autoconfigure:5.1.1")
   implementation("com.netflix.graphql.dgs:graphql-dgs-subscriptions-websockets-autoconfigure")

//// incompatible with spring boot 3x due to old mongo driver
//   implementation("com.querydsl:querydsl-mongodb:5.0.0")
//   implementation("com.querydsl:querydsl-apt:5.0.0")

   implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
   implementation("org.springframework.boot:spring-boot-starter-data-jpa")
   implementation("org.springframework.boot:spring-boot-starter-web")
   implementation("org.springframework.boot:spring-boot-starter")
   implementation("org.jetbrains.kotlin:kotlin-reflect")

   compileOnly("org.hibernate:hibernate-jpamodelgen:5.6.4.Final")
//   implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

//   kapt("com.querydsl:querydsl-apt:5.0.0:general")

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


tasks.withType<JavaCompile> {
   options.generatedSourceOutputDirectory.set(file("$buildDir/generated/sources/annotationProcessor"))
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
