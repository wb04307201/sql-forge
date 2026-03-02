import React from 'react';
import {AlertComponent, ToastComponent} from 'amis';
import AmisRender from './render/AmisRender';
import keySchema from './pages/KeySchema';

function App() {
  return (
    <>
      <ToastComponent key="toast" position={'top-right'} />
      <AlertComponent key="alert" />
      <AmisRender
        schema={{
          type: 'page',
          id: 'page',
          title: 'SQL Forge',
          body: [
            {
              type: 'tabs',
              id: 'tabs',
              tabs: [
                {
                  title: keySchema.sql.label,
                  body: keySchema.sql.schema
                },
                {
                  title: keySchema.json.label,
                  body: keySchema.json.schema
                },
                {
                  title: keySchema.templateSql.label,
                  body: keySchema.templateSql.schema
                },
                {
                  title: keySchema.templateAmis.label,
                  body: keySchema.templateAmis.schema
                }
              ]
            }
          ]
        }}
      />
    </>
  );
}

export default App;
