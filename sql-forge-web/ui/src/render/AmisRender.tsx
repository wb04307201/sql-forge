import {render, RenderOptions} from 'amis';
import axios from "axios";
import copy from "copy-to-clipboard";
import {fetcherResult, fetchOptions, Schema} from "amis-core/lib/types";
import {RootRenderProps} from "amis-core/lib/Root";
import {alert, confirm, toast} from 'amis-ui';
import keySchema from '../pages/KeySchema';

interface AMISComponentProps {
  schema: Schema;
  props?: RootRenderProps;
  options?: RenderOptions;
}

function AmisRender(props: AMISComponentProps) {
    return render(props.schema, props.props, props.options);
}

AmisRender.defaultProps = {
    page: {},
    props: {},
    // amis 环境配置
    options: {
        // 下面三个接口必须实现
        fetcher: (options: fetchOptions): Promise<fetcherResult> => {
          console.log('options', options)

            let {
              url,
              method,
              data,
              responseType,
              config,
              headers
            } = options;

            let axiosConfig = config || {};
            axiosConfig.withCredentials = true;
            responseType && (axiosConfig.responseType = responseType);

            if (axiosConfig.cancelExecutor) {
                axiosConfig.cancelToken = new (axios as any).CancelToken(
                    axiosConfig.cancelExecutor
                );
            }

            axiosConfig.headers = headers || {};

            const httpMethod = method?.toLowerCase() || 'get';

            if (httpMethod !== 'post' && httpMethod !== 'put' && httpMethod !== 'patch') {
                if (data) {
                    axiosConfig.params = data;
                }
                return (axios as any)[httpMethod](url, axiosConfig).catch((error: any) => {
                    if (error.response && error.response.status === 401) {
                        window.location.href = '/sql/forge/web/login.html';
                    }
                    throw error;
                });
            } else if (data && data instanceof FormData) {
                axiosConfig.headers = axiosConfig.headers || {};
                axiosConfig.headers['Content-Type'] = 'multipart/form-data';
            } else if (
                data &&
                typeof data !== 'string' &&
                !(data instanceof Blob) &&
                !(data instanceof ArrayBuffer)
            ) {
                data = JSON.stringify(data);
                axiosConfig.headers = axiosConfig.headers || {};
                axiosConfig.headers['Content-Type'] = 'application/json';
            }

            return (axios as any)[httpMethod](url, data, axiosConfig).catch((error: any) => {
                if (error.response && error.response.status === 401) {
                    window.location.href = '/sql/forge/web/login.html';
                }
                throw error;
            });
        },
        isCancel: (value: any) => (axios as any).isCancel(value),
        copy: (content: string) => {
            copy(content);
            toast.success('内容已复制到粘贴板');
        },
        notify: (
          type: 'error' | 'success' /**/,
          msg: string /*提示内容*/
        ) => {
          toast[type]
            ? toast[type](msg, type === 'error' ? '系统错误' : '系统消息')
            : console.warn('[Notify]', type, msg);
        },
        alert,
        confirm,
        updateLocation: (to: string, replace?: boolean) => {
          // Use history API to avoid full page reload
          if (to.startsWith('?') || !to.startsWith('http')) {
            const url = new URL(window.location.origin + window.location.pathname + to);
            if (replace) {
              window.history.replaceState({}, '', url.toString());
            } else {
              window.history.pushState({}, '', url.toString());
            }
          } else {
            if (replace) {
              window.location.replace(to);
            } else {
              window.location.href = to;
            }
          }
        },
        theme: 'cxd' // cxd 或 antd
    }
};

export default AmisRender
