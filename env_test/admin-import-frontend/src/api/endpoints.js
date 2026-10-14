export const API_ENDPOINTS = {
  AUTH: {
    VERIFY: '/api/v1/public/verify',
    RESET_PASSWORD: '/api/v1/public/reset-password',
    RESEND_CODE: '/api/v1/public/resend',
    REGISTER: '/api/v1/public/register',
    REFRESH_TOKEN: '/api/v1/public/refresh',
    LOGOUT: '/api/v1/public/logout',
    FORGOT_PASSWORD: '/api/v1/public/forgot-password',
    LOGIN: '/api/v1/public/email/username/login',
    CHECK_USER: '/api/v1/public/check-user',
  },

  SCANNER: {
    SCAN: '/api/v1/product-scanner/scan',
    RECOGNIZE: '/api/v1/product-scanner/recognize',
  },

  PRODUCTS: {
    UPDATE: '/api/v1/products/update/',
    GET_ALL: '/api/v1/products/get/all',
    DELETE: '/api/v1/products/delete',
    UPDATE_STATUS: '/api/v1/products/update/status/',
    CREATE : '/api/v1/products/create/'
  },

  CATEGORIES: {
    GET_ALL: '/api/v1/categories/get/all',
    UPDATE: '/api/v1/categories/id/update',
    UPDATE_ICON: '/api/v1/categories/id/icon',
    WITH_SUBCATEGORIES: '/api/v1/categories/with-subcategories',
    BY_NAME: '/api/v1/categories/name/',
    BY_ID: '/api/v1/categories/id/get/',
    CREATE: '/api/v1/categories/create/',
    DELETE: '/api/v1/categories/delete',
  },

  CATEGORY_ICONS: {
    UPLOAD: '/api/v1/category-icons/upload',
    BY_ID: '/api/v1/category-icons/id',
    GET_ALL: '/api/v1/category-icons/get/all',
    DELETE: '/api/v1/category-icons/delete',
  },
  
   SUB_CATEGORIES: {
    GET_ALL: '/api/v1/subcategories/get/all',
    CREATE: '/api/v1/subcategories/create/',
    UPDATE: '/api/v1/subcategories/update/',  
    DELETE: '/api/v1/subcategories/delete/',
  },
  ATTRIBUTES: {
    GET_ALL: '/api/v1/attributes',
    BY_ID: '/api/v1/attributes/{id}',
    DELETE: '/api/v1/attributes/{id}',
    BY_NAME: '/api/v1/attributes/name/{name}',
  },

  ATTRIBUTE_VALUES: {
    BY_ID: '/api/v1/attribute-values/{id}',
    UPDATE: '/api/v1/attribute-values/{id}',
    DELETE: '/api/v1/attribute-values/{id}',
    GET_BY_ATTR_AND_VALUE: '/api/v1/attribute-values',
    BY_ATTRIBUTE_ID: '/api/v1/attribute-values/attribute/{attributeId}',
  },

  INVENTORY: {
    CREATE: '/api/v1/inventory',
    BY_SKU: '/api/v1/inventory/sku/',
    BY_PRODUCT: '/api/v1/inventory/product/',
    LOW_STOCK: '/api/v1/inventory/low-stock',
    BY_ID: '/api/v1/inventory/id/',
    ADJUST: '/api/v1/inventory/exact',
    DELETE: '/api/v1/inventory/delete',
    GET_ALL: '/api/v1/inventory/all/',
    RESTOCK: '/api/v1/inventory/restock', // PATCH
    SUMMARY: '/api/v1/inventory/summary',
    HISTORY: '/api/v1/inventory/history/{inventoryId}'
  },

  ORDERS: {
  GET_ALL: '/api/v1/orders',              // GET
  GET_ALL_ALT: '/api/v1/orders/get/all',  // POST — ត្រូវសាកសួរ backend ថាតើប្រើមួយណា
  SUMMARY: '/api/v1/orders/summary',      // GET — ថ្មី
  ITEMS: '/api/v1/orders/items',          // POST — ថ្មី
  BY_NUMBER: '/api/v1/orders/number/',    // POST
  BY_ID: '/api/v1/orders/id/',            // POST (មិនមែន GET)
  BAKONG_VERIFY: '/api/v1/orders/bakong/verify',
  BAKONG_INITIATE: '/api/v1/orders/bakong/initiate',
  BAKONG_CALLBACK: '/api/v1/orders/bakong/callback',
  USER_BY_ID: '/api/v1/orders/user/id/',
  USER_HISTORY: '/api/v1/orders/user/history',
  USER_FROM_CART: '/api/v1/orders/user/from-cart',
  USER_FROM_CART_BAKONG: '/api/v1/orders/user/from-cart/bakong',
  USER_DETAIL: '/api/v1/orders/user/detail',
  USER_CANCEL: '/api/v1/orders/user/cancel',
  UPDATE_STATUS: '/api/v1/orders/status/',
  },
  PAYMENTS: {
    USER_ID :'api/v1/payments/user/id',
    USER_HISTORY: '/api/v1/payments/user/history',
    USER_DETAIL: '/api/v1/payments/user/detail',
    USER_ALL: '/api/v1/payments/user/all',
    BY_TRANSACTION: '/api/v1/payments/transaction/',
    BY_ORDER: '/api/v1/payments/order/',
    BY_ID: '/api/v1/payments/id/',
    GET_ALL: '/api/v1/payments/get/all',
  },

  TRANSACTION: {
    UPDATE_STATUS: '/api/v1/payment-transactions/{id}/status',
    CREATE: '/api/v1/payment-transactions',
    GET_ALL: '/api/v1/payment-transactions/get/all',
    GET_BY_ID: '/api/v1/payment-transactions/{id}',
    GET_STATUS_HISTORY: '/api/v1/payment-transactions/{id}/status-history',
    GET_BY_ORDER_ID: '/api/v1/payment-transactions/order/{orderId}',
    GET_BY_TRANSACTION_NO: '/api/v1/payment-transactions/no/{transactionNo}',
    GET_BY_CUSTOMER_ID: '/api/v1/payment-transactions/customer/{customerId}',
  },
  
  RETURN: {
  REJECT: '/admin/returns/{returnId}/reject',
  RECEIVE: '/admin/returns/{returnId}/receive',
  START_INSPECTION: '/admin/returns/{returnId}/inspect/start',
  COMPLETE_INSPECTION: '/admin/returns/{returnId}/inspect/complete',
  APPROVE: '/admin/returns/{returnId}/approve',

  CREATE_USER_RETURN: '/admin/returns/user',
  GET_ALL: '/admin/returns/returns',
  GET_BY_ID: '/admin/returns/{returnId}',
  GET_HISTORY: '/admin/returns/{returnId}/history',
  GET_SUMMARY: '/admin/returns/summary',
  },

  REFUND: {
   PROCESS: '/admin/refunds/{refundId}/process',
   CANCEL: '/admin/refunds/{refundId}/cancel',
   LIST: '/admin/refunds/list',
   BY_ID: '/admin/refunds/{refundId}',
   HISTORY: '/admin/refunds/{refundId}/history',
   SUMMARY: '/admin/refunds/summary'
  }
};