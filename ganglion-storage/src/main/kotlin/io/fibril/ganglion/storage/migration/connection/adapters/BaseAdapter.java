package io.fibril.ganglion.storage.migration.connection.adapters;

import io.fibril.ganglion.storage.migration.connection.ColumnType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class BaseAdapter {

    /**
     * Class representation of a column. This type of instance is
     * typically used by TableCommand class to generate SQL statements
     * pertaining to the said Table.
     */
    static class ColumnCommand {
        String name;
        String type;
        Map<String, ?> options;

        ColumnCommand(String name, String type, Map<String, ?> options) {
            this.name = name;
            this.type = type;
            this.options = options;
        }
    }

    static class AddColumnCommand extends ColumnCommand {
        AddColumnCommand(String name, String type, Map<String, ?> options) {
            super(name, type, options);
        }
    }

    static class ChangeColumnCommand {
        ChangeColumnCommand(String name, String type, Map<String, ?> options) {

        }
    }

    static class TableCommand {
        //TODO:  connection
        String name;
        ArrayList<String> indexes = new ArrayList<>();
        Map<String, ColumnCommand> columnMap = Map.of();

        TableCommand addColumn(String name, String type, Map<String, ?> options) {
            throwOnDuplicateColumn(name);
            columnMap.put(name, new AddColumnCommand(name, type, options));
            return this;
        }

        TableCommand addColumn(String name, String type) {
            return addColumn(name, type, Map.of());
        }

        TableCommand removeColumn(String name) {
            this.columnMap.remove(name);
            return this;
        }

        TableCommand index(String columnName) {
            this.indexes.add(columnName);
            return this;
        }

        TableCommand timestamps() {
//            options[:null] = false if options[:null].nil?
//
//            if !options.key?(:precision) && @conn.supports_datetime_with_precision?
//            options[:precision] = 6
            return addColumn("created_at", ColumnType.DATETIME)
                    .addColumn("updated_at", ColumnType.DATETIME);
        }

        TableCommand references() {
            // TODO:- Add references
            return this;
        }

        Collection<ColumnCommand> columns() {
            return columnMap.values();
        }


        private void throwOnDuplicateColumn(String columnName) throws IllegalArgumentException {
            if (columnMap.containsKey(columnName)) {
                if ((Boolean) columnMap.get(columnName).options.get("primaryKey")) {
                    throw new IllegalArgumentException(String.format("you can't redefine the primary key column on %s. To define a custom primary key, pass { id: false } to options.", columnName));
                }
                throw new IllegalArgumentException(String.format("you can't define an already defined column %s.", columnName));
            }
        }
    }
}
