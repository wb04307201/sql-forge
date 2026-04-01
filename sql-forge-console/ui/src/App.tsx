import React, {useEffect, useState} from 'react';
import {AlertComponent, ToastComponent} from 'amis';
import AmisRender from './render/AmisRender';
import keySchema from './pages/KeySchema';
import {Schema} from 'amis-core/lib/types';
import axios from 'axios';

function App() {
  const [viewId, setViewId] = useState<string>();
  const [view, setView] = useState<Schema>();
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const hasId = searchParams.has('id');

    if (hasId) {
      const id = searchParams.get('id');
      if (id) {
        setLoading(true);
        setViewId(id);
        axios.get(`sql/forge/api/template/amis/${id}`).then(res => {
          setView(JSON.parse(res.data.context));
          setLoading(false);
        });
      }
    }
  }, []);

  return (
    <>
      <ToastComponent key="toast" position={'top-right'} />
      <AlertComponent key="alert" />
      <AmisRender
        schema={
          loading
            ? {
                type: 'page',
                title: '加载中...',
                body: [
                  {
                    type: 'spinner',
                    show: true
                  }
                ]
              }
            : view
            ? view
            : {
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
              }
        }
      />
    </>
  );
}

export default App;
