import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const returnService = {
  getAll: (data) => {
    return axiosClient.post(API_ENDPOINTS.RETURN.GET_ALL, {
      page: data.page,
      size: data.size,
      criteria_type: data.criteria_type,
      criteria_value: data.criteria_value,
    });
  },
  getById: (id) => {
    return axiosClient.get(API_ENDPOINTS.RETURN.GET_BY_ID.replace('{returnId}', id));
  },
  getHistory: (id) => {
    return axiosClient.get(API_ENDPOINTS.RETURN.GET_HISTORY.replace('{returnId}', id));
  },
  approve: (id, data) => {
    return axiosClient.post(API_ENDPOINTS.RETURN.APPROVE.replace('{returnId}', id), data);
  },
  reject: (id, data) => {
    return axiosClient.post(API_ENDPOINTS.RETURN.REJECT.replace('{returnId}', id), data);
  },
  receive: (id, data) => {
    return axiosClient.post(API_ENDPOINTS.RETURN.RECEIVE.replace('{returnId}', id), data);
  },
  startInspection: (id, data) => {
    return axiosClient.post(API_ENDPOINTS.RETURN.START_INSPECTION.replace('{returnId}', id), data);
  },
  completeInspection: (id, data) => {
    return axiosClient.post(API_ENDPOINTS.RETURN.COMPLETE_INSPECTION.replace('{returnId}', id), data);
  },
  getSummary: () => {
    return axiosClient.get(API_ENDPOINTS.RETURN.GET_SUMMARY);
  },
};

export default returnService;
