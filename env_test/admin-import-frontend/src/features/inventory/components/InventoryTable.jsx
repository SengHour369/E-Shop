import { Eye, Edit2, Trash2 } from 'lucide-react';
import { cn } from '@/utils/cn';
import Table from '@/components/ui/Table';
import DeleteButton from '@/components/ui/DeleteButton';

export default function InventoryTable({
  data,
  onViewClick,
  onEditClick,
  onDelete,
  loading,
}) {

  const columns = [
    {
      key: 'product',
      label: 'Product / SKU',
      render: (_, item) => (
        <div>
          <p className="font-semibold text-slate-900">{item.name}</p>
          <p className="text-xs text-slate-400 font-mono mt-0.5">{item.sku}</p>
        </div>
      ),
    },
    {
      key: 'warehouse',
      label: 'Warehouse',
      render: (_, item) => <span className="text-slate-600">{item.warehouse}</span>,
    },
    {
      key: 'stockQty',
      label: 'Stock Qty',
      render: (_, item) => item.stockQty,
    },
    {
      key: 'reservedQty',
      label: 'Reserved Qty',
      render: (_, item) => <span className="text-slate-500">{item.reservedQty}</span>,
    },
    {
      key: 'availableQty',
      label: 'Available Qty',
      render: (_, item) => (
        <span className={cn('font-semibold', {
          'text-green-600': item.availableQty > item.lowStockThreshold,
          'text-amber-500': item.availableQty > 0 && item.availableQty <= item.lowStockThreshold,
          'text-red-600':   item.availableQty === 0,
        })}>
          {item.availableQty}
        </span>
      ),
    },
    {
      key: 'lowStockThreshold',
      label: 'Low Stock Threshold',
      render: (_, item) => item.lowStockThreshold,
    },
    {
      key: 'status',
      label: 'Status',
      render: (_, item) => (
        <span className={cn('px-2.5 py-1 rounded-full text-xs font-semibold', {
          'bg-green-50 text-green-700': item.status === 'In Stock',
          'bg-amber-50 text-amber-700': item.status === 'Low Stock',
          'bg-red-50 text-red-700':     item.status === 'Out of Stock',
        })}>
          {item.status}
        </span>
      ),
    },
    {
      key: 'lastUpdated',
      label: 'Last Updated',
      render: (_, item) => <span className="text-xs text-slate-500">{item.lastUpdated}</span>,
    },
    {
      key: 'action',
      label: 'Action',
      align: 'center',
      render: (_, item) => (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => onViewClick(item)}
            className="p-2 bg-blue-50/50 text-blue-400 hover:bg-blue-100 hover:text-blue-600 rounded-lg transition-colors"
            title="View Details"
          >
            <Eye className="h-4 w-4" />
          </button>
          <button
            onClick={() => onEditClick(item)}
            className="p-2 bg-amber-50/50 text-amber-400 hover:bg-amber-100 hover:text-amber-600 rounded-lg transition-colors "
            title="Edit Stock"
          >
            <Edit2 className="h-4 w-4" />
          </button>
   
          <DeleteButton
            onConfirm={() => onDelete?.(item.id)}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-rose-100 bg-rose-50 text-rose-600 hover:bg-rose-100 transition-colors"
          />
        </div>
      ),
    },
  ];

  return (
    <Table 
      columns={columns} 
      data={data} 
      loading={loading}
      emptyState={
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <p className="text-sm font-medium text-slate-500">No inventory records found.</p>
        </div>
      }
    />
  );
}
