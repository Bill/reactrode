plugins {
    `java-platform`
}

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    constraints {

        api("io.projectreactor:reactor-core:3.3.0.M3")
        api("io.projectreactor:reactor-tools:3.3.0.M3")
        api("io.projectreactor:reactor-test:3.3.0.M3")

        api("io.vavr:vavr:0.9.2")

//        api("org.springframework.boot:spring-boot-starter-parent:2.2.0.M5")

        api("org.springframework.geode:spring-geode-starter:1.2.0.M1")
        api("org.springframework.data:spring-data-geode-test:0.0.7.RELEASE")

        api("org.springframework.data:spring-data-commons:2.1.9.RELEASE")

        api("com.fasterxml.jackson.core:jackson-annotations:2.9.8")
        api("com.fasterxml.jackson.core:jackson-databind:2.9.8")

        api("org.junit.jupiter:junit-jupiter:5.4.2")
        api("org.junit.jupiter:junit-jupiter-params:5.4.2")

        api("org.assertj:assertj-core:3.11.1")
    }
}
