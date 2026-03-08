```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/USERS",
      "data": {
        "@column": [
          "USERS.ID",
          "USERS.USERNAME",
          "sex.item_name as SEX",
          "USERS.EMAIL"
        ],
        "@join": [
          {
            "type": "JOIN",
            "joinTable": "sys_dict_items sex",
            "on": "USERS.DICT_SEX = sex.item_code"
          }
        ],
        "@where": [
          {
            "column": "USERS.USERNAME",
            "condition": "LIKE",
            "value": "${USERNAME | default:undefined}"
          },
          {
            "column": "USERS.DICT_SEX",
            "condition": "IN",
            "value": "${SEX | default:undefined | split}"
          },
          {
            "column": "USERS.EMAIL",
            "condition": "LIKE",
            "value": "${EMAIL | default:undefined}"
          },
          {
            "column": "sex.dict_code",
            "condition": "EQ",
            "value": "sex"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (orderBy + ' ' + orderDir):'',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "headerToolbar": [
      {
        "label": "新增",
        "type": "button",
        "icon": "fa fa-plus",
        "level": "primary",
        "actionType": "drawer",
        "drawer": {
          "title": "新增表单",
          "body": {
            "type": "form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/USERS",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "USERNAME": "${USERNAME | default:undefined}",
                  "DICT_SEX": "${DICT_SEX | default:undefined}",
                  "EMAIL": "${EMAIL | default:undefined}"
                }
              }
            },
            "onEvent": {
              "submitSucc": {
                "actions": [
                  {
                    "actionType": "reload",
                    "componentId": "crud_table"
                  }
                ]
              }
            },
            "body": [
              {
                "type": "uuid",
                "id": "insert-ID",
                "name": "ID"
              },
              {
                "type": "input-text",
                "name": "USERNAME",
                "label": "用户名",
                "maxLength": 50,
                "disabled": false,
                "id": "insert-USERNAME"
              },
              {
                "type": "select",
                "name": "DICT_SEX",
                "label": "性别",
                "maxLength": 100,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/sys_dict_items",
                  "data": {
                    "@column": [
                      "item_code",
                      "item_name"
                    ],
                    "@where": [
                      {
                        "column": "dict_code",
                        "condition": "EQ",
                        "value": "sex"
                      }
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name ||  item.ITEM_NAME\n  }))\n};"
                },
                "clearable": true,
                "disabled": false,
                "id": "insert-SEX"
              },
              {
                "type": "input-text",
                "name": "EMAIL",
                "label": "用户邮箱地址",
                "maxLength": 100,
                "disabled": false,
                "id": "insert-EMAIL"
              }
            ]
          }
        }
      },
      "bulkActions",
      {
        "type": "columns-toggler",
        "draggable": true,
        "align": "right"
      },
      {
        "type": "export-excel",
        "label": "导出",
        "icon": "fa fa-file-excel",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/select/USERS",
          "data": {
            "@column": [
              "USERS.ID",
              "USERS.USERNAME",
              "sex.item_name as SEX",
              "USERS.EMAIL"
            ],
            "@join": [
              {
                "type": "JOIN",
                "joinTable": "sys_dict_item sex",
                "on": "USERS.DICT_SEX = sex.item_code"
              }
            ],
            "@where": [
              {
                "column": "USERS.USERNAME",
                "condition": "LIKE",
                "value": "${USERNAME | default:undefined}"
              },
              {
                "column": "USERS.DICT_SEX",
                "condition": "IN",
                "value": "${DICT_SEX | default:undefined | split}"
              },
              {
                "column": "USERS.EMAIL",
                "condition": "LIKE",
                "value": "${EMAIL | default:undefined}"
              },
              {
                "column": "sex.dict_code",
                "condition": "EQ",
                "value": "sex"
              }
            ]
          }
        },
        "align": "right"
      }
    ],
    "footerToolbar": [
      "statistics",
      {
        "type": "pagination",
        "layout": "total,perPage,pager,go"
      }
    ],
    "bulkActions": [
      {
        "label": "批量删除",
        "icon": "fa fa-trash",
        "actionType": "ajax",
        "api": {
          "method": "post",
          "url": "/sql/forge/api/json/delete/USERS",
          "data": {
            "@where": [
              {
                "column": "ID",
                "condition": "IN",
                "value": "${ids | split}"
              }
            ]
          }
        },
        "confirmText": "确定要批量删除?"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "labelTpl": "${USERNAME}",
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ID",
    "columns": [
      {
        "name": "ID",
        "hidden": true
      },
      {
        "name": "USERNAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USERNAME",
          "label": "用户名",
          "maxLength": 50,
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "SEX",
        "label": "性别",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "SEX",
          "label": "性别",
          "maxLength": 100,
          "placeholder": "输入性别",
          "multiple": true,
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": [
                "item_code",
                "item_name"
              ],
              "@where": [
                {
                  "column": "dict_code",
                  "condition": "EQ",
                  "value": "sex"
                }
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name ||  item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "EMAIL",
        "label": "用户邮箱地址",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "EMAIL",
          "label": "用户邮箱地址",
          "maxLength": 100,
          "placeholder": "输入用户邮箱地址"
        }
      },
      {
        "type": "operation",
        "label": "操作",
        "buttons": [
          {
            "label": "修改",
            "type": "button",
            "icon": "fa fa-pen-to-square",
            "actionType": "drawer",
            "drawer": {
              "title": "新增表单",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/USERS",
                  "data": {
                    "@column": [
                      "USERS.ID",
                      "USERS.USERNAME",
                      "USERS.SEX",
                      "USERS.EMAIL"
                    ],
                    "@join": [
                      {
                        "type": "JOIN",
                        "joinTable": "sys_dict_item sex_a814d446",
                        "on": "USERS.SEX = sex_a814d446.item_code"
                      }
                    ],
                    "@where": [
                      {
                        "column": "USERS.ID",
                        "condition": "EQ",
                        "value": "${ID}"
                      }
                    ]
                  },
                  "responseData": {
                    "&": "${items | first}"
                  }
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/USERS",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "USERNAME": "${USERNAME}",
                      "SEX": "${SEX}",
                      "EMAIL": "${EMAIL}"
                    },
                    "@where": [
                      {
                        "column": "USERS.ID",
                        "condition": "EQ",
                        "value": "${ID}"
                      }
                    ]
                  }
                },
                "body": [
                  {
                    "type": "input-text",
                    "name": "ID",
                    "hidden": true,
                    "id": "update-ID"
                  },
                  {
                    "type": "input-text",
                    "name": "USERNAME",
                    "label": "用户名",
                    "maxLength": 50,
                    "disabled": false,
                    "id": "update-USERNAME"
                  },
                  {
                    "type": "select",
                    "name": "SEX",
                    "label": "性别",
                    "maxLength": 100,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/sys_dict_item",
                      "data": {
                        "@column": [
                          "item_code",
                          "item_name"
                        ],
                        "@where": [
                          {
                            "column": "dict_code",
                            "condition": "EQ",
                            "value": "sex"
                          }
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name ||  item.ITEM_NAME\n  }))\n};"
                    },
                    "clearable": true,
                    "disabled": false,
                    "id": "update-SEX"
                  },
                  {
                    "type": "input-text",
                    "name": "EMAIL",
                    "label": "用户邮箱地址",
                    "maxLength": 100,
                    "disabled": false,
                    "id": "update-EMAIL"
                  }
                ]
              }
            }
          },
          {
            "label": "删除",
            "type": "button",
            "icon": "fa fa-minus",
            "actionType": "ajax",
            "level": "danger",
            "confirmText": "确认要删除？",
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
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}
```