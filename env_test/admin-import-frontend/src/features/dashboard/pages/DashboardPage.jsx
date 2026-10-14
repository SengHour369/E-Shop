import {
  HiOutlineShoppingBag, HiOutlineClipboardDocumentList,
  HiOutlineCurrencyDollar, HiOutlineUsers,
} from 'react-icons/hi2';
import StatsCard from '../components/StatsCard';
import SalesChart from '../components/SalesChart';
import OrdersChart from '../components/OrdersChart';
import RecentOrdersTable from '../components/RecentOrdersTable';
import { PageHeader } from '@/components/ui';
import PageContainer from '@/components/layouts/PageContainer';
import { useDashboard } from '../hooks/useDashboard';

export default function DashboardPage() {
  const { data, loading, error } = useDashboard();

  if (loading) {
    return (
      <PageContainer>
        <PageHeader title="Dashboard" crumbs={[{ label: 'Dashboard' }]} />
        <div className="animate-pulse space-y-6">
          <div className="h-28 rounded-2xl bg-slate-200 w-full"></div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-32 rounded-2xl bg-slate-200"></div>
            ))}
          </div>
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
            <div className="xl:col-span-2 h-72 rounded-2xl bg-slate-200"></div>
            <div className="h-72 rounded-2xl bg-slate-200"></div>
          </div>
          <div className="h-64 rounded-2xl bg-slate-200"></div>
        </div>
      </PageContainer>
    );
  }

  if (error && !data) {
    return (
      <PageContainer>
         <div className="flex h-64 items-center justify-center rounded-2xl border border-red-100 bg-red-50 text-red-500">
            <p>Error loading dashboard data: {error}</p>
         </div>
      </PageContainer>
    );
  }

  const { stats, charts, recentOrders } = data;

  return (
    <PageContainer>
      <PageHeader 
        title="Dashboard" 
        crumbs={[{ label: 'Dashboard' }]}
      />

      {/* Welcome banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-600 to-violet-600 p-8 text-white shadow-lg">
        {/* Decorative elements */}
        <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-white opacity-10 blur-3xl mix-blend-overlay"></div>
        <div className="absolute -left-10 -bottom-10 h-40 w-40 rounded-full bg-blue-400 opacity-20 blur-2xl mix-blend-overlay"></div>
        
        <div className="relative z-10">
          <h2 className="text-2xl font-bold tracking-tight">Welcome back, Admin 👋</h2>
          <p className="mt-2 text-indigo-100 max-w-xl text-sm leading-relaxed">Here's a detailed overview of your store's performance today. Monitor sales, track recent orders, and manage your inventory all in one place.</p>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 mt-6">
        <StatsCard
          title="Total Sales"
          value={stats.totalSales}
          growth={stats.salesGrowth}
          color="indigo"
          isCurrency
          icon={<HiOutlineCurrencyDollar className="h-6 w-6" />}
        />
        <StatsCard
          title="Total Orders"
          value={stats.totalOrders}
          growth={stats.ordersGrowth}
          color="amber"
          icon={<HiOutlineClipboardDocumentList className="h-6 w-6" />}
        />
        <StatsCard
          title="Total Revenue"
          value={stats.totalRevenue}
          growth={stats.revenueGrowth}
          color="emerald"
          isCurrency
          icon={<HiOutlineShoppingBag className="h-6 w-6" />}
        />
        <StatsCard
          title="Total Customers"
          value={stats.totalCustomers}
          growth={stats.customersGrowth}
          color="violet"
          icon={<HiOutlineUsers className="h-6 w-6" />}
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3 mt-6">
        <div className="xl:col-span-2">
          <SalesChart data={charts.sales} />
        </div>
        <div>
          <OrdersChart data={charts.orders} />
        </div>
      </div>

      {/* Recent orders */}
      <div className="mt-6">
        <RecentOrdersTable orders={recentOrders} />
      </div>
    </PageContainer>
  );
}
