// src/features/auth/services/auth.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const login = async (data) => {
  return axiosClient.post(API_ENDPOINTS.AUTH.LOGIN, data);
};

export const logout = async () => {
  try {
    await axiosClient.post(API_ENDPOINTS.AUTH.LOGOUT);
  } catch {
    console.log('Backend logout failed, forcing local logout');
  } finally {
    localStorage.removeItem('accessToken');
  }
};

export const authService = { login, logout };

export default authService;
