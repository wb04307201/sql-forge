-- AMIS 模板种子数据（简化版用于测试导航加载）

-- 1. 商品管理
INSERT INTO sql_forge_template_amis (id, name, context)
VALUES ('amis-template-products', '商品管理', '{
  "type": "page",
  "title": "商品管理",
  "body": {
    "type": "crud",
    "api": "/sql/forge/api/json/selectPage/products",
    "headerToolbar": ["add"],
    "columns": [
      {"name": "id", "label": "ID"},
      {"name": "name", "label": "商品名称"},
      {"name": "price", "label": "价格"}
    ]
  }
}');

-- 2. 订单管理
INSERT INTO sql_forge_template_amis (id, name, context)
VALUES ('amis-template-orders', '订单管理', '{
  "type": "page",
  "title": "订单管理",
  "body": {
    "type": "crud",
    "api": "/sql/forge/api/json/selectPage/orders",
    "columns": [
      {"name": "id", "label": "订单ID"},
      {"name": "user_id", "label": "用户ID"},
      {"name": "product_id", "label": "产品ID"},
      {"name": "quantity", "label": "数量"},
      {"name": "order_date", "label": "日期"}
    ]
  }
}');

-- 3. 订单明细
INSERT INTO sql_forge_template_amis (id, name, context)
VALUES ('amis-template-order-items', '订单明细', '{
  "type": "page",
  "title": "订单明细",
  "body": "订单明细页面"
}');

-- 4. 订单总览
INSERT INTO sql_forge_template_amis (id, name, context)
VALUES ('amis-template-order-overview', '订单总览', '{
  "type": "page",
  "title": "订单总览",
  "body": "订单总览页面"
}');
