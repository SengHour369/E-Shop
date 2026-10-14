import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import PageContainer from '@/components/layouts/PageContainer';
import { PageHeader, PaymentDetailSkeleton } from '@/components/ui';
import { paymentService } from '../services/payment.service';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { HiOutlineCheckCircle, HiOutlineXCircle, HiOutlineClock } from 'react-icons/hi2';

export default function PaymentDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const res = await paymentService.getById(id);
        const data = res.data?.payload || res.data;
        if (data) {
          setPayment(data);
        } else {
          // If BY_ID is not implemented, let's fallback to fetch from list for demo
          const allRes = await paymentService.getAll({ page: 1, size: 100 });
          const list = allRes.data?.payload || [];
          const found = list.find(p => String(p.id) === String(id));
          if (found) setPayment(found);
          else setError('Payment not found.');
        }
      } catch (err) {
        console.error(err);
        // Fallback for demo
        try {
          const allRes = await paymentService.getAll({ page: 1, size: 100 });
          const list = allRes.data?.payload || [];
          const found = list.find(p => String(p.id) === String(id));
          if (found) {
            setPayment(found);
            setError(null);
          } else {
            setError('Payment not found.');
          }
        } catch (e) {
          setError('Failed to fetch payment details.');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchDetail();
  }, [id]);

  if (loading) return (
    <PageContainer>
      <PaymentDetailSkeleton />
    </PageContainer>
  );
  if (error || !payment) return (
    <PageContainer>
      <div className="py-12 text-center text-red-500">{error || 'Payment not found.'}</div>
    </PageContainer>
  );

  const stStr = (payment.status || 'PENDING').toUpperCase();

  // Real API returns: COMPLETED | SUCCESS | PAID | PENDING | FAILED | CANCELLED | REFUNDED
  const isSuccess  = ['SUCCESS', 'PAID', 'COMPLETED'].includes(stStr);
  const isFailed   = ['FAILED', 'CANCELLED'].includes(stStr);
  const isPending  = !isSuccess && !isFailed;

  const statusBadgeVariant = isSuccess ? 'success' : isFailed ? 'danger' : 'warning';

  // Human-readable label
  const STATUS_LABEL = {
    COMPLETED: 'Completed',
    SUCCESS:   'Success',
    PAID:      'Paid',
    PENDING:   'Pending',
    FAILED:    'Failed',
    CANCELLED: 'Cancelled',
    REFUNDED:  'Refunded',
  };
  const statusLabel = STATUS_LABEL[stStr] ?? stStr;

  const formatFullDate = (dt) => {
    if (!dt) return '-';
    return new Date(dt).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit', hour12: true });
  };
  const payDateStr = formatFullDate(payment.payment_date || payment.datetime);

  return (
    <PageContainer>
      <PageHeader
        title="Payment Details"
        crumbs={[
          { label: "Dashboard", path: "/" },
          { label: "Payments", path: "/dashboard/payments" },
          { label: payment.code || id }
        ]}
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Main Info Card */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-3">
                <h3 className="text-sm font-medium text-slate-500">Payment Code: {payment.code || payment.id}</h3>
                <Badge variant={statusBadgeVariant}>{statusLabel}</Badge>
              </div>
            </div>
            <div className="mb-4">
              <h1 className="text-4xl font-bold text-slate-900 tracking-tight">
                {payment.amount} {payment.currency || 'KHR'}
              </h1>
              <p className="text-sm text-slate-500 mt-2">
                {isFailed
                  ? 'Bakong reported the payment could not be completed.'
                  : isSuccess
                  ? 'Payment confirmed and settled via Bakong callback.'
                  : 'Waiting for the customer to scan and pay the KHQR code.'}
              </p>
            </div>
            {/* Method pill — color reflects real status */}
            <div className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold
              ${ isSuccess ? 'bg-emerald-50 text-emerald-700' :
                 isFailed  ? 'bg-red-50 text-red-700' :
                             'bg-amber-50 text-amber-700' }`}>
              <span className={`h-2 w-2 rounded-full
                ${ isSuccess ? 'bg-emerald-500' : isFailed ? 'bg-red-500' : 'bg-amber-500' }`} />
              {payment.payment_method || 'BAKONG'} - KHQR
            </div>
          </div>

          {/* Transaction Info */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h3 className="text-base font-bold text-slate-900 mb-6">Transaction Info</h3>
            <div className="space-y-4 text-sm">
              <div className="flex justify-between py-2 border-b border-slate-50">
                <span className="text-slate-500">Payment ID</span>
                <span className="font-mono text-slate-900">{payment.id}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-50">
                <span className="text-slate-500">Transaction ID</span>
                <span className="font-mono text-slate-900 break-all ml-4">{payment.transaction_id || '-'}</span>
              </div>
              <div className="flex justify-between py-2 border-b border-slate-50">
                <span className="text-slate-500">Payment Date</span>
                <span className="text-slate-900">{payDateStr}</span>
              </div>
              <div className="flex justify-between py-2">
                <span className="text-slate-500">Status</span>
                <Badge variant={statusBadgeVariant}>{statusLabel}</Badge>
              </div>
            </div>
          </div>

          {/* Action Panel */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h3 className="text-base font-bold text-slate-900 mb-6">Action Panel</h3>
            
            {/* ── Failed / Cancelled ──────────────────────────────────── */}
            {isFailed && (
              <div className="rounded-xl border border-red-100 bg-red-50/50 p-5 mb-5">
                <div className="flex items-start gap-3">
                  <HiOutlineXCircle className="h-6 w-6 text-red-600 mt-0.5" />
                  <div>
                    <h4 className="font-semibold text-red-900">Payment {statusLabel}</h4>
                    <p className="text-sm text-red-700 mt-1">
                      {payDateStr}. Bakong callback reported a failure. Customer will need to retry checkout.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* ── Success / Completed / Paid ──────────────────────────── */}
            {isSuccess && (
              <div className="rounded-xl border border-emerald-100 bg-emerald-50/50 p-5 mb-5">
                <div className="flex items-start gap-3">
                  <HiOutlineCheckCircle className="h-6 w-6 text-emerald-600 mt-0.5" />
                  <div>
                    <h4 className="font-semibold text-emerald-900">Payment {statusLabel}</h4>
                    <p className="text-sm text-emerald-700 mt-1">
                      {payDateStr}. Bakong callback verified the transaction and matched it to Order {payment.order_number || payment.order_id}.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* ── Pending ─────────────────────────────────────────────── */}
            {isPending && (
              <div className="rounded-xl border border-amber-100 bg-amber-50/50 p-5 mb-5">
                <div className="flex items-start gap-3">
                  <HiOutlineClock className="h-6 w-6 text-amber-600 mt-0.5" />
                  <div>
                    <h4 className="font-semibold text-amber-900">Waiting for Bakong confirmation</h4>
                    <p className="text-sm text-amber-700 mt-1">
                      QR generated {payDateStr}. Status updates automatically when Bakong sends the callback.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* ── Action buttons ──────────────────────────────────────── */}
            {isFailed && (
              <>
                <button className="w-full rounded-xl border border-slate-200 bg-white py-2.5 text-sm font-semibold text-slate-800 shadow-sm hover:bg-slate-50 transition-all">
                  Generate New QR
                </button>
                <button
                  onClick={() => navigate(`/dashboard/payments/bakong-callback/${id}`)}
                  className="w-full mt-2 rounded-xl border border-blue-200 bg-blue-50 py-2.5 text-sm font-semibold text-blue-800 shadow-sm hover:bg-blue-100 transition-all flex items-center justify-center gap-2"
                >
                  <HiOutlineCheckCircle className="h-4 w-4 text-blue-500" />
                  Process Callback
                </button>
                <p className="text-xs text-slate-400 mt-3 text-center">Creates a fresh KHQR code for the customer to retry.</p>
              </>
            )}

            {isPending && (
              <>
                <button className="w-full rounded-xl border border-slate-200 bg-white py-2.5 text-sm font-semibold text-slate-800 shadow-sm hover:bg-slate-50 transition-all flex items-center justify-center gap-2">
                  <HiOutlineClock className="h-4 w-4 text-amber-500" />
                  Check Status Now
                </button>
                <button
                  onClick={() => navigate(`/dashboard/payments/bakong-callback/${id}`)}
                  className="w-full mt-2 rounded-xl border border-blue-200 bg-blue-50 py-2.5 text-sm font-semibold text-blue-800 shadow-sm hover:bg-blue-100 transition-all flex items-center justify-center gap-2"
                >
                  <HiOutlineCheckCircle className="h-4 w-4 text-blue-500" />
                  Process Callback
                </button>
                <p className="text-xs text-slate-400 mt-3 text-center">Manually triggers callback processing in case the Bakong webhook was missed.</p>
              </>
            )}

            {isSuccess && (
              <p className="text-xs text-slate-400 text-center">No action required — payment is settled. Refund flows are handled from the related Return.</p>
            )}
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-6">
          
          {/* Associated Order */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h3 className="text-base font-bold text-slate-900 mb-6">Associated Order</h3>
            <div className="space-y-4 text-sm">
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Order Number</span>
                <span className="text-slate-900">{payment.order_number || '-'}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Order Code</span>
                <span className="text-slate-900">{payment.codeOrder || '-'}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Order ID</span>
                <span className="text-slate-900">{payment.order_id || '-'}</span>
              </div>
              <div className="pt-2">
                <Link 
                  to={`/dashboard/orders/${payment.order_id || payment.order_number || payment.codeOrder}`} 
                  state={{ userId: payment.user_id || payment.customer_id || payment.customerId || 1 }}
                  className="text-blue-600 hover:text-blue-700 font-medium text-sm inline-flex items-center gap-1"
                >
                  View Order <span>↗</span>
                </Link>
              </div>
            </div>
          </div>

          {/* Payment Method */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h3 className="text-base font-bold text-slate-900 mb-6">Payment Method</h3>
            <div className="space-y-4 text-sm">
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Method</span>
                <span className="text-slate-900 uppercase">{payment.payment_method || 'BAKONG'}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Provider</span>
                <span className="text-slate-900 uppercase">{payment.payment_provider || 'BAKONG'}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Currency</span>
                <span className="text-slate-900 uppercase">{payment.currency || 'KHR'}</span>
              </div>
            </div>
          </div>

          {/* Payment Timeline */}
          <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
            <h3 className="text-base font-bold text-slate-900 mb-6">Payment Timeline</h3>
            <div className="relative pl-6 space-y-6 before:absolute before:inset-y-0 before:left-2.5 before:w-px before:bg-slate-200">
              
              {/* Outcome node */}
              {(isSuccess || isFailed) && (
                <div className="relative">
                  <div className={`absolute -left-[30px] top-1 h-3 w-3 rounded-full border-2 border-white ring-2 ${
                    isSuccess ? 'bg-emerald-500 ring-emerald-100' : 'bg-red-500 ring-red-100'
                  }`} />
                  <h4 className={`text-sm font-semibold ${
                    isSuccess ? 'text-emerald-700' : 'text-red-700'
                  }`}>
                    {isSuccess ? `Payment ${statusLabel}` : `Payment ${statusLabel}`}
                  </h4>
                  <p className="text-xs text-slate-500 mt-1">{payDateStr} (Bakong Callback)</p>
                </div>
              )}
              
              {/* Initiation node */}
              <div className="relative">
                <div className="absolute -left-[30px] top-1 h-3 w-3 rounded-full bg-amber-500 border-2 border-white ring-2 ring-amber-100"></div>
                <h4 className="text-sm font-semibold text-slate-800">Payment Initiated</h4>
                <p className="text-xs text-slate-500 mt-1">{payDateStr} (KHQR generated)</p>
              </div>

            </div>
          </div>

        </div>
      </div>
    </PageContainer>
  );
}
