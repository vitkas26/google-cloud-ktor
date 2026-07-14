plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    application
}

group = "com.nurtelecom.nurai"
version = "0.1.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"
val koinVersion = "3.5.6"

dependencies {
    // --- Server ---
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // --- Client (used to call Google Cloud REST APIs) ---
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // --- Google Cloud service-account auth (Bearer token from GOOGLE_APPLICATION_CREDENTIALS) ---
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // --- DI, same pattern as the Android client (Koin) ---
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    // --- Logging ---
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.nurtelecom.nurai.googlecloudproxy.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// Produces a single runnable "fat" jar for the Dockerfile (see README)
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    archiveVersion.set("")
    manifest { attributes["Main-Class"] = "com.nurtelecom.nurai.googlecloudproxy.ApplicationKt" }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
