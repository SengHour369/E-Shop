// src/features/payments/hooks/useTransactionDetail.js
import { useState, useEffect, useCallback } from 'react';
import { transactionService } from '../services/transactions.service';

export function useTransactionDetail(id) {
  const [transaction, setTransaction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDetail = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const res = await transactionService.getById(id);
      console.log("jjj", res);
      // Depending on API structure, data might be in res.data, res.data.payload, or res.data.data
      const data = res?.data?.data || res?.data?.payload || res?.data;
      setTransaction(data);
    } catch (err) {
      setError(
        err?.response?.data?.message ||
        err?.message ||
        'Failed to load transaction details.'
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  return { transaction, loading, error, refetch: fetchDetail };
}
