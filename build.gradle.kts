plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.23"
  id("org.jetbrains.intellij") version "1.17.2"
}

group = "cn.devecor"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.2.5")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf(
    "com.intellij.gradle",
  ))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
  testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
