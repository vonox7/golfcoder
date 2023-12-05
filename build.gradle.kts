import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.21"
    id("io.ktor.plugin") version "2.3.6"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "golf.adventofcode"
version = "0.0.1"

application {
    mainClass.set("golf.adventofcode.ServerKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io") // Needed for katerbase
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-client-core-jvm")
    implementation("io.ktor:ktor-client-cio-jvm")
    implementation("io.ktor:ktor-client-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-sessions-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-caching-headers-jvm")
    implementation("io.ktor:ktor-server-compression-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-default-headers-jvm")
    implementation("io.ktor:ktor-server-forwarded-header-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-html-builder-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("com.github.studoverse:katerbase:feb131944cef43e6accd101d3b2579e7a0b171b7")
    implementation("org.mongodb:mongodb-driver-sync:4.9.0")
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

tasks {
    getByName<JavaExec>("run") {
        dependsOn(shadowJar)
        classpath(shadowJar)
    }

    shadowJar {
        setProperty("archiveFileName", "Server.jar")
        isZip64 = true // Our jar files might have more than 65535 classes/files
    }

    // Copy Server.jar from build folder to root folder so we can delete all other folders
    register<Copy>("copyServer") {
        dependsOn(shadowJar)
        from(file("build/libs/Server.jar"))
        into(file("."))
    }

    register("stage") {
        group = "distribution"
        dependsOn(getByName("copyServer"))
    }

    build {
        mustRunAfter(clean)
    }
}
