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
    
    // Vert.x
    implementation("io.vertx:vertx-core:5.0.6")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:5.0.6")
    
    // Internal dependencies
    implementation(project(":components:event-protocol"))
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

