import { useState, useEffect, useCallback } from 'react';
import returnService from '../services/return.service';

export function useReturns({ page = 1, size = 10, criteria_type = 0, criteria_value = '' } = {}) {
  const [returns, setReturns] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchReturns = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await returnService.getAll({ page, size, criteria_type, criteria_value });
      console.log("Return",res);

      const body = res?.data ?? res;

      if (body?.payload) {
        setReturns(body.payload);
        setTotalElements(body.totalElements ?? body.payload.length);
        setTotalPages(body.totalPages ?? 1);
      } else if (Array.isArray(body)) {
        setReturns(body);
        setTotalElements(body.length);
        setTotalPages(1);
      } else if (Array.isArray(res?.payload)) {
        setReturns(res.payload);
        setTotalElements(res.totalElements ?? res.payload.length);
        setTotalPages(res.totalPages ?? 1);
      } else {
        setReturns([]);
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
        err?.data?.message ||
        err.message ||
        'Failed to load returns.'
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, criteria_type, criteria_value]);

  useEffect(() => {
    fetchReturns();
  }, [fetchReturns]);

  return {
    returns,
    totalElements,
    totalPages,
    loading,
    error,
    refetch: fetchReturns,
  };
}

export default useReturns;