import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { cn } from '@/utils/cn';
import { HiOutlineEllipsisVertical } from 'react-icons/hi2';

// ── Payment method badges ──────────────────────────────────────────────────────
function MethodBadge({ method }) {
  const m = (method || '').toLowerCase();
  if (m === 'visa') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex items-center justify-center rounded px-2 py-0.5 text-[10px] font-black tracking-widest bg-blue-700 text-white leading-none">
          VISA
        </span>
      </span>
    );
  }
  if (m === 'mastercard') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex items-center">
          <span className="h-4 w-4 rounded-full bg-red-500" />
          <span className="h-4 w-4 -ml-2 rounded-full bg-amber-400 opacity-90" />
        </span>
        <span className="text-xs text-slate-500 font-mono">Mastercard</span>
      </span>
    );
  }
  if (m === 'paypal') {
    return (
      <span className="inline-flex items-center gap-1 font-extrabold text-sm tracking-tight">
        <span className="text-[#003087]">Pay</span>
        <span className="text-[#009cde]">Pal</span>
      </span>
    );
  }
  if (m === 'applepay') {
    return (
      <span className="inline-flex items-center gap-1 text-slate-800 font-semibold text-sm tracking-tight">
        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98l-.09.06c-.22.14-2.18 1.27-2.16 3.8.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.74M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
        </svg>
        Pay
      </span>
    );
  }
  if (m === 'upi') {
    return (
      <span className="inline-flex items-center gap-1 font-bold text-sm">
        <span className="text-[#097939]">U</span>
        <span className="text-[#ed752e]">P</span>
        <span className="text-[#097939]">I</span>
        <svg className="h-3 w-3 ml-0.5 text-[#097939]" viewBox="0 0 24 24" fill="currentColor">
          <path d="M5 12l7-7 7 7M5 12l7 7 7-7" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" />
        </svg>
      </span>
    );
  }
  if (m === 'stripe') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex h-5 w-5 items-center justify-center rounded bg-indigo-600 text-white text-xs font-bold">S</span>
        <span className="text-xs text-slate-600 font-medium">Stripe</span>
      </span>
    );
  }
  if (m === 'bakong') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex items-center justify-center rounded px-2 py-0.5 text-[10px] font-black tracking-widest bg-red-600 text-white leading-none">
          BAKONG
        </span>
      </span>
    );
  }
  return <span className="text-xs text-slate-500 font-medium uppercase">{method}</span>;
}

// ── Status config ──────────────────────────────────────────────────────────────
const STATUS_MAP = {
  completed: { label: 'Completed', variant: 'success' },
  paid: { label: 'Success', variant: 'success' },
  success: { label: 'Success', variant: 'success' },
  pending: { label: 'Pending', variant: 'warning' },
  failed: { label: 'Failed', variant: 'danger' },
  cancelled: { label: 'Cancelled', variant: 'danger' },
  refunded: { label: 'Refunded', variant: 'default' },
};

export default function PaymentTable({ payments, loading }) {
  const [actionMenu, setActionMenu] = useState(null);
  const navigate = useNavigate();

  const formatDT = (dt) => {
    if (!dt) return { date: '-', time: '-' };
    const d = new Date(dt);
    if (isNaN(d.getTime())) return { date: '-', time: '-' };
    return {
      date: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
  };

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-100">
        <thead>
          <tr className="bg-slate-50">
            {[
              { label: 'Payment ID' },
              { label: 'Order / Code' },
              { label: 'Amount' },
              { label: 'Payment Method' },
              { label: 'Status' },
              { label: 'Payment Date' },
              { label: 'Action', align: 'right' },
            ].map(h => (
              <th
                key={h.label}
                className={cn(
                  "px-4 py-3 text-[11px] font-semibold uppercase tracking-wide text-slate-400 whitespace-nowrap",
                  h.align === 'right' ? 'text-right' : 'text-left'
                )}
              >
                {h.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-50 bg-white relative">
          {loading && payments.length === 0 ? (
            <tr>
              <td colSpan={7} className="py-16 text-center text-sm text-slate-400">
                Loading payments...
              </td>
            </tr>
          ) : payments.length === 0 ? (
            <tr>
              <td colSpan={7} className="py-16 text-center text-sm text-slate-400">
                No payments found.
              </td>
            </tr>
          ) : payments.map(p => {
            const { date, time } = formatDT(p.payment_date || p.datetime);
            const stStr = (p.status || 'pending').toLowerCase();
            const st = STATUS_MAP[stStr] || STATUS_MAP.pending;
            
            return (
              <tr
                key={p.id}
                className="hover:bg-slate-50/70 transition-colors group"
                onClick={() => setActionMenu(null)}
              >
                {/* Payment ID / Code */}
                <td className="px-4 py-3.5 font-mono text-xs font-semibold text-slate-700 whitespace-nowrap">
                  {p.code || p.id}
                </td>

                {/* Order ID */}
                <td className="px-4 py-3.5 text-xs font-medium text-slate-600 whitespace-nowrap">
                  {p.order_number || p.codeOrder || p.order_id || p.orderId || '-'}
                </td>

                {/* Amount */}
                <td className="px-4 py-3.5 whitespace-nowrap text-sm font-bold text-slate-800 tabular-nums">
                  {formatCurrency(p.amount, p.currency || 'USD')}
                </td>

                {/* Payment Method */}
                <td className="px-4 py-3.5 whitespace-nowrap">
                  <MethodBadge method={p.payment_method || p.method} />
                </td>

                {/* Status */}
                <td className="px-4 py-3.5 whitespace-nowrap">
                  <Badge variant={st.variant} dot>{st.label}</Badge>
                </td>

                {/* Payment Date */}
                <td className="px-4 py-3.5 whitespace-nowrap">
                  <p className="text-xs font-medium text-slate-700">{date}</p>
                  <p className="text-[11px] text-slate-400">{time}</p>
                </td>

                {/* Action */}
                <td className="px-4 py-3.5 whitespace-nowrap w-[1%] text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      title="View Details"
                      className="inline-flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-blue-600 transition-colors"
                      onClick={(e) => { 
                        e.stopPropagation(); 
                        navigate(`/dashboard/payments/${p.id}`); 
                      }}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </button>
                    <button
                      title="Edit Payment"
                      className="inline-flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-amber-600 transition-colors"
                      onClick={(e) => { e.stopPropagation(); /* handle edit */ }}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487zm0 0L19.5 7.125" />
                      </svg>
                    </button>
                  
                    {((p.payment_method || p.method || '').toLowerCase() === 'bakong' || true) && (
                      <button
                        title="Process Bakong Callback"
                        className="inline-flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-emerald-600 transition-colors"
                        onClick={(e) => { 
                          e.stopPropagation(); 
                          navigate(`/dashboard/payments/bakong-callback/${p.id}`);
                        }}
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
                        </svg>
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
