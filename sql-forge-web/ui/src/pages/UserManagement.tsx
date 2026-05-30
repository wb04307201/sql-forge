import {Schema} from 'amis-core/lib/types';

const UserManagement: Schema = {
  type: 'page',
  title: '用户管理',
  body: {
    type: 'crud',
    id: 'user_crud',
    api: {
      method: 'get',
      url: '/sql/forge/api/user'
    },
    filterTogglable: false,
    filter: {
      type: 'form',
      body: [
        {type: 'input-text', name: 'username', label: '用户名', placeholder: '输入用户名'},
        {type: 'select', name: 'category', label: '用户分类', clearable: true, placeholder: '请选择',
          options: [
            {label: '管理员', value: 'admin'},
            {label: '普通用户', value: 'user'}
          ]
        },
        {type: 'select', name: 'roleId', label: '角色', clearable: true, placeholder: '请选择',
          source: '/sql/forge/api/role',
          labelField: 'name',
          valueField: 'id'
        }
      ]
    },
    headerToolbar: [
      {
        type: 'button',
        label: '新增用户',
        level: 'primary',
        actionType: 'dialog',
        dialog: {
          title: '新增用户',
          body: {
            type: 'form',
            api: {
              method: 'put',
              url: '/sql/forge/api/user',
              messages: { saveSuccess: '用户创建成功' }
            },
            onEvent: {
              submitSucc: {
                actions: [
                  {
                    actionType: 'ajax',
                    api: {
                      method: 'put',
                      url: '/sql/forge/api/user-role',
                      data: { userId: '${id}', roleIds: '${roleIds}' }
                    }
                  },
                  { actionType: 'reload', componentId: 'user_crud' }
                ]
              }
            },
            body: [
              {type: 'input-text', name: 'id', label: 'ID', required: true},
              {type: 'input-text', name: 'username', label: '用户名', required: true},
              {type: 'select', name: 'category', label: '用户分类', required: true, value: 'user',
                options: [
                  {label: '管理员', value: 'admin'},
                  {label: '普通用户', value: 'user'}
                ]
              },
              {type: 'input-password', name: 'password', label: '密码', required: true},
              {type: 'switch', name: 'enabled', label: '启用', value: true},
              {
                type: 'transfer',
                name: 'roleIds',
                label: '角色分配',
                source: '/sql/forge/api/role',
                labelField: 'name',
                valueField: 'id',
                joinValues: false,
                extractValue: true
              }
            ]
          }
        }
      },
      'bulkActions'
    ],
    columns: [
      {name: 'id', label: 'ID'},
      {name: 'username', label: '用户名'},
      {name: 'category', label: '用户分类'},
      {name: 'roles', label: '角色'},
      {name: 'enabled', label: '状态', type: 'switch', disabled: true},
      {
        type: 'operation',
        label: '操作',
        buttons: [
          {
            type: 'button',
            label: '编辑',
            actionType: 'dialog',
            dialog: {
              title: '编辑用户',
              body: {
                type: 'form',
                initApi: {
                  method: 'get',
                  url: '/sql/forge/api/user-role?userId=${id}'
                },
                api: {
                  method: 'put',
                  url: '/sql/forge/api/user',
                  messages: { saveSuccess: '用户信息保存成功' }
                },
                onEvent: {
                  submitSucc: {
                    actions: [
                      {
                        actionType: 'ajax',
                        api: {
                          method: 'put',
                          url: '/sql/forge/api/user-role',
                          data: { userId: '${id}', roleIds: '${roleIds}' }
                        }
                      },
                      { actionType: 'reload', componentId: 'user_crud' }
                    ]
                  }
                },
                body: [
                  {type: 'input-text', name: 'id', label: 'ID', required: true, disabled: true},
                  {type: 'input-text', name: 'username', label: '用户名', required: true, disabled: true},
                  {type: 'select', name: 'category', label: '用户分类', required: true,
                    options: [
                      {label: '管理员', value: 'admin'},
                      {label: '普通用户', value: 'user'}
                    ]
                  },
                  {type: 'input-password', name: 'password', label: '新密码', placeholder: '留空不修改'},
                  {type: 'switch', name: 'enabled', label: '启用'},
                  {
                    type: 'transfer',
                    name: 'roleIds',
                    label: '角色分配',
                    source: '/sql/forge/api/role',
                    labelField: 'name',
                    valueField: 'id',
                    joinValues: false,
                    extractValue: true
                  }
                ]
              }
            }
          },
          {
            type: 'button',
            label: '删除',
            actionType: 'ajax',
            confirmMsg: '确定删除此用户？',
            api: {method: 'delete', url: '/sql/forge/api/user/${id}'}
          }
        ]
      }
    ]
  }
};

export default UserManagement;
