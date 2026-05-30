package cn.wubo.sql.forge;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 产品实体，用于 EntityExecutor 测试。
 */
@Data
@Table(name = "products")
public class Product {
    @Id
    private String id;
    @Column
    private String name;
    @Column
    private java.math.BigDecimal price;
}
