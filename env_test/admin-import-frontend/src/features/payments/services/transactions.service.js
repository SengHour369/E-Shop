// src/features/payments/services/transactions.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const transactionService = {
  /**
   * Get all payment transactions (paginated)
   * @param {Object} params - { page, size, criteria_type, criteria_value }
   */
  getAll: ({ page = 1, size = 10, criteria_type = 0, criteria_value = 'asc' } = {}) =>
    axiosClient.post(API_ENDPOINTS.TRANSACTION.GET_ALL, {
      page,
      size,
      criteria_type,
      criteria_value,
    }),

  /**
   * Get a single transaction by ID
   * @param {number|string} id
   */
  getById: (id) =>
    axiosClient.get(API_ENDPOINTS.TRANSACTION.GET_BY_ID.replace('{id}', id)),

  /**
   * Get transactions by order ID
   * @param {number|string} orderId
   */
  getByOrderId: (orderId) =>
    axiosClient.get(API_ENDPOINTS.TRANSACTION.GET_BY_ORDER_ID.replace('{orderId}', orderId)),

  /**
   * Get transactions by customer ID
   * @param {number|string} customerId
   */
  getByCustomerId: (customerId) =>
    axiosClient.get(API_ENDPOINTS.TRANSACTION.GET_BY_CUSTOMER_ID.replace('{customerId}', customerId)),

  /**
   * Get transaction by transaction number
   * @param {string} transactionNo
   */
  getByTransactionNo: (transactionNo) =>
    axiosClient.get(
      API_ENDPOINTS.TRANSACTION.GET_BY_TRANSACTION_NO.replace(
        '{transactionNo}',
        encodeURIComponent(transactionNo)
      )
    ),

  /**
   * Get status history of a transaction
   * @param {number|string} id
   */
  getStatusHistory: (id) =>
    axiosClient.get(API_ENDPOINTS.TRANSACTION.GET_STATUS_HISTORY.replace('{id}', id)),

  /**
   * Update transaction status
   * @param {number|string} id
   * @param {string} status - e.g. 'COMPLETED', 'REFUNDED', 'FAILED'
   */
  updateStatus: (id, status) =>
    axiosClient.patch(
      API_ENDPOINTS.TRANSACTION.UPDATE_STATUS.replace('{id}', id),
      { status }
    ),

  /**
   * Create a new transaction
   * @param {Object} payload
   */
  create: (payload) =>
    axiosClient.post(API_ENDPOINTS.TRANSACTION.CREATE, payload),
};

export const transactionApi = transactionService;
export default transactionService;
