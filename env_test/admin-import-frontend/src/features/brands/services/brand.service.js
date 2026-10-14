// src/features/brands/services/brand.service.js
// Migrated from api/modules/sub-category.api.js
// NOTE: The backend API uses /subcategories/* endpoints for what the admin
// calls "brands". The naming is inherited from the API contract and must
// not be changed here.
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

const GET_ALL_URL = API_ENDPOINTS.SUB_CATEGORIES.GET_ALL;

export const brandService = {
  /**
   * Get all brands (paginated)
   */
  getAll: (page = 1, size = 10) =>
    axiosClient.post(`${GET_ALL_URL}?sort=id,desc`, {
      criteria_type: 0,
      criteria_value: '',
      page,
      size,
    }),

  /**
   * Search brands by name (fuzzy)
   */
  searchByName: (name, page = 1, size = 10) =>
    axiosClient.post(GET_ALL_URL, {
      criteria_type: 1,
      criteria_value: name,
      page,
      size,
    }),

  /**
   * Get brand by ID (no pagination, returns single object)
   */
  getById: (id) =>
    axiosClient.post(GET_ALL_URL, {
      criteria_type: 4,
      criteria_value: String(id),
    }),

  /**
   * Get brands filtered by category ID (paginated)
   * Used by useSubCategories in the products feature.
   */
  getByCategoryId: (categoryId, page = 1, size = 10) =>
    axiosClient.post(GET_ALL_URL, {
      criteria_type: 2,
      criteria_value: String(categoryId),
      page,
      size,
    }),

  /**
   * Get brand with its products (no pagination, single object)
   */
  getWithProducts: (id) =>
    axiosClient.post(GET_ALL_URL, {
      criteria_type: 5,
      criteria_value: String(id),
    }),

  create: ({ name, categoryId, description, image }) => {
    const formData = new FormData();
    if (image) formData.append('image', image);
    return axiosClient.post(
      `${API_ENDPOINTS.SUB_CATEGORIES.CREATE}?name=${encodeURIComponent(name)}&categoryId=${categoryId}&description=${encodeURIComponent(description)}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  update: (id, { name, categoryId, description, file }) => {
    const formData = new FormData();
    if (file) formData.append('file', file);
    return axiosClient.post(
      `${API_ENDPOINTS.SUB_CATEGORIES.UPDATE}?id=${id}&name=${encodeURIComponent(name)}&categoryId=${categoryId}&description=${encodeURIComponent(description)}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  delete: (id) =>
    axiosClient.post(`${API_ENDPOINTS.SUB_CATEGORIES.DELETE}?id=${id}`),
};

// Alias kept for compatibility with useSubCategories which uses the old name
export const subCategoryApi = brandService;

export default brandService;
