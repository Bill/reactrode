plugins {
    //    TODO: port to latest milestone
    //    id("org.springframework.boot") version "2.2.0.M4"
    id("org.springframework.boot") version "2.2.0.M3"
    java
}

apply(plugin = "io.spring.dependency-management")

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation("io.projectreactor:reactor-core")

    implementation("io.vavr:vavr:0.9.2")

    implementation("com.fasterxml.jackson.core:jackson-annotations:2.9.8")


    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("io.projectreactor:reactor-test")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
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
