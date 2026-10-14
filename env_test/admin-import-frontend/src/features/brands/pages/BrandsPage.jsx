// src/features/brands/pages/BrandsPage.jsx
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import BrandStats from '../components/BrandStats';
import BrandToolbar from '../components/BrandToolbar';
import BrandTable from '../components/BrandTable';
import Pagination from '@/components/ui/Pagination';
import { PageHeader, Button, DataTableCard } from '@/components/ui';
import { HiPlus } from 'react-icons/hi2';
import { brandService } from '@/features/brands/services/brand.service';
import Swal from 'sweetalert2';
import PageContainer from '@/components/layouts/PageContainer';

export default function BrandsPage() {
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [globalStats, setGlobalStats] = useState({ total: 0, active: 0, inactive: 0, deleted: 0 });

  // Filter state
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({ category: '' });

  const hasActive = !!search || !!filters.category;

  const navigate = useNavigate();

  const fetchBrands = useCallback(async (p = page, s = size) => {
    setLoading(true);
    setError(null);
    try {
      const res = await brandService.getAll(p, s);
      const data = res?.data || res;
      const raw = data?.payload ?? data?.content ?? data?.items ?? [];

      if (!Array.isArray(data)) {
        setTotalPages(data.totalPages ?? data.total_pages ?? Math.ceil((data.totalElements ?? data.total ?? raw.length) / size) ?? 1);
        setTotalElements(data.totalElements ?? data.total ?? raw.length ?? 0);
      } else {
        setTotalPages(Math.ceil(raw.length / size));
        setTotalElements(raw.length);
      }

      const list = raw
        .filter(item => item && item.id != null)
        .map(item => ({
          id: item.id,
          name: item.name ?? 'Unnamed brand',
          logo: item.image ?? `https://placehold.co/64x64/1e293b/fff?text=${encodeURIComponent((item.name ?? 'B').slice(0, 2).toUpperCase())}`,
          category_id: item.category_id ?? null,
          category_name: item.category_name ?? 'Uncategorized',
          description: item.description ?? '',
          image: item.image ?? null,
          status: item.status === true ? 'active' : 'inactive',
          created_at: item.created_at ?? null,
        }));

      setBrands(list);
    } catch (err) {
      console.error('Failed to fetch brands:', err);
      setError('Failed to load brands.');
    } finally {
      setLoading(false);
    }
  }, [page, size]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (hasActive) {
        void fetchBrands(1, 1000);
      } else {
        void fetchBrands(page, size);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [page, size, hasActive, fetchBrands]);

  useEffect(() => {
    // Fetch global stats in the background without blocking the main table
    const fetchStats = async () => {
      try {
        const res = await brandService.getAll(1, 10000);
        const data = res?.data || res;
        const raw = data?.payload ?? data?.content ?? data?.items ?? [];
        
        let active = 0, inactive = 0, deleted = 0;
        raw.forEach(item => {
          if (item.status === 'deleted') deleted++;
          else if (item.status === true || item.status === 'active') active++;
          else inactive++; // null, false, or 'inactive'
        });
        setGlobalStats({ total: raw.length, active, inactive, deleted });
      } catch (err) {
        console.error('Failed to fetch global stats:', err);
      }
    };
    void fetchStats();
  }, [brands]); // Re-fetch stats when brands change (e.g., after add/edit/delete)

  const handleDelete = async (id) => {
    const brand = brands.find(item => item.id === id);
    if (!brand) return;
    try {
      await brandService.delete(id);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Brand deleted successfully',
      });
      await fetchBrands();
    } catch (err) {
      console.error('Failed to delete brand:', err);
      setError('Failed to delete brand.');
    }
  };

  // Client-side filtering
  const filteredBrands = useMemo(() => {
    let result = brands;

    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(b =>
        b.name.toLowerCase().includes(q) ||
        b.description.toLowerCase().includes(q) ||
        b.category_name.toLowerCase().includes(q)
      );
    }

    if (filters.category) {
      result = result.filter(b => String(b.category_id) === String(filters.category) || b.category_name === filters.category);
    }

    if (filters.status) {
      result = result.filter(b => b.status === filters.status);
    }

    return result;
  }, [brands, search, filters]);

  const handleFilter = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleReset = () => {
    setSearch('');
    setFilters({ category: '' });
  };

  return (
    <PageContainer>
      <PageHeader
        title="Brands"
        description="Manage product brands and partnerships."
        crumbs={[{ label: 'Dashboard', path: '/dashboard' }, { label: 'Brands' }]}
      >
        <Button className="bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm flex items-center gap-2" onClick={() => navigate('/dashboard/brands/add')}>
          <HiPlus className="h-4 w-4" />
          Add Brand
        </Button>
      </PageHeader>

      <BrandStats 
        brands={brands} 
        totalElements={totalElements} 
        globalStats={globalStats}
        loading={loading} 
      />

      <DataTableCard
        toolbar={
          <BrandToolbar
            search={search}
            onSearch={setSearch}
            filters={filters}
            onFilter={handleFilter}
          />
        }
        loading={loading}
        error={error}
        loadingMessage="Loading brands..."
        footer={
          <>
            <div className="flex items-center gap-3 text-sm text-slate-500">
              <span>Rows per page:</span>
              <select 
                value={size} 
                onChange={(e) => setSize(Number(e.target.value))}
                className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-1.5 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
            <div className="w-full sm:w-auto overflow-hidden rounded-lg border border-slate-100">
              <Pagination
                pageNumber={page}
                totalPages={hasActive ? Math.max(1, Math.ceil(filteredBrands.length / size)) : totalPages}
                pageSize={size}
                totalResults={hasActive ? filteredBrands.length : totalElements}
                onPageChange={setPage}
              />
            </div>
          </>
        }
      >
        <BrandTable
          brands={hasActive ? filteredBrands.slice((page - 1) * size, page * size) : filteredBrands}
          onView={(brand) => navigate(`/dashboard/brands/view/${brand.id}`)}
          onEdit={(brand) => navigate(`/dashboard/brands/edit/${brand.id}`)}
          onDelete={handleDelete}
          loading={loading}
        />
      </DataTableCard>
    </PageContainer>
  );
}
