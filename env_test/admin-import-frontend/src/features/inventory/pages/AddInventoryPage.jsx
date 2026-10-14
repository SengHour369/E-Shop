import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Package } from 'lucide-react';
import PageContainer from '@/components/layouts/PageContainer';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import inventoryService from '../services/inventory.service';
import Swal from 'sweetalert2';

export default function AddInventoryPage() {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    product_sku_id: '',
    quantity: '',
    lowStockThreshold: '',
    warehouse_location: '',
  });
  
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const showError = (message) => {
    Swal.fire({
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer: 3500,
      timerProgressBar: true,
      icon: 'error',
      title: message,
    });
  };

  const showSuccess = (message) => {
    Swal.fire({
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer: 3000,
      timerProgressBar: true,
      icon: 'success',
      title: message,
    });
  };

  const validate = () => {
    const newErrors = {};
    
    if (!formData.product_sku_id) newErrors.product_sku_id = 'Product SKU ID is required';
    else if (isNaN(Number(formData.product_sku_id))) newErrors.product_sku_id = 'Must be a valid numeric ID';
    
    if (!formData.quantity) newErrors.quantity = 'Quantity is required';
    else if (Number(formData.quantity) < 0) newErrors.quantity = 'Must be ≥ 0';
    
    if (!formData.lowStockThreshold) newErrors.lowStockThreshold = 'Low stock threshold is required';
    else if (Number(formData.lowStockThreshold) < 0) newErrors.lowStockThreshold = 'Must be ≥ 0';
    else if (
      formData.quantity && 
      !isNaN(Number(formData.quantity)) && 
      Number(formData.lowStockThreshold) >= Number(formData.quantity)
    ) {
      newErrors.lowStockThreshold = 'Must be less than quantity';
    }

    if (!formData.warehouse_location.trim()) newErrors.warehouse_location = 'Warehouse location is required';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    setErrors({});

    try {
      try {
        const checkRes = await inventoryService.getBySku(formData.product_sku_id);
        if (checkRes) {
          showError('Inventory already exists for this SKU — use Edit Inventory instead');
          setIsSubmitting(false);
          return;
        }
      } catch (checkErr) {
        if (checkErr.response?.status !== 404) {
          throw checkErr;
        }
      }

      const payload = {
        quantity: Number(formData.quantity),
        lowStockThreshold: Number(formData.lowStockThreshold),
        warehouse_location: formData.warehouse_location.trim()
      };

      await inventoryService.create(formData.product_sku_id, payload);
      showSuccess('Inventory created successfully!');
      navigate('/dashboard/inventory');
      
    } catch (err) {
      console.error('Create inventory error:', err);
      showError(err.response?.data?.message || err.message || 'Failed to create inventory.');
      setIsSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-xl font-bold text-slate-900">Add Inventory</h1>
          <p className="text-slate-500 text-sm mt-1">Dashboard › Inventory › Add Inventory</p>
        </div>
        <div className="flex gap-3">
          <Button type="button" variant="secondary" onClick={() => navigate('/dashboard/inventory')}>Cancel</Button>
          <Button type="button" onClick={handleSubmit} loading={isSubmitting}>Save Inventory</Button>
        </div>
      </div>

      {/* Main Card */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200">
        
        {/* Card Body with 3 columns */}
        <div className="p-6 md:p-8">
          <div className="grid grid-cols-12 gap-x-8 gap-y-10">
            
            {/* Column 1: Product Information */}
            <div className="col-span-12 lg:col-span-4 space-y-5">
              <h3 className="font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Product Information</h3>
              
              <Input
                label="Product SKU ID *"
                placeholder="Enter Product SKU ID (e.g. 13)"
                type="number"
                value={formData.product_sku_id}
                onChange={(e) => handleChange('product_sku_id', e.target.value)}
                error={errors.product_sku_id}
              />
              
              <div className="border border-slate-200 rounded-xl p-5 bg-slate-50/50 mt-4 flex flex-col items-center justify-center min-h-[220px] text-center">
                {!formData.product_sku_id ? (
                  <>
                    <div className="w-12 h-12 bg-blue-50 text-blue-500 rounded-full flex items-center justify-center mb-3">
                      <Package className="h-6 w-6" />
                    </div>
                    <h4 className="font-semibold text-slate-800 text-sm">Enter a Product SKU ID</h4>
                    <p className="text-xs text-slate-500 mt-1 max-w-[200px]">
                      Enter a numeric Product SKU ID above to initialize its inventory.
                    </p>
                  </>
                ) : (
                  <div className="w-full text-left">
                    <div className="space-y-4 text-sm">
                      <div className="flex justify-between border-b border-slate-100 pb-2">
                        <span className="text-slate-500">Product SKU ID</span>
                        <span className="font-medium text-slate-900">{formData.product_sku_id}</span>
                      </div>
                      <div className="flex justify-between border-b border-slate-100 pb-2">
                        <span className="text-slate-500">Warehouse</span>
                        <span className="font-medium text-slate-900">{formData.warehouse_location || '—'}</span>
                      </div>
                      <div className="flex justify-between pb-2">
                        <span className="text-slate-500">Initial Qty</span>
                        <span className="font-medium text-slate-900">{formData.quantity || '—'}</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Column 2: Inventory Details */}
            <div className="col-span-12 lg:col-span-5 space-y-5">
              <h3 className="font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Inventory Details</h3>
              
              <Input
                label="Warehouse Location *"
                placeholder="e.g. WH-NYC-01"
                value={formData.warehouse_location}
                onChange={(e) => handleChange('warehouse_location', e.target.value)}
                error={errors.warehouse_location}
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Initial Quantity *"
                  placeholder="e.g. 500"
                  type="number"
                  min="0"
                  value={formData.quantity}
                  onChange={(e) => handleChange('quantity', e.target.value)}
                  error={errors.quantity}
                />
                <Input
                  label="Low Stock Threshold *"
                  placeholder="e.g. 50"
                  type="number"
                  min="0"
                  value={formData.lowStockThreshold}
                  onChange={(e) => handleChange('lowStockThreshold', e.target.value)}
                  error={errors.lowStockThreshold}
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
                    <div className="font-bold text-slate-900 text-2xl">0</div>
                  </div>
                  <div className="pb-4 border-b border-slate-100">
                    <div className="text-slate-400 mb-1 text-xs font-semibold uppercase tracking-widest">ADDED QUANTITY</div>
                    <div className="font-bold text-blue-600 text-2xl">{formData.quantity || <span className="text-slate-300">—</span>}</div>
                  </div>
                  <div>
                    <div className="text-slate-400 mb-1 text-xs font-semibold uppercase tracking-widest">NEW AVAILABLE QTY</div>
                    <div className="font-bold text-slate-900 text-2xl">{formData.quantity || '0'}</div>
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
            Create Inventory
          </Button>
        </div>

      </div>
    </PageContainer>
  );
}
