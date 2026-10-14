import Table from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import { formatCurrency } from '@/utils/formatCurrency';
import { HiOutlineEye, HiOutlineEyeSlash } from 'react-icons/hi2';
import { formatDate } from '@/utils/formatDate';
import { RETURN_STATUSES } from '@/constants/status';
import { cn } from '@/utils/cn';

const TYPE_COLORS = {
  return: { bg: 'bg-red-50', text: 'text-red-700' },
  refund: { bg: 'bg-amber-50', text: 'text-amber-700' },
  exchange: { bg: 'bg-blue-50', text: 'text-blue-700' },
};

export default function ReturnsTable({ returns: items, onView }) {
  const columns = [
    {
      key: 'return_id', label: 'Return ID',
      render: val => <span className="font-semibold text-slate-700 font-mono text-sm">{val}</span>
    },
    {
      key: 'order_no', label: 'Order',
      render: val => <span className="font-semibold text-indigo-600">{val || '-'}</span>
    },
    {
      key: 'customer_name', label: 'Customer',
      render: val => <span>{val || '-'}</span>
    },
    {
      key: 'product_name', label: 'Product',
      render: val => <span className="font-medium text-slate-700">{val || '-'}</span>
    },
    {
      key: 'return_type', label: 'Type',
      render: val => {
        const typeKey = (val || 'return').toLowerCase();
        const c = TYPE_COLORS[typeKey] || TYPE_COLORS.return;
        return (
          <span className={cn('rounded-md px-2 py-0.5 text-xs font-semibold capitalize', c.bg, c.text)}>
            {typeKey}
          </span>
        );
      },
    },
    {
      key: 'reason', label: 'Reason',
      render: val => <span className="text-sm text-slate-500 max-w-[180px] truncate block">{val}</span>
    },
    {
      key: 'status', label: 'Status',
      render: val => {
        const rawStatus = val || 'Unknown';
        const statusKey = rawStatus.toLowerCase();
        const s = RETURN_STATUSES[statusKey] || { variant: 'default' };

        // Map directly from database string: replace underscores with spaces and capitalize words
        const displayLabel = rawStatus
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, l => l.toUpperCase());

        return <Badge variant={s.variant || 'default'} dot>{displayLabel}</Badge>;
      },
    },
    {
      key: 'amount', label: 'Amount', align: 'right',
      render: val => <span className="font-semibold">{formatCurrency(val)}</span>
    },
    {
      key: 'actions', label: 'Actions', align: 'right',
      render: (_, row) => {
        const isTerminal = ['rejected', 'completed', 'failed'].includes((row.status || '').toLowerCase());
        return (
          <div className="flex items-center justify-end">
            <button
              onClick={() => !isTerminal && onView?.(row)}
              disabled={isTerminal}
              className={cn(
                "flex items-center justify-center w-8 h-8 rounded-lg transition-colors",
                isTerminal
                  ? "text-slate-300 cursor-not-allowed"
                  : "text-slate-400 hover:bg-blue-50 hover:text-blue-600"
              )}
              title={isTerminal ? "Details unavailable" : "View Details"}
            >
              {isTerminal ? <HiOutlineEyeSlash className="w-4 h-4" /> : <HiOutlineEye className="w-4 h-4" />}
            </button>
          </div>
        );
      },
    },
  ];

  return <Table columns={columns} data={items || []} />;
}
