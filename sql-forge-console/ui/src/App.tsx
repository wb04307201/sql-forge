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
              type: 'service',
              schemaApi: {
                url: '/sql/forge/console/api/state',
                method: 'get',
                adaptor: function (payload, response, api, context) {
                  let tabs: any[] = [];
                  if (payload.database.enabled){
                    tabs.push({
                      title: keySchema.sql.label,
                      body: keySchema.sql.schema
                    });
                  }
                  if (payload.json.enabled) {
                    tabs.push({
                      title: keySchema.json.label,
                      body: keySchema.json.schema
                    });
                  }
                  if (payload.template.sql.enabled) {
                    tabs.push({
                      title: keySchema.templateSql.label,
                      body: keySchema.templateSql.schema
                    });
                  }
                  if (payload.template.amis.enabled) {
                    tabs.push({
                      title: keySchema.templateAmis.label,
                      body: keySchema.templateAmis.schema
                    });
                  }

                  tabs.push({
                    title: keySchema.aiAmis.label,
                    body: keySchema.aiAmis.schema
                  })

                  return {
                    data: {
                      type: 'tabs',
                      id: 'tabs',
                      tabs: [...tabs]
                    }
                  };
                }
              }
            }
          ]
        }}
      />
    </>
  );
}

export default App;
