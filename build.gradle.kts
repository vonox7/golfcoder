import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "org.golfcoder"
version = "0.0.1"

application {
    mainClass.set("org.golfcoder.ServerKt")

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
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("com.github.studoverse:katerbase:4e944a1d19b7b96a80080d42ecfebd9b732f5270")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-r2dbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-migration-r2dbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-json:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")
    implementation("org.mongodb:mongodb-driver-sync:4.9.0")
    implementation("io.sentry:sentry:8.27.1")
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_21 // FIXME change to 25 once kotlin gradle plugin supports it
    jvmToolchain(21)
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
