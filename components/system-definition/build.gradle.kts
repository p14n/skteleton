plugins {
    kotlin("jvm")
}

group = "com.skteleton"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Event protocol component dependency
    implementation(project(":components:event-protocol"))
    
    // Kotlin standard library
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

