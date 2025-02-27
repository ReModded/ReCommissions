import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    kotlin("jvm") version "2.1.20-RC"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.2.0"
}

group = "dev.remodded"
version = "1.0.0-SNAPSHOT"

paperweight.injectPaperRepository = false

repositories {
    maven("https://repo.remodded.dev/repository/Mojang/")
    maven("https://repo.remodded.dev/repository/PaperMC/")
    maven("https://repo.remodded.dev/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    runServer {
        minecraftVersion("1.21.4")

        downloadPlugins {
            modrinth("MCKotlin", "Z25PwYNh")
        }
    }

    withType<AbstractRun> {
        javaLauncher = project.javaToolchains.launcherFor {
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = JavaLanguageVersion.of(21)
        }
        jvmArgs("-XX:+AllowEnhancedClassRedefinition")
    }
}

paperPluginYaml {
    name = "ReCommission"
    version = project.version.toString()
    description = "ReModded Commission Plugin"

    main = "dev.remodded.recommission.ReCommission"
    authors.add("ReModded Team")
    apiVersion = "1.21.4"

    website = "https://github.com/ReModded/ReCommission"

    dependencies {
        server("MCKotlin-Paper", required = true)
    }
}
