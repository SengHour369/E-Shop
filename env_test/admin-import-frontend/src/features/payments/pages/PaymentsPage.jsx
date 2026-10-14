import { PageHeader, DataTableCard, TableSkeleton } from '@/components/ui';
import PageContainer from '@/components/layouts/PageContainer';
import Pagination from '@/components/ui/Pagination';
import { paymentStats } from '../services/payment.service';
import PaymentStats from '../components/PaymentStats';
import PaymentToolbar from '../components/PaymentToolbar';
import PaymentTable from '../components/PaymentTable';
import { usePayments } from '../hooks/usePayments';

export default function PaymentsPage() {
  const {
    payments,
    loading,
    error,
    page,
    setPage,
    size,
    setSize,
    search,
    handleSearch,
    filters,
    handleFilter,
    totalResults,
    stats
  } = usePayments();

  return (
    <PageContainer>
      <PageHeader
        title="Payment Details"
        description="Summary of all payments for the selected period"
        crumbs={[
          { label: "Dashboard", path: "/" },
          { label: "Payment Details" }
        ]}
      />
      
      <PaymentStats stats={stats} />

      <DataTableCard
        toolbar={
          <PaymentToolbar
            search={search}
            onSearch={handleSearch}
            filters={filters}
            onFilter={handleFilter}
          />
        }
        loading={loading}
        error={error}
        loadingMessage="Loading payments..."
        footer={
          (Math.max(1, Math.ceil(totalResults / size)) > 1 || totalResults > 0) && (
            <div className="w-full sm:w-auto overflow-hidden rounded-lg border border-slate-100 ml-auto">
              <Pagination
                pageNumber={page}
                totalPages={Math.max(1, Math.ceil(totalResults / size))}
                pageSize={size}
                totalResults={totalResults}
                onPageChange={setPage}
              />
            </div>
          )
        }
      >
        {loading && payments.length === 0 ? (
          <div className="p-4">
            <TableSkeleton rows={size} cols={7} />
          </div>
        ) : (
          <PaymentTable
            payments={payments}
            loading={loading}
          />
        )}
      </DataTableCard>
    </PageContainer>
  );
}
