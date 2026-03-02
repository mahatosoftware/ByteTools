// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.ksp) apply false
}

// Redirect build files to local drive to avoid macOS "._*" issues on external drives
layout.buildDirectory.set(file("/Users/debasish/AndroidBuilds/ByteTools/root"))
