// src/features/orders/services/order.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

/**
 * Helper to handle API calls with standard try/catch and unwrapping.
 */
const handleRequest = async (requestPromise) => {
  try {
    const response = await requestPromise;
    // Unwrap the response. Depending on your backend, this might need to be response.data.data
    return response.data;
  } catch (error) {
    throw error.response?.data || error;
  }
};

const orderService = {
  /**
   * Get all orders with full filters
   * 
   * @param {Object} params - The filter parameters (search, orderStatus, paymentStatus, paymentMethod, date, page, size)
   * @returns {Promise<any>} The paginated orders
   */
  getAll: async (params = {}) => {
    // Pass as query params according to the new API contract
    return handleRequest(axiosClient.get(API_ENDPOINTS.ORDERS.GET_ALL, { params }));
  },

  /**
   * Get all orders using alternative endpoint
   * 
   * @param {Object} params - The filter parameters
   * @returns {Promise<any>} The paginated orders
   */
  getAllAlt: async (params = {}) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.GET_ALL_ALT, params));
  },

  /**
   * Get order summary statistics
   * 
   * @returns {Promise<any>} The order summary data
   */
  getSummary: async () => {
    return handleRequest(axiosClient.get(API_ENDPOINTS.ORDERS.SUMMARY));
  },

  /**
   * Get items for a specific order
   * // UNCONFIRMED: Confirm if ITEMS endpoint is actually POST taking { orderId } in body
   * 
   * @param {string|number} orderId - The order ID
   * @returns {Promise<any>} The order items
   */
  getItems: async (orderId) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.ITEMS, { orderId }));
  },

  /**
   * Get order by ID
   * // UNCONFIRMED: Confirm if BY_ID truly wants a request body instead of a path param
   * 
   * @param {string|number} id - The order ID
   * @returns {Promise<any>} The order details
   */
  getById: async (id) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.BY_ID, { id }));
  },

  /**
   * Get order by order number
   * 
   * @param {string} orderNumber - The order number
   * @returns {Promise<any>} The order details
   */
  getByNumber: async (orderNumber) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.BY_NUMBER, { orderNumber }));
  },

  /**
   * Get all orders for a user (paginated)
   * 
   * @param {string|number} userId - The user ID
   * @param {Object} params - Pagination params
   * @param {number} [params.page] - Page number
   * @param {number} [params.size] - Page size
   * @returns {Promise<any>} The paginated orders for the user
   */
  getByUser: async (userId, { page, size } = {}) => {
    const payload = { userId };
    if (page !== undefined) payload.page = page;
    if (size !== undefined) payload.size = size;
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_BY_ID, payload));
  },

  /**
   * Get order detail for a specific user
   * 
   * @param {string|number} userId - The user ID
   * @param {string|number} orderId - The order ID
   * @returns {Promise<any>} The order detail
   */
  getUserDetail: async (userId, orderId) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_DETAIL, null, { params: { userId, orderId } }));
  },

  /**
   * Get order history for a user (with optional filters)
   * 
   * @param {string|number} userId - The user ID
   * @param {Object} filters - Filter parameters
   * @returns {Promise<any>} The user's order history
   */
  getUserHistory: async (userId, filters = {}) => {
    const payload = { userId };

    // Only include filters that are set
    const allowedFilters = ['status', 'startDate', 'endDate', 'page', 'size'];
    allowedFilters.forEach(key => {
      if (filters[key] !== undefined && filters[key] !== null && filters[key] !== '') {
        payload[key] = filters[key];
      }
    });

    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_HISTORY, payload));
  },
  /**
   * Get canceled orders
   * 
   * @param {Object} params - Query parameters
   * @returns {Promise<any>} The canceled orders list
   */
  getUserCancel: async (params = {}) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_CANCEL, params));
  },

  /**
   * Update order status (Admin)
   * 
   * @param {string|number} id - The order ID
   * @param {string} status - The new status
   * @returns {Promise<any>} The updated order
   */
  updateStatus: async (id, status) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.UPDATE_STATUS, null, { params: { id, status } }));
  },

  /**
   * Cancel an order
   * 
   * @param {string|number} id - The order ID
   * @param {string|number} userId - The user ID
   * @param {string} [reason] - Optional cancellation reason
   * @returns {Promise<any>} The cancellation response
   */
  cancel: async (id, userId, reason) => {
    const params = { id, userId };
    if (reason !== undefined && reason !== null && reason !== '') {
      params.reason = reason;
    }
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_CANCEL, null, { params }));
  },

  /**
   * Create order from cart (Cash on Delivery)
   * 
   * @param {string|number} userId - The user ID
   * @param {Object} orderRequest - The order request details
   * @returns {Promise<any>} The created order
   */
  createFromCart: async (userId, orderRequest) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_FROM_CART, { userId, ...orderRequest }));
  },

  /**
   * Create order from cart with Bakong payment
   * 
   * @param {string|number} userId - The user ID
   * @param {Object} orderRequest - The order request details
   * @returns {Promise<any>} The created order
   */
  createFromCartBakong: async (userId, orderRequest) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.USER_FROM_CART_BAKONG, { userId, ...orderRequest }));
  },

  /**
   * Initiate Bakong payment for an existing order
   * 
   * @param {string|number} orderId - The order ID
   * @returns {Promise<any>} The Bakong payment initiation response
   */
  initiateBakong: async (orderId) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.BAKONG_INITIATE, { orderId }));
  },

  /**
   * Verify a Bakong payment transaction
   * 
   * @param {string|number} orderId - The order ID
   * @param {string} transactionId - The Bakong transaction ID
   * @returns {Promise<any>} The verification response
   */
  verifyBakong: async (orderId, transactionId) => {
    return handleRequest(axiosClient.post(API_ENDPOINTS.ORDERS.BAKONG_VERIFY, { orderId, transactionId }));
  },
};

export default orderService;
