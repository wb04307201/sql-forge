import {Button, Table, type TableProps} from 'antd';
import React from 'react';
import {
  checkBoxItems,
  checkBoxItemsKey,
  DataType,
  type JoinInfo
} from '../../type';
import {CalculatorOutlined} from '@ant-design/icons';
import ColumnRenderInput from './ColumnRenderInput';
import ColumnRenderMutilCheckBox from './ColumnRenderMutilCheckBox';
import ColumnRenderJoin from './ColumnRenderJoin';

const CrudTable = ({
  mutilCheckBoxItems = [
    'primary',
    'uniqueIndex',
    'table',
    'search',
    'add',
    'add_disabled',
    'edit',
    'edit_disabled'
  ],
  dataSource = [],
  setDataSource,
  scroll = {y: 'calc(100vh - 270px)'}
}: {
  mutilCheckBoxItems?: checkBoxItemsKey[];
  dataSource: DataType[];
  setDataSource: (dataTypes: DataType[]) => void;
  scroll?: {y: string};
}) => {
  const columns: TableProps<DataType>['columns'] = [
    {
      title: (
        <>
          列名
          <Button shape={'circle'} icon={<CalculatorOutlined />} size="small" />
        </>
      ),
      dataIndex: `columnName`
    },
    {
      title: '类型',
      dataIndex: 'columnType',
      render: (_, row) => {
        return `${
          row.javaSqlType
            ? row.javaSqlType +
              (row.columnSize
                ? '(' +
                  row.columnSize +
                  (row.decimalDigits ? ',' + row.decimalDigits : '') +
                  ')'
                : '')
            : ''
        }`;
      }
    },
    {
      title: '备注',
      dataIndex: 'remarks',
      render: (value, _, index: number) => {
        return (
          <ColumnRenderInput
            value={value}
            index={index}
            dataIndex={'remarks'}
            data={dataSource}
            setData={setDataSource}
          />
        );
      }
    },
    {
      title: checkBoxItems.map(item => item.slabel).join('|'),
      dataIndex: 'mutilCheckBox',
      render: (_, row: DataType, index: number) => {
        return (
          <ColumnRenderMutilCheckBox
            row={row}
            mutilCheckBoxItems={mutilCheckBoxItems}
            index={index}
            data={dataSource}
            setData={setDataSource}
          />
        );
      }
    },
    {
      title: '关联',
      dataIndex: 'join',
      render: (value: JoinInfo, row: DataType) => {
        return (
          <ColumnRenderJoin
            value={value}
            row={row}
            data={dataSource}
            setData={setDataSource}
          />
        );
      }
    }
  ];

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      pagination={false}
      scroll={scroll}
    />
  );
};

export default CrudTable;
