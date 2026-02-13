package cn.wubo.sql.forge;

import cn.wubo.sql.forge.crud.Insert;
import cn.wubo.sql.forge.inter.IExecute;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class Argon2InsertExecute implements IExecute<Insert> {
    @Override
    public Boolean support(String tableName, Insert insert) {
        return "users".equalsIgnoreCase(tableName);
    }

    @Override
    public Insert before(String tableName, Insert insert) {
        if (insert.sets().keySet().stream().anyMatch("password"::equalsIgnoreCase)) {
            Map<String, Object> newSets = new HashMap<>();
            insert.sets().forEach((k, v) -> {
                if ("password".equalsIgnoreCase(k) && v instanceof String str && StringUtils.hasText(str)) {
                    Argon2 argon2 = Argon2Factory.create();
                    char[] password = str.toCharArray();
                    newSets.put(k, argon2.hash(10, 65536, 1, password));
                } else {
                    newSets.put(k, v);
                }
            });
            return new Insert(newSets, insert.select());
        }

        return insert;
    }
}
