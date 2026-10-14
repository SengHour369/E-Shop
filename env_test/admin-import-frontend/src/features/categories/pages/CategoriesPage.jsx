import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import CategoriesTable from '../components/CategoriesTable';
import CategoriesFilter from '../components/CategoriesFilter';
import { Button, PageHeader, DataTableCard } from '@/components/ui';
import Pagination from '@/components/ui/Pagination';
import { categoryStats } from '@/features/reports/components/PageStats';
import { HiPlus } from 'react-icons/hi2';
import { categoryApi } from '@/features/categories/services/category.service';
import Swal from 'sweetalert2';
import PageContainer from '@/components/layouts/PageContainer';
import { useCategories } from '@/features/categories/hooks/useCategories';

export default function CategoriesPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const { categories, loading, error, totalPages, totalElements, fetchCategories } = useCategories({ autoFetch: false });

  // Map real data to stats
  const mappedStats = useMemo(() => {
    const total = totalElements; // Use the global total from pagination
    const active = categories.filter(c => c.status === 'active' || c.status === true).length;
    const inactive = categories.filter(c => c.status === 'inactive' || c.status === false).length;

    return categoryStats.map(stat => {
      let newValue = stat.value;
      let badgeText = "";

      if (stat.label === "Total Categories") newValue = total;
      if (stat.label === "Active Categories") newValue = active;
      if (stat.label === "Top Performing") newValue = 0;
      if (stat.label === "Inactive Categories") newValue = inactive;

      return { ...stat, value: newValue, badgeText };
    });
  }, [categories]);

  // Filter state
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({ status: '' });

  const hasActive = !!search || !!filters.status;

  useEffect(() => {
    const timer = setTimeout(() => {
      if (hasActive) {
        fetchCategories({ page: 0, size: 1000 });
      } else {
        fetchCategories({ page, size });
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [page, size, hasActive, fetchCategories]);

  const handleDelete = async (id) => {
    try {
      await categoryApi.delete(id);
      await fetchCategories({ page, size });

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
      Toast.fire({
        icon: 'success',
        title: 'Category deleted successfully!'
      });
    } catch (err) {
      console.error('Failed to delete category:', err);
      const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
      });
      Toast.fire({
        icon: 'error',
        title: err?.response?.data?.message || 'Failed to delete category.'
      });
    }
  };

  // Client-side filtering
  const filteredCategories = useMemo(() => {
    let result = categories;
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(cat =>
        cat.name?.toLowerCase().includes(q) ||
        cat.description?.toLowerCase().includes(q)
      );
    }
    if (filters.status) {
      result = result.filter(cat => cat.status === filters.status);
    }
    return result;
  }, [categories, search, filters]);

  const handleFilter = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleReset = () => {
    setSearch('');
    setFilters({ status: '' });
  };

  return (
    <PageContainer>
      <PageHeader
        title="Categories"
        crumbs={[{ label: 'Dashboard', path: '/dashboard' }, { label: 'Products', path: '/dashboard/products' }, { label: 'Categories' }]}
        stats={mappedStats}
        loading={loading}
      >
        <Button className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm" onClick={() => navigate('/dashboard/categories/add')}>
          <HiPlus className="h-4 w-4" />
          Add Category
        </Button>
      </PageHeader>

      {/* Filter Bar */}
      <DataTableCard
        toolbar={
          <CategoriesFilter
            search={search}
            onSearch={setSearch}
            filters={filters}
            onFilter={handleFilter}
          />
        }
        loading={loading}
        error={error}
        loadingMessage="Loading categories..."
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
                totalPages={hasActive ? Math.max(1, Math.ceil(filteredCategories.length / size)) : totalPages}
                pageSize={size}
                totalResults={hasActive ? filteredCategories.length : totalElements}
                onPageChange={setPage}
              />
            </div>
          </>
        }
      >
        <CategoriesTable
          categories={hasActive ? filteredCategories.slice((page - 1) * size, page * size) : filteredCategories}
          onView={(cat) => navigate(`/dashboard/categories/view/${cat.id}`)}
          onEdit={(cat) => navigate(`/dashboard/categories/edit/${cat.id}`)}
          onDelete={handleDelete}
          loading={loading}
        />
      </DataTableCard>
    </PageContainer>
  );
}
