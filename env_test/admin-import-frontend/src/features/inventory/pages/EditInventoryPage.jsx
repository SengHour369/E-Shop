import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Package, Info } from 'lucide-react';
import PageContainer from '@/components/layouts/PageContainer';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import inventoryService from '../services/inventory.service';

export default function EditInventoryPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const inventoryItem = location.state;
  
  const [formData, setFormData] = useState({
    product_sku_id: '',
    quantity: '', // This will represent the added quantity for restocking
    lowStockThreshold: '',
    warehouse_location: '',
  });
  
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [generalError, setGeneralError] = useState('');

  // Pre-fill data if available
  useEffect(() => {
    if (inventoryItem) {
      setFormData({
        product_sku_id: inventoryItem.product_sku_id || inventoryItem.sku || '',
        quantity: '', // Start with blank so they can enter how much to add
        lowStockThreshold: inventoryItem.lowStockThreshold || '',
        warehouse_location: inventoryItem.warehouse || '',
      });
    } else {
      // If no state, go back
      navigate('/dashboard/inventory');
    }
  }, [inventoryItem, navigate]);

  const validate = () => {
    const newErrors = {};
    
    if (!formData.quantity) newErrors.quantity = 'Quantity to add is required';
    else if (Number(formData.quantity) <= 0) newErrors.quantity = 'Must be > 0 to restock';
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field] || generalError) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
      setGeneralError('');
    }
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    setErrors({});
    setGeneralError('');

    try {
      const payload = {
        quantity: Number(formData.quantity),
      };

      await inventoryService.restock(inventoryItem.id, payload);

      navigate('/dashboard/inventory');
      
    } catch (err) {
      console.error('Restock inventory error:', err);
      setGeneralError(err.response?.data?.message || err.message || 'Failed to restock inventory.');
      setIsSubmitting(false);
    }
  };

  if (!inventoryItem) return null;

  const currentStock = inventoryItem.stockQty || 0;
  const addedStock = Number(formData.quantity) || 0;
  const newStock = currentStock + addedStock;

  return (
    <PageContainer>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Edit Inventory (Restock)</h1>
          <p className="text-slate-500 text-sm mt-1">Dashboard › Inventory › Edit Inventory</p>
        </div>
        <div className="flex gap-3">
          <Button type="button" variant="secondary" onClick={() => navigate('/dashboard/inventory')}>Cancel</Button>
          <Button type="button" onClick={handleSubmit} loading={isSubmitting}>Confirm Restock</Button>
        </div>
      </div>

      {generalError && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700 shadow-sm">
          <svg className="h-5 w-5 flex-shrink-0 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <p className="font-semibold">Something went wrong</p>
            <p className="text-red-500 mt-0.5">{generalError}</p>
          </div>
        </div>
      )}

      {/* Main Card */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200">
        
        {/* Card Body with 3 columns */}
        <div className="p-6 md:p-8">
          <div className="grid grid-cols-12 gap-x-8 gap-y-10">
            
            {/* Column 1: Product Information */}
            <div className="col-span-12 lg:col-span-4 space-y-5">
              <h3 className="font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Product Information</h3>
              
              <Input
                label="Product SKU ID"
                value={formData.product_sku_id}
                disabled
                className="bg-slate-50"
              />
              
              <div className="border border-slate-200 rounded-xl p-5 bg-slate-50/50 mt-4 flex flex-col justify-center min-h-[220px]">
                <div className="w-full text-left">
                  <div className="space-y-4 text-sm">
                    <div className="flex justify-between border-b border-slate-100 pb-2">
                      <span className="text-slate-500">Product</span>
                      <span className="font-medium text-slate-900">{inventoryItem.name}</span>
                    </div>
                    <div className="flex justify-between border-b border-slate-100 pb-2">
                      <span className="text-slate-500">SKU ID</span>
                      <span className="font-medium text-slate-900">{inventoryItem.sku}</span>
                    </div>
                    <div className="flex justify-between border-b border-slate-100 pb-2">
                      <span className="text-slate-500">Warehouse</span>
                      <span className="font-medium text-slate-900">{inventoryItem.warehouse}</span>
                    </div>
                    <div className="flex justify-between pb-2">
                      <span className="text-slate-500">Current Stock</span>
                      <span className="font-medium text-slate-900">{currentStock}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Column 2: Inventory Details */}
            <div className="col-span-12 lg:col-span-5 space-y-5">
              <h3 className="font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Edit Details</h3>
              
              <Input
                label="Warehouse Location"
                value={formData.warehouse_location}
                disabled
                className="bg-slate-50"
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Add Quantity *"
                  placeholder="e.g. 10"
                  type="number"
                  min="1"
                  value={formData.quantity}
                  onChange={(e) => handleChange('quantity', e.target.value)}
                  error={errors.quantity}
                />
                <Input
                  label="Low Stock Threshold"
                  value={formData.lowStockThreshold}
                  disabled
                  className="bg-slate-50"
                />
              </div>
            </div>

            {/* Column 3: Summary */}
            <div className="col-span-12 lg:col-span-3 space-y-4">
              <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
                <div className="flex items-center gap-2 text-slate-800 font-semibold mb-5">
                  <svg className="w-4 h-4 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                  Summary
                </div>
                <div className="space-y-4 text-sm">
                  <div className="pb-4 border-b border-slate-100">
                    <div className="text-slate-400 mb-1 text-xs font-semibold uppercase tracking-widest">CURRENT QTY</div>
                    <div className="font-bold text-slate-900 text-2xl">{currentStock}</div>
                  </div>
                  <div className="pb-4 border-b border-slate-100">
                    <div className="text-slate-400 mb-1 text-xs font-semibold uppercase tracking-widest">ADDED QUANTITY</div>
                    <div className="font-bold text-blue-600 text-2xl">{addedStock || <span className="text-slate-300">—</span>}</div>
                  </div>
                  <div>
                    <div className="text-slate-400 mb-1 text-xs font-semibold uppercase tracking-widest">NEW AVAILABLE QTY</div>
                    <div className="font-bold text-slate-900 text-2xl">{newStock}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Card Footer */}
        <div className="px-6 py-4 border-t border-slate-100 bg-slate-50/50 rounded-b-xl flex items-center justify-end gap-3">
          <Button 
            variant="secondary" 
            onClick={() => navigate('/dashboard/inventory')}
            className="px-6 bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Button>
          <Button 
            onClick={handleSubmit} 
            loading={isSubmitting}
            className="px-6 bg-blue-600 hover:bg-blue-700 flex items-center gap-2 shadow-sm"
          >
            <Package className="w-4 h-4" />
            Confirm Restock
          </Button>
        </div>

      </div>
    </PageContainer>
  );
}
