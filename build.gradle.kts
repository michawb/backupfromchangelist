plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}

group = "de.ml.backupfromchangelist"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 👉 Wichtig: PhpStorm (PS), nicht IC oder IU
        create("PS", "2025.2")

        // WebDeployment ist bei PhpStorm bereits gebundled
        bundledPlugin("com.jetbrains.plugins.webDeployment")
        implementation("com.jcraft:jsch:0.1.55")

        // Test-Framework für IDE-Tests
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
        }
        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

// Optional: buildSearchableOptions deaktivieren (bekannter Bug)
tasks.named("buildSearchableOptions") {
    enabled = false
}