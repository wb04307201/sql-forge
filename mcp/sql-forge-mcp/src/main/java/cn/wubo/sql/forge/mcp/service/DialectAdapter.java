package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.enums.DialectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库方言适配器
 * 根据目标数据库类型转换和优化SQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialectAdapter {

    // 方言缓存
    private final Map<DialectType, DialectConfig> dialectConfigs = new ConcurrentHashMap<>();

    // 函数转换映射
    private static final Map<DialectType, Map<String, String>> FUNCTION_CONVERSIONS = new ConcurrentHashMap<>();

    static {
        // MySQL 特殊函数转换
        Map<String, String> mysqlFunctions = Map.of(
                "NOW()", "NOW()",
                "CURRENT_TIMESTAMP", "NOW()",
                "STRING_AGG", "GROUP_CONCAT",
                "DATE_PART", "DATE_FORMAT"
        );

        // PostgreSQL 特殊函数转换
        Map<String, String> pgFunctions = Map.of(
                "GROUP_CONCAT", "STRING_AGG",
                "IFNULL", "COALESCE",
                "LIMIT ? OFFSET ?", "LIMIT ? OFFSET ?",
                "NOW()", "CURRENT_TIMESTAMP"
        );

        FUNCTION_CONVERSIONS.put(DialectType.MYSQL, mysqlFunctions);
        FUNCTION_CONVERSIONS.put(DialectType.POSTGRESQL, pgFunctions);
    }

    /**
     * 适配SQL到指定方言
     */
    public String adapt(String sql, DialectType targetDialect) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        String adaptedSql = sql.trim();

        // 1. 转换关键字
        adaptedSql = convertKeywords(adaptedSql, targetDialect);

        // 2. 转换函数
        adaptedSql = convertFunctions(adaptedSql, targetDialect);

        // 3. 转换数据类型
        adaptedSql = convertDataTypes(adaptedSql, targetDialect);

        // 4. 转换特殊语法
        adaptedSql = convertSpecialSyntax(adaptedSql, targetDialect);

        return adaptedSql;
    }

    /**
     * 转换关键字
     */
    private String convertKeywords(String sql, DialectType dialect) {
        switch (dialect) {
            case SQL_SERVER:
                // SQL Server 使用 TOP 而不是 LIMIT
                sql = convertLimitToTop(sql);
                break;
            case ORACLE:
                // Oracle 使用 ROWNUM 或 FETCH
                sql = convertLimitToRownum(sql);
                break;
            default:
                break;
        }
        return sql;
    }

    /**
     * 转换LIMIT为TOP (SQL Server)
     */
    private String convertLimitToTop(String sql) {
        Pattern limitPattern = Pattern.compile("(?i)LIMIT\\s+(\\d+)\\s*(?:OFFSET\\s+(\\d+))?", Pattern.DOTALL);
        Matcher matcher = limitPattern.matcher(sql);

        if (matcher.find()) {
            String limit = matcher.group(1);
            String offset = matcher.group(2);

            // 移除原LIMIT子句
            sql = sql.substring(0, matcher.start()) + sql.substring(matcher.end());

            // 在SELECT后添加TOP
            Pattern selectPattern = Pattern.compile("(?i)(SELECT)(\\s+(DISTINCT|ALL)?)", Pattern.DOTALL);
            Matcher selectMatcher = selectPattern.matcher(sql);
            if (selectMatcher.find()) {
                sql = sql.substring(0, selectMatcher.end(2)) +
                        " TOP(" + limit + ")" +
                        sql.substring(selectMatcher.end(2));
            }

            // 处理OFFSET
            if (offset != null) {
                sql += " OFFSET " + offset + " ROWS";
            }
        }

        return sql;
    }

    /**
     * 转换LIMIT为ROWNUM (Oracle)
     */
    private String convertLimitToRownum(String sql) {
        Pattern limitPattern = Pattern.compile("(?i)LIMIT\\s+(\\d+)\\s*(?:OFFSET\\s+(\\d+))?", Pattern.DOTALL);
        Matcher matcher = limitPattern.matcher(sql);

        if (matcher.find()) {
            String limit = matcher.group(1);
            String offset = matcher.group(2);
            int offsetInt = offset != null ? Integer.parseInt(offset) : 0;

            // 移除原LIMIT子句
            sql = sql.substring(0, matcher.start()) + sql.substring(matcher.end());

            if (offsetInt > 0) {
                // 使用子查询实现分页
                sql = String.format(
                        "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (%s) t WHERE ROWNUM <= %d) WHERE rn > %d",
                        sql,
                        offsetInt + Integer.parseInt(limit),
                        offsetInt
                );
            } else {
                // 简单添加ROWNUM限制
                if (!sql.contains("WHERE")) {
                    sql += " WHERE ROWNUM <= " + limit;
                } else {
                    sql = sql.replaceFirst("(?i)(WHERE\\s+)", "WHERE ROWNUM <= " + limit + " AND ");
                }
            }
        }

        return sql;
    }

    /**
     * 转换函数
     */
    private String convertFunctions(String sql, DialectType dialect) {
        Map<String, String> conversions = FUNCTION_CONVERSIONS.get(dialect);
        if (conversions == null) {
            return sql;
        }

        for (Map.Entry<String, String> entry : conversions.entrySet()) {
            sql = sql.replaceAll("(?i)" + Pattern.quote(entry.getKey()), entry.getValue());
        }

        return sql;
    }

    /**
     * 转换数据类型
     */
    private String convertDataTypes(String sql, DialectType dialect) {
        switch (dialect) {
            case ORACLE:
                // Oracle 没有 BOOLEAN 类型，使用 NUMBER(1)
                sql = sql.replaceAll("(?i)BOOLEAN", "NUMBER(1)");
                // Oracle 没有 DATETIME，使用 TIMESTAMP
                sql = sql.replaceAll("(?i)DATETIME", "TIMESTAMP");
                break;
            case SQL_SERVER:
                // SQL Server 使用 DATETIME2 而不是 DATETIME
                sql = sql.replaceAll("(?i)DATETIME\\b", "DATETIME2");
                break;
            case POSTGRESQL:
                // PostgreSQL 使用 SERIAL 而不是 AUTO_INCREMENT
                sql = sql.replaceAll("(?i)AUTO_INCREMENT", "SERIAL");
                // PostgreSQL 使用 TIMESTAMP WITHOUT TIME ZONE
                sql = sql.replaceAll("(?i)DATETIME", "TIMESTAMP");
                break;
            case MYSQL:
                // MySQL 使用 DATETIME
                break;
            default:
                break;
        }
        return sql;
    }

    /**
     * 转换特殊语法
     */
    private String convertSpecialSyntax(String sql, DialectType dialect) {
        switch (dialect) {
            case MYSQL:
                // MySQL 特定语法处理
                break;
            case POSTGRESQL:
                // PostgreSQL 字符串拼接使用 ||
                sql = convertConcatToPg(sql);
                break;
            case SQLITE:
                // SQLite 特殊处理
                sql = convertToSqlite(sql);
                break;
            default:
                break;
        }
        return sql;
    }

    /**
     * 转换CONCAT为PostgreSQL格式
     */
    private String convertConcatToPg(String sql) {
        // CONCAT(a, b, c) -> a || b || c
        Pattern concatPattern = Pattern.compile("CONCAT\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = concatPattern.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String[] parts = matcher.group(1).split(",");
            String pgConcat = String.join(" || ", parts).trim();
            matcher.appendReplacement(sb, pgConcat);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 转换为SQLite兼容语法
     */
    private String convertToSqlite(String sql) {
        // SQLite 不支持某些函数，需要替换
        sql = sql.replaceAll("(?i)GROUP_CONCAT", "GROUP_CONCAT");
        return sql;
    }

    /**
     * 获取方言配置
     */
    public DialectConfig getConfig(DialectType dialect) {
        return dialectConfigs.computeIfAbsent(dialect, DialectConfig::new);
    }

    /**
     * 获取分页SQL
     */
    public String getPaginatedSql(String baseSql, DialectType dialect, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return dialect.getLimitClause(offset, pageSize);
    }

    /**
     * 判断是否支持某功能
     */
    public boolean supportsFeature(DialectType dialect, String feature) {
        return switch (feature) {
            case "window_functions" -> dialect != DialectType.SQLITE || dialect != DialectType.H2;
            case "common_table_expressions" -> dialect != DialectType.MYSQL || dialect != DialectType.H2;
            case "recursive_cte" -> dialect == DialectType.POSTGRESQL || dialect == DialectType.ORACLE;
            case "full_outer_join" -> dialect != DialectType.MYSQL || dialect == DialectType.MYSQL;
            default -> true;
        };
    }

    /**
     * 方言配置类
     */
    @lombok.Data
    public static class DialectConfig {
        private final DialectType dialect;
        private boolean supportsWindowFunctions;
        private boolean supportsCTE;
        private boolean supportsRecursiveCTE;
        private boolean supportsFullOuterJoin;
        private String identifierQuote;
        private String stringQuote;
        private String dateLiteralPrefix;
        private String dateLiteralSuffix;

        public DialectConfig(DialectType dialect) {
            this.dialect = dialect;
            initializeConfig();
        }

        private void initializeConfig() {
            switch (dialect) {
                case MYSQL:
                    supportsWindowFunctions = true;
                    supportsCTE = true;
                    supportsRecursiveCTE = true;
                    supportsFullOuterJoin = true;
                    identifierQuote = "`";
                    stringQuote = "'";
                    dateLiteralPrefix = "'";
                    dateLiteralSuffix = "'";
                    break;
                case POSTGRESQL:
                    supportsWindowFunctions = true;
                    supportsCTE = true;
                    supportsRecursiveCTE = true;
                    supportsFullOuterJoin = true;
                    identifierQuote = "\"";
                    stringQuote = "'";
                    dateLiteralPrefix = "DATE '";
                    dateLiteralSuffix = "'";
                    break;
                case ORACLE:
                    supportsWindowFunctions = true;
                    supportsCTE = true;
                    supportsRecursiveCTE = true;
                    supportsFullOuterJoin = true;
                    identifierQuote = "\"";
                    stringQuote = "'";
                    dateLiteralPrefix = "DATE '";
                    dateLiteralSuffix = "'";
                    break;
                case SQL_SERVER:
                    supportsWindowFunctions = true;
                    supportsCTE = true;
                    supportsRecursiveCTE = true;
                    supportsFullOuterJoin = true;
                    identifierQuote = "[";
                    stringQuote = "'";
                    dateLiteralPrefix = "'";
                    dateLiteralSuffix = "'";
                    break;
                default:
                    supportsWindowFunctions = false;
                    supportsCTE = false;
                    supportsRecursiveCTE = false;
                    supportsFullOuterJoin = true;
                    identifierQuote = "\"";
                    stringQuote = "'";
                    dateLiteralPrefix = "'";
                    dateLiteralSuffix = "'";
                    break;
            }
        }
    }
}
