// src/features/brands/pages/BrandDetailPage.jsx
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { HiOutlinePencilSquare, HiOutlineArrowLeft } from 'react-icons/hi2';
import Button from '@/components/ui/Button';
import { brandService } from '@/features/brands/services/brand.service';
import PageContainer from '@/components/layouts/PageContainer';
import Badge from '@/components/ui/Badge';
import SkeletonBlock from '@/components/ui/Skeleton';

export default function BrandDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [brand, setBrand] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;

    const fetchBrand = async () => {
      try {
        setLoading(true);
        setError('');
        const res = await brandService.getById(id);
        const payloadData = res?.data?.payload ?? res?.payload ?? res?.data ?? [];
        const fetchedBrand = Array.isArray(payloadData) ? payloadData[0] : payloadData;
        
        if (fetchedBrand) {
          setBrand(fetchedBrand);
        } else {
          setError('Brand not found.');
        }
      } catch (err) {
        console.error('Failed to load brand:', err);
        setError('Failed to load brand details.');
      } finally {
        setLoading(false);
      }
    };

    void fetchBrand();
  }, [id]);

  const getStatusBadgeVariant = (status) => {
    if (status === true || status === 'active') return 'success';
    if (status === false || status === 'inactive') return 'warning';
    return 'default';
  };

  return (
    <PageContainer>
      <div className="flex flex-col justify-between gap-4 py-4 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Brand Details</h2>
          <div className="mt-1 flex items-center gap-2 text-xs font-medium text-slate-500">
            <Link to="/dashboard" className="transition-colors hover:text-blue-600">Dashboard</Link>
            <span className="text-slate-300">›</span>
            <Link to="/dashboard/brands" className="transition-colors hover:text-blue-600">Brands</Link>
            <span className="text-slate-300">›</span>
            <span className="text-slate-400">{brand?.name || 'Details'}</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={() => navigate('/dashboard/brands')} className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm flex items-center gap-2">
            <HiOutlineArrowLeft className="h-4 w-4" />
            Back to Brands
          </Button>
          <Button onClick={() => navigate(`/dashboard/brands/edit/${id}`)} className="bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm flex items-center gap-2">
            <HiOutlinePencilSquare className="h-4 w-4" />
            Edit Brand
          </Button>
        </div>
      </div>

      {error && (
        <div className="rounded-xl border px-4 py-3 text-sm border-red-200 bg-red-50 text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
              <SkeletonBlock className="h-5 w-40 mb-6" />
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <SkeletonBlock className="h-4 w-24 mb-2" />
                  <SkeletonBlock className="h-5 w-48" />
                </div>
                <div>
                  <SkeletonBlock className="h-4 w-20 mb-2" />
                  <SkeletonBlock className="h-6 w-32" />
                </div>
                <div>
                  <SkeletonBlock className="h-4 w-16 mb-2" />
                  <SkeletonBlock className="h-6 w-20 rounded-full" />
                </div>
                <div>
                  <SkeletonBlock className="h-4 w-24 mb-2" />
                  <SkeletonBlock className="h-5 w-32" />
                </div>
                <div className="sm:col-span-2">
                  <SkeletonBlock className="h-4 w-24 mb-2" />
                  <div className="rounded-lg border border-slate-100 bg-slate-50 p-4 space-y-2">
                    <SkeletonBlock className="h-4 w-full" />
                    <SkeletonBlock className="h-4 w-5/6" />
                    <SkeletonBlock className="h-4 w-4/6" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col items-center">
              <SkeletonBlock className="h-5 w-24 self-start mb-6" />
              
              <div className="relative flex items-center justify-center rounded-xl border border-slate-200 bg-slate-50 p-4 w-full aspect-square max-h-64">
                <SkeletonBlock className="h-full w-full rounded-lg" />
              </div>
            </div>
          </div>
        </div>
      ) : brand ? (
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
          <div className="space-y-6 lg:col-span-2">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
              <h3 className="text-base font-semibold text-slate-900 mb-6">Brand Information</h3>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <p className="text-sm font-medium text-slate-500">Brand Name</p>
                  <p className="mt-1 text-base font-semibold text-slate-900">{brand.name}</p>
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Category</p>
                  <p className="mt-1 text-base text-slate-900">
                    <span className="inline-flex items-center rounded-md bg-slate-100 px-2.5 py-1 text-sm font-medium text-slate-600">
                      {brand.category_name || 'N/A'}
                    </span>
                  </p>
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Status</p>
                  <div className="mt-1">
                    <Badge variant={getStatusBadgeVariant(brand.status)}>
                      {brand.status === true ? 'Active' : brand.status === false ? 'Inactive' : 'Active'}
                    </Badge>
                  </div>
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-500">Created At</p>
                  <p className="mt-1 text-base text-slate-900">
                    {brand.created_at ? new Date(brand.created_at).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                  </p>
                </div>
                <div className="sm:col-span-2">
                  <p className="text-sm font-medium text-slate-500">Description</p>
                  <p className="mt-2 text-sm text-slate-700 whitespace-pre-line leading-relaxed rounded-lg border border-slate-100 bg-slate-50 p-4">
                    {brand.description || 'No description available.'}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col items-center">
              <h3 className="text-base font-semibold text-slate-900 self-start w-full mb-6">Brand Logo</h3>
              
              {brand.image ? (
                <div className="relative flex items-center justify-center rounded-xl border border-slate-200 bg-slate-50 p-4 w-full aspect-square max-h-64">
                  <img
                    src={brand.image}
                    alt={brand.name}
                    className="h-full w-full object-contain"
                  />
                </div>
              ) : (
                <div className="relative flex items-center justify-center rounded-xl border border-slate-200 bg-slate-50 p-4 w-full aspect-square max-h-64">
                  <span className="text-slate-400 font-medium">No Logo Available</span>
                </div>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </PageContainer>
  );
}
