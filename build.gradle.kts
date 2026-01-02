plugins {
    kotlin("jvm") version "2.0.21" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
        }
    }

    dependencies {
        "testImplementation"(kotlin("test"))
        //"testImplementation"("org.junit.jupiter:junit-jupiter:5.10.3")
        //"testImplementation"("io.kotest:kotest-runner-junit5:5.9.1")
        //"testImplementation"("org.mockito.kotlin:mockito-kotlin:5.4.0")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
