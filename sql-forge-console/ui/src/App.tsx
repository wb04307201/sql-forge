import React, {useState} from 'react';
import {AlertComponent, ToastComponent} from 'amis';
import AmisRender from './render/AmisRender';
import {MenuProps, TabsProps} from 'antd';
import {Layout, Tabs, Menu} from 'antd';
import keySchema, {KeySchemaProps} from './pages/KeySchema';
import {ApiOutlined, OpenAIOutlined} from '@ant-design/icons';

type MenuItem = Required<MenuProps>['items'][number];
type TabsItem = Required<TabsProps>['items'][number];

const items: MenuItem[] = [
  {
    key: 'api',
    label: 'API',
    icon: <ApiOutlined />,
    children: [
      {key: 'sql', label: 'SQL'},
      {key: 'json', label: 'JSON'},
      {key: 'templateSql', label: 'SQL模板'},
      {key: 'templateAmis', label: 'AMIS模板'}
    ]
  },
  {
    key: 'ai',
    label: 'AI',
    icon: <OpenAIOutlined />,
    children: []
  }
];

type TargetKey = React.MouseEvent | React.KeyboardEvent | string;

function App() {
  const [tabsItems, setTabsItems] = useState<TabsItem[]>([]);
  const [activeKey, setActiveKey] = useState<string>();
  const [openKeys, setOpenKeys] = useState<string[]>(['api', 'ai']);

  const onEdit = (targetKey: TargetKey, action: 'add' | 'remove') => {
    if (action === 'add') {
      console.error('add not support');
    } else {
      remove(targetKey);
    }
  };

  const remove = (targetKey: TargetKey) => {
    const targetIndex = tabsItems.findIndex(pane => pane.key === targetKey);
    const newPanes = tabsItems.filter(pane => pane.key !== targetKey);
    if (newPanes.length && targetKey === activeKey) {
      const {key} =
        newPanes[
          targetIndex === newPanes.length ? targetIndex - 1 : targetIndex
        ];
      setActiveKey(key);
    }
    setTabsItems(newPanes);
  };

  return (
    <>
      <ToastComponent key="toast" position={'top-right'} />
      <AlertComponent key="alert" />
      <Layout style={{height: '100%', width: '100%'}}>
        <Layout.Sider>
          <Menu
            defaultSelectedKeys={['1']}
            defaultOpenKeys={['sub1']}
            mode="inline"
            theme="dark"
            items={items}
            onClick={({_, key}) => {
              if (tabsItems?.some(item => item.key === key)) {
                setActiveKey(key);
                return;
              }

              const keySchemaProps: KeySchemaProps = keySchema[key];

              setTabsItems([
                ...tabsItems,
                {
                  label: keySchemaProps.label,
                  key: key,
                  children: (
                    <div
                      style={{
                        width: '100%',
                        height: 'calc(100vh - 56px)',
                        overflowY: 'auto'
                      }}
                    >
                      <AmisRender schema={keySchemaProps.schema} />
                    </div>
                  )
                }
              ]);
              setActiveKey(key);
            }}
            openKeys={openKeys}
            onOpenChange={setOpenKeys}
          />
        </Layout.Sider>
        <Layout.Content>
          <Tabs
            items={tabsItems}
            activeKey={activeKey}
            onChange={key => setActiveKey(key)}
            hideAdd
            type="editable-card"
            onEdit={onEdit}
          />
        </Layout.Content>
      </Layout>
    </>
  );
}

export default App;
