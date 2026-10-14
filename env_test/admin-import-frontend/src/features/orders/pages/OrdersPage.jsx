import { useState, useEffect, useMemo } from 'react';
import OrdersTable from '../components/OrdersTable';
import { PageHeader } from '@/components/ui';
import { orderStats } from '@/features/reports/components/PageStats';
import OrderFilter from '../components/OrderFilter';
import { DataTableCard } from '@/components/ui';
import Pagination from '@/components/ui/Pagination';
import { useOrders } from '../hooks/useOrders';
import { HiOutlineCalendarDays, HiOutlineChevronDown, HiOutlineArrowDownTray } from 'react-icons/hi2';
import PageContainer from '@/components/layouts/PageContainer';
import { CriteriaType } from '../types/orderTypes';

export default function OrdersPage() {
  const [pagination, setPagination] = useState({
    page: 1,
    size: 10,
  });

  const [filters, setFilters] = useState({
    search: '',
    orderStatus: 'ALL',
    paymentMethod: 'all',
    date: '',
  });

  const params = useMemo(() => {
    let criteriaType = CriteriaType.ALL;
    let criteriaValue = "";

    const p = {
      page: pagination.page,
      size: pagination.size,
      criteria_type: criteriaType,
      criteria_value: criteriaValue,
    };

    if (filters.search) p.orderNumber = filters.search.trim();

    return p;
  }, [pagination, filters.search]);

  const { orders: allOrders, loading, totalPages, totalElements, summary, fetchOrders, updateOrderStatus, cancelOrder } = useOrders(params);

  const filteredOrders = useMemo(() => {
    return (allOrders || []).filter(order => {
      const status = filters.orderStatus;
      const paymentMethod = filters.paymentMethod;
      const selectedDate = filters.date;

      if (status && status !== 'ALL' && status !== 'All Status') {
        if (order.status?.toUpperCase() !== status.toUpperCase()) return false;
      }

      if (paymentMethod && paymentMethod !== 'all' && paymentMethod !== 'All Payment Methods') {
        if (order.payment?.payment_method?.toUpperCase() !== paymentMethod.toUpperCase()) return false;
      }

      if (selectedDate) {
        const d = order.order_date || order.created_at || order.createdAt || order.date;
        if (!d) return false;

        let matched = false;
        const dStr = String(d);

        // 1. Direct string match (handles if backend string is already exactly matching)
        if (dStr.includes(selectedDate)) {
          matched = true;
        }

        // 2. Format explicitly to Cambodia timezone (UTC+7)
        if (!matched) {
          const getCambodiaDateString = (dateInput) => {
            const orderDateObj = new Date(dateInput);
            if (!isNaN(orderDateObj.getTime())) {
              const formatter = new Intl.DateTimeFormat('en-US', {
                timeZone: 'Asia/Phnom_Penh',
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
              });
              const parts = formatter.formatToParts(orderDateObj);
              let year, month, day;
              for (const part of parts) {
                if (part.type === 'year') year = part.value;
                if (part.type === 'month') month = part.value;
                if (part.type === 'day') day = part.value;
              }
              return `${year}-${month}-${day}`;
            }
            return null;
          };

          // Try parsing the date as-is
          if (getCambodiaDateString(d) === selectedDate) {
            matched = true;
          }
          // If no 'Z' and no offset at the end, backend might have sent UTC implicitly. Try forcing UTC.
          else if (!dStr.endsWith('Z') && dStr.includes('T')) {
            if (getCambodiaDateString(dStr + 'Z') === selectedDate) {
              matched = true;
            }
          }
        }

        if (!matched) return false;
      }
      return true;
    });
  }, [allOrders, filters.orderStatus, filters.paymentMethod, filters.date]);

  // Map the real summary data to the stats array
  const mappedStats = orderStats.map(stat => {
    let newValue = stat.value;
    let badgeText = stat.badgeText;
    if (summary) {
      if (stat.label === "Total Orders") {
        newValue = summary.total_orders ?? summary.totalOrders ?? summary.total ?? stat.value;
        badgeText = ""; // Remove fake trend for now, or update if API provides it
      }
      if (stat.label === "Completed Orders") {
        newValue = summary.completed_orders ?? summary.completedOrders ?? summary.completed ?? stat.value;
        badgeText = "";
      }
      if (stat.label === "Pending Orders") {
        newValue = summary.pending_orders ?? summary.pendingOrders ?? summary.pending ?? stat.value;
        badgeText = "";
      }
      if (stat.label === "Cancelled Orders") {
        newValue = summary.cancelled_orders ?? summary.cancelledOrders ?? summary.cancelled ?? summary.canceled ?? stat.value;
        badgeText = "";
      }
    }
    return { ...stat, value: newValue, badgeText };
  });

  useEffect(() => {
    fetchOrders(params);
  }, [params, fetchOrders]);

  const handlePageChange = (newPage) => {
    setPagination(prev => ({ ...prev, page: newPage }));
  };

  const handleFilterChange = (newFilters) => {
    setFilters(prev => ({ ...prev, ...newFilters }));
    setPagination(prev => ({ ...prev, page: 1 }));
  };

  const handleClearFilters = () => {
    setFilters({
      search: '',
      orderStatus: 'ALL',
      paymentMethod: 'all',
      date: '',
    });
    setPagination(prev => ({ ...prev, page: 1 }));
  };

  return (
    <PageContainer>
      <PageHeader
        title="History Order Details"
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Orders' }, { label: 'History Order Details' }]}
        stats={mappedStats}
        loading={loading}
      >
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <button className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg shadow-sm hover:bg-slate-50">
            <HiOutlineCalendarDays className="w-4 h-4 text-slate-500" />
            May 20, 2024 - May 26, 2024
            <HiOutlineChevronDown className="w-4 h-4 text-slate-400 ml-2" />
          </button>
          <button className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg shadow-sm hover:bg-blue-700">
            <HiOutlineArrowDownTray className="w-4 h-4" />
            Export
            <HiOutlineChevronDown className="w-4 h-4 ml-1 opacity-70" />
          </button>
        </div>
      </PageHeader>

      <DataTableCard
        toolbar={
          <OrderFilter
            search={filters.search}
            onSearchChange={(search) => handleFilterChange({ search })}
            orderStatus={filters.orderStatus}
            onOrderStatusChange={(orderStatus) => handleFilterChange({ orderStatus })}
            paymentMethod={filters.paymentMethod}
            onPaymentMethodChange={(paymentMethod) => handleFilterChange({ paymentMethod })}
            date={filters.date}
            onDateChange={(date) => handleFilterChange({ date })}
            onClearFilters={handleClearFilters}
          />
        }
        loading={loading}
        loadingMessage="Loading orders..."
        footer={
          (totalPages > 1 || filteredOrders.length !== (allOrders || []).length || totalElements > 0) && (
            <div className="w-full sm:w-auto overflow-hidden rounded-lg border border-slate-100">
              <Pagination
                pageNumber={params.page}
                totalPages={filters.orderStatus !== 'ALL' || filters.paymentMethod !== 'all' || filters.date !== '' ? Math.max(1, Math.ceil(filteredOrders.length / pagination.size)) : totalPages}
                pageSize={pagination.size}
                totalResults={filters.orderStatus !== 'ALL' || filters.paymentMethod !== 'all' || filters.date !== '' ? filteredOrders.length : totalElements}
                onPageChange={handlePageChange}
              />
            </div>
          )
        }
      >
        <OrdersTable
          orders={filteredOrders}
          onUpdateStatus={(id, status) => updateOrderStatus(id, status, params)}
          onCancel={(id, userId, reason) => cancelOrder(id, userId, reason, params)}
          loading={loading}
        />
      </DataTableCard>
    </PageContainer>
  );
}
