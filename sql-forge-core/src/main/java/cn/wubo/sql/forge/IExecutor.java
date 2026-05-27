package cn.wubo.sql.forge;

import cn.wubo.sql.forge.enums.DatasourceType;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.DatabaseInfo;
import cn.wubo.sql.forge.records.EntireTable;
import cn.wubo.sql.forge.records.EntireTableInfo;
import cn.wubo.sql.forge.records.SqlScript;
import jakarta.validation.Valid;

import java.sql.SQLException;
import java.util.List;

public interface IExecutor {

    String getExecutorName();

    List<RowMap> executeQuery(SqlScript sqlScript) throws SQLException;

    RowMap executeInsert(SqlScript sqlScript) throws SQLException;

    int executeUpdate(SqlScript sqlScript) throws SQLException;

    Object execute(@Valid SqlScript sqlScript) throws SQLException;

    TreeNode<DatabaseInfo> getMetaDataTree() throws SQLException;

    DatabaseInfo getMetaDataDatabase() throws SQLException;

    List<String> getTableTypes() throws SQLException;

    List<EntireTable> getMetaDataTables() throws SQLException;

    List<EntireTableInfo> getMetaDataTableInfos(String catalog, String schemaPattern, String tableNamePattern, String tableType) throws SQLException;
}
