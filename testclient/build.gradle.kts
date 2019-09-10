plugins {
    id("org.springframework.boot")
    java
}

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

dependencies {
    implementation(enforcedPlatform(project(":bom")))

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
    gradleVersion = "5.6"
}

springBoot {mainClassName = "com.thoughtpropulsion.reactrode.client.TestClientApplication"}