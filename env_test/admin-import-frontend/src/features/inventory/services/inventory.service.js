// src/features/inventory/services/inventory.service.js
import axiosClient from '@/api/axiosClient';
import { API_ENDPOINTS } from '@/api/endpoints';

const EP = API_ENDPOINTS.INVENTORY;

export const inventoryService = {
    /**
     * Create a new inventory record
     * POST /api/v1/inventory?skuId={skuId}
     * @param {number|string} skuId
     * @param {object} data - { quantity, lowStockThreshold, warehouse_location }
     */
    create: (skuId, data) =>
        axiosClient.post(`${EP.CREATE}?skuId=${skuId}`, data),

    /**
     * Get all inventory records (paginated)
     * POST /api/v1/inventory/all/
     * @param {number} page  - 0-based page index
     * @param {number} size  - page size
     */
    getAll: (page = 0, size = 10) =>
        axiosClient.post(EP.GET_ALL, { page, size }),

    /**
     * Get inventory by record ID
     * GET /api/v1/inventory/id/{id}
     * @param {string|number} id
     */
    getById: (id) =>
        axiosClient.get(`${EP.BY_ID}${id}`),

    /**
     * Get inventory by SKU
     * GET /api/v1/inventory/sku/{sku}
     * @param {string} sku
     */
    getBySku: (sku) =>
        axiosClient.get(`${EP.BY_SKU}${sku}`),

    /**
     * Get all inventory records for a product
     * GET /api/v1/inventory/product/{productId}
     * @param {string|number} productId
     */
    getByProduct: (productId) =>
        axiosClient.get(`${EP.BY_PRODUCT}${productId}`),

    /**
     * Get all low-stock inventory records
     * GET /api/v1/inventory/low-stock
     */
    getLowStock: () =>
        axiosClient.get(EP.LOW_STOCK),

    /**
     * Set exact stock quantity AND/OR warehouse location (adjust)
     * POST /api/v1/inventory/exact?id={id}
     * @param {number} id                 - Inventory record ID (query param)
     * @param {object} body               - { quantity, warehouse_location? }
     */
    adjust: (id, body) =>
        axiosClient.post(`${EP.ADJUST}?id=${id}`, body),

    /**
     * Restock inventory — add quantity to existing stock
     * PATCH /api/v1/inventory/restock?id={id}
     * @param {number} id       - Inventory record ID (query param)
     * @param {object} body     - { quantity }
     */
    restock: (id, body) =>
        axiosClient.patch(`${EP.RESTOCK}?id=${id}`, body),

    /**
     * Delete an inventory record
     * DELETE /api/v1/inventory/delete
     * @param {string|number} id
     */
    delete: (id) =>
        axiosClient.delete(`${EP.DELETE}?id=${id}`),
};

export default inventoryService;
