import { useState, useCallback } from 'react';

/**
 * Generic filter state hook.
 * @param {object} initialFilters — initial filter values
 */
export function useFilter(initialFilters = {}) {
  const [filters, setFilters] = useState(initialFilters);

  const setFilter = useCallback((key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(initialFilters);
  }, [initialFilters]);

  const hasActiveFilters = Object.entries(filters).some(
    ([, val]) => val !== '' && val !== null && val !== undefined
  );

  return { filters, setFilter, resetFilters, hasActiveFilters };
}
