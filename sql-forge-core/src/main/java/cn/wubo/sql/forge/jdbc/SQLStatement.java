package cn.wubo.sql.forge.jdbc;

import cn.wubo.sql.forge.enums.LimitingRowsStrategy;
import cn.wubo.sql.forge.enums.StatementType;

import java.util.ArrayList;
import java.util.List;

import static cn.wubo.sql.forge.Constant.*;

public class SQLStatement {

    StatementType statementType;
    List<String> sets = new ArrayList<>();
    List<String> select = new ArrayList<>();
    List<String> tables = new ArrayList<>();
    List<String> join = new ArrayList<>();
    List<String> innerJoin = new ArrayList<>();
    List<String> outerJoin = new ArrayList<>();
    List<String> leftOuterJoin = new ArrayList<>();
    List<String> rightOuterJoin = new ArrayList<>();
    List<String> where = new ArrayList<>();
    List<String> having = new ArrayList<>();
    List<String> groupBy = new ArrayList<>();
    List<String> orderBy = new ArrayList<>();
    List<String> lastList = new ArrayList<>();
    List<String> columns = new ArrayList<>();
    List<List<String>> valuesList = new ArrayList<>();
    boolean distinct;
    String offset;
    String limit;
    LimitingRowsStrategy limitingRowsStrategy = LimitingRowsStrategy.NOP;

    public SQLStatement() {
        valuesList.add(new ArrayList<>());
    }

    private void sqlClause(SafeAppendable builder, String keyword, List<String> parts, String open, String close,
                           String conjunction) {
        if (!parts.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(keyword);
            builder.append(" ");
            builder.append(open);
            String last = "________";
            for (int i = 0, n = parts.size(); i < n; i++) {
                String part = parts.get(i);
                if (i > 0 && !PAREN_AND.equals(part) && !PAREN_OR.equals(part) && !PAREN_AND.equals(last) && !PAREN_OR.equals(last)) {
                    builder.append(conjunction);
                }
                builder.append(part);
                last = part;
            }
            builder.append(close);
        }
    }

    private void selectSQL(SafeAppendable builder) {
        if (distinct) {
            sqlClause(builder, "SELECT DISTINCT", select, "", "", ", ");
        } else {
            sqlClause(builder, "SELECT", select, "", "", ", ");
        }

        sqlClause(builder, "FROM", tables, "", "", ", ");
        joins(builder);
        sqlClause(builder, "WHERE", where, "(", ")", " AND ");
        sqlClause(builder, "GROUP BY", groupBy, "", "", ", ");
        sqlClause(builder, "HAVING", having, "(", ")", " AND ");
        sqlClause(builder, "ORDER BY", orderBy, "", "", ", ");
        limitingRowsStrategy.appendClause(builder, offset, limit);
    }

    private void joins(SafeAppendable builder) {
        sqlClause(builder, "JOIN", join, "", "", "\nJOIN ");
        sqlClause(builder, "INNER JOIN", innerJoin, "", "", "\nINNER JOIN ");
        sqlClause(builder, "OUTER JOIN", outerJoin, "", "", "\nOUTER JOIN ");
        sqlClause(builder, "LEFT OUTER JOIN", leftOuterJoin, "", "", "\nLEFT OUTER JOIN ");
        sqlClause(builder, "RIGHT OUTER JOIN", rightOuterJoin, "", "", "\nRIGHT OUTER JOIN ");
    }

    private void insertSQL(SafeAppendable builder) {
        sqlClause(builder, "INSERT INTO", tables, "", "", "");
        sqlClause(builder, "", columns, "(", ")", ", ");
        for (int i = 0; i < valuesList.size(); i++) {
            sqlClause(builder, i > 0 ? "," : "VALUES", valuesList.get(i), "(", ")", ", ");
        }
    }

    private void deleteSQL(SafeAppendable builder) {
        sqlClause(builder, "DELETE FROM", tables, "", "", "");
        sqlClause(builder, "WHERE", where, "(", ")", " AND ");
        limitingRowsStrategy.appendClause(builder, null, limit);
    }

    private void updateSQL(SafeAppendable builder) {
        sqlClause(builder, "UPDATE", tables, "", "", "");
        joins(builder);
        sqlClause(builder, "SET", sets, "", "", ", ");
        sqlClause(builder, "WHERE", where, "(", ")", " AND ");
        limitingRowsStrategy.appendClause(builder, null, limit);
    }

    public void sql(Appendable a) {
        SafeAppendable builder = new SafeAppendable(a);
        if (statementType == null) {
            return;
        }

        switch (statementType) {
            case DELETE -> deleteSQL(builder);
            case INSERT -> insertSQL(builder);
            case SELECT -> selectSQL(builder);
            case UPDATE -> updateSQL(builder);
        }
    }
}
