// src/features/payments/services/callback.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

/**
 * Process a Bakong payment callback.
 *
 * The backend accepts ONLY query parameters — no request body.
 *
 * @param {Object} params
 * @param {string} params.orderNumber   - e.g. "ORD-20260612-0021"
 * @param {string} params.transactionId - e.g. "FT25061210243312345678"
 * @param {string} params.status        - "SUCCESS" | "FAILED" | "PENDING"
 * @returns {Promise<AxiosResponse>}
 */
export const processBakongCallback = ({ orderNumber, transactionId, status }) =>
  axiosClient.post(
    API_ENDPOINTS.ORDERS.BAKONG_CALLBACK,
    null,                               // no request body
    { params: { orderNumber, transactionId, status } }
  );

export const callbackService = {
  processBakongCallback,
};

export default callbackService;
