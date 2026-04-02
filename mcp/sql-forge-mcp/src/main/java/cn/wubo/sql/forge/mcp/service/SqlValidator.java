package cn.wubo.sql.forge.mcp.service;

import cn.wubo.sql.forge.mcp.model.SqlValidationResult;
import cn.wubo.sql.forge.mcp.model.SqlValidationResult.ValidationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL验证服务
 * 负责验证生成SQL的正确性、安全性和性能
 */
@Slf4j
@Service
public class SqlValidator {

    // SQL注入风险关键词
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "--", ";", "/*", "*/", "xp_", "sp_", "exec", "execute",
            "drop", "truncate", "delete from", "alter table",
            "insert into", "update ", "create table", "grant",
            "revoke", "shutdown"
    );

    // SELECT 语句模式
    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "(?i)^\\s*SELECT\\s+.*\\s+FROM\\s+.*", Pattern.DOTALL
    );

    // INSERT 语句模式
    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "(?i)^\\s*INSERT\\s+INTO\\s+.*", Pattern.DOTALL
    );

    // UPDATE 语句模式
    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "(?i)^\\s*UPDATE\\s+\\w+\\s+SET\\s+.*", Pattern.DOTALL
    );

    // DELETE 语句模式
    private static final Pattern DELETE_PATTERN = Pattern.compile(
            "(?i)^\\s*DELETE\\s+FROM\\s+.*", Pattern.DOTALL
    );

    // 子查询模式
    private static final Pattern SUBQUERY_PATTERN = Pattern.compile(
            "(?i)\\(\\s*SELECT\\s+.*\\s+FROM\\s+.*\\s*\\)", Pattern.DOTALL
    );

    // JOIN 模式
    private static final Pattern JOIN_PATTERN = Pattern.compile(
            "(?i)\\s+(INNER\\s+|LEFT\\s+|RIGHT\\s+|FULL\\s+)?JOIN\\s+\\w+", Pattern.DOTALL
    );

    // 聚合函数
    private static final Pattern AGGREGATE_PATTERN = Pattern.compile(
            "(?i)(COUNT|SUM|AVG|MIN|MAX)\\s*\\([^)]*\\)", Pattern.DOTALL
    );

    // ORDER BY 模式
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
            "(?i)\\s+ORDER\\s+BY\\s+.*", Pattern.DOTALL
    );

    // GROUP BY 模式
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile(
            "(?i)\\s+GROUP\\s+BY\\s+.*", Pattern.DOTALL
    );

    // LIMIT 模式
    private static final Pattern LIMIT_PATTERN = Pattern.compile(
            "(?i)\\s+(LIMIT|TOP|FETCH)\\s+.*", Pattern.DOTALL
    );

    // WHERE 子句模式
    private static final Pattern WHERE_PATTERN = Pattern.compile(
            "(?i)\\s+WHERE\\s+.*", Pattern.DOTALL
    );

    // 危险的字符串拼接模式
    private static final Pattern STRING_CONCAT_PATTERN = Pattern.compile(
            "'\\s*\\+\\s*'|CONCAT\\s*\\("
    );

    // UNION 模式
    private static final Pattern UNION_PATTERN = Pattern.compile(
            "(?i)\\s+UNION\\s+(ALL\\s+)?SELECT\\s+", Pattern.DOTALL
    );

    // WITH 递归 CTE 模式
    private static final Pattern RECURSIVE_CTE_PATTERN = Pattern.compile(
            "(?i)WITH\\s+RECURSIVE\\s+", Pattern.DOTALL
    );

    /**
     * 完整验证
     */
    public SqlValidationResult validate(String sql) {
        SqlValidationResult result = SqlValidationResult.builder()
                .valid(true)
                .validationType(ValidationType.COMPLETE)
                .build();

        if (sql == null || sql.trim().isEmpty()) {
            result.addError("SQL语句为空");
            return result;
        }

        String trimmedSql = sql.trim();

        // 1. 安全性验证
        validateSecurity(trimmedSql, result);

        // 2. 语法验证
        validateSyntax(trimmedSql, result);

        // 3. 语义验证
        validateSemantics(trimmedSql, result);

        // 4. 性能验证
        validatePerformance(trimmedSql, result);

        return result;
    }

    /**
     * 安全性验证
     */
    private void validateSecurity(String sql, SqlValidationResult result) {
        String lowerSql = sql.toLowerCase();

        // 检查危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lowerSql.contains(keyword.toLowerCase())) {
                // 特殊处理 SELECT 和 FROM 的组合，这是正常的
                if (keyword.equalsIgnoreCase("from") && lowerSql.contains("select")) {
                    continue;
                }
                result.addError("检测到潜在危险关键词: " + keyword);
            }
        }

        // 检查字符串拼接（可能的SQL注入）
        if (STRING_CONCAT_PATTERN.matcher(sql).find()) {
            result.addWarning("检测到字符串拼接操作，请确保参数已正确转义");
        }

        // 检查注释
        if (sql.contains("--") || sql.contains("/*")) {
            result.addWarning("SQL语句包含注释，可能影响某些数据库兼容性");
        }

        // 检查是否有分号分隔的多条语句
        if (sql.contains(";")) {
            String[] statements = sql.split(";");
            if (statements.length > 1) {
                for (int i = 1; i < statements.length; i++) {
                    String stmt = statements[i].trim();
                    if (!stmt.isEmpty()) {
                        result.addWarning("检测到多条SQL语句，可能存在SQL注入风险");
                        break;
                    }
                }
            }
        }
    }

    /**
     * 语法验证
     */
    private void validateSyntax(String sql, SqlValidationResult result) {
        String lowerSql = sql.toLowerCase();
        boolean validStatement = false;

        // 检查是否是有效的SQL语句
        if (SELECT_PATTERN.matcher(sql).matches()) {
            validStatement = true;
            validateSelectSyntax(sql, result);
        } else if (INSERT_PATTERN.matcher(sql).matches()) {
            validStatement = true;
            validateInsertSyntax(sql, result);
        } else if (UPDATE_PATTERN.matcher(sql).matches()) {
            validStatement = true;
            validateUpdateSyntax(sql, result);
        } else if (DELETE_PATTERN.matcher(sql).matches()) {
            validStatement = true;
            validateDeleteSyntax(sql, result);
        }

        if (!validStatement) {
            result.addError("无法识别的SQL语句类型");
        }
    }

    /**
     * 验证SELECT语句语法
     */
    private void validateSelectSyntax(String sql, SqlValidationResult result) {
        // 检查SELECT ... FROM
        if (!Pattern.compile("(?i)SELECT\\s+.*\\s+FROM\\s+").matcher(sql).find()) {
            result.addError("SELECT语句缺少FROM子句");
        }

        // 检查GROUP BY后是否有HAVING
        boolean hasGroupBy = GROUP_BY_PATTERN.matcher(sql).find();
        boolean hasAggregate = AGGREGATE_PATTERN.matcher(sql).find();

        if (hasGroupBy && !hasAggregate) {
            result.addWarning("GROUP BY语句中建议包含聚合函数或用于SELECT中");
        }

        // 检查HAVING是否与GROUP BY配合使用
        boolean hasHaving = Pattern.compile("(?i)\\s+HAVING\\s+").matcher(sql).find();
        if (hasHaving && !hasGroupBy) {
            result.addError("HAVING子句必须与GROUP BY子句配合使用");
        }

        // 检查括号匹配
        if (!isBalanced(sql, '(', ')')) {
            result.addError("SQL语句括号不匹配");
        }

        // 检查引号匹配
        if (!isQuoteBalanced(sql)) {
            result.addError("SQL语句引号不匹配");
        }
    }

    /**
     * 验证INSERT语句语法
     */
    private void validateInsertSyntax(String sql, SqlValidationResult result) {
        if (!Pattern.compile("(?i)INSERT\\s+INTO\\s+\\w+\\s*\\(").matcher(sql).find()) {
            result.addWarning("INSERT语句建议指定插入的列名");
        }
    }

    /**
     * 验证UPDATE语句语法
     */
    private void validateUpdateSyntax(String sql, SqlValidationResult result) {
        if (!Pattern.compile("(?i)UPDATE\\s+\\w+\\s+SET\\s+\\w+\\s*=").matcher(sql).find()) {
            result.addError("UPDATE语句格式不正确");
        }

        // 检查UPDATE是否有WHERE
        if (!WHERE_PATTERN.matcher(sql).find()) {
            result.addWarning("UPDATE语句建议包含WHERE条件，否则将更新所有记录");
        }
    }

    /**
     * 验证DELETE语句语法
     */
    private void validateDeleteSyntax(String sql, SqlValidationResult result) {
        if (!WHERE_PATTERN.matcher(sql).find()) {
            result.addError("DELETE语句缺少WHERE条件，将删除所有记录");
        }
    }

    /**
     * 语义验证
     */
    private void validateSemantics(String sql, SqlValidationResult result) {
        // 检查SELECT * 的使用
        if (Pattern.compile("(?i)SELECT\\s+\\*").matcher(sql).find()) {
            result.addWarning("使用SELECT * 可能返回不需要的字段，影响性能");
        }

        // 检查GROUP BY 与 SELECT字段的关系
        String groupByFields = extractGroupByFields(sql);
        if (groupByFields != null && !groupByFields.isEmpty()) {
            Set<String> nonAggregateSelectFields = extractNonAggregateSelectFields(sql);
            for (String field : nonAggregateSelectFields) {
                if (!containsGroupByField(field, groupByFields)) {
                    result.addError("SELECT字段 '" + field + "' 既不在聚合函数中，也不在GROUP BY子句中");
                }
            }
        }

        // 检查JOIN条件
        Matcher joinMatcher = JOIN_PATTERN.matcher(sql);
        int joinCount = 0;
        while (joinMatcher.find()) {
            joinCount++;
        }
        if (joinCount > 0) {
            // 检查是否有足够的JOIN条件
            Matcher onMatcher = Pattern.compile("(?i)\\s+ON\\s+").matcher(sql);
            int onCount = 0;
            while (onMatcher.find()) {
                onCount++;
            }
            if (onCount < joinCount) {
                result.addWarning("JOIN数量(" + joinCount + ")与ON条件数量(" + onCount + ")不匹配");
            }
        }
    }

    /**
     * 性能验证
     */
    private void validatePerformance(String sql, SqlValidationResult result) {
        String lowerSql = sql.toLowerCase();

        // 检查是否有限制返回行数
        if (!LIMIT_PATTERN.matcher(sql).find() && !lowerSql.contains("top ")) {
            // 如果没有LIMIT但有聚合函数，可能可以接受
            if (!AGGREGATE_PATTERN.matcher(sql).find()) {
                result.addWarning("查询未限制返回行数，可能返回大量数据");
            }
        }

        // 检查子查询嵌套层级
        int subqueryDepth = countSubqueryDepth(sql);
        if (subqueryDepth > 3) {
            result.addWarning("子查询嵌套层级过深(" + subqueryDepth + ")，建议优化");
        }

        // 检查是否使用SELECT DISTINCT
        if (Pattern.compile("(?i)SELECT\\s+DISTINCT").matcher(sql).find()) {
            result.addWarning("使用DISTINCT可能影响查询性能");
        }

        // 检查LIKE以通配符开头
        if (Pattern.compile("(?i)LIKE\\s+'%").matcher(sql).find()) {
            result.addWarning("LIKE模式以通配符开头无法使用索引");
        }

        // 检查OR条件
        if (Pattern.compile("(?i)\\s+OR\\s+").matcher(sql).find()) {
            result.addWarning("使用OR条件可能无法有效使用索引，考虑使用UNION");
        }

        // 检查函数在WHERE子句中使用
        if (Pattern.compile("(?i)WHERE\\s+.*\\([^)]*\\)\\s*[=<>]").matcher(sql).find()) {
            result.addWarning("WHERE子句中对字段使用函数可能无法使用索引");
        }
    }

    /**
     * 检查括号是否匹配
     */
    private boolean isBalanced(String sql, char open, char close) {
        int count = 0;
        for (char c : sql.toCharArray()) {
            if (c == open) count++;
            if (c == close) count--;
            if (count < 0) return false;
        }
        return count == 0;
    }

    /**
     * 检查引号是否匹配
     */
    private boolean isQuoteBalanced(String sql) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char prev = i > 0 ? sql.charAt(i - 1) : 0;

            // 跳过转义
            if (prev == '\\') continue;

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
        }

        return !inSingleQuote && !inDoubleQuote;
    }

    /**
     * 提取GROUP BY字段
     */
    private String extractGroupByFields(String sql) {
        Matcher matcher = GROUP_BY_PATTERN.matcher(sql);
        if (matcher.find()) {
            String groupByClause = matcher.group();
            return groupByClause.replaceAll("(?i)^\\s*GROUP\\s+BY\\s+", "");
        }
        return null;
    }

    /**
     * 提取SELECT中的非聚合字段
     */
    private Set<String> extractNonAggregateSelectFields(String sql) {
        Set<String> fields = new HashSet<>();
        Pattern selectPattern = Pattern.compile("(?i)SELECT\\s+(.*?)\\s+FROM", Pattern.DOTALL);
        Matcher matcher = selectPattern.matcher(sql);
        if (matcher.find()) {
            String selectClause = matcher.group(1);
            String[] parts = selectClause.split(",");
            for (String part : parts) {
                part = part.trim();
                if (!AGGREGATE_PATTERN.matcher(part).find() && !part.equals("*")) {
                    // 提取字段名
                    String fieldName = part.contains(".") ?
                            part.substring(part.lastIndexOf(".") + 1).trim() :
                            part.trim();
                    fieldName = fieldName.replaceAll("\\s+as\\s+.*$", "").trim();
                    fields.add(fieldName);
                }
            }
        }
        return fields;
    }

    /**
     * 检查字段是否在GROUP BY中
     */
    private boolean containsGroupByField(String field, String groupByFields) {
        return groupByFields.toLowerCase().contains(field.toLowerCase());
    }

    /**
     * 计算子查询深度
     */
    private int countSubqueryDepth(String sql) {
        int maxDepth = 0;
        int currentDepth = 0;

        for (int i = 0; i < sql.length(); i++) {
            if (sql.substring(i).matches("(?i)\\(\\s*SELECT\\s+.*")) {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (sql.charAt(i) == ')' && i > 0 && sql.substring(0, i).contains("SELECT")) {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * 尝试修正SQL
     */
    public String tryCorrect(String sql) {
        String corrected = sql.trim();

        // 移除末尾的分号
        while (corrected.endsWith(";")) {
            corrected = corrected.substring(0, corrected.length() - 1);
        }

        // 统一大写关键词
        corrected = corrected.replaceAll("(?i)\\bselect\\b", "SELECT");
        corrected = corrected.replaceAll("(?i)\\bfrom\\b", "FROM");
        corrected = corrected.replaceAll("(?i)\\bwhere\\b", "WHERE");
        corrected = corrected.replaceAll("(?i)\\band\\b", "AND");
        corrected = corrected.replaceAll("(?i)\\bor\\b", "OR");

        return corrected;
    }
}
