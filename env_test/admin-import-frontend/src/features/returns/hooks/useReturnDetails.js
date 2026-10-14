import { useState, useEffect, useCallback } from 'react';
import returnService from '../services/return.service';

export function useReturnDetails(id) {
  const [data, setData] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchDetails = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const [detailsRes, historyRes] = await Promise.all([
        returnService.getById(id),
        returnService.getHistory(id).catch(() => null),
      ]);

      const body = detailsRes?.data ?? detailsRes;
      setData(body?.payload ?? body);

      if (historyRes) {
        const hBody = historyRes?.data ?? historyRes;
        // API returns { message, code, data: [...] }
        const hPayload = hBody?.data ?? hBody?.payload ?? hBody;
        setHistory(Array.isArray(hPayload) ? hPayload : []);
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
        err?.data?.message ||
        err.message ||
        'Failed to load return details.'
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchDetails();
  }, [fetchDetails]);

  return {
    data,
    history,
    loading,
    error,
    refetch: fetchDetails,
  };
}

export default useReturnDetails;
