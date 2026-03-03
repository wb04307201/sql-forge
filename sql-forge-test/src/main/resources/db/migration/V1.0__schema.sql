-- 字典表
CREATE TABLE sys_dicts
(
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    dict_code VARCHAR(64)  NOT NULL UNIQUE,
    dict_name VARCHAR(100) NOT NULL
);

COMMENT
ON TABLE sys_dicts IS '字典表';
COMMENT
ON COLUMN sys_dicts.id IS '字典ID';
COMMENT
ON COLUMN sys_dicts.dict_code IS '字典编码';
COMMENT
ON COLUMN sys_dicts.dict_name IS '字典名称';

-- 字典项表
CREATE TABLE sys_dict_items
(
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    dict_code VARCHAR(64)  NOT NULL,
    item_code VARCHAR(64)  NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    sort      INT DEFAULT 0,

    -- 联合唯一约束
    CONSTRAINT uk_dict_items UNIQUE (dict_code, item_code),
    -- 外键约束
    CONSTRAINT fk_dict_items_dict_code
        FOREIGN KEY (dict_code) REFERENCES sys_dicts (dict_code)
            ON DELETE CASCADE -- 主表删除时自动清理子项
);

COMMENT
ON TABLE sys_dict_items IS '字典项表';
COMMENT
ON COLUMN sys_dict_items.id IS '字典项ID';
COMMENT
ON COLUMN sys_dict_items.dict_code IS '字典编码';
COMMENT
ON COLUMN sys_dict_items.item_code IS '字典项编码';
COMMENT
ON COLUMN sys_dict_items.item_name IS '字典项名称';
COMMENT
ON COLUMN sys_dict_items.sort IS '排序值';

-- 用户表
CREATE TABLE users
(
    id       VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    dict_sex VARCHAR(100),
    email    VARCHAR(100),
    CONSTRAINT uk_username UNIQUE (username)
);

COMMENT
ON TABLE users IS '用户表';
COMMENT
ON COLUMN users.id IS '用户ID';
COMMENT
ON COLUMN users.username IS '用户名';
COMMENT
ON COLUMN users.dict_sex IS '性别';
COMMENT
ON COLUMN users.email IS '用户邮箱地址';

-- 商品表
CREATE TABLE products
(
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    dict_categories VARCHAR(100),
    price           DECIMAL(10, 2)
);

COMMENT
ON TABLE products IS '商品表';
COMMENT
ON COLUMN products.id IS '商品ID';
COMMENT
ON COLUMN products.name IS '商品名称';
COMMENT
ON COLUMN products.name IS '商品类型';
COMMENT
ON COLUMN products.price IS '商品价格';

-- 订单表
CREATE TABLE orders
(
    id                VARCHAR(36)    NOT NULL PRIMARY KEY,
    user_id           VARCHAR(36)    NOT NULL,
    order_date        DATE        DEFAULT CURRENT_DATE,
    total_amount      DECIMAL(10, 2) NOT NULL,
    dict_order_status VARCHAR(20) DEFAULT 'pending',
    FOREIGN KEY (user_id) REFERENCES users (id)
);

COMMENT
ON TABLE orders IS '订单表';
COMMENT
ON COLUMN orders.id IS '订单ID';
COMMENT
ON COLUMN orders.user_id IS '用户ID';
COMMENT
ON COLUMN orders.order_date IS '订单日期';
COMMENT
ON COLUMN orders.total_amount IS '订单总金额';
COMMENT
ON COLUMN orders.dict_order_status IS '订单状态';

-- 订单明细表
CREATE TABLE order_items
(
    id         VARCHAR(36)    NOT NULL PRIMARY KEY,
    order_id   VARCHAR(36)    NOT NULL,
    product_id VARCHAR(36)    NOT NULL,
    quantity   INT            NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id),
    FOREIGN KEY (product_id) REFERENCES products (id),

    -- 联合唯一约束
    CONSTRAINT uk_order_items UNIQUE (order_id, product_id),
    -- 外键约束
    CONSTRAINT uk_order_items_order_id
        FOREIGN KEY (order_id) REFERENCES orders (id)
            ON DELETE CASCADE -- 主表删除时自动清理子项
);

COMMENT
ON TABLE order_items IS '订单明细表';
COMMENT
ON COLUMN order_items.id IS '订单明细ID';
COMMENT
ON COLUMN order_items.order_id IS '订单ID';
COMMENT
ON COLUMN order_items.product_id IS '商品ID';
COMMENT
ON COLUMN order_items.quantity IS '订购数量';
COMMENT
ON COLUMN order_items.unit_price IS '订购数量';

