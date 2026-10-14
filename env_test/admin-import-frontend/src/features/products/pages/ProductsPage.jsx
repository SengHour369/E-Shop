import { useState, useEffect, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ProductsTable from '../components/ProductsTable';
import ProductFilters from '../components/ProductFilters';
import Pagination from '@/components/ui/Pagination';
import { useSearch } from '@/hooks/useSearch';
import { useFilter } from '@/features/products/hooks/useFilter';
import { Button, PageHeader, DataTableCard } from '@/components/ui';
import { HiPlus } from 'react-icons/hi2';
import { productStats } from '@/features/reports/components/PageStats';
import { productApi } from '@/features/products/services/product.service';
import { brandService } from '@/features/brands/services/brand.service';
import PageContainer from '@/components/layouts/PageContainer';
import Swal from 'sweetalert2';


const CRITERIA_TYPE = {
  ALL: 0,
  NAME: 1,
  SUB_CATEGORY_ID: 2,
  CATEGORY_ID: 3,
  ACTIVE: 4,
  PRODUCT_ID: 5,
  PRODUCT_WITH_SKUS: 6,
};

export default function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pageNumber, setPageNumber] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);
  const [brandsMap, setBrandsMap] = useState({});

  useEffect(() => {
    // Fetch brands once to map subcategoryId to brand and category names
    brandService.getAll(1, 1000).then(res => {
      const data = res?.data?.payload || res?.data?.content || res?.data || [];
      const raw = Array.isArray(data) ? data : (data.payload || []);
      const map = {};
      raw.forEach(b => {
        if (b && b.id) {
          map[b.id] = { brand: b.name, category: b.category_name };
        }
      });
      setBrandsMap(map);
    }).catch(err => console.error("Failed to load brands map", err));
  }, []);

  const { query, setQuery, debouncedQuery } = useSearch();

  const initialFilters = useMemo(() => ({ category: '', status: '' }), []);
  const { filters, setFilter, resetFilters, hasActiveFilters } = useFilter(initialFilters);

  const { criteriaType, criteriaValue } = useMemo(() => {
    if (debouncedQuery) {
      return { criteriaType: CRITERIA_TYPE.NAME, criteriaValue: debouncedQuery };
    }
    if (filters.category) {
      return { criteriaType: CRITERIA_TYPE.CATEGORY_ID, criteriaValue: filters.category };
    }
    if (filters.status === 'active') {
      return { criteriaType: CRITERIA_TYPE.ACTIVE, criteriaValue: '' };
    }
    return { criteriaType: CRITERIA_TYPE.ALL, criteriaValue: '' };
  }, [debouncedQuery, filters.category, filters.status]);

  const mappedStats = useMemo(() => {
    const total = totalItems;
    const active = products.filter(p => p.is_active).length;
    const lowStock = products.filter(p => p.stock > 0 && p.stock <= 10).length;
    const outOfStock = products.filter(p => p.stock === 0).length;

    return productStats.map(stat => {
      let newValue = stat.value;
      let badgeText = "";

      if (stat.label === "Total Products") newValue = total;
      if (stat.label === "Active Products") newValue = active;
      if (stat.label === "Low Stock") newValue = lowStock;
      if (stat.label === "Out of Stock") newValue = outOfStock;

      return { ...stat, value: newValue, badgeText };
    });
  }, [products, totalItems]);

  // Reset ទៅ page 1 រាល់ពេល search ឬ filter ប្តូរ
  useEffect(() => {
    setPageNumber(1);
  }, [debouncedQuery, filters.category, filters.status]);

  useEffect(() => {
    let isCancelled = false;

    const fetchProducts = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await productApi.getAll({
          page: pageNumber,
          size: pageSize,
          criteria_type: criteriaType,
          criteria_value: criteriaValue,
        });
        console.log("Product", res);
        if (isCancelled) return;

        const { payload, total_pages, total_items } = res.data;

        // We just store the raw products and total info
        setProducts(payload || []);
        setTotalPages(total_pages);
        setTotalItems(total_items);
      } catch (err) {
        if (!isCancelled) setError(err);
      } finally {
        if (!isCancelled) setLoading(false);
      }
    };

    fetchProducts();

    return () => {
      isCancelled = true;
    };
  }, [pageNumber, pageSize, criteriaType, criteriaValue]);

  // Transform the nested API data into a flat format for the table
  const formattedProducts = useMemo(() => {
    return products.map(product => {
      const defaultSku = product.skus?.find(s => s.is_default) || product.skus?.[0] || {};

      return {
        id: product.id,
        name: product.name,
        image: product.main_image?.[0] || 'https://upload.wikimedia.org/wikipedia/commons/6/65/No-Image-Placeholder.svg',
        sku: defaultSku.sku || 'N/A',
        price: defaultSku.price || 0,
        stock: defaultSku.quantity ?? 0,
        is_active: product.is_active,
        category: brandsMap[product.subcategoryId]?.category || product.category?.name || 'N/A',
        brand: brandsMap[product.subcategoryId]?.brand || product.brand?.name || 'N/A',
        createdAt: product.createdAt ? new Date(product.createdAt).toLocaleString() : (product.created_at || 'N/A'),
        updatedAt: product.updated_at ? new Date(product.updated_at).toLocaleString() : 'N/A'
      };
    });
  }, [products, brandsMap]);

  // "inactive" មិនមាន criteria_type ដាច់ដោយឡែកទេ (មានតែ ACTIVE=4)
  // ដូច្នេះនៅតែត្រូវ filter "inactive" នៅ client-side
  const filtered = formattedProducts.filter(p => {
    if (filters.status === 'inactive') return p.is_active !== true;
    return true;
  });

  const handleDelete = async (id) => {
    try {
      await productApi.delete(id);
      setProducts(prev => prev.filter(p => p.id !== id));
      setTotalItems(prev => Math.max(0, prev - 1));
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Product deleted successfully'
      });
    } catch (err) {
      console.error('Failed to delete product', err);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'error',
        title: err?.response?.data?.message || err?.message || 'Failed to delete product'
      });
      setError(null);
    }
  };

  const handlePageChange = (nextPage) => {
    if (nextPage < 1 || nextPage > totalPages || nextPage === pageNumber) return;
    setPageNumber(nextPage);
  };

  return (
    <PageContainer>
      <PageHeader
        title="Products"
        description="Manage your product inventory and pricing."
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Products' }]}
        stats={mappedStats}
        loading={loading}
      >
        <Link to="/dashboard/products/add">
          <Button className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm">
            <HiPlus className="h-4 w-4" />
            Add Product
          </Button>
        </Link>
      </PageHeader>

      <DataTableCard
        toolbar={
          <ProductFilters
            search={query}
            onSearch={setQuery}
            filters={filters}
            onFilter={setFilter}
            onReset={resetFilters}
            hasActive={hasActiveFilters}
          />
        }
        loading={loading}
        error={error && error.message}
        loadingMessage="Loading products..."
        footer={
          !loading && !error && filtered.length > 0 && (
            <>
              <div className="flex items-center gap-3 text-sm text-slate-500">
                <span>Rows per page:</span>
                <select
                  value={pageSize}
                  onChange={(e) => setPageSize(Number(e.target.value))}
                  className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-1.5 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
              </div>
              <div className="w-full sm:w-auto overflow-hidden rounded-lg border border-slate-100">
                <Pagination
                  pageNumber={pageNumber}
                  totalPages={totalPages}
                  pageSize={pageSize}
                  totalResults={totalItems}
                  onPageChange={handlePageChange}
                />
              </div>
            </>
          )
        }
      >
        {!error && <ProductsTable products={filtered} onDelete={handleDelete} loading={loading} />}
      </DataTableCard>
    </PageContainer>
  );
}
