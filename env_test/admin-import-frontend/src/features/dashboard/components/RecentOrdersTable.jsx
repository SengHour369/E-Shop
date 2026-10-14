import { Link } from 'react-router-dom';
import { formatCurrency } from '@/utils/formatCurrency';
import { HiOutlineInbox } from 'react-icons/hi2';


function StatusBadge({ status }) {
  const s = (status || 'pending').toUpperCase();
  let bg = 'bg-slate-100', text = 'text-slate-600', dot = 'bg-slate-400';

  if (['COMPLETED', 'DELIVERED', 'SUCCESS'].includes(s)) {
    bg = 'bg-emerald-50'; text = 'text-emerald-700'; dot = 'bg-emerald-500';
  } else if (['PROCESSING', 'CONFIRMED', 'SHIPPED'].includes(s)) {
    bg = 'bg-blue-50'; text = 'text-blue-700'; dot = 'bg-blue-500';
  } else if (s === 'PENDING') {
    bg = 'bg-amber-50'; text = 'text-amber-700'; dot = 'bg-amber-500';
  } else if (['CANCELLED', 'CANCELED', 'FAILED'].includes(s)) {
    bg = 'bg-red-50'; text = 'text-red-700'; dot = 'bg-red-500';
  }

  const label = s.charAt(0) + s.slice(1).toLowerCase();
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold border ${bg} ${text}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${dot}`} />
      {label}
    </span>
  );
}

export default function RecentOrdersTable({ orders = [] }) {
  if (!orders || orders.length === 0) {
    return (
      <div className="card border-0 shadow-sm ring-1 ring-slate-200/50 flex flex-col items-center justify-center p-12 text-center">
        <div className="rounded-full bg-slate-50 p-4 mb-4">
          <HiOutlineInbox className="h-8 w-8 text-slate-400" />
        </div>
        <h3 className="text-sm font-semibold text-slate-900">No Recent Orders</h3>
        <p className="mt-1 text-sm text-slate-500">When customers place orders, they will appear here.</p>
      </div>
    );
  }

  return (
    <div className="card border-0 shadow-sm ring-1 ring-slate-200/50">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h3 className="text-lg font-bold text-slate-900 tracking-tight">Recent Orders</h3>
          <p className="text-sm text-slate-500 mt-1">Latest customer transactions</p>
        </div>
        <Link
          to="/dashboard/orders"
          className="text-sm font-semibold text-indigo-600 hover:text-indigo-700 bg-indigo-50 hover:bg-indigo-100 px-4 py-2 rounded-lg transition-colors"
        >
          View all
        </Link>
      </div>

      <div className="overflow-x-auto -mx-6 px-6">
        <table className="w-full min-w-[600px] border-collapse">
          <thead>
            <tr className="border-b-2 border-slate-100">
              <th className="pb-3 text-left text-xs font-bold uppercase tracking-wider text-slate-500">Order ID</th>
              <th className="pb-3 text-left text-xs font-bold uppercase tracking-wider text-slate-500">Customer</th>
              <th className="pb-3 text-left text-xs font-bold uppercase tracking-wider text-slate-500">Date</th>
              <th className="pb-3 text-left text-xs font-bold uppercase tracking-wider text-slate-500">Status</th>
              <th className="pb-3 text-right text-xs font-bold uppercase tracking-wider text-slate-500">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100/80">
            {orders.map((order, idx) => {
              // Real API field names (from OrdersTable.jsx and useOrders.js)
              const orderId = order.id || '';
              const orderNumber = order.order_number || order.orderNumber || order.codeOrder || `#${orderId}`;
              const customerName =
                order.customer_name || order.customer?.name || order.customerName ||
                (typeof order.customer === 'string' ? order.customer : 'Guest');
              const dateStr = order.created_at || order.order_date || order.createdAt || order.date;
              const total = order.total_amount ?? order.totalAmount ?? order.total ?? 0;
              const status = order.status || 'pending';

              const displayDate = dateStr
                ? new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                : '—';

              return (
                <tr key={orderId || idx} className="group transition-colors hover:bg-slate-50/80">
                  <td className="py-4 pr-4">
                    <Link
                      to={`/dashboard/orders/${String(orderId).replace('#', '')}`}
                      state={{ order }}
                      className="text-sm font-semibold text-indigo-600 hover:text-indigo-700 group-hover:underline"
                    >
                      {String(orderNumber).startsWith('#') ? orderNumber : `#${orderNumber}`}
                    </Link>
                  </td>
                  <td className="py-4 pr-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-slate-600">
                        {customerName.charAt(0).toUpperCase()}
                      </div>
                      <span className="text-sm font-medium text-slate-900">{customerName}</span>
                    </div>
                  </td>
                  <td className="py-4 pr-4">
                    <span className="text-sm text-slate-500">{displayDate}</span>
                  </td>
                  <td className="py-4 pr-4">
                    <StatusBadge status={status} />
                  </td>
                  <td className="py-4 text-right">
                    <span className="text-sm font-bold text-slate-900">
                      {formatCurrency(total)}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}


