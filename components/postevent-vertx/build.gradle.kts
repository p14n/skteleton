plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "com.skteleton"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Vert.x
    implementation("io.vertx:vertx-core:5.0.6")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:5.0.6")

    // Internal dependencies
    implementation(project(":components:event-protocol"))

    // Postevent library for persistent event storage
    implementation("com.p14n:postevent-core:1.3.3")
    implementation("com.p14n:postevent-vertx:1.3.3")

    // Database
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")

    // OpenTelemetry (required by postevent)
    implementation("io.opentelemetry:opentelemetry-api:1.32.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation("io.zonky.test:embedded-postgres:2.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

