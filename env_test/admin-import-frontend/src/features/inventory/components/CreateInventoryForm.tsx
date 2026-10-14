import React, { useState } from 'react';
import { Loader2, CheckCircle2, AlertCircle } from 'lucide-react';
import axios from 'axios';

// Stubbed useAuth hook as requested to retrieve the Bearer token
function useAuth() {
  // In a real application, this would fetch from Context or State management
  return { token: localStorage.getItem('token') || 'stub-token-123' };
}

interface FormData {
  skuId: string;
  quantity: string;
  lowStockThreshold: string;
  warehouse_location: string;
}

interface FormErrors {
  skuId?: string;
  quantity?: string;
  lowStockThreshold?: string;
  warehouse_location?: string;
  general?: string;
}

export default function CreateInventoryForm() {
  const { token } = useAuth();
  
  const [formData, setFormData] = useState<FormData>({
    skuId: '',
    quantity: '',
    lowStockThreshold: '',
    warehouse_location: '',
  });
  
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successData, setSuccessData] = useState<any>(null);

  const validate = (): boolean => {
    const newErrors: FormErrors = {};
    
    // Validate skuId
    if (!formData.skuId) {
      newErrors.skuId = 'SKU ID is required';
    } else if (isNaN(Number(formData.skuId))) {
      newErrors.skuId = 'Must be a valid number';
    }
    
    // Validate quantity
    if (!formData.quantity) {
      newErrors.quantity = 'Quantity is required';
    } else if (Number(formData.quantity) < 0) {
      newErrors.quantity = 'Must be ≥ 0';
    }
    
    // Validate lowStockThreshold
    if (!formData.lowStockThreshold) {
      newErrors.lowStockThreshold = 'Low stock threshold is required';
    } else if (Number(formData.lowStockThreshold) < 0) {
      newErrors.lowStockThreshold = 'Must be ≥ 0';
    } else if (
      formData.quantity && 
      !isNaN(Number(formData.quantity)) && 
      Number(formData.lowStockThreshold) >= Number(formData.quantity)
    ) {
      newErrors.lowStockThreshold = 'Must be less than quantity';
    }

    // Validate warehouse_location
    if (!formData.warehouse_location.trim()) {
      newErrors.warehouse_location = 'Warehouse location is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Clear errors when user types
    if (errors[name as keyof FormErrors] || errors.general) {
      setErrors(prev => ({ ...prev, [name]: undefined, general: undefined }));
    }
    setSuccessData(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setErrors({});
    setSuccessData(null);

    const axiosConfig = {
      headers: {
        Authorization: `Bearer ${token}`
      }
    };

    try {
      // 1. Check if inventory already exists for this SKU
      try {
        const checkRes = await axios.get(
          `/api/v1/inventory/sku?skuId=${formData.skuId}`,
          axiosConfig
        );
        
        // If the request succeeds and returns data, it means inventory exists
        if (checkRes.data) {
          setErrors({ 
            general: 'Inventory already exists for this SKU — use Restock or Set Exact Quantity instead' 
          });
          setIsSubmitting(false);
          return;
        }
      } catch (checkErr: any) {
        // If 404 Not Found, it means no inventory exists, which is the expected happy path.
        // If it's a different error, we throw it to be caught by the outer catch block.
        if (checkErr.response?.status !== 404) {
          throw checkErr;
        }
      }

      // 2. Create the inventory
      const payload = {
        quantity: Number(formData.quantity),
        lowStockThreshold: Number(formData.lowStockThreshold),
        warehouse_location: formData.warehouse_location.trim()
      };

      const createRes = await axios.post(
        `/api/v1/inventory?skuId=${formData.skuId}`,
        payload,
        axiosConfig
      );

      setSuccessData(createRes.data || payload);
      setFormData({ skuId: '', quantity: '', lowStockThreshold: '', warehouse_location: '' });
      
    } catch (err: any) {
      console.error('Create inventory error:', err);
      setErrors({ 
        general: err.response?.data?.message || err.message || 'Failed to create inventory. Please try again.' 
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto p-6 bg-white rounded-xl shadow-sm border border-slate-200">
      <h2 className="text-xl font-semibold text-slate-900 mb-6">Create Inventory</h2>

      {/* Success Message */}
      {successData && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
          <CheckCircle2 className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-medium text-green-900">Inventory Created Successfully</h3>
            <p className="text-sm text-green-700 mt-1">
              Quantity: {successData.quantity ?? successData.stock_qty} | Location: {successData.warehouse_location}
            </p>
          </div>
        </div>
      )}

      {/* General Error / Warning Message */}
      {errors.general && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <p className="text-sm text-red-700 font-medium">{errors.general}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5">
        {/* SKU ID */}
        <div>
          <label htmlFor="skuId" className="block text-sm font-medium text-slate-700 mb-1.5">
            SKU ID *
          </label>
          <input
            id="skuId"
            name="skuId"
            type="number"
            value={formData.skuId}
            onChange={handleChange}
            placeholder="e.g. 1042"
            className={`w-full px-3 py-2 bg-white border rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-100 transition-colors ${
              errors.skuId ? 'border-red-300 focus:border-red-400' : 'border-slate-300 focus:border-blue-400'
            }`}
          />
          {errors.skuId && <p className="mt-1.5 text-sm text-red-600">{errors.skuId}</p>}
        </div>

        {/* Quantity */}
        <div>
          <label htmlFor="quantity" className="block text-sm font-medium text-slate-700 mb-1.5">
            Quantity *
          </label>
          <input
            id="quantity"
            name="quantity"
            type="number"
            min="0"
            value={formData.quantity}
            onChange={handleChange}
            placeholder="e.g. 500"
            className={`w-full px-3 py-2 bg-white border rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-100 transition-colors ${
              errors.quantity ? 'border-red-300 focus:border-red-400' : 'border-slate-300 focus:border-blue-400'
            }`}
          />
          {errors.quantity && <p className="mt-1.5 text-sm text-red-600">{errors.quantity}</p>}
        </div>

        {/* Low Stock Threshold */}
        <div>
          <label htmlFor="lowStockThreshold" className="block text-sm font-medium text-slate-700 mb-1.5">
            Low Stock Threshold *
          </label>
          <input
            id="lowStockThreshold"
            name="lowStockThreshold"
            type="number"
            min="0"
            value={formData.lowStockThreshold}
            onChange={handleChange}
            placeholder="e.g. 50"
            className={`w-full px-3 py-2 bg-white border rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-100 transition-colors ${
              errors.lowStockThreshold ? 'border-red-300 focus:border-red-400' : 'border-slate-300 focus:border-blue-400'
            }`}
          />
          {errors.lowStockThreshold && <p className="mt-1.5 text-sm text-red-600">{errors.lowStockThreshold}</p>}
        </div>

        {/* Warehouse Location */}
        <div>
          <label htmlFor="warehouse_location" className="block text-sm font-medium text-slate-700 mb-1.5">
            Warehouse Location *
          </label>
          <input
            id="warehouse_location"
            name="warehouse_location"
            type="text"
            value={formData.warehouse_location}
            onChange={handleChange}
            placeholder="e.g. WH-NYC-01"
            className={`w-full px-3 py-2 bg-white border rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-100 transition-colors ${
              errors.warehouse_location ? 'border-red-300 focus:border-red-400' : 'border-slate-300 focus:border-blue-400'
            }`}
          />
          {errors.warehouse_location && <p className="mt-1.5 text-sm text-red-600">{errors.warehouse_location}</p>}
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isSubmitting || !!errors.general}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-200 disabled:opacity-60 disabled:cursor-not-allowed mt-4"
        >
          {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
          {isSubmitting ? 'Creating Inventory...' : 'Create Inventory'}
        </button>
      </form>
    </div>
  );
}
