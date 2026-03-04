import React from 'react';
import {Editor} from 'amis-editor';

export default {
  type: 'page',
  id: 'amisTemplate',
  title: 'AMIS模板',
  body: [
    {
      type: 'crud',
      id: 'amisTemplate_crud',
      api: {
        method: 'get',
        url: '/sql/forge/api/template/amis',
        data: {
          id: '${search_id | default:undefined}',
          executorName: '${search_executorName | default:undefined}',
          context: '${search_context | default:undefined}'
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
            title: '新增AMIS模板',
            size: 'xl',
            body: [
              {
                type: 'form',
                id: 'amisTemplate_create_form',
                api: {
                  method: 'put',
                  url: '/sql/forge/api/template/amis'
                },
                data: {
                  context: JSON.stringify(
                    {
                      type: 'page',
                      title: '标题',
                      body: 'Hello World!'
                    },
                    null,
                    2
                  )
                },
                body: [
                  {
                    type: 'input-text',
                    name: 'id',
                    label: '模板标识',
                    required: true
                  },
                  {
                    type: 'editor',
                    name: 'context',
                    label: 'AMIS模板',
                    language: 'json',
                    required: true
                  }
                ]
              },
              {
                type: 'wrapper',
                body: [
                  {
                    type: 'button',
                    label: '编辑器',
                    level: 'primary',
                    actionType: 'drawer',
                    drawer: {
                      title: '编辑器',
                      width: '100%',
                      body: [
                        {
                          type: 'form',
                          data: {
                            context:
                              '${GETRENDERERDATA("amisTemplate_create_form", "context")}'
                          },
                          body: [
                            {
                              name: 'context',
                              asFormItem: true,
                              labelWidth: '0px',
                              children: ({value, onChange, data}) => (
                                <div style={{height: 'calc(100vh - 159px)'}}>
                                  <Editor
                                    value={JSON.parse(value)}
                                    onChange={value =>
                                      onChange(JSON.stringify(value, null, 2))
                                    }
                                  />
                                </div>
                              )
                            }
                          ]
                        }
                      ],
                      onEvent: {
                        confirm: {
                          actions: [
                            {
                              actionType: 'setValue',
                              componentId: 'amisTemplate_create_form',
                              args: {
                                value: {
                                  context: '${context}'
                                }
                              }
                            }
                          ]
                        }
                      }
                    }
                  }
                ],
                style: {
                  display: 'flex',
                  justifyContent: 'flex-end',
                  gap: '8px'
                }
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
            name: 'search-id',
            label: '模板标识'
          }
        },
        {
          name: 'context',
          label: '内容',
          searchable: {
            type: 'input-text',
            name: 'search-context',
            label: '内容'
          }
        },
        {
          type: 'operation',
          label: '操作',
          buttons: [
            {
              label: '预览',
              type: 'button',
              icon: 'fa fa-eye',
              actionType: 'drawer',
              drawer: {
                title: '预览AMIS模板',
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
                    type: 'service',
                    schemaApi: {
                      method: 'get',
                      url: '/sql/forge/api/template/amis/${id}',
                      adaptor: function (payload: any) {
                        return JSON.parse(payload.context);
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
                title: '修改AMIS模板',
                size: 'xl',
                body: [
                  {
                    type: 'form',
                    id: 'amisTemplate_edit_form',
                    initApi: {
                      method: 'get',
                      url: '/sql/forge/api/template/amis/${id}'
                    },
                    api: {
                      method: 'put',
                      url: '/sql/forge/api/template/amis'
                    },
                    body: [
                      {
                        type: 'input-text',
                        name: 'id',
                        label: '模板标识',
                        disabled: true
                      },
                      {
                        type: 'editor',
                        name: 'context',
                        label: 'AMIS模板',
                        language: 'json',
                        required: true
                      }
                    ]
                  },
                  {
                    type: 'wrapper',
                    body: [
                      {
                        type: 'button',
                        label: '编辑器',
                        level: 'primary',
                        actionType: 'drawer',
                        drawer: {
                          title: '编辑器',
                          width: '100%',
                          body: [
                            {
                              type: 'form',
                              data: {
                                context:
                                  '${GETRENDERERDATA("amisTemplate_edit_form", "context")}'
                              },
                              body: [
                                {
                                  name: 'context',
                                  asFormItem: true,
                                  labelWidth: '0px',
                                  children: ({value, onChange, data}) => (
                                    <div
                                      style={{height: 'calc(100vh - 159px)'}}
                                    >
                                      <Editor
                                        value={JSON.parse(value)}
                                        onChange={value =>
                                          onChange(
                                            JSON.stringify(value, null, 2)
                                          )
                                        }
                                      />
                                    </div>
                                  )
                                }
                              ]
                            }
                          ],
                          onEvent: {
                            confirm: {
                              actions: [
                                {
                                  actionType: 'setValue',
                                  componentId: 'amisTemplate_edit_form',
                                  args: {
                                    value: {
                                      context: '${context}'
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      }
                    ],
                    style: {
                      display: 'flex',
                      justifyContent: 'flex-end',
                      gap: '8px'
                    }
                  }
                ]
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
                url: '/sql/forge/api/template/amis/${id}'
              }
            }
          ],
          fixed: 'right'
        }
      ]
    }
  ]
};
