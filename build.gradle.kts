import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.os.OperatingSystem

plugins {
    base
}

val scalaVersion: String by properties
val gdxVersion: String by properties

subprojects {
    group = "dev.foobuilders"
    version = "0.1.0-SNAPSHOT"

    apply(plugin = "scala")

    repositories {
        mavenCentral()
    }

    dependencies {
        "implementation"("org.scala-lang:scala3-library_3:$scalaVersion")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<ScalaCompile>().configureEach {
        scalaCompileOptions.additionalParameters = listOf(
            "-deprecation",
            "-feature",
        )
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

project(":core") {
    dependencies {
        "implementation"(project(":shared"))
    }
}

project(":client-desktop") {
    apply(plugin = "application")

    dependencies {
        "implementation"(project(":core"))
        "implementation"(project(":shared"))
        "implementation"("com.badlogicgames.gdx:gdx:$gdxVersion")
        "implementation"("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
        "runtimeOnly"("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    }

    extensions.configure<JavaApplication> {
        mainClass.set("dev.foobuilders.client.DesktopLauncher")
    }

    tasks.withType<JavaExec>().configureEach {
        systemProperties["java.awt.headless"] = "false"
        if (OperatingSystem.current().isMacOsX) {
            jvmArgs("-XstartOnFirstThread")
        }
    }
}
