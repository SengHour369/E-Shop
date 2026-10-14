export const ENV = {
    // APP_API_URL_LOCAL : import.meta.env.VITE_API_URL_LOCAL,
    APP_API_URL : import.meta.env.VITE_API_URL,
    APP_NAME : import.meta.env.VITE_APP_NAME,
    APP_VERSION : import.meta.env.VITE_APP_VERSION,
    TIMEOUT : Number(import.meta.env.VITE_TIMEOUT) || 10000,

    DEV: import.meta.env.DEV,
    PROD: import.meta.env.PROD,
};