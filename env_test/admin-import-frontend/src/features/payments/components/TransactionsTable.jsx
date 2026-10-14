// src/features/payments/components/TransactionsTable.jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { cn } from '@/utils/cn';
import {
  HiOutlineEye,
  HiOutlineMagnifyingGlass,
  HiOutlineFunnel,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiMiniArrowTrendingUp,
  HiMiniArrowTrendingDown,
  HiOutlineCreditCard,
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineXCircle,
  HiOutlineCurrencyDollar,
} from 'react-icons/hi2';

// ── Payment method label map ───────────────────────────────────────────────────
const METHOD_ICON = (method = '') => {
  const m = method.toUpperCase();
  if (m === 'VISA')
    return <span className="rounded px-1.5 py-0.5 text-[10px] font-black tracking-widest bg-blue-700 text-white">VISA</span>;
  if (m === 'MASTERCARD')
    return (
      <span className="inline-flex items-center gap-1">
        <span className="h-4 w-4 rounded-full bg-red-500 opacity-90" />
        <span className="h-4 w-4 -ml-2 rounded-full bg-yellow-400 opacity-80" />
      </span>
    );
  if (m === 'PAYPAL')
    return <span className="inline-flex items-center gap-1 font-bold text-blue-600 text-xs"><span className="text-blue-800">P</span><span className="text-blue-500">P</span></span>;
  if (m === 'APPLEPAY' || m === 'APPLE_PAY')
    return <span className="inline-flex items-center text-slate-800 font-semibold text-xs tracking-tight">⊛ Pay</span>;
  if (m === 'STRIPE')
    return <span className="inline-flex items-center justify-center h-5 w-5 rounded bg-indigo-600 text-white text-xs font-bold">S</span>;
  if (m === 'RAZORPAY')
    return <span className="inline-flex items-center justify-center h-5 w-5 rounded bg-blue-500 text-white text-xs font-bold">R</span>;
  if (m === 'SINGE' || m === 'BAKONG')
    return <span className="inline-flex items-center justify-center rounded px-1.5 py-0.5 text-[10px] font-bold bg-emerald-600 text-white">{m}</span>;
  return <span className="text-xs text-slate-500">{method}</span>;
};

// ── Status config ──────────────────────────────────────────────────────────────
const STATUS_MAP = {
  COMPLETED:  { label: 'Successful', variant: 'success' },
  PAID:       { label: 'Paid',       variant: 'success' },
  SUCCESS:    { label: 'Successful', variant: 'success' },
  PENDING:    { label: 'Pending',    variant: 'warning' },
  FAILED:     { label: 'Failed',     variant: 'danger'  },
  REFUNDED:   { label: 'Refunded',   variant: 'default' },
  CANCELLED:  { label: 'Cancelled',  variant: 'default' },
  // legacy lowercase keys (mock data fallback)
  paid:     { label: 'Successful', variant: 'success' },
  pending:  { label: 'Pending',    variant: 'warning' },
  failed:   { label: 'Failed',     variant: 'danger'  },
  refunded: { label: 'Refunded',   variant: 'default' },
};

// ── Stat Card ──────────────────────────────────────────────────────────────────
function StatCard({ icon, iconBg, label, value, growth, prefix = '', loading = false }) {
  const isPositive = growth >= 0;
  
  if (loading) {
    return (
      <div className="card flex items-start gap-4 p-5">
        <div className="h-11 w-11 flex-shrink-0 rounded-xl bg-slate-100 animate-pulse" />
        <div className="min-w-0 flex-1 space-y-2.5 py-1">
          <div className="h-2.5 w-24 rounded bg-slate-100 animate-pulse" />
          <div className="h-4 w-32 rounded bg-slate-100 animate-pulse" />
          <div className="h-2 w-16 rounded bg-slate-100 animate-pulse" />
        </div>
      </div>
    );
  }

  return (
    <div className="card flex items-start gap-4 p-5">
      <div className={cn('flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl text-white text-xl', iconBg)}>
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-slate-400 truncate">{label}</p>
        <p className="mt-0.5 text-2xl font-bold text-slate-900 tabular-nums">
          {prefix}{typeof value === 'number' && prefix === '$'
            ? value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
            : (typeof value === 'number' ? value.toLocaleString() : value)}
        </p>
        {growth != null && (
          <p className={cn('mt-1 flex items-center gap-1 text-xs font-medium',
            isPositive ? 'text-emerald-600' : 'text-red-500')}>
            {isPositive
              ? <HiMiniArrowTrendingUp className="h-3.5 w-3.5" />
              : <HiMiniArrowTrendingDown className="h-3.5 w-3.5" />}
            {Math.abs(growth)}%
            <span className="text-slate-400 font-normal">vs last period</span>
          </p>
        )}
      </div>
    </div>
  );
}

// ── Skeleton Row ──────────────────────────────────────────────────────────────
function SkeletonRow() {
  return (
    <tr className="animate-pulse">
      {[...Array(8)].map((_, i) => (
        <td key={i} className="px-4 py-3">
          <div className="h-4 rounded bg-slate-100" style={{ width: `${60 + (i % 3) * 20}%` }} />
        </td>
      ))}
    </tr>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────────
const PAGE_SIZE = 10;

export default function TransactionsTable({ transactions = [], stats, loading = false, error = null }) {
  const [search, setSearch] = useState('');
  const [method, setMethod] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);

  // Normalise a transaction — works for both API shape and legacy mock shape
  const normalise = (t) => ({
    originalId:    t.id,
    id:            t.transaction_no ?? t.id,
    orderId:       t.order_id       ?? t.orderId,
    customerId:    t.customer_id    ?? t.customerId ?? null,
    customerName:  t.customer?.name ?? `Customer #${t.customer_id ?? t.customerId ?? ''}`,
    customerEmail: t.customer?.email ?? t.masked_account ?? '',
    customerAvatar: t.customer?.avatar ?? null,
    datetime:      t.createdAt      ?? t.created_at ?? t.datetime ?? null,
    updatedAt:     t.updatedAt      ?? null,
    method:        t.payment_method ?? t.method   ?? '',
    maskedAccount: t.masked_account ?? t.maskedAccount ?? '',
    status:        t.status         ?? 'PENDING',
    amount:        t.amount         ?? 0,
    currency:      t.currency       ?? 'USD',
    remarks:       t.remarks        ?? '',
  });

  const normalised = transactions.map(normalise);

  // filter
  const filtered = normalised.filter(t => {
    const q = search.toLowerCase();
    const idStr     = String(t.id).toLowerCase();
    const orderStr  = String(t.orderId).toLowerCase();
    const custStr   = t.customerName.toLowerCase();
    const matchQ = !q || idStr.includes(q) || orderStr.includes(q) || custStr.includes(q);
    const matchM = !method || t.method.toUpperCase() === method.toUpperCase();
    const matchS = !status || t.status.toUpperCase() === status.toUpperCase();
    return matchQ && matchM && matchS;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage   = Math.min(page, totalPages);
  const paginated  = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  // Derive stats from live data if not provided externally
  const computedStats = stats ?? (() => {
    const total     = normalised.length;
    const successful = normalised.filter(t => ['COMPLETED','PAID','SUCCESS','paid'].includes(t.status)).length;
    const pending   = normalised.filter(t => ['PENDING','pending'].includes(t.status)).length;
    const failed    = normalised.filter(t => ['FAILED','failed'].includes(t.status)).length;
    const refunded  = normalised.filter(t => ['REFUNDED','refunded'].includes(t.status)).length;
    const totalAmount = normalised.reduce((acc, t) => acc + Number(t.amount || 0), 0);
    return {
      total:       { count: total,      growth: null },
      successful:  { count: successful, growth: null },
      pending:     { count: pending,    growth: null },
      failed:      { count: failed,     growth: null },
      refunded:    { count: refunded,   growth: null },
      totalAmount: { value: totalAmount, growth: null },
    };
  })();

  const formatTime = (dt) => {
    if (!dt) return { date: '—', time: '' };
    const d = new Date(dt);
    return {
      date: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
      time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    };
  };

  // Unique payment methods from real data for filter dropdown
  const uniqueMethods = [...new Set(normalised.map(t => t.method).filter(Boolean))];

  return (
    <div className="space-y-5">
      {/* ── Summary Stats ── */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <StatCard loading={loading} icon={<HiOutlineCreditCard className="h-6 w-6" />} iconBg="bg-blue-500"    label="Total Transactions"      value={computedStats?.total?.count ?? 0}       growth={computedStats?.total?.growth} />
        <StatCard loading={loading} icon={<HiOutlineCheckCircle className="h-6 w-6" />} iconBg="bg-emerald-500" label="Successful Transactions"  value={computedStats?.successful?.count ?? 0}  growth={computedStats?.successful?.growth} />
        <StatCard loading={loading} icon={<HiOutlineClock className="h-6 w-6" />} iconBg="bg-amber-500"   label="Pending Transactions"     value={computedStats?.pending?.count ?? 0}     growth={computedStats?.pending?.growth} />
        <StatCard loading={loading} icon={<HiOutlineXCircle className="h-6 w-6" />}  iconBg="bg-red-500"     label="Failed Transactions"      value={computedStats?.failed?.count ?? 0}      growth={computedStats?.failed?.growth} />
        <StatCard loading={loading} icon={<HiOutlineCurrencyDollar className="h-6 w-6" />}  iconBg="bg-purple-500"  label="Total Amount"             value={computedStats?.totalAmount?.value ?? 0} growth={computedStats?.totalAmount?.growth} prefix="$" />
      </div>

      {/* ── Error Banner ── */}
      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          ⚠️ {error}
        </div>
      )}

      {/* ── Filters bar ── */}
      <div className="card p-4">
        <div className="flex flex-wrap gap-3 items-center">
          {/* Search */}
          <div className="relative flex-1 min-w-[220px]">
            <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by transaction no, order ID, customer..."
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(1); }}
              className="w-full rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm placeholder-slate-400 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>

          {/* Method filter */}
          <select
            value={method}
            onChange={e => { setMethod(e.target.value); setPage(1); }}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
          >
            <option value="">All Payment Methods</option>
            {uniqueMethods.map(m => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>

          {/* Status filter */}
          <select
            value={status}
            onChange={e => { setStatus(e.target.value); setPage(1); }}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
          >
            <option value="">All Status</option>
            <option value="COMPLETED">Successful</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
            <option value="REFUNDED">Refunded</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          {/* Filters button */}
          <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-50 transition-colors">
            <HiOutlineFunnel className="h-4 w-4" />
            Filters
          </button>
        </div>
      </div>

      {/* ── Table ── */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-100">
            <thead>
              <tr className="bg-slate-50">
                {['Transaction No', 'Order ID', 'Date', 'Customer', 'Payment Method', 'Masked Account', 'Status', 'Amount', 'Action'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {loading ? (
                [...Array(5)].map((_, i) => <SkeletonRow key={i} />)
              ) : paginated.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-16 text-center text-sm text-slate-400">
                    {error ? 'Could not load transactions.' : 'No transactions found.'}
                  </td>
                </tr>
              ) : paginated.map((t, idx) => {
                const { date, time } = formatTime(t.datetime);
                const st = STATUS_MAP[t.status] || STATUS_MAP['PENDING'];
                return (
                  <tr key={`${t.id}-${idx}`} className="hover:bg-slate-50 transition-colors">
                    {/* Transaction No */}
                    <td className="px-4 py-3 font-mono text-xs font-semibold text-slate-700 whitespace-nowrap">
                      {t.id}
                    </td>

                    {/* Order ID */}
                    <td className="px-4 py-3 text-sm font-medium text-slate-600 whitespace-nowrap">
                      {t.orderId != null ? `#${t.orderId}` : '—'}
                    </td>

                    {/* Date */}
                    <td className="px-4 py-3 whitespace-nowrap">
                      <p className="text-sm font-medium text-slate-800">{date}</p>
                      <p className="text-xs text-slate-400">{time}</p>
                    </td>

                    {/* Customer */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2.5">
                        {t.customerAvatar ? (
                          <img
                            src={t.customerAvatar}
                            alt={t.customerName}
                            className="h-8 w-8 rounded-full object-cover flex-shrink-0"
                          />
                        ) : (
                          <div className="h-8 w-8 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center flex-shrink-0 text-white text-xs font-bold">
                            {t.customerName?.charAt(0)?.toUpperCase() ?? '?'}
                          </div>
                        )}
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-slate-800 truncate">{t.customerName}</p>
                          {t.customerEmail && (
                            <p className="text-xs text-slate-400 truncate">{t.customerEmail}</p>
                          )}
                        </div>
                      </div>
                    </td>

                    {/* Payment Method */}
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        {METHOD_ICON(t.method)}
                        <span className="text-xs text-slate-600">{t.method}</span>
                      </div>
                    </td>

                    {/* Masked Account */}
                    <td className="px-4 py-3 whitespace-nowrap font-mono text-xs text-slate-500">
                      {t.maskedAccount || '—'}
                    </td>

                    {/* Status */}
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge variant={st?.variant ?? 'default'} dot>{st?.label ?? t.status}</Badge>
                    </td>

                    {/* Amount */}
                    <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-slate-800 tabular-nums">
                      {t.currency} {formatCurrency(t.amount)}
                    </td>

                    {/* Action */}
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Link
                        to={`/dashboard/transactions/${t.originalId}`}
                        title={t.remarks || 'View details'}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-blue-50 hover:text-blue-600 transition-colors"
                      >
                        <HiOutlineEye className="h-4 w-4" />
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* ── Pagination ── */}
        <div className="flex items-center justify-between border-t border-slate-100 bg-white px-4 py-3">
          <p className="text-sm text-slate-500">
            Showing {filtered.length === 0 ? 0 : (safePage - 1) * PAGE_SIZE + 1} to{' '}
            {Math.min(safePage * PAGE_SIZE, filtered.length)} of{' '}
            {filtered.length.toLocaleString()} results
          </p>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={safePage === 1}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              <HiOutlineChevronLeft className="h-4 w-4" />
            </button>

            {[...Array(Math.min(totalPages, 3))].map((_, i) => {
              const pg = i + 1;
              return (
                <button
                  key={pg}
                  onClick={() => setPage(pg)}
                  className={cn(
                    'inline-flex h-8 w-8 items-center justify-center rounded-lg text-sm font-medium border transition-colors',
                    safePage === pg
                      ? 'border-blue-600 bg-blue-600 text-white'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  )}
                >
                  {pg}
                </button>
              );
            })}

            {totalPages > 3 && (
              <>
                <span className="px-1 text-slate-400">…</span>
                <button
                  onClick={() => setPage(totalPages)}
                  className={cn(
                    'inline-flex h-8 w-8 items-center justify-center rounded-lg text-sm font-medium border transition-colors',
                    safePage === totalPages
                      ? 'border-blue-600 bg-blue-600 text-white'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  )}
                >
                  {totalPages}
                </button>
              </>
            )}

            <button
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={safePage === totalPages}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              <HiOutlineChevronRight className="h-4 w-4" />
            </button>

            <select
              value={PAGE_SIZE}
              readOnly
              className="ml-2 rounded-lg border border-slate-200 bg-white px-2 py-1 text-sm text-slate-600"
            >
              <option>10 / page</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );
}
