```json
{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_products",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/products",
      "data": {
        "@column": [
          "products.ID",
          "products.name",
          "categories.item_name as dict_categories_name",
          "products.price"
        ],
        "@join": [
          {
            "type": "JOIN",
            "joinTable": "sys_dict_items categories",
            "on": "products.dict_categories = categories.item_code"
          }
        ],
        "@where": [
          {
            "column": "products.name",
            "condition": "LIKE",
            "value": "${name | default:undefined}"
          },
          {
            "column": "products.dict_categories",
            "condition": "IN",
            "value": "${dict_categories | default:undefined | split}"
          },
          {
            "column": "products.price",
            "condition": "EQ",
            "value": "${price | default:undefined}"
          },
          {
            "column": "categories.dict_code",
            "condition": "EQ",
            "value": "categories"
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
          "title": "新增商品",
          "body": {
            "type": "form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/products",
              "data": {
                "@set": {
                  "ID": "${ID}",
                  "name": "${name}",
                  "dict_categories": "${dict_categories}",
                  "price": "${price}"
                }
              }
            },
            "onEvent": {
              "submitSucc": {
                "actions": [
                  {
                    "actionType": "reload",
                    "componentId": "crud_products"
                  }
                ]
              }
            },
            "body": [
              {
                "type": "uuid",
                "name": "ID",
                "hidden": true
              },
              {
                "type": "input-text",
                "name": "name",
                "label": "商品名称",
                "maxLength": 50,
                "required": true
              },
              {
                "type": "select",
                "name": "dict_categories",
                "label": "商品类型",
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
                        "value": "categories"
                      }
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code,\n    label: item.item_name\n  }))\n};"
                },
                "clearable": true
              },
              {
                "type": "input-number",
                "name": "price",
                "label": "价格",
                "max": 10,
                "precision": 2
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
          "url": "/sql/forge/api/json/select/products",
          "data": {
            "@column": [
              "products.ID",
              "products.name",
              "categories.item_name as dict_categories_name",
              "products.price"
            ],
            "@join": [
              {
                "type": "JOIN",
                "joinTable": "sys_dict_items categories",
                "on": "products.dict_categories = categories.item_code"
              }
            ],
            "@where": [
              {
                "column": "products.name",
                "condition": "LIKE",
                "value": "${name | default:undefined}"
              },
              {
                "column": "products.dict_categories",
                "condition": "IN",
                "value": "${dict_categories | default:undefined | split}"
              },
              {
                "column": "products.price",
                "condition": "EQ",
                "value": "${price | default:undefined}"
              },
              {
                "column": "categories.dict_code",
                "condition": "EQ",
                "value": "categories"
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
          "url": "/sql/forge/api/json/delete/products",
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
        "confirmText": "确定要批量删除选中的商品吗？"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "labelTpl": "${name}",
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
        "name": "name",
        "label": "商品名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "name",
          "label": "商品名称",
          "maxLength": 50,
          "placeholder": "输入商品名称"
        }
      },
      {
        "name": "dict_categories_name",
        "label": "商品类型",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "dict_categories",
          "label": "商品类型",
          "placeholder": "选择商品类型",
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
                  "value": "categories"
                }
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code,\n    label: item.item_name\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "price",
        "label": "价格",
        "sortable": true,
        "type": "number",
        "searchable": {
          "type": "input-number",
          "name": "price",
          "label": "价格",
          "placeholder": "输入价格",
          "max": 10,
          "precision": 2
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
              "title": "编辑商品",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/products",
                  "data": {
                    "@column": [
                      "products.ID",
                      "products.name",
                      "products.dict_categories",
                      "products.price"
                    ],
                    "@where": [
                      {
                        "column": "products.ID",
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
                  "url": "/sql/forge/api/json/update/products",
                  "data": {
                    "@set": {
                      "name": "${name}",
                      "dict_categories": "${dict_categories}",
                      "price": "${price}"
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
                    "type": "input-text",
                    "name": "ID",
                    "hidden": true
                  },
                  {
                    "type": "input-text",
                    "name": "name",
                    "label": "商品名称",
                    "maxLength": 50,
                    "required": true
                  },
                  {
                    "type": "select",
                    "name": "dict_categories",
                    "label": "商品类型",
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
                            "value": "categories"
                          }
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code,\n    label: item.item_name\n  }))\n};"
                    },
                    "clearable": true
                  },
                  {
                    "type": "input-number",
                    "name": "price",
                    "label": "价格",
                    "max": 10,
                    "precision": 2
                  }
                ]
              }
            }
          },
          {
            "label": "删除",
            "type": "button",
            "icon": "fa fa-trash",
            "actionType": "ajax",
            "level": "danger",
            "confirmText": "确认要删除该商品吗？",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/delete/products",
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