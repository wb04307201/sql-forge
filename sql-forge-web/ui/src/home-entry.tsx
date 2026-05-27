import React from 'react';
import ReactDOM from 'react-dom';
import '@fortawesome/fontawesome-free/css/all.css';
import '@fortawesome/fontawesome-free/css/v4-shims.css';
import 'amis/lib/themes/cxd.css';
import 'amis/lib/helper.css';
import 'amis/sdk/iconfont.css';
import 'amis-editor-core/lib/style.css';
import {setDefaultTheme} from 'amis';
import {setThemeConfig} from 'amis-editor-core';
import themeConfig from 'amis-theme-editor-helper/lib/systemTheme/cxd';

setDefaultTheme('cxd');
setThemeConfig(themeConfig);

import Home from './Home';

ReactDOM.render(<Home />, document.getElementById('root'));
