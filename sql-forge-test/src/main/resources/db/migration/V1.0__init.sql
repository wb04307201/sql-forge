-- 启用 H2 的 UUID 支持（如果需要显式使用 UUID 函数，但本例中 UUID 由应用提供，所以仅作注释说明）
-- 注意：H2 2.x+ 默认支持 UUID 类型，但本脚本使用 VARCHAR(36) 存储 UUID 字符串以兼容性更好

-- 1. 创建 users 表（UUID 主键，应用生成）
CREATE TABLE users
(
    id       VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    sex      VARCHAR(100),
    email    VARCHAR(100),
    password VARCHAR(100) NOT NULL,
    CONSTRAINT uk_username UNIQUE (username),
    create TIMESTAMP,
    update TIMESTAMP
);

COMMENT
ON TABLE users IS '用户表';
COMMENT
ON COLUMN users.id IS '用户ID';
COMMENT
ON COLUMN users.username IS '用户名';
COMMENT
ON COLUMN users.sex IS '性别';
COMMENT
ON COLUMN users.email IS '用户邮箱地址';
COMMENT
ON COLUMN users.password IS '密码';
COMMENT
ON COLUMN users.create IS '创建时间';
COMMENT
ON COLUMN users.update IS '更新时间';

-- 2. 创建 products 表（UUID 主键，应用生成）
CREATE TABLE products
(
    id    VARCHAR(36)  NOT NULL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(10, 2),
    create TIMESTAMP,
    update TIMESTAMP
);

COMMENT
ON TABLE products IS '产品表';
COMMENT
ON COLUMN products.id IS '产品ID';
COMMENT
ON COLUMN products.name IS '产品名称';
COMMENT
ON COLUMN products.price IS '产品价格';
COMMENT
ON COLUMN products.create IS '创建时间';
COMMENT
ON COLUMN products.update IS '更新时间';

-- 3. 创建 orders 表（自增主键）
CREATE TABLE orders
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    order_date DATE            DEFAULT CURRENT_DATE,
    quantity   INT         NOT NULL DEFAULT 1,
    create TIMESTAMP,
    update TIMESTAMP
);

COMMENT
ON TABLE orders IS '订单表';
COMMENT
ON COLUMN orders.id IS '订单ID';
COMMENT
ON COLUMN orders.user_id IS '用户ID';
COMMENT
ON COLUMN orders.product_id IS '产品ID';
COMMENT
ON COLUMN orders.quantity IS '订购数量';
COMMENT
ON COLUMN orders.order_date IS '订单日期';
COMMENT
ON COLUMN orders.quantity IS '订购数量';
COMMENT
ON COLUMN orders.create IS '创建时间';
COMMENT
ON COLUMN orders.update IS '更新时间';

-- 4. 模板 sql_forge_template 表
CREATE TABLE sql_forge_template
(
    id            VARCHAR(64) NOT NULL PRIMARY KEY,
    template_type VARCHAR(50),
    context       TEXT,
    create TIMESTAMP,
    update TIMESTAMP
);

COMMENT
ON TABLE sql_forge_template IS '模板表';
COMMENT
ON COLUMN sql_forge_template.id IS '模板ID';
COMMENT
ON COLUMN sql_forge_template.template_type IS '模板类型';
COMMENT
ON COLUMN sql_forge_template.context IS '模板内容';
COMMENT
ON COLUMN sql_forge_template.create IS '创建时间';
COMMENT
ON COLUMN sql_forge_template.update IS '更新时间';

-- 5.字典主表：存储字典类型（如：性别、状态）
CREATE TABLE sys_dict
(
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    dict_code   VARCHAR(64)  NOT NULL UNIQUE,
    dict_name   VARCHAR(100) NOT NULL,
    dict_type   VARCHAR(100) NOT NULL,
    create TIMESTAMP,
    update TIMESTAMP
);

COMMENT
ON TABLE sys_dict IS '字典表';
COMMENT
ON COLUMN sys_dict.id IS '字典ID';
COMMENT
ON COLUMN sys_dict.dict_code IS '字典编码';
COMMENT
ON COLUMN sys_dict.dict_name IS '字典名称';
COMMENT
ON COLUMN sys_dict.dict_type IS '字典类型';
COMMENT
ON COLUMN sys_dict.create IS '创建时间';
COMMENT
ON COLUMN sys_dict.update IS '更新时间';

-- 6.字典子表：存储字典明细项（如：男、女）
CREATE TABLE sys_dict_item
(
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    dict_code VARCHAR(64)  NOT NULL,
    item_code VARCHAR(64)  NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    sort      INT DEFAULT 0,
    create TIMESTAMP,
    update TIMESTAMP,

    -- 联合唯一约束：同一字典下编码唯一
    CONSTRAINT uk_dict_item UNIQUE (dict_code, item_code),
    -- 外键约束
    CONSTRAINT fk_dict_item_dict_code
        FOREIGN KEY (dict_code) REFERENCES sys_dict (dict_code)
            ON DELETE CASCADE -- 主表删除时自动清理子项
);

-- 创建索引
CREATE INDEX idx_dict_code ON sys_dict_item (dict_code);
COMMENT
ON TABLE sys_dict_item IS '字典项表';
COMMENT
ON COLUMN sys_dict_item.id IS '字典项ID';
COMMENT
ON COLUMN sys_dict_item.dict_code IS '字典编码';
COMMENT
ON COLUMN sys_dict_item.item_code IS '字典项编码';
COMMENT
ON COLUMN sys_dict_item.item_name IS '字典项名称';
COMMENT
ON COLUMN sys_dict_item.sort IS '排序值';
COMMENT
ON COLUMN sys_dict_item.create IS '创建时间';
COMMENT
ON COLUMN sys_dict_item.update IS '更新时间';

-- 插入测试用户数据（使用预定义 UUID）
INSERT INTO users (id, username, sex, email, password)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'alice', 'female', 'alice@example.com','$argon2i$v=19$m=65536,t=10,p=1$m+vkvMIzPjFUDCQc0X2AdQ$4LrnP44n8SpdCwzCKy2gpu6syCzh7DKTiGhvv4VYyOQ'),
       ('550e8400-e29b-41d4-a716-446655440001', 'bob', 'male', 'bob@example.com','$argon2i$v=19$m=65536,t=10,p=1$m+vkvMIzPjFUDCQc0X2AdQ$4LrnP44n8SpdCwzCKy2gpu6syCzh7DKTiGhvv4VYyOQ'),
       ('550e8400-e29b-41d4-a716-446655440002', 'charlie', 'male', 'charlie@example.com','$argon2i$v=19$m=65536,t=10,p=1$m+vkvMIzPjFUDCQc0X2AdQ$4LrnP44n8SpdCwzCKy2gpu6syCzh7DKTiGhvv4VYyOQ'),
       ('550e8400-e29b-41d4-a716-446655440003', 'wubo01', 'male', 'wubo01@@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440004', 'wubo02', 'male', 'wubo02@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440005', 'wubo03', 'male', 'wubo03@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440006', 'wubo04', 'male', 'wubo04@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440007', 'wubo05', 'male', 'wubo05@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440008', 'wubo06', 'male', 'wubo06@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440009', 'wubo07', 'male', 'wubo07@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440010', 'wubo08', 'male', 'wubo08@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440011', 'wubo09', 'male', 'wubo09@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440012', 'wubo10', 'male', 'wubo10@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440013', 'wubo11', 'male', 'wubo11@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440014', 'wubo12', 'male', 'wubo12@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440015', 'wubo13', 'male', 'wubo13@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440016', 'wubo14', 'male', 'wubo14@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440017', 'wubo15', 'male', 'wubo15@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440018', 'wubo16', 'male', 'wubo16@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440019', 'wubo17', 'male', 'wubo17@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440020', 'wubo18', 'male', 'wubo18@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440021', 'wubo19', 'male', 'wubo19@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g'),
       ('550e8400-e29b-41d4-a716-446655440022', 'wubo20', 'male', 'wubo20@example.com','$argon2i$v=19$m=65536,t=10,p=1$EbJKqGGoSzq2eZJiP5YLPQ$nFO6z4aWJBaCT0/iRebXThYxERXdWBt5EDx229iaJ/g');

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


-- 插入测试字典数据
INSERT INTO PUBLIC.SYS_DICT (ID, DICT_CODE, DICT_NAME, DICT_TYPE)
VALUES ('1', 'dict_type', '数据类型', 'system');
INSERT INTO PUBLIC.SYS_DICT (ID, DICT_CODE, DICT_NAME,DICT_TYPE)
VALUES ('2', 'sex', '性别', 'system');

INSERT INTO PUBLIC.SYS_DICT_ITEM (ID, DICT_CODE, ITEM_CODE, ITEM_NAME, SORT)
VALUES ('1-1', 'dict_type', 'system', '系统', 1);
INSERT INTO PUBLIC.SYS_DICT_ITEM (ID, DICT_CODE, ITEM_CODE, ITEM_NAME, SORT)
VALUES ('1-2', 'dict_type', 'business', '业务', 2);
INSERT INTO PUBLIC.SYS_DICT_ITEM (ID, DICT_CODE, ITEM_CODE, ITEM_NAME, SORT)
VALUES ('2-1', 'sex', 'male', '男', 1);
INSERT INTO PUBLIC.SYS_DICT_ITEM (ID, DICT_CODE, ITEM_CODE, ITEM_NAME, SORT)
VALUES ('2-2', 'sex', 'female', '女', 2);


-- 示例联表查询：查询每个订单的用户、商品信息
SELECT o.id                   AS order_id,
       u.username,
       p.name                 AS product_name,
       p.price,
       o.quantity,
       (p.price * o.quantity) AS total
FROM orders o
         JOIN users u ON o.user_id = u.id
         JOIN products p ON o.product_id = p.id;

