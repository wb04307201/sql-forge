```json
[{
  "table": "USERS",
  "desc": "用户表",
  "type": "crud",
  "fields": {
    "ID": {"type": "uuid", "desc": "用户ID"},
    "USERNAME": {"type": "string", "length": 50, "desc": "用户名","search": true},
    "DICT_SEX": {"type": "dict", "length": 100, "desc": "性别", "dict_code": "sex", "search": true},
    "EMAIL": {"type": "string", "length": 100, "desc": "邮箱地址","search": true}
  }
},
  {
    "table": "SYS_DICT_ITEMS",
    "desc": "字典项表",
    "type": "dict",
    "fields": {
      "DICT_CODE": {"type": "string"},
      "ITEM_CODE": {"type": "string"},
      "ITEM_NAME": {"type": "string"}
    }
  }]
```

```json
[{
  "table": "PRODUCTS",
  "desc": "商品表",
  "type": "crud",
  "fields": {
    "ID": {"type": "uuid", "desc": "商品ID"},
    "NAME": {"type": "string", "length": 50, "desc": "商品名称","search": true},
    "DICT_CATEGORIES": {"type": "dict", "length": 100, "desc": "商品类型", "dict_code": "categories", "search": true},
    "PRICE": {"type": "number", "max": 9999999999, "precision": 2, "desc": "邮箱地址", "search": true}
  }
},
  {
    "table": "SYS_DICT_ITEMS",
    "desc": "字典项表",
    "type": "dict",
    "fields": {
      "DICT_CODE": {"type": "string"},
      "ITEM_CODE": {"type": "string"},
      "ITEM_NAME": {"type": "string"}
    }
  }]
```