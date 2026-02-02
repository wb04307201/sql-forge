import React, {
  forwardRef,
  useEffect,
  useImperativeHandle,
  useState
} from 'react';
import type {
  AmisTemplateCrudMethods,
  AmisTemplateCrudProps,
  ColumnInfo,
  DatabaseInfo,
  DataType,
  OptionType,
  TableColumn
} from '../../type.tsx';
import apiClient from '../../apiClient.tsx';
import {Col, Form, Modal, Row, Select} from 'antd';
import {
  buildSingleTable,
  getIndex,
  getPrimaryKey,
  isNumberJavaSqlType,
  isSysColumn
} from '../utils/CrudBuild';
import CrudTable from '../components/CrudTable';
import {v4 as uuidv4} from 'uuid';
import {getSchemas, getTable, getTables} from '../../utils/database';

const SingleTable = forwardRef<AmisTemplateCrudMethods, AmisTemplateCrudProps>(
  (props, ref) => {
    const [database, setDatabase] = useState<DatabaseInfo>();
    const [schema, setSchema] = useState<string>();
    const [schemaOptions, setSchemaOptions] = useState<OptionType[]>([]);
    const [table, setTable] = useState<string>();
    const [tableOptions, setTableOptions] = useState<OptionType[]>([]);
    const [data, setData] = useState<DataType[]>([]);

    const load = async () => {
      const database: DatabaseInfo = await apiClient.get(
        '/sql/forge/api/databaseMetaData'
      );
      setDatabase(database);
      setSchemaOptions(
        getSchemas(database).map(item => ({
          value: item.schema.tableSchema,
          label: item.schema.tableSchema
        }))
      );
      setTableOptions([]);
    };

    useEffect(() => {
      load();
    }, []);

    const onSchemaChange = (value: string) => {
      setSchema(value);
      setTableOptions(
        getTables(database, value).map((item: TableColumn) => ({
          value: item.table.tableName,
          label: item.table.tableName
        }))
      );
    };

    const onTableChange = (value: string) => {
      setTable(value);
      const tableColumn: TableColumn | undefined = getTable(
        database,
        schema,
        value
      );
      if (tableColumn) {
        const columns: ColumnInfo[] = tableColumn.columns;
        const primaryKey = getPrimaryKey(tableColumn.primaryKeys);
        const uniqueIndex = getIndex(primaryKey, tableColumn.indexes);

        const data: DataType[] = columns.map(column => {
          const isPrimaryKey = primaryKey === column.columnName;
          const isUniqueIndex = uniqueIndex === column.columnName;
          return {
            columnName: column.columnName,
            dataType: column.dataType,
            javaSqlType: column.javaSqlType,
            typeName: column.typeName,
            columnSize: column.columnSize,
            decimalDigits: column.decimalDigits,
            remarks: column.remarks,
            key: uuidv4(),
            tableName: value,
            columnType: 'origin',
            primary: isPrimaryKey,
            uniqueIndex: isUniqueIndex,
            table: !isSysColumn(column.columnName),
            search:
              !isSysColumn(column.columnName) &&
              !isPrimaryKey &&
              !isNumberJavaSqlType(column.javaSqlType),
            add: !isSysColumn(column.columnName),
            add_disabled: false,
            edit: !isSysColumn(column.columnName),
            edit_disabled: false,
            join: undefined
          };
        });
        setData(data);
      } else {
        setData([]);
      }
    };

    const getContext = () => {
      if (!table) {
        Modal.error({title: '错误', content: '请先选择表'});
        return;
      }

      if (!data.find(item => item.primary)) {
        Modal.error({title: '错误', content: '需要一个主键'});
        return;
      }

      const context = {
        type: 'page',
        body: buildSingleTable('crud_table', table, data)
      };

      return JSON.stringify(context, null, 2);
    };

    useImperativeHandle(ref, () => ({
      getContext,
      getApiTemplateId: () => `SingleTable-${table}`
    }));

    return (
      <>
        <Form>
          <Row style={{height: '33px'}}>
            <Col span={12}>
              <Form.Item>
                <Select
                  placeholder="请选择schema"
                  value={schema}
                  onChange={onSchemaChange}
                  options={schemaOptions}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item>
                <Select
                  placeholder="请选择table"
                  value={table}
                  onChange={onTableChange}
                  options={tableOptions}
                />
              </Form.Item>
            </Col>
          </Row>
        </Form>
        <Row style={{height: 'calc(100% - 99px)'}}>
          <Col span={24} style={{height: '100%'}}>
            <CrudTable dataSource={data} setDataSource={setData} />
          </Col>
        </Row>
      </>
    );
  }
);

export default SingleTable;
