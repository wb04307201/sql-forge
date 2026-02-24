package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.SqlScript;
import cn.wubo.sql.forge.utils.ExecutorUtils;
import cn.wubo.sql.forge.utils.MetaDataUtils;
import jakarta.validation.Valid;

import java.sql.Connection;
import java.sql.SQLException;
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
    public TreeNode<?> getMetaData() throws SQLException {
        try (Connection connection = CalciteExcutorUtils.getConnection(model)) {
            return MetaDataUtils.getMetaData(connection);
        }
    }
}
