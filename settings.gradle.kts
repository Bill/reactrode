rootProject.name = "reactrode"
include("bom", "model","gameserver","geodeconfig","geodeserver","recorder","testclient","webapp")

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/libs-milestone") }
        maven { url = uri("https://repo.spring.io/libs-snapshots") }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.springframework.boot") {
                useModule("org.springframework.boot:spring-boot-gradle-plugin:2.2.0.M5")
            }
        }
    }
}
