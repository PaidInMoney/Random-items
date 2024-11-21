plugins {
    kotlin("jvm") version "1.9.0" // Pick one version, preferably the stable version
    java
    id("com.github.johnrengelman.shadow") version "8.1.1" // Add Shadow plugin for shading
}

group = "net.paidinmoney"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

// ShadowJar task to bundle Kotlin runtime
tasks {
    jar {
        archiveBaseName.set("RandomItems")
        manifest {
            attributes(
                "Main-Class" to "net.paidinmoney.randomItem.RandomItem"
            )
        }
    }

    shadowJar {
        archiveBaseName.set("RandomItems")
        configurations = listOf(project.configurations.runtimeClasspath.get())
        relocate("kotlin", "net.paidinmoney.randomItem.kotlin") // Avoid conflicts
    }
}

// Ensure Shadow plugin builds the final jar
tasks.build {
    dependsOn(tasks.shadowJar)
}
