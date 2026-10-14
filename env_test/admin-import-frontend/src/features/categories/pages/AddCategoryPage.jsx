import { useEffect, useMemo, useState } from 'react';
import { useNavigate, Link, useParams } from 'react-router-dom';
import { HiOutlineInformationCircle, HiOutlineCheckCircle } from 'react-icons/hi2';
import Button from '@/components/ui/Button';
import { categoryApi } from '@/features/categories/services/category.service';
import { categoryIconApi } from '@/features/categories/services/categoryIcon.service';
import PageContainer from '@/components/layouts/PageContainer';
import Swal from 'sweetalert2';


// Tries every field name we've seen APIs use for an icon image, in order.
const getIconImageUrl = (icon) => {
  if (!icon) return null;
  return (
    icon.image_url ||
    icon.icon_url ||
    icon.url ||
    icon.image ||
    icon.icon ||
    icon.path ||
    icon.file_url ||
    icon.src ||
    (icon.svg ? `data:image/svg+xml;utf8,${encodeURIComponent(icon.svg)}` : null) ||
    null
  );
};

const getIconLabel = (icon) => icon?.name ?? icon?.label ?? icon?.title ?? (icon?.id != null ? `Icon ${icon.id}` : 'Icon');

const getCategoryFromResponse = (response) => {
  if (response?.content?.[0]?.data) return response.content[0].data;
  if (response?.data) return response.data;
  return null;
};

export default function AddCategoryPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    name: '',
    description: '',
    status: true,
    icon_id: '',
    icon_name: '',
  });
  const [categoryIcons, setCategoryIcons] = useState([]);
  const [loadingIcons, setLoadingIcons] = useState(true);
  const [iconError, setIconError] = useState('');
  const [iconSearch, setIconSearch] = useState('');
  const [loadingCategory, setLoadingCategory] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState('');

  const filteredIcons = useMemo(() => {
    if (!iconSearch.trim()) return categoryIcons;
    const q = iconSearch.trim().toLowerCase();
    return categoryIcons.filter((icon) => getIconLabel(icon).toLowerCase().includes(q));
  }, [categoryIcons, iconSearch]);

  useEffect(() => {
    const fetchIcons = async () => {
      try {
        setLoadingIcons(true);
        setIconError('');
        const res = await categoryIconApi.getAll();
        const raw = Array.isArray(res) ? res : (res?.data ?? res?.content ?? res?.items ?? []);
        const list = raw
          .map((item) => (item?.data != null ? item.data : item))
          .filter((item) => item && item.id != null);
        setCategoryIcons(list);
      } catch (err) {
        setIconError(err?.response?.data?.message || err?.message || 'Failed to load category icons.');
        setCategoryIcons([]);
      } finally {
        setLoadingIcons(false);
      }
    };

    fetchIcons();
  }, []);

  useEffect(() => {
    if (!isEdit) return;

    const fetchCategory = async () => {
      try {
        setLoadingCategory(true);
        setLoadError('');

        const response = await categoryApi.getById(Number(id));
        console.log('Raw category getById response:', response);

        const category = getCategoryFromResponse(response);

        if (!category) {
          throw new Error('Category payload was empty or did not match the expected shape.');
        }

        setForm({
          name: category.name ?? '',
          description: category.description ?? '',
          status: category.status ?? true,
          icon_id: category.icon_id ?? category.icon?.id ?? '',
          icon_name: category.icon_name ?? category.icon?.name ?? '',
        });
      } catch (err) {
        const message = err?.response?.data?.message || err?.message || 'Failed to load category details.';
        setLoadError(message);
      } finally {
        setLoadingCategory(false);
      }
    };

    fetchCategory();
  }, [isEdit, id]);

  const handleSelectIcon = (icon) => {
    setForm((prev) => ({
      ...prev,
      icon_id: icon?.id != null ? String(icon.id) : '',
      icon_name: getIconLabel(icon),
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const Toast = Swal.mixin({
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer: 3000,
      timerProgressBar: true,
      didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer)
        toast.addEventListener('mouseleave', Swal.resumeTimer)
      }
    });

    if (loadingCategory) {
      Toast.fire({ icon: 'warning', title: 'Category details are still loading. Please wait a moment.' });
      return;
    }

    if (!form.name.trim()) {
      Toast.fire({ icon: 'warning', title: 'Category name is required.' });
      return;
    }

    try {
      setSubmitting(true);

      const payload = {
        name: form.name.trim(),
        description: form.description.trim(),
        status: form.status,
        icon_id: form.icon_id ? Number(form.icon_id) : undefined,
      };

      if (isEdit) {
        await categoryApi.update(Number(id), payload);
      } else {
        await categoryApi.create(payload);
      }

      Toast.fire({
        icon: 'success',
        title: isEdit ? 'Category updated successfully.' : 'Category created successfully.'
      });

      // Navigate back to the categories list
      navigate('/dashboard/categories');
    } catch (err) {
      const message = err?.response?.data?.message || err?.message || 'Failed to save category.';
      Toast.fire({
        icon: 'error',
        title: message
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer>
      <div className="flex flex-col justify-between gap-4 py-4 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">{isEdit ? 'Edit Category' : 'Add Category'}</h2>
          <div className="mt-1 flex items-center gap-2 text-xs font-medium text-slate-500">
            <Link to="/dashboard" className="transition-colors hover:text-blue-600">Dashboard</Link>
            <span className="text-slate-300">›</span>
            <Link to="/dashboard/categories" className="transition-colors hover:text-blue-600">Categories</Link>
            <span className="text-slate-300">›</span>
            <span className="text-slate-400">{isEdit ? 'Edit Category' : 'Add Category'}</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button type="button" variant="secondary" onClick={() => navigate('/dashboard/categories')} className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm">
            Cancel
          </Button>
          <Button type="button" onClick={handleSubmit} loading={submitting} disabled={loadingCategory} className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm">
            {isEdit ? 'Save Changes' : 'Save Category'}
          </Button>
        </div>
      </div>

      {loadingCategory && isEdit && (
        <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
          Loading category details…
        </div>
      )}

      {loadError && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {loadError}
        </div>
      )}

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Category Information</h3>
            <p className="mb-6 mt-1 text-sm text-slate-500">
              {isEdit ? 'Update the details of this category.' : 'Enter the details of the category.'}
            </p>

            <div className="space-y-5">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Category Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. Mobiles & Tablets"
                  value={form.name}
                  disabled={loadingCategory}
                  onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Description
                </label>
                <textarea
                  placeholder="Category description…"
                  value={form.description}
                  disabled={loadingCategory}
                  onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value.slice(0, 500) }))}
                  rows={4}
                  maxLength={500}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
                <p className="mt-1.5 text-right text-xs text-slate-400">{form.description.length}/500</p>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Status
                </label>
                <div className="flex items-center gap-6 mt-2">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="status"
                      checked={form.status === true}
                      disabled={loadingCategory}
                      onChange={() => setForm(prev => ({ ...prev, status: true }))}
                      className="w-4 h-4 accent-emerald-600 text-emerald-600 border-slate-300 focus:ring-emerald-500"
                    />
                    <span className="text-sm text-slate-700">Active</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="status"
                      checked={form.status === false}
                      disabled={loadingCategory}
                      onChange={() => setForm(prev => ({ ...prev, status: false }))}
                      className="w-4 h-4 accent-red-600 text-red-600 border-slate-300 focus:ring-red-500"
                    />
                    <span className="text-sm text-slate-700">Inactive</span>
                  </label>
                </div>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Category Type
                </label>
                <input
                  type="text"
                  placeholder="Select an icon to set the type…"
                  value={form.icon_name}
                  readOnly
                  disabled={loadingCategory}
                  className="w-full cursor-not-allowed select-none rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:outline-none"
                />
                <p className="mt-1.5 text-xs text-slate-400">
                  This is filled in automatically from the icon you pick on the right.
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-base font-semibold text-slate-900">Category Icon</h3>
            </div>

            {iconError && (
              <div className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
                {iconError}
              </div>
            )}

            {categoryIcons.length > 8 && (
              <input
                type="text"
                value={iconSearch}
                disabled={loadingCategory}
                onChange={(e) => setIconSearch(e.target.value)}
                placeholder="Search icons…"
                className="mb-3 w-full rounded-md border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-700 placeholder-slate-400 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
              />
            )}

            {loadingIcons ? (
              <div className="grid grid-cols-4 gap-2">
                {Array.from({ length: 12 }).map((_, i) => (
                  <div key={i} className="aspect-square animate-pulse rounded-lg bg-slate-100" />
                ))}
              </div>
            ) : (
              <div className="grid max-h-80 grid-cols-4 gap-2 overflow-y-auto rounded-lg border border-slate-200 p-3">
                {filteredIcons.map((icon) => {
                  const label = getIconLabel(icon);
                  const imageUrl = getIconImageUrl(icon);
                  const isSelected = String(icon.id) === String(form.icon_id);

                  return (
                    <button
                      key={icon.id}
                      type="button"
                      title={label}
                      disabled={loadingCategory}
                      onClick={() => handleSelectIcon(icon)}
                      className={`group relative flex aspect-square flex-col items-center justify-center gap-1 rounded-lg border p-2 transition-all ${
                        isSelected
                          ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200'
                          : 'border-slate-200 bg-white hover:border-blue-300 hover:bg-blue-50/50'
                      }`}
                    >
                      {isSelected && (
                        <HiOutlineCheckCircle className="absolute -right-1.5 -top-1.5 h-5 w-5 rounded-full bg-white text-blue-600" />
                      )}

                      {imageUrl ? (
                        <img
                          src={imageUrl}
                          alt={label}
                          className="h-8 w-8 object-contain"
                          onError={(e) => {
                            e.currentTarget.style.display = 'none';
                            e.currentTarget.nextSibling.style.display = 'flex';
                          }}
                        />
                      ) : null}

                      <div
                        className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold text-slate-500"
                        style={{ display: imageUrl ? 'none' : 'flex' }}
                      >
                        {label.charAt(0).toUpperCase()}
                      </div>

                      <span className="w-full truncate text-center text-[10px] leading-tight text-slate-500">
                        {label}
                      </span>
                    </button>
                  );
                })}

                {filteredIcons.length === 0 && (
                  <div className="col-span-full py-6 text-center text-xs text-slate-400">
                    No icons found.
                  </div>
                )}
              </div>
            )}

          </div>

          <div className="flex items-start gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4">
            <HiOutlineInformationCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-500" />
            <div>
              <p className="text-sm font-semibold text-blue-800">Category visibility</p>
              <p className="mt-1 text-xs leading-relaxed text-blue-600/90">
                Once saved, the category will be available for selection while adding or editing products.
              </p>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}