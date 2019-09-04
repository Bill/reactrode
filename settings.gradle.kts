rootProject.name = "reactrode"
include("model","gameserver","geodeserver","recorder","testclient","webapp")

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/libs-milestone") }
        maven { url = uri("https://repo.spring.io/libs-snapshots") }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.springframework.boot") {
                useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
            }
        }
    }
}