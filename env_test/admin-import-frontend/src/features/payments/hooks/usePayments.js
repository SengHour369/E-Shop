import { useState, useCallback, useEffect } from 'react';
import { paymentService } from '../services/payment.service';

export function usePayments() {
  const [payments, setPayments] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({ status: '', method: '' });
  
  const [totalResults, setTotalResults] = useState(0);

  const fetchPayments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let criteria_type = 0;
      let criteria_value = '';
      
      if (filters.status) {
        criteria_type = 3;
        criteria_value = filters.status;
      } else if (filters.method) {
        criteria_type = 4;
        criteria_value = filters.method;
      }

      const res = await paymentService.getAll({ 
        page, 
        size,
        criteria_type,
        criteria_value
      });
      console.log("Res Pay",res);
      let raw = res?.data?.payload || [];
      
      // Client side search filter 
      if (search.trim()) {
        const q = search.toLowerCase();
        raw = raw.filter(p => 
          (p.code && p.code.toLowerCase().includes(q)) || 
          (p.order_number && p.order_number.toLowerCase().includes(q)) ||
          (p.transaction_id && p.transaction_id.toLowerCase().includes(q))
        );
      }
      
      // Secondary client-side filters if multiple filters are selected
      if (filters.status && filters.method) {
        raw = raw.filter(p => p.payment_method === filters.method);
      }
      
      setPayments(raw);
      setTotalResults(res?.data?.total || raw.length);

      // --- Compute Real Stats from All Payments ---
      try {
        const allRes = await paymentService.getAll({ page: 1, size: 10000 });
        const allPayments = allRes?.data?.payload || [];
        
        let totalValue = 0, successfulValue = 0, pendingValue = 0, failedValue = 0;

        allPayments.forEach(p => {
          const amt = Number(p.amount) || 0;
          totalValue += amt;
          const status = String(p.status || '').toUpperCase();
          
          if (['COMPLETED', 'PAID', 'SUCCESS'].includes(status)) {
            successfulValue += amt;
          } else if (status === 'PENDING') {
            pendingValue += amt;
          } else if (['FAILED', 'CANCELLED'].includes(status)) {
            failedValue += amt;
          }
        });

        setStats({
          total: { value: totalValue },
          successful: { value: successfulValue },
          pending: { value: pendingValue },
          failed: { value: failedValue }
        });
      } catch (statErr) {
        console.error('Failed to compute stats:', statErr);
      }

    } catch (err) {
      console.error('Failed to fetch payments:', err);
      setError('Failed to load payments.');
    } finally {
      setLoading(false);
    }
  }, [page, size, filters.status, filters.method, search]);

  useEffect(() => {
    void fetchPayments();
  }, [fetchPayments]);

  const handleFilter = useCallback((key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPage(1);
  }, []);

  const handleSearch = useCallback((value) => {
    setSearch(value);
    setPage(1);
  }, []);

  return {
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
    stats,
    refetch: fetchPayments
  };
}