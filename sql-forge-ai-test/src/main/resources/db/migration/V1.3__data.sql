INSERT INTO sql_forge_template_sql (id, executor_name, context)
VALUES ('sql-template-database', 'database', 'SELECT * FROM users WHERE 1=1
<if test="name != null && name != ''''">AND username = #{name}</if>
<if test="ids != null && !ids.isEmpty()"><foreach collection="ids" item="id" open="AND id IN (" separator="," close=")">#{id}</foreach></if>
<if test="(name == null || name == '''') && (ids == null || ids.isEmpty()) ">AND 0=1</if>
ORDER BY username DESC'),
       ('sql-template-calcite', 'calcite', 'select student.name, sum(score.grade) as grade
from MYSQL.student as student join POSTGRES.score as score on student.id=score.student_id where 1=1
<if test="ids == null || ids.isEmpty()">AND 0=1</if>
<if test="ids != null && !ids.isEmpty()">
<foreach collection="ids" item="id" open="AND student.id IN (" separator="," close=")">#{id}</foreach>
</if>
group by student.name');

-- 图表
INSERT INTO sql_forge_template_amis (id, context)
VALUES ('amis-template-chart',  '{
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
      "series": [
        {
          "data": "${items | pick:grade}",
          "type": "bar"
        }
      ]
    }
  }
}');
-- 字典
INSERT INTO sql_forge_template_amis (id, context)
VALUES ('amis-template-dicts',  '{
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
                "${default(orderBy && orderDir ? (orderBy + '' '' + orderDir):'''',undefined)}"
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
                "${default(orderBy && orderDir ? (orderBy + '' '' + orderDir):'''',undefined)}"
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
-- 用户
INSERT INTO sql_forge_template_amis (id, context)
VALUES ('amis-template-users',  '{
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
            "type": "LEFT_OUTER_JOIN",
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
          "${default(orderBy && orderDir ? (orderBy + '' '' + orderDir):'''',undefined)}"
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
                "type": "LEFT_OUTER_JOIN",
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
                        "type": "LEFT_OUTER_JOIN",
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
}');



select *
from sql_forge_template_sql;
select *
from sql_forge_template_amis;