package cn.wubo.sql.forge.inter;


import cn.wubo.sql.forge.crud.IAllowedRecord;

public interface IExecute<T extends IAllowedRecord> {

    Boolean support(String tableName, T t);

    T before(String tableName, T t);
}
