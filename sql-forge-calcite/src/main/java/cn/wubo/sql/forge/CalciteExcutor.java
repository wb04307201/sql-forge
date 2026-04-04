package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.DatabaseInfo;
import cn.wubo.sql.forge.records.EntireTableInfo;
import cn.wubo.sql.forge.records.SqlScript;
import cn.wubo.sql.forge.utils.ExecutorUtils;
import cn.wubo.sql.forge.utils.MetaDataUtils;
import jakarta.validation.Valid;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public record CalciteExcutor(String model) implements IExecutor {

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
            return MetaDataUtils.getMetaDataTree(connection, Collections.emptyList());
        }
    }

    @Override
    public DatabaseInfo getMetaDataDatabase() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getDatabase(connection);
        }
    }

    @Override
    public List<EntireTableInfo> getMetaDataTables() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getMetaDataTables(connection, Collections.emptyList());
        }
    }
}
