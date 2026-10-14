import { useState, useEffect, useCallback } from 'react';

/**
 * Debounced search hook.
 * @param {number} delay — ms to debounce (default 300ms)
 */
export function useSearch(delay = 300) {
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
    }, delay);
    return () => clearTimeout(timer);
  }, [query, delay]);

  const clear = useCallback(() => {
    setQuery('');
    setDebouncedQuery('');
  }, []);

  return { query, setQuery, debouncedQuery, clear };
}
