plugins {
    id("org.springframework.boot")
    java
}

apply(plugin = "io.spring.dependency-management")

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

    implementation("org.springframework.geode:spring-geode-starter")
    {
        exclude(group= "javax.servlet", module= "javax.servlet-api")
        exclude(group= "org.eclipse.jetty", module= "jetty-server")
//        exclude(group="org.apache.logging.log4j", module=   "log4j-core")
    }

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.assertj:assertj-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.data:spring-data-geode-test")

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

springBoot {mainClassName = "com.thoughtpropulsion.reactrode.recorder.Application"}

configurations.all  {
// TODO: remove this exclusion once we are using Geode 1.10
// fixed here: https://issues.apache.org/jira/browse/GEODE-7050
    exclude( group="org.apache.logging.log4j", module = "log4j-to-slf4j")
}