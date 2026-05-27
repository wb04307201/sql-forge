package cn.wubo.sql.forge;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL Forge 全局配置属性，绑定 {@code sql.forge} 前缀。
 * <p>
 * 涵盖 Schema 配置、Calcite 联邦查询、API 开关和 Web Console 开关。
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "sql.forge")
public class SqlForgeProperties {

    private List<String> schemata = new ArrayList<>();
    private  CalciteProperties calcite = new CalciteProperties();
    private ApiProperties api = new ApiProperties();
    private ConsoleProperties console = new ConsoleProperties();

    /**
     * Apache Calcite 跨库联邦查询配置。
     */
    @Data
    public static class CalciteProperties {
        private Boolean enabled = false;
        private String configuration;
        private List<String> schemata = new ArrayList<>();
    }

    /**
     * API 端点开关配置。
     */
    @Data
    public static class ApiProperties {
        private DatabaseProperties database = new DatabaseProperties();
        private JsonProperties json = new JsonProperties();
        private TemplateProperties template = new TemplateProperties();

        /**
         * 数据库直连 API 配置。
         */
        @Data
        public static class DatabaseProperties {
            private Boolean enabled = false;
            private Boolean selectOnly = true;
        }

        /**
         * JSON CRUD API 配置。
         */
        @Data
        public static class JsonProperties {
            private Boolean enabled = true;
        }

        /**
         * 模板 API 配置（SQL 模板和 Amis 模板）。
         */
        @Data
        public static class TemplateProperties {
            private SqlTemplateProperties sql = new SqlTemplateProperties();
            private AmisTemplateProperties amis = new AmisTemplateProperties();

            /**
             * SQL 模板 API 配置。
             */
            @Data
            public static class SqlTemplateProperties {
                private Boolean enabled = true;
            }

            /**
             * Amis 模板 API 配置。
             */
            @Data
            public static class AmisTemplateProperties {
                private Boolean enabled = true;
            }
        }
    }

    /**
     * Web Console 控制台配置。
     */
    @Data
    public static class ConsoleProperties {
        private Boolean enabled = true;
    }
}
