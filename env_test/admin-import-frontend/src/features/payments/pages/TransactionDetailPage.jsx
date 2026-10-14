import React from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PageContainer from '@/components/layouts/PageContainer';
import { PageHeader } from '@/components/ui';
import Badge from '@/components/ui/Badge';
import { useTransactionDetail } from '../hooks/useTransactionDetail';
import { formatCurrency } from '@/utils/formatCurrency';
import { 
  HiOutlineCreditCard, 
  HiOutlineUser, 
  HiOutlineShoppingCart 
} from 'react-icons/hi2';

export default function TransactionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { transaction, loading, error } = useTransactionDetail(id);

  if (loading) {
    return (
      <PageContainer>
        <div className="flex h-64 items-center justify-center">
          <div className="text-center text-slate-500 animate-pulse">Loading transaction details...</div>
        </div>
      </PageContainer>
    );
  }

  if (error || !transaction) {
    return (
      <PageContainer>
        <div className="flex h-64 flex-col items-center justify-center gap-4">
          <div className="text-red-500 font-medium">{error || 'Transaction not found.'}</div>
          <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline">
            Go back
          </button>
        </div>
      </PageContainer>
    );
  }

  const {
    amount,
    currency,
    status,
    remarks,
    createdAt,
    updatedAt,
    transaction_no,
    order_id,
    customer_id,
    payment_method,
    masked_account,
  } = transaction;

  const STATUS_MAP = {
    COMPLETED:  { label: 'Successful', variant: 'success' },
    PAID:       { label: 'Paid',       variant: 'success' },
    SUCCESS:    { label: 'Successful', variant: 'success' },
    PENDING:    { label: 'Pending',    variant: 'warning' },
    FAILED:     { label: 'Failed',     variant: 'danger'  },
    REFUNDED:   { label: 'Refunded',   variant: 'default' },
    CANCELLED:  { label: 'Cancelled',  variant: 'default' },
  };

  const st = STATUS_MAP[status?.toUpperCase()] || STATUS_MAP['PENDING'];

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  };

  return (
    <PageContainer>
      <PageHeader
        title="Transaction Details"
        crumbs={[
          { label: 'Dashboard', path: '/' },
          { label: 'Transactions', path: '/dashboard/transactions' },
          { label: transaction_no || id },
        ]}
      />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <div className="space-y-6 lg:col-span-2">
          <div className="card p-6">
            <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
              <div>
                <h2 className="text-4xl font-bold text-slate-900 tabular-nums">
                  {currency || 'USD'} {formatCurrency(amount || 0)}
                </h2>
                <div className="mt-3 flex flex-wrap items-center gap-3">
                  <Badge variant={st.variant} className="uppercase px-2.5 py-1 text-xs">
                    {st.label || status}
                  </Badge>
                  <span className="text-sm font-mono text-slate-500 bg-slate-100 px-2.5 py-1 rounded-md">
                    {transaction_no || id}
                  </span>
                </div>
              </div>
            </div>

            {remarks && (
              <div className="mt-6 rounded-xl bg-slate-50 p-4 border border-slate-100">
                <h4 className="text-sm font-semibold text-slate-700 mb-1.5">Remarks</h4>
                <p className="text-sm text-slate-600">{remarks}</p>
              </div>
            )}
          </div>

          <div className="card p-6">
            <h3 className="mb-5 text-base font-bold text-slate-900">Timeline</h3>
            <div className="relative border-l-2 border-slate-100 ml-2.5 space-y-8">
              {/* Created */}
              <div className="relative pl-6">
                <div className="absolute -left-[9px] top-1.5 h-4 w-4 rounded-full border-[3px] border-white bg-blue-500 shadow-sm" />
                <h4 className="text-sm font-semibold text-slate-800">Transaction Created</h4>
                <p className="text-sm text-slate-500 mt-1">{formatDate(createdAt)}</p>
              </div>

              {/* Updated */}
              {updatedAt && updatedAt !== createdAt && (
                <div className="relative pl-6">
                  <div className="absolute -left-[9px] top-1.5 h-4 w-4 rounded-full border-[3px] border-white bg-slate-400 shadow-sm" />
                  <h4 className="text-sm font-semibold text-slate-800">Last Updated</h4>
                  <p className="text-sm text-slate-500 mt-1">{formatDate(updatedAt)}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar Info */}
        <div className="space-y-6">
          <div className="card p-6">
            <h3 className="mb-5 text-base font-bold text-slate-900">Details</h3>
            
            <div className="space-y-6">
              <div className="flex items-start gap-4">
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                  <HiOutlineCreditCard className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Payment Method</p>
                  <p className="text-sm font-bold text-slate-900 mt-0.5">{payment_method || '—'}</p>
                  {masked_account && (
                    <p className="text-xs font-mono text-slate-500 mt-1 truncate">{masked_account}</p>
                  )}
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                  <HiOutlineUser className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Customer</p>
                  {customer_id ? (
                    <Link to={`/dashboard/customers/${customer_id}`} className="text-sm font-bold text-indigo-600 hover:text-indigo-700 hover:underline mt-0.5 block truncate">
                      Customer #{customer_id}
                    </Link>
                  ) : (
                    <p className="text-sm font-bold text-slate-900 mt-0.5">—</p>
                  )}
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
                  <HiOutlineShoppingCart className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Order</p>
                  {order_id ? (
                    <Link to={`/dashboard/orders/${order_id}`} className="text-sm font-bold text-emerald-600 hover:text-emerald-700 hover:underline mt-0.5 block truncate">
                      Order #{order_id}
                    </Link>
                  ) : (
                    <p className="text-sm font-bold text-slate-900 mt-0.5">—</p>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
