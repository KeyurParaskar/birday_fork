// Cosmos: the pure-Kotlin "traditions engine" of Auspice.
// No Android dependencies on purpose: everything here is computed offline from a date (and optionally a name),
// so it can be unit tested on the JVM and reused by widgets, workers and the UI alike.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
