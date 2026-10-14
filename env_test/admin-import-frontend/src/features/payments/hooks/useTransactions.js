// src/features/payments/hooks/useTransactions.js
import { useState, useEffect, useCallback } from 'react';
import transactionService from '../services/transactions.service';

/**
 * Hook to fetch and manage all transactions with pagination.
 *
 * NOTE: axiosClient interceptor already unwraps `response.data`,
 * so the resolved value from the service IS the response body directly.
 */
export function useTransactions({ page = 1, size = 10, criteria_type = 0, criteria_value = '' } = {}) {
  const [transactions, setTransactions] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // The interceptor already unwraps response.data, so `res` IS the body
      const res = await transactionService.getAll({ page, size, criteria_type, criteria_value });
      console.log("res",res);

      // Handle: { data: { payload: [...], totalElements, totalPages } }
      const body = res?.data ?? res;

      if (body?.payload) {
        setTransactions(body.payload);
        setTotalElements(body.totalElements ?? body.payload.length);
        setTotalPages(body.totalPages ?? 1);
      } else if (Array.isArray(body)) {
        setTransactions(body);
        setTotalElements(body.length);
        setTotalPages(1);
      } else if (Array.isArray(res?.payload)) {
        // top-level payload (interceptor unwrapped one level)
        setTransactions(res.payload);
        setTotalElements(res.totalElements ?? res.payload.length);
        setTotalPages(res.totalPages ?? 1);
      } else {
        setTransactions([]);
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
        err?.data?.message ||
        err.message ||
        'Failed to load transactions.'
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, criteria_type, criteria_value]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  return {
    transactions,
    totalElements,
    totalPages,
    loading,
    error,
    refetch: fetchTransactions,
  };
}

export default useTransactions;
