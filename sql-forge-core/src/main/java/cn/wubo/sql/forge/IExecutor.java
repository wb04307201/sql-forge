package cn.wubo.sql.forge;

import cn.wubo.sql.forge.map.RowMap;
import jakarta.validation.Valid;

import java.sql.SQLException;
import java.util.List;

/**
 * 数据库执行器核心接口，定义 SQL 执行与元数据查询的统一抽象。
 * <p>
 * 所有数据源实现（如 {@link DatabaseExecutor}、CalciteExcutor）均需实现此接口，
 * 由 {@link ExecutorService} 统一管理并按名称路由。
 * </p>
 */
public interface IExecutor {

    /**
     * 获取执行器名称，用于 {@link ExecutorService} 路由标识。
     *
     * @return 执行器名称，如 "database"、"calcite"
     */
    String getExecutorName();

    /**
     * 执行查询 SQL，返回结果集。
     *
     * @param sqlScript 包含 SQL 语句和参数的脚本对象
     * @return 查询结果列表，每行为一个 {@link cn.wubo.sql.forge.map.RowMap}
     * @throws SQLException SQL 执行异常
     */
    List<RowMap> executeQuery(SqlScript sqlScript) throws SQLException;

    /**
     * 执行插入 SQL，返回生成的主键。
     *
     * @param sqlScript 包含 SQL 语句和参数的脚本对象
     * @return 包含自增主键的行映射
     * @throws SQLException SQL 执行异常
     */
    RowMap executeInsert(SqlScript sqlScript) throws SQLException;

    /**
     * 执行更新 SQL，返回受影响行数。
     *
     * @param sqlScript 包含 SQL 语句和参数的脚本对象
     * @return 受影响的行数
     * @throws SQLException SQL 执行异常
     */
    int executeUpdate(SqlScript sqlScript) throws SQLException;

    /**
     * 通用执行方法，根据 SQL 类型返回查询结果或更新行数。
     *
     * @param sqlScript 包含 SQL 语句和参数的脚本对象
     * @return 查询结果为 {@code List<RowMap>}，更新/删除/插入结果为受影响行数
     * @throws SQLException SQL 执行异常
     */
    Object execute(@Valid SqlScript sqlScript) throws SQLException;

    /**
     * 获取数据库元数据树形结构（数据库 → Schema → 表）。
     *
     * @return 元数据树根节点
     * @throws SQLException SQL 执行异常
     */
    TreeNode<DatabaseInfo> getMetaDataTree() throws SQLException;

    /**
     * 获取当前数据库基本信息（产品名称和版本）。
     *
     * @return 数据库信息
     * @throws SQLException SQL 执行异常
     */
    DatabaseInfo getMetaDataDatabase() throws SQLException;


    /**
     * 获取所有表的基本信息列表。
     *
     * @return 表信息列表
     * @throws SQLException SQL 执行异常
     */
    List<TableInfo> getMetaDataTables() throws SQLException;

    /**
     * 获取指定表的详细元数据信息（列、主键、外键、索引）。
     *
     * @param catalog           目录名称
     * @param schemaPattern     Schema 匹配模式
     * @param tableNamePattern  表名匹配模式
     * @param tableType         表类型
     * @return 表详细元数据信息列表
     * @throws SQLException SQL 执行异常
     */
    List<TableDefinitionInfo> getMetaDataDefinitions(String catalog, String schemaPattern, String tableNamePattern, String tableType) throws SQLException;
}
