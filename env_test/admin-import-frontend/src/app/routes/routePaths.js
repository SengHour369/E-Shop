// src/app/routes/routePaths.js
// Centralised route-path constants — import these everywhere instead of
// hard-coding strings, so a path change only needs updating here.

export const PATHS = {
  ROOT: '/',
  LOGIN: '/login',

  DASHBOARD: '/dashboard',
  AI_ASSISTANT: '/dashboard/ai-assistant',
  IMPORT_APPROVALS: '/dashboard/import-approvals',
  IMPORT_EXECUTIONS: '/dashboard/import-executions',

  // Orders
  ORDERS: '/dashboard/orders',
  ORDER_DETAIL: '/dashboard/orders/:id',
  RETURNS: '/dashboard/returns',
  CANCELLATIONS: '/dashboard/cancellations',

  // Payments
  PAYMENTS: '/dashboard/payments',
  BAKONG_CALLBACK: '/dashboard/payments/bakong-callback/:id',
  REFUNDS: '/dashboard/refunds',
  TRANSACTIONS: '/dashboard/transactions',

  // Customers
  CUSTOMERS: '/dashboard/customers',
  CUSTOMER_DETAIL: '/dashboard/customers/:id',
  CUSTOMER_GROUPS: '/dashboard/customer-groups',
  CUSTOMER_GROUPS_ADD: '/dashboard/customer-groups/add',
  CUSTOMER_GROUPS_EDIT: '/dashboard/customer-groups/edit/:id',

  // Products
  SCANNER: '/dashboard/scanner',
  PRODUCTS: '/dashboard/products',
  PRODUCTS_ADD: '/dashboard/products/add',
  PRODUCTS_VIEW: '/dashboard/products/view/:id',
  PRODUCTS_EDIT: '/dashboard/products/edit/:id',
  CATEGORIES: '/dashboard/categories',
  CATEGORIES_ADD: '/dashboard/categories/add',
  CATEGORIES_EDIT: '/dashboard/categories/edit/:id',
  BRANDS: '/dashboard/brands',
  BRANDS_ADD: '/dashboard/brands/add',
  BRANDS_EDIT: '/dashboard/brands/edit/:id',

  // Reports
  SALES_REPORT: '/dashboard/sales-report',
  PAYMENT_REPORT: '/dashboard/payment-report',
  TAX_REPORT: '/dashboard/tax-report',

  // Settings
  SETTINGS: '/dashboard/settings',
  USERS: '/dashboard/users',

  // Misc
  NOT_FOUND: '*',
};

export default PATHS;
