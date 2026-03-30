package cn.wubo.sql.forge;


import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.DatabaseInfo;
import cn.wubo.sql.forge.records.EntireTableInfo;
import cn.wubo.sql.forge.records.SqlScript;
import cn.wubo.sql.forge.utils.ExecutorUtils;
import cn.wubo.sql.forge.utils.MetaDataUtils;
import jakarta.validation.Valid;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public record DatabaseExecutor(DataSource dataSource, SqlForgeProperties properties) implements IExecutor {

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
    public TreeNode<DatabaseInfo> getMetaDataTree() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getMetaDataTree(connection, properties.getSchemata());
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public List<EntireTableInfo> getMetaDataTables() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getMetaDataTables(connection, properties.getSchemata());
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
