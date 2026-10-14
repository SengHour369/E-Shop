// src/features/payments/pages/BakongCallbackPage.jsx
import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PageContainer from '@/components/layouts/PageContainer';
import { PageHeader, Loader } from '@/components/ui';
import Badge from '@/components/ui/Badge';
import { useBakongCallbackData } from '../hooks/useBakongCallbackData';
import { useBakongCallback } from '../hooks/useBakongCallback';
import { useOrderDetail } from '../../orders/hooks/useOrderDetail';
import {
  HiOutlineCheckCircle,
  HiOutlineClock,
  HiOutlineDocumentDuplicate,
  HiOutlineShieldCheck,
  HiOutlineExclamationCircle,
  HiOutlineXCircle,
  HiOutlineArrowPath,
  HiOutlineClipboardDocumentList,
  HiOutlineShoppingBag,
} from 'react-icons/hi2';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (dt) => {
  if (!dt) return '—';
  const d = new Date(dt);
  if (isNaN(d)) return String(dt);
  return d.toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric',
    hour: 'numeric', minute: '2-digit', hour12: true,
  });
};

const copyText = (text) => {
  if (text && text !== '—') navigator.clipboard?.writeText(String(text)).catch(() => { });
};

const buildAmountDisplay = (p) => {
  if (!p) return '—';
  const khr = p.amount_khr ? ` (KHR ${Number(p.amount_khr).toLocaleString()})` : '';
  return p.amount ? `$${Number(p.amount).toFixed(2)}${khr}` : '—';
};

const STATUS_VARIANT = { SUCCESS: 'success', PAID: 'success', COMPLETED: 'success', FAILED: 'danger', PENDING: 'warning' };

// ─── Sub-components ───────────────────────────────────────────────────────────

function InfoRow({ label, value, valueNode, mono = false, copyable = false }) {
  if ((value === null || value === undefined || value === '—' || value === '') && !valueNode) return null;
  const text = value ? String(value) : '';
  return (
    <div className="flex justify-between items-start py-2.5 border-b border-slate-50 last:border-0">
      <span className="text-sm text-slate-500 flex-shrink-0 mr-4">{label}</span>
      <span className={`text-sm font-medium text-slate-900 text-right break-all flex items-center gap-1.5 ${mono ? 'font-mono' : ''}`}>
        {valueNode || text}
        {copyable && text && (
          <button onClick={() => copyText(text)} className="shrink-0 text-slate-400 hover:text-blue-600 transition-colors">
            <HiOutlineDocumentDuplicate className="w-3.5 h-3.5" />
          </button>
        )}
      </span>
    </div>
  );
}

function ProcessingHistory({ history }) {
  if (history.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-6 text-center">
        <HiOutlineClock className="w-8 h-8 text-slate-300 mb-2" />
        <p className="text-sm font-medium text-slate-500">No processing history yet</p>
        <p className="text-xs text-slate-400 mt-1">Submit the form below to process this callback</p>
      </div>
    );
  }

  return (
    <div className="relative pl-6 space-y-5 before:absolute before:inset-y-2 before:left-[18px] before:w-px before:bg-slate-200">
      {/* Dynamic submitted entries */}
      {history.map((entry) => (
        <div key={entry.id} className="relative flex gap-4">
          <div className="absolute -left-[30px] top-0.5 bg-white rounded-full">
            {entry.status === 'SUCCESS'
              ? <HiOutlineCheckCircle className="w-5 h-5 text-emerald-500" />
              : <HiOutlineXCircle className="w-5 h-5 text-red-500" />}
          </div>
          <div className="flex-1">
            <p className="text-sm font-semibold text-slate-900 flex items-center gap-2">
              Submitted
              <Badge variant={STATUS_VARIANT[entry.status] ?? 'default'} dot>{entry.status}</Badge>
            </p>
            <p className="text-xs text-slate-500 mt-0.5 font-mono truncate">{entry.transactionId}</p>
            <p className="text-[11px] text-slate-400 mt-0.5">{fmt(entry.processedAt)}</p>
            {entry.adminNote && (
              <p className="text-xs text-slate-500 mt-1 italic">"{entry.adminNote}"</p>
            )}
          </div>
        </div>
      ))}

    </div>
  );
}

/** Callback History Log Table — only visible after ≥1 submission */
function CallbackHistoryLog({ history }) {
  if (history.length === 0) return null;
  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
      <div className="px-6 py-4 border-b border-slate-50 flex items-center gap-2">
        <HiOutlineClipboardDocumentList className="w-5 h-5 text-slate-400" />
        <h3 className="text-base font-bold text-slate-900">Callback History</h3>
        <span className="ml-auto text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-100 px-2 py-0.5 rounded-full">
          {history.length} submitted
        </span>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-50 text-sm">
          <thead>
            <tr className="bg-slate-50">
              {['#', 'Order Number', 'Transaction ID', 'Status', 'Note', 'Processed At'].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wide text-slate-400 whitespace-nowrap">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50 bg-white">
            {history.map((entry, idx) => (
              <tr key={entry.id} className="hover:bg-slate-50/70 transition-colors">
                <td className="px-4 py-3 text-xs text-slate-400 font-medium">{history.length - idx}</td>
                <td className="px-4 py-3 font-medium text-slate-800 whitespace-nowrap">{entry.orderNumber}</td>
                <td className="px-4 py-3 font-mono text-xs text-slate-700 whitespace-nowrap">
                  <span className="flex items-center gap-1">
                    {entry.transactionId.length > 18
                      ? `${entry.transactionId.slice(0, 18)}…`
                      : entry.transactionId}
                    <button onClick={() => copyText(entry.transactionId)} className="text-slate-400 hover:text-blue-600">
                      <HiOutlineDocumentDuplicate className="w-3.5 h-3.5" />
                    </button>
                  </span>
                </td>
                <td className="px-4 py-3 whitespace-nowrap">
                  <Badge variant={STATUS_VARIANT[entry.status] ?? 'default'} dot>{entry.status}</Badge>
                </td>
                <td className="px-4 py-3 text-xs text-slate-500 max-w-[140px] truncate">
                  {entry.adminNote || <span className="text-slate-300">—</span>}
                </td>
                <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">{fmt(entry.processedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/** Loading skeleton */
function PageSkeleton() {
  const Bone = ({ className }) => <div className={`bg-slate-100 rounded-lg animate-pulse ${className}`} />;
  return (
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <div className="xl:col-span-2 space-y-6">
        {[1, 2, 3].map((i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-100 p-6 space-y-4">
            <Bone className="h-5 w-48" /><Bone className="h-4 w-full" /><Bone className="h-4 w-3/4" />
          </div>
        ))}
      </div>
      <div className="space-y-6">
        {[1, 2].map((i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-100 p-6 space-y-4">
            <Bone className="h-5 w-36" />
            {[1, 2, 3, 4].map((j) => (
              <div key={j} className="flex justify-between">
                <Bone className="h-4 w-24" /><Bone className="h-4 w-28" />
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export default function BakongCallbackPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { payment, loading, error, refetch } = useBakongCallbackData(id);
  const { isProcessing, callbackHistory, submitCallback } = useBakongCallback();

  // Editable params — pre-filled from real data
  const [orderNumber, setOrderNumber] = useState('');
  const [paymentId, setPaymentId] = useState('');
  const [action, setAction] = useState('SUCCESS');
  const [adminNote, setNote] = useState('');

  useEffect(() => {
    if (payment) {
      setOrderNumber(payment.order_number ?? payment.codeOrder ?? '');
      setPaymentId(payment.code ?? payment.id ?? '');
    }
  }, [payment]);

  const lastEntry = callbackHistory[0];
  const pmtStatus = lastEntry
    ? (lastEntry.status === 'SUCCESS' ? 'PAID' : 'FAILED')
    : (payment?.status ?? 'PENDING');

  const callbackId = payment?.code ?? `#CBK-${id}`;

  const handleProcess = async () => {
    const ok = await submitCallback({ orderNumber, transactionId: paymentId, status: action, adminNote });
    if (ok) setNote('');
  };

  const rawPayload = {
    responseCode: '0000',
    responseDesc: 'Successful',
    requestorReference: paymentId || (payment?.code ?? ''),
    bakongTransactionId: paymentId || (payment?.code ?? ''),
    amount: payment?.amount ?? 0,
    currency: payment?.currency ?? 'KHR',
    transactionDate: payment?.payment_date ?? payment?.datetime ?? new Date().toISOString(),
    additionalData: { billNumber: orderNumber || (payment?.order_number ?? '') },
  };

  // ── States ───────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <PageContainer>
        <PageHeader
          title="Process Bakong Payment Callback"
          crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Payments', path: '/dashboard/payments' }, { label: 'Bakong Callback' }]}
        />
        <PageSkeleton />
      </PageContainer>
    );
  }

  if (error || !payment) {
    return (
      <PageContainer>
        <PageHeader
          title="Process Bakong Payment Callback"
          crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Payments', path: '/dashboard/payments' }, { label: 'Bakong Callback' }]}
        />
        <div className="flex flex-col items-center justify-center h-64 gap-4">
          <HiOutlineExclamationCircle className="w-12 h-12 text-red-400" />
          <p className="text-red-500 font-medium text-sm">{error ?? 'Payment record not found.'}</p>
          <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline text-sm">← Go back</button>
        </div>
      </PageContainer>
    );
  }

  const bakongResponse = {
    responseCode: lastEntry?.response?.responseCode ?? lastEntry?.response?.code ?? '—',
    description: lastEntry?.response?.responseDesc ?? lastEntry?.response?.message ?? '—',
    txnId: lastEntry?.response?.bakongTransactionId ?? lastEntry?.response?.transactionId ?? payment.transaction_id ?? '—',
    txnDate: payment.payment_date ?? payment.datetime ?? null,
    amount: payment.amount_khr
      ? `KHR ${Number(payment.amount_khr).toLocaleString()}`
      : payment.amount
        ? `${payment.currency ?? 'KHR'} ${Number(payment.amount).toLocaleString()}`
        : '—',
    currency: payment.currency ?? 'KHR',
  };

  // ─────────────────────────────────────────────────────────────────────────────
  return (
    <PageContainer>

      <PageHeader
        title="Process Bakong Payment Callback"
        crumbs={[
          { label: 'Dashboard', path: '/' },
          { label: 'Payments', path: '/dashboard/payments' },
          { label: 'Bakong Callback', path: '/dashboard/payments' },
          { label: callbackId },
        ]}
      />

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">

        {/* ══════════════════ MAIN COLUMN ══════════════════ */}
        <div className="xl:col-span-2 space-y-6">

          {/* ── 1. Callback Information ─────────────────────────────────────── */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
            <div className="px-6 py-4 border-b border-slate-50">
              <div className="flex items-center gap-2 flex-wrap">
                <h3 className="text-base font-bold text-slate-900">Callback Information</h3>
                {/* Real status badge derived from payment.status */}
                <Badge variant={STATUS_VARIANT[payment.status?.toUpperCase()] ?? 'warning'}>
                  {(payment.status ?? 'PENDING').toUpperCase()}
                </Badge>
                {callbackHistory.length > 0 && (
                  <Badge variant="info">{callbackHistory.length} Processed</Badge>
                )}
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Review and process payment callback for {payment.payment_method ?? payment.method ?? 'Bakong'}.
              </p>
            </div>
            <div className="px-6 py-5 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-x-4 gap-y-5">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400 mb-1">Callback ID</p>
                <p className="text-sm font-semibold text-slate-900 flex items-center gap-1.5">
                  {callbackId}
                  <button onClick={() => copyText(callbackId)} className="text-slate-400 hover:text-blue-600">
                    <HiOutlineDocumentDuplicate className="w-3.5 h-3.5" />
                  </button>
                </p>
              </div>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400 mb-1">Received At</p>
                <p className="text-sm font-medium text-slate-900">
                  {fmt(payment.payment_date ?? payment.datetime)}
                </p>
              </div>
              {(payment.source_ip || payment.ip_address) && (
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400 mb-1">Source IP</p>
                  <p className="text-sm font-medium text-slate-900">
                    {payment.source_ip ?? payment.ip_address}
                  </p>
                </div>
              )}
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400 mb-1">Callback Status</p>
                {/* Real callback status: received if any transaction_id exists */}
                <Badge
                  variant={payment.transaction_id ? 'success' : 'warning'}
                  dot
                >
                  {payment.transaction_id ? 'RECEIVED' : 'PENDING'}
                </Badge>
              </div>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400 mb-1">Processed</p>
                {/* Real processed status: based on payment.status from API */}
                {['PAID', 'SUCCESS', 'COMPLETED'].includes((payment.status ?? '').toUpperCase())
                  ? <Badge variant="success" dot>Yes</Badge>
                  : <span className="text-sm font-semibold text-red-500">No</span>
                }
              </div>
            </div>
          </div>

          {/* ── 2. Payment Information ───────────────────────────────────────── */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
            <div className="px-6 py-4 border-b border-slate-50">
              <h3 className="text-base font-bold text-slate-900">Payment Information</h3>
            </div>
            <div className="px-6 py-5 grid grid-cols-1 sm:grid-cols-2 gap-x-10">
              <div>
                <InfoRow label="Order ID" valueNode={
                  payment.order_id ? (
                    <div className="flex items-center gap-3">
                      <span className="font-semibold text-slate-800">#{payment.order_id}</span>
                      <Link 
                        to={`/dashboard/orders/${payment.order_id}`} 
                        state={{ userId: payment.user_id || payment.customer_id || 1 }} 
                        className="px-2 py-1 bg-blue-50 hover:bg-blue-100 text-blue-600 text-[10px] uppercase font-bold tracking-wider rounded transition-colors"
                      >
                        View Detail
                      </Link>
                    </div>
                  ) : '—'
                } />
                <InfoRow label="Order Number" value={payment.order_number ?? payment.codeOrder ?? '—'} />
                <InfoRow label="Payment Method" value={(payment.payment_method ?? payment.method ?? '—').toUpperCase()} />
                <InfoRow label="Payment Provider" value={(payment.payment_provider ?? 'BAKONG').toUpperCase()} />
              </div>
              <div>
                <InfoRow label="Amount" value={buildAmountDisplay(payment)} />
                <InfoRow label="Currency" value={payment.currency ?? '—'} />
                <div className="flex justify-between items-center py-2.5 border-b border-slate-50">
                  <span className="text-sm text-slate-500">Payment Status</span>
                  <Badge variant={STATUS_VARIANT[pmtStatus.toUpperCase()] ?? 'warning'} dot>
                    {pmtStatus.toUpperCase()}
                  </Badge>
                </div>
                <InfoRow label="Bakong Txn ID" value={payment.transaction_id ?? '—'} mono copyable />
              </div>
            </div>
          </div>

          {/* End of Main Content Column */}


        </div>

        {/* ══════════════════ RIGHT SIDEBAR ══════════════════════ */}
        <div className="space-y-6">

          {/* ── Bakong Response ──────────────────────────────────────────────── */}
          {lastEntry?.response && (
            <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
              <div className="px-6 py-4 border-b border-slate-50">
                <h3 className="text-base font-bold text-slate-900">Bakong Response</h3>
              </div>
              <div className="px-6 py-5">
                <div className="flex justify-between items-center py-2.5 border-b border-slate-50">
                  <span className="text-sm text-slate-500">Response Code</span>
                  <span className="bg-emerald-50 text-emerald-700 border border-emerald-100 px-2 py-0.5 rounded font-mono text-xs font-semibold">
                    {bakongResponse.responseCode}
                  </span>
                </div>
                <InfoRow label="Response Desc" value={bakongResponse.description} />
                <InfoRow label="Bakong Txn ID" value={bakongResponse.txnId} mono copyable />
                <InfoRow label="Transaction Date" value={fmt(bakongResponse.txnDate)} />
                <InfoRow label="Amount" value={bakongResponse.amount} />
                <InfoRow label="Currency" value={bakongResponse.currency} />
              </div>
            </div>
          )}

          {/* ── Process Action — single, consolidated form ───────────────────── */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm">
            <div className="px-6 py-4 border-b border-slate-50">
              <h3 className="text-base font-bold text-slate-900">Process Action</h3>
              <p className="text-xs text-slate-500 mt-1">
                Edit params and submit. Multiple callbacks are supported.
              </p>
            </div>
            <div className="px-6 py-5 space-y-4">

              {/* Last submission indicator */}
              {lastEntry && (
                <div className="flex items-center gap-2 bg-blue-50 border border-blue-100 rounded-xl px-3 py-2.5">
                  <HiOutlineCheckCircle className="w-4 h-4 text-blue-600 shrink-0" />
                  <p className="text-xs text-blue-700">
                    Last: <strong>{lastEntry.status}</strong> · {fmt(lastEntry.processedAt)}
                  </p>
                </div>
              )}

              {/* Order Number */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                  Order Number <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={orderNumber}
                  onChange={(e) => setOrderNumber(e.target.value)}
                  placeholder="ORD-XXXXXXXX-XXXX"
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow"
                />
              </div>

              {/* Payment ID */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                  Payment ID <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={paymentId}
                  onChange={(e) => setPaymentId(e.target.value)}
                  placeholder="PAY-XXXXXXXX"
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm font-mono text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow"
                />
              </div>

              {/* Status */}
              <div>
                <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                  Status <span className="text-red-400">*</span>
                </p>
                <div className="flex gap-2">
                  {[
                    { value: 'SUCCESS', color: 'emerald' },
                    { value: 'FAILED', color: 'red' },
                    { value: 'PENDING', color: 'amber' },
                  ].map(({ value, color }) => (
                    <label
                      key={value}
                      className={`flex-1 flex items-center justify-center gap-1.5 cursor-pointer rounded-lg border py-2 text-sm font-semibold transition-colors ${action === value
                          ? color === 'emerald'
                            ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                            : color === 'red'
                              ? 'border-red-200 bg-red-50 text-red-700'
                              : 'border-amber-200 bg-amber-50 text-amber-700'
                          : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300'
                        }`}
                    >
                      <input
                        type="radio"
                        name="cb-status"
                        value={value}
                        checked={action === value}
                        onChange={() => setAction(value)}
                        className="sr-only"
                      />
                      {value}
                    </label>
                  ))}
                </div>
              </div>

              {/* Admin Note */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
                  Admin Note <span className="font-normal text-slate-400">(Optional)</span>
                </label>
                <textarea
                  id="admin-note"
                  rows={2}
                  maxLength={500}
                  value={adminNote}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Note for this submission…"
                  className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-shadow"
                />
                <p className="text-right text-[11px] text-slate-400 mt-0.5">{adminNote.length}/500</p>
              </div>

              {/* Single action row */}
              <div className="flex gap-3 pt-1">
                <button
                  id="btn-cancel"
                  onClick={() => navigate(-1)}
                  className="flex-1 border border-slate-200 bg-white text-slate-700 text-sm font-semibold py-2.5 rounded-xl hover:bg-slate-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  id="btn-process"
                  onClick={handleProcess}
                  disabled={isProcessing}
                  className="flex-1 inline-flex items-center justify-center gap-2 text-sm font-semibold py-2.5 rounded-xl shadow-sm bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-60 transition-colors"
                >
                  {isProcessing
                    ? <><Loader size="sm" className="text-white" /> Processing…</>
                    : <><HiOutlineArrowPath className="w-4 h-4" /> Process Callback</>
                  }
                </button>
              </div>

            </div>
          </div>

        </div>

      </div>
    </PageContainer>
  );
}
