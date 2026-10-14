// src/features/subcategorie/hooks/useSubCategories.js
// Backward-compatibility adapter.
// useProducts still uses the old property names (subCategories,
// selectedSubCategoryId, handleSubCategoryChange). This adapter wraps
// useBrands and maps the names so useProducts needs no changes.
import { useBrands } from '../../brands/hooks/useBrands';

export function useSubCategories(categoryId, initialSubCategoryId = null) {
  const {
    brands: subCategories,
    loading,
    selectedBrandId: selectedSubCategoryId,
    setSelectedBrandId: setSelectedSubCategoryId,
    handleBrandChange: handleSubCategoryChange,
  } = useBrands(categoryId, initialSubCategoryId);

  return {
    subCategories,
    loading,
    selectedSubCategoryId,
    setSelectedSubCategoryId,
    handleSubCategoryChange,
  };
}

export default useSubCategories;
