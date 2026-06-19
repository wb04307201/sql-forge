import {buildTableData} from '../utils/CommonUtils';

export default {
  type: 'page',
  id: 'sqlTemplate',
  title: 'SQL模板',
  body: [
    {
      type: 'crud',
      id: 'sqlTemplate_crud',
      api: {
        method: 'get',
        url: '/sql/forge/api/template/sql',
        data: {
          id: '${id | default:undefined}',
          name: '${name | default:undefined}',
          description: '${description | default:undefined}',
          executorName: '${executorName | default:undefined}',
          context: '${context | default:undefined}'
        }
      },
      autoFillHeight: true,
      autoGenerateFilter: true,
      showIndex: true,
      primaryField: 'ID',
      headerToolbar: [
        {
          label: '新增',
          type: 'button',
          icon: 'fa fa-plus',
          level: 'primary',
          actionType: 'drawer',
          drawer: {
            title: '新增SQL模板',
            size: 'xl',
            body: [
              {
                type: 'form',
                api: {
                  method: 'put',
                  url: '/sql/forge/api/template/sql'
                },
                body: [
                  {
                    type: 'input-text',
                    name: 'id',
                    label: '模板标识',
                    required: true
                  },
                  {
                    type: 'input-text',
                    name: 'name',
                    label: '模板名称',
                    required: true
                  },
                  {
                    type: 'input-text',
                    name: 'description',
                    label: '模板描述'
                  },
                  {
                    type: 'select',
                    name: 'executorName',
                    label: '数据源',
                    required: true,
                    source: {
                      url: '/sql/forge/api/console/executorName',
                      method: 'get',
                      adaptor: (payload: any) => {
                        return {
                          options: payload.map((item: any) => {
                            return {
                              label: item,
                              value: item
                            };
                          })
                        };
                      }
                    }
                  },
                  {
                    type: 'editor',
                    name: 'context',
                    label: 'SQL模板',
                    language: 'xml',
                    required: true
                  }
                ]
              }
            ]
          }
        }
      ],
      columns: [
        {
          name: 'id',
          label: '模板标识',
          searchable: {
            type: 'input-text',
            name: 'id',
            label: '模板标识'
          }
        },
        {
          name: 'name',
          label: '模板名称',
          searchable: {
            type: 'input-text',
            name: 'name',
            label: '模板名称'
          }
        },
        {
          name: 'description',
          label: '模板描述',
          searchable: {
            type: 'input-text',
            name: 'description',
            label: '模板名称'
          }
        },
        {
          name: 'executorName',
          label: '数据源',
          searchable: {
            type: 'select',
            name: 'executorName',
            label: '数据源',
            clearable: true,
            source: {
              url: '/sql/forge/api/console/executorName',
              method: 'get',
              adaptor: (payload: any) => {
                return {
                  options: payload.map((item: any) => {
                    return {
                      label: item,
                      value: item
                    };
                  })
                };
              }
            }
          }
        },
        {
          name: 'context',
          label: '内容',
          searchable: {
            type: 'input-text',
            name: 'context',
            label: '内容'
          }
        },
        {
          type: 'operation',
          label: '操作',
          buttons: [
            {
              label: '测试',
              type: 'button',
              icon: 'fa fa-plug',
              actionType: 'drawer',
              drawer: {
                title: '测试SQL模板',
                size: 'xl',
                actions: [
                  {
                    type: 'button',
                    actionType: 'cancel',
                    label: '取消'
                  }
                ],
                body: [
                  {
                    type: 'form',
                    id: 'execute_sqlTemplate',
                    initApi: {
                      method: 'get',
                      url: '/sql/forge/api/template/sql/${id}'
                    },
                    body: [
                      {
                        type: 'group',
                        body: [
                          {
                            type: 'input-text',
                            name: 'id',
                            label: '模板标识',
                            required: true
                          },
                          {
                            type: 'select',
                            name: 'executorName',
                            label: '数据源',
                            disabled: true,
                            source: {
                              url: '/sql/forge/api/console/executorName',
                              method: 'get',
                              adaptor: (payload: any) => {
                                return {
                                  options: payload.map((item: any) => {
                                    return {
                                      label: item,
                                      value: item
                                    };
                                  })
                                };
                              }
                            }
                          }
                        ]
                      },
                      {
                        type: 'group',
                        body: [
                          {
                            type: 'editor',
                            name: 'context',
                            label: 'SQL模板',
                            language: 'xml',
                            disabled: true,
                            columnRatio: 6
                          },
                          {
                            type: 'editor',
                            name: 'json',
                            label: '请求体',
                            language: 'json',
                            columnRatio: 6
                          }
                        ]
                      }
                    ]
                  },
                  {
                    type: 'wrapper',
                    body: [
                      {
                        type: 'action',
                        label: '执行',
                        level: 'primary',
                        onEvent: {
                          click: {
                            actions: [
                              {
                                actionType: 'reload',
                                componentId: 'sqlTemplate_crud',
                                data: {
                                  id: '${GETRENDERERDATA("execute_sqlTemplate", "id")}',
                                  json: '${GETRENDERERDATA("execute_sqlTemplate", "json")}'
                                }
                              }
                            ]
                          }
                        }
                      }
                    ],
                    style: {
                      display: 'flex',
                      justifyContent: 'flex-end',
                      gap: '8px'
                    }
                  },
                  {
                    type: 'crud',
                    id: 'sqlTemplate_crud',
                    api: {
                      method: 'post',
                      url: '/sql/forge/api/template/sql/${id}',
                      sendOn: 'this.id != null && this.json != null',
                      requestAdaptor: function (api: any, context: any) {
                        return {
                          ...api,
                          data: JSON.parse(context.json)
                        };
                      },
                      adaptor: (payload: any) => {
                        console.log('payload', payload);
                        if (payload != null) {
                          return buildTableData(payload);
                        } else {
                          return {
                            columns: [],
                            rows: []
                          };
                        }
                      }
                    }
                  }
                ]
              }
            },
            {
              label: '修改',
              type: 'button',
              icon: 'fa fa-pen-to-square',
              actionType: 'drawer',
              drawer: {
                title: '修改SQL模板',
                size: 'xl',
                body: {
                  type: 'form',
                  initApi: {
                    method: 'get',
                    url: '/sql/forge/api/template/sql/${id}'
                  },
                  api: {
                    method: 'put',
                    url: '/sql/forge/api/template/sql'
                  },
                  body: [
                    {
                      type: 'input-text',
                      name: 'id',
                      label: '模板标识',
                      disabled: true
                    },
                    {
                      type: 'input-text',
                      name: 'name',
                      label: '模板名称',
                      required: true
                    },
                    {
                      type: 'input-text',
                      name: 'description',
                      label: '模板描述'
                    },
                    {
                      type: 'select',
                      name: 'executorName',
                      label: '数据源',
                      required: true,
                      source: {
                        url: '/sql/forge/api/console/executorName',
                        method: 'get',
                        adaptor: (payload: any) => {
                          return {
                            options: payload.map((item: any) => {
                              return {
                                label: item,
                                value: item
                              };
                            })
                          };
                        }
                      }
                    },
                    {
                      type: 'editor',
                      name: 'context',
                      label: 'SQL模板',
                      language: 'xml',
                      required: true
                    }
                  ]
                }
              }
            },
            {
              label: '删除',
              type: 'button',
              icon: 'fa fa-minus',
              actionType: 'ajax',
              level: 'danger',
              confirmText: '确认要删除？',
              api: {
                method: 'delete',
                url: '/sql/forge/api/template/sql/${id}'
              }
            }
          ],
          fixed: 'right'
        }
      ]
    }
  ]
};
