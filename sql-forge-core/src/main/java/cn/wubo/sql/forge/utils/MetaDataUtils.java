package cn.wubo.sql.forge.utils;

import cn.wubo.sql.forge.TreeNode;
import cn.wubo.sql.forge.records.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.wubo.sql.forge.constant.Constant.NODE_VALUE_TEMPLATE;

/**
 * 数据库元数据工具类，通过 JDBC {@link DatabaseMetaData} 查询 Schema、表、列、主键、外键、索引等元数据信息。
 */
@Slf4j
@UtilityClass
public class MetaDataUtils {

    /**
     * 获取当前数据库产品和版本信息。
     */
    public DatabaseInfo getDatabase(Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        return new DatabaseInfo(
                databaseMetaData.getDatabaseProductName(),
                databaseMetaData.getDatabaseProductVersion()
        );
    }

    /**
     * 获取指定 catalog 下的 Schema 列表。
     */
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

    /**
     * 获取数据库支持的表类型列表（如 TABLE、VIEW）。
     */
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

    /**
     * 获取符合条件的表基本信息列表。
     */
    public List<TableInfo> getTables(Connection connection, String catalog, String schemaPattern,
                                     String tableNamePattern, String[] types) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet rs = databaseMetaData.getTables(catalog, schemaPattern, tableNamePattern, types)) {
            List<TableInfo> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(new TableInfo(
                        rs.getString("TABLE_NAME"),
                        rs.getString("TABLE_SCHEM"),
                        rs.getString("TABLE_TYPE"),
                        rs.getString("REMARKS")
                ));
            }
            return tables;
        }
    }

    /**
     * 获取指定表的列信息列表。
     */
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

    /**
     * 获取指定表的主键信息列表，支持复合主键合并。
     */
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

    /**
     * 获取指定表的外键（导入键）信息列表。
     */
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

    /**
     * 获取指定表的索引信息列表，支持复合索引列合并。
     */
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

    /**
     * 构建完整的数据库元数据树（数据库 → Schema → 表类型 → 表 → 列/主键/外键/索引）。
     *
     * @param connection 数据库连接
     * @param schemata   需要过滤的 Schema 名称列表，为空则包含所有
     * @return 元数据树根节点
     */
    public TreeNode<DatabaseInfo> getMetaDataTree(Connection connection, List<String> schemata) throws SQLException {
        TreeNode<DatabaseInfo> root = new TreeNode<>();
        DatabaseInfo databaseInfo = getDatabase(connection);

        root.setLabel(databaseInfo.productName());
        root.setValue(databaseInfo.productName());
        root.setData(databaseInfo);

        List<SchemaInfo> schemas = getSchemas(connection, null, null)
                .stream()
                .filter(s -> schemata.isEmpty() || schemata.contains(s.tableSchema()))
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
                if (!tables.isEmpty()) {
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

                        for (String columnName : primaryKey.columnName()) {
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

                        for (String columnName : index.columnName()) {
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

    /**
     * 获取所有表的基本信息列表（表名、Schema、类型、备注）。
     */
    public List<EntireTable> getMetaDataTables(Connection connection, List<String> schemata) throws SQLException {
        List<EntireTable> entireTables = new ArrayList<>();

        List<SchemaInfo> schemas = getSchemas(connection, null, null)
                .stream()
                .filter(s -> schemata.isEmpty() || schemata.contains(s.tableSchema()))
                .toList();

        List<String> tableTypes = new ArrayList<>();
        for (SchemaInfo schema : schemas) {
            if (tableTypes.isEmpty())
                tableTypes = getTableTypes(connection);
            for (String tableType : tableTypes) {
                List<TableInfo> tables = getTables(connection, null, schema.tableSchema(), null, new String[]{tableType});
                for (TableInfo table : tables) {
                    entireTables.add(
                            new EntireTable(
                                    table.tableName(),
                                    schema.tableSchema(),
                                    table.tableType(),
                                    table.remarks()
                            )
                    );
                }
            }
        }

        return entireTables;
    }

    /**
     * 获取指定表的完整元数据信息（列、主键、外键、索引）。
     */
    public List<EntireTableInfo> getMetaDataTableInfos(Connection connection, String catalog, String schemaPattern, String tableNamePattern, String tableType, List<String> schemata) throws SQLException {
        List<EntireTableInfo> entireTableInfos = new ArrayList<>();

        List<TableInfo> tables = getTables(connection, catalog, schemaPattern, tableNamePattern, new String[]{tableType});

        for (TableInfo table : tables) {
            entireTableInfos.add(
                    new EntireTableInfo(
                            table.tableName(),
                            table.tableSchema(),
                            table.tableType(),
                            table.remarks(),
                            getColumns(connection, catalog, table.tableSchema(), table.tableName(), null),
                            getPrimaryKeys(connection, catalog, table.tableSchema(), table.tableName()),
                            getImportedKeys(connection, catalog, table.tableSchema(), table.tableName()),
                            getIndexInfo(connection, catalog, table.tableSchema(), table.tableName(), false, false)
                    )
            );
        }

        return entireTableInfos;
    }
}
