// src/config/navigation.js
// Canonical navigation configuration for the admin sidebar.
// Sidebar imports NAV_ITEMS and NAV_GROUPS from utils/constants (backward compat)
// but this file is the intended long-term home.

export const NAV_ITEMS = [
  { path: '/dashboard', label: 'Dashboard', icon: 'dashboard', group: 'overview' },
  { path: '/dashboard/ai-assistant', label: 'AI Assistant', icon: 'assistant', group: 'ai' },
  { path: '/dashboard/import-approvals', label: 'Import Approvals', icon: 'approvals', group: 'ai' },
  { path: '/dashboard/import-executions', label: 'Tool Executions', icon: 'executions', group: 'ai' },
  { path: '/dashboard/orders', label: 'Orders', icon: 'orders', group: 'orders' },
  { path: '/dashboard/returns', label: 'Returns', icon: 'returns', group: 'orders' },
  // { path: '/dashboard/cancellations', label: 'Cancellations', icon: 'cancellations', group: 'orders' },
  { path: '/dashboard/payments', label: 'Payment Details', icon: 'payments', group: 'payments' },
  { path: '/dashboard/refunds', label: 'Refunds', icon: 'refunds', group: 'payments' },
  { path: '/dashboard/transactions', label: 'Transactions', icon: 'transactions', group: 'payments' },
  { path: '/dashboard/customers', label: 'Customers', icon: 'customers', group: 'customers' },
  { path: '/dashboard/customer-groups', label: 'Customer Groups', icon: 'customerGroups', group: 'customers' },
  { path: '/dashboard/products', label: 'Products', icon: 'products', group: 'products' },
  { path: '/dashboard/scanner', label: 'Product Scanner', icon: 'scanner', group: 'products' },
  { path: '/dashboard/categories', label: 'Categories', icon: 'categories', group: 'products' },
  { path: '/dashboard/brands', label: 'Brands', icon: 'brands', group: 'products' },
  { path: '/dashboard/inventory', label: 'Inventory', icon: 'inventory', group: 'products' },
  { path: '/dashboard/sales-report', label: 'Sales Report', icon: 'reports', group: 'reports' },
  { path: '/dashboard/payment-report', label: 'Payment Report', icon: 'reports', group: 'reports' },
  { path: '/dashboard/tax-report', label: 'Tax Report', icon: 'reports', group: 'reports' },
  { path: '/dashboard/settings', label: 'Settings', icon: 'settings', group: 'settings' },
  { path: '/dashboard/users', label: 'Users & Roles', icon: 'users', group: 'settings' },
];

export const NAV_GROUPS = [
  { key: 'overview', label: 'OVERVIEW' },
  { key: 'ai', label: 'AI WORKSPACE' },
  { key: 'orders', label: 'ORDERS' },
  { key: 'payments', label: 'PAYMENTS' },
  { key: 'customers', label: 'CUSTOMERS' },
  { key: 'products', label: 'PRODUCTS' },
  { key: 'reports', label: 'REPORTS' },
  { key: 'settings', label: 'SETTINGS' },
];

export default NAV_ITEMS;
