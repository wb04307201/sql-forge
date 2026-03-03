-- SQL模板表
CREATE TABLE sql_forge_template_sql
(
    id            VARCHAR(64) NOT NULL PRIMARY KEY,
    executor_name VARCHAR(50) NOT NULL,
    context       TEXT        NOT NULL
);

COMMENT
ON TABLE sql_forge_template_sql IS 'SQL模板表';
COMMENT
ON COLUMN sql_forge_template_sql.id IS 'SQL模板ID';
COMMENT
ON COLUMN sql_forge_template_sql.executor_name IS '数据源';
COMMENT
ON COLUMN sql_forge_template_sql.context IS '模板内容';

-- AMIS模板表
CREATE TABLE sql_forge_template_amis
(
    id      VARCHAR(64) NOT NULL PRIMARY KEY,
    context TEXT        NOT NULL
);

COMMENT
ON TABLE sql_forge_template_amis IS 'AMIS模板表';
COMMENT
ON COLUMN sql_forge_template_amis.id IS 'AMIS模板ID';
COMMENT
ON COLUMN sql_forge_template_amis.context IS '模板内容';

