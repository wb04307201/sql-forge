package cn.wubo.sql.forge;

import com.mysql.cj.jdbc.MysqlDataSource;
import lombok.experimental.UtilityClass;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;

@UtilityClass
public class CalciteHelp {

    public void init() {
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setURL("jdbc:mysql://localhost:3306/test");
        mysqlDataSource.setUser("root");
        mysqlDataSource.setPassword("123456");

        Flyway mysqlFlyway = Flyway.configure()
                .dataSource(mysqlDataSource)
                .locations("classpath:mysql")
                .cleanDisabled(false)
                .baselineOnMigrate(true)
                .load();
        mysqlFlyway.clean();
        mysqlFlyway.migrate();

        PGSimpleDataSource pgSimpleDataSource = new PGSimpleDataSource();
        pgSimpleDataSource.setURL("jdbc:postgresql://localhost:5432/test");
        pgSimpleDataSource.setUser("postgres");
        pgSimpleDataSource.setPassword("123456");

        Flyway pgFlyway = Flyway.configure()
                .dataSource(pgSimpleDataSource)
                .locations("classpath:postgresql")
                .cleanDisabled(false)
                .baselineOnMigrate(true)
                .load();
        pgFlyway.clean();
        pgFlyway.migrate();
    }
}
