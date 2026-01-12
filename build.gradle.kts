import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.scala.ScalaCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("ch.epfl.scala:gradle-bloop_2.13:1.6.4")
        classpath("org.scala-lang:scala-library:2.13.18")
    }
}

plugins {
    scala
    application
    id("cz.augi.gradle.scalafmt") version "1.21.5"
    id("io.github.cosmicsilence.scalafix") version "0.2.6"
}

allprojects {
    repositories {
        mavenCentral()
    }

    if (!plugins.hasPlugin("bloop")) {
        apply(plugin = "bloop")
    }
}

group = "com.foobuilders"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

scala {
    scalaVersion = "3.7.4"
    zincVersion = "1.10.4"
}

application {
    mainClass.set("com.foobuilders.GameMain")
}

dependencies {
    implementation("org.scala-lang:scala3-library_3:3.7.4")

    implementation("com.badlogicgames.gdx:gdx:1.14.0")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.0")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.0:natives-desktop")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.2")
}

tasks.withType<ScalaCompile>().configureEach {
    val extraScalacOptions = listOf(
        "-Wunused:all"
    )
    scalaCompileOptions.additionalParameters = (scalaCompileOptions.additionalParameters + extraScalacOptions).distinct()
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.named<JavaExec>("run") {
    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.check {
    dependsOn("checkScalafmt")
    dependsOn("checkScalafix")
}
