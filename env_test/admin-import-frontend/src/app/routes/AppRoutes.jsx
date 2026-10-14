import { Routes, Route } from 'react-router-dom';
import AdminLayout from '../../components/layouts/AdminLayout';

// Pages
import DashboardPage from '@/features/dashboard/pages/DashboardPage';
import OrdersPage from '@/features/orders/pages/OrdersPage';
import OrderDetailPage from '@/features/orders/pages/OrderDetailPage';
import ReturnsPage from '@/features/returns/pages/ReturnsPage';
import ReturnDetailsPage from '@/features/returns/pages/ReturnDetailsPage';
import CancellationsPage from '@/features/cancellations/pages/CancellationsPage';
import PaymentsPage from '@/features/payments/pages/PaymentsPage';
import PaymentDetailPage from '@/features/payments/pages/PaymentDetailPage';
import BakongCallbackPage from '@/features/payments/pages/BakongCallbackPage';
import RefundsPage from '@/features/refunds/pages/RefundsPage';
import ReportsPage from '@/features/reports/pages/ReportsPage';
import TransactionsPage from '@/features/payments/pages/TransactionsPage';
import TransactionDetailPage from '@/features/payments/pages/TransactionDetailPage';
import CustomersPage from '@/features/customers/pages/CustomersPage';
import CustomerDetailPage from '@/features/customers/pages/CustomerDetailPage';
import CustomerGroupsPage from '@/features/customerGroups/pages/CustomerGroupsPage';
import AddCustomerGroupPage from '@/features/customerGroups/pages/AddCustomerGroupPage';
import ProductsPage from '@/features/products/pages/ProductsPage';
import ProductScannerPage from '@/features/scanner/pages/ProductScannerPage';
import AddProductPage from '@/features/products/pages/AddProductPage';
import EditProductPage from '@/features/products/pages/EditProductPage';
import ProductDetailPage from '@/features/products/pages/ProductDetailPage';
import CategoriesPage from '@/features/categories/pages/CategoriesPage';
import AddCategoryPage from '@/features/categories/pages/AddCategoryPage';
import CategoryDetailPage from '@/features/categories/pages/CategoryDetailPage';
import BrandsPage from '@/features/brands/pages/BrandsPage';
import AddBrandPage from '@/features/brands/pages/AddBrandPage';
import EditBrandPage from '@/features/brands/pages/EditBrandPage';
import BrandDetailPage from '@/features/brands/pages/BrandDetailPage';
import InventoryPage from '@/features/inventory/pages/InventoryPage';
import AddInventoryPage from '@/features/inventory/pages/AddInventoryPage';
import EditInventoryPage from '@/features/inventory/pages/EditInventoryPage';
import InventoryDetailsPage from '@/features/inventory/pages/InventoryDetailsPage';
import SettingsPage from '@/features/settings/pages/SettingsPage';
import UsersPage from '@/features/users/pages/UsersPage';
import NotFoundPage from '../../pages/NotFoundPage';
import LoginPage from '@/features/auth/pages/LoginPage';
import AiImportsPage from '@/features/aiImports/pages/AiImportsPage';

export default function AppRoutes() {
  return (
    <Routes>
      {/* ── Public routes ── */}
      <Route path="/" element={<LoginPage />} />
      <Route path="/login" element={<LoginPage />} />

      {/* ── All admin screens share the AdminLayout (sidebar + topbar + Outlet) ── */}
      <Route path="/dashboard" element={<AdminLayout />}>

        {/* Overview */}
        <Route index element={<DashboardPage />} />
        <Route path="ai-assistant" element={<AiImportsPage />} />
        <Route path="import-approvals" element={<AiImportsPage mode="review" />} />
        <Route path="import-executions" element={<AiImportsPage mode="executions" />} />

        {/* Orders group */}
        <Route path="orders" element={<OrdersPage />} />
        <Route path="orders/:id" element={<OrderDetailPage />} />
        <Route path="returns" element={<ReturnsPage />} />
        <Route path="returns/:id" element={<ReturnDetailsPage />} />
        <Route path="cancellations" element={<CancellationsPage />} />

        {/* Payments group */}
        <Route path="payments" element={<PaymentsPage />} />
        <Route path="payments/:id" element={<PaymentDetailPage />} />
        <Route path="payments/bakong-callback/:id" element={<BakongCallbackPage />} />
        <Route path="refunds" element={<RefundsPage />} />
        <Route path="transactions" element={<TransactionsPage />} />
        <Route path="transactions/:id" element={<TransactionDetailPage />} />

        {/* Customers group */}
        <Route path="customers" element={<CustomersPage />} />
        <Route path="customers/:id" element={<CustomerDetailPage />} />
        <Route path="customer-groups" element={<CustomerGroupsPage />} />
        <Route path="customer-groups/add" element={<AddCustomerGroupPage />} />
        <Route path="customer-groups/edit/:id" element={<AddCustomerGroupPage />} />

        {/* Products group */}
        <Route path="products" element={<ProductsPage />} />
        <Route path="scanner" element={<ProductScannerPage />} />
        <Route path="products/add" element={<AddProductPage />} />
        <Route path="products/view/:id" element={<ProductDetailPage />} />
        <Route path="products/edit/:id" element={<EditProductPage />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="categories/add" element={<AddCategoryPage />} />
        <Route path="categories/edit/:id" element={<AddCategoryPage />} />
        <Route path="categories/view/:id" element={<CategoryDetailPage />} />
        <Route path="brands" element={<BrandsPage />} />
        <Route path="brands/add" element={<AddBrandPage />} />
        <Route path="brands/view/:id" element={<BrandDetailPage />} />
        <Route path="brands/edit/:id" element={<EditBrandPage />} />
        <Route path="inventory" element={<InventoryPage />} />
        <Route path="inventory/add" element={<AddInventoryPage />} />
        <Route path="inventory/edit" element={<EditInventoryPage />} />
        <Route path="inventory/details" element={<InventoryDetailsPage />} />

        {/* Reports group */}
        <Route path="sales-report" element={<ReportsPage />} />
        <Route path="payment-report" element={<ReportsPage />} />
        <Route path="tax-report" element={<ReportsPage />} />

        {/* Settings group */}
        <Route path="settings" element={<SettingsPage />} />
        <Route path="users" element={<UsersPage />} />

      </Route>

      {/* ── Catch-all 404 – only fires when NO admin route matches ── */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
