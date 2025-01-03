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

gradlePlugin {
    plugins {
        create("generate-migration-plugin") {
            id = "io.fibril.ganglion.storage.migration.generate-migration-plugin"
            implementationClass = "GenerateMigrationPlugin"
        }
    }
}
