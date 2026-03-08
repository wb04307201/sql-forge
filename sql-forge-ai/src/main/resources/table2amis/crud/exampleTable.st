```json
[{
  "table": "USERS",
  "desc": "用户表",
  "type": "table",
  "fields": {
    "ID": {"type": "string", "pk": true, "desc": "用户ID"},
    "USERNAME": {"type": "string", "max": 50, "desc": "用户名","search": true},
    "DICT_SEX": {"type": "string", "max": 100, "desc": "性别", "ref": {"type":"JOIN","table": "sys_dict_items", "on": "item_code", "filter": {"dict_code": "sex"}},"search": true},
    "EMAIL": {"type": "string", "max": 100, "desc": "邮箱地址","search": true}
  }
},
{
  "table": "sys_dict_items",
  "desc": "字典项表",
  "type": "ref",
  "fields": {
    "item_code": {"type": "string", "desc": "字典项编码"},
    "dict_code": {"type": "string", "desc": "字典项编码"},
    "item_name": {"type": "string", "desc": "字典项名称"}
  }
}]
```