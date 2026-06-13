package cn.wubo.sql.forge;

import cn.wubo.sql.forge.Entity;
import cn.wubo.sql.forge.EntityExecutor;
import cn.wubo.sql.forge.entity.*;
import cn.wubo.sql.forge.record.SelectPageResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class EntityExecutorDatabaseTest {

    @Autowired
    EntityExecutor entityExecutor;

    @Test
    void testEntitySelect() throws Exception {
        EntitySelect<Product> select = Entity.select(Product.class)
                .distinct(true)
                .columns(Product::getId, Product::getName, Product::getPrice)
                .orders(Product::getName)
                .in(Product::getName, "笔记本电脑", "鼠标");
        List<Product> products = entityExecutor.run(select);
        log.info("{}", products);
        assertEquals(2, products.size());
    }

    @Test
    void testEntitySelectPage() throws Exception {
        EntitySelectPage<Product> select = Entity.selectPage(Product.class)
                .distinct(true)
                .columns(Product::getId, Product::getName, Product::getPrice)
                .orders(Product::getName)
                .in(Product::getName, "笔记本电脑", "鼠标")
                .page(0, 1);
        SelectPageResult<Product> products = entityExecutor.run(select);
        log.info("{}", products);
        assertEquals(2, products.total());
        assertEquals(1, products.rows().size());
    }

    @Test
    void testEntity() throws Exception {
        Product product = new Product();
        product.setName("EntityExecutorDatabaseTest");
        product.setPrice(new BigDecimal("99.99"));
        product = entityExecutor.run(Entity.save(product));
        log.info("{}", product);
        product.setPrice(new BigDecimal("199.99"));
        product = entityExecutor.run(Entity.save(product));
        log.info("{}", product);
        int count = entityExecutor.run(Entity.delete(product));
        log.info("{}", count);
    }

    @Test
    void test() throws Exception {
        String id = UUID.randomUUID().toString();
        EntityInsert<Product> insert = Entity.insert(Product.class).set(Product::getId, id)
                .set(Product::getName, "测试产品")
                .set(Product::getPrice, new BigDecimal("49.99"));
        Object key = entityExecutor.run(insert);
        log.info("{}", key);

        EntityUpdate<Product> update = Entity.update(Product.class)
                .set(Product::getPrice, new BigDecimal("59.99"))
                .eq(Product::getId, id);
        int count = entityExecutor.run(update);
        log.info("{}", count);

        EntityDelete<Product> delete = Entity.delete(Product.class)
                .eq(Product::getId, id);
        count = entityExecutor.run(delete);
        log.info("{}", count);
    }
}
