import com.adarshr.gradle.testlogger.theme.ThemeType

group = "no.nav.syfo"
version = "0.0.1"

val jacksonDataTypeVersion = "2.21.2"
val ktorVersion = "3.4.2"
val logbackVersion = "1.5.32"
val logstashEncoderVersion = "9.0"
val micrometerVersion = "1.16.4"
val mockkVersion = "1.14.9"

plugins {
    kotlin("jvm") version "2.3.10"
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.adarshr.test-logger") version "4.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Ktor client – used by AzureAdClient and VeilederTilgangskontrollClient
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Ktor server – ApplicationCall / RoutingContext extensions in PipelineUtil
    // Also transitively provides com.auth0:java-jwt used for JWT.decode()
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    // Logging – logback is compileOnly so consuming apps own the runtime binding
    compileOnly("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    // Metrics – core only; consuming apps add their own registry implementation
    implementation("io.micrometer:micrometer-core:$micrometerVersion")

    // Serialization
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

tasks {
    create("printVersion") {
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
        create<MavenPublication>("maven") {
            groupId = "no.nav.syfo"
            artifactId = "isyfo-check-veiledertilgang-util"
            version = project.version.toString()
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/isyfo-check-veiledertilgang-util")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

