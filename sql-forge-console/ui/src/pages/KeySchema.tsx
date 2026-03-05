import Sql from './Sql';
import Json from './Json';
import TemplateSql from './TemplateSql';
import TemplateAmis from './TemplateAmis';
import {Schema} from 'amis-core/lib/types';
import AiAmis from './AiAmis';

export interface KeySchemaProps {
  label: string;
  schema: Schema;
}

const keySchema: Record<string, KeySchemaProps> = {
  sql: {label: 'SQL', schema: Sql},
  json: {label: 'JSON', schema: Json},
  templateSql: {label: 'SQL模板', schema: TemplateSql},
  templateAmis: {label: 'AMIS模板', schema: TemplateAmis},
  aiAmis: {label: 'AI AMIS', schema: AiAmis}
};

export default keySchema;
