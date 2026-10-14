import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Table from '@/components/ui/Table';
import { formatCurrency } from '@/utils/formatCurrency';
import { HiOutlineEye, HiOutlinePencil, HiCheck, HiXMark } from 'react-icons/hi2';
import { MdCancel } from "react-icons/md";
import Swal from 'sweetalert2';
import { OrderStatus } from '../types/orderTypes';

// Mock Payment logos mapped
const VisaLogo = () => <span className="font-bold text-blue-800 italic mr-2 text-xs">VISA</span>;
const MastercardLogo = () => <span className="flex mr-2 items-center"><span className="w-3 h-3 rounded-full bg-red-500 -mr-1 mix-blend-multiply"></span><span className="w-3 h-3 rounded-full bg-yellow-500"></span></span>;
const PaypalLogo = () => <span className="font-bold text-blue-500 italic mr-2 text-xs">PayPal</span>;
const UpiLogo = () => <span className="font-bold text-slate-700 italic mr-2 text-xs">UPI</span>;

export default function OrdersTable({ orders, onUpdateStatus, onCancel, loading }) {
  const navigate = useNavigate();
  const statuses = Object.values(OrderStatus);
  
  const [editingRowId, setEditingRowId] = useState(null);
  const [editStatusValue, setEditStatusValue] = useState('');

  const handleCancelClick = (row) => {
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
        const userId = row.customer_id || row.userId || row.customer?.id || row.user?.id || 1; 
        onCancel(row.id, userId, result.value);
      }
    });
  };

  const columns = [
    { key: 'id', label: 'Order ID',
      render: (_, row) => <span className="font-medium text-slate-700">{row.order_number || row.orderNumber || row.id}</span> },
    { key: 'customer', label: 'Customer',
      render: (_, row) => {
        const name = row.customer_name || row.customer?.name || row.customer || 'Unknown';
        const email = row.customer_email || row.customer?.email;
        const initial = name.charAt(0).toUpperCase();
        return (
          <div className="flex items-center gap-3 py-1">
            <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-600 font-semibold flex-shrink-0">
              {initial}
            </div>
            <div className="flex flex-col">
              <span className="font-medium text-slate-900 text-sm leading-tight">{name}</span>
              {email && <span className="text-xs text-slate-500">{email}</span>}
            </div>
          </div>
        )
      } 
    },
    { key: 'product', label: 'Product',
      render: (_, row) => {
        const firstItem = row.items && row.items.length > 0 ? row.items[0].product_name || row.items[0].name || `Product ID: ${row.items[0].product_id}` : 'Unknown Product';
        return <span className="text-sm text-slate-600">{firstItem}</span>;
      }
    },
    { key: 'quantity', label: 'Quantity', align: 'center',
      render: (_, row) => {
        const qty = row.items && row.items.length > 0 ? row.items[0].quantity : 1;
        return <span className="text-sm text-slate-600">{qty}</span>;
      }
    },
    { key: 'total', label: 'Total Amount',
      render: (_, row) => <span className="font-medium text-slate-700 text-sm">{formatCurrency(row.total_amount || row.totalAmount || row.total || 0)}</span> },
    { key: 'payment_method', label: 'Payment Method',
      render: (_, row) => {
        const method = row.payment?.payment_method?.toUpperCase() || 'UNKNOWN';
        
        let Logo = null;
        let text = method;

        if (method === 'VISA') Logo = VisaLogo;
        else if (method === 'MASTERCARD') Logo = MastercardLogo;
        else if (method === 'PAYPAL') Logo = PaypalLogo;
        else if (method === 'UPI') Logo = UpiLogo;
        else if (method === 'COD') text = 'Cash on Delivery';

        return (
          <div className="flex items-center">
            {Logo && <Logo />}
            <span className="text-xs font-medium text-slate-600">{!Logo ? text : ''}</span>
          </div>
        );
      }
    },
    { key: 'payment_status', label: 'Payment Status',
      render: (_, row) => {
        const status = (row.payment?.status || 'PENDING').toUpperCase();
        let bg = 'bg-slate-100', text = 'text-slate-600';
        let label = status.charAt(0) + status.slice(1).toLowerCase();

        if (['COMPLETED', 'PAID', 'SUCCESS'].includes(status)) { 
          bg = 'bg-emerald-100'; 
          text = 'text-emerald-700'; 
        } else if (status === 'PENDING') { 
          bg = 'bg-orange-100'; 
          text = 'text-orange-700'; 
        } else if (['FAILED', 'CANCELLED', 'REFUNDED'].includes(status)) { 
          bg = 'bg-red-100'; 
          text = 'text-red-700'; 
        }

        return (
          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${bg} ${text}`}>
            {label === 'Cancelled' ? 'Cancelled' : label}
          </span>
        );
      }
    },
    { key: 'order_status', label: 'Order Status',
      render: (_, row) => {
        const status = (row.status || 'PENDING').toUpperCase();

        if (editingRowId === row.id) {
          return (
            <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
              <select
                value={editStatusValue}
                onChange={(e) => setEditStatusValue(e.target.value)}
                className="text-xs font-medium border border-slate-300 rounded px-2 py-1 outline-none focus:border-indigo-400 bg-white min-w-[100px]"
              >
                {statuses.map((s) => (
                  <option key={s} value={s}>
                    {s.charAt(0) + s.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
              <button
                onClick={() => {
                  if (editStatusValue !== status) {
                    onUpdateStatus(row.id, editStatusValue);
                  }
                  setEditingRowId(null);
                }}
                disabled={editStatusValue === status}
                className="text-emerald-600 hover:bg-emerald-50 p-1 rounded disabled:opacity-50"
                title="Save"
              >
                <HiCheck className="w-4 h-4" />
              </button>
              <button
                onClick={() => setEditingRowId(null)}
                className="text-slate-500 hover:bg-slate-100 p-1 rounded"
                title="Cancel"
              >
                <HiXMark className="w-4 h-4" />
              </button>
            </div>
          );
        }

        let bg = 'bg-slate-50 border-slate-200', text = 'text-slate-600';
        let label = status.charAt(0) + status.slice(1).toLowerCase();

        if (['DELIVERED', 'COMPLETED', 'SUCCESS'].includes(status)) { 
          bg = 'bg-emerald-50 border-emerald-200'; 
          text = 'text-emerald-600'; 
        } else if (['PROCESSING', 'CONFIRMED', 'SHIPPED'].includes(status)) { 
          bg = 'bg-blue-50 border-blue-200'; 
          text = 'text-blue-600'; 
        } else if (status === 'PENDING') { 
          bg = 'bg-orange-50 border-orange-200'; 
          text = 'text-orange-600'; 
        } else if (['CANCELLED', 'FAILED', 'REFUNDED'].includes(status)) { 
          bg = 'bg-red-50 border-red-200'; 
          text = 'text-red-600'; 
        }

        return (
          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border ${bg} ${text}`}>
            {label === 'Cancelled' ? 'Cancelled' : label}
          </span>
        );
      }
    },
    { key: 'order_date', label: 'Order Date',
      render: (_, row) => {
        const dateStr = row.created_at || row.order_date || row.createdAt || row.date;
        if (!dateStr) return null;
        const d = new Date(dateStr);
        const dStr = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
        const timeStr = d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        return (
          <div className="flex flex-col text-xs text-slate-500">
            <span>{dStr}</span>
            <span>{timeStr}</span>
          </div>
        );
      }
    },
    { key: 'actions', label: 'Action', align: 'center',
      render: (_, row) => (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={(e) => {
              e.stopPropagation();
              const userId = row.customer_id || row.userId || row.customer?.id || 1;
              navigate(`/dashboard/orders/${(row.id || '').toString().replace('#', '')}`, { state: { userId } });
              console.log("jkdsd",userId);
            }}
            className="flex items-center justify-center w-6 h-6 rounded-full bg-blue-50 text-blue-500 hover:bg-blue-100 transition-colors"
            title="View order"
          >
            <HiOutlineEye className="w-3.5 h-3.5" />
          </button>
          <button
            className="flex items-center justify-center w-6 h-6 rounded-full bg-blue-50 text-blue-500 hover:bg-blue-100 transition-colors"
            title="Edit order status"
            onClick={(e) => {
              e.stopPropagation();
              setEditingRowId(row.id);
              setEditStatusValue((row.status || 'PENDING').toUpperCase());
            }}
          >
            <HiOutlinePencil className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={(e) => {
              e.stopPropagation();
              handleCancelClick(row);
            }}
            className="flex items-center justify-center w-6 h-6 rounded-full bg-red-50 text-red-500 hover:bg-red-100 transition-colors"
            title="Cancel order"
          >
            <MdCancel className="w-3.5 h-3.5" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <Table 
      columns={columns} 
      data={orders || []} 
      loading={loading}
      onRowClick={(row) => console.log('Clicked Order ID:', row.id)}
    />
  );
}
