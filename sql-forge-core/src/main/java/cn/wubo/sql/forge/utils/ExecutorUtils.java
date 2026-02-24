package cn.wubo.sql.forge.utils;

import cn.wubo.sql.forge.map.ParamMap;
import cn.wubo.sql.forge.map.RowMap;
import cn.wubo.sql.forge.records.SqlScript;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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


    public List<RowMap> executeQuery(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return ResultSetUtils.resultSetToList(resultSet);
            }
        }
    }

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

    public int executeUpdate(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());
            return preparedStatement.executeUpdate();
        }
    }

    public long executeLargeUpdate(@NonNull Connection connection, @NonNull SqlScript sqlScript) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlScript.sql())) {
            buildPrepareStatement(preparedStatement, sqlScript.params());
            if (preparedStatement.isWrapperFor(PreparedStatement.class))
                return preparedStatement.executeLargeUpdate();
            else
                return preparedStatement.executeUpdate();
        }
    }

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

    public int[] executeBatch(@NonNull Connection connection, @NonNull String sql, @NonNull List<ParamMap> paramsList) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (ParamMap params : paramsList) {
                buildPrepareStatement(preparedStatement, params);
                preparedStatement.addBatch();
            }

            return preparedStatement.executeBatch();
        }
    }

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

    public List<Object> executeByLine(@NonNull Connection connection, @NonNull List<SqlScript> sqlScripts) throws SQLException {
        List<Object> list = new ArrayList<>();
        for (SqlScript sqlScript : sqlScripts) {
            list.add(execute(connection, sqlScript));
        }
        return list;
    }
}
