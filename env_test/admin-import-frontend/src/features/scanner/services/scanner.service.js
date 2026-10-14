import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

const requestId = () => `req_${Math.random().toString(36).slice(2)}${Date.now().toString(36)}`;

export const scannerService = {
  scan: (code, format) => axiosClient.post(API_ENDPOINTS.SCANNER.SCAN, {
    code,
    ...(format ? { format } : {}),
  }, { headers: { 'X-Request-ID': requestId() } }),

  recognize: (file) => {
    const body = new FormData();
    body.append('image', file, file.name || 'scan.jpg');
    return axiosClient.post(API_ENDPOINTS.SCANNER.RECOGNIZE, body, {
      headers: { 'Content-Type': undefined, 'X-Request-ID': requestId() },
      timeout: 20000,
    });
  },
};

export default scannerService;
