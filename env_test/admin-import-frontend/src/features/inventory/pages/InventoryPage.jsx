import { useState } from 'react';
import { Plus } from 'lucide-react';

import PageContainer from '@/components/layouts/PageContainer';
import { PageHeader, DataTableCard } from '@/components/ui';
import StatCards from '../components/StatCards';
import Pagination from '@/components/ui/Pagination';
import FilterBar from '../components/FilterBar';
import InventoryTable from '../components/InventoryTable';
import { useNavigate } from 'react-router-dom';
import { useInventory } from '../hooks/useInventory';
import inventoryService from '../services/inventory.service';
import Swal from 'sweetalert2';

export default function InventoryPage() {
  const navigate = useNavigate();
  const {
    data,
    loading,
    error,
    page,
    setPage,
    pageSize,
    totalPages,
    totalElements,
    statSummary,
    refetch,
  } = useInventory(0, 10);

  // ── Handlers ───────────────────────────────────────────────────────────────
  const handleViewClick = (product) => {
    navigate('/dashboard/inventory/details', { state: product });
  };

  const handleEditClick = (product) => {
    navigate('/dashboard/inventory/edit', { state: product });
  };

  const handleDelete = async (id) => {
    try {
      await inventoryService.delete(id);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        icon: 'success',
        title: 'Inventory deleted successfully',
      });
      void refetch();
    } catch (err) {
      console.error('Delete inventory error:', err);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3500,
        timerProgressBar: true,
        icon: 'error',
        title: err.response?.data?.message || err.message || 'Failed to delete inventory',
      });
    }
  };

  // Re-fetch after a successful save
  const handleSaved = () => {
    void refetch();
  };

  // Pagination: UI component uses 1-based, API uses 0-based
  const handlePageChange = (onePage) => {
    setPage(onePage - 1);
  };

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <PageContainer>
      <PageHeader
        title="Inventory"
        description="Manage stock levels and product availability."
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Inventory' }]}
        stats={statSummary}
      >
        <button
          onClick={() => navigate('/dashboard/inventory/add')}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
        >
          <Plus className="h-4 w-4" />
          Create Inventory
        </button>
      </PageHeader>

      <div className="space-y-6 pb-10">
        <StatCards summary={statSummary} loading={loading} />

        {error && (
          <div className="mb-4 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700 shadow-sm">
            <svg className="h-5 w-5 flex-shrink-0 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <p className="font-semibold">Failed to load inventory</p>
              <p className="text-red-500 mt-0.5">{error}</p>
            </div>
          </div>
        )}

        <DataTableCard
          toolbar={<FilterBar />}
          loading={loading}
          loadingMessage="Loading inventory..."
          footer={
            data.length > 0 && (
              <div className="w-full sm:w-auto overflow-hidden rounded-lg border border-slate-100">
                <Pagination
                  pageNumber={page + 1}
                  totalPages={totalPages}
                  pageSize={pageSize}
                  totalResults={totalElements}
                  onPageChange={handlePageChange}
                />
              </div>
            )
          }
        >
          <InventoryTable
            data={data}
            onViewClick={handleViewClick}
            onEditClick={handleEditClick}
            onDelete={handleDelete}
            loading={loading}
          />
        </DataTableCard>
      </div>
    </PageContainer>
  );
}
