group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"

subprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6" // 5.6.2 Sep 25, 2019
}