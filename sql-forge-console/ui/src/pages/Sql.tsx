import {buildTableData} from '../utils/CommonUtils';

export default {
  type: 'page',
  id: 'sql',
  title: 'SQL',
  asideResizor: true,
  data: {
    executorName: 'database'
  },
  aside: {
    type: 'input-tree',
    id: 'metaData',
    name: 'metaData',
    heightAuto: true,
    initiallyOpen: false,
    source: {
      url: '/sql/forge/api/database/metaData?executorName=${executorName}',
      method: 'get',
      adaptor: (payload: any) => {
        return [payload];
      }
    }
  },
  body: [
    {
      type: 'form',
      id: 'sql_form',
      submitText: '执行',
      wrapWithPanel: false,
      data: {
        selectExecutorName: 'database'
      },
      body: [
        {
          label: '数据源',
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
          },
          onEvent: {
            change: {
              actions: [
                {
                  actionType: 'reload',
                  componentId: 'metaData',
                  args: {
                    executorName: '${event.data.value}'
                  }
                }
              ]
            }
          }
        },
        {
          type: 'editor',
          name: 'sql',
          language: 'sql',
          placeholder: '请输入sql语句'
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
                  componentId: 'sql_crud',
                  data: {
                    sql: '${GETRENDERERDATA("sql_form", "sql")}'
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
      id: 'sql_crud',
      api: {
        method: 'post',
        url: '/sql/forge/api/database/execute',
        sendOn: 'this.sql != null && this.sql != ""',
        adaptor: (payload: any) => {
          console.log('payload', payload);
          if (payload != null && payload.length > 0) {
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
