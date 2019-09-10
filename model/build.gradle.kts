plugins {
    `java-library`
}

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation(enforcedPlatform(project(":bom")))

    implementation("io.projectreactor:reactor-core")
    implementation("io.projectreactor:reactor-tools")

    implementation("com.fasterxml.jackson.core:jackson-annotations")

    implementation("org.springframework.data:spring-data-commons")

    testImplementation("io.vavr:vavr")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core")
    testImplementation("io.projectreactor:reactor-test")

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
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