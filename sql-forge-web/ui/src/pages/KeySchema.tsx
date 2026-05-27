import Sql from './Sql';
import Json from './Json';
import TemplateSql from './TemplateSql';
import TemplateAmis from './TemplateAmis';
import UserManagement from './UserManagement';
import RoleManagement from './RoleManagement';
import {Schema} from 'amis-core/lib/types';

export interface KeySchemaProps {
  label: string;
  schema: Schema;
}

const keySchema: Record<string, KeySchemaProps> = {
  sql: {label: 'SQL', schema: Sql},
  json: {label: 'JSON', schema: Json},
  templateSql: {label: 'SQL模板', schema: TemplateSql},
  templateAmis: {label: 'AMIS模板', schema: TemplateAmis},
  userManagement: {label: '用户管理', schema: UserManagement},
  roleManagement: {label: '角色管理', schema: RoleManagement}
};

export default keySchema;
