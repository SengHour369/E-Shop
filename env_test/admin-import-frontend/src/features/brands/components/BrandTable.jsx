import React from 'react';
import { HiOutlineEye, HiOutlinePencilSquare, HiOutlineTag } from 'react-icons/hi2';
import Badge from '@/components/ui/Badge';
import DeleteButton from '@/components/ui/DeleteButton';
import Table from '@/components/ui/Table';

export default function BrandTable({ brands, onView, onEdit, onDelete, loading }) {
  const getStatusBadgeVariant = (status) => {
    switch(status?.toLowerCase()) {
      case 'active': return 'success'; // Green
      case 'inactive': return 'warning'; // Yellow
      case 'deleted': return 'danger'; // Red
      default: return 'default';
    }
  };

  const columns = [
    { 
      key: 'brand', 
      label: 'Brand',
      render: (_, brand) => (
        <div className="flex items-center gap-3">
          <img
            src={brand.logo}
            alt={brand.name}
            onError={(e) => { e.currentTarget.src = 'https://placehold.co/64x64/1e293b/fff?text=?'; }}
            className="h-10 w-10 rounded-lg border border-slate-200 object-cover bg-white"
          />
          <div>
            <div className="font-medium text-slate-900">{brand.name}</div>
            <div className="text-xs text-slate-500">www.{brand.name.toLowerCase().replace(/\s+/g, '')}.com</div>
          </div>
        </div>
      )
    },
    { 
      key: 'category', 
      label: 'Category',
      render: (_, brand) => (
        <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">
          {brand.category_name}
        </span>
      )
    },
    { 
      key: 'description', 
      label: 'Description',
      render: (_, brand) => (
        <span className="text-sm text-slate-500 line-clamp-2 max-w-xs" title={brand.description}>
          {brand.description || 'No description'}
        </span>
      )
    },
    { 
      key: 'status', 
      label: 'Status',
      render: (_, brand) => (
        <Badge variant={getStatusBadgeVariant(brand.status)}>
          {brand.status ? brand.status.charAt(0).toUpperCase() + brand.status.slice(1) : 'Active'}
        </Badge>
      )
    },
    { 
      key: 'created_at', 
      label: 'Created At',
      render: (_, brand) => (
        <span className="text-slate-500">
          {brand.created_at ? new Date(brand.created_at).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : 'N/A'}
        </span>
      )
    },
    { 
      key: 'action', 
      label: 'Action', 
      align: 'right',
      render: (_, brand) => (
        <div className="flex items-center justify-end gap-2">
          <button 
            onClick={() => onView?.(brand)}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-50 hover:text-slate-700 transition-colors"
          >
            <HiOutlineEye className="h-4 w-4" />
          </button>
          <button 
            onClick={() => onEdit?.(brand)}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-indigo-100 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition-colors"
          >
            <HiOutlinePencilSquare className="h-4 w-4" />
          </button>
          <DeleteButton 
            onConfirm={() => onDelete?.(brand.id)}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-rose-100 bg-rose-50 text-rose-600 hover:bg-rose-100 transition-colors"
          />
        </div>
      )
    }
  ];

  return (
    <Table 
      columns={columns} 
      data={brands} 
      loading={loading}
      emptyState={
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <HiOutlineTag className="h-10 w-10 text-slate-300" />
          <p className="mt-3 text-sm font-medium text-slate-500">No brands found</p>
        </div>
      }
    />
  );
}
