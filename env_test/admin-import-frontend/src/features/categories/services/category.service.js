// src/features/categories/services/category.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const categoryService = {
  /**
   * Get all categories (paginated)
   * @param {number} page - 0-based page index
   * @param {number} size - items per page
   */
  getAll: (page = 0, size = 10, sort = 'id,desc') =>
    axiosClient.post(`${API_ENDPOINTS.CATEGORIES.GET_ALL}?page=${page}&size=${size}&sort=${sort}`),

  /**
   * Get category by ID
   * @param {number} id
   */
  getById: (id) =>
    axiosClient.post(`${API_ENDPOINTS.CATEGORIES.BY_ID}?id=${id}`),

  /**
   * Get category by name
   * @param {string} name
   */
  getByName: (name) =>
    axiosClient.post(`${API_ENDPOINTS.CATEGORIES.BY_NAME}?name=${encodeURIComponent(name)}`),

  /**
   * Get category with its subcategories
   * @param {number} id
   */
  getWithSubCategories: (id) =>
    axiosClient.post(`${API_ENDPOINTS.CATEGORIES.WITH_SUBCATEGORIES}?id=${id}`),

  /**
   * Create a new category
   * @param {{ name: string, description?: string, icon_id?: number }} data
   */
  create: (data) =>
    axiosClient.post(API_ENDPOINTS.CATEGORIES.CREATE, data),

  /**
   * Update category info (name, description, icon)
   * @param {number} id
   * @param {{ name: string, description?: string, icon_id?: number }} data
   */
  update: (id, data) =>
    axiosClient.put(`${API_ENDPOINTS.CATEGORIES.UPDATE}?id=${id}`, data),

  /**
   * Upload / update category icon image
   * @param {number} id
   * @param {File} file
   */
  uploadIcon: (id, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.put(
      `${API_ENDPOINTS.CATEGORIES.UPDATE_ICON}?id=${id}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  /**
   * Delete a category by ID
   * @param {number} id
   */
  delete: (id) =>
    axiosClient.delete(`${API_ENDPOINTS.CATEGORIES.DELETE}?id=${id}`),
};

// Named re-export for code that uses the old categoryApi shape
export const categoryApi = categoryService;

export default categoryService;
