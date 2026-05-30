-- 1. 创建 users 表（UUID 主键，应用生成）
CREATE TABLE users
(
    id       VARCHAR(36)  NOT NULL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(200),
    enabled  BOOLEAN DEFAULT TRUE,
    category VARCHAR(50)
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';

-- 2. 创建 products 表（UUID 主键，应用生成）
CREATE TABLE products
(
    id    VARCHAR(36)  NOT NULL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10, 2)
);

COMMENT ON TABLE products IS '产品表';
COMMENT ON COLUMN products.id IS '产品ID';
COMMENT ON COLUMN products.name IS '产品名称';
COMMENT ON COLUMN products.price IS '产品价格';

-- 3. 创建 orders 表（自增主键）
CREATE TABLE orders
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    order_date DATE                 DEFAULT CURRENT_DATE,
    quantity   INT         NOT NULL DEFAULT 1
);

COMMENT ON TABLE orders IS '订单表';
COMMENT ON COLUMN orders.id IS '订单ID';
COMMENT ON COLUMN orders.user_id IS '用户ID';
COMMENT ON COLUMN orders.product_id IS '产品ID';
COMMENT ON COLUMN orders.quantity IS '订购数量';
COMMENT ON COLUMN orders.order_date IS '订单日期';

-- 4. 模板 sql_forge_template_sql 表
CREATE TABLE sql_forge_template_sql
(
    id            VARCHAR(64) NOT NULL PRIMARY KEY,
    name          VARCHAR(100),
    description   VARCHAR(500),
    executor_name VARCHAR(50) NOT NULL,
    context       TEXT        NOT NULL
);

COMMENT ON TABLE sql_forge_template_sql IS 'SQL模板表';
COMMENT ON COLUMN sql_forge_template_sql.id IS '模板ID';
COMMENT ON COLUMN sql_forge_template_sql.name IS '模板名称';
COMMENT ON COLUMN sql_forge_template_sql.description IS '模板描述';
COMMENT ON COLUMN sql_forge_template_sql.executor_name IS '数据源';
COMMENT ON COLUMN sql_forge_template_sql.context IS '模板内容';

-- 5. 模板 sql_forge_template_amis 表
CREATE TABLE sql_forge_template_amis
(
    id          VARCHAR(64) NOT NULL PRIMARY KEY,
    name        VARCHAR(100),
    description VARCHAR(500),
    context     TEXT        NOT NULL
);

COMMENT ON TABLE sql_forge_template_amis IS 'AMIS模板表';
COMMENT ON COLUMN sql_forge_template_amis.id IS '模板ID';
COMMENT ON COLUMN sql_forge_template_amis.name IS '模板名称';
COMMENT ON COLUMN sql_forge_template_amis.description IS '模板描述';
COMMENT ON COLUMN sql_forge_template_amis.context IS '模板内容';

-- 插入测试用户数据
INSERT INTO users (id, username, password, enabled, category)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'alice', '123456', TRUE, 'user'),
       ('550e8400-e29b-41d4-a716-446655440001', 'bob', '123456', TRUE, 'user'),
       ('550e8400-e29b-41d4-a716-446655440002', 'charlie', '123456', TRUE, 'user');

-- 插入测试商品数据
INSERT INTO products (id, name, price)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479', '笔记本电脑', 999.99),
       ('f47ac10b-58cc-4372-a567-0e02b2c3d480', '鼠标', 25.50),
       ('f47ac10b-58cc-4372-a567-0e02b2c3d481', '键盘', 75.00);

-- 插入测试订单数据（自增 id）
INSERT INTO orders (user_id, product_id, quantity)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 1),
       ('550e8400-e29b-41d4-a716-446655440000', 'f47ac10b-58cc-4372-a567-0e02b2c3d480', 2),
       ('550e8400-e29b-41d4-a716-446655440001', 'f47ac10b-58cc-4372-a567-0e02b2c3d481', 1),
       ('550e8400-e29b-41d4-a716-446655440002', 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 1);

-- 示例联表查询：查询每个订单的用户、商品信息
SELECT *
FROM users;
SELECT *
FROM products;
SELECT *
FROM orders;

SELECT o.id                   AS order_id,
       u.username,
       p.name                 AS product_name,
       p.price,
       o.quantity,
       (p.price * o.quantity) AS total
FROM orders o
         JOIN users u ON o.user_id = u.id
         JOIN products p ON o.product_id = p.id;
