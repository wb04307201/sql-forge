package cn.wubo.sql.forge.record;

/**
 * Record 操作前置拦截器接口，在 SQL 执行前对操作记录进行拦截和增强。
 * <p>
 * 可按操作类型（Delete/Insert/Select/SelectPage/Update）实现对应的拦截器，
 * 用于权限校验、数据脱敏、自动填充等场景。
 * </p>
 *
 * @param <T> 操作记录类型
 */
public interface IBeforeRecordExecutor<T extends IAllowedRecord> {

    /**
     * 判断是否支持拦截当前表和操作。
     *
     * @param tableName 表名
     * @param t         操作记录
     * @return 是否支持
     */
    Boolean support(String tableName, T t);

    /**
     * 在 SQL 执行前对操作记录进行增强处理。
     *
     * @param tableName 表名
     * @param t         原始操作记录
     * @return 增强后的操作记录
     */
    T before(String tableName, T t);
}
