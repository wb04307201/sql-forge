import React from 'react';
import ReactDOM from 'react-dom';
import APP from './App';
import '@fortawesome/fontawesome-free/css/all.css';
import '@fortawesome/fontawesome-free/css/v4-shims.css';
import 'amis/lib/themes/cxd.css';
import 'amis/lib/helper.css';
import 'amis/sdk/iconfont.css';
// 或 import 'amis/lib/themes/antd.css';
import 'amis-editor-core/lib/style.css';
import {setDefaultTheme} from 'amis';
import {setThemeConfig} from 'amis-editor-core';
import themeConfig from 'amis-theme-editor-helper/lib/systemTheme/cxd';

setDefaultTheme('cxd');
setThemeConfig(themeConfig);

ReactDOM.render(<APP />, document.getElementById('root'));
