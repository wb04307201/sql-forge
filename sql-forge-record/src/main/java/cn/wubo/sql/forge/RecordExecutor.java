package cn.wubo.sql.forge;

import cn.wubo.sql.forge.record.base.Join;
import cn.wubo.sql.forge.record.base.Page;
import cn.wubo.sql.forge.record.base.Where;
import cn.wubo.sql.forge.enums.ConditionType;
import cn.wubo.sql.forge.jdbc.SQL;
import cn.wubo.sql.forge.map.ParamMap;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.record.*;
import cn.wubo.sql.forge.records.SqlScript;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static cn.wubo.sql.forge.constant.Constant.QUESTION_MARK;

public record RecordExecutor(ExecutorService executorService,
                             List<IBeforeRecordExecutor<Delete>> deleteExecutes,
                             List<IBeforeRecordExecutor<Insert>> insertExecutes,
                             List<IBeforeRecordExecutor<Select>> selectExecutes,
                             List<IBeforeRecordExecutor<SelectPage>> selectPageExecutes,
                             List<IBeforeRecordExecutor<Update>> updateExecutes) {

    public Object delete(@NotBlank String tableName, @Valid Delete orginDelete) throws SQLException {
        return delete("database", tableName, orginDelete);
    }

    /**
     * 删除指定表中的记录
     *
     * @param tableName   表名，不能为空
     * @param orginDelete 删除操作对象，包含删除条件和查询条件
     * @return 如果delete.select()不为null则返回查询结果，否则返回删除记录的数量
     * @throws SQLException SQL执行异常
     */
    public Object delete(@NotBlank String executorName, @NotBlank String tableName, @Valid Delete orginDelete) throws SQLException {
        Delete delete = orginDelete;
        if (deleteExecutes != null && !deleteExecutes.isEmpty()) {
            for (IBeforeRecordExecutor<Delete> execute : deleteExecutes) {
                if (execute.support(tableName, delete)) delete = execute.before(tableName, delete);
            }
        }
        IExecutor executor = executorService.getExecutor(executorName);

        // 创建参数映射和SQL构建器
        ParamMap params = new ParamMap();
        SQL sql = new SQL().DELETE_FROM(tableName);

        // 应用删除条件到SQL中
        applyWheres(sql, delete.wheres(), params);

        // 执行删除操作并获取影响的记录数
        int count = executor.executeUpdate(new SqlScript(sql.toString(), params));

        // 如果存在查询条件则执行查询并返回结果，否则返回删除数量
        if (delete.select() != null) return select(executorName, tableName, delete.select());
        else return count;
    }

    public Object insert(@NotBlank String tableName, @Valid Insert orginInsert) throws SQLException {
        return insert("database", tableName, orginInsert);
    }

    /**
     * 插入数据到指定表中
     *
     * @param tableName   表名，不能为空
     * @param orginInsert 插入操作对象，包含要插入的字段和值
     * @return 如果指定了select查询则返回查询结果，否则返回插入记录的主键值
     * @throws SQLException SQL执行异常
     */
    public Object insert(@NotBlank String executorName, @NotBlank String tableName, @Valid Insert orginInsert) throws SQLException {
        Insert insert = orginInsert;
        if (insertExecutes != null && !insertExecutes.isEmpty()) {
            for (IBeforeRecordExecutor<Insert> execute : insertExecutes) {
                if (execute.support(tableName, insert)) insert = execute.before(tableName, insert);
            }
        }
        IExecutor executor = executorService.getExecutor(executorName);

        // 创建参数映射和SQL构建器
        ParamMap params = new ParamMap();
        SQL sql = new SQL().INSERT_INTO(tableName);

        // 遍历插入字段和值，构建VALUES子句
        for (Map.Entry<String, Object> set : insert.sets().entrySet()) {
            params.put(set.getValue());
            sql.VALUES(set.getKey(), QUESTION_MARK);
        }

        // 执行插入操作并获取主键值
        Object key = executor.executeInsert(new SqlScript(sql.toString(), params));

        // 如果插入操作中指定了select查询，则执行查询并返回结果，否则返回主键值
        if (insert.select() != null) return select(executorName, tableName, insert.select());
        else return key;
    }

    public List<RowMap> select(@NotBlank String tableName, @Valid Select orginSelect) throws SQLException {
        return select("database", tableName, orginSelect);
    }

    /**
     * 执行SELECT查询操作
     *
     * @param tableName   表名，不能为空
     * @param orginSelect 查询条件对象，必须符合校验规则
     * @return 查询结果行映射列表
     * @throws SQLException SQL执行异常
     */
    public List<RowMap> select(@NotBlank String executorName, @NotBlank String tableName, @Valid Select orginSelect) throws SQLException {
        Select select = orginSelect;
        if (selectExecutes != null && !selectExecutes.isEmpty()) {
            for (IBeforeRecordExecutor<Select> execute : selectExecutes) {
                if (execute.support(tableName, select)) select = execute.before(tableName, select);
            }
        }
        IExecutor executor = executorService.getExecutor(executorName);

        ParamMap params = new ParamMap();
        SQL sql = new SQL().FROM(tableName);
        String[] columns = select.columns() == null || select.columns().isEmpty() ? new String[]{"*"} : select.columns().toArray(String[]::new);
        if (select.distinct()) sql.SELECT_DISTINCT(columns);
        else sql.SELECT(columns);

        applyJoins(sql, select.joins());

        applyWheres(sql, select.wheres(), params);

        // 处理GROUP BY子句
        if (select.groups() != null && !select.groups().isEmpty()) sql.GROUP_BY(select.groups().toArray(String[]::new));

        // 处理ORDER BY子句，过滤掉空值和未求值的模板表达式
        List<String> orders = sanitizeOrders(select.orders());
        if (!orders.isEmpty()) sql.ORDER_BY(orders.toArray(String[]::new));

        return executor.executeQuery(new SqlScript(sql.toString(), params));
    }

    public SelectPageResult<RowMap> selectPage(@NotBlank String tableName, @Valid SelectPage orginSelect) throws SQLException {
        return selectPage("database", tableName, orginSelect);
    }


    public SelectPageResult<RowMap> selectPage(@NotBlank String executorName, @NotBlank String tableName, @Valid SelectPage orginSelect) throws SQLException {
        SelectPage select = orginSelect;
        if (selectPageExecutes != null && !selectPageExecutes.isEmpty()) {
            for (IBeforeRecordExecutor<SelectPage> execute : selectPageExecutes) {
                if (execute.support(tableName, select)) select = execute.before(tableName, select);
            }
        }
        IExecutor executor = executorService.getExecutor(executorName);

        ParamMap params = new ParamMap();
        SQL sql = new SQL().FROM(tableName);
        String[] columns = select.columns() == null || select.columns().isEmpty() ? new String[]{"*"} : select.columns().toArray(String[]::new);
        if (select.distinct()) sql.SELECT_DISTINCT(columns);
        else sql.SELECT(columns);

        applyJoins(sql, select.joins());

        applyWheres(sql, select.wheres(), params);

        // 处理ORDER BY子句，过滤掉空值和未求值的模板表达式
        List<String> orders = sanitizeOrders(select.orders());
        if (!orders.isEmpty()) sql.ORDER_BY(orders.toArray(String[]::new));

        // 处理分页参数
        Page page = select.page();
        if (page == null) {
            page = new Page(0, 10);
        }
        params.put(page.pageSize());
        params.put((long) page.pageIndex() * page.pageSize());
        sql.LIMIT(QUESTION_MARK).OFFSET(QUESTION_MARK);

        List<RowMap> countList = select(executorName, tableName, select.selectCount());

        return new SelectPageResult<>((Long) countList.get(0).entrySet().stream().findFirst().get().getValue(), executor.executeQuery(new SqlScript(sql.toString(), params)));
    }

    public Object update(@NotBlank String tableName, @Valid Update orginUpdate) throws SQLException {
        return update("database", tableName, orginUpdate);
    }

    /**
     * 更新指定表中的数据记录
     *
     * @param tableName   要更新的表名，不能为空
     * @param orginUpdate 更新操作对象，包含SET子句、WHERE条件和可选的SELECT查询
     * @return 如果指定了select查询则返回查询结果，否则返回受影响的记录数量
     * @throws SQLException 执行SQL操作时可能抛出的数据库异常
     */
    public Object update(@NotBlank String executorName, @NotBlank String tableName, @Valid Update orginUpdate) throws SQLException {
        Update update = orginUpdate;
        if (updateExecutes != null && !updateExecutes.isEmpty()) {
            for (IBeforeRecordExecutor<Update> execute : updateExecutes) {
                if (execute.support(tableName, update)) update = execute.before(tableName, update);
            }
        }
        IExecutor executor = executorService.getExecutor(executorName);

        ParamMap params = new ParamMap();
        SQL sql = new SQL().UPDATE(tableName);

        // 构建SET子句，遍历更新字段和值的映射关系
        for (Map.Entry<String, Object> set : update.sets().entrySet()) {
            params.put(set.getValue());
            sql.SET(set.getKey() + ConditionType.EQ.getValue() + QUESTION_MARK);
        }

        applyWheres(sql, update.wheres(), params);

        int count = executor.executeUpdate(new SqlScript(sql.toString(), params));

        // 根据是否包含select查询决定返回结果
        if (update.select() != null) return select(executorName, tableName, update.select());
        else return count;
    }

    private void applyJoins(SQL sql, List<Join> joins) {
        if (joins != null && !joins.isEmpty()) {
            for (Join join : joins) {
                String joinStr = join.create();
                if (joinStr != null) {
                    switch (join.type()) {
                        case INNER_JOIN -> sql.INNER_JOIN(joinStr);
                        case LEFT_OUTER_JOIN -> sql.LEFT_OUTER_JOIN(joinStr);
                        case RIGHT_OUTER_JOIN -> sql.RIGHT_OUTER_JOIN(joinStr);
                        case OUTER_JOIN -> sql.OUTER_JOIN(joinStr);
                        default -> sql.JOIN(joinStr);
                    }
                }

            }
        }
    }

    /**
     * 过滤排序字段列表，移除空值和未求值的模板表达式（如 Amis 的 ${...} 表达式）
     *
     * @param orders 原始排序字段列表
     * @return 过滤后的排序字段列表
     */
    private List<String> sanitizeOrders(List<String> orders) {
        if (orders == null || orders.isEmpty()) return List.of();
        return orders.stream()
                .filter(o -> o != null && !o.isBlank() && !o.contains("${"))
                .toList();
    }

    private void applyWheres(SQL sql, List<Where> wheres, ParamMap params) {
        if (wheres != null && !wheres.isEmpty()) {
            for (Where where : wheres) {
                String whereStr = where.create(params);
                if (whereStr != null) sql.WHERE(whereStr);
            }
        }
    }
}
