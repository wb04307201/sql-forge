package cn.wubo.sql.forge.utils;

import cn.wubo.sql.forge.TreeNode;
import cn.wubo.sql.forge.records.*;
import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.wubo.sql.forge.constant.Constant.NODE_VALUE_TEMPLATE;

@UtilityClass
public class MetaDataUtils {

    public DatabaseInfo getDatabase(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        return new DatabaseInfo(
                databaseMetaData.getDatabaseProductName(),
                databaseMetaData.getDatabaseProductVersion()
        );
    }

    public List<SchemaInfo> getSchemas(Connection connection, String catalog, String schemaPattern) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getSchemas(catalog, schemaPattern)) {
            List<SchemaInfo> schemas = new ArrayList<>();
            while (rs.next()) {
                schemas.add(new SchemaInfo(
                        rs.getString("TABLE_SCHEM")
                ));
            }
            return schemas;
        }
    }

    public List<String> getTableTypes(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getTableTypes()) {
            List<String> tableTypes = new ArrayList<>();
            while (rs.next()) {
                tableTypes.add(rs.getString("TABLE_TYPE"));
            }
            return tableTypes;
        }
    }

    public List<TableInfo> getTables(Connection connection, String catalog, String schemaPattern,
                                     String tableNamePattern, String[] types) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getTables(catalog, schemaPattern, tableNamePattern, types)) {
            List<TableInfo> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(new TableInfo(
                        rs.getString("TABLE_NAME"),
                        rs.getString("REMARKS")
                ));
            }
            return tables;
        }
    }

    public List<ColumnInfo> getColumns(Connection connection, String catalog, String schemaPattern,

                                       String tableNamePattern, String columnNamePattern) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern)) {
            List<ColumnInfo> columns = new ArrayList<>();
            while (rs.next()) {
                columns.add(new ColumnInfo(
                        rs.getString("COLUMN_NAME"),
                        rs.getString("TYPE_NAME"),
                        rs.getInt("COLUMN_SIZE"),
                        rs.getInt("DECIMAL_DIGITS"),
                        rs.getInt("NULLABLE"),
                        rs.getString("REMARKS"),
                        rs.getString("COLUMN_DEF"),
                        rs.getInt("ORDINAL_POSITION"),
                        rs.getString("IS_NULLABLE"),
                        rs.getString("IS_AUTOINCREMENT"),
                        rs.getString("IS_GENERATEDCOLUMN")
                ));
            }
            return columns;
        }
    }

    public List<PrimaryKeyInfo> getPrimaryKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getPrimaryKeys(catalog, schema, table)) {
            List<PrimaryKeyInfo> primaryKeys = new ArrayList<>();
            while (rs.next()) {
                primaryKeys.add(new PrimaryKeyInfo(
                        rs.getString("PK_NAME"),
                        Collections.singletonList(rs.getString("COLUMN_NAME"))
                ));
            }
            return primaryKeys
                    .stream()
                    .collect(Collectors.groupingBy(PrimaryKeyInfo::pkName))
                    .values()
                    .stream()
                    .map(primaryKeyInfos -> {
                        List<String> columnNames = primaryKeyInfos.stream().flatMap(primaryKey -> primaryKey.columnName().stream()).toList();
                        PrimaryKeyInfo primaryKey = primaryKeyInfos.get(0);
                        return new PrimaryKeyInfo(
                                primaryKey.pkName(),
                                columnNames
                        );
                    })
                    .toList();
        }
    }

    public List<ForeignKeyInfo> getImportedKeys(Connection connection, String catalog, String schema, String table) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getImportedKeys(catalog, schema, table)) {
            List<ForeignKeyInfo> foreignKeys = new ArrayList<>();
            while (rs.next()) {
                foreignKeys.add(new ForeignKeyInfo(
                        rs.getString("FK_NAME"),
                        rs.getString("PK_NAME"),
                        rs.getString("PKTABLE_NAME"),
                        rs.getString("PKCOLUMN_NAME"),
                        rs.getString("FKTABLE_NAME"),
                        rs.getString("FKCOLUMN_NAME")
                ));
            }
            return foreignKeys;
        }
    }

    public List<IndexInfo> getIndexInfo(Connection connection, String catalog, String schema, String table,
                                        boolean unique, boolean approximate) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getIndexInfo(catalog, schema, table, unique, approximate)) {
            List<IndexInfo> indexes = new ArrayList<>();
            while (rs.next()) {
                indexes.add(new IndexInfo(
                        rs.getString("INDEX_NAME"),
                        rs.getBoolean("NON_UNIQUE"),
                        Collections.singletonList(rs.getString("COLUMN_NAME")),
                        rs.getString("ASC_OR_DESC")
                ));
            }
            return indexes
                    .stream()
                    .collect(Collectors.groupingBy(IndexInfo::indexName))
                    .values()
                    .stream()
                    .map(indexInfos -> {
                        List<String> columnNames = indexInfos.stream().flatMap(index -> index.columnName().stream()).toList();
                        IndexInfo indexInfo = indexInfos.get(0);
                        return new IndexInfo(
                                indexInfo.indexName(),
                                indexInfo.nonUnique(),
                                columnNames,
                                indexInfo.ascOrDesc()
                        );
                    })
                    .toList();
        }
    }

    public TreeNode<?> getMetaData(Connection connection) throws SQLException {
        TreeNode<DatabaseInfo> root = new TreeNode<>();
        DatabaseInfo databaseInfo = getDatabase(connection);

        root.setLabel(databaseInfo.productName());
        root.setValue(databaseInfo.productName());
        root.setData(databaseInfo);

        Set<String> systemSchemas = buildSystemSchemaSet(databaseInfo.productName().toUpperCase());
        List<SchemaInfo> schemas = getSchemas(connection, null, null)
                .stream()
                .filter(s -> !systemSchemas.contains(s.tableSchema().trim().toUpperCase()))
                .toList();

        List<String> tableTypes = new ArrayList<>();
        for (SchemaInfo schema : schemas) {
            TreeNode<SchemaInfo> schemaNode = new TreeNode<>();
            schemaNode.setLabel(schema.tableSchema());
            schemaNode.setValue(String.format(NODE_VALUE_TEMPLATE, root.getData().productName(), schema.tableSchema()));
            schemaNode.setData(schema);
            root.addChild(schemaNode);

            if (tableTypes.isEmpty())
                tableTypes = getTableTypes(connection);
            for (String tableType : tableTypes) {
                TreeNode<String> tableTypeNode = new TreeNode<>();
                tableTypeNode.setLabel(tableType);
                tableTypeNode.setValue(String.format(NODE_VALUE_TEMPLATE, schemaNode.getValue(), tableType));
                tableTypeNode.setData(tableType);

                List<TableInfo> tables = getTables(connection, null, schema.tableSchema(), null, new String[]{tableType});
                if (!tables.isEmpty()){
                    schemaNode.addChild(tableTypeNode);
                }
                for (TableInfo table : tables) {
                    TreeNode<TableInfo> tableNode = new TreeNode<>();
                    tableNode.setLabel(table.tableName());
                    tableNode.setValue(String.format(NODE_VALUE_TEMPLATE, tableTypeNode.getValue(), table.tableName()));
                    tableNode.setData(table);
                    tableTypeNode.addChild(tableNode);

                    TreeNode<String> rootColumnNode = new TreeNode<>();
                    rootColumnNode.setLabel("列");
                    rootColumnNode.setValue(String.format(NODE_VALUE_TEMPLATE, tableNode.getValue(), "columns"));
                    rootColumnNode.setData("columns");
                    tableNode.addChild(rootColumnNode);
                    List<ColumnInfo> columns = getColumns(connection, null, schema.tableSchema(), table.tableName(), null);
                    for (ColumnInfo column : columns) {
                        TreeNode<ColumnInfo> columnNode = new TreeNode<>();
                        columnNode.setLabel(column.columnName());
                        columnNode.setValue(String.format(NODE_VALUE_TEMPLATE, rootColumnNode.getValue(), column.columnName()));
                        columnNode.setData(column);
                        rootColumnNode.addChild(columnNode);
                    }

                    TreeNode<String> rootPrimaryKeyNode = new TreeNode<>();
                    rootPrimaryKeyNode.setLabel("主键");
                    rootPrimaryKeyNode.setValue(String.format(NODE_VALUE_TEMPLATE, tableNode.getValue(), "primaryKeys"));
                    rootPrimaryKeyNode.setData("primaryKeys");
                    tableNode.addChild(rootPrimaryKeyNode);
                    List<PrimaryKeyInfo> primaryKeys = getPrimaryKeys(connection, null, schema.tableSchema(), table.tableName());
                    for (PrimaryKeyInfo primaryKey : primaryKeys) {
                        TreeNode<PrimaryKeyInfo> primaryKeyNode = new TreeNode<>();
                        primaryKeyNode.setLabel(primaryKey.pkName());
                        primaryKeyNode.setValue(String.format(NODE_VALUE_TEMPLATE, rootPrimaryKeyNode.getValue(), primaryKey.pkName()));
                        primaryKeyNode.setData(primaryKey);
                        rootPrimaryKeyNode.addChild(primaryKeyNode);

                        for (String columnName : primaryKey.columnName()){
                            TreeNode<String> columnNode = new TreeNode<>();
                            columnNode.setLabel(columnName);
                            columnNode.setValue(String.format(NODE_VALUE_TEMPLATE, primaryKeyNode.getValue(), columnName));
                            columnNode.setData(columnName);
                            primaryKeyNode.addChild(columnNode);
                        }
                    }

                    TreeNode<String> rootForeignKeyNode = new TreeNode<>();
                    rootForeignKeyNode.setLabel("外键");
                    rootForeignKeyNode.setValue(String.format(NODE_VALUE_TEMPLATE, tableNode.getValue(), "foreignKeys"));
                    rootForeignKeyNode.setData("foreignKeys");
                    tableNode.addChild(rootForeignKeyNode);
                    List<ForeignKeyInfo> foreignKeys = getImportedKeys(connection, null, schema.tableSchema(), table.tableName());
                    for (ForeignKeyInfo foreignKey : foreignKeys) {
                        TreeNode<ForeignKeyInfo> foreignKeyNode = new TreeNode<>();
                        foreignKeyNode.setLabel(foreignKey.fkName());
                        foreignKeyNode.setValue(String.format(NODE_VALUE_TEMPLATE, rootForeignKeyNode.getValue(), foreignKey.fkName()));
                        foreignKeyNode.setData(foreignKey);
                        rootForeignKeyNode.addChild(foreignKeyNode);
                    }

                    TreeNode<String> rootIndexNode = new TreeNode<>();
                    rootIndexNode.setLabel("索引");
                    rootIndexNode.setValue(String.format(NODE_VALUE_TEMPLATE, tableNode.getValue(), "indexes"));
                    rootIndexNode.setData("indexes");
                    tableNode.addChild(rootIndexNode);
                    List<IndexInfo> indexes = getIndexInfo(connection, null, schema.tableSchema(), table.tableName(), false, false);
                    for (IndexInfo index : indexes) {
                        TreeNode<IndexInfo> indexNode = new TreeNode<>();
                        indexNode.setLabel(index.indexName());
                        indexNode.setValue(String.format(NODE_VALUE_TEMPLATE, rootIndexNode.getValue(), index.indexName()));
                        indexNode.setData(index);
                        rootIndexNode.addChild(indexNode);

                        for (String columnName : index.columnName()){
                            TreeNode<String> columnNode = new TreeNode<>();
                            columnNode.setLabel(columnName);
                            columnNode.setValue(String.format(NODE_VALUE_TEMPLATE, indexNode.getValue(), columnName));
                            columnNode.setData(columnName);
                            indexNode.addChild(columnNode);
                        }
                    }
                }
            }
        }

        return root;
    }


    private Set<String> buildSystemSchemaSet(String dbType) {
        Set<String> set = new HashSet<>();
        if (dbType.contains("H2")) {
            set.add("INFORMATION_SCHEMA");
        } else if (dbType.contains("MYSQL")) {
            set.addAll(Set.of("INFORMATION_SCHEMA", "MYSQL", "PERFORMANCE_SCHEMA", "SYS"));
        } else if (dbType.contains("POSTGRESQL")) {
            set.addAll(Set.of("PG_CATALOG", "INFORMATION_SCHEMA", "PG_TOAST"));
        } else if (dbType.contains("ORACLE")) {
            set.addAll(Set.of("SYS", "SYSTEM", "OUTLN", "CTXSYS", "XDB"));
        } else if (dbType.contains("SQL SERVER") || dbType.contains("MICROSOFT")) {
            set.addAll(Set.of("SYS", "INFORMATION_SCHEMA"));
        } else if (dbType.contains("CALCITE")) {
            set.addAll(Set.of("METADATA"));
        }
        // 其他数据库按需扩展
        return set;
    }
}
