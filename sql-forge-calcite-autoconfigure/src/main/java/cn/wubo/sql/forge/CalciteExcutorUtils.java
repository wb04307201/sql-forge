package cn.wubo.sql.forge;

import lombok.experimental.UtilityClass;

import java.sql.*;
import java.util.Properties;

@UtilityClass
public class CalciteExcutorUtils {

    public Connection getConnection(String model) throws SQLException {
        if (model == null || model.trim().isEmpty()) {
            throw new SQLException("model is null");
        }

        Properties info = new Properties();
        info.setProperty("model", "inline:" + model);
        info.setProperty("lex", "JAVA");

        return DriverManager.getConnection("jdbc:calcite:", info);
    }
}
