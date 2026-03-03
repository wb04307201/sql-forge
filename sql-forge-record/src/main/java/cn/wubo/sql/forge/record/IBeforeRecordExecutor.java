package cn.wubo.sql.forge.record;

public interface IBeforeRecordExecutor<T extends IAllowedRecord> {

    Boolean support(String tableName, T t);

    T before(String tableName, T t);
}
