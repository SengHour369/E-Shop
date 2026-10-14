// src/features/products/services/product.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const productService = {
  /**
   * Get all products (paginated, with optional filtering)
   *
   * POST /api/v1/products/get/all
   * Body: { page, size, criteria_type, criteria_value }
   */
  getAll: ({ page = 1, size = 10, criteria_type = 5, criteria_value = '' } = {}) =>
    axiosClient.post(API_ENDPOINTS.PRODUCTS.GET_ALL, {
      page,
      size,
      criteria_type,
      criteria_value,
    }),

  /**
   * Create a new product with images
   */
  create: ({ name, sub_category_id, description, is_active = true, skus = [], files = [] }) => {
    const params = new URLSearchParams();
    params.append('name', name);
    params.append('sub_category_id', sub_category_id);
    if (description) params.append('description', description);
    params.append('is_active', is_active);
    if (skus.length > 0) params.append('skus', JSON.stringify(skus));

    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));

    return axiosClient.post(
      `${API_ENDPOINTS.PRODUCTS.CREATE}?${params.toString()}`,
      formData,
      { headers: { 'Content-Type': undefined } }
    );
  },

  /**
   * Update an existing product
   */
  update: (id, { name, sub_category_id, description, is_active = true, skus = [], files = [] }) => {
    const params = new URLSearchParams();
    params.append('id', id);
    params.append('name', name);
    params.append('sub_category_id', sub_category_id);
    if (description) params.append('description', description);
    params.append('is_active', is_active);
    if (skus.length > 0) params.append('skus', JSON.stringify(skus));

    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));

    return axiosClient.put(
      `${API_ENDPOINTS.PRODUCTS.UPDATE}?${params.toString()}`,
      formData,
      { headers: { 'Content-Type': undefined } }
    );
  },

  /**
   * Delete a product by ID
   */
  delete: (id) =>
    axiosClient.post(`${API_ENDPOINTS.PRODUCTS.DELETE}?id=${id}`),

  /**
   * Toggle product active status
   */
  updateStatus: (id, isActive) =>
    axiosClient.patch(`${API_ENDPOINTS.PRODUCTS.UPDATE_STATUS}?id=${id}&isActive=${isActive}`),
};

// Named re-export for code that uses the old productApi shape
export const productApi = productService;

export default productService;
