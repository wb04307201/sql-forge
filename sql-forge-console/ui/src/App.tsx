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
          data: {
            tabs: [
              // {
              //   label: 'SQL',
              //   value: 'sql'
              // },
              // {
              //   label: 'JSON',
              //   value: 'json'
              // },
              {
                label: 'SQL模板',
                value: 'templateSql'
              },
              {
                label: 'AMIS模板',
                value: 'templateAmis'
              }
            ]
          },
          aside: [
            {
              type: 'nav',
              links: [
                {
                  label: 'SQL'
                }
              ],
              onEvent: {
                click: {
                  actions: [
                    // {
                    //   actionType: 'setValue',
                    //   componentId: 'page',
                    //   args: {
                    //     value: {
                    //       tabs: "${concat(tabs, [{label: event.data.item.label, value: 'sql'}])}"
                    //     }
                    //   }
                    // },
                    {
                      actionType: 'setValue',
                      componentId: 'page',
                      args: {
                        value: {
                          tabs: [
                            {
                              label: 'SQL',
                              value: 'sql'
                            },
                            {
                              label: 'JSON',
                              value: 'json'
                            },
                            {
                              label: 'SQL模板',
                              value: 'templateSql'
                            },
                            {
                              label: 'AMIS模板',
                              value: 'templateAmis'
                            }
                          ]
                        }
                      }
                    }
                  ]
                }
              }
            }
          ],
          body: [
            {
              type: 'tabs',
              id: 'tabs',
              source: '${tabs}',
              tabs: [
                {
                  title: '${label}',
                  body: [
                    {
                      type: 'service',
                      schemaApi: {
                        url: '${value}',
                        method: 'json'
                      }
                    }
                  ]
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
