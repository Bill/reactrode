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

    api(platform("io.projectreactor:reactor-bom:Dysprosium-RC1"))
    api(platform("org.springframework.boot:spring-boot-dependencies:2.2.0.M5"))

    constraints {

        api("org.springframework.geode:spring-geode-starter:1.2.0.M1") // Aug 21, 2019

        api("org.springframework.data:spring-data-geode-test:0.0.7.RELEASE") // Aug 14, 2019

        api("org.springframework.data:spring-data-commons:2.1.10.RELEASE") // Aug 5, 2019

        api("com.fasterxml.jackson.core:jackson-annotations:2.9.9") // May 16, 2019 (old)
        api("com.fasterxml.jackson.core:jackson-databind:2.9.9")    // May 16, 2019 (old)

        api("org.junit.jupiter:junit-jupiter:5.4.2")
        api("org.junit.jupiter:junit-jupiter-params:5.4.2")

        api("org.assertj:assertj-core:3.13.2") // Aug 4, 2019

        api("io.vavr:vavr:0.10.2") // Aug 2, 2019

    }
}

