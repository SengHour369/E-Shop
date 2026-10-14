import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Table from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import DeleteButton from '@/components/ui/DeleteButton';
import { formatCurrency } from '@/utils/formatCurrency';
import { PRODUCT_STATUSES } from '@/constants/status';
import {
  HiOutlinePencilSquare,
  HiOutlineEye,
} from 'react-icons/hi2';

export default function ProductsTable({ products, onDelete, loading }) {
  const navigate = useNavigate();
  const [sortKey, setSortKey] = useState('name');
  const [sortDir, setSortDir] = useState('asc');
  const [selectedIds, setSelectedIds] = useState([]);

  const handleSort = (key) => {
    if (sortKey === key) setSortDir(d => (d === 'asc' ? 'desc' : 'asc'));
    else { setSortKey(key); setSortDir('asc'); }
  };

  const sorted = [...products].sort((a, b) => {
    const mul = sortDir === 'desc' ? 1 : -1;
    if (typeof a[sortKey] === 'string') return a[sortKey].localeCompare(b[sortKey]) * mul;
    return (a[sortKey] - b[sortKey]) * mul;
  });

  const allSelected = products.length > 0 && selectedIds.length === products.length;

  const toggleSelectAll = () => {
    setSelectedIds(allSelected ? [] : products.map(p => p.id));
  };

  const toggleSelectRow = (id) => {
    setSelectedIds(prev => (
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    ));
  };

  const resolveStatusKey = (row) => {
    if (row.status) return row.status;
    if (row.stock === 0) return 'out_of_stock';
    if (row.stock < 20) return 'low_stock';
    return 'active';
  };

  const columns = [
    {
      key: 'select',
      label: (
        <input
          type="checkbox"
          checked={allSelected}
          onChange={toggleSelectAll}
          className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
        />
      ),
      render: (_, row) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(row.id)}
          onChange={() => toggleSelectRow(row.id)}
          className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
        />
      ),
    },
    {
      key: 'name', label: 'Product', sortable: true,
      render: (val, row) => (
        <div className="flex items-center gap-3">
          <img src={row.image} alt={val} className="h-10 w-10 rounded-lg object-cover flex-shrink-0 bg-slate-100" />
          <div>
            <p className="font-medium text-slate-900">{val}</p>
            <p className="text-xs text-slate-400">{row.sku}</p>
          </div>
        </div>
      ),
    },
    { key: 'sku', label: 'SKU', sortable: true },
    { key: 'category', label: 'Category', sortable: true },
    { key: 'brand', label: 'Brand', sortable: true },
    {
      key: 'price', label: 'Price', sortable: true, align: 'right',
      render: val => <span className="font-semibold">{formatCurrency(val)}</span>,
    },
    {
      key: 'stock', label: 'Stock', sortable: true, align: 'center',
      render: val => (
        <span className={val === 0 ? 'font-semibold text-red-600' : val < 20 ? 'font-semibold text-amber-600' : 'text-slate-700'}>
          {val === 0 ? 'Out of stock' : val}
        </span>
      ),
    },
    {
      key: 'status', label: 'Status',
      render: (_, row) => {
        const s = PRODUCT_STATUSES[resolveStatusKey(row)] || PRODUCT_STATUSES.draft;
        return <Badge variant={s.variant} dot>{s.label}</Badge>;
      },
    },
    {
      key: 'createdAt', label: 'Created At', sortable: true,
      render: val => <span className="text-sm text-slate-500">{val}</span>,
    },
    {
      key: 'updatedAt', label: 'Updated At', sortable: true,
      render: val => <span className="text-sm text-slate-500">{val}</span>,
    },
    {
      key: 'actions', label: 'Actions', align: 'right',
      render: (_, row) => (
        <div className="flex items-center justify-end gap-1">
          <button
            onClick={() => navigate(`/dashboard/products/view/${row.id}`)}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-indigo-50 hover:bg-indigo-100 hover:text-indigo-600 transition-colors"
            title="View"
          >
            <HiOutlineEye className="h-4 w-4" />
          </button>
          <button
            onClick={() => navigate(`/dashboard/products/edit/${row.id}`)}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-yellow-100 hover:bg-yellow-200 hover:text-yellow-700 transition-colors"
            title="Edit"
          >
            <HiOutlinePencilSquare className="h-4 w-4" />
          </button>
          <DeleteButton
            onConfirm={() => onDelete?.(row.id)}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-pink-100 hover:bg-red-50 hover:text-red-600 transition-colors"
          />
        </div>
      ),
    },
  ];

  return (
    <Table
        columns={columns}
        data={sorted}
        loading={loading}
        sortKey={sortKey}
        sortDir={sortDir}
        onSort={handleSort}
      />
  );
}