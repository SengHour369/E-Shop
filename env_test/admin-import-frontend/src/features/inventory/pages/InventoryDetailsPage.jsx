import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Edit2, Package, ArrowLeft } from 'lucide-react';
import { cn } from '@/utils/cn';
import PageContainer from '@/components/layouts/PageContainer';
import Button from '@/components/ui/Button';

export default function InventoryDetailsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const product = location.state;

  if (!product) {
    navigate('/dashboard/inventory');
    return null;
  }

  return (
    <PageContainer>
      <div className="flex items-center gap-4 mb-6">
        <button 
          onClick={() => navigate('/dashboard/inventory')}
          className="p-2 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-xl font-bold text-slate-900">Inventory Details</h1>
          <p className="text-slate-500 text-sm mt-1">Dashboard › Inventory › Details</p>
        </div>
        <div className="ml-auto">
          <Button 
            onClick={() => navigate('/dashboard/inventory/edit', { state: product })}
            className="flex items-center gap-2"
          >
            <Edit2 className="h-4 w-4" />
            Edit Stock
          </Button>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 md:p-8">
        
        {/* Header / Summary */}
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-6 mb-8 pb-8 border-b border-slate-100">
          <div className="flex items-start gap-4">
            <div className="w-16 h-16 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center shrink-0">
              <Package className="h-8 w-8" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-slate-900 leading-tight">{product.name}</h2>
              <div className="flex items-center gap-3 mt-2 text-sm text-slate-500">
                <span className="font-mono bg-slate-100 px-2 py-0.5 rounded text-slate-700">
                  SKU: {product.sku}
                </span>
                <span>•</span>
                <span>{product.warehouse}</span>
              </div>
            </div>
          </div>
          <div className="shrink-0">
            <span className={cn('px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-wider', {
              'bg-green-50 text-green-700': product.status === 'In Stock',
              'bg-amber-50 text-amber-700': product.status === 'Low Stock',
              'bg-red-50   text-red-700':   product.status === 'Out of Stock',
            })}>
              {product.status}
            </span>
          </div>
        </div>

        {/* Quantities */}
        <h3 className="text-lg font-bold text-slate-900 mb-4">Stock Overview</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="bg-slate-50 rounded-xl p-5 border border-slate-100">
            <p className="text-sm font-medium text-slate-500 mb-2">Stock Qty</p>
            <p className="text-3xl font-bold text-slate-900">{product.stockQty}</p>
          </div>
          <div className="bg-slate-50 rounded-xl p-5 border border-slate-100">
            <p className="text-sm font-medium text-slate-500 mb-2">Reserved Qty</p>
            <p className="text-3xl font-bold text-slate-900">{product.reservedQty}</p>
          </div>
          <div className="bg-slate-50 rounded-xl p-5 border border-slate-100">
            <p className="text-sm font-medium text-slate-500 mb-2">Available Qty</p>
            <p className={cn('text-3xl font-bold', {
              'text-green-600': product.availableQty > product.lowStockThreshold,
              'text-amber-500': product.availableQty > 0 && product.availableQty <= product.lowStockThreshold,
              'text-red-600':   product.availableQty === 0,
            })}>
              {product.availableQty}
            </p>
          </div>
          <div className="bg-slate-50 rounded-xl p-5 border border-slate-100">
            <p className="text-sm font-medium text-slate-500 mb-2">Low Stock Threshold</p>
            <p className="text-3xl font-bold text-slate-900">{product.lowStockThreshold}</p>
          </div>
        </div>

        {/* Details List */}
        <h3 className="text-lg font-bold text-slate-900 mb-4">Additional Details</h3>
        <div className="bg-slate-50 rounded-xl border border-slate-100 overflow-hidden">
          <div className="divide-y divide-slate-100">
            <div className="flex justify-between items-center p-4 bg-white">
              <span className="text-slate-500 text-sm">Warehouse Location</span>
              <span className="text-slate-900 font-medium">{product.warehouse}</span>
            </div>
            <div className="flex justify-between items-center p-4 bg-white">
              <span className="text-slate-500 text-sm">Product SKU ID</span>
              <span className="font-mono text-slate-900 font-medium">{product.product_sku_id || '—'}</span>
            </div>
            <div className="flex justify-between items-center p-4 bg-white">
              <span className="text-slate-500 text-sm">Last Restocked</span>
              <span 
                className="text-slate-900 font-medium"
                title={product.lastRestockedAtRaw ?? product.updatedAtRaw ?? ''}
              >
                {product.lastUpdated}
              </span>
            </div>
            <div className="flex justify-between items-center p-4 bg-white">
              <span className="text-slate-500 text-sm">Created At</span>
              <span 
                className="text-slate-900 font-medium"
                title={product.createdAtRaw ?? ''}
              >
                {product.createdAt}
              </span>
            </div>
          </div>
        </div>

      </div>
    </PageContainer>
  );
}
