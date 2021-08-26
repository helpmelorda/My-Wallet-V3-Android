plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("kotlin-android-extensions")
}

kotlin {
    android("android") {
    }

    sourceSets.all {
        languageSettings.apply {
            useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(project(":common:interface-mp"))
                api(project(":common-utils"))

                implementation(Libraries.timber)
            }
        }

        val commonTest by getting {
            dependencies {

            }
        }

        val androidTest by getting {
            dependencies {

            }
        }

        val androidMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(Libraries.biometricsApi)
            }
        }
    }
}

android {
    compileSdkVersion(Versions.compileSdk)
    buildToolsVersion(Versions.buildTools)
    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode(Versions.versionCode)
        versionName(Versions.versionName)
        testInstrumentationRunner("android.support.test.runner.AndroidJUnitRunner")
    }
    sourceSets {
        // Change 'main' to 'androidMain' for clarity in multiplatform environment
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            java.srcDirs(file("src/androidMain/kotlin"))
            res.srcDirs(file("src/androidMain/res"))
        }
        getByName("androidTest") {
            java.srcDirs(file("src/androidTest/kotlin"))
            res.srcDirs(file("src/androidTest/res"))
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.jvmTarget = Versions.kotlinJvmTarget
}