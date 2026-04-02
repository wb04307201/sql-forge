package cn.wubo.sql.forge.mcp.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据库方言类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum DialectType {
    MYSQL("MySQL", "mysql") {
        @Override
        public String getLimitClause(int offset, int limit) {
            if (offset > 0) {
                return String.format(" LIMIT %d, %d", offset, limit);
            }
            return String.format(" LIMIT %d", limit);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return "CONCAT(" + String.join(", ", parts) + ")";
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("DATEDIFF(%s, %s)", endDate, startDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("DATE_FORMAT(%s, '%s')", dateExpr, format);
        }
    },

    POSTGRESQL("PostgreSQL", "postgresql") {
        @Override
        public String getLimitClause(int offset, int limit) {
            return String.format(" LIMIT %d OFFSET %d", limit, offset);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return "CONCAT(" + String.join(" || ", parts) + ")";
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("DATE_PART('day', %s::timestamp - %s::timestamp)", endDate, startDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("TO_CHAR(%s, '%s')", dateExpr, format);
        }
    },

    ORACLE("Oracle", "oracle") {
        @Override
        public String getLimitClause(int offset, int limit) {
            return String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return "CONCAT(" + String.join(", ", parts) + ")";
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("(%s - %s)", endDate, startDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("TO_CHAR(%s, '%s')", dateExpr, format);
        }
    },

    SQL_SERVER("SQL Server", "sqlserver") {
        @Override
        public String getLimitClause(int offset, int limit) {
            return String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return String.join(" + ", parts);
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("DATEDIFF(day, %s, %s)", startDate, endDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("FORMAT(%s, '%s')", dateExpr, format);
        }
    },

    H2("H2 Database", "h2") {
        @Override
        public String getLimitClause(int offset, int limit) {
            return String.format(" LIMIT %d OFFSET %d", limit, offset);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return "CONCAT(" + String.join(", ", parts) + ")";
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("DATEDIFF('%s', '%s')", "DAY", startDate, endDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("FORMATDATETIME(%s, '%s')", dateExpr, format);
        }
    },

    SQLITE("SQLite", "sqlite") {
        @Override
        public String getLimitClause(int offset, int limit) {
            return String.format(" LIMIT %d OFFSET %d", limit, offset);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return String.join(" || ", parts);
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("JULIANDAY(%s) - JULIANDAY(%s)", endDate, startDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("STRFTIME('%s', %s)", format, dateExpr);
        }
    },

    STANDARD_SQL("Standard SQL", "standard") {
        @Override
        public String getLimitClause(int offset, int limit) {
            return String.format(" LIMIT %d OFFSET %d", limit, offset);
        }

        @Override
        public String getConcatFunction(String... parts) {
            return "CONCAT(" + String.join(", ", parts) + ")";
        }

        @Override
        public String getDateDiffFunction(String startDate, String endDate) {
            return String.format("DATE_DIFF(%s, %s, DAY)", endDate, startDate);
        }

        @Override
        public String getDateFormatFunction(String dateExpr, String format) {
            return String.format("FORMAT_DATE('%s', %s)", format, dateExpr);
        }
    };

    private final String displayName;
    private final String identifier;

    /**
     * 获取分页子句
     */
    public abstract String getLimitClause(int offset, int limit);

    /**
     * 获取字符串拼接函数
     */
    public abstract String getConcatFunction(String... parts);

    /**
     * 获取日期差函数
     */
    public abstract String getDateDiffFunction(String startDate, String endDate);

    /**
     * 获取日期格式化函数
     */
    public abstract String getDateFormatFunction(String dateExpr, String format);

    /**
     * 根据数据库类型字符串识别方言
     */
    public static DialectType fromDatabaseType(String dbType) {
        if (dbType == null) return STANDARD_SQL;
        String lower = dbType.toLowerCase();
        if (lower.contains("mysql")) return MYSQL;
        if (lower.contains("postgresql") || lower.contains("postgres")) return POSTGRESQL;
        if (lower.contains("oracle")) return ORACLE;
        if (lower.contains("sqlserver") || lower.contains("mssql")) return SQL_SERVER;
        if (lower.contains("h2")) return H2;
        if (lower.contains("sqlite")) return SQLITE;
        return STANDARD_SQL;
    }

    /**
     * 根据产品名称识别方言
     */
    public static DialectType fromProductName(String productName) {
        if (productName == null) return STANDARD_SQL;
        return fromDatabaseType(productName);
    }
}
