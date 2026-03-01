-- 插入字典数据
INSERT INTO sys_dicts (id, dict_code, dict_name)
VALUES ('1', 'sex', '性别'),
       ('2', 'categories', '商品分类'),
       ('3', 'order_status', '订单状态');

INSERT INTO sys_dict_items (id, dict_code, item_code, item_name, sort)
VALUES ('1-1', 'sex', 'male', '男', 1),
       ('1-2', 'sex', 'female', '女', 2),
       ('2-1', 'categories', 'electronic', '电子产品', 1),
       ('2-2', 'categories', 'clothing', '服装', 2),
       ('2-3', 'categories', 'book', '图书', 3),
       ('2-4', 'categories', 'household', '家居用品', 4),
       ('2-5', 'categories', 'food', '食品', 5),
       ('3-1', 'order_status', 'pending', '进行中', 5),
       ('3-2', 'order_status', 'completed', '已完成', 5),
       ('3-3', 'order_status', 'cancelled', '已取消', 5);

-- 插入用户数据
INSERT INTO users (id, username, dict_sex, email)
VALUES ('1', 'alice', 'female', 'alice@example.com'),
       ('2', 'bob', 'male', 'bob@example.com'),
       ('3', 'charlie', 'male', 'charlie@example.com'),
       ('4', 'wubo01', 'male', 'wubo01@@example.com'),
       ('5', 'wubo02', 'male', 'wubo02@example.com'),
       ('6', 'wubo03', 'male', 'wubo03@example.com'),
       ('7', 'wubo04', 'male', 'wubo04@example.com'),
       ('8', 'wubo05', 'male', 'wubo05@example.com'),
       ('9', 'wubo06', 'male', 'wubo06@example.com'),
       ('10', 'wubo07', 'male', 'wubo07@example.com'),
       ('11', 'wubo08', 'male', 'wubo08@example.com'),
       ('12', 'wubo09', 'male', 'wubo09@example.com'),
       ('13', 'wubo10', 'male', 'wubo10@example.com'),
       ('14', 'wubo11', 'male', 'wubo11@example.com'),
       ('15', 'wubo12', 'male', 'wubo12@example.com'),
       ('16', 'wubo13', 'male', 'wubo13@example.com'),
       ('17', 'wubo14', 'male', 'wubo14@example.com'),
       ('18', 'wubo15', 'male', 'wubo15@example.com'),
       ('19', 'wubo16', 'male', 'wubo16@example.com'),
       ('20', 'wubo17', 'male', 'wubo17@example.com'),
       ('21', 'wubo18', 'male', 'wubo18@example.com'),
       ('22', 'wubo19', 'male', 'wubo19@example.com'),
       ('23', 'wubo20', 'male', 'wubo20@example.com');

-- 插入商品数据
INSERT INTO products (id, name, dict_categories, price)
VALUES ('1', '笔记本电脑', 'electronic', 3250.),
       ('2', '鼠标', 'electronic', 25.50),
       ('3', '键盘', 'electronic', 75.00),
       ('4', '毛选', 'book', 21.25),
       ('5', '工装', 'clothing', 200.00);

-- 插入订单数据
INSERT INTO orders (id, user_id, total_amount, dict_order_status)
VALUES ('1', '1', 6551, 'pending'),
       ('2', '1', 1500, 'completed'),
       ('3', '2', 108.75, 'completed'),
       ('4', '3', 350.25, 'completed'),
       ('5', '3', 350.25, 'cancelled');

-- 插入订单明细数据
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price)
VALUES ('1-1', '1', '1', 2, 3250),
       ('1-2', '1', '2', 2, 25.5),
       ('2-1', '2', '5', 17, 75),
       ('3-1', '3', '4', 33, 21.25),
       ('4-1', '4', '2', 5, 25.5),
       ('4-2', '4', '3', 5, 75),
       ('5-1', '5', '2', 5, 25.5),
       ('5-2', '5', '3', 5, 75);

-- 查询数据
SELECT u.username,
       sex.item_name             AS sex_name,
       o.total_amount,
       p.name               AS product_name,
       categories.item_name AS product_categories,
       oi.unit_price,
       oi.quantity,
       p.price
FROM orders o
         LEFT JOIN users u ON o.user_id = u.id
         LEFT JOIN sys_dict_items sex ON u.dict_sex = sex.item_code
         LEFT JOIN order_items oi ON o.id = oi.order_id
         LEFT JOIN products p ON oi.product_id = p.id
         LEFT JOIN sys_dict_items categories ON p.dict_categories = categories.item_code
WHERE categories.dict_code = 'categories'
  AND sex.dict_code = 'sex';

