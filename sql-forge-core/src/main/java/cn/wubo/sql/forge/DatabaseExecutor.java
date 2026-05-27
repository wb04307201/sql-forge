package cn.wubo.sql.forge;


import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.DatabaseInfo;
import cn.wubo.sql.forge.records.EntireTable;
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

/**
 * 基于 Spring {@link DataSource} 的默认数据库执行器，执行器名称为 "database"。
 * <p>
 * 通过 {@link DataSourceUtils} 管理连接获取与释放，支持 Spring 事务绑定。
 * </p>
 *
 * @param dataSource Spring 数据源
 * @param properties SQL Forge 配置属性
 */
public record DatabaseExecutor(DataSource dataSource,
                               SqlForgeProperties properties) implements IExecutor {

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
    public DatabaseInfo getMetaDataDatabase() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getDatabase(connection);
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public List<String> getTableTypes() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getTableTypes(connection);
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public List<EntireTable> getMetaDataTables() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getMetaDataTables(connection, properties.getSchemata());
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public List<EntireTableInfo> getMetaDataTableInfos(String catalog, String schemaPattern, String tableNamePattern, String tableType) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return MetaDataUtils.getMetaDataTableInfos(connection, catalog, schemaPattern, tableNamePattern, tableType, properties.getSchemata());
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
