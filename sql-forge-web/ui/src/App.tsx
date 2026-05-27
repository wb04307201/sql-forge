import React, { useState, useEffect } from 'react';
import { Layout, Menu, Button, message } from 'antd';
import {
  LogoutOutlined, HomeOutlined,
  DatabaseOutlined, CodeOutlined, FileTextOutlined,
  BgColorsOutlined, UserOutlined, SafetyOutlined
} from '@ant-design/icons';
import axios from 'axios';
import AmisRender from './render/AmisRender';
import keySchema from './pages/KeySchema';

const { Sider, Content, Header } = Layout;

interface AuthState {
  loggedIn: boolean;
  username: string | null;
  roles: string[];
  category: string | null;
}

const consoleMenuItems = [
  { key: 'sql', label: 'SQL', icon: <DatabaseOutlined />, schema: keySchema.sql.schema },
  { key: 'json', label: 'JSON', icon: <CodeOutlined />, schema: keySchema.json.schema },
  { key: 'templateSql', label: 'SQL模板', icon: <FileTextOutlined />, schema: keySchema.templateSql.schema },
  { key: 'templateAmis', label: 'AMIS模板', icon: <BgColorsOutlined />, schema: keySchema.templateAmis.schema },
  { key: 'userManagement', label: '用户管理', icon: <UserOutlined />, schema: keySchema.userManagement.schema },
  { key: 'roleManagement', label: '角色管理', icon: <SafetyOutlined />, schema: keySchema.roleManagement.schema },
];

function App() {
  const [auth, setAuth] = useState<AuthState>({ loggedIn: false, username: null, roles: [], category: null });
  const [loading, setLoading] = useState(true);
  const [selectedKey, setSelectedKey] = useState('sql');
  const [currentSchema, setCurrentSchema] = useState(keySchema.sql.schema);
  const [viewSchema, setViewSchema] = useState<any>(null);

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const id = searchParams.get('id');
    if (id) {
      axios.get(`/sql/forge/api/template/amis/${id}`, { withCredentials: true })
        .then(res => setViewSchema(JSON.parse(res.data.context)))
        .catch(() => {});
    }

    axios.get('/sql/forge/api/auth/status', { withCredentials: true })
      .then(res => {
        const data = res.data;
        if (data.success && data.data.loggedIn) {
          setAuth({ loggedIn: true, username: data.data.username, roles: data.data.roles || [], category: data.data.category || null });
        } else {
          setAuth({ loggedIn: false, username: null, roles: [], category: null });
        }
        setLoading(false);
      })
      .catch(() => {
        setAuth({ loggedIn: false, username: null, roles: [], category: null });
        setLoading(false);
      });
  }, []);

  const handleMenuClick = (info: { key: string }) => {
    setSelectedKey(info.key);
    const item = consoleMenuItems.find(m => m.key === info.key);
    if (item) setCurrentSchema(item.schema);
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
    return <div style={{ padding: 20, textAlign: 'center' }}>加载中...</div>;
  }

  // Not logged in -> redirect to login page
  if (!auth.loggedIn) {
    window.location.href = '/sql/forge/web/login.html';
    return null;
  }

  // Non-admin -> access denied
  if (auth.category !== 'admin') {
    return (
      <div style={{ padding: 40, textAlign: 'center' }}>
        <h2>无权访问</h2>
        <p>控制台仅允许管理员访问</p>
        <Button type="primary" onClick={() => window.location.href = '/sql/forge/web/home.html'}>
          返回首页
        </Button>
      </div>
    );
  }

  // If ?id= view, render directly
  if (viewSchema) {
    return <AmisRender schema={viewSchema} />;
  }

  // Admin console with sidebar layout
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 16px' }}>
        <span style={{ fontSize: 18, fontWeight: 'bold', color: '#fff' }}>SQL Forge 控制台</span>
        <div>
          <Button type="link" style={{ color: '#fff' }} icon={<HomeOutlined />}
            onClick={() => window.location.href = '/sql/forge/web/home.html'}>
            应用页
          </Button>
          <Button type="link" style={{ color: '#fff' }} icon={<LogoutOutlined />} onClick={handleLogout}>
            退出
          </Button>
        </div>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff', overflow: 'auto', height: 'calc(100vh - 64px)' }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            style={{ height: '100%', borderRight: 0 }}
            items={consoleMenuItems.map(item => ({
              key: item.key,
              label: item.label,
              icon: item.icon,
            }))}
            onClick={handleMenuClick}
          />
        </Sider>
        <Content style={{ padding: 16, overflow: 'auto' }}>
          <AmisRender schema={currentSchema} />
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;
