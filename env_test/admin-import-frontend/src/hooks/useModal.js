import { useState, useCallback } from 'react';

/**
 * Hook for managing modal open/close state.
 * @param {boolean} initialState
 */
export function useModal(initialState = false) {
  const [isOpen, setIsOpen] = useState(initialState);
  const [data, setData] = useState(null);

  const open = useCallback((payload = null) => {
    setData(payload);
    setIsOpen(true);
  }, []);

  const close = useCallback(() => {
    setIsOpen(false);
    // Slight delay before clearing data so close animation can finish
    setTimeout(() => setData(null), 200);
  }, []);

  return { isOpen, data, open, close };
}
