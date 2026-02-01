import dev.scaffoldit.hytale.HytaleManifest

plugins {
    id("dev.scaffoldit") version "0.2.4-rc1"
    java
    idea
}

group = "com.diamantino"
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
}

dependencies {
    runtimeOnly("dev.scaffoldit:devtools:0.2.4-rc1")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}