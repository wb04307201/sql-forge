import React, {useState} from 'react';
import {Button, Col, Form, Modal, Row, Select} from 'antd';
import {
  ColumnInfo,
  type DatabaseInfo,
  DataType,
  DictJoinInfo,
  JoinInfo,
  type OptionType,
  type SchemaTableTypeTable,
  type TableColumn,
  TableJoinInfo,
  type TableTypeTable
} from '../../type';
import {SettingOutlined} from '@ant-design/icons';
import apiClient from '../../apiClient';
import {
  SYS_DICT,
  DICT_CODE,
  DICT_NAME,
  generateRandomAlias,
  isSysColumn,
  isNumberJavaSqlType
} from '../utils/CrudBuild';
import {v4 as uuidv4} from 'uuid';
import {getColumn, getTable} from '../../utils/database';

const ColumnRenderJoin = (props: {
  value: JoinInfo;
  row: DataType;
  data: any[];
  setData: (data: any[]) => void;
}) => {
  const [show, setShow] = useState(false);
  const [joinInfo, setJoinInfo] = useState<JoinInfo>(props.value);
  const [dictOptions, setDictOptions] = useState();
  const [database, setDatabase] = useState<DatabaseInfo>();
  const [schemaOptions, setSchemaOptions] = useState<OptionType[]>([]);
  const [tableOptions, setTableOptions] = useState<OptionType[]>([]);
  const [columnOptions, setColumnOptions] = useState<OptionType[]>([]);

  const renderTitle = () => {
    return (
      <>
        {props.value?.joinType === 'dict' && <div>字典:{joinInfo?.dict}</div>}
        {props.value?.joinType === 'table' && <div>表:{joinInfo?.table}</div>}
      </>
    );
  };

  const loadDictOptions = async () => {
    const data = await apiClient.post(`sql/forge/api/json/select/${SYS_DICT}`, {
      '@column': [`${DICT_CODE}`, `${DICT_NAME}`]
    });

    setDictOptions(
      data.map(item => ({
        value: item[DICT_CODE.toLowerCase()] || item[DICT_CODE.toUpperCase()],
        label: item[DICT_NAME.toLowerCase()] || item[DICT_NAME.toUpperCase()]
      }))
    );
  };

  const loadDatasource = async () => {
    const database: DatabaseInfo = await apiClient.get(
      '/sql/forge/api/databaseMetaData'
    );
    setDatabase(database);
    setSchemaOptions(
      database?.schemaTableTypeTables.map(item => ({
        value: item.schema.tableSchema,
        label: item.schema.tableSchema
      }))
    );
    setTableOptions([]);
  };

  const onSchemaChange = (value: string) => {
    setJoinInfo({
      ...joinInfo,
      schema: value,
      table: undefined,
      onColumn: undefined,
      selectColumn: undefined
    } as TableJoinInfo);
    setTableOptions(
      database?.schemaTableTypeTables
        .find((item: SchemaTableTypeTable) => item.schema.tableSchema === value)
        ?.tableTypeTables.find(
          (item: TableTypeTable) =>
            item.tableType === 'TABLE' ||
            item.tableType === 'BASE TABLE' ||
            item.tableType === 'table'
        )
        ?.tables.map((item: TableColumn) => ({
          value: item.table.tableName,
          label: item.table.tableName
        })) || []
    );
  };

  const onTableChange = (value: string) => {
    setJoinInfo({
      ...joinInfo,
      table: value,
      onColumn: undefined,
      selectColumn: undefined
    } as TableJoinInfo);
    const tableColumn: TableColumn | undefined = database?.schemaTableTypeTables
      .find(
        (item: SchemaTableTypeTable) =>
          item.schema.tableSchema === joinInfo?.schema
      )
      ?.tableTypeTables.find(
        (item: TableTypeTable) =>
          item.tableType === 'TABLE' ||
          item.tableType === 'BASE TABLE' ||
          item.tableType === 'table'
      )
      ?.tables.find((item: TableColumn) => item.table.tableName === value);
    if (tableColumn) {
      setColumnOptions(
        tableColumn.columns.map((item: ColumnInfo) => {
          return {
            value: item.columnName,
            label: item.columnName
          };
        })
      );
    } else {
      setColumnOptions([]);
    }
  };

  const gettableName = (joinInfo: JoinInfo): string => {
    if (joinInfo?.joinType === 'dict') {
      return generateRandomAlias(joinInfo?.dict);
    } else if (joinInfo?.joinType === 'table') {
      return generateRandomAlias(joinInfo?.table);
    }
  };

  const onOk = () => {
    if (
      (joinInfo?.joinType === 'dict' && joinInfo.dict) ||
      (joinInfo?.joinType === 'table' &&
        joinInfo.table &&
        joinInfo.onColumn &&
        joinInfo.selectColumn)
    ) {
      let newData = [...props.data];
      const originJoin = props.row.join;
      if (originJoin?.joinType === 'table') {
        newData = newData.filter(item => item.tableName !== originJoin.alias);
      }
      const newInfoInfo = {
        ...joinInfo,
        alias: gettableName(joinInfo)
      };
      const index = newData.findIndex(item => item.key === props.row.key);
      newData[index].join = newInfoInfo;
      if (
        newInfoInfo?.joinType === 'table' &&
        newInfoInfo.extraSelectColumns &&
        newInfoInfo.extraSelectColumns.length > 0
      ) {
        const tableColumn: TableColumn | undefined = getTable(
          database,
          newInfoInfo.schema,
          newInfoInfo.table
        );
        newInfoInfo.extraSelectColumns.forEach(item => {
          const column = getColumn(
            database,
            newInfoInfo.schema,
            newInfoInfo.table,
            item
          );
          newData.push({
            columnName: item,
            dataType: column?.dataType,
            javaSqlType: column?.javaSqlType,
            typeName: column?.typeName,
            columnSize: column?.columnSize,
            decimalDigits: column?.decimalDigits,
            remarks: column?.remarks,
            key: uuidv4(),
            tableName: newInfoInfo.alias,
            columnType: 'join',
            primary: false,
            table: true,
            table_hidden: false,
            search: false,
            check: false,
            add: true,
            add_hidden: false,
            add_disabled: true,
            edit: true,
            edit_hidden: false,
            edit_disabled: true
          });
        });
      }
      props.setData(newData);
      setShow(false);
    }
  };

  return (
    <div style={{display: 'flex'}}>
      <Button
        shape="circle"
        icon={<SettingOutlined />}
        size="small"
        onClick={() => {
          setShow(true);
        }}
      />
      {renderTitle()}
      <Modal
        title="关联信息"
        open={show}
        cancelText={'取消'}
        okText={'确定'}
        onCancel={() => setShow(false)}
        onOk={onOk}
      >
        <Form>
          <Row>
            <Col span={24}>
              <Form.Item>
                <Select
                  value={joinInfo?.joinType}
                  options={[
                    {value: 'dict', label: '字典'},
                    {value: 'table', label: '表'}
                  ]}
                  onChange={async value => {
                    if (value === 'dict') {
                      setJoinInfo({joinType: 'dict'} as DictJoinInfo);
                      await loadDictOptions();
                    } else if (value === 'table') {
                      setJoinInfo({joinType: 'table'} as TableJoinInfo);
                      await loadDatasource();
                    } else {
                      setJoinInfo(undefined);
                    }
                  }}
                  allowClear={true}
                />
              </Form.Item>
            </Col>
          </Row>
          {joinInfo?.joinType === 'dict' && (
            <Row>
              <Col span={24}>
                <Form.Item>
                  <Select
                    placeholder="请选择字典"
                    value={joinInfo?.dict}
                    onChange={value => {
                      setJoinInfo({...joinInfo, dict: value});
                    }}
                    options={dictOptions}
                  />
                </Form.Item>
              </Col>
            </Row>
          )}
          {joinInfo?.joinType === 'table' && (
            <>
              <Row>
                <Col span={12}>
                  <Form.Item>
                    <Select
                      placeholder="请选择schema"
                      value={joinInfo?.schema}
                      onChange={onSchemaChange}
                      options={schemaOptions}
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item>
                    <Select
                      placeholder="请选择表"
                      value={joinInfo?.table}
                      onChange={onTableChange}
                      options={tableOptions}
                    />
                  </Form.Item>
                </Col>
              </Row>
              <Row>
                <Col span={12}>
                  <Form.Item>
                    <Select
                      placeholder="请选择关联列"
                      value={joinInfo?.onColumn}
                      onChange={value =>
                        setJoinInfo({...joinInfo, onColumn: value})
                      }
                      options={columnOptions}
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item>
                    <Select
                      placeholder="请选择显示列"
                      value={joinInfo?.selectColumn}
                      onChange={value =>
                        setJoinInfo({...joinInfo, selectColumn: value})
                      }
                      options={columnOptions}
                    />
                  </Form.Item>
                </Col>
              </Row>
              <Row>
                <Col span={24}>
                  <Form.Item>
                    <Select
                      placeholder="请选择额外显示列"
                      value={joinInfo?.extraSelectColumns}
                      onChange={value =>
                        setJoinInfo({...joinInfo, extraSelectColumns: value})
                      }
                      options={columnOptions.filter(
                        item => item.value != joinInfo?.selectColumn
                      )}
                      mode="multiple"
                    />
                  </Form.Item>
                </Col>
              </Row>
            </>
          )}
        </Form>
      </Modal>
    </div>
  );
};

export default ColumnRenderJoin;
