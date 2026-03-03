import {buildTableData} from '../utils/CommonUtils';

export default {
  type: 'page',
  id: 'json',
  title: 'JSON',
  body: [
    {
      type: 'form',
      id: 'json_form',
      submitText: '执行',
      wrapWithPanel: false,
      data: {
        selectExecutorName: 'database'
      },
      body: [
        {
          type: 'group',
          body: [
            {
              type: 'select',
              name: 'selectExecutorName',
              placeholder: '请选择数据源',
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
              type: 'select',
              name: 'method',
              placeholder: '请选择方法',
              options: [
                {
                  label: '查询',
                  value: 'select'
                },
                {
                  label: '分页查询',
                  value: 'selectPage'
                },
                {
                  label: '插入',
                  value: 'insert'
                },
                {
                  label: '更新',
                  value: 'update'
                },
                {
                  label: '删除',
                  value: 'delete'
                }
              ]
            },
            {
              type: 'input-text',
              name: 'table',
              placeholder: '请输入表名/表名 别名'
            }
          ]
        },
        {
          type: 'editor',
          name: 'json',
          language: 'json',
          placeholder: '请输入json'
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
                  componentId: 'json_crud',
                  data: {
                    executorName:
                      '${GETRENDERERDATA("json_form", "selectExecutorName")}',
                    method: '${GETRENDERERDATA("json_form", "method")}',
                    table: '${GETRENDERERDATA("json_form", "table")}',
                    json: '${GETRENDERERDATA("json_form", "json")}'
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
      id: 'json_crud',
      api: {
        method: 'post',
        url: '/sql/forge/api/json/${method}/${table}?executorName=${executorName}',
        sendOn: 'this.method != null && this.table != null && this.json !=null',
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
};
