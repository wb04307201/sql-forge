package cn.wubo.sql.forge;


/**
 * 数据库信息记录类
 */
public record DatabaseInfo(
        String productName,
        String productVersion
) {
}