package cn.wubo.sql.forge;


import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.SqlScript;
import cn.wubo.sql.forge.utils.ExecutorUtils;
import cn.wubo.sql.forge.utils.MetaDataUtils;
import jakarta.validation.Valid;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

public record DatabaseExecutor(DataSource dataSource) implements IExecutor {

    @Override
    public String getExecutorName() {
        return "database";
    }

    @Override
    public List<RowMap> executeQuery(@Valid SqlScript sqlScript) throws
            SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return ExecutorUtils.executeQuery(connection, sqlScript);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public RowMap executeInsert(@Valid SqlScript sqlScript) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return ExecutorUtils.executeInsert(connection, sqlScript);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public int executeUpdate(@Valid SqlScript sqlScript) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return ExecutorUtils.executeUpdate(connection, sqlScript);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    public Object execute(@Valid SqlScript sqlScript) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return ExecutorUtils.execute(connection, sqlScript);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public TreeNode<?> getMetaData() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getMetaData(connection);
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
