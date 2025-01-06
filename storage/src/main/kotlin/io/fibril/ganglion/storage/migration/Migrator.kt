package io.fibril.ganglion.storage.migration

import java.io.File

class Migrator {

    companion object {
        const val migrationFilesPath = "/db/migrations"

        @Throws(NullPointerException::class)
        fun getMigrationFiles(): List<File> {
            val files = mutableListOf<File>()

            val dir = File(object {}.javaClass.getResource(migrationFilesPath)?.file)
            dir.walk().forEach {
                if (it.isFile) {
                    files.add(it)
                }
            }
            return files.sortedBy {
                it.name
            }
        }

        fun buildMigrationSQL(): String {
            val files = getMigrationFiles()
            val result = StringBuilder().append(initSQL).append("\n")

            files.forEach { file ->
                val migrationName = toSnakeCase(
                    file.name.substringAfter("_").split(".")[0]
                )

                file.useLines {
                    result.append("SELECT apply_migration('$migrationName', \n \$\$ \n")
                    result.append(it.toList().joinToString(" "))
                    result.append("\n \$\$); \n")
                }
            }

            return result.toString()
        }


        val initSQL = """
            CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
            
            -- Add your Trigger functions manually here. TODO:- Make adding trigger function in migration files possible --
            
            CREATE OR REPLACE FUNCTION update_updated_at_column()
            RETURNS TRIGGER AS ${'$'}${'$'}
            BEGIN
               NEW.updated_at = now();
               RETURN NEW;
            END;
            ${'$'}${'$'} language 'plpgsql';
            
            -- end of trigger functions--
            
            
            DO
            ${"$"}body$
            BEGIN
                IF NOT EXISTS (SELECT FROM pg_catalog.pg_proc WHERE proname = 'apply_migration') THEN
                    CREATE FUNCTION apply_migration (migration_name TEXT, ddl TEXT) RETURNS BOOLEAN
                    AS $$
                    BEGIN
                        IF NOT EXISTS (SELECT FROM pg_catalog.pg_tables WHERE tablename = 'applied_migrations') THEN
                            CREATE TABLE applied_migrations (
                                identifier TEXT NOT NULL PRIMARY KEY,
                                ddl TEXT NOT NULL,
                                applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                            );
                        END IF;
                        LOCK TABLE applied_migrations IN EXCLUSIVE MODE;
                        IF NOT EXISTS (SELECT 1 FROM applied_migrations m WHERE m.identifier = migration_name) THEN 
                            RAISE NOTICE 'Applying migration: %', migration_name;
                            EXECUTE ddl;
                            INSERT INTO applied_migrations (identifier, ddl) VALUES (migration_name, ddl);
                            RETURN TRUE;
                        END IF;
                        RETURN FALSE;
                    END;
                    $$ LANGUAGE plpgsql;
                END IF;
            END
            ${"$"}body$;
        """.trimIndent()


        private fun toSnakeCase(input: String): String {
            return input
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")  // Handle camelCase and PascalCase
                .replace(Regex("[^a-zA-Z0-9]+"), "_")       // Replace non-alphanumeric characters with _
                .replace(Regex("_+"), "_")                 // Replace multiple underscores with a single _
                .trim('_')                                 // Remove leading or trailing underscores
                .lowercase()                               // Convert the string to lowercase
        }
    }
}