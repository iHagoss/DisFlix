import com.google.protobuf.gradle.*

group = "com.github.Stremio"
version = "1.11.2"

allprojects {
  repositories {
    google()
    mavenCentral()
  }
}

plugins {
  kotlin("multiplatform") version "2.2.21"
  id("org.mozilla.rust-android-gradle.rust-android") version "0.9.6"
  id("com.google.protobuf") version "0.9.5"
  id("com.android.library") version "8.13.1"
  id("maven-publish")
}

val kotlinVersion: String by extra
val pbandkVersion: String by extra
val protobufVersion: String by extra

buildscript {
  extra["kotlinVersion"] = "2.2.21"
  extra["pbandkVersion"] = "0.16.0"
  extra["protobufVersion"] = "4.33.2"

  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

kotlin {
  jvmToolchain(21)
  androidTarget {
    // TODO: Adding a "debug" variant here results in failing imports in KMM projects. Figure out why.
    publishLibraryVariants("release")
  }

  @Suppress("UNUSED_VARIABLE")
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("pro.streem.pbandk:pbandk-runtime:${pbandkVersion}")
      }
    }
    val androidMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
      }
    }
  }
}

android {
  ndkVersion = "28.2.13676358" // configure in .cargo/config.toml and workflows/release.yml as well

  defaultConfig {
    namespace = "com.stremio.core"
    minSdk = 21
    compileSdk = 34
  }

  sourceSets {
    getByName("main") {
      proto {
        srcDirs("../stremio-core-protobuf/proto")
      }
      manifest.srcFile("src/androidMain/AndroidManifest.xml")
    }
  }

  packaging {
    resources {
      excludes += "**/*.proto"
    }
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${protobufVersion}"
  }

  plugins {
    id("pbandk") {
      artifact = "pro.streem.pbandk:protoc-gen-pbandk-jvm:${pbandkVersion}:jvm8@jar"
    }
  }

  generateProtoTasks {
    all().forEach { task ->
      task.plugins {
        id("pbandk")
      }
    }
  }
}

cargo {
  module = "./"
  libname = "stremio_core_kotlin"
  targetDirectory = "../target"
  targets = listOf("arm", "arm64", "x86", "x86_64")
  verbose = true
  profile = "release"
}

tasks.whenTaskAdded {
  if (name == "javaPreCompileDebug" || name == "javaPreCompileRelease" || name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
    dependsOn("cargoBuild")
  }
}
