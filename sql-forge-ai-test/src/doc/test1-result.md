```json
{
  "type": "page",
  "title": "用户管理",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "name": "users_crud",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/USERS",
      "data": {
        "@column": [
          "u.ID",
          "u.USERNAME",
          "u.DICT_SEX",
          "sex.item_name AS SEX_NAME",
          "u.EMAIL"
        ],
        "@where": [
          {
            "column": "u.USERNAME",
            "condition": "LIKE",
            "value": "${username | default:undefined}"
          },
          {
            "column": "u.DICT_SEX",
            "condition": "IN",
            "value": "${dict_sex | default:undefined | split}"
          },
          {
            "column": "u.EMAIL",
            "condition": "LIKE",
            "value": "${email | default:undefined}"
          }
        ],
        "@join": [
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items sex",
            "on": "u.DICT_SEX = sex.item_code AND sex.dict_code = 'sex'"
          }
        ],
        "@order": "${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}",
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      },
      "adaptor": "return { data: payload.rows || [], count: payload.total || 0 };"
    },
    "autoGenerateFilter": {
      "columns": ["USERNAME", "DICT_SEX", "EMAIL"]
    },
    "filter": {
      "title": "条件筛选",
      "body": [
        {
          "type": "input-text",
          "name": "username",
          "label": "用户名",
          "addOn": {
            "label": "搜索",
            "type": "submit",
            "icon": "fa fa-search"
          }
        },
        {
          "type": "select",
          "name": "dict_sex",
          "label": "性别",
          "clearable": true,
          "mode": "inline",
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": ["item_code", "item_name"],
              "@where": [
                {
                  "column": "dict_code",
                  "condition": "EQ",
                  "value": "sex"
                }
              ]
            },
            "adaptor": "return { options: payload.map(item => ({ value: item.item_code, label: item.item_name })) };"
          }
        },
        {
          "type": "input-text",
          "name": "email",
          "label": "邮箱"
        }
      ]
    },
    "headerToolbar": [
      {
        "type": "button",
        "icon": "fa fa-plus",
        "label": "新增",
        "actionType": "drawer",
        "drawer": {
          "id": "insert-drawer",
          "title": "新增用户",
          "size": "lg",
          "body": {
            "type": "form",
            "id": "insert-form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/USERS",
              "data": {
                "@set": {
                  "ID": "${id}",
                  "USERNAME": "${username | default:undefined}",
                  "DICT_SEX": "${dict_sex | default:undefined}",
                  "EMAIL": "${email | default:undefined}"
                },
                "@with_select": {
                  "@column": ["ID"],
                  "@where": [
                    {
                      "column": "ID",
                      "condition": "EQ",
                      "value": "${id}"
                    }
                  ]
                }
              }
            },
            "body": [
              {
                "type": "input-text",
                "name": "id",
                "label": "用户ID",
                "value": "${uuid()}",
                "hidden": true
              },
              {
                "type": "input-text",
                "name": "username",
                "label": "用户名",
                "required": true,
                "maxLength": 50
              },
              {
                "type": "select",
                "name": "dict_sex",
                "label": "性别",
                "clearable": true,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/sys_dict_items",
                  "data": {
                    "@column": ["item_code", "item_name"],
                    "@where": [
                      {
                        "column": "dict_code",
                        "condition": "EQ",
                        "value": "sex"
                      }
                    ]
                  },
                  "adaptor": "return { options: payload.map(item => ({ value: item.item_code, label: item.item_name })) };"
                }
              },
              {
                "type": "input-email",
                "name": "email",
                "label": "邮箱地址",
                "maxLength": 100
              }
            ]
          }
        }
      },
      {
        "type": "export-excel",
        "icon": "fa fa-download",
        "label": "导出",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/select/USERS",
          "data": {
            "@column": [
              "u.ID",
              "u.USERNAME",
              "u.DICT_SEX",
              "sex.item_name AS SEX_NAME",
              "u.EMAIL"
            ],
            "@where": [
              {
                "column": "u.USERNAME",
                "condition": "LIKE",
                "value": "${username | default:undefined}"
              },
              {
                "column": "u.DICT_SEX",
                "condition": "IN",
                "value": "${dict_sex | default:undefined | split}"
              },
              {
                "column": "u.EMAIL",
                "condition": "LIKE",
                "value": "${email | default:undefined}"
              }
            ],
            "@join": [
              {
                "type": "LEFT_OUTER_JOIN",
                "joinTable": "sys_dict_items sex",
                "on": "u.DICT_SEX = sex.item_code AND sex.dict_code = 'sex'"
              }
            ],
            "@order": "${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}"
          }
        }
      },
      "bulkActions",
      "columns-toggler"
    ],
    "bulkActions": [
      {
        "label": "批量删除",
        "actionType": "ajax",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/delete/USERS",
          "data": {
            "@where": [
              {
                "column": "ID",
                "condition": "IN",
                "value": "${ids | join:','}"
              }
            ]
          }
        },
        "confirmText": "确定要批量删除选中的用户吗？",
        "reload": "crud_table"
      }
    ],
    "columns": [
      {
        "name": "ID",
        "label": "用户ID",
        "hidden": true
      },
      {
        "name": "USERNAME",
        "label": "用户名",
        "searchable": {
          "type": "input-text",
          "name": "USERNAME",
          "addOn": {
            "label": "搜索",
            "type": "submit"
          }
        }
      },
      {
        "name": "SEX_NAME",
        "label": "性别",
        "type": "text"
      },
      {
        "name": "EMAIL",
        "label": "邮箱地址",
        "searchable": {
          "type": "input-text",
          "name": "EMAIL"
        }
      },
      {
        "type": "operation",
        "label": "操作",
        "width": 150,
        "buttons": [
          {
            "type": "button",
            "icon": "fa fa-pencil",
            "tooltip": "编辑",
            "actionType": "drawer",
            "drawer": {
              "id": "update-drawer",
              "title": "编辑用户",
              "size": "lg",
              "body": {
                "type": "form",
                "id": "update-form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/USERS",
                  "data": {
                    "@column": ["ID", "USERNAME", "DICT_SEX", "EMAIL"],
                    "@where": [
                      {
                        "column": "ID",
                        "condition": "EQ",
                        "value": "${id}"
                      }
                    ]
                  },
                  "adaptor": "return { data: payload[0] || {} };"
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/USERS",
                  "data": {
                    "@set": {
                      "USERNAME": "${username | default:undefined}",
                      "DICT_SEX": "${dict_sex | default:undefined}",
                      "EMAIL": "${email | default:undefined}"
                    },
                    "@where": [
                      {
                        "column": "ID",
                        "condition": "EQ",
                        "value": "${id}"
                      }
                    ]
                  }
                },
                "body": [
                  {
                    "type": "input-text",
                    "name": "id",
                    "label": "用户ID",
                    "hidden": true,
                    "static": true
                  },
                  {
                    "type": "input-text",
                    "name": "username",
                    "label": "用户名",
                    "required": true,
                    "maxLength": 50
                  },
                  {
                    "type": "select",
                    "name": "dict_sex",
                    "label": "性别",
                    "clearable": true,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/sys_dict_items",
                      "data": {
                        "@column": ["item_code", "item_name"],
                        "@where": [
                          {
                            "column": "dict_code",
                            "condition": "EQ",
                            "value": "sex"
                          }
                        ]
                      },
                      "adaptor": "return { options: payload.map(item => ({ value: item.item_code, label: item.item_name })) };"
                    }
                  },
                  {
                    "type": "input-email",
                    "name": "email",
                    "label": "邮箱地址",
                    "maxLength": 100
                  }
                ]
              }
            }
          },
          {
            "type": "button",
            "icon": "fa fa-trash",
            "tooltip": "删除",
            "actionType": "ajax",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/delete/USERS",
              "data": {
                "@where": [
                  {
                    "column": "ID",
                    "condition": "EQ",
                    "value": "${id}"
                  }
                ]
              }
            },
            "confirmText": "确定要删除该用户吗？",
            "className": "text-danger",
            "reload": "crud_table"
          }
        ]
      }
    ],
    "footerToolbar": ["switch-per-page", "pagination"]
  }
}
```