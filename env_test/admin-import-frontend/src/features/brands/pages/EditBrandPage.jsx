// src/features/brands/pages/EditBrandPage.jsx
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { HiOutlineCloudArrowUp, HiOutlineInformationCircle } from 'react-icons/hi2';
import Button from '@/components/ui/Button';
import { categoryApi } from '../../categories/services/category.service';
import { brandService } from '@/features/brands/services/brand.service';
import Swal from 'sweetalert2';
import PageContainer from '@/components/layouts/PageContainer';
import SearchableSelect from '@/components/ui/SearchableSelect';

export default function EditBrandPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [categories, setCategories] = useState([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [loadingBrand, setLoadingBrand] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [previewUrl, setPreviewUrl] = useState('');

  const [form, setForm] = useState({
    name: '',
    description: '',
    categoryID: '',
    image: null,
    currentImageUrl: '',
  });

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const handleImageChange = (e) => {
    const file = e.target.files?.[0] ?? null;
    setForm(f => ({ ...f, image: file }));

    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(file ? URL.createObjectURL(file) : '');
  };

  const handleRemoveImage = () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setForm(f => ({ ...f, image: null, currentImageUrl: '' }));
    setPreviewUrl('');
  };

  const normalizeList = (response) => {
    const raw = Array.isArray(response) ? response : (response?.data?.payload ?? response?.payload ?? response?.data ?? response?.content ?? response?.items ?? []);
    console.log("Js",raw);
    return raw
      .map(item => (item?.data != null ? item.data : item))
      .filter(item => item && item.id != null);
  };

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoadingCategories(true);
        const res = await categoryApi.getAll(0, 100);
        // console.log("Js",res);
        setCategories(normalizeList(res));
      } catch (err) {
        console.error('Failed to load categories:', err);
        setError('Failed to load categories.');
      } finally {
        setLoadingCategories(false);
      }
    };
    void fetchCategories();
  }, []);

  useEffect(() => {
    if (!id || categories.length === 0) return;

    const fetchBrand = async () => {
      try {
        setLoadingBrand(true);
        setError('');
        const res = await brandService.getById(id);
        const payloadData = res?.data?.payload ?? res?.payload ?? res?.data ?? [];
        const brand = Array.isArray(payloadData) ? payloadData[0] : payloadData;

        if (brand) {
          const matchedCategory = categories.find(cat => cat.name === brand.category_name);

          setForm({
            name: brand.name ?? '',
            description: brand.description ?? '',
            categoryID: String(
              brand.categoryId ?? brand.category_id ?? brand.category?.id ?? matchedCategory?.id ?? ''
            ),
            image: null,
            currentImageUrl: brand.image ?? '',
          });
        }
      } catch (err) {
        console.error('Failed to load brand:', err);
        setError('Failed to load brand details.');
      } finally {
        setLoadingBrand(false);
      }
    };

    void fetchBrand();
  }, [id, categories]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    const payload = {
      name: form.name.trim(),
      categoryId: Number(form.categoryID),
      description: form.description.trim(),
    };

    if (!payload.name) {
      setError('Brand name is required.');
      return;
    }
    if (!payload.categoryId) {
      setError('Category is required.');
      return;
    }

    try {
      setSubmitting(true);
      await brandService.update(id, { ...payload, file: form.image ?? undefined });
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Brand updated successfully',
      });
      navigate('/dashboard/brands');
    } catch (err) {
      console.error('Failed to update brand:', err);
      setError(err?.response?.data?.message || err?.message || 'Failed to update brand.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <div className="flex flex-col justify-between gap-4 py-4 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Edit Brand</h2>
          <div className="mt-1 flex items-center gap-2 text-xs font-medium text-slate-500">
            <Link to="/dashboard" className="transition-colors hover:text-blue-600">Dashboard</Link>
            <span className="text-slate-300">›</span>
            <Link to="/dashboard/brands" className="transition-colors hover:text-blue-600">Brands</Link>
            <span className="text-slate-300">›</span>
            <span className="text-slate-400">Edit Brand</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={() => navigate('/dashboard/brands')} className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm">
            Cancel
          </Button>
          <Button type="submit" form="brand-form" className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm" loading={submitting}>
            Update Brand
          </Button>
        </div>
      </div>

      {error && (
        <div className="rounded-xl border px-4 py-3 text-sm border-red-200 bg-red-50 text-red-700">
          {error}
        </div>
      )}

      <form id="brand-form" onSubmit={handleSubmit} className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Brand Information</h3>
            <p className="mb-6 mt-1 text-sm text-slate-500">Update the details of the brand.</p>

            <div className="space-y-5">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Brand Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="Enter brand name"
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
                <textarea
                  placeholder="Enter brand description"
                  value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={4}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
                <div className="mt-1 flex justify-end">
                  <span className="text-xs text-slate-400">{form.description.length}/500</span>
                </div>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">Category</label>
                <SearchableSelect
                  value={form.categoryID}
                  onChange={val => setForm(f => ({ ...f, categoryID: val }))}
                  disabled={loadingCategories}
                  placeholder="Select category"
                  options={categories.map(cat => ({ value: cat.id, label: cat.name }))}
                />
              </div>
            </div>
          </div>

          <div className="flex items-start gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4">
            <HiOutlineInformationCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-500" />
            <div>
              <p className="text-sm font-semibold text-blue-800">Brand visibility</p>
              <p className="mt-1 text-xs text-blue-600/90 leading-relaxed">
                Once saved, the brand will be available for selection while adding or editing products.
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Brand Logo</h3>
            <p className="mb-4 mt-1 text-sm text-slate-500">Upload a logo for the brand.</p>

            {(previewUrl || form.currentImageUrl) && (
              <div className="relative mb-4 flex items-center justify-center rounded-xl border border-slate-200 bg-slate-50 p-4">
                <img
                  src={previewUrl || form.currentImageUrl}
                  alt="Brand logo preview"
                  className="h-32 w-32 rounded-lg object-contain"
                />
                <button
                  type="button"
                  onClick={handleRemoveImage}
                  className="absolute right-2 top-2 rounded-full bg-white p-1 text-slate-500 shadow-sm hover:bg-red-50 hover:text-red-600"
                  title="Remove image"
                >
                  ✕
                </button>
              </div>
            )}

            <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center transition-colors hover:border-blue-400 hover:bg-blue-50/50">
              <HiOutlineCloudArrowUp className="mb-3 h-8 w-8 text-slate-400" />
              <p className="text-sm font-medium text-slate-700">Drag and drop your file here</p>
              <p className="my-2 text-xs text-slate-400">or</p>
              <label className="cursor-pointer rounded-lg border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-blue-600 shadow-sm transition-colors hover:bg-slate-50">
                Browse Files
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={handleImageChange}
                />
              </label>
              <p className="mt-3 text-xs text-slate-500">
                {form.image ? form.image.name : (form.currentImageUrl ? 'Using current image' : 'No image selected')}
              </p>
              <p className="mt-4 text-[10px] uppercase tracking-wide text-slate-400">
                PNG, JPG, SVG up to 2MB. Recommended size: 512x512px
              </p>
            </div>
          </div>
        </div>
      </form>
    </PageContainer>
  );
}
