import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.*


abstract class CreateMigrationFile : DefaultTask() {
    companion object {
        const val migrationNameProperty = "migrationName"
        const val tableNameProperty = "tableName"
    }

    private fun generateMigrationFile(): String {
        if (!project.hasProperty(migrationNameProperty)) {
            throw IllegalArgumentException("No migrationClass prop specified")
        }

        val timestamp = Date().time
        val filepath =
            project.projectDir.toString() + String.format(
                "/src/main/resources/db/migrations/%s_%s.sql",
                timestamp,
                project.property(migrationNameProperty)
            )

        return filepath
    }

    @OutputFile
    val migrationFile: File = File(if (project.hasProperty(migrationNameProperty)) generateMigrationFile() else "")

    @TaskAction
    fun action() {
        try {
            val created = migrationFile.createNewFile()

            if (created && project.hasProperty(migrationNameProperty)) {
                OutputStreamWriter(FileOutputStream(migrationFile), StandardCharsets.UTF_8).use { writer ->
                    writer.write(
                        StatementGenerator.initStatement(
                            if (project.hasProperty(tableNameProperty)) project.property(tableNameProperty)
                                ?.toString() else null
                        )
                    )
                }
            }
        } catch (e: IOException) {
            //
        }
    }
}

class GenerateMigrationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("generateMigration", CreateMigrationFile::class.java) {
            group = "migration"
            description = "Create Migration file in resources/db/migration directory"
        }
    }
}


object StatementGenerator {
    fun initStatement(tableName: String?): String {
        if (tableName != null) {
            return """
                CREATE TABLE IF NOT EXISTS $tableName (
                    id VARCHAR PRIMARY KEY NOT NULL DEFAULT REPLACE(uuid_generate_v4()::TEXT, '-', '')::VARCHAR,
                    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
                    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
                );
                
                CREATE TRIGGER update_${tableName}_updated_at
                    BEFORE UPDATE
                    ON
                        $tableName
                    FOR EACH ROW
                EXECUTE PROCEDURE update_updated_at_column();
            """.trimIndent()
        }
        return ""
    }
}
