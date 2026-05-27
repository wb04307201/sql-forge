package cn.wubo.sql.forge.utils;

import cn.wubo.sql.forge.map.RowMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ResultSet 工具类，将 {@link java.sql.ResultSet} 转换为 {@link RowMap} 列表。
 */
@UtilityClass
public class ResultSetUtils {

    /**
     * 将 ResultSet 转换为 RowMap 列表。
     *
     * @param rs 查询结果集
     * @return 行映射列表
     * @throws SQLException SQL 异常
     */
    public List<RowMap> resultSetToList(@NonNull ResultSet rs) throws SQLException {
        List<RowMap> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        try {
            if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
                rs.last();
                int rowCount = rs.getRow();
                if (rowCount > 0) {
                    list = new ArrayList<>(rowCount);
                }
                rs.beforeFirst();
            }
        } catch (SQLException e) {
            list = new ArrayList<>();
        }

        while (rs.next()) {
            RowMap row = new RowMap(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            list.add(row);
        }

        return list;
    }
}
