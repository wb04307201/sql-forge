package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.DatabaseInfo;
import cn.wubo.sql.forge.records.EntireTable;
import cn.wubo.sql.forge.records.EntireTableInfo;
import cn.wubo.sql.forge.records.SqlScript;
import cn.wubo.sql.forge.utils.ExecutorUtils;
import cn.wubo.sql.forge.utils.MetaDataUtils;
import jakarta.validation.Valid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 基于 Apache Calcite 的跨库联邦查询执行器，仅支持查询操作（executeQuery），不支持插入和更新。执行器名称为 "calcite"。
 */
public record CalciteExcutor(String model,
                             SqlForgeProperties properties) implements IExecutor {

    @Override
    public String getExecutorName() {
        return "calcite";
    }

    @Override
    public List<RowMap> executeQuery(@Valid SqlScript sqlScript) throws
            SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return ExecutorUtils.executeQuery(connection, sqlScript);
        }
    }

    @Override
    public RowMap executeInsert(SqlScript sqlScript) throws SQLException {
        throw new SQLException("CalciteExcutor not support executeInsert");
    }

    @Override
    public int executeUpdate(SqlScript sqlScript) throws SQLException {
        throw new SQLException("CalciteExcutor not support executeUpdate");
    }

    @Override
    public Object execute(SqlScript sqlScript) throws SQLException {
        return executeQuery(sqlScript);
    }

    @Override
    public TreeNode<DatabaseInfo> getMetaDataTree() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getMetaDataTree(connection, properties.getCalcite().getSchemata());
        }
    }

    @Override
    public DatabaseInfo getMetaDataDatabase() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getDatabase(connection);
        }
    }

    @Override
    public List<String> getTableTypes() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getTableTypes(connection);
        }
    }

    @Override
    public List<EntireTable> getMetaDataTables() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getMetaDataTables(connection, properties.getCalcite().getSchemata());
        }
    }

    @Override
    public List<EntireTableInfo> getMetaDataTableInfos(String catalog, String schemaPattern, String tableNamePattern, String tableType) throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getMetaDataTableInfos(connection, catalog, schemaPattern, tableNamePattern, tableType, properties.getCalcite().getSchemata());
        }
    }
}
