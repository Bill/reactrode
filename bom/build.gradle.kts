/*
 See also settings.gradle.kts for spring-boot-gradle-plugin version
 */
plugins {
    `java-platform`
}

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

javaPlatform {
    allowDependencies()
}

dependencies {

    api(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:2.2.0.M5"))

    // Spring Boot constrains the project reactor artifacts, so we do not need to unless we use an
    // esoteric reactor module
//    api(enforcedPlatform("io.projectreactor:reactor-bom:Dysprosium-RC1"))

    constraints {

        api("org.springframework.geode:spring-geode-starter:1.2.0.M1") // Aug 21, 2019

        api("org.springframework.data:spring-data-geode-test:0.0.7.RELEASE") // Aug 14, 2019

        // Spring Boot constrains the jackson artifacts, so we do not need to unless we use an
        // esoteric module
//        api("com.fasterxml.jackson.core:jackson-annotations:2.9.9") // May 16, 2019 (old)
//        api("com.fasterxml.jackson.core:jackson-databind:2.9.9")    // May 16, 2019 (old)

        api("org.junit.jupiter:junit-jupiter:5.4.2")
        api("org.junit.jupiter:junit-jupiter-params:5.4.2")

        api("org.assertj:assertj-core:3.13.2") // Aug 4, 2019

        api("io.vavr:vavr:0.10.2") // Aug 2, 2019

    }
}

