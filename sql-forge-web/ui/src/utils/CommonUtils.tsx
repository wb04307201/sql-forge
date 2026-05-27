export const buildTableData = (data: any[] | {rows: any[]} | any) => {
  let rows: any;
  if (data instanceof Array) {
    rows = data;
  } else if (data instanceof Object && data.rows) {
    rows = data.rows;
  }
  if (rows) {
    const row = rows[0];
    const columns = [];
    for (const key in row) {
      columns.push({
        label: key,
        name: key
      });
    }

    return {columns: columns, rows: rows};
  } else if (data instanceof Object) {
    const columns = [];
    for (const key in data) {
      columns.push({
        label: key,
        name: key
      });
    }
    rows = [data];
    return {columns: columns, rows: rows};
  } else {
    return {
      columns: [{
        label: '影响行数',
        name: 'count'
      }],
      rows: [{count: data}]
    };
  }
};
