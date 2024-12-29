import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*


abstract class CreateMigrationFile : DefaultTask() {
    @get:Input
    abstract val migrationText: Property<String>

    private fun generateMigrationFile(): String {
        if (!project.hasProperty("migrationClass")) {
            throw IllegalArgumentException("No migrationClass prop specified")
        }
        val timestamp = Date().time
        val filepath =
            project.projectDir.toString() + String.format(
                "/src/main/resources/migrations/%s_%s.java",
                timestamp,
                project.property("migrationClass")
            )
        migrationText.set(
            StatementGenerator.createStatement(
                project.property("migrationClass") as String
            )
        )
        return filepath
    }

    @OutputFile
    val migrationFile: File = File(if (project.hasProperty("migrationClass")) generateMigrationFile() else "")

    @TaskAction
    fun action() {
        migrationFile.createNewFile()
        migrationFile.writeText(migrationText.get())
    }
}

class GenerateMigrationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("generateMigration", CreateMigrationFile::class.java) {
            group = "migration"
            description = "Create Migration file in resources/migration directory"
        }
    }
}


class StatementGenerator {
    companion object {
        fun createStatement(migrationClass: String): String {
            return """
                class $migrationClass {
                  TableCommand change() {
                    // add changes here
                    
                  }
                }
            """.trimIndent()
        }
    }
}