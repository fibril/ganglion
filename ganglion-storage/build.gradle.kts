
plugins {
    kotlin("jvm") version "1.9.0"
    id("ganglion.default")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.pg.client)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}