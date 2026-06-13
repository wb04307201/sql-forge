-- ========================================
-- SQL Forge - SQL模板和AMIS模板数据
-- 合并自: V1.2__template.sql, V1.7__data.sql, V1.12__templates.sql
-- 所有模板均已补充 name 和 description 字段
-- ========================================

-- ========================================
-- SQL模板
-- ========================================

INSERT INTO sql_forge_template_sql (id, name, description, executor_name, context)
VALUES ('sql-template-database', '数据库查询', '通用数据库查询模板', 'database', 'SELECT * FROM users WHERE 1=1
<if test="name != null && name != ''''">AND username = #{name}</if>
<if test="ids != null && !ids.isEmpty()"><foreach collection="ids" item="id" open="AND id IN (" separator="," close=")">#{id}</foreach></if>
<if test="(name == null || name == '''') && (ids == null || ids.isEmpty()) ">AND 0=1</if>
ORDER BY username DESC'),
       ('sql-template-calcite', 'Calcite跨库查询', '跨数据库联邦查询模板', 'calcite', 'select student.name, sum(score.grade) as grade
from MYSQL.student as student join POSTGRES.score as score on student.id=score.student_id where 1=1
<if test="ids == null || ids.isEmpty()">AND 0=1</if>
<if test="ids != null && !ids.isEmpty()">
<foreach collection="ids" item="id" open="AND student.id IN (" separator="," close=")">#{id}</foreach>
</if>
group by student.name');

-- ========================================
-- AMIS模板
-- ========================================

-- 1. 图表
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-chart', '图表', '数据可视化图表模板', '{
	"type": "page",
	"body": {
		"type": "chart",
		"api": {
			"method": "post",
			"url": "/sql/forge/api/template/sql/sql-template-calcite",
			"data": {
				"ids": [
					1,
					2,
					3,
					4,
					5,
					6,
					7
				]
			}
		},
		"height": "100vh",
		"config": {
			"xAxis": {
				"type": "category",
				"data": "${items | pick:name}"
			},
			"yAxis": {
				"type": "value"
			},
			"series": [{
				"data": "${items | pick:grade}",
				"type": "bar"
			}]
		}
	}
}');

-- 2. 字典管理
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-dicts', '字典管理', '系统字典及字典项管理', '{
  "type": "page",
  "style": {
    "width": "100vw",
    "height": "100vh"
  },
  "body": {
    "type": "flex",
    "style": {
      "width": "100%",
      "height": "100%"
    },
    "items": [
      {
        "style": {
          "width": "50%",
          "height": "100%"
        },
        "type": "wrapper",
        "body": {
          "type": "crud",
          "id": "crud_table",
          "api": {
            "method": "post",
            "url": "/sql/forge/api/json/selectPage/SYS_DICTS",
            "data": {
              "@column": [
                "ID",
                "DICT_CODE",
                "DICT_NAME"
              ],
              "@join": [],
              "@where": [
                {
                  "column": "DICT_CODE",
                  "condition": "LIKE",
                  "value": "${DICT_CODE | default:undefined}"
                },
                {
                  "column": "DICT_NAME",
                  "condition": "LIKE",
                  "value": "${DICT_NAME | default:undefined}"
                }
              ],
              "@order": [
                "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
                    "url": "/sql/forge/api/json/insert/SYS_DICTS",
                    "data": {
                      "@set": {
                        "ID": "${ID | default:undefined}",
                        "DICT_CODE": "${DICT_CODE | default:undefined}",
                        "DICT_NAME": "${DICT_NAME | default:undefined}"
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
                      "name": "DICT_CODE",
                      "label": "字典编码",
                      "maxLength": 64,
                      "disabled": false,
                      "id": "insert-DICT_CODE"
                    },
                    {
                      "type": "input-text",
                      "name": "DICT_NAME",
                      "label": "字典名称",
                      "maxLength": 100,
                      "disabled": false,
                      "id": "insert-DICT_NAME"
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
                "url": "/sql/forge/api/json/select/SYS_DICTS",
                "data": {
                  "@column": [
                    "ID",
                    "DICT_CODE",
                    "DICT_NAME"
                  ],
                  "@join": [],
                  "@where": [
                    {
                      "column": "DICT_CODE",
                      "condition": "LIKE",
                      "value": "${DICT_CODE | default:undefined}"
                    },
                    {
                      "column": "DICT_NAME",
                      "condition": "LIKE",
                      "value": "${DICT_NAME | default:undefined}"
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
                "url": "/sql/forge/api/json/delete/SYS_DICTS",
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
          "labelTpl": "${DICT_CODE}",
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
              "name": "DICT_CODE",
              "label": "字典编码",
              "sortable": true,
              "searchable": {
                "type": "input-text",
                "name": "DICT_CODE",
                "label": "字典编码",
                "maxLength": 64,
                "placeholder": "输入字典编码"
              }
            },
            {
              "name": "DICT_NAME",
              "label": "字典名称",
              "sortable": true,
              "searchable": {
                "type": "input-text",
                "name": "DICT_NAME",
                "label": "字典名称",
                "maxLength": 100,
                "placeholder": "输入字典名称"
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
                        "url": "/sql/forge/api/json/select/SYS_DICTS",
                        "data": {
                          "@column": [
                            "ID",
                            "DICT_CODE",
                            "DICT_NAME"
                          ],
                          "@join": [],
                          "@where": [
                            {
                              "column": "ID",
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
                        "url": "/sql/forge/api/json/update/SYS_DICTS",
                        "data": {
                          "@set": {
                            "ID": "${ID}",
                            "DICT_CODE": "${DICT_CODE}",
                            "DICT_NAME": "${DICT_NAME}"
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
                          "hidden": true,
                          "id": "update-ID"
                        },
                        {
                          "type": "input-text",
                          "name": "DICT_CODE",
                          "label": "字典编码",
                          "maxLength": 64,
                          "disabled": false,
                          "id": "update-DICT_CODE"
                        },
                        {
                          "type": "input-text",
                          "name": "DICT_NAME",
                          "label": "字典名称",
                          "maxLength": 100,
                          "disabled": false,
                          "id": "update-DICT_NAME"
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
                    "url": "/sql/forge/api/json/delete/SYS_DICTS",
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
          ],
          "onEvent": {
            "rowClick": {
              "actions": [
                {
                  "actionType": "reload",
                  "componentId": "detail_table",
                  "data": {
                    "DICT_CODE": "${event.data.item.DICT_CODE}"
                  }
                }
              ]
            }
          }
        }
      },
      {
        "style": {
          "width": "50%",
          "height": "100%"
        },
        "type": "wrapper",
        "body": {
          "type": "crud",
          "id": "detail_table",
          "api": {
            "method": "post",
            "url": "/sql/forge/api/json/selectPage/SYS_DICT_ITEMS",
            "data": {
              "@column": [
                "ID",
                "DICT_CODE",
                "ITEM_CODE",
                "ITEM_NAME",
                "SORT"
              ],
              "@join": [],
              "@where": [
                {
                  "column": "DICT_CODE",
                  "condition": "EQ",
                  "value": "${DICT_CODE | default:\"\"}"
                }
              ],
              "@order": [
                "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
                    "url": "/sql/forge/api/json/insert/SYS_DICT_ITEMS",
                    "data": {
                      "@set": {
                        "ID": "${ID | default:undefined}",
                        "DICT_CODE": "${DICT_CODE | default:undefined}",
                        "ITEM_CODE": "${ITEM_CODE | default:undefined}",
                        "ITEM_NAME": "${ITEM_NAME | default:undefined}",
                        "SORT": "${SORT | default:undefined}"
                      }
                    }
                  },
                  "onEvent": {
                    "submitSucc": {
                      "actions": [
                        {
                          "actionType": "reload",
                          "componentId": "detail_table"
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
                      "name": "DICT_CODE",
                      "label": "字典编码",
                      "maxLength": 64,
                      "disabled": true,
                      "id": "insert-DICT_CODE"
                    },
                    {
                      "type": "input-text",
                      "name": "ITEM_CODE",
                      "label": "字典项编码",
                      "maxLength": 64,
                      "disabled": false,
                      "id": "insert-ITEM_CODE"
                    },
                    {
                      "type": "input-text",
                      "name": "ITEM_NAME",
                      "label": "字典项名称",
                      "maxLength": 100,
                      "disabled": false,
                      "id": "insert-ITEM_NAME"
                    },
                    {
                      "type": "input-number",
                      "name": "SORT",
                      "label": "排序值",
                      "precision": 0,
                      "disabled": false,
                      "id": "insert-SORT"
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
                "url": "/sql/forge/api/json/select/SYS_DICT_ITEMS",
                "data": {
                  "@column": [
                    "ID",
                    "DICT_CODE",
                    "ITEM_CODE",
                    "ITEM_NAME",
                    "SORT"
                  ],
                  "@join": [],
                  "@where": [
                    {
                      "column": "DICT_CODE",
                      "condition": "EQ",
                      "value": "${DICT_CODE | default:\"\"}"
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
                "url": "/sql/forge/api/json/delete/SYS_DICT_ITEMS",
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
          "labelTpl": "${ITEM_CODE}",
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
              "name": "DICT_CODE",
              "label": "字典编码",
              "sortable": true
            },
            {
              "name": "ITEM_CODE",
              "label": "字典项编码",
              "sortable": true
            },
            {
              "name": "ITEM_NAME",
              "label": "字典项名称",
              "sortable": true
            },
            {
              "name": "SORT",
              "label": "排序值",
              "sortable": true,
              "align": "right"
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
                        "url": "/sql/forge/api/json/select/SYS_DICT_ITEMS",
                        "data": {
                          "@column": [
                            "ID",
                            "DICT_CODE",
                            "ITEM_CODE",
                            "ITEM_NAME",
                            "SORT"
                          ],
                          "@join": [],
                          "@where": [
                            {
                              "column": "ID",
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
                        "url": "/sql/forge/api/json/update/SYS_DICT_ITEMS",
                        "data": {
                          "@set": {
                            "ID": "${ID}",
                            "DICT_CODE": "${DICT_CODE}",
                            "ITEM_CODE": "${ITEM_CODE}",
                            "ITEM_NAME": "${ITEM_NAME}",
                            "SORT": "${SORT}"
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
                          "hidden": true,
                          "id": "update-ID"
                        },
                        {
                          "type": "input-text",
                          "name": "DICT_CODE",
                          "label": "字典编码",
                          "maxLength": 64,
                          "disabled": true,
                          "id": "update-DICT_CODE"
                        },
                        {
                          "type": "input-text",
                          "name": "ITEM_CODE",
                          "label": "字典项编码",
                          "maxLength": 64,
                          "disabled": false,
                          "id": "update-ITEM_CODE"
                        },
                        {
                          "type": "input-text",
                          "name": "ITEM_NAME",
                          "label": "字典项名称",
                          "maxLength": 100,
                          "disabled": false,
                          "id": "update-ITEM_NAME"
                        },
                        {
                          "type": "input-number",
                          "name": "SORT",
                          "label": "排序值",
                          "precision": 0,
                          "disabled": false,
                          "id": "update-SORT"
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
                    "url": "/sql/forge/api/json/delete/SYS_DICT_ITEMS",
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
    ]
  }
}');

-- 3. 商品管理
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-products', '商品管理', '商品增删改查', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/PRODUCTS",
      "data": {
        "@column": [
          "PRODUCTS.ID",
          "PRODUCTS.NAME",
          "categories.item_name as DICT_CATEGORIES",
          "PRODUCTS.PRICE"
        ],
        "@join": [
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items categories",
            "on": "PRODUCTS.DICT_CATEGORIES = categories.item_code"
          }
        ],
        "@where": [
          {
            "column": "PRODUCTS.NAME",
            "condition": "LIKE",
            "value": "${NAME | default:undefined}"
          },
          {
            "column": "categories.dict_code",
            "condition": "EQ",
            "value": "categories"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
              "url": "/sql/forge/api/json/insert/PRODUCTS",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "NAME": "${NAME | default:undefined}",
                  "DICT_CATEGORIES": "${DICT_CATEGORIES | default:undefined}",
                  "PRICE": "${PRICE | default:undefined}"
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
                "name": "NAME",
                "label": "商品名称",
                "maxLength": 100,
                "required": true,
                "disabled": false,
                "id": "insert-NAME"
              },
              {
                "type": "select",
                "name": "DICT_CATEGORIES",
                "label": "商品分类",
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
                        "value": "categories"
                      }
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                },
                "clearable": true,
                "disabled": false,
                "id": "insert-CATEGORIES"
              },
              {
                "type": "input-number",
                "name": "PRICE",
                "label": "商品价格",
                "precision": 2,
                "min": 0,
                "disabled": false,
                "id": "insert-PRICE"
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
          "url": "/sql/forge/api/json/select/PRODUCTS",
          "data": {
            "@column": [
              "PRODUCTS.ID",
              "PRODUCTS.NAME",
              "categories.item_name as DICT_CATEGORIES",
              "PRODUCTS.PRICE"
            ],
            "@join": [
              {
                "type": "LEFT_OUTER_JOIN",
                "joinTable": "sys_dict_items categories",
                "on": "PRODUCTS.DICT_CATEGORIES = categories.item_code"
              }
            ],
            "@where": [
              {
                "column": "PRODUCTS.NAME",
                "condition": "LIKE",
                "value": "${NAME | default:undefined}"
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
          "url": "/sql/forge/api/json/delete/PRODUCTS",
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
    "labelTpl": "${NAME}",
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
        "name": "NAME",
        "label": "商品名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "NAME",
          "label": "商品名称",
          "maxLength": 100,
          "placeholder": "输入商品名称"
        }
      },
      {
        "name": "DICT_CATEGORIES",
        "label": "商品分类",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "DICT_CATEGORIES",
          "label": "商品分类",
          "placeholder": "选择分类",
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
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "PRICE",
        "label": "商品价格",
        "sortable": true,
        "align": "right"
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
              "title": "编辑表单",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/PRODUCTS",
                  "data": {
                    "@column": [
                      "PRODUCTS.ID",
                      "PRODUCTS.NAME",
                      "PRODUCTS.DICT_CATEGORIES",
                      "PRODUCTS.PRICE"
                    ],
                    "@join": [],
                    "@where": [
                      {
                        "column": "PRODUCTS.ID",
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
                  "url": "/sql/forge/api/json/update/PRODUCTS",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "NAME": "${NAME}",
                      "DICT_CATEGORIES": "${DICT_CATEGORIES}",
                      "PRICE": "${PRICE}"
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
                    "hidden": true,
                    "id": "update-ID"
                  },
                  {
                    "type": "input-text",
                    "name": "NAME",
                    "label": "商品名称",
                    "maxLength": 100,
                    "required": true,
                    "disabled": false,
                    "id": "update-NAME"
                  },
                  {
                    "type": "select",
                    "name": "DICT_CATEGORIES",
                    "label": "商品分类",
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
                            "value": "categories"
                          }
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                    },
                    "clearable": true,
                    "disabled": false,
                    "id": "update-CATEGORIES"
                  },
                  {
                    "type": "input-number",
                    "name": "PRICE",
                    "label": "商品价格",
                    "precision": 2,
                    "min": 0,
                    "disabled": false,
                    "id": "update-PRICE"
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
              "url": "/sql/forge/api/json/delete/PRODUCTS",
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
}');

-- 4. 订单管理
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-orders', '订单管理', '订单增删改查', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/ORDERS",
      "data": {
        "@column": [
          "ORDERS.ID",
          "users.USERNAME as USER_NAME",
          "ORDERS.ORDER_DATE",
          "ORDERS.TOTAL_AMOUNT",
          "order_status.item_name as DICT_ORDER_STATUS"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "users users",
            "on": "ORDERS.USER_ID = users.ID"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items order_status",
            "on": "ORDERS.DICT_ORDER_STATUS = order_status.item_code"
          }
        ],
        "@where": [
          {
            "column": "users.USERNAME",
            "condition": "LIKE",
            "value": "${USER_NAME | default:undefined}"
          },
          {
            "column": "ORDERS.DICT_ORDER_STATUS",
            "condition": "IN",
            "value": "${DICT_ORDER_STATUS | default:undefined | split}"
          },
          {
            "column": "order_status.dict_code",
            "condition": "EQ",
            "value": "order_status"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
              "url": "/sql/forge/api/json/insert/ORDERS",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "USER_ID": "${USER_ID | default:undefined}",
                  "TOTAL_AMOUNT": "${TOTAL_AMOUNT | default:undefined}",
                  "DICT_ORDER_STATUS": "${DICT_ORDER_STATUS | default:undefined}"
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
                "type": "select",
                "name": "USER_ID",
                "label": "下单用户",
                "required": true,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/users",
                  "data": {
                    "@column": [
                      "ID",
                      "USERNAME"
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.username || item.USERNAME\n  }))\n};"
                },
                "clearable": false,
                "disabled": false,
                "id": "insert-USER_ID"
              },
              {
                "type": "select",
                "name": "DICT_ORDER_STATUS",
                "label": "订单状态",
                "maxLength": 20,
                "value": "pending",
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
                        "value": "order_status"
                      }
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                },
                "clearable": false,
                "disabled": false,
                "id": "insert-ORDER_STATUS"
              },
              {
                "type": "input-number",
                "name": "TOTAL_AMOUNT",
                "label": "订单总金额",
                "precision": 2,
                "min": 0,
                "required": true,
                "disabled": false,
                "id": "insert-TOTAL_AMOUNT"
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
          "url": "/sql/forge/api/json/select/ORDERS",
          "data": {
            "@column": [
              "ORDERS.ID",
              "users.USERNAME as USER_NAME",
              "ORDERS.ORDER_DATE",
              "ORDERS.TOTAL_AMOUNT",
              "order_status.item_name as DICT_ORDER_STATUS"
            ],
            "@join": [
              {
                "type": "INNER_JOIN",
                "joinTable": "users users",
                "on": "ORDERS.USER_ID = users.ID"
              },
              {
                "type": "LEFT_OUTER_JOIN",
                "joinTable": "sys_dict_items order_status",
                "on": "ORDERS.DICT_ORDER_STATUS = order_status.item_code"
              }
            ],
            "@where": [
              {
                "column": "users.USERNAME",
                "condition": "LIKE",
                "value": "${USER_NAME | default:undefined}"
              },
              {
                "column": "ORDERS.DICT_ORDER_STATUS",
                "condition": "IN",
                "value": "${DICT_ORDER_STATUS | default:undefined | split}"
              },
              {
                "column": "order_status.dict_code",
                "condition": "EQ",
                "value": "order_status"
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
          "url": "/sql/forge/api/json/delete/ORDERS",
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
    "labelTpl": "${USER_NAME}",
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
        "name": "USER_NAME",
        "label": "下单用户",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USER_NAME",
          "label": "下单用户",
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "ORDER_DATE",
        "label": "订单日期",
        "sortable": true
      },
      {
        "name": "TOTAL_AMOUNT",
        "label": "总金额",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "DICT_ORDER_STATUS",
        "label": "订单状态",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "DICT_ORDER_STATUS",
          "label": "订单状态",
          "placeholder": "选择状态",
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
                  "value": "order_status"
                }
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "type": "operation",
        "label": "操作",
        "buttons": [
          {
            "label": "查看明细",
            "type": "button",
            "icon": "fa fa-list",
            "actionType": "dialog",
            "dialog": {
              "title": "订单明细",
              "size": "xl",
              "body": {
                "type": "page",
                "body": {
                  "type": "crud",
                  "id": "crud_order_items",
                  "api": {
                    "method": "post",
                    "url": "/sql/forge/api/json/selectPage/ORDER_ITEMS",
                    "data": {
                      "@column": [
                        "ORDER_ITEMS.ID",
                        "ORDERS.ID as ORDER_ID",
                        "users.USERNAME as USER_NAME",
                        "products.NAME as PRODUCT_NAME",
                        "ORDER_ITEMS.QUANTITY",
                        "ORDER_ITEMS.UNIT_PRICE",
                        "ORDER_ITEMS.QUANTITY * ORDER_ITEMS.UNIT_PRICE as SUBTOTAL"
                      ],
                      "@join": [
                        {
                          "type": "INNER_JOIN",
                          "joinTable": "orders ORDERS",
                          "on": "ORDER_ITEMS.ORDER_ID = ORDERS.ID"
                        },
                        {
                          "type": "INNER_JOIN",
                          "joinTable": "users users",
                          "on": "ORDERS.USER_ID = users.ID"
                        },
                        {
                          "type": "INNER_JOIN",
                          "joinTable": "products products",
                          "on": "ORDER_ITEMS.PRODUCT_ID = products.ID"
                        }
                      ],
                      "@where": [
                        {
                          "column": "ORDER_ITEMS.ORDER_ID",
                          "condition": "EQ",
                          "value": "${ID}"
                        }
                      ]
                    }
                  },
                  "columns": [
                    {
                      "name": "ORDER_ID",
                      "label": "订单号"
                    },
                    {
                      "name": "USER_NAME",
                      "label": "用户名"
                    },
                    {
                      "name": "PRODUCT_NAME",
                      "label": "商品名称"
                    },
                    {
                      "name": "QUANTITY",
                      "label": "数量",
                      "align": "right"
                    },
                    {
                      "name": "UNIT_PRICE",
                      "label": "单价",
                      "align": "right"
                    },
                    {
                      "name": "SUBTOTAL",
                      "label": "小计",
                      "align": "right"
                    }
                  ]
                }
              }
            }
          },
          {
            "label": "修改",
            "type": "button",
            "icon": "fa fa-pen-to-square",
            "actionType": "drawer",
            "drawer": {
              "title": "编辑表单",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/ORDERS",
                  "data": {
                    "@column": [
                      "ORDERS.ID",
                      "ORDERS.USER_ID",
                      "ORDERS.ORDER_DATE",
                      "ORDERS.TOTAL_AMOUNT",
                      "ORDERS.DICT_ORDER_STATUS"
                    ],
                    "@join": [
                      {
                        "type": "LEFT_OUTER_JOIN",
                        "joinTable": "sys_dict_items order_status_b71e44a6",
                        "on": "ORDERS.DICT_ORDER_STATUS = order_status_b71e44a6.item_code"
                      }
                    ],
                    "@where": [
                      {
                        "column": "ORDERS.ID",
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
                  "url": "/sql/forge/api/json/update/ORDERS",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "USER_ID": "${USER_ID}",
                      "TOTAL_AMOUNT": "${TOTAL_AMOUNT}",
                      "DICT_ORDER_STATUS": "${DICT_ORDER_STATUS}"
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
                    "hidden": true,
                    "id": "update-ID"
                  },
                  {
                    "type": "select",
                    "name": "USER_ID",
                    "label": "下单用户",
                    "required": true,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/users",
                      "data": {
                        "@column": [
                          "ID",
                          "USERNAME"
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.username || item.USERNAME\n  }))\n};"
                    },
                    "clearable": false,
                    "disabled": false,
                    "id": "update-USER_ID"
                  },
                  {
                    "type": "select",
                    "name": "DICT_ORDER_STATUS",
                    "label": "订单状态",
                    "maxLength": 20,
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
                            "value": "order_status"
                          }
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                    },
                    "clearable": false,
                    "disabled": false,
                    "id": "update-ORDER_STATUS"
                  },
                  {
                    "type": "input-number",
                    "name": "TOTAL_AMOUNT",
                    "label": "订单总金额",
                    "precision": 2,
                    "min": 0,
                    "required": true,
                    "disabled": false,
                    "id": "update-TOTAL_AMOUNT"
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
              "url": "/sql/forge/api/json/delete/ORDERS",
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
}');

-- 5. 订单明细
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-order-items', '订单明细', '订单明细管理', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/ORDER_ITEMS",
      "data": {
        "@column": [
          "ORDER_ITEMS.ID",
          "ORDERS.ID as ORDER_ID",
          "users.USERNAME as USER_NAME",
          "products.NAME as PRODUCT_NAME",
          "ORDER_ITEMS.QUANTITY",
          "ORDER_ITEMS.UNIT_PRICE",
          "ORDER_ITEMS.QUANTITY * ORDER_ITEMS.UNIT_PRICE as SUBTOTAL"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "orders ORDERS",
            "on": "ORDER_ITEMS.ORDER_ID = ORDERS.ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "users users",
            "on": "ORDERS.USER_ID = users.ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "products products",
            "on": "ORDER_ITEMS.PRODUCT_ID = products.ID"
          }
        ],
        "@where": [
          {
            "column": "ORDER_ITEMS.ORDER_ID",
            "condition": "LIKE",
            "value": "${order_id | default:undefined}"
          },
          {
            "column": "users.USERNAME",
            "condition": "LIKE",
            "value": "${USER_NAME | default:undefined}"
          },
          {
            "column": "products.NAME",
            "condition": "LIKE",
            "value": "${PRODUCT_NAME | default:undefined}"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
              "url": "/sql/forge/api/json/insert/ORDER_ITEMS",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "ORDER_ID": "${ORDER_ID | default:undefined}",
                  "PRODUCT_ID": "${PRODUCT_ID | default:undefined}",
                  "QUANTITY": "${QUANTITY | default:undefined}",
                  "UNIT_PRICE": "${UNIT_PRICE | default:undefined}"
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
                "type": "select",
                "name": "ORDER_ID",
                "label": "所属订单",
                "required": true,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/orders",
                  "data": {
                    "@column": [
                      "ID",
                      "USER_ID",
                      "TOTAL_AMOUNT",
                      "ORDER_DATE"
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.id\n  }))\n};"
                },
                "clearable": false,
                "disabled": false,
                "id": "insert-ORDER_ID"
              },
              {
                "type": "select",
                "name": "PRODUCT_ID",
                "label": "商品",
                "required": true,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/products",
                  "data": {
                    "@column": [
                      "ID",
                      "NAME",
                      "PRICE"
                    ]
                  },
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.name || item.NAME\n  }))\n};"
                },
                "clearable": false,
                "disabled": false,
                "id": "insert-PRODUCT_ID"
              },
              {
                "type": "input-number",
                "name": "QUANTITY",
                "label": "数量",
                "precision": 0,
                "min": 1,
                "required": true,
                "value": 1,
                "disabled": false,
                "id": "insert-QUANTITY"
              },
              {
                "type": "input-number",
                "name": "UNIT_PRICE",
                "label": "单价",
                "precision": 2,
                "min": 0,
                "required": true,
                "disabled": false,
                "id": "insert-UNIT_PRICE"
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
          "url": "/sql/forge/api/json/select/ORDER_ITEMS",
          "data": {
            "@column": [
              "ORDER_ITEMS.ID",
              "ORDERS.ID as ORDER_ID",
              "users.USERNAME as USER_NAME",
              "products.NAME as PRODUCT_NAME",
              "ORDER_ITEMS.QUANTITY",
              "ORDER_ITEMS.UNIT_PRICE",
              "ORDER_ITEMS.QUANTITY * ORDER_ITEMS.UNIT_PRICE as SUBTOTAL"
            ],
            "@join": [
              {
                "type": "INNER_JOIN",
                "joinTable": "orders ORDERS",
                "on": "ORDER_ITEMS.ORDER_ID = ORDERS.ID"
              },
              {
                "type": "INNER_JOIN",
                "joinTable": "users users",
                "on": "ORDERS.USER_ID = users.ID"
              },
              {
                "type": "INNER_JOIN",
                "joinTable": "products products",
                "on": "ORDER_ITEMS.PRODUCT_ID = products.ID"
              }
            ],
            "@where": [
              {
                "column": "ORDER_ITEMS.ORDER_ID",
                "condition": "EQ",
                "value": "${order_id | default:undefined}"
              },
              {
                "column": "users.USERNAME",
                "condition": "LIKE",
                "value": "${USER_NAME | default:undefined}"
              },
              {
                "column": "products.NAME",
                "condition": "LIKE",
                "value": "${PRODUCT_NAME | default:undefined}"
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
          "url": "/sql/forge/api/json/delete/ORDER_ITEMS",
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
    "labelTpl": "${PRODUCT_NAME}",
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
        "name": "ORDER_ID",
        "label": "订单号",
        "sortable": true
      },
      {
        "name": "USER_NAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USER_NAME",
          "label": "用户名",
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "PRODUCT_NAME",
        "label": "商品名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "PRODUCT_NAME",
          "label": "商品名称",
          "placeholder": "输入商品名称"
        }
      },
      {
        "name": "QUANTITY",
        "label": "数量",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "UNIT_PRICE",
        "label": "单价",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "SUBTOTAL",
        "label": "小计",
        "sortable": false,
        "align": "right"
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
              "title": "编辑表单",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/ORDER_ITEMS",
                  "data": {
                    "@column": [
                      "ORDER_ITEMS.ID",
                      "ORDER_ITEMS.ORDER_ID",
                      "ORDER_ITEMS.PRODUCT_ID",
                      "ORDER_ITEMS.QUANTITY",
                      "ORDER_ITEMS.UNIT_PRICE"
                    ],
                    "@join": [],
                    "@where": [
                      {
                        "column": "ORDER_ITEMS.ID",
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
                  "url": "/sql/forge/api/json/update/ORDER_ITEMS",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "ORDER_ID": "${ORDER_ID}",
                      "PRODUCT_ID": "${PRODUCT_ID}",
                      "QUANTITY": "${QUANTITY}",
                      "UNIT_PRICE": "${UNIT_PRICE}"
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
                    "hidden": true,
                    "id": "update-ID"
                  },
                  {
                    "type": "select",
                    "name": "ORDER_ID",
                    "label": "所属订单",
                    "required": true,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/orders",
                      "data": {
                        "@column": [
                          "ID",
                          "USER_ID",
                          "TOTAL_AMOUNT",
                          "ORDER_DATE"
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.id\n  }))\n};"
                    },
                    "clearable": false,
                    "disabled": false,
                    "id": "update-ORDER_ID"
                  },
                  {
                    "type": "select",
                    "name": "PRODUCT_ID",
                    "label": "商品",
                    "required": true,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/products",
                      "data": {
                        "@column": [
                          "ID",
                          "NAME",
                          "PRICE"
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.name || item.NAME\n  }))\n};"
                    },
                    "clearable": false,
                    "disabled": false,
                    "id": "update-PRODUCT_ID"
                  },
                  {
                    "type": "input-number",
                    "name": "QUANTITY",
                    "label": "数量",
                    "precision": 0,
                    "min": 1,
                    "required": true,
                    "disabled": false,
                    "id": "update-QUANTITY"
                  },
                  {
                    "type": "input-number",
                    "name": "UNIT_PRICE",
                    "label": "单价",
                    "precision": 2,
                    "min": 0,
                    "required": true,
                    "disabled": false,
                    "id": "update-UNIT_PRICE"
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
              "url": "/sql/forge/api/json/delete/ORDER_ITEMS",
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
}');

-- 6. 订单总览
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-order-overview', '订单总览', '订单综合视图', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/ORDERS",
      "data": {
        "@column": [
          "ORDERS.ID as ORDER_ID",
          "users.USERNAME as USER_NAME",
          "products.NAME as PRODUCT_NAME",
          "categories.item_name as PRODUCT_CATEGORY",
          "order_items.QUANTITY",
          "order_items.UNIT_PRICE",
          "order_items.QUANTITY * order_items.UNIT_PRICE as SUBTOTAL",
          "order_status.item_name as ORDER_STATUS",
          "ORDERS.ORDER_DATE"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "users users",
            "on": "ORDERS.USER_ID = users.ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "order_items order_items",
            "on": "ORDERS.ID = order_items.ORDER_ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "products products",
            "on": "order_items.PRODUCT_ID = products.ID"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items categories",
            "on": "products.DICT_CATEGORIES = categories.item_code"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items order_status",
            "on": "ORDERS.DICT_ORDER_STATUS = order_status.item_code"
          }
        ],
        "@where": [
          {
            "column": "users.USERNAME",
            "condition": "LIKE",
            "value": "${USER_NAME | default:undefined}"
          },
          {
            "column": "products.NAME",
            "condition": "LIKE",
            "value": "${PRODUCT_NAME | default:undefined}"
          },
          {
            "column": "ORDERS.DICT_ORDER_STATUS",
            "condition": "IN",
            "value": "${ORDER_STATUS | default:undefined | split}"
          },
          {
            "column": "categories.dict_code",
            "condition": "EQ",
            "value": "categories"
          },
          {
            "column": "order_status.dict_code",
            "condition": "EQ",
            "value": "order_status"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "footerToolbar": [
      "statistics",
      {
        "type": "pagination",
        "layout": "total,perPage,pager,go"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "labelTpl": "${ORDER_ID}",
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ORDER_ID",
    "columns": [
      {
        "name": "ORDER_ID",
        "label": "订单号",
        "sortable": true
      },
      {
        "name": "USER_NAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USER_NAME",
          "label": "用户名",
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "PRODUCT_NAME",
        "label": "商品名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "PRODUCT_NAME",
          "label": "商品名称",
          "placeholder": "输入商品名称"
        }
      },
      {
        "name": "PRODUCT_CATEGORY",
        "label": "商品分类",
        "sortable": true
      },
      {
        "name": "QUANTITY",
        "label": "数量",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "UNIT_PRICE",
        "label": "单价",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "SUBTOTAL",
        "label": "小计",
        "sortable": false,
        "align": "right"
      },
      {
        "name": "ORDER_STATUS",
        "label": "订单状态",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "ORDER_STATUS",
          "label": "订单状态",
          "placeholder": "选择状态",
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
                  "value": "order_status"
                }
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "ORDER_DATE",
        "label": "订单日期",
        "sortable": true
      }
    ]
  }
}');

-- 7. 支付记录
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-payments', '支付记录', '支付记录查询', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/PAYMENTS",
      "data": {
        "@column": [
          "PAYMENTS.ID",
          "ORDERS.ID as ORDER_ID",
          "pay_method.item_name as PAY_METHOD",
          "pay_status.item_name as PAY_STATUS",
          "PAYMENTS.AMOUNT",
          "PAYMENTS.PAY_TIME",
          "PAYMENTS.TRANSACTION_NO"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "orders ORDERS",
            "on": "PAYMENTS.ORDER_ID = ORDERS.ID"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items pay_method",
            "on": "PAYMENTS.PAY_METHOD = pay_method.item_code"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items pay_status",
            "on": "PAYMENTS.PAY_STATUS = pay_status.item_code"
          }
        ],
        "@where": [
          {
            "column": "PAYMENTS.PAY_STATUS",
            "condition": "IN",
            "value": "${PAY_STATUS | default:undefined | split}"
          },
          {
            "column": "PAYMENTS.TRANSACTION_NO",
            "condition": "LIKE",
            "value": "${TRANSACTION_NO | default:undefined}"
          },
          {
            "column": "pay_method.dict_code",
            "condition": "EQ",
            "value": "pay_method"
          },
          {
            "column": "pay_status.dict_code",
            "condition": "EQ",
            "value": "pay_status"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "headerToolbar": [
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
          "url": "/sql/forge/api/json/select/PAYMENTS",
          "data": {
            "@column": [
              "PAYMENTS.ID",
              "ORDERS.ID as ORDER_ID",
              "pay_method.item_name as PAY_METHOD",
              "pay_status.item_name as PAY_STATUS",
              "PAYMENTS.AMOUNT",
              "PAYMENTS.PAY_TIME",
              "PAYMENTS.TRANSACTION_NO"
            ],
            "@join": [
              {
                "type": "INNER_JOIN",
                "joinTable": "orders ORDERS",
                "on": "PAYMENTS.ORDER_ID = ORDERS.ID"
              },
              {
                "type": "LEFT_OUTER_JOIN",
                "joinTable": "sys_dict_items pay_method",
                "on": "PAYMENTS.PAY_METHOD = pay_method.item_code"
              },
              {
                "type": "LEFT_OUTER_JOIN",
                "joinTable": "sys_dict_items pay_status",
                "on": "PAYMENTS.PAY_STATUS = pay_status.item_code"
              }
            ],
            "@where": [
              {
                "column": "PAYMENTS.PAY_STATUS",
                "condition": "IN",
                "value": "${PAY_STATUS | default:undefined | split}"
              },
              {
                "column": "PAYMENTS.TRANSACTION_NO",
                "condition": "LIKE",
                "value": "${TRANSACTION_NO | default:undefined}"
              },
              {
                "column": "pay_method.dict_code",
                "condition": "EQ",
                "value": "pay_method"
              },
              {
                "column": "pay_status.dict_code",
                "condition": "EQ",
                "value": "pay_status"
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
          "url": "/sql/forge/api/json/delete/PAYMENTS",
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
        "name": "ORDER_ID",
        "label": "订单号",
        "sortable": true
      },
      {
        "name": "PAY_METHOD",
        "label": "支付方式",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "PAY_METHOD",
          "label": "支付方式",
          "placeholder": "选择支付方式",
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": ["item_code", "item_name"],
              "@where": [
                {"column": "dict_code", "condition": "EQ", "value": "pay_method"}
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "PAY_STATUS",
        "label": "支付状态",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "PAY_STATUS",
          "label": "支付状态",
          "placeholder": "选择状态",
          "multiple": true,
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": ["item_code", "item_name"],
              "@where": [
                {"column": "dict_code", "condition": "EQ", "value": "pay_status"}
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "AMOUNT",
        "label": "支付金额",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "PAY_TIME",
        "label": "支付时间",
        "sortable": true,
        "type": "datetime"
      },
      {
        "name": "TRANSACTION_NO",
        "label": "交易流水号",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "TRANSACTION_NO",
          "label": "交易流水号",
          "placeholder": "输入流水号"
        }
      },
      {
        "type": "operation",
        "label": "操作",
        "buttons": [
          {
            "label": "删除",
            "type": "button",
            "icon": "fa fa-minus",
            "actionType": "ajax",
            "level": "danger",
            "confirmText": "确认要删除？",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/delete/PAYMENTS",
              "data": {
                "@where": [
                  {"column": "ID", "condition": "EQ", "value": "${ID}"}
                ]
              }
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}');

-- 8. 地址管理
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-user-addresses', '地址管理', '用户收货地址管理', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/USER_ADDRESSES",
      "data": {
        "@column": [
          "USER_ADDRESSES.ID",
          "users.USERNAME as USER_NAME",
          "USER_ADDRESSES.RECEIVER_NAME",
          "USER_ADDRESSES.PHONE",
          "USER_ADDRESSES.PROVINCE",
          "USER_ADDRESSES.CITY",
          "USER_ADDRESSES.DISTRICT",
          "USER_ADDRESSES.DETAIL_ADDRESS",
          "USER_ADDRESSES.IS_DEFAULT"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "users users",
            "on": "USER_ADDRESSES.USER_ID = users.ID"
          }
        ],
        "@where": [
          {
            "column": "users.USERNAME",
            "condition": "LIKE",
            "value": "${USER_NAME | default:undefined}"
          },
          {
            "column": "USER_ADDRESSES.RECEIVER_NAME",
            "condition": "LIKE",
            "value": "${RECEIVER_NAME | default:undefined}"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
          "title": "新增地址",
          "body": {
            "type": "form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/USER_ADDRESSES",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "USER_ID": "${USER_ID | default:undefined}",
                  "RECEIVER_NAME": "${RECEIVER_NAME | default:undefined}",
                  "PHONE": "${PHONE | default:undefined}",
                  "PROVINCE": "${PROVINCE | default:undefined}",
                  "CITY": "${CITY | default:undefined}",
                  "DISTRICT": "${DISTRICT | default:undefined}",
                  "DETAIL_ADDRESS": "${DETAIL_ADDRESS | default:undefined}",
                  "IS_DEFAULT": "${IS_DEFAULT | default:false}"
                }
              }
            },
            "onEvent": {
              "submitSucc": {
                "actions": [
                  {"actionType": "reload", "componentId": "crud_table"}
                ]
              }
            },
            "body": [
              {"type": "uuid", "id": "insert-ID", "name": "ID"},
              {
                "type": "select",
                "name": "USER_ID",
                "label": "所属用户",
                "required": true,
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/users",
                  "data": {"@column": ["ID", "USERNAME"]},
                  "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.username || item.USERNAME\n  }))\n};"
                },
                "clearable": false,
                "disabled": false,
                "id": "insert-USER_ID"
              },
              {
                "type": "input-text",
                "name": "RECEIVER_NAME",
                "label": "收货人",
                "maxLength": 100,
                "required": true,
                "disabled": false,
                "id": "insert-RECEIVER_NAME"
              },
              {
                "type": "input-text",
                "name": "PHONE",
                "label": "联系电话",
                "maxLength": 20,
                "required": true,
                "disabled": false,
                "id": "insert-PHONE"
              },
              {
                "type": "input-text",
                "name": "PROVINCE",
                "label": "省份",
                "maxLength": 50,
                "required": true,
                "disabled": false,
                "id": "insert-PROVINCE"
              },
              {
                "type": "input-text",
                "name": "CITY",
                "label": "城市",
                "maxLength": 50,
                "required": true,
                "disabled": false,
                "id": "insert-CITY"
              },
              {
                "type": "input-text",
                "name": "DISTRICT",
                "label": "区县",
                "maxLength": 50,
                "required": true,
                "disabled": false,
                "id": "insert-DISTRICT"
              },
              {
                "type": "input-text",
                "name": "DETAIL_ADDRESS",
                "label": "详细地址",
                "maxLength": 200,
                "required": true,
                "disabled": false,
                "id": "insert-DETAIL_ADDRESS"
              },
              {
                "type": "switch",
                "name": "IS_DEFAULT",
                "label": "默认地址",
                "disabled": false,
                "id": "insert-IS_DEFAULT"
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
          "url": "/sql/forge/api/json/delete/USER_ADDRESSES",
          "data": {
            "@where": [
              {"column": "ID", "condition": "IN", "value": "${ids | split}"}
            ]
          }
        },
        "confirmText": "确定要批量删除?"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ID",
    "columns": [
      {"name": "ID", "hidden": true},
      {
        "name": "USER_NAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USER_NAME",
          "label": "用户名",
          "placeholder": "输入用户名"
        }
      },
      {"name": "RECEIVER_NAME", "label": "收货人", "sortable": true},
      {"name": "PHONE", "label": "联系电话"},
      {"name": "PROVINCE", "label": "省份"},
      {"name": "CITY", "label": "城市"},
      {"name": "DISTRICT", "label": "区县"},
      {"name": "DETAIL_ADDRESS", "label": "详细地址"},
      {
        "name": "IS_DEFAULT",
        "label": "默认",
        "type": "switch",
        "disabled": true
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
              "title": "编辑地址",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/USER_ADDRESSES",
                  "data": {
                    "@column": [
                      "USER_ADDRESSES.ID",
                      "USER_ADDRESSES.USER_ID",
                      "USER_ADDRESSES.RECEIVER_NAME",
                      "USER_ADDRESSES.PHONE",
                      "USER_ADDRESSES.PROVINCE",
                      "USER_ADDRESSES.CITY",
                      "USER_ADDRESSES.DISTRICT",
                      "USER_ADDRESSES.DETAIL_ADDRESS",
                      "USER_ADDRESSES.IS_DEFAULT"
                    ],
                    "@join": [],
                    "@where": [
                      {"column": "USER_ADDRESSES.ID", "condition": "EQ", "value": "${ID}"}
                    ]
                  },
                  "responseData": {"&": "${items | first}"}
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/USER_ADDRESSES",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "USER_ID": "${USER_ID}",
                      "RECEIVER_NAME": "${RECEIVER_NAME}",
                      "PHONE": "${PHONE}",
                      "PROVINCE": "${PROVINCE}",
                      "CITY": "${CITY}",
                      "DISTRICT": "${DISTRICT}",
                      "DETAIL_ADDRESS": "${DETAIL_ADDRESS}",
                      "IS_DEFAULT": "${IS_DEFAULT}"
                    },
                    "@where": [
                      {"column": "ID", "condition": "EQ", "value": "${ID}"}
                    ]
                  }
                },
                "body": [
                  {"type": "input-text", "name": "ID", "hidden": true, "id": "update-ID"},
                  {
                    "type": "select",
                    "name": "USER_ID",
                    "label": "所属用户",
                    "required": true,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/users",
                      "data": {"@column": ["ID", "USERNAME"]},
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.username || item.USERNAME\n  }))\n};"
                    },
                    "clearable": false,
                    "disabled": false,
                    "id": "update-USER_ID"
                  },
                  {
                    "type": "input-text",
                    "name": "RECEIVER_NAME",
                    "label": "收货人",
                    "maxLength": 100,
                    "required": true,
                    "disabled": false,
                    "id": "update-RECEIVER_NAME"
                  },
                  {
                    "type": "input-text",
                    "name": "PHONE",
                    "label": "联系电话",
                    "maxLength": 20,
                    "required": true,
                    "disabled": false,
                    "id": "update-PHONE"
                  },
                  {
                    "type": "input-text",
                    "name": "PROVINCE",
                    "label": "省份",
                    "maxLength": 50,
                    "required": true,
                    "disabled": false,
                    "id": "update-PROVINCE"
                  },
                  {
                    "type": "input-text",
                    "name": "CITY",
                    "label": "城市",
                    "maxLength": 50,
                    "required": true,
                    "disabled": false,
                    "id": "update-CITY"
                  },
                  {
                    "type": "input-text",
                    "name": "DISTRICT",
                    "label": "区县",
                    "maxLength": 50,
                    "required": true,
                    "disabled": false,
                    "id": "update-DISTRICT"
                  },
                  {
                    "type": "input-text",
                    "name": "DETAIL_ADDRESS",
                    "label": "详细地址",
                    "maxLength": 200,
                    "required": true,
                    "disabled": false,
                    "id": "update-DETAIL_ADDRESS"
                  },
                  {
                    "type": "switch",
                    "name": "IS_DEFAULT",
                    "label": "默认地址",
                    "disabled": false,
                    "id": "update-IS_DEFAULT"
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
              "url": "/sql/forge/api/json/delete/USER_ADDRESSES",
              "data": {
                "@where": [
                  {"column": "ID", "condition": "EQ", "value": "${ID}"}
                ]
              }
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}');

-- 9. 库存管理
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-inventory', '库存管理', '商品库存查询', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/INVENTORY",
      "data": {
        "@column": [
          "INVENTORY.PRODUCT_ID",
          "products.NAME as PRODUCT_NAME",
          "INVENTORY.STOCK",
          "INVENTORY.MIN_STOCK",
          "INVENTORY.UPDATED_AT",
          "CASE WHEN INVENTORY.STOCK <= INVENTORY.MIN_STOCK THEN ''预警'' ELSE ''正常'' END AS STOCK_STATUS"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "products products",
            "on": "INVENTORY.PRODUCT_ID = products.ID"
          }
        ],
        "@where": [
          {
            "column": "products.NAME",
            "condition": "LIKE",
            "value": "${PRODUCT_NAME | default:undefined}"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "headerToolbar": [
      "bulkActions",
      {
        "type": "columns-toggler",
        "draggable": true,
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
    "keepItemSelectionOnPageChange": true,
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "PRODUCT_ID",
    "columns": [
      {"name": "PRODUCT_ID", "hidden": true},
      {
        "name": "PRODUCT_NAME",
        "label": "商品名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "PRODUCT_NAME",
          "label": "商品名称",
          "placeholder": "输入商品名称"
        }
      },
      {
        "name": "STOCK",
        "label": "当前库存",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "MIN_STOCK",
        "label": "最低库存",
        "sortable": true,
        "align": "right"
      },
      {
        "name": "STOCK_STATUS",
        "label": "库存状态",
        "type": "mapping",
        "map": {
          "预警": "danger",
          "正常": "success"
        }
      },
      {
        "name": "UPDATED_AT",
        "label": "更新时间",
        "sortable": true,
        "type": "datetime"
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
              "title": "编辑库存",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/INVENTORY",
                  "data": {
                    "@column": [
                      "INVENTORY.PRODUCT_ID",
                      "INVENTORY.STOCK",
                      "INVENTORY.MIN_STOCK"
                    ],
                    "@join": [],
                    "@where": [
                      {"column": "INVENTORY.PRODUCT_ID", "condition": "EQ", "value": "${PRODUCT_ID}"}
                    ]
                  },
                  "responseData": {"&": "${items | first}"}
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/INVENTORY",
                  "data": {
                    "@set": {
                      "PRODUCT_ID": "${PRODUCT_ID}",
                      "STOCK": "${STOCK}",
                      "MIN_STOCK": "${MIN_STOCK}"
                    },
                    "@where": [
                      {"column": "PRODUCT_ID", "condition": "EQ", "value": "${PRODUCT_ID}"}
                    ]
                  }
                },
                "body": [
                  {
                    "type": "input-text",
                    "name": "PRODUCT_ID",
                    "label": "商品ID",
                    "disabled": true,
                    "id": "update-PRODUCT_ID"
                  },
                  {
                    "type": "input-number",
                    "name": "STOCK",
                    "label": "当前库存",
                    "precision": 0,
                    "min": 0,
                    "required": true,
                    "disabled": false,
                    "id": "update-STOCK"
                  },
                  {
                    "type": "input-number",
                    "name": "MIN_STOCK",
                    "label": "最低库存",
                    "precision": 0,
                    "min": 0,
                    "required": true,
                    "disabled": false,
                    "id": "update-MIN_STOCK"
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
}');

-- 10. 物流追踪
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-order-logistics', '物流追踪', '订单物流信息', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/ORDER_LOGISTICS",
      "data": {
        "@column": [
          "ORDER_LOGISTICS.ID",
          "ORDERS.ID as ORDER_ID",
          "users.USERNAME as USER_NAME",
          "ORDER_LOGISTICS.TRACKING_NO",
          "ORDER_LOGISTICS.CARRIER",
          "logistics_status.item_name as LOGISTICS_STATUS",
          "ORDER_LOGISTICS.SHIP_TIME",
          "ORDER_LOGISTICS.RECEIVE_TIME"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "orders ORDERS",
            "on": "ORDER_LOGISTICS.ORDER_ID = ORDERS.ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "users users",
            "on": "ORDERS.USER_ID = users.ID"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items logistics_status",
            "on": "ORDER_LOGISTICS.LOGISTICS_STATUS = logistics_status.item_code"
          }
        ],
        "@where": [
          {
            "column": "ORDER_LOGISTICS.TRACKING_NO",
            "condition": "LIKE",
            "value": "${TRACKING_NO | default:undefined}"
          },
          {
            "column": "ORDER_LOGISTICS.LOGISTICS_STATUS",
            "condition": "IN",
            "value": "${LOGISTICS_STATUS | default:undefined | split}"
          },
          {
            "column": "logistics_status.dict_code",
            "condition": "EQ",
            "value": "logistics_status"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "headerToolbar": [
      "bulkActions",
      {
        "type": "columns-toggler",
        "draggable": true,
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
          "url": "/sql/forge/api/json/delete/ORDER_LOGISTICS",
          "data": {
            "@where": [
              {"column": "ID", "condition": "IN", "value": "${ids | split}"}
            ]
          }
        },
        "confirmText": "确定要批量删除?"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ID",
    "columns": [
      {"name": "ID", "hidden": true},
      {
        "name": "ORDER_ID",
        "label": "订单号",
        "sortable": true
      },
      {
        "name": "USER_NAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USER_NAME",
          "label": "用户名",
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "TRACKING_NO",
        "label": "物流单号",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "TRACKING_NO",
          "label": "物流单号",
          "placeholder": "输入单号"
        }
      },
      {
        "name": "CARRIER",
        "label": "物流公司",
        "sortable": true
      },
      {
        "name": "LOGISTICS_STATUS",
        "label": "物流状态",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "LOGISTICS_STATUS",
          "label": "物流状态",
          "placeholder": "选择状态",
          "multiple": true,
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": ["item_code", "item_name"],
              "@where": [
                {"column": "dict_code", "condition": "EQ", "value": "logistics_status"}
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "SHIP_TIME",
        "label": "发货时间",
        "sortable": true,
        "type": "datetime"
      },
      {
        "name": "RECEIVE_TIME",
        "label": "签收时间",
        "sortable": true,
        "type": "datetime"
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
              "title": "更新物流",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/ORDER_LOGISTICS",
                  "data": {
                    "@column": [
                      "ORDER_LOGISTICS.ID",
                      "ORDER_LOGISTICS.ORDER_ID",
                      "ORDER_LOGISTICS.TRACKING_NO",
                      "ORDER_LOGISTICS.CARRIER",
                      "ORDER_LOGISTICS.LOGISTICS_STATUS",
                      "ORDER_LOGISTICS.SHIP_TIME",
                      "ORDER_LOGISTICS.RECEIVE_TIME"
                    ],
                    "@join": [],
                    "@where": [
                      {"column": "ORDER_LOGISTICS.ID", "condition": "EQ", "value": "${ID}"}
                    ]
                  },
                  "responseData": {"&": "${items | first}"}
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/ORDER_LOGISTICS",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "ORDER_ID": "${ORDER_ID}",
                      "TRACKING_NO": "${TRACKING_NO}",
                      "CARRIER": "${CARRIER}",
                      "LOGISTICS_STATUS": "${LOGISTICS_STATUS}",
                      "SHIP_TIME": "${SHIP_TIME}",
                      "RECEIVE_TIME": "${RECEIVE_TIME}"
                    },
                    "@where": [
                      {"column": "ID", "condition": "EQ", "value": "${ID}"}
                    ]
                  }
                },
                "body": [
                  {"type": "input-text", "name": "ID", "hidden": true, "id": "update-ID"},
                  {
                    "type": "input-text",
                    "name": "ORDER_ID",
                    "label": "订单号",
                    "disabled": true,
                    "id": "update-ORDER_ID"
                  },
                  {
                    "type": "input-text",
                    "name": "TRACKING_NO",
                    "label": "物流单号",
                    "maxLength": 100,
                    "disabled": false,
                    "id": "update-TRACKING_NO"
                  },
                  {
                    "type": "input-text",
                    "name": "CARRIER",
                    "label": "物流公司",
                    "maxLength": 100,
                    "disabled": false,
                    "id": "update-CARRIER"
                  },
                  {
                    "type": "select",
                    "name": "LOGISTICS_STATUS",
                    "label": "物流状态",
                    "required": true,
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/sys_dict_items",
                      "data": {
                        "@column": ["item_code", "item_name"],
                        "@where": [
                          {"column": "dict_code", "condition": "EQ", "value": "logistics_status"}
                        ]
                      },
                      "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
                    },
                    "clearable": false,
                    "disabled": false,
                    "id": "update-LOGISTICS_STATUS"
                  },
                  {
                    "type": "input-datetime",
                    "name": "SHIP_TIME",
                    "label": "发货时间",
                    "disabled": false,
                    "id": "update-SHIP_TIME"
                  },
                  {
                    "type": "input-datetime",
                    "name": "RECEIVE_TIME",
                    "label": "签收时间",
                    "disabled": false,
                    "id": "update-RECEIVE_TIME"
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
              "url": "/sql/forge/api/json/delete/ORDER_LOGISTICS",
              "data": {
                "@where": [
                  {"column": "ID", "condition": "EQ", "value": "${ID}"}
                ]
              }
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}');

-- 11. 商品分类
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-product-categories', '商品分类', '商品分类树形管理', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/PRODUCT_CATEGORIES",
      "data": {
        "@column": [
          "PRODUCT_CATEGORIES.ID",
          "PRODUCT_CATEGORIES.NAME",
          "parent.NAME as PARENT_NAME",
          "PRODUCT_CATEGORIES.SORT"
        ],
        "@join": [
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "product_categories parent",
            "on": "PRODUCT_CATEGORIES.PARENT_ID = parent.ID"
          }
        ],
        "@where": [
          {
            "column": "PRODUCT_CATEGORIES.NAME",
            "condition": "LIKE",
            "value": "${NAME | default:undefined}"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (:orderBy + '' '' + orderDir):'''',undefined)}"
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
          "title": "新增分类",
          "body": {
            "type": "form",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/insert/PRODUCT_CATEGORIES",
              "data": {
                "@set": {
                  "ID": "${ID | default:undefined}",
                  "NAME": "${NAME | default:undefined}",
                  "PARENT_ID": "${PARENT_ID | default:undefined}",
                  "SORT": "${SORT | default:0}"
                }
              }
            },
            "onEvent": {
              "submitSucc": {
                "actions": [
                  {"actionType": "reload", "componentId": "crud_table"}
                ]
              }
            },
            "body": [
              {"type": "uuid", "id": "insert-ID", "name": "ID"},
              {
                "type": "input-text",
                "name": "NAME",
                "label": "分类名称",
                "maxLength": 100,
                "required": true,
                "disabled": false,
                "id": "insert-NAME"
              },
              {
                "type": "select",
                "name": "PARENT_ID",
                "label": "父分类",
                "source": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/PRODUCT_CATEGORIES",
                  "data": {"@column": ["ID", "NAME"]},
                  "adaptor": "return {\n  options: [{value: '''', label: ''顶级分类''}, ...payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.name || item.NAME\n  }))]\n};"
                },
                "clearable": true,
                "disabled": false,
                "id": "insert-PARENT_ID"
              },
              {
                "type": "input-number",
                "name": "SORT",
                "label": "排序",
                "precision": 0,
                "value": 0,
                "disabled": false,
                "id": "insert-SORT"
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
          "url": "/sql/forge/api/json/delete/PRODUCT_CATEGORIES",
          "data": {
            "@where": [
              {"column": "ID", "condition": "IN", "value": "${ids | split}"}
            ]
          }
        },
        "confirmText": "确定要批量删除?"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ID",
    "columns": [
      {"name": "ID", "hidden": true},
      {
        "name": "NAME",
        "label": "分类名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "NAME",
          "label": "分类名称",
          "placeholder": "输入分类名称"
        }
      },
      {"name": "PARENT_NAME", "label": "父分类", "sortable": true},
      {"name": "SORT", "label": "排序", "sortable": true, "align": "right"},
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
              "title": "编辑分类",
              "body": {
                "type": "form",
                "initApi": {
                  "method": "post",
                  "url": "/sql/forge/api/json/select/PRODUCT_CATEGORIES",
                  "data": {
                    "@column": [
                      "PRODUCT_CATEGORIES.ID",
                      "PRODUCT_CATEGORIES.NAME",
                      "PRODUCT_CATEGORIES.PARENT_ID",
                      "PRODUCT_CATEGORIES.SORT"
                    ],
                    "@join": [],
                    "@where": [
                      {"column": "PRODUCT_CATEGORIES.ID", "condition": "EQ", "value": "${ID}"}
                    ]
                  },
                  "responseData": {"&": "${items | first}"}
                },
                "api": {
                  "method": "post",
                  "url": "/sql/forge/api/json/update/PRODUCT_CATEGORIES",
                  "data": {
                    "@set": {
                      "ID": "${ID}",
                      "NAME": "${NAME}",
                      "PARENT_ID": "${PARENT_ID}",
                      "SORT": "${SORT}"
                    },
                    "@where": [
                      {"column": "ID", "condition": "EQ", "value": "${ID}"}
                    ]
                  }
                },
                "body": [
                  {"type": "input-text", "name": "ID", "hidden": true, "id": "update-ID"},
                  {
                    "type": "input-text",
                    "name": "NAME",
                    "label": "分类名称",
                    "maxLength": 100,
                    "required": true,
                    "disabled": false,
                    "id": "update-NAME"
                  },
                  {
                    "type": "select",
                    "name": "PARENT_ID",
                    "label": "父分类",
                    "source": {
                      "method": "post",
                      "url": "/sql/forge/api/json/select/PRODUCT_CATEGORIES",
                      "data": {"@column": ["ID", "NAME"]},
                      "adaptor": "return {\n  options: [{value: '''', label: ''顶级分类''}, ...payload.map(item => ({\n    value: item.id || item.ID,\n    label: item.name || item.NAME\n  }))]\n};"
                    },
                    "clearable": true,
                    "disabled": false,
                    "id": "update-PARENT_ID"
                  },
                  {
                    "type": "input-number",
                    "name": "SORT",
                    "label": "排序",
                    "precision": 0,
                    "disabled": false,
                    "id": "update-SORT"
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
              "url": "/sql/forge/api/json/delete/PRODUCT_CATEGORIES",
              "data": {
                "@where": [
                  {"column": "ID", "condition": "EQ", "value": "${ID}"}
                ]
              }
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}');

-- 12. 评价管理
INSERT INTO sql_forge_template_amis (id, name, description, context)
VALUES ('amis-template-product-reviews', '评价管理', '商品评价查询', '{
  "type": "page",
  "body": {
    "type": "crud",
    "id": "crud_table",
    "api": {
      "method": "post",
      "url": "/sql/forge/api/json/selectPage/PRODUCT_REVIEWS",
      "data": {
        "@column": [
          "PRODUCT_REVIEWS.ID",
          "ORDERS.ID as ORDER_ID",
          "products.NAME as PRODUCT_NAME",
          "users.USERNAME as USER_NAME",
          "review_rating.item_name as RATING",
          "PRODUCT_REVIEWS.CONTENT",
          "PRODUCT_REVIEWS.CREATED_AT"
        ],
        "@join": [
          {
            "type": "INNER_JOIN",
            "joinTable": "orders ORDERS",
            "on": "PRODUCT_REVIEWS.ORDER_ID = ORDERS.ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "products products",
            "on": "PRODUCT_REVIEWS.PRODUCT_ID = products.ID"
          },
          {
            "type": "INNER_JOIN",
            "joinTable": "users users",
            "on": "PRODUCT_REVIEWS.USER_ID = users.ID"
          },
          {
            "type": "LEFT_OUTER_JOIN",
            "joinTable": "sys_dict_items review_rating",
            "on": "CAST(PRODUCT_REVIEWS.RATING AS VARCHAR) = review_rating.item_code"
          }
        ],
        "@where": [
          {
            "column": "users.USERNAME",
            "condition": "LIKE",
            "value": "${USER_NAME | default:undefined}"
          },
          {
            "column": "products.NAME",
            "condition": "LIKE",
            "value": "${PRODUCT_NAME | default:undefined}"
          },
          {
            "column": "PRODUCT_REVIEWS.RATING",
            "condition": "IN",
            "value": "${RATING | default:undefined | split}"
          },
          {
            "column": "review_rating.dict_code",
            "condition": "EQ",
            "value": "review_rating"
          }
        ],
        "@order": [
          "${default(orderBy && orderDir ? (orderBy + '' '' + orderDir):'''',undefined)}"
        ],
        "@page": {
          "pageIndex": "${page - 1}",
          "pageSize": "${perPage}"
        }
      }
    },
    "headerToolbar": [
      "bulkActions",
      {
        "type": "columns-toggler",
        "draggable": true,
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
          "url": "/sql/forge/api/json/delete/PRODUCT_REVIEWS",
          "data": {
            "@where": [
              {"column": "ID", "condition": "IN", "value": "${ids | split}"}
            ]
          }
        },
        "confirmText": "确定要批量删除?"
      }
    ],
    "keepItemSelectionOnPageChange": true,
    "autoFillHeight": true,
    "autoGenerateFilter": true,
    "showIndex": true,
    "primaryField": "ID",
    "columns": [
      {"name": "ID", "hidden": true},
      {
        "name": "ORDER_ID",
        "label": "订单号",
        "sortable": true
      },
      {
        "name": "PRODUCT_NAME",
        "label": "商品名称",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "PRODUCT_NAME",
          "label": "商品名称",
          "placeholder": "输入商品名称"
        }
      },
      {
        "name": "USER_NAME",
        "label": "用户名",
        "sortable": true,
        "searchable": {
          "type": "input-text",
          "name": "USER_NAME",
          "label": "用户名",
          "placeholder": "输入用户名"
        }
      },
      {
        "name": "RATING",
        "label": "评分",
        "sortable": true,
        "searchable": {
          "type": "select",
          "name": "RATING",
          "label": "评分",
          "placeholder": "选择评分",
          "multiple": true,
          "source": {
            "method": "post",
            "url": "/sql/forge/api/json/select/sys_dict_items",
            "data": {
              "@column": ["item_code", "item_name"],
              "@where": [
                {"column": "dict_code", "condition": "EQ", "value": "review_rating"}
              ]
            },
            "adaptor": "return {\n  options: payload.map(item => ({\n    value: item.item_code || item.ITEM_CODE,\n    label: item.item_name || item.ITEM_NAME\n  }))\n};"
          },
          "clearable": true
        }
      },
      {
        "name": "CONTENT",
        "label": "评价内容",
        "type": "text"
      },
      {
        "name": "CREATED_AT",
        "label": "评价时间",
        "sortable": true,
        "type": "datetime"
      },
      {
        "type": "operation",
        "label": "操作",
        "buttons": [
          {
            "label": "删除",
            "type": "button",
            "icon": "fa fa-minus",
            "actionType": "ajax",
            "level": "danger",
            "confirmText": "确认要删除？",
            "api": {
              "method": "post",
              "url": "/sql/forge/api/json/delete/PRODUCT_REVIEWS",
              "data": {
                "@where": [
                  {"column": "ID", "condition": "EQ", "value": "${ID}"}
                ]
              }
            }
          }
        ],
        "fixed": "right"
      }
    ]
  }
}');
