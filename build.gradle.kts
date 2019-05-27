plugins {
    java
}

group = "com.thoughtpropulsion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.projectreactor:reactor-core:3.2.8.RELEASE")
    implementation("io.vavr:vavr:0.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.4.2")
    testCompile("org.assertj:assertj-core:3.11.1")
    testCompile("io.projectreactor:reactor-test:3.2.8.RELEASE")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.4"
}
