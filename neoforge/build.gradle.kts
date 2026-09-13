plugins {
    alias(libs.plugins.moddev)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.minotaur)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
        content { includeGroup("thedarkcolour") }
    }
}

val commonProject = project(":common")

neoForge {
    version = libs.versions.neoforge.get()

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create("coh") {
            sourceSet(sourceSets.main.get())
        }
    }
}

kotlin {
    jvmToolchain(21)
}

base {
    archivesName = "coh-neoforge"
}

// Modrinth publishing through Modrinth's own plugin; CurseForge stays on mc-publish in the workflow.
// `./gradlew modrinth` uploads, MODRINTH_DEBUG=1 prints the request instead.
modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN").orElse("")
    projectId = providers.environmentVariable("MODRINTH_ID").orElse("")
    debugMode = providers.environmentVariable("MODRINTH_DEBUG").isPresent
    versionNumber = project.version.toString()
    versionName = "COH ${project.version} (NeoForge)"
    versionType = "release"
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    gameVersions.add(libs.versions.minecraft.get())
    loaders.add("neoforge")
    uploadFile.set(tasks.jar)
    dependencies {
        required.project("kotlin-for-forge")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

// MultiLoader pattern: common's sources compile directly into this module's jar,
// so there is no separate common artifact to bundle or relocate.
sourceSets.main {
    kotlin.srcDir(commonProject.file("src/main/kotlin"))
    java.srcDir(commonProject.file("src/main/java"))
    resources.srcDir(commonProject.file("src/main/resources"))
}

dependencies {
    implementation(libs.kff.neoforge)
}
