package cn.wubo.sql.forge.enums;

import cn.wubo.sql.forge.jdbc.SafeAppendable;

/**
 * SQL 行数限制策略枚举，支持 ISO 标准和 LIMIT/OFFSET 两种语法。
 */
public enum LimitingRowsStrategy {
    /** 不添加行数限制 */
    NOP {
        @Override
        public void appendClause(SafeAppendable builder, String offset, String limit) {
        }
    },
    /** ISO 标准语法（OFFSET ... ROWS FETCH FIRST ... ROWS ONLY） */
    ISO {
        @Override
        public void appendClause(SafeAppendable builder, String offset, String limit) {
            if (offset != null) {
                builder.append(" OFFSET ").append(offset).append(" ROWS");
            }
            if (limit != null) {
                builder.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
            }
        }
    },
    /** LIMIT/OFFSET 语法（MySQL、PostgreSQL 等） */
    OFFSET_LIMIT {
        @Override
        public void appendClause(SafeAppendable builder, String offset, String limit) {
            if (limit != null) {
                builder.append(" LIMIT ").append(limit);
            }
            if (offset != null) {
                builder.append(" OFFSET ").append(offset);
            }
        }
    };

    /**
     * 将行数限制子句追加到 SQL 构建器。
     *
     * @param builder SQL 构建器
     * @param offset  偏移量
     * @param limit   限制行数
     */
    public abstract void appendClause(SafeAppendable builder, String offset, String limit);
}
