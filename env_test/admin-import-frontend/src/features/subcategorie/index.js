// src/features/subcategorie/index.js
// This feature has been superseded by features/brands.
// This file provides backward compatibility only.
export { BrandsPage as SubCategoryPage, AddBrandPage as AddSubCategoryPage } from '../brands';
export { useBrands as useSubCategories } from '../brands';
export { subcategoryService as subCategoryApi } from './services/subcategory.service';
