import { useState, useEffect } from 'react';
import { PageHeader, Badge, Button } from '@/components/ui';
import {  cancelationStats  } from '@/features/reports/components/PageStats';
import { HiOutlineMagnifyingGlass, HiOutlineFunnel } from 'react-icons/hi2';
import orderService from '@/features/orders/services/order.service';
import { formatCurrency } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import PageContainer from '@/components/layouts/PageContainer';
import { Link } from 'react-router-dom';


export default function CancellationsPage() {
  const [search, setSearch] = useState('');
  const [cancellations, setCancellations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCancellations = async () => {
      try {
        setLoading(true);
        // Fetch all orders and filter for cancelled ones
        const response = await orderService.getAll({ page: 1, size: 500 });
        const allData = response?.payload || response?.data || response || [];
        const rawArray = Array.isArray(allData) ? allData : [];
        
        const canceledOrders = rawArray.filter(o => 
          o.status?.toUpperCase() === 'CANCELED' || 
          o.status?.toUpperCase() === 'CANCELLED' ||
          o.orderStatus?.toUpperCase() === 'CANCELED'
        );
        
        setCancellations(canceledOrders);
      } catch (err) {
        console.error('Failed to fetch cancellations:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchCancellations();
  }, []);

  const filtered = cancellations.filter(c => 
    c.id?.toString().toLowerCase().includes(search.toLowerCase()) || 
    c.order_number?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <PageContainer>
      <PageHeader 
        title="Cancellations" 
        description="Review and process order cancelation requests."
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Cancellations' }]}
        stats={cancelationStats}
      />

      {/* Toolbar */}
      <div className="card flex flex-col sm:flex-row justify-between gap-4">
        <div className="relative max-w-sm flex-1">
          <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search by order ID..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm placeholder-slate-400 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
          />
        </div>
        <Button variant="secondary">
          <HiOutlineFunnel className="h-4 w-4" />
          Filter
        </Button>
      </div>

      <div className="card overflow-hidden p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="border-b border-slate-100 bg-slate-50/50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-6 py-4 font-medium">Order ID</th>
                <th className="px-6 py-4 font-medium">Customer</th>
                <th className="px-6 py-4 font-medium">Cancel Date</th>
                <th className="px-6 py-4 font-medium">Reason</th>
                <th className="px-6 py-4 font-medium">Amount</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="px-6 py-4 font-medium text-blue-600">
                    {item.order_number || item.codeOrder || `#${item.id}`}
                  </td>
                  <td className="px-6 py-4">
                    {item.customer?.name || item.user?.username || item.user?.email || 'Unknown'}
                  </td>
                  <td className="px-6 py-4">
                    {formatDate(item.cancelDate || item.updated_at || item.created_at || item.date)}
                  </td>
                  <td className="px-6 py-4">
                    <Badge variant="danger" dot>{item.cancelReason || item.reason || 'Canceled'}</Badge>
                  </td>
                  <td className="px-6 py-4 font-medium text-slate-900">
                    {formatCurrency(item.total_amount || item.total || item.amount || 0)}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <Link to={`/dashboard/orders/${item.id}`} state={{ userId: item.user_id || item.customer_id || 1 }} className="text-blue-600 hover:text-blue-800 text-xs font-semibold">
                      View Details
                    </Link>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-500">No cancellations found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </PageContainer>
  );
}
