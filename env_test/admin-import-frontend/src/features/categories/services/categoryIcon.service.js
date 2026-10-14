// src/features/categories/services/categoryIcon.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const categoryIconService = {
  /** Get all category icons */
  getAll: () =>
    axiosClient.get(API_ENDPOINTS.CATEGORY_ICONS.GET_ALL),

  /** Get category icon by ID */
  getById: (id) =>
    axiosClient.get(`${API_ENDPOINTS.CATEGORY_ICONS.BY_ID}?id=${id}`),

  /**
   * Upload a new category icon
   * @param {string} name
   * @param {File} file
   */
  upload: (name, file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post(
      `${API_ENDPOINTS.CATEGORY_ICONS.UPLOAD}?name=${encodeURIComponent(name)}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },

  /** Delete a category icon by ID */
  delete: (id) =>
    axiosClient.delete(`${API_ENDPOINTS.CATEGORY_ICONS.DELETE}?id=${id}`),
};

// Named re-export for code that uses the old categoryIconApi shape
export const categoryIconApi = categoryIconService;

export default categoryIconService;
