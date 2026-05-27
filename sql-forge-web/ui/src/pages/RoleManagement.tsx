import {Schema} from 'amis-core/lib/types';

const RoleManagement: Schema = {
  type: 'page',
  title: '角色管理',
  body: {
    type: 'crud',
    id: 'role_crud',
    api: {
      method: 'get',
      url: '/sql/forge/api/role'
    },
    filter: {
      type: 'form',
      title: '搜索',
      submitText: '提交',
      body: [
        {type: 'input-text', name: 'name', label: '角色名称', placeholder: '输入角色名称'}
      ]
    },
    headerToolbar: [
      {
        type: 'button',
        label: '新增角色',
        level: 'primary',
        actionType: 'dialog',
        dialog: {
          title: '新增角色',
          body: {
            type: 'form',
            api: {
              method: 'put',
              url: '/sql/forge/api/role',
              messages: { saveSuccess: '角色创建成功' }
            },
            body: [
              {type: 'input-text', name: 'id', label: '角色ID', required: true, placeholder: '如: editor'},
              {type: 'input-text', name: 'name', label: '角色名称', required: true},
              {type: 'input-text', name: 'description', label: '描述'}
            ]
          },
          onEvent: {
            submitSucc: {
              actions: [
                { actionType: 'reload', componentId: 'role_crud' }
              ]
            }
          }
        }
      },
      'bulkActions'
    ],
    columns: [
      {name: 'id', label: '角色ID'},
      {name: 'name', label: '角色名称'},
      {name: 'description', label: '描述'},
      {
        type: 'button',
        label: '编辑',
        level: 'link',
        actionType: 'dialog',
        dialog: {
          title: '编辑角色',
          body: {
            type: 'form',
            api: {
              method: 'put',
              url: '/sql/forge/api/role',
              messages: { saveSuccess: '角色保存成功' }
            },
            body: [
              {type: 'input-text', name: 'id', label: '角色ID', disabled: true},
              {type: 'input-text', name: 'name', label: '角色名称', required: true},
              {type: 'input-text', name: 'description', label: '描述'}
            ]
          },
          onEvent: {
            submitSucc: {
              actions: [
                { actionType: 'reload', componentId: 'role_crud' }
              ]
            }
          }
        }
      },
      {
        type: 'button',
        label: '分配模板',
        level: 'link',
        actionType: 'dialog',
        dialog: {
          title: '分配AMIS模板 - ${name}',
          size: 'lg',
          body: {
            type: 'form',
            api: {
              method: 'put',
              url: '/sql/forge/api/role-template',
              data: {
                role: '${id}',
                templateIds: '${templateIds}'
              },
              messages: { saveSuccess: '模板分配成功' }
            },
            initApi: {
              method: 'get',
              url: '/sql/forge/api/role-template?role=${id}'
            },
            body: [
              {type: 'hidden', name: 'role', value: '${id}'},
              {
                type: 'select',
                name: 'templateIds',
                label: 'AMIS模板',
                multiple: true,
                extractValue: true,
                joinValues: false,
                source: {
                  method: 'get',
                  url: '/sql/forge/api/template/amis'
                },
                labelField: 'name',
                valueField: 'id'
              }
            ]
          },
          onEvent: {
            submitSucc: {
              actions: [
                { actionType: 'reload', componentId: 'role_crud' }
              ]
            }
          }
        }
      },
      {
        type: 'button',
        label: '删除',
        level: 'danger',
        actionType: 'ajax',
        api: 'delete:/sql/forge/api/role/${id}',
        confirmText: '确定删除此角色吗？'
      }
    ]
  }
};

export default RoleManagement;
