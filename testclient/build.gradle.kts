plugins {
    id("org.springframework.boot") version "2.2.0.M5"
    java
}

apply(plugin = "io.spring.dependency-management")

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation(project(":model"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-rsocket")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // TODO: without this dependency, this RSocket TCP client crashes the server
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.5"
}

springBoot {mainClassName = "com.thoughtpropulsion.reactrode.client.Application"}