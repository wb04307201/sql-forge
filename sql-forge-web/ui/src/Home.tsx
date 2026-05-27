import React, { useState, useEffect } from 'react';
import { Menu, Layout, Button, message, Tabs } from 'antd';
import { LogoutOutlined, DashboardOutlined } from '@ant-design/icons';
import axios from 'axios';
import AmisRender from './render/AmisRender';

const { Sider, Content, Header } = Layout;

interface TemplateItem {
  id: string;
  name: string;
  context: string;
}

function Home() {
  const [menuItems, setMenuItems] = useState<{ key: string; label: string }[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);
  const [tabs, setTabs] = useState<{ key: string; label: string; schema: any }[]>([]);
  const [activeKey, setActiveKey] = useState<string>('');

  useEffect(() => {
    axios.get('/sql/forge/api/auth/user', { withCredentials: true })
      .then(res => {
        if (res.data.success) {
          const data = res.data.data;
          const roles = data.roles || [];
          setIsAdmin(data.category === 'admin');

          // 根据角色获取所有关联的模板ID（去重）
          const fetchTemplates = async () => {
            const allTemplateIds = new Set<string>();
            for (const role of roles) {
              try {
                const tplRes = await axios.get(`/sql/forge/api/role-template?role=${role}`, { withCredentials: true });
                const ids = tplRes.data.templateIds || [];
                ids.forEach((id: string) => allTemplateIds.add(id));
              } catch {
                // 忽略单个角色的请求失败
              }
            }

            // 根据模板ID获取模板详情
            const templates: TemplateItem[] = [];
            for (const id of allTemplateIds) {
              try {
                const detailRes = await axios.get(`/sql/forge/api/template/amis/${id}`, { withCredentials: true });
                if (detailRes.data) {
                  templates.push(detailRes.data);
                }
              } catch {
                // 忽略单个模板的获取失败
              }
            }

            const items = templates.map(t => ({ key: t.id, label: t.name || t.id }));
            setMenuItems(items);
            setLoading(false);
          };

          fetchTemplates();
        }
      })
      .catch(() => {
        setLoading(false);
      });
  }, []);

  const handleSelect = async (info: any) => {
    const templateId = info.key;
    setSelectedKeys([templateId]);

    try {
      const res = await axios.get(`/sql/forge/api/template/amis/${templateId}`, { withCredentials: true });
      const data = res.data;
      const schema = data.context ? JSON.parse(data.context) : data;

      // 以 Tab 形式打开：已存在的 Tab 切换，不存在的新增
      const existingTab = tabs.find(t => t.key === templateId);
      if (existingTab) {
        setActiveKey(templateId);
      } else {
        setTabs(prev => [...prev, { key: templateId, label: data.name || templateId, schema }]);
        setActiveKey(templateId);
      }
    } catch {
      message.error('加载页面失败');
    }
  };

  const handleTabRemove = (targetKey: string) => {
    const newTabs = tabs.filter(t => t.key !== targetKey);
    setTabs(newTabs);
    if (activeKey === targetKey && newTabs.length > 0) {
      setActiveKey(newTabs[newTabs.length - 1].key);
    } else if (newTabs.length === 0) {
      setActiveKey('');
    }
  };

  const handleLogout = async () => {
    try {
      await axios.post('/sql/forge/api/auth/logout', {}, { withCredentials: true });
      window.location.href = '/sql/forge/web/login.html';
    } catch {
      message.error('退出失败');
    }
  };

  if (loading) {
    return <div style={{ padding: 20 }}>加载中...</div>;
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 16px' }}>
        <span style={{ fontSize: 18, fontWeight: 'bold', color: '#fff' }}>SQL Forge</span>
        <div>
          {isAdmin && <Button type="link" style={{ color: '#fff' }} icon={<DashboardOutlined />} onClick={() => window.location.href = '/sql/forge/web/index.html'}>控制台</Button>}
          <Button type="link" style={{ color: '#fff' }} icon={<LogoutOutlined />} onClick={handleLogout}>退出</Button>
        </div>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff', overflow: 'auto', height: 'calc(100vh - 64px)' }}>
          <Menu
            mode="inline"
            selectedKeys={selectedKeys}
            style={{ height: '100%', borderRight: 0 }}
            items={menuItems}
            onSelect={handleSelect}
          />
        </Sider>
        <Content style={{ padding: '8px 8px 0', overflow: 'auto', background: '#fff' }}>
          {tabs.length > 0 ? (
            <Tabs
              type="editable-card"
              hideAdd
              activeKey={activeKey}
              onChange={setActiveKey}
              onEdit={(key, action) => action === 'remove' && handleTabRemove(key as string)}
              items={tabs.map(t => ({
                key: t.key,
                label: t.label,
                children: <AmisRender schema={t.schema} />
              }))}
              style={{ height: 'calc(100vh - 72px)' }}
            />
          ) : (
            <div style={{ padding: 40, textAlign: 'center', color: '#999' }}>
              请从左侧菜单选择页面
            </div>
          )}
        </Content>
      </Layout>
    </Layout>
  );
}

export default Home;
