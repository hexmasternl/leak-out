// Pure Kotlin game logic. No Android dependencies, so everything here runs and is tested on the JVM.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(libs.versions.jdk.get().toInt())
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The bundled level files live in the app's assets. The level tests read them from there, so
// there is only one copy of every level.
val bundledLevelsDir: File = rootProject.file("app/src/main/assets/levels")

// The level format doc. A test parses its full example so that the doc cannot drift from the parser.
val levelFormatDoc: File = rootProject.file("../docs/level-format.md")

tasks.test {
    useJUnitPlatform()
    systemProperty("leakout.levelsDir", bundledLevelsDir.absolutePath)
    inputs.files(fileTree(bundledLevelsDir)).withPropertyName("bundledLevels")
    systemProperty("leakout.levelFormatDoc", levelFormatDoc.absolutePath)
    inputs.file(levelFormatDoc).withPropertyName("levelFormatDoc")
}
