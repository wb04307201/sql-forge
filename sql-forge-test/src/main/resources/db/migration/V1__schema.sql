-- ========================================
-- SQL Forge - 完整建表脚本
-- 合并自: V1.0__schema.sql, V1.5__schema.sql, V1.8__auth_schema.sql
-- ========================================

-- ========================================
-- 基础业务表
-- ========================================

-- 字典表
CREATE TABLE sys_dicts
(
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    dict_code VARCHAR(64)  NOT NULL UNIQUE,
    dict_name VARCHAR(100) NOT NULL
);

COMMENT ON TABLE sys_dicts IS '字典表';
COMMENT ON COLUMN sys_dicts.id IS '字典ID';
COMMENT ON COLUMN sys_dicts.dict_code IS '字典编码';
COMMENT ON COLUMN sys_dicts.dict_name IS '字典名称';

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
            ON DELETE CASCADE
);

COMMENT ON TABLE sys_dict_items IS '字典项表';
COMMENT ON COLUMN sys_dict_items.id IS '字典项ID';
COMMENT ON COLUMN sys_dict_items.dict_code IS '字典编码';
COMMENT ON COLUMN sys_dict_items.item_code IS '字典项编码';
COMMENT ON COLUMN sys_dict_items.item_name IS '字典项名称';
COMMENT ON COLUMN sys_dict_items.sort IS '排序值';

-- 用户表
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
COMMENT ON COLUMN users.password IS '密码';
COMMENT ON COLUMN users.enabled IS '是否启用';
COMMENT ON COLUMN users.category IS '用户分类';

-- 商品表
CREATE TABLE products
(
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    dict_categories VARCHAR(100),
    price           DECIMAL(10, 2)
);

COMMENT ON TABLE products IS '商品表';
COMMENT ON COLUMN products.id IS '商品ID';
COMMENT ON COLUMN products.name IS '商品名称';
COMMENT ON COLUMN products.price IS '商品价格';

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

COMMENT ON TABLE orders IS '订单表';
COMMENT ON COLUMN orders.id IS '订单ID';
COMMENT ON COLUMN orders.user_id IS '用户ID';
COMMENT ON COLUMN orders.order_date IS '订单日期';
COMMENT ON COLUMN orders.total_amount IS '订单总金额';
COMMENT ON COLUMN orders.dict_order_status IS '订单状态';

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
    CONSTRAINT fk_order_items_order_id
        FOREIGN KEY (order_id) REFERENCES orders (id)
            ON DELETE CASCADE
);

COMMENT ON TABLE order_items IS '订单明细表';
COMMENT ON COLUMN order_items.id IS '订单明细ID';
COMMENT ON COLUMN order_items.order_id IS '订单ID';
COMMENT ON COLUMN order_items.product_id IS '商品ID';
COMMENT ON COLUMN order_items.quantity IS '订购数量';
COMMENT ON COLUMN order_items.unit_price IS '单价';

-- ========================================
-- 扩展业务表（支付、地址、库存、物流、评价、分类）
-- ========================================

-- 支付记录表
CREATE TABLE payments
(
    id             VARCHAR(36)    NOT NULL PRIMARY KEY,
    order_id       VARCHAR(36)    NOT NULL,
    pay_method     VARCHAR(64)    NOT NULL,
    pay_status     VARCHAR(64)    NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    pay_time       TIMESTAMP,
    transaction_no VARCHAR(100),
    FOREIGN KEY (order_id) REFERENCES orders (id)
);

COMMENT ON TABLE payments IS '支付记录表';
COMMENT ON COLUMN payments.id IS '支付记录ID';
COMMENT ON COLUMN payments.order_id IS '订单ID';
COMMENT ON COLUMN payments.pay_method IS '支付方式(alipay/wechat/bank_card)';
COMMENT ON COLUMN payments.pay_status IS '支付状态(unpaid/paid/refunded)';
COMMENT ON COLUMN payments.amount IS '支付金额';
COMMENT ON COLUMN payments.pay_time IS '支付时间';
COMMENT ON COLUMN payments.transaction_no IS '交易流水号';

-- 用户地址表
CREATE TABLE user_addresses
(
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id        VARCHAR(36)  NOT NULL,
    receiver_name  VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    province       VARCHAR(50)  NOT NULL,
    city           VARCHAR(50)  NOT NULL,
    district       VARCHAR(50)  NOT NULL,
    detail_address VARCHAR(200) NOT NULL,
    is_default     BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE user_addresses IS '用户地址表';
COMMENT ON COLUMN user_addresses.id IS '地址ID';
COMMENT ON COLUMN user_addresses.user_id IS '用户ID';
COMMENT ON COLUMN user_addresses.receiver_name IS '收货人姓名';
COMMENT ON COLUMN user_addresses.phone IS '联系电话';
COMMENT ON COLUMN user_addresses.province IS '省份';
COMMENT ON COLUMN user_addresses.city IS '城市';
COMMENT ON COLUMN user_addresses.district IS '区县';
COMMENT ON COLUMN user_addresses.detail_address IS '详细地址';
COMMENT ON COLUMN user_addresses.is_default IS '是否默认地址';

-- 库存表
CREATE TABLE inventory
(
    product_id VARCHAR(36) NOT NULL PRIMARY KEY,
    stock      INT         NOT NULL DEFAULT 0,
    min_stock  INT         NOT NULL DEFAULT 0,
    updated_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

COMMENT ON TABLE inventory IS '库存表';
COMMENT ON COLUMN inventory.product_id IS '商品ID';
COMMENT ON COLUMN inventory.stock IS '当前库存';
COMMENT ON COLUMN inventory.min_stock IS '最低库存预警值';
COMMENT ON COLUMN inventory.updated_at IS '更新时间';

-- 订单物流表
CREATE TABLE order_logistics
(
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    order_id         VARCHAR(36)  NOT NULL,
    tracking_no      VARCHAR(100),
    carrier          VARCHAR(100),
    logistics_status VARCHAR(64)  NOT NULL,
    ship_time        TIMESTAMP,
    receive_time     TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

COMMENT ON TABLE order_logistics IS '订单物流表';
COMMENT ON COLUMN order_logistics.id IS '物流记录ID';
COMMENT ON COLUMN order_logistics.order_id IS '订单ID';
COMMENT ON COLUMN order_logistics.tracking_no IS '物流单号';
COMMENT ON COLUMN order_logistics.carrier IS '物流公司';
COMMENT ON COLUMN order_logistics.logistics_status IS '物流状态(pending/shipped/in_transit/received)';
COMMENT ON COLUMN order_logistics.ship_time IS '发货时间';
COMMENT ON COLUMN order_logistics.receive_time IS '签收时间';

-- 商品分类表（树形结构）
CREATE TABLE product_categories
(
    id        VARCHAR(36)  NOT NULL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    parent_id VARCHAR(36),
    sort      INT DEFAULT 0,
    FOREIGN KEY (parent_id) REFERENCES product_categories (id) ON DELETE SET NULL
);

COMMENT ON TABLE product_categories IS '商品分类表';
COMMENT ON COLUMN product_categories.id IS '分类ID';
COMMENT ON COLUMN product_categories.name IS '分类名称';
COMMENT ON COLUMN product_categories.parent_id IS '父分类ID';
COMMENT ON COLUMN product_categories.sort IS '排序值';

-- 商品评价表
CREATE TABLE product_reviews
(
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    order_id   VARCHAR(36)  NOT NULL,
    product_id VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    rating     INT          NOT NULL,
    content    VARCHAR(500),
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON TABLE product_reviews IS '商品评价表';
COMMENT ON COLUMN product_reviews.id IS '评价ID';
COMMENT ON COLUMN product_reviews.order_id IS '订单ID';
COMMENT ON COLUMN product_reviews.product_id IS '商品ID';
COMMENT ON COLUMN product_reviews.user_id IS '用户ID';
COMMENT ON COLUMN product_reviews.rating IS '评分(1-5)';
COMMENT ON COLUMN product_reviews.content IS '评价内容';
COMMENT ON COLUMN product_reviews.created_at IS '评价时间';

-- ========================================
-- 认证与权限表
-- ========================================

-- 角色表
CREATE TABLE IF NOT EXISTS sql_forge_role
(
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

COMMENT ON TABLE sql_forge_role IS '角色表';
COMMENT ON COLUMN sql_forge_role.id IS '角色ID';
COMMENT ON COLUMN sql_forge_role.name IS '角色名称';
COMMENT ON COLUMN sql_forge_role.description IS '角色描述';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sql_forge_user_role
(
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sql_forge_user_role IS '用户-角色关联表';
COMMENT ON COLUMN sql_forge_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sql_forge_user_role.role_id IS '角色ID';

-- 角色-模板关联表
CREATE TABLE IF NOT EXISTS sql_forge_role_template
(
    role_id     VARCHAR(64) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_id, template_id)
);

COMMENT ON TABLE sql_forge_role_template IS '角色-模板关联表';
COMMENT ON COLUMN sql_forge_role_template.role_id IS '角色ID';
COMMENT ON COLUMN sql_forge_role_template.template_id IS '模板ID';

-- ========================================
-- SQL模板和AMIS模板表
-- ========================================

CREATE TABLE sql_forge_template_sql
(
    id            VARCHAR(64) NOT NULL PRIMARY KEY,
    name          VARCHAR(100),
    description   VARCHAR(500),
    executor_name VARCHAR(50) NOT NULL,
    context       TEXT        NOT NULL
);

COMMENT ON TABLE sql_forge_template_sql IS 'SQL模板表';
COMMENT ON COLUMN sql_forge_template_sql.id IS 'SQL模板ID';
COMMENT ON COLUMN sql_forge_template_sql.name IS '模板名称';
COMMENT ON COLUMN sql_forge_template_sql.description IS '模板描述';
COMMENT ON COLUMN sql_forge_template_sql.executor_name IS '数据源';
COMMENT ON COLUMN sql_forge_template_sql.context IS '模板内容';

CREATE TABLE sql_forge_template_amis
(
    id          VARCHAR(64) NOT NULL PRIMARY KEY,
    name        VARCHAR(100),
    description VARCHAR(500),
    context     TEXT        NOT NULL
);

COMMENT ON TABLE sql_forge_template_amis IS 'AMIS模板表';
COMMENT ON COLUMN sql_forge_template_amis.id IS 'AMIS模板ID';
COMMENT ON COLUMN sql_forge_template_amis.name IS '模板名称';
COMMENT ON COLUMN sql_forge_template_amis.description IS '模板描述';
COMMENT ON COLUMN sql_forge_template_amis.context IS '模板内容';
