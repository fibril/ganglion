plugins {
    kotlin("jvm") version "1.9.0"
    application
    alias(libs.plugins.shadow)
    id("ganglion.default")
    id("io.fibril.ganglion.storage.migration.generate-migration-plugin")
}

repositories {
    mavenCentral()
}

val mainVerticleName = "io.fibril.ganglion.app.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"
val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
    mainClass.set(launcherClassName)
}

dependencies {
    implementation(libs.vertx.core)
    implementation(libs.vertx.config)
    implementation(libs.vertx.web)
    implementation(libs.vertx.kotlin.coroutines)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.guice)
    implementation(libs.imgscalr)
    implementation(project(":storage"))
    implementation(project(":clientServer"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    args = listOf(
        "run",
        mainVerticleName,
        "--redeploy=$watchForChange",
        "--launcher-class=$launcherClassName",
        "--on-redeploy=$doOnChange"
    )
}

tasks.named("generateMigration").configure {
    dependsOn("processResources")
}