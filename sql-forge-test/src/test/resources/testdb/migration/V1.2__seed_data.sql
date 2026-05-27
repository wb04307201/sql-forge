-- 添加管理员用户
INSERT INTO users (id, username, password, enabled, category)
VALUES ('admin-001', 'admin', 'admin123', TRUE, 'admin');

-- 用户-角色关联（将 alice, bob, charlie 关联到 editor 角色）
INSERT INTO sql_forge_user_role (user_id, role_id)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'editor'),
       ('550e8400-e29b-41d4-a716-446655440001', 'editor'),
       ('550e8400-e29b-41d4-a716-446655440002', 'editor');
