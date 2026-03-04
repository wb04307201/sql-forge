```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "syncLocation": false,
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/USERS",
      "data": {
        "@column": ["ID", "USERNAME", "EMAIL", "sex.item_name AS sex_name"],
        "@where": [
          {
            "column": "USERNAME",
            "condition": "LIKE",
            "value": "${USERNAME | default: undefined}"
          },
          {
            "column": "DICT_SEX",
            "condition": "IN",
            "value": "${DICT_SEX | default: undefined}"
          }
        ],
        "@join": [
          {
            "type": "LEFT JOIN",
            "joinTable": "sys_dict_items sex",
            "on": "DICT_SEX = sex.item_code AND sex.dict_code = 'sex'"
          }
        ],
        "@order": "${orderBy && orderDir ? (orderBy + ' ' + orderDir) : ''}",
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "filter": {
      "title": "条件搜索",
      "body": [
        {
          "type": "input-text",
          "name": "USERNAME",
          "label": "用户名",
          "placeholder": "请输入用户名",
          "addOn": {
            "label": "搜索",
            "type": "submit",
            "level": "primary"
          }
        },
        {
          "type": "select",
          "name": "DICT_SEX",
          "label": "性别",
          "multiple": true,
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
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          }
        }
      ]
    },
    "headerToolbar": [
      {
        "type": "button",
        "icon": "fa fa-plus",
        "label": "新增",
        "level": "primary",
        "actionType": "drawer",
        "drawer": {
          "title": "新增用户",
          "size": "lg",
          "body": {
            "type": "form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/USERS",
              "data": {
                "@set": {
                  "ID": "${ID}",
                  "USERNAME": "${USERNAME}",
                  "DICT_SEX": "${DICT_SEX}",
                  "EMAIL": "${EMAIL}"
                }
              }
            },
            "body": [
              {
                "type": "uuid",
                "name": "ID",
                "label": "用户ID",
                "hidden": true
              },
              {
                "type": "input-text",
                "name": "USERNAME",
                "label": "用户名",
                "required": true,
                "maxLength": 50
              },
              {
                "type": "select",
                "name": "DICT_SEX",
                "label": "性别",
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
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                },
                "valueField": "item_code",
                "labelField": "item_name"
              },
              {
                "type": "input-text",
                "name": "EMAIL",
                "label": "邮箱地址",
                "maxLength": 100
              }
            ]
          }
        }
      },
      "bulkActions",
      {
        "type": "columns-toggler"
      },
      {
        "type": "export-excel",
        "label": "导出",
        "icon": "fa fa-download",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/select/USERS",
          "data": {
            "@column": ["ID", "USERNAME", "EMAIL", "sex.item_name AS sex_name"],
            "@where": [
              {
                "column": "USERNAME",
                "condition": "LIKE",
                "value": "${USERNAME | default: undefined}"
              },
              {
                "column": "DICT_SEX",
                "condition": "IN",
                "value": "${DICT_SEX | default: undefined}"
              }
            ],
            "@join": [
              {
                "type": "LEFT JOIN",
                "joinTable": "sys_dict_items sex",
                "on": "DICT_SEX = sex.item_code AND sex.dict_code = 'sex'"
              }
            ],
            "@order": "${orderBy && orderDir ? (orderBy + ' ' + orderDir) : ''}"
          }
        }
      }
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
                "value": "${ids}"
              }
            ]
          }
        },
        "confirmText": "确定要批量删除选中的用户吗？"
      }
    ],
    "columns": [
      {
        "name": "ID",
        "label": "用户ID",
        "visible": false
      },
      {
        "name": "USERNAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USERNAME",
          "placeholder": "请输入用户名"
        }
      },
      {
        "name": "sex_name",
        "label": "性别",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "DICT_SEX",
          "multiple": true,
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
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          }
        }
      },
      {
        "name": "EMAIL",
        "label": "邮箱地址",
        "sortable": true
      },
      {
        "type": "operation",
        "label": "操作",
        "fixed": "right",
        "buttons": [
          {
            "type": "button",
            "icon": "fa fa-pencil",
            "tooltip": "修改",
            "actionType": "drawer",
            "drawer": {
              "title": "编辑用户",
              "size": "lg",
              "body": {
                "type": "form",
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/USERS",
                  "data": {
                    "@set": {
                      "USERNAME": "${USERNAME}",
                      "DICT_SEX": "${DICT_SEX}",
                      "EMAIL": "${EMAIL}"
                    },
                    "@where": [
                      {
                        "column": "ID",
                        "condition": "EQ",
                        "value": "${ID}"
                      }
                    ]
                  }
                },
                "body": [
                  {
                    "type": "hidden",
                    "name": "ID",
                    "value": "${ID}"
                  },
                  {
                    "type": "input-text",
                    "name": "USERNAME",
                    "label": "用户名",
                    "required": true,
                    "maxLength": 50
                  },
                  {
                    "type": "select",
                    "name": "DICT_SEX",
                    "label": "性别",
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
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                    },
                    "valueField": "item_code",
                    "labelField": "item_name"
                  },
                  {
                    "type": "input-text",
                    "name": "EMAIL",
                    "label": "邮箱地址",
                    "maxLength": 100
                  }
                ]
              }
            }
          },
          {
            "type": "button",
            "icon": "fa fa-times text-danger",
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
                    "value": "${ID}"
                  }
                ]
              }
            },
            "confirmText": "确定要删除该用户吗？",
            "level": "danger"
          }
        ]
      }
    ],
    "footerToolbar": ["switch-per-page", "pagination"]
  }
}
```