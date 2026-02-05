import io.gitlab.arturbosch.detekt.Detekt

val kotlinVersion: String by project
val logbackVersion: String by project
val ktorVersion: String by project
val exposedVersion: String by project
val flywayVersion: String by project
val hikariVersion: String by project
val postgresVersion: String by project
val h2Version: String by project
val hopliteVersion: String by project
val xmlUtilVersion: String by project
val koinVersion: String by project

plugins {
    kotlin("jvm") version "2.3.10"
    id("io.ktor.plugin") version "3.4.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
    id("com.diffplug.spotless") version "8.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "io.pcast"
version = "0.0.1"

application {
    mainClass.set("io.pcast.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

spotless {
    kotlin {
        ktlint().setEditorConfigPath("$projectDir/.editorconfig")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/config/detekt/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(false)
        sarif.required.set(false)
        md.required.set(true)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-xml-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.7.1-0.6.x-compat")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Exposed + database drivers
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.h2database:h2:$h2Version")

    // Flyway
    implementation("org.flywaydb:flyway-core:${flywayVersion}")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:${flywayVersion}")

    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging")

    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")

    implementation("cash.z.ecc.android:kotlin-bip39:1.0.9")
    implementation("io.viascom.nanoid:nanoid:1.0.1")

    implementation("io.github.pdvrieze.xmlutil:core-jdk:$xmlUtilVersion")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlUtilVersion")

    // Koin for dependency injection
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")

    implementation("io.github.serpro69:kotlin-faker:1.16.0")
}
