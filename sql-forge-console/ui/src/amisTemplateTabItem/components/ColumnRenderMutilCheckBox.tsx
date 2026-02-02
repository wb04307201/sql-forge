import React, {useState} from 'react';
import {Checkbox} from 'antd';
import {checkBoxItems, checkBoxItemsKey, DataType} from '../../type';

const ColumnRenderMutilCheckBox = ({
  row,
  index,
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
  data,
  setData
}: {
  row: DataType;
  index: number;
  mutilCheckBoxItems?: checkBoxItemsKey[];
  data: DataType[];
  setData: (data: DataType[]) => void;
}) => {
  return (
    <div style={{display: 'flex', alignItems: 'center', gap: '2px'}}>
      {checkBoxItems.map(item => {
        return (
          <Checkbox
            checked={row[item.key]}
            onChange={e => {
              const newData = [...data];
              newData[index][item.key] = e.target.checked;
              setData(newData);
            }}
            disabled={!mutilCheckBoxItems.some(e => e === item.key)}
          />
        );
      })}
    </div>
  );
};

export default ColumnRenderMutilCheckBox;
