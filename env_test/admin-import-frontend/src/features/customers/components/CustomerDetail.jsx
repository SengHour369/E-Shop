import { Link } from 'react-router-dom';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import { CUSTOMER_STATUSES, ORDER_STATUSES } from '@/constants/status';
import {
  HiOutlineEnvelope, HiOutlinePhone, HiOutlineMapPin, HiOutlineCalendar,
} from 'react-icons/hi2';
import { PageHeader } from '@/components/ui';

export default function CustomerDetail({ customer, orders }) {
  const statusDef = CUSTOMER_STATUSES[customer.status] || CUSTOMER_STATUSES.active;

  return (
    <div className="space-y-6">
      {/* Back */}
      <PageHeader 
        title="Customer Detail"
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Customers', path: '/customers' }, { label: customer.name }]}
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Profile card */}
        <div className="card flex flex-col items-center text-center">
          <img
            src={customer.avatar}
            alt={customer.name}
            className="h-20 w-20 rounded-full object-cover border-4 border-indigo-100"
          />
          <h3 className="mt-3 text-lg font-bold text-slate-900">{customer.name}</h3>
          <Badge variant={statusDef.variant} dot className="mt-1">{statusDef.label}</Badge>

          <div className="mt-4 w-full space-y-2 border-t border-slate-100 pt-4 text-left">
            <div className="flex items-center gap-2 text-sm text-slate-600">
              <HiOutlineEnvelope className="h-4 w-4 text-slate-400" />
              {customer.email}
            </div>
            <div className="flex items-center gap-2 text-sm text-slate-600">
              <HiOutlinePhone className="h-4 w-4 text-slate-400" />
              {customer.phone}
            </div>
            <div className="flex items-center gap-2 text-sm text-slate-600">
              <HiOutlineMapPin className="h-4 w-4 text-slate-400" />
              {customer.city}
            </div>
            <div className="flex items-center gap-2 text-sm text-slate-600">
              <HiOutlineCalendar className="h-4 w-4 text-slate-400" />
              Joined {formatDate(customer.joinDate)}
            </div>
          </div>

          {/* Stats */}
          <div className="mt-4 grid w-full grid-cols-2 gap-3 border-t border-slate-100 pt-4">
            <div className="rounded-xl bg-indigo-50 p-3 text-center">
              <p className="text-xl font-bold text-indigo-600">{customer.orders}</p>
              <p className="text-xs text-slate-500">Orders</p>
            </div>
            <div className="rounded-xl bg-emerald-50 p-3 text-center">
              <p className="text-xl font-bold text-emerald-600">{formatCurrency(customer.totalSpent)}</p>
              <p className="text-xs text-slate-500">Total Spent</p>
            </div>
          </div>
        </div>

        {/* Order history */}
        <div className="lg:col-span-2">
          <div className="card">
            <h3 className="mb-4 text-base font-semibold text-slate-900">Order History</h3>
            {orders.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-400">No orders found.</p>
            ) : (
              <div className="space-y-3">
                {orders.map(order => {
                  const s = ORDER_STATUSES[order.status] || ORDER_STATUSES.pending;
                  return (
                    <div
                      key={order.id}
                      className="flex items-center justify-between rounded-xl border border-slate-100 p-3 hover:bg-slate-50 transition-colors"
                    >
                      <div>
                        <Link
                          to={`/orders/${order.id.replace('#', '')}`}
                          className="font-semibold text-indigo-600 hover:underline text-sm"
                        >
                          {order.id}
                        </Link>
                        <p className="text-xs text-slate-400">{formatDate(order.date)} · {order.items} item{order.items !== 1 ? 's' : ''}</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <Badge variant={s.variant} dot>{s.label}</Badge>
                        <span className="font-semibold text-slate-900 text-sm">{formatCurrency(order.total)}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
