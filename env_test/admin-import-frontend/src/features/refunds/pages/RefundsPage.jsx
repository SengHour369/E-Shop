import { useState } from 'react';
import { PageHeader, Badge, Button } from '@/components/ui';
import { refundStats } from '@/features/reports/components/PageStats';
import { HiOutlineMagnifyingGlass, HiOutlineFunnel, HiArrowPath } from 'react-icons/hi2';
import { formatCurrency } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import PageContainer from '@/components/layouts/PageContainer';
import { useRefunds } from '../hooks/useRefunds';

const criteriaOptions = [
  { value: 0, label: 'All' },
  { value: 1, label: 'Refund ID' },
  { value: 2, label: 'Order No' },
  { value: 3, label: 'Customer Name' },
  { value: 4, label: 'Status' },
  { value: 5, label: 'Date Range' },
];

export default function RefundsPage() {
  const {
    data,
    loading,
    error,
    page,
    setPage,
    totalPages,
    criteriaType,
    setCriteriaType,
    criteriaValue,
    setCriteriaValue,
    refetch
  } = useRefunds(1, 10);

  const [tempSearchValue, setTempSearchValue] = useState(criteriaValue);

  const handleSearch = () => {
    setCriteriaValue(tempSearchValue);
    setPage(1); // Reset to first page on search
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <PageContainer>
      <PageHeader
        title="Refunds"
        description="Manage customer refunds and financial adjustments."
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Payments' }, { label: 'Refunds' }]}
        stats={refundStats}
      />

      <div className="card flex flex-col sm:flex-row justify-between gap-4">
        <div className="flex flex-1 gap-2">
          <select
            value={criteriaType}
            onChange={(e) => {
              setCriteriaType(Number(e.target.value));
              setTempSearchValue('');
              setCriteriaValue('');
            }}
            className="rounded-lg border border-slate-200 bg-white py-2 pl-3 pr-8 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
          >
            {criteriaOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {criteriaType !== 0 && (
            <div className="relative flex-1 max-w-sm">
              <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder={`Search by ${criteriaOptions.find(o => o.value === criteriaType)?.label}...`}
                value={tempSearchValue}
                onChange={e => setTempSearchValue(e.target.value)}
                onKeyDown={handleKeyDown}
                className="w-full rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm placeholder-slate-400 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
              />
            </div>
          )}
          
          <Button variant="primary" onClick={handleSearch} disabled={loading}>
            Search
          </Button>
          <Button variant="secondary" onClick={refetch} disabled={loading} title="Refresh">
            <HiArrowPath className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
        <Button variant="secondary">
          <HiOutlineFunnel className="h-4 w-4" />
          Filter
        </Button>
      </div>

      {error && (
        <div className="bg-red-50 text-red-600 p-4 rounded-lg mt-4">
          {error}
        </div>
      )}

      <div className="card overflow-hidden p-0 mt-4">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="border-b border-slate-100 bg-slate-50/50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-6 py-4 font-medium">Refund ID</th>
                <th className="px-6 py-4 font-medium">Order No</th>
                <th className="px-6 py-4 font-medium">Customer Name</th>
                <th className="px-6 py-4 font-medium">Date Requested</th>
                <th className="px-6 py-4 font-medium">Amount</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan="7" className="px-6 py-8 text-center text-slate-500">
                    Loading refunds...
                  </td>
                </tr>
              ) : data.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-6 py-8 text-center text-slate-500">
                    No refunds found.
                  </td>
                </tr>
              ) : (
                data.map((item, idx) => (
                  <tr key={item.refundId || item.id || idx} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4 font-medium text-slate-900">{item.refundId || item.id || '-'}</td>
                    <td className="px-6 py-4 font-medium text-blue-600">{item.orderNo || item.orderId || '-'}</td>
                    <td className="px-6 py-4">{item.customerName || item.customer?.name || '-'}</td>
                    <td className="px-6 py-4">{formatDate(item.createdAt || item.refundDate || item.date)}</td>
                    <td className="px-6 py-4 font-medium text-slate-900">{formatCurrency(item.amount || item.refundAmount || 0)}</td>
                    <td className="px-6 py-4">
                      <Badge variant={(item.status || '').toLowerCase() === 'processed' ? 'success' : 'warning'} dot>
                        {item.status || 'Pending'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {(item.status || '').toLowerCase() === 'pending' ? (
                        <button className="text-blue-600 hover:text-blue-800 text-xs font-semibold">Process</button>
                      ) : (
                        <button className="text-slate-400 hover:text-slate-600 text-xs font-semibold">View</button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        
        {/* Pagination controls can be added here if needed */}
        {!loading && totalPages > 1 && (
          <div className="p-4 border-t border-slate-100 flex items-center justify-between text-sm">
            <span className="text-slate-500">
              Page {page} of {totalPages}
            </span>
            <div className="flex gap-2">
              <Button 
                variant="secondary" 
                disabled={page <= 1} 
                onClick={() => setPage(p => Math.max(1, p - 1))}
              >
                Previous
              </Button>
              <Button 
                variant="secondary" 
                disabled={page >= totalPages} 
                onClick={() => setPage(p => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
