package cn.wubo.sql.forge.utils;

import cn.wubo.sql.forge.map.ParamMap;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.SqlScript;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行工具类，封装 JDBC {@link PreparedStatement} 的参数绑定与各类 SQL 执行操作。
 */
@UtilityClass
public class ExecutorUtils {

    private void buildPrepareStatement(@NotNull PreparedStatement preparedStatement, ParamMap params) throws SQLException {
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                Integer index = entry.getKey();
                Object value = entry.getValue();

                if (index == null) {
                    throw new SQLException("Parameter index cannot be null");
                }

                if (index <= 0) {
                    throw new SQLException("Parameter index must be positive, got: " + entry.getKey());
                }

                preparedStatement.setObject(index, value);
            }
        }
    }


    /**
     * 执行查询 SQL，返回结果集。
     *
     * @param connection 数据库连接
     * @param sqlScript  SQL 脚本
     * @return 查询结果列表
     * @throws SQLException SQL 执行异常
     */
    public List<RowMap> executeQuery(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return ResultSetUtils.resultSetToList(resultSet);
            }
        }
    }

    /**
     * 执行插入 SQL，返回自增主键。
     *
     * @param connection 数据库连接
     * @param sqlScript  SQL 脚本
     * @return 包含生成主键的行映射
     * @throws SQLException SQL 执行异常
     */
    public RowMap executeInsert(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql(), Statement.RETURN_GENERATED_KEYS)) {
            buildPrepareStatement(preparedStatement, sqlScript.params());

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                ResultSetMetaData metaData = generatedKeys.getMetaData();
                int columnCount = metaData.getColumnCount();
                if (generatedKeys.next() && columnCount >= 1) {
                    RowMap row = new RowMap(1);
                    row.put(metaData.getColumnName(1), generatedKeys.getObject(metaData.getColumnName(1)));
                    return row;
                } else
                    return new RowMap(0);
            }
        }
    }

    /**
     * 执行更新/删除 SQL，返回受影响行数。
     *
     * @param connection 数据库连接
     * @param sqlScript  SQL 脚本
     * @return 受影响行数
     * @throws SQLException SQL 执行异常
     */
    public int executeUpdate(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());
            return preparedStatement.executeUpdate();
        }
    }

    /**
     * 执行更新 SQL（大数据量），返回受影响行数（long 类型）。
     *
     * @param connection 数据库连接
     * @param sqlScript  SQL 脚本
     * @return 受影响行数
     * @throws SQLException SQL 执行异常
     */
    public long executeLargeUpdate(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());
            if (preparedStatement.isWrapperFor(PreparedStatement.class))
                return preparedStatement.executeLargeUpdate();
            else
                return preparedStatement.executeUpdate();
        }
    }

    /**
     * 批量执行插入 SQL，返回所有自增主键。
     *
     * @param connection 数据库连接
     * @param sql        SQL 语句
     * @param paramsList 每行的参数映射列表
     * @return 每行生成的主键列表
     * @throws SQLException SQL 执行异常
     */
    public List<RowMap> executeBatchInsert(@NonNull Connection connection, @NonNull String sql, List<ParamMap> paramsList) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (ParamMap params : paramsList) {
                buildPrepareStatement(preparedStatement, params);
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                ResultSetMetaData metaData = generatedKeys.getMetaData();
                int columnCount = metaData.getColumnCount();
                if (columnCount == 0) {
                    return new ArrayList<>();
                }
                List<RowMap> ids = new ArrayList<>(paramsList.size());
                while (generatedKeys.next() && columnCount >= 1) {
                    RowMap row = new RowMap(1);
                    row.put(metaData.getColumnName(1), generatedKeys.getObject(metaData.getColumnName(1)));
                    ids.add(row);
                }
                return ids;
            }
        }
    }

    /**
     * 批量执行 SQL，返回每条语句的受影响行数数组。
     *
     * @param connection 数据库连接
     * @param sql        SQL 语句
     * @param paramsList 每条语句的参数映射列表
     * @return 每条语句的受影响行数数组
     * @throws SQLException SQL 执行异常
     */
    public int[] executeBatch(@NonNull Connection connection, @NonNull String sql, @NonNull List<ParamMap> paramsList) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (ParamMap params : paramsList) {
                buildPrepareStatement(preparedStatement, params);
                preparedStatement.addBatch();
            }

            return preparedStatement.executeBatch();
        }
    }

    /**
     * 通用执行方法，根据 SQL 类型自动判断返回查询结果或更新行数。
     *
     * @param connection 数据库连接
     * @param sqlScript  SQL 脚本
     * @return 查询时为 {@code List<RowMap>}，更新时为受影响行数 {@code Integer}
     * @throws SQLException SQL 执行异常
     */
    public Object execute(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());
            boolean isResultSet = preparedStatement.execute();
            if (isResultSet) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return ResultSetUtils.resultSetToList(resultSet);
                }
            } else {
                return preparedStatement.getUpdateCount();
            }
        }
    }

    /**
     * 逐条执行多条 SQL 脚本，返回每条的执行结果。
     *
     * @param connection 数据库连接
     * @param sqlScripts SQL 脚本列表
     * @return 每条 SQL 的执行结果列表
     * @throws SQLException SQL 执行异常
     */
    public List<Object> executeByLine(@NonNull Connection connection, @NonNull List<SqlScript> sqlScripts) throws SQLException {
        List<Object> list = new ArrayList<>();
        for (SqlScript sqlScript : sqlScripts) {
            list.add(execute(connection, sqlScript));
        }
        return list;
    }
}
