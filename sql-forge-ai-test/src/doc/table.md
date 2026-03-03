```json
{
  "table": "USERS",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "用户ID"},
    "USERNAME": {"type": "string", "max": 50, "desc": "用户名"},
    "DICT_SEX": {"type": "string", "max": 100, "desc": "性别字典code", "ref": "sys_dict_items.item_code"},
    "EMAIL": {"type": "string", "max": 100, "desc": "邮箱地址"}
  }
}
```

```json
{
  "table": "sys_dict_items",
  "fields": {
    "item_code": {"type": "string", "desc": "字典项值"},
    "item_name": {"type": "string", "desc": "字典项标签"},
    "dict_code": {"type": "string", "desc": "字典分类编码"}
  },
  "filter": {"dict_code": "sex"}
}
```