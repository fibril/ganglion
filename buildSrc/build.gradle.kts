plugins {
    kotlin("jvm") version "1.9.0"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}


kotlin {
    jvmToolchain(17)
}
