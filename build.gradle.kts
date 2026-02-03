import dev.scaffoldit.hytale.HytaleManifest
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("dev.scaffoldit") version "0.2.4-rc1"
    id("com.gradleup.shadow") version "9.3.1"
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

val bundle: Configuration by configurations.creating

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    runtimeOnly("dev.scaffoldit:devtools:0.2.4-rc1")
    implementation("ws.schild:jave-all-deps:3.3.1")
    bundle("ws.schild:jave-all-deps:3.3.1")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    configurations = listOf(bundle)

    mergeServiceFiles()

    include("ws/schild/**")
    include("nativebin/**")

    include("net/wanmine/**")
    include("Common/**")
    include("Server/**")
    include("manifest.json")
}

tasks.named("build") {
    dependsOn("shadowJar")
}
