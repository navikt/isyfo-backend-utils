import com.adarshr.gradle.testlogger.theme.ThemeType

group = "no.nav.syfo"
version = "0.0.8"
description = "Shared Kotlin library for checking veileder access via istilgangskontroll"

val jacksonDataTypeVersion = "2.21.2"
val ktorVersion = "3.4.2"
val logbackVersion = "1.5.32"
val micrometerVersion = "1.16.4"
val mockkVersion = "1.14.9"
val slf4jVersion = "2.0.17"

plugins {
    kotlin("jvm") version "2.3.10"
    `java-library`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.adarshr.test-logger") version "4.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Public API types
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    api("io.micrometer:micrometer-core:$micrometerVersion")

    // Ktor client – used by AzureAdClient and VeilederTilgangskontrollClient
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Logging facade only; consuming apps own the runtime binding
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    // Serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonDataTypeVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonDataTypeVersion")

    // Tests
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

tasks {
    register("printVersion") {
        doLast {
            println(project.version)
        }
    }

    test {
        useJUnitPlatform()
        testlogger {
            theme = ThemeType.STANDARD_PARALLEL
            showFullStackTraces = true
            showPassed = false
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "no.nav.syfo"
            artifactId = "isyfo-backend-common"
            version = project.version.toString()
            from(components["java"])

            pom {
                name.set("isyfo-backend-common")
                description.set(project.description)
                url.set("https://github.com/navikt/isyfo-backend-common")
                scm {
                    url.set("https://github.com/navikt/isyfo-backend-common")
                    connection.set("scm:git:https://github.com/navikt/isyfo-backend-common.git")
                    developerConnection.set("scm:git:ssh://git@github.com/navikt/isyfo-backend-common.git")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/isyfo-backend-common")
            credentials {
                username = System.getenv("ORG_GRADLE_PROJECT_githubUser")
                password = System.getenv("ORG_GRADLE_PROJECT_githubPassword")
            }
        }
    }
}
