package cn.wubo.sql.forge.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static cn.wubo.sql.forge.constant.Constant.*;
import static cn.wubo.sql.forge.enums.LimitingRowsStrategy.ISO;
import static cn.wubo.sql.forge.enums.LimitingRowsStrategy.OFFSET_LIMIT;
import static cn.wubo.sql.forge.enums.StatementType.*;

/**
 * 流式 SQL 构建器抽象基类，支持链式调用构建 SELECT、INSERT、UPDATE、DELETE 语句。
 * <p>
 * 灵感来源于 MyBatis SQL Builder，通过泛型 CRTP 模式保证子类链式调用返回正确类型。
 * </p>
 *
 * @param <T> 具体子类类型，用于链式调用返回值
 */
public abstract class AbstractSQL<T> {

    private final SQLStatement sql = new SQLStatement();

    public abstract T getSelf();

    public T UPDATE(String table) {
        sql().statementType = UPDATE;
        sql().tables.add(table);
        return getSelf();
    }

    public T SET(String sets) {
        sql().sets.add(sets);
        return getSelf();
    }

    public T SET(String... sets) {
        sql().sets.addAll(Arrays.asList(sets));
        return getSelf();
    }

    public T INSERT_INTO(String tableName) {
        sql().statementType = INSERT;
        sql().tables.add(tableName);
        return getSelf();
    }

    public T VALUES(String columns, String values) {
        INTO_COLUMNS(columns);
        INTO_VALUES(values);
        return getSelf();
    }

    public T INTO_COLUMNS(String... columns) {
        sql().columns.addAll(Arrays.asList(columns));
        return getSelf();
    }

    public T INTO_VALUES(String... values) {
        List<String> list = sql().valuesList.get(sql().valuesList.size() - 1);
        Collections.addAll(list, values);
        return getSelf();
    }

    public T SELECT(String columns) {
        sql().statementType = SELECT;
        sql().select.add(columns);
        return getSelf();
    }

    public T SELECT(String... columns) {
        sql().statementType = SELECT;
        sql().select.addAll(Arrays.asList(columns));
        return getSelf();
    }

    public T SELECT_DISTINCT(String columns) {
        sql().distinct = true;
        SELECT(columns);
        return getSelf();
    }

    public T SELECT_DISTINCT(String... columns) {
        sql().distinct = true;
        SELECT(columns);
        return getSelf();
    }

    public T DELETE_FROM(String table) {
        sql().statementType = DELETE;
        sql().tables.add(table);
        return getSelf();
    }

    public T FROM(String table) {
        sql().tables.add(table);
        return getSelf();
    }

    public T FROM(String... tables) {
        sql().tables.addAll(Arrays.asList(tables));
        return getSelf();
    }

    public T JOIN(String join) {
        sql().join.add(join);
        return getSelf();
    }

    public T JOIN(String... joins) {
        sql().join.addAll(Arrays.asList(joins));
        return getSelf();
    }

    public T INNER_JOIN(String join) {
        sql().innerJoin.add(join);
        return getSelf();
    }

    public T INNER_JOIN(String... joins) {
        sql().innerJoin.addAll(Arrays.asList(joins));
        return getSelf();
    }

    public T LEFT_OUTER_JOIN(String join) {
        sql().leftOuterJoin.add(join);
        return getSelf();
    }

    public T LEFT_OUTER_JOIN(String... joins) {
        sql().leftOuterJoin.addAll(Arrays.asList(joins));
        return getSelf();
    }

    public T RIGHT_OUTER_JOIN(String join) {
        sql().rightOuterJoin.add(join);
        return getSelf();
    }

    public T RIGHT_OUTER_JOIN(String... joins) {
        sql().rightOuterJoin.addAll(Arrays.asList(joins));
        return getSelf();
    }

    public T OUTER_JOIN(String join) {
        sql().outerJoin.add(join);
        return getSelf();
    }

    public T OUTER_JOIN(String... joins) {
        sql().outerJoin.addAll(Arrays.asList(joins));
        return getSelf();
    }

    public T WHERE(String conditions) {
        sql().where.add(conditions);
        sql().lastList = sql().where;
        return getSelf();
    }

    public T WHERE(String... conditions) {
        sql().where.addAll(Arrays.asList(conditions));
        sql().lastList = sql().where;
        return getSelf();
    }

    public T OR() {
        sql().lastList.add(PAREN_OR);
        return getSelf();
    }

    public T AND() {
        sql().lastList.add(PAREN_AND);
        return getSelf();
    }

    public T GROUP_BY(String columns) {
        sql().groupBy.add(columns);
        return getSelf();
    }

    public T GROUP_BY(String... columns) {
        sql().groupBy.addAll(Arrays.asList(columns));
        return getSelf();
    }

    public T HAVING(String conditions) {
        sql().having.add(conditions);
        sql().lastList = sql().having;
        return getSelf();
    }

    public T HAVING(String... conditions) {
        sql().having.addAll(Arrays.asList(conditions));
        sql().lastList = sql().having;
        return getSelf();
    }

    public T ORDER_BY(String columns) {
        sql().orderBy.add(columns);
        return getSelf();
    }

    public T ORDER_BY(String... columns) {
        sql().orderBy.addAll(Arrays.asList(columns));
        return getSelf();
    }

    public T LIMIT(String variable) {
        sql().limit = variable;
        sql().limitingRowsStrategy = OFFSET_LIMIT;
        return getSelf();
    }

    public T LIMIT(int value) {
        return LIMIT(String.valueOf(value));
    }

    public T OFFSET(String variable) {
        sql().offset = variable;
        sql().limitingRowsStrategy = OFFSET_LIMIT;
        return getSelf();
    }

    public T OFFSET(long value) {
        return OFFSET(String.valueOf(value));
    }

    public T FETCH_FIRST_ROWS_ONLY(String variable) {
        sql().limit = variable;
        sql().limitingRowsStrategy = ISO;
        return getSelf();
    }

    public T FETCH_FIRST_ROWS_ONLY(int value) {
        return FETCH_FIRST_ROWS_ONLY(String.valueOf(value));
    }

    public T OFFSET_ROWS(String variable) {
        sql().offset = variable;
        sql().limitingRowsStrategy = ISO;
        return getSelf();
    }

    public T OFFSET_ROWS(long value) {
        return OFFSET_ROWS(String.valueOf(value));
    }

    public T ADD_ROW() {
        sql().valuesList.add(new ArrayList<>());
        return getSelf();
    }

    private SQLStatement sql() {
        return sql;
    }

    public <A extends Appendable> A usingAppender(A a) {
        sql().sql(a);
        return a;
    }

    public T applyIf(boolean applyCondition, Consumer<T> sqlConsumer) {
        T self = getSelf();
        if (applyCondition) {
            sqlConsumer.accept(self);
        }
        return self;
    }

    public T applyIf(BooleanSupplier applyConditionSupplier, Consumer<T> sqlConsumer) {
        return applyIf(applyConditionSupplier.getAsBoolean(), sqlConsumer);
    }

    public <E> T applyForEach(Iterable<E> iterable, ForEachConsumer<T, E> forEachSqlConsumer) {
        T self = getSelf();
        int elementIndex = 0;
        for (E element : iterable) {
            forEachSqlConsumer.accept(self, element, elementIndex);
            elementIndex++;
        }
        return self;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sql().sql(sb);
        return sb.toString();
    }

    public interface ForEachConsumer<T, E> {

        void accept(T sql, E element, int elementIndex);

    }

}
