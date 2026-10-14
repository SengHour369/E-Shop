import { useState, useEffect, useCallback } from 'react';
import returnService from '../services/return.service';

export function useReturnSummary() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchSummary = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await returnService.getSummary();
      const body = res?.data ?? res;
      setSummary(body?.payload ?? body);
    } catch (err) {
      console.error('Failed to fetch return summary:', err);
      setError(
        err?.response?.data?.message ||
        err?.message ||
        'Failed to load return summary.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  return {
    summary,
    loading,
    error,
    refetch: fetchSummary
  };
}
