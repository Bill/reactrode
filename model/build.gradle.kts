plugins {
    `java-library`
}

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    implementation("io.projectreactor:reactor-core:3.3.0.M3")
    implementation("io.projectreactor:reactor-tools:3.3.0.M3")

    implementation("io.vavr:vavr:0.9.2")

    implementation("com.fasterxml.jackson.core:jackson-annotations:2.9.8")

    implementation("org.springframework.data:spring-data-commons:2.1.9.RELEASE")

    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("io.projectreactor:reactor-test:3.2.11.RELEASE")

    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
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