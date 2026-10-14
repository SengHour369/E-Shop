import { useEffect, useState, useCallback } from 'react';
import { categoryApi } from '@/features/categories/services/category.service';

export function useCategories(initialParams = {}) {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchCategories = useCallback(async (params = {}) => {
    try {
      setLoading(true);
      setError('');
      const p = params.page !== undefined ? params.page : 0;
      const s = params.size !== undefined ? params.size : 1000;

      const res = await categoryApi.getAll(p, s);
      console.log("Res Cate", res);
      const raw = Array.isArray(res)
        ? res
        : (res?.data?.payload ?? res?.payload ?? res?.data ?? res?.content ?? res?.items ?? []);

      if (!Array.isArray(res)) {
        setTotalPages(res.totalPages ?? res.total_pages ?? Math.ceil((res.totalElements ?? res.total ?? raw.length) / s) ?? 1);
        setTotalElements(res.totalElements ?? res.total ?? raw.length ?? 0);
      } else {
        setTotalPages(Math.ceil(raw.length / s));
        setTotalElements(raw.length);
      }
      const list = raw
        .map(item => (item?.data != null ? item.data : item))
        .filter(item => item && item.id != null);
      setCategories(list);
    } catch {
      setError('Failed to load categories');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (initialParams.autoFetch !== false) {
      fetchCategories(initialParams);
    }
  }, [fetchCategories]);

  const handleCategoryChange = (e) => {
    setSelectedCategoryId(e.target.value);
  };

  return {
    categories,
    loading,
    error,
    totalPages,
    totalElements,
    selectedCategoryId,
    setSelectedCategoryId,
    handleCategoryChange,
    fetchCategories,
  };
}

export default useCategories;