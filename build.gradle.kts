import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

group = "com.thoughtpropulsion"
version = "0.0.1-SNAPSHOT"

plugins {
    id("org.springframework.boot") version "2.2.0.M5"
}

apply(plugin = "io.spring.dependency-management")

configure<DependencyManagementExtension> {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("io.projectreactor:reactor-bom:Californium-RELEASE")

    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6"
}