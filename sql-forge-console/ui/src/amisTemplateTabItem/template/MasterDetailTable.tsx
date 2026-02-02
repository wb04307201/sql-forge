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
  buildMainDetailTable,
  getIndex,
  getPrimaryKey,
  isNumberJavaSqlType,
  isSysColumn
} from '../utils/CrudBuild';
import CrudTable from '../components/CrudTable';
import {v4 as uuidv4} from 'uuid';
import {getSchemas, getTable, getTables} from '../../utils/database';

const MasterDetailTable = forwardRef<
  AmisTemplateCrudMethods,
  AmisTemplateCrudProps
>((props, ref) => {
  const [database, setDatabase] = useState<DatabaseInfo>();
  const [schema, setSchema] = useState<string>();
  const [schemaOptions, setSchemaOptions] = useState<OptionType[]>([]);
  const [mainTable, setMainTable] = useState<string>();
  const [detailTable, setDetailTable] = useState<string>();
  const [tableOptions, setTableOptions] = useState<OptionType[]>([]);
  const [mainData, setMainData] = useState<DataType[]>([]);
  const [detailData, setDetailData] = useState<DataType[]>([]);
  const [mainColumn, setMainColumn] = useState<string>();
  const [detailColumn, setDetailColumn] = useState<string>();
  const [mainColumnOptions, setMainColumnOptions] = useState<OptionType[]>([]);
  const [detailColumnOptions, setDetailColumnOptions] = useState<OptionType[]>(
    []
  );
  const [dictOptions, setDictOptions] = useState();

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

    const result = await apiClient.post('sql/forge/api/json/select/SYS_DICT', {
      '@column': ['DICT_CODE', 'DICT_NAME']
    });

    setDictOptions(
      result.map(item => ({
        value: item.DICT_CODE,
        label: item.DICT_NAME
      }))
    );
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

  const onMainTableChange = (value: string) => {
    setMainTable(value);
    const tableColumn: TableColumn | undefined = getTable(
      database,
      schema,
      value
    );
    if (tableColumn) {
      setMainColumnOptions(
        tableColumn.columns.map((item: ColumnInfo) => {
          return {
            value: item.columnName,
            label: item.columnName
          };
        })
      );
    } else {
      setMainColumnOptions([]);
    }
    setMainColumn(undefined);
  };

  const onMainTableColumnChange = (value: string) => {
    setMainColumn(value);
    const tableColumn: TableColumn | undefined = getTable(
      database,
      schema,
      mainTable
    );
    if (tableColumn) {
      const columns: ColumnInfo[] = tableColumn.columns;
      const primaryKey = getPrimaryKey(tableColumn.primaryKeys);
      const uniqueIndex = getIndex(primaryKey, tableColumn.indexes);
      const data: DataType[] = columns.map(column => {
        const isPrimaryKey = primaryKey === column.columnName;
        return {
          columnName: column.columnName,
          dataType: column.dataType,
          javaSqlType: column.javaSqlType,
          typeName: column.typeName,
          columnSize: column.columnSize,
          decimalDigits: column.decimalDigits,
          remarks: column.remarks,
          key: uuidv4(),
          tableName: mainTable,
          columnType: 'origin',
          primary: isPrimaryKey,
          table: !isSysColumn(column.columnName),
          table_hidden: !isSysColumn(column.columnName) && isPrimaryKey,
          search:
            !isSysColumn(column.columnName) &&
            !isPrimaryKey &&
            !isNumberJavaSqlType(column.javaSqlType),
          check:
            !isSysColumn(column.columnName) &&
            uniqueIndex === column.columnName,
          add:
            !isSysColumn(column.columnName) &&
            !(isPrimaryKey && isNumberJavaSqlType(column.javaSqlType)),
          add_hidden:
            !isSysColumn(column.columnName) &&
            !(isPrimaryKey && isNumberJavaSqlType(column.javaSqlType)) &&
            isPrimaryKey,
          add_disabled: false,
          edit: !isSysColumn(column.columnName),
          edit_hidden: !isSysColumn(column.columnName) && isPrimaryKey,
          edit_disabled: false,
          join: undefined
        };
      });
      setMainData(data);
    } else {
      setMainData([]);
    }
  };

  const onDetailTableChange = (value: string) => {
    setDetailTable(value);
    const tableColumn: TableColumn | undefined = getTable(
      database,
      schema,
      value
    );
    if (tableColumn) {
      setDetailColumnOptions(
        tableColumn.columns.map((item: ColumnInfo) => {
          return {
            value: item.columnName,
            label: item.columnName
          };
        })
      );
    } else {
      setDetailColumnOptions([]);
    }
    setDetailColumn(undefined);
  };

  const onDetailTableColumnChange = (value: string) => {
    setDetailColumn(value);
    const tableColumn: TableColumn | undefined = getTable(
      database,
      schema,
      detailTable
    );
    if (tableColumn) {
      const columns: ColumnInfo[] = tableColumn.columns;
      const primaryKey = getPrimaryKey(tableColumn.primaryKeys);
      const uniqueIndex = getIndex(primaryKey, tableColumn.indexes, [value]);
      const data: DataType[] = columns.map(column => {
        const isPrimaryKey = primaryKey === column.columnName;
        return {
          columnName: column.columnName,
          dataType: column.dataType,
          javaSqlType: column.javaSqlType,
          typeName: column.typeName,
          columnSize: column.columnSize,
          decimalDigits: column.decimalDigits,
          remarks: column.remarks,
          key: uuidv4(),
          tableName: detailTable,
          columnType: 'origin',
          primary: isPrimaryKey,
          table: !isSysColumn(column.columnName),
          table_hidden: !isSysColumn(column.columnName) && isPrimaryKey,
          search: column.columnName === value,
          check:
            !isSysColumn(column.columnName) &&
            uniqueIndex === column.columnName,
          add:
            !isSysColumn(column.columnName) &&
            !(isPrimaryKey && isNumberJavaSqlType(column.javaSqlType)),
          add_hidden:
            !isSysColumn(column.columnName) &&
            !(isPrimaryKey && isNumberJavaSqlType(column.javaSqlType)) &&
            isPrimaryKey,
          add_disabled: column.columnName === value,
          edit: !isSysColumn(column.columnName),
          edit_hidden: !isSysColumn(column.columnName) && isPrimaryKey,
          edit_disabled: column.columnName === value,
          join: undefined
        };
      });
      setDetailData(data);
    } else {
      setDetailData([]);
    }
  };

  const getContext = () => {
    if (!mainTable || !detailTable || !mainColumn || !detailColumn) {
      Modal.error({title: '错误', content: '请先选择主子表和列'});
      return;
    }

    const primaryKey: DataType | undefined = mainData.find(
      item => item.primary
    );
    if (!primaryKey) {
      Modal.error({title: '错误', content: '主表需要一个主键'});
      return;
    }
    const detailTablePrimaryKey: DataType | undefined = detailData.find(
      item => item.primary
    );
    if (!detailTablePrimaryKey) {
      Modal.error({title: '错误', content: '子表需要一个主键'});
      return;
    }

    const context = {
      type: 'page',
      style: {
        width: '100vw',
        height: '100vh'
      },
      body: buildMainDetailTable(
        'crud_table',
        mainTable,
        mainColumn,
        mainData,
        'detail_table',
        detailTable,
        detailColumn,
        detailData
      )
    };

    return JSON.stringify(context, null, 2);
  };

  useImperativeHandle(ref, () => ({
    getContext,
    getApiTemplateId: () => `MainDetailTable-${mainTable}-${detailTable}`
  }));

  return (
    <>
      <Form>
        <Row style={{height: '33px'}}>
          <Col span={4}>
            <Form.Item>
              <Select
                placeholder="请选择schema"
                value={schema}
                onChange={onSchemaChange}
                options={schemaOptions}
              />
            </Form.Item>
          </Col>
          <Col span={5}>
            <Form.Item>
              <Select
                placeholder="请选择主表"
                value={mainTable}
                onChange={onMainTableChange}
                options={tableOptions}
              />
            </Form.Item>
          </Col>
          <Col span={5}>
            <Form.Item>
              <Select
                placeholder="请选择关联主列"
                value={mainColumn}
                onChange={onMainTableColumnChange}
                options={mainColumnOptions}
              />
            </Form.Item>
          </Col>
          <Col span={5}>
            <Form.Item>
              <Select
                placeholder="请选择子表"
                value={detailTable}
                onChange={onDetailTableChange}
                options={tableOptions}
              />
            </Form.Item>
          </Col>
          <Col span={5}>
            <Form.Item>
              <Select
                placeholder="请选择关联子列"
                value={detailColumn}
                onChange={onDetailTableColumnChange}
                options={detailColumnOptions}
              />
            </Form.Item>
          </Col>
        </Row>
      </Form>
      <Row style={{height: 'calc(50% - 50px)'}}>
        <Col span={24}>
          <CrudTable
            dataSource={mainData}
            setDataSource={setMainData}
            scroll={{y: 'calc(50vh - 165px)'}}
          />
        </Col>
      </Row>
      <Row style={{height: 'calc(50% - 50px)'}}>
        <Col span={24}>
          <CrudTable
            dataSource={detailData}
            setDataSource={setDetailData}
            scroll={{y: 'calc(50vh - 165px)'}}
          />
        </Col>
      </Row>
    </>
  );
});

export default MasterDetailTable;
