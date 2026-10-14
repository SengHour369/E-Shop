import { useState } from 'react';
import Input, { Textarea, Select } from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import ImageUploadInput from '@/components/ui/ImageUploadInput';
import { PRODUCT_STATUSES } from '@/constants/status';
import { useCategories } from '../../categories/hooks/useCategories';

const BRANDS_LIST = ['Apple', 'Samsung', 'Nike', 'Adidas', 'Sony', 'Louis Vuitton', 'Amazon', 'Zara', 'Patagonia', 'DJI'];

export default function ProductForm({ initialData = {}, onSubmit, onCancel, loading }) {
  const { categories, loading: categoriesLoading } = useCategories();

  const [form, setForm] = useState({
    name: initialData.name || '',
    price: initialData.price || '',
    stock: initialData.stock || '',
    sku: initialData.sku || '',
    category: initialData.category || '',
    brand: initialData.brand || '',
    status: initialData.status || 'draft',
    description: initialData.description || '',
    image: initialData.image || null,
  });
  const [errors, setErrors] = useState({});

  const set = (key, val) => {
    setForm(prev => ({ ...prev, [key]: val }));
    setErrors(prev => ({ ...prev, [key]: '' }));
  };

  const validate = () => {
    const e = {};
    if (!form.name.trim())        e.name = 'Product name is required';
    if (!form.price || form.price <= 0) e.price = 'Valid price is required';
    if (!form.category)           e.category = 'Category is required';
    return e;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    onSubmit?.(form);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Input
          label="Product Name"
          placeholder="e.g. iPhone 15 Pro Max"
          value={form.name}
          onChange={e => set('name', e.target.value)}
          error={errors.name}
          required
        />
        <Input
          label="SKU"
          placeholder="e.g. IP15PM-256"
          value={form.sku}
          onChange={e => set('sku', e.target.value)}
        />
        <Input
          label="Price (USD)"
          type="number"
          placeholder="0.00"
          value={form.price}
          onChange={e => set('price', e.target.value)}
          error={errors.price}
          required
          leftIcon={<span className="text-sm font-medium text-slate-500">$</span>}
        />
        <Input
          label="Stock Quantity"
          type="number"
          placeholder="0"
          value={form.stock}
          onChange={e => set('stock', e.target.value)}
        />
        <Select
          label="Category"
          value={form.category}
          onChange={e => set('category', e.target.value)}
          error={errors.category}
          required
          disabled={categoriesLoading}
        >
          <option value="">{categoriesLoading ? 'Loading...' : 'Select category…'}</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </Select>
        <Select
          label="Brand"
          value={form.brand}
          onChange={e => set('brand', e.target.value)}
        >
          <option value="">Select brand…</option>
          {BRANDS_LIST.map(b => <option key={b} value={b}>{b}</option>)}
        </Select>
        <Select
          label="Status"
          value={form.status}
          onChange={e => set('status', e.target.value)}
        >
          {Object.entries(PRODUCT_STATUSES).map(([key, { label }]) => (
            <option key={key} value={key}>{label}</option>
          ))}
        </Select>
      </div>

      <Textarea
        label="Description"
        placeholder="Product description…"
        value={form.description}
        onChange={e => set('description', e.target.value)}
        rows={4}
      />

      <ImageUploadInput
        value={form.image}
        onChange={val => set('image', val)}
      />

      <div className="flex items-center justify-end gap-3 border-t border-slate-100 pt-4">
        {onCancel && (
          <Button type="button" variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
        )}
        <Button type="submit" loading={loading}>
          {initialData.id ? 'Update Product' : 'Create Product'}
        </Button>
      </div>
    </form>
  );
}
