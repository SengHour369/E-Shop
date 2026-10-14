// src/features/products/services/inventory.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const inventoryService = {
  /**
   * Get all inventory records (paginated, Admin only)
   */
  getAll: (page = 0, size = 10) =>
    axiosClient.post(`${API_ENDPOINTS.INVENTORY.GET_ALL}?page=${page}&size=${size}`),

  /** Get inventory record by ID */
  getById: (id) =>
    axiosClient.post(`${API_ENDPOINTS.INVENTORY.BY_ID}?id=${id}`),

  /** Get inventory by product SKU ID */
  getBySkuId: (skuId) =>
    axiosClient.post(`${API_ENDPOINTS.INVENTORY.BY_SKU}?skuId=${skuId}`),

  /** Get all SKU inventories for a product (paginated) */
  getByProductId: (productId, page = 0, size = 10) =>
    axiosClient.post(
      `${API_ENDPOINTS.INVENTORY.BY_PRODUCT}?productId=${productId}&page=${page}&size=${size}`
    ),

  /** Get low stock items */
  getLowStock: (threshold = 10, page = 0, size = 10) =>
    axiosClient.post(
      `${API_ENDPOINTS.INVENTORY.LOW_STOCK}?threshold=${threshold}&page=${page}&size=${size}`
    ),

  /** Create inventory record for a product SKU */
  create: (data) =>
    axiosClient.post(API_ENDPOINTS.INVENTORY.CREATE, data),

  /** Set exact quantity / warehouse location (adjust) */
  adjust: (id, data) =>
    axiosClient.post(`${API_ENDPOINTS.INVENTORY.ADJUST}?id=${id}`, data),

  /** Add quantity to existing inventory (restock) */
  restock: (id, data) =>
    axiosClient.patch(`${API_ENDPOINTS.INVENTORY.RESTOCK}?id=${id}`, data),

  /** Delete an inventory record by ID */
  delete: (id) =>
    axiosClient.post(`${API_ENDPOINTS.INVENTORY.DELETE}?id=${id}`),
};

export const inventoryApi = inventoryService;

export default inventoryService;
