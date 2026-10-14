export const refundService = {};
export default refundService;
   * @param { Object } params - Query parameters like page, size, status, etc.
   */
getList: async (params = {}) => {
  return handleRequest(axiosClient.post(API_ENDPOINTS.REFUND.LIST, params));
}
};

export default refundService;
