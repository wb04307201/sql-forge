package cn.wubo.sql.forge.jdbc;

/**
 * {@link AbstractSQL} 的具体实现，可直接 {@code new SQL()} 使用。
 */
public class SQL extends AbstractSQL<SQL> {

    @Override
    public SQL getSelf() {
        return this;
    }

}
