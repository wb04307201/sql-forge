import {
  ColumnInfo,
  DatabaseInfo,
  SchemaTableTypeTable,
  type TableColumn,
  type TableTypeTable
} from '../type';

export const getSchemas = (database: DatabaseInfo): SchemaTableTypeTable[] => {
  return database?.schemaTableTypeTables || [];
};

export const getTables = (
  database: DatabaseInfo,
  schema: string
): TableColumn[] => {
  return (
    database?.schemaTableTypeTables
      .find((item: SchemaTableTypeTable) => item.schema.tableSchema === schema)
      ?.tableTypeTables.find(
        (item: TableTypeTable) =>
          item.tableType === 'TABLE' ||
          item.tableType === 'BASE TABLE' ||
          item.tableType === 'table'
      )?.tables || []
  );
};

export const getTable = (
  database: DatabaseInfo,
  schema: string,
  table: string
): TableColumn | undefined => {
  return (
    database?.schemaTableTypeTables
      .find((item: SchemaTableTypeTable) => item.schema.tableSchema === schema)
      ?.tableTypeTables.find(
        (item: TableTypeTable) =>
          item.tableType === 'TABLE' ||
          item.tableType === 'BASE TABLE' ||
          item.tableType === 'table'
      )
      ?.tables.find((item: TableColumn) => item.table.tableName === table)
  );
};

export const getColumn = (
  database: DatabaseInfo,
  schema: string,
  table: string,
  column: string
): ColumnInfo | undefined => {
  return database?.schemaTableTypeTables
    .find((item: SchemaTableTypeTable) => item.schema.tableSchema === schema)
    ?.tableTypeTables.find(
      (item: TableTypeTable) =>
        item.tableType === 'TABLE' ||
        item.tableType === 'BASE TABLE' ||
        item.tableType === 'table'
    )
    ?.tables.find((item: TableColumn) => item.table.tableName === table)
    ?.columns.find((item: ColumnInfo) => item.columnName === column);
};
