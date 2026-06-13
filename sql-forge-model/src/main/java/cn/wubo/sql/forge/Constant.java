package cn.wubo.sql.forge;

public class Constant {

    private Constant() {
    }

    public static final String PERCENT = "%";
    public static final String UPPER = "UPPER";
    public static final String OPERN_PAREN = "(";
    public static final String CLOSE_PAREN = ")";
    public static final String QUESTION_MARK = "?";
    public static final String UPPER_QUESTION_MARK = "UPPER(?)";
    public static final String AND = " AND ";
    public static final String PAREN_AND = ") \nAND (";
    public static final String PAREN_OR = ") \nOR (";
    public static final String ON_TEMPLATE = "%s ON %s";
    public static final String NODE_VALUE_TEMPLATE = "%s.%s";

    public final static String GET_METADATA_DATABASE_URL = "/sql/forge/api/database/metaDataDatabase";
    public final static String GET_METADATA_TABLES_URL = "/sql/forge/api/database/metaDataTables";
    public final static String GET_METADATA_TABLE_DEFINITIONS_URL = "/sql/forge/api/database/metaDataDefinitions";
    public final static String GET_METADATA_TREE_URL = "/sql/forge/api/database/metaDataTree";
    public final static String EXECUTE_SQL_URL = "/sql/forge/api/database/execute";
    public final static String JSON_CRUD_URL = "/sql/forge/api/json/%s/%s";
    public final static String LIST_EXECUTOR_NAMES_URL = "/sql/forge/api/console/executorName";
    public final static String TEMPLATE_SQL_URL = "/sql/forge/api/template/sql";
    public final static String TEMPLATE_SQL_ID_URL = "/sql/forge/api/template/sql/%s";
    public final static String GET_TEMPLATE_AMIS_URL = "/sql/forge/api/template/amis";
    public final static String GET_TEMPLATE_AMIS_ID_URL = "/sql/forge/api/template/amis/%s";
    public final static String PUT_TEMPLATE_AMIS_URL = "/sql/forge/api/template/amis";

}
