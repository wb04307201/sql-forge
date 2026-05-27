-- 角色表 sql_forge_role
CREATE TABLE sql_forge_role
(
    id          VARCHAR(64) NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(200)
);

COMMENT ON TABLE sql_forge_role IS '角色表';
COMMENT ON COLUMN sql_forge_role.id IS '角色ID';
COMMENT ON COLUMN sql_forge_role.name IS '角色名称';
COMMENT ON COLUMN sql_forge_role.description IS '角色描述';

-- 用户-角色关联表 sql_forge_user_role
CREATE TABLE sql_forge_user_role
(
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sql_forge_user_role IS '用户-角色关联表';
COMMENT ON COLUMN sql_forge_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sql_forge_user_role.role_id IS '角色ID';

-- 角色-模板关联表 sql_forge_role_template
CREATE TABLE sql_forge_role_template
(
    role_id     VARCHAR(64) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_id, template_id)
);

COMMENT ON TABLE sql_forge_role_template IS '角色-模板关联表';
COMMENT ON COLUMN sql_forge_role_template.role_id IS '角色ID';
COMMENT ON COLUMN sql_forge_role_template.template_id IS '模板ID';
