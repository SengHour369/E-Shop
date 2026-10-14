// src/features/payments/PaymentDetailsTable.jsx
// Full Payment Details table matching the design screenshot
import { useState } from 'react';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { cn } from '@/utils/cn';
import {
  HiOutlineMagnifyingGlass,
  HiOutlineFunnel,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineEllipsisVertical,
  HiArrowUpTray,
  HiOutlineCreditCard,
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineXCircle,
} from 'react-icons/hi2';

// ── Payment method badges ──────────────────────────────────────────────────────
function MethodBadge({ method, detail }) {
  if (method === 'visa') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex items-center justify-center rounded px-2 py-0.5 text-[10px] font-black tracking-widest bg-blue-700 text-white leading-none">
          VISA
        </span>
        <span className="text-xs text-slate-500 font-mono">{detail}</span>
      </span>
    );
  }
  if (method === 'mastercard') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex items-center">
          <span className="h-4 w-4 rounded-full bg-red-500" />
          <span className="h-4 w-4 -ml-2 rounded-full bg-amber-400 opacity-90" />
        </span>
        <span className="text-xs text-slate-500 font-mono">{detail}</span>
      </span>
    );
  }
  if (method === 'paypal') {
    return (
      <span className="inline-flex items-center gap-1 font-extrabold text-sm tracking-tight">
        <span className="text-[#003087]">Pay</span>
        <span className="text-[#009cde]">Pal</span>
      </span>
    );
  }
  if (method === 'applepay') {
    return (
      <span className="inline-flex items-center gap-1 text-slate-800 font-semibold text-sm tracking-tight">
        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98l-.09.06c-.22.14-2.18 1.27-2.16 3.8.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.74M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
        </svg>
        Pay
      </span>
    );
  }
  if (method === 'upi') {
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
  if (method === 'stripe') {
    return (
      <span className="inline-flex items-center gap-2">
        <span className="inline-flex h-5 w-5 items-center justify-center rounded bg-indigo-600 text-white text-xs font-bold">S</span>
        <span className="text-xs text-slate-600 font-medium">Stripe</span>
      </span>
    );
  }
  return <span className="text-xs text-slate-500">{detail}</span>;
}

// ── Status config ──────────────────────────────────────────────────────────────
const STATUS_MAP = {
  paid: { label: 'Success', variant: 'success' },
  pending: { label: 'Pending', variant: 'warning' },
  failed: { label: 'Failed', variant: 'danger' },
  refunded: { label: 'Refunded', variant: 'default' },
};

// ── Stat Overview Card ─────────────────────────────────────────────────────────
function OverviewCard({ icon, iconBg, iconRing, label, value, growth, sub }) {
  const isPositive = growth >= 0;
  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 flex items-start gap-4">
      <div className={cn(
        'flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full text-xl',
        iconBg, iconRing, 'ring-4'
      )}>
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-slate-400 mb-0.5">{label}</p>
        <p className="text-xl font-bold text-slate-900 tabular-nums">
          ${typeof value === 'number'
            ? value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
            : value}
        </p>
        <p className={cn(
          'mt-1 text-xs font-medium flex items-center gap-1',
          isPositive ? 'text-emerald-600' : 'text-red-500'
        )}>
          <span>{isPositive ? '↑' : '↓'} {Math.abs(growth)}%</span>
          <span className="text-slate-400 font-normal">from last 7 days</span>
        </p>
      </div>
    </div>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────────
const PAGE_SIZES = [10, 20, 50];

export default function PaymentDetailsTable({ payments, stats }) {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [method, setMethod] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [actionMenu, setActionMenu] = useState(null);

  // filter
  const filtered = payments.filter(p => {
    const q = search.toLowerCase();
    const matchQ = !q ||
      p.id.toLowerCase().includes(q) ||
      p.orderId.toLowerCase().includes(q) ||
      p.customer.name.toLowerCase().includes(q) ||
      p.customer.email.toLowerCase().includes(q);
    const matchS = !status || p.status === status;
    const matchM = !method || p.method === method;
    return matchQ && matchS && matchM;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const safePage = Math.min(page, totalPages);
  const paginated = filtered.slice((safePage - 1) * pageSize, safePage * pageSize);

  const formatDT = (dt) => {
    const d = new Date(dt);
    return {
      date: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
  };

  // pagination pages to show
  const getPageNumbers = () => {
    const pages = [];
    if (totalPages <= 5) {
      for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
      pages.push(1, 2, 3);
      if (safePage > 4) pages.push('…');
      if (safePage > 3 && safePage < totalPages - 1) pages.push(safePage);
      if (totalPages > 3) pages.push(totalPages);
    }
    return [...new Set(pages)];
  };

  return (
    <div className="space-y-5">
      {/* ── Overview Stats ── */}
      <div>
        <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
          <OverviewCard
            icon={<HiOutlineCreditCard className="h-6 w-6 text-blue-600" />}
            iconBg="bg-blue-50"
            iconRing="ring-blue-100"
            label={stats.total.label}
            value={stats.total.value}
            growth={stats.total.growth}
          />
          <OverviewCard
            icon={<HiOutlineCheckCircle className="h-6 w-6 text-emerald-600" />}
            iconBg="bg-emerald-50"
            iconRing="ring-emerald-100"
            label={stats.successful.label}
            value={stats.successful.value}
            growth={stats.successful.growth}
          />
          <OverviewCard
            icon={<HiOutlineClock className="h-6 w-6 text-amber-600" />}
            iconBg="bg-amber-50"
            iconRing="ring-amber-100"
            label={stats.pending.label}
            value={stats.pending.value}
            growth={stats.pending.growth}
          />
          <OverviewCard
            icon={<HiOutlineXCircle className="h-6 w-6 text-red-600" />}
            iconBg="bg-red-50"
            iconRing="ring-red-100"
            label={stats.failed.label}
            value={stats.failed.value}
            growth={stats.failed.growth}
          />
        </div>
      </div>

      {/* ── Table Card ── */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">

        {/* Table header / filters bar */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4 border-b border-slate-100">
          <h3 className="text-sm font-semibold text-slate-800">Payment Details</h3>
          <div className="flex flex-wrap items-center gap-2 ml-auto">
            {/* Search */}
            <div className="relative">
              <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="Search by order ID, customer, payment ID..."
                value={search}
                onChange={e => { setSearch(e.target.value); setPage(1); }}
                className="w-56 rounded-lg border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-xs placeholder-slate-400 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 focus:bg-white transition-all"
              />
            </div>

            {/* Status filter */}
            <select
              value={status}
              onChange={e => { setStatus(e.target.value); setPage(1); }}
              className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs text-slate-600 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            >
              <option value="">All Status</option>
              <option value="paid">Success</option>
              <option value="pending">Pending</option>
              <option value="failed">Failed</option>
              <option value="refunded">Refunded</option>
            </select>

            {/* Method filter */}
            <select
              value={method}
              onChange={e => { setMethod(e.target.value); setPage(1); }}
              className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs text-slate-600 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            >
              <option value="">All Payment Methods</option>
              <option value="visa">Visa</option>
              <option value="mastercard">Mastercard</option>
              <option value="paypal">PayPal</option>
              <option value="applepay">Apple Pay</option>
              <option value="upi">UPI</option>
              <option value="stripe">Stripe</option>
            </select>

            {/* Filters button */}
            <button className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-100 transition-colors">
              <HiOutlineFunnel className="h-3.5 w-3.5" />
              Filters
            </button>

            {/* Export button */}
            <button className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 transition-colors shadow-sm">
              <HiArrowUpTray className="h-3.5 w-3.5" />
              Export
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-100">
            <thead>
              <tr className="bg-slate-50">
                {[
                  { label: 'Payment ID', sortable: true },
                  { label: 'Order ID' },
                  { label: 'Customer' },
                  { label: 'Amount' },
                  { label: 'Payment Method' },
                  { label: 'Status' },
                  { label: 'Payment Date' },
                  { label: 'Action' },
                ].map(h => (
                  <th
                    key={h.label}
                    className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wide text-slate-400 whitespace-nowrap"
                  >
                    <span className="flex items-center gap-1">
                      {h.label}
                      {h.sortable && (
                        <svg className="h-3 w-3 text-slate-300" viewBox="0 0 16 16" fill="none">
                          <path d="M8 3l3 4H5l3-4zm0 10l-3-4h6l-3 4z" fill="currentColor" />
                        </svg>
                      )}
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 bg-white">
              {paginated.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-16 text-center text-sm text-slate-400">
                    No payments found.
                  </td>
                </tr>
              ) : paginated.map(p => {
                const { date, time } = formatDT(p.datetime);
                const st = STATUS_MAP[p.status] || STATUS_MAP.pending;
                return (
                  <tr
                    key={p.id}
                    className="hover:bg-slate-50/70 transition-colors group"
                    onClick={() => setActionMenu(null)}
                  >
                    {/* Payment ID */}
                    <td className="px-4 py-3.5 font-mono text-xs font-semibold text-slate-700 whitespace-nowrap">
                      {p.id}
                    </td>

                    {/* Order ID */}
                    <td className="px-4 py-3.5 text-xs font-medium text-slate-600 whitespace-nowrap">
                      {p.orderId}
                    </td>

                    {/* Customer */}
                    <td className="px-4 py-3.5">
                      <div className="flex items-center gap-2.5 min-w-[160px]">
                        <img
                          src={p.customer.avatar}
                          alt={p.customer.name}
                          className="h-8 w-8 rounded-full object-cover flex-shrink-0 ring-2 ring-white shadow-sm"
                        />
                        <div className="min-w-0">
                          <p className="text-xs font-semibold text-slate-800 truncate">{p.customer.name}</p>
                          <p className="text-[11px] text-slate-400 truncate">{p.customer.email}</p>
                        </div>
                      </div>
                    </td>

                    {/* Amount */}
                    <td className="px-4 py-3.5 whitespace-nowrap text-sm font-bold text-slate-800 tabular-nums">
                      {formatCurrency(p.amount)}
                    </td>

                    {/* Payment Method */}
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <MethodBadge method={p.method} detail={p.methodDetail} />
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
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <div className="relative">
                        <button
                          title="Actions"
                          onClick={e => { e.stopPropagation(); setActionMenu(actionMenu === p.id ? null : p.id); }}
                          className="inline-flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
                        >
                          <HiOutlineEllipsisVertical className="h-4 w-4" />
                        </button>
                        {actionMenu === p.id && (
                          <div className="absolute right-0 z-50 mt-1 w-36 rounded-xl border border-slate-100 bg-white py-1 shadow-xl shadow-slate-200/60">
                            <button className="block w-full px-3 py-2 text-left text-xs text-slate-700 hover:bg-slate-50 transition-colors">
                              View Details
                            </button>
                            <button className="block w-full px-3 py-2 text-left text-xs text-slate-700 hover:bg-slate-50 transition-colors">
                              Download Receipt
                            </button>
                            <button className="block w-full px-3 py-2 text-left text-xs text-red-600 hover:bg-red-50 transition-colors">
                              Refund Payment
                            </button>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* ── Pagination ── */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 bg-white px-5 py-3">
          <p className="text-xs text-slate-500">
            Showing{' '}
            <span className="font-medium text-slate-700">
              {filtered.length === 0 ? 0 : (safePage - 1) * pageSize + 1}
            </span>{' '}
            to{' '}
            <span className="font-medium text-slate-700">
              {Math.min(safePage * pageSize, filtered.length)}
            </span>{' '}
            of{' '}
            <span className="font-medium text-slate-700">{filtered.length}</span>{' '}
            results
          </p>

          <div className="flex items-center gap-1.5">
            {/* Prev */}
            <button
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={safePage === 1}
              className="inline-flex h-7 w-7 items-center justify-center rounded-lg border border-slate-200 text-slate-500 disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              <HiOutlineChevronLeft className="h-3.5 w-3.5" />
            </button>

            {getPageNumbers().map((pg, idx) =>
              pg === '…' ? (
                <span key={`ellipsis-${idx}`} className="px-0.5 text-slate-400 text-sm">…</span>
              ) : (
                <button
                  key={pg}
                  onClick={() => setPage(pg)}
                  className={cn(
                    'inline-flex h-7 w-7 items-center justify-center rounded-lg text-xs font-medium border transition-colors',
                    safePage === pg
                      ? 'border-blue-600 bg-blue-600 text-white shadow-sm'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  )}
                >
                  {pg}
                </button>
              )
            )}

            {/* Next */}
            <button
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={safePage === totalPages}
              className="inline-flex h-7 w-7 items-center justify-center rounded-lg border border-slate-200 text-slate-500 disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              <HiOutlineChevronRight className="h-3.5 w-3.5" />
            </button>

            {/* Per-page */}
            <select
              value={pageSize}
              onChange={e => { setPageSize(Number(e.target.value)); setPage(1); }}
              className="ml-1 rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-600 focus:border-blue-400 focus:outline-none"
            >
              {PAGE_SIZES.map(s => (
                <option key={s} value={s}>{s} / page</option>
              ))}
            </select>
          </div>
        </div>
      </div>
    </div>
  );
}
