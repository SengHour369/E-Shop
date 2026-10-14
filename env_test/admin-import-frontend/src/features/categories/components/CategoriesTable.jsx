import Table from '@/components/ui/Table';
import { HiOutlinePencilSquare, HiOutlineEye } from 'react-icons/hi2';
import DeleteButton from '@/components/ui/DeleteButton';
import Badge from '@/components/ui/Badge';

export default function CategoriesTable({ categories, onEdit, onDelete, onView, loading }) {
  const getStatusBadgeVariant = (status) => {
    if (status === true || status === 'active') return 'success';
    if (status === false || status === 'inactive') return 'danger';
    return 'success'; // Default to active for older items where status is null
  };
  const columns = [
    {
      key: 'id',
      label: 'ID',
      align: 'center',
      render: val => (
        <span className="font-mono text-xs text-slate-400">{val}</span>
      ),
    },
    {
      key: 'icon',
      label: 'Icon',
      align: 'center',
      render: (val, row) =>
        val ? (
          <img
            src={val}
            alt={row.name}
            className="h-9 w-9 rounded-lg object-cover ring-1 ring-slate-200 mx-auto"
          />
        ) : (
          <div className="h-10 w-10 rounded-lg bg-slate-100 flex items-center justify-center text-xs font-semibold text-slate-400 mx-auto">
            {row.name?.[0]?.toUpperCase() ?? '?'}
          </div>
        ),
    },
    {
      key: 'name',
      label: 'Name',
      render: val => (
        <span className="font-medium text-slate-900">{val}</span>
      ),
    },
    {
      key: 'description',
      label: 'Description',
      render: val => (
        <span
          className="text-sm text-slate-500 line-clamp-1 max-w-xs"
          title={val}
        >
          {val || '—'}
        </span>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      render: (_, row) => (
        <Badge variant={getStatusBadgeVariant(row.status)}>
          {row.status === false || row.status === 'inactive' ? 'Inactive' : 'Active'}
        </Badge>
      ),
    },
    {
      key: 'created_at',
      label: 'Created At',
      render: val => (
        <span className="text-sm text-slate-500">
          {val ? new Date(val).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
        </span>
      ),
    },
    {
      key: 'updated_at',
      label: 'Updated At',
      render: val => (
        <span className="text-sm text-slate-500">
          {val ? new Date(val).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : '—'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (_, row) => (
        <div className="flex items-center justify-end gap-1.5">
          <button
            onClick={() => onView?.(row)}
            aria-label={`View ${row.name}`}
            title="View"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-blue-600 bg-blue-50 hover:bg-blue-100 transition-colors"
          >
            <HiOutlineEye className="h-4 w-4" />
          </button>
          <button
            onClick={() => onEdit?.(row)}
            aria-label={`Edit ${row.name}`}
            title="Edit"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-amber-600 bg-amber-50 hover:bg-amber-100 transition-colors"
          >
            <HiOutlinePencilSquare className="h-4 w-4" />
          </button>
          <DeleteButton
            onConfirm={() => {
              onDelete?.(row.id);
              console.log('Delete confirmed for ID:', row.id);
            }}
            aria-label={`Delete ${row.name}`}
            title="Delete"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-rose-600 bg-rose-50 hover:bg-rose-100 transition-colors"
          />
          
        </div>
      ),
    },
  ];

  return (
    <Table columns={columns} data={categories} loading={loading} />
  );
}