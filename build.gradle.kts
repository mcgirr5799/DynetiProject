// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false

    id("com.google.gms.google-services") version "4.4.1" apply false
}

buildscript {
    repositories {
        // Check that you have the following line (if not, add it):
        google()
    }
    dependencies {
        // ...
        // Add this line:
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.7.1")
    }
}