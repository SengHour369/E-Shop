import config from './vite.config.js';
export default { ...config, server: { host: '127.0.0.1', port: 5190, strictPort: true, proxy: { '/api': 'http://127.0.0.1:5191' } } };
