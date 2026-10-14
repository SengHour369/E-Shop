// src/features/brands/hooks/useBrands.js
// Previously: features/subcategorie/hooks/useSubCategories.js
// Renamed: now this hook loads brands (sub-categories) filtered by category.
import { useEffect, useRef, useState } from 'react';
import { brandService } from '@/features/brands/services/brand.service';

/**
 * useBrands
 *
 * Loads brands (sub-categories) for a given category id.
 *
 * @param {string|number} categoryId
 * @param {string|number|null} initialBrandId - optional brand id to restore
 *   once its list has loaded. Used when hydrating an edit form from an
 *   existing product where the brand is already known. Only ever applied once.
 */
export function useBrands(categoryId, initialBrandId = null) {
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedBrandId, setSelectedBrandId] = useState('');

  const hasRestoredRef = useRef(false);

  useEffect(() => {
    setSelectedBrandId('');

    if (!categoryId) {
      setBrands([]);
      return;
    }

    let cancelled = false;

    const fetchBrands = async () => {
      try {
        setLoading(true);
        const res = await brandService.getByCategoryId(categoryId, 1, 1000);
        console.log('[useBrands] raw API response:', res);
        const raw = Array.isArray(res)
          ? res
          : (
              res?.data?.payload ??
              res?.payload ??
              (Array.isArray(res?.data) ? res.data : null) ??
              res?.content ??
              res?.items ??
              []
            );
        console.log('[useBrands] extracted raw list:', raw);
        const list = raw
          .map(item => (item?.data != null ? item.data : item))
          .filter(item => item && item.id != null);

        if (cancelled) return;

        setBrands(list);

        const canRestore =
          !hasRestoredRef.current &&
          initialBrandId != null &&
          list.some(b => String(b.id) === String(initialBrandId));

        if (canRestore) {
          setSelectedBrandId(String(initialBrandId));
          hasRestoredRef.current = true;
        }
      } catch {
        if (!cancelled) {
          setBrands([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchBrands();

    return () => {
      cancelled = true;
    };
  }, [categoryId, initialBrandId]);

  const handleBrandChange = (e) => {
    setSelectedBrandId(e.target.value);
  };

  return {
    brands,
    loading,
    selectedBrandId,
    setSelectedBrandId,
    handleBrandChange,
  };
}

export default useBrands;
