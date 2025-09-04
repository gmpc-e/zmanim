// Root build.gradle.kts (keep it minimal)
plugins {
    // no Android plugins here
}

tasks.register("cleanAll") {
    doLast { println("Root clean placeholder") }
}
