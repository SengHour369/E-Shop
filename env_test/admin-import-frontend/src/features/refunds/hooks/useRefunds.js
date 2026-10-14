import { useState, useCallback, useEffect } from 'react';
import refundsService from '../services/refunds.service';

export function useRefunds(initialPage = 1, initialPageSize = 10) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [criteriaType, setCriteriaType] = useState(0);
  const [criteriaValue, setCriteriaValue] = useState("");

  const fetchRefunds = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        page: page,
        size: pageSize,
        criteria_type: criteriaType,
        criteria_value: criteriaValue
      };
      const res = await refundsService.getList(payload);
      
      // Assuming response has content, totalPages, totalElements similar to standard spring boot paginated responses
      const resData = res?.data || res; // depending on axios response interceptor
      const content = resData?.content || [];
      setData(content);
      setTotalPages(resData?.totalPages || 1);
      setTotalElements(resData?.totalElements || content.length);
    } catch (err) {
      console.error('Failed to fetch refunds:', err);
      setError('Failed to load refunds. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, criteriaType, criteriaValue]);

  useEffect(() => {
    fetchRefunds();
  }, [fetchRefunds]);

  return {
    data,
    loading,
    error,
    page,
    setPage,
    pageSize,
    setPageSize,
    totalPages,
    totalElements,
    criteriaType,
    setCriteriaType,
    criteriaValue,
    setCriteriaValue,
    refetch: fetchRefunds,
  };
}