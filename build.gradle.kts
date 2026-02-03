import dev.scaffoldit.hytale.wire.HytaleManifest
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI

plugins {
    id("dev.scaffoldit") version "0.2.4"
    id("com.gradleup.shadow") version "9.3.1"
    id("maven-publish")
    java
    idea
}

group = "net.wanmine"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

hytale {
	projectDir = ""
    manifest {
        Name = project.property("hytale.name").toString()
        Group = project.property("hytale.group").toString()
        Version = project.property("hytale.version").toString()
		Description = project.property("hytale.description").toString()
		Authors = project.property("hytale.authors").toString().split(',').map { HytaleManifest.Author(it.trim()) }
		Website = project.property("hytale.website").toString()
		ServerVersion = project.property("hytale.server_version").toString()
		Main = project.property("hytale.main").toString()
		DisabledByDefault = false
		IncludesAssetPack = true
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

val shade: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations.implementation.get().extendsFrom(shade)

dependencies {
    runtimeOnly("dev.scaffoldit:devtools:0.2.4")

    shade("ws.schild:jave-core:3.3.1")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)

    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    relocate("ws.schild.jave", "net.wanmine.musicrecorder.jave") {
        skipStringConstants = true
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/WanMine-Hytale/Wans-Music-Recorder")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}