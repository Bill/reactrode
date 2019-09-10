plugins {
    id("org.springframework.boot")
    java
}

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

dependencies {
    implementation(enforcedPlatform(project(":bom")))

    implementation(project(":model"))

    implementation("org.springframework.boot:spring-boot-starter-rsocket")
//    webflux is needed to cause rsocket/websocket to bind to a well-known port
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6"
}

springBoot {mainClassName = "com.thoughtpropulsion.reactrode.server.GameServerApplication"}
