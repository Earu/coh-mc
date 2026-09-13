plugins {
    id("net.neoforged.moddev.legacyforge")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.minotaur)
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.minecraftforge.net/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
        content { includeGroup("thedarkcolour") }
    }
}

val commonProject = project(":common")

legacyForge {
    // NeoForged's 1.20.1 release keeps Forge coordinates and APIs.
    version = libs.versions.forge.get()

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

// Forge 47 finds mixin configs through a run argument in dev and the jar manifest in production.
mixin {
    config("coh.mixins.json")
}

tasks.jar {
    manifest {
        attributes("MixinConfigs" to "coh.mixins.json")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

base {
    archivesName = "coh-forge"
}

// Modrinth publishing through Modrinth's own plugin; CurseForge stays on mc-publish in the workflow.
// `./gradlew modrinth` uploads, MODRINTH_DEBUG=1 prints the request instead.
modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN").orElse("")
    projectId = providers.environmentVariable("MODRINTH_ID").orElse("")
    debugMode = providers.environmentVariable("MODRINTH_DEBUG").isPresent
    versionNumber = project.version.toString()
    versionName = "COH ${project.version} (Forge)"
    versionType = "release"
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    gameVersions.add(libs.versions.minecraft.get())
    loaders.add("forge")
    uploadFile.set(tasks.jar)
    dependencies {
        required.project("kotlin-for-forge")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}

// MultiLoader pattern: common's sources compile directly into this module's jar.
sourceSets.main {
    kotlin.srcDir(commonProject.file("src/main/kotlin"))
    java.srcDir(commonProject.file("src/main/java"))
    resources.srcDir(commonProject.file("src/main/resources"))
}

dependencies {
    implementation(libs.kff.forge)
}
