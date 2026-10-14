import axiosClient from '@/api/axiosClient';
import { API_ENDPOINTS } from '@/api/endpoints';

const EP = API_ENDPOINTS.REFUND;

export const refundsService = {
  /**
   * Get all refunds (paginated and filterable)
   * POST /admin/refunds/list
   * @param {object} payload - { page, size, criteria_type, criteria_value }
   */
  getList: (payload) => axiosClient.post(EP.LIST, payload),
};

export default refundsService;
