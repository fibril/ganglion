
plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("ganglion.default")
}

repositories {
    mavenCentral()
}

val mainVerticleName = "io.fibril.ganglion.app.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

application {
    mainClass.set(launcherClassName)
}

dependencies {
    implementation(libs.vertx.core)
    implementation(libs.vertx.kotlin.coroutines)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.withType<JavaExec> {
    args = listOf(
        "run",
        mainVerticleName,
        "--launcher-class=$launcherClassName"
    )
}