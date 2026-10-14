import { Link, useNavigate } from 'react-router-dom';
import { formatCurrency } from '@/utils/formatCurrency';
import { ORDER_STATUSES, PAYMENT_STATUSES } from '@/constants/status';
import OrderTimeline from './OrderTimeline';
import { 
  HiOutlineArrowLeft, HiOutlinePrinter, HiOutlineArrowDownTray, HiOutlinePencil,
  HiOutlineUser, HiOutlineClipboardDocumentList, HiOutlineCalculator, HiOutlineMapPin,
  HiOutlineShoppingBag, HiOutlineShieldCheck, HiOutlineBuildingStorefront, HiOutlineChatBubbleLeftEllipsis
} from 'react-icons/hi2';
import { MdCancel } from "react-icons/md";
import Swal from 'sweetalert2';

// Payment logos mapped for reuse
const VisaLogo = () => <span className="font-bold text-blue-800 italic text-xs">VISA</span>;
const MastercardLogo = () => <span className="flex items-center"><span className="w-3 h-3 rounded-full bg-red-500 -mr-1 mix-blend-multiply"></span><span className="w-3 h-3 rounded-full bg-yellow-500"></span></span>;
const PaypalLogo = () => <span className="font-bold text-blue-500 italic text-xs">PayPal</span>;
const UpiLogo = () => <span className="font-bold text-slate-700 italic text-xs">UPI</span>;

export default function OrderDetail({ order, onUpdateStatus, onCancel }) {
  const navigate = useNavigate();
  
  if (!order) return <div className="p-8 text-center text-slate-500">Loading order details...</div>;

  const handleCancelClick = () => {
    Swal.fire({
      title: 'Cancel Order?',
      text: "Are you sure you want to cancel this order? This action cannot be undone.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ef4444',
      cancelButtonColor: '#94a3b8',
      confirmButtonText: 'Yes, cancel it!',
      input: 'text',
      inputPlaceholder: 'Reason for cancellation (optional)',
    }).then((result) => {
      if (result.isConfirmed) {
        const userId = order.userId || order.customer?.id || order.user?.id || 1;
        if (onCancel) onCancel(order.id, userId, result.value);
      }
    });
  };

  const orderIdLabel = order.order_number || order.orderNumber || order.id || '';
  const currentStatus = order?.status?.toLowerCase() || 'pending';
  
  let statusBadgeColor = 'bg-slate-100 text-slate-700';
  if (currentStatus === 'completed' || currentStatus === 'delivered') statusBadgeColor = 'bg-emerald-100 text-emerald-700';
  else if (currentStatus === 'cancelled') statusBadgeColor = 'bg-red-100 text-red-700';
  else if (currentStatus === 'processing' || currentStatus === 'shipped') statusBadgeColor = 'bg-blue-100 text-blue-700';
  
  const statusLabel = ORDER_STATUSES[currentStatus]?.label || 'Pending';

  const paymentMethod = order?.payment?.payment_method || 'Unknown';
  const paymentStatus = order?.payment?.status?.toLowerCase() || 'pending';
  
  let payBadgeColor = 'bg-slate-100 text-slate-700';
  if (paymentStatus === 'completed' || paymentStatus === 'paid' || paymentStatus === 'success') payBadgeColor = 'bg-emerald-100 text-emerald-700';
  else if (paymentStatus === 'failed') payBadgeColor = 'bg-red-100 text-red-700';
  else if (paymentStatus === 'pending') payBadgeColor = 'bg-amber-100 text-amber-700';
  
  const payStatusLabel = paymentStatus === 'completed' ? 'Paid' : paymentStatus.charAt(0).toUpperCase() + paymentStatus.slice(1);

  const total = order.total_amount || order.totalAmount || order.total || 0;

  // Format dates
  const d = new Date(order.created_at || order.order_date || order.createdAt || order.date || new Date());
  const dateStr = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  const timeStr = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  
  // Calculate days ago
  const diffTime = Math.abs(new Date() - d);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  // Customer logic
  const customerName = order.customer_name || order.customer?.name || order.customer || 'Unknown';
  const customerEmail = order.customer_email || order.customer?.email || 'N/A';
  const initial = customerName.charAt(0).toUpperCase();

  const getPaymentLogo = (method) => {
    const m = method.toUpperCase();
    if (m === 'VISA') return <VisaLogo />;
    if (m === 'MASTERCARD') return <MastercardLogo />;
    if (m === 'PAYPAL') return <PaypalLogo />;
    if (m === 'UPI') return <UpiLogo />;
    if (m === 'COD') return <span className="font-medium text-xs">Cash on Delivery</span>;
    return null;
  };

  const paymentLogo = getPaymentLogo(paymentMethod);
  const isCard = paymentMethod.toUpperCase() === 'VISA' || paymentMethod.toUpperCase() === 'MASTERCARD';

  return (
    <div className="space-y-6">
      {/* Title & Breadcrumbs */}
      <div className="flex flex-col mb-4">
        <h1 className="text-xl font-bold text-slate-800">Order Details</h1>
        <div className="text-sm text-slate-500 mt-1">
          <Link to="/dashboard/orders" className="hover:text-blue-600 transition-colors">Orders</Link>
          <span className="mx-2">›</span>
          <span className="text-slate-400">Order Details</span>
        </div>
      </div>

      {/* Top Header Card */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="p-2 rounded-full hover:bg-slate-100 text-slate-500 transition-colors">
            <HiOutlineArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-lg font-bold text-slate-800">Order #{orderIdLabel}</h2>
              <span className={`px-2 py-0.5 rounded text-xs font-semibold ${statusBadgeColor}`}>
                {statusLabel}
              </span>
            </div>
            <p className="text-sm text-slate-500 mt-1">
              Placed on {dateStr} at {timeStr} <span className="text-slate-400 ml-1">({diffDays} days ago)</span>
            </p>
          </div>
        </div>
        
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors">
            <HiOutlinePrinter className="w-4 h-4" />
            Print Invoice
          </button>
          <button className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors">
            <HiOutlineArrowDownTray className="w-4 h-4" />
            Download Invoice
          </button>
        </div>
      </div>

      {/* 4 Top Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        
        {/* Customer Details */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col h-full">
          <div className="flex items-center gap-2 text-slate-700 font-semibold mb-4">
            <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center">
              <HiOutlineUser className="w-4 h-4" />
            </div>
            Customer Details
          </div>
          <div className="flex gap-3 mb-4">
            <div className="w-12 h-12 rounded-full bg-slate-200 flex items-center justify-center text-slate-600 font-bold text-lg flex-shrink-0">
              {initial}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <p className="font-semibold text-slate-900">{customerName}</p>
              </div>
              <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                {customerEmail}
              </p>
            </div>
          </div>
        </div>

        {/* Order Summary */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col h-full">
          <div className="flex items-center gap-2 text-slate-700 font-semibold mb-4">
            <div className="w-8 h-8 rounded-full bg-purple-50 text-purple-500 flex items-center justify-center">
              <HiOutlineClipboardDocumentList className="w-4 h-4" />
            </div>
            Order Summary
          </div>
          <div className="space-y-3 text-sm flex-1">
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Order ID</span>
              <span className="font-medium text-slate-800">:{orderIdLabel}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Order Status</span>
              <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${statusBadgeColor}`}>{statusLabel}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Payment Status</span>
              <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${payBadgeColor}`}>{payStatusLabel}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Payment Method</span>
              <span className="font-medium text-slate-800 flex items-center gap-2">
                :{paymentLogo ? <span className="ml-1">{paymentLogo}</span> : null}
                {isCard && <span className="text-xs">•••• 4242</span>}
                {!paymentLogo && <span className="ml-1">{paymentMethod}</span>}
              </span>
            </div>
          </div>
        </div>

        {/* Pricing Summary */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col h-full">
          <div className="flex items-center gap-2 text-slate-700 font-semibold mb-4">
            <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center">
              <HiOutlineCalculator className="w-4 h-4" />
            </div>
            Pricing Summary
          </div>
          <div className="flex-1"></div>
          <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center">
            <span className="font-bold text-slate-800">Total Amount</span>
            <span className="font-bold text-slate-900">{formatCurrency(total)}</span>
          </div>
          <div className="mt-2 text-right">
             <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${payBadgeColor}`}>{payStatusLabel}</span>
          </div>
        </div>

        {/* Shipping Address */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col h-full">
          <div className="flex items-center gap-2 text-slate-700 font-semibold mb-4">
            <div className="w-8 h-8 rounded-full bg-orange-50 text-orange-500 flex items-center justify-center">
              <HiOutlineMapPin className="w-4 h-4" />
            </div>
            Shipping Address
          </div>
          <div className="text-sm text-slate-600 space-y-1.5 flex-1">
            <p className="font-medium text-slate-800">{customerName}</p>
            {order.shipping_address ? (
              <>
                <p>{order.shipping_address.address_line1}</p>
                <p>{order.shipping_address.city}, {order.shipping_address.country} {order.shipping_address.zip_code}</p>
              </>
            ) : (
              <p className="text-slate-400 italic mt-2">No shipping address provided</p>
            )}
          </div>
        </div>

      </div>

      {/* Main Grid: 2 Columns left, 1 right */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left Span 2: Ordered Items + Payment Info */}
        <div className="lg:col-span-2 space-y-6">
          
          {/* Ordered Items Table */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
            <div className="flex items-center gap-2 text-slate-700 font-semibold mb-4">
              <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center">
                <HiOutlineShoppingBag className="w-4 h-4" />
              </div>
              Ordered Items ({order.items?.length || 0})
            </div>
            
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead>
                  <tr className="border-b border-slate-100 text-slate-500">
                    <th className="pb-3 font-medium">Product</th>
                    <th className="pb-3 font-medium text-right">Price</th>
                    <th className="pb-3 font-medium text-center">Qty</th>
                    <th className="pb-3 font-medium text-right">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {order.items && order.items.length > 0 ? (
                    order.items.map((item, idx) => (
                      <tr key={idx}>
                        <td className="py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded bg-slate-100 flex items-center justify-center text-slate-400">
                               <HiOutlineShoppingBag className="w-5 h-5" />
                            </div>
                            <div className="flex flex-col">
                              <span className="font-medium text-slate-800">{item.product_name || item.name || 'Unknown Product'}</span>
                              <span className="text-xs text-slate-400">SKU: PROD-{item.product_id || item.id || '001'}</span>
                            </div>
                          </div>
                        </td>
                        <td className="py-4 text-right font-medium">{formatCurrency(item.unit_price || item.price || 0)}</td>
                        <td className="py-4 text-center">{item.quantity || item.qty || 1}</td>
                        <td className="py-4 text-right font-medium text-slate-800">{formatCurrency(item.total_price || (item.unit_price || item.price || 0) * (item.quantity || item.qty || 1))}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="4" className="py-8 text-center text-slate-400">No items in this order.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="mt-4 pt-3 border-t border-slate-100">
               <span className="px-2 py-1 rounded text-xs font-semibold bg-blue-50 text-blue-600 border border-blue-100">
                 Total Items: {order.items?.length || 0}
               </span>
            </div>
          </div>

          {/* Payment Information */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
            <div className="flex items-center gap-2 text-slate-700 font-semibold mb-4">
              <div className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center">
                <HiOutlineShieldCheck className="w-4 h-4" />
              </div>
              Payment Information
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-y-4 gap-x-12 text-sm">
              <div className="flex flex-col gap-3">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Payment ID</span>
                  <span className="font-medium text-slate-800">:{order.payment?.id || 'N/A'}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Payment Method</span>
                  <span className="font-medium text-slate-800 flex items-center gap-2">
                    :{paymentLogo ? <span className="ml-1">{paymentLogo}</span> : null}
                    {!paymentLogo && <span className="ml-1">{paymentMethod}</span>}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Transaction ID</span>
                  <span className="font-medium text-slate-800">:{order.payment?.transaction_id || 'N/A'}</span>
                </div>
              </div>
              <div className="flex flex-col gap-3">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Paid Amount</span>
                  <span className="font-medium text-slate-800">:{formatCurrency(order.payment?.amount || total)}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Payment Status</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${payBadgeColor}`}>:{payStatusLabel}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Paid On</span>
                  <span className="font-medium text-slate-800">:{order.payment?.payment_date ? new Date(order.payment.payment_date).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute:'2-digit' }) : 'N/A'}</span>
                </div>
              </div>
            </div>
          </div>

        </div>

        {/* Right Span 1: Timeline + Seller Info */}
        <div className="space-y-6">
          
          {/* Order Timeline */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
             <div className="flex items-center gap-2 text-slate-700 font-semibold mb-6">
              <div className="w-8 h-8 rounded-full bg-orange-50 text-orange-500 flex items-center justify-center">
                <HiOutlineCalculator className="w-4 h-4" /> 
              </div>
              Order Timeline
            </div>
            
            {/* Dynamic Timeline based on actual status */}
            <div className="space-y-0">
               <div className="flex gap-4">
                  <div className="flex flex-col items-center">
                    <div className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-emerald-500 text-white"><svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg></div>
                    {currentStatus !== 'pending' && <div className="my-1 w-0.5 flex-1 bg-emerald-200" style={{ minHeight: 24 }}></div>}
                  </div>
                  <div className={`pb-6 pt-0.5 ${currentStatus === 'pending' ? 'pb-0' : ''}`}>
                    <p className="text-sm font-semibold text-slate-900">Order Placed</p>
                    <p className="text-xs text-slate-400">{dateStr} {timeStr}</p>
                  </div>
               </div>
               
               {currentStatus !== 'pending' && (
                 <div className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-emerald-500 text-white"><svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg></div>
                    </div>
                    <div className="pb-0 pt-0.5">
                      <p className="text-sm font-semibold text-slate-900">{statusLabel}</p>
                      <p className="text-xs text-slate-400">Latest Update</p>
                    </div>
                 </div>
               )}
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
