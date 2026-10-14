import { useState, useEffect, useCallback } from 'react';
import orderService from '../services/order.service';
import Swal from 'sweetalert2';

export function useOrders(initialParams = {}) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [summary, setSummary] = useState(null);

  const fetchOrders = useCallback(async (params = {}) => {
    setLoading(true);
    setError(null);
    try {
      if (params.orderNumber) {
        const data = await orderService.getByNumber(params.orderNumber);
        console.log("Order by number res", data);
        const orderData = data.payload || data.data || data;
        setOrders(orderData ? [orderData] : []);
        setTotalPages(1);
        setTotalElements(orderData ? 1 : 0);
      } else {
        const data = await orderService.getAllAlt(params);
        console.log("Order res", data);

        if (data && Array.isArray(data.payload)) {
          setOrders(data.payload);
          setTotalPages(data.total_pages || 1);
          setTotalElements(data.total_items || data.payload.length);
        } else if (Array.isArray(data)) {
          setOrders(data);
          setTotalPages(1);
          setTotalElements(data.length);
        } else if (data && data.data && Array.isArray(data.data)) {
          setOrders(data.data);
          setTotalPages(data.totalPages || data.meta?.totalPages || 1);
          setTotalElements(data.totalElements || data.meta?.totalElements || data.data.length);
        } else {
          setOrders([]);
        }
      }
    } catch (err) {
      console.error('Failed to fetch orders:', err);
      setError(err?.message || 'Failed to fetch orders');
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'error',
        title: 'Failed to load orders',
      });
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSummary = useCallback(async () => {
    try {
      const data = await orderService.getSummary();
      setSummary(data.payload || data.data || data);
    } catch (err) {
      console.error('Failed to fetch order summary:', err);
    }
  }, []);

  useEffect(() => {
    fetchOrders(initialParams);
    fetchSummary();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run on mount, let components trigger refetch by calling fetchOrders when filters change

  const updateOrderStatus = async (id, status, currentParams = {}) => {
    try {
      await orderService.updateStatus(id, status);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Order status updated successfully',
      });
      fetchOrders(currentParams);
      return true;
    } catch (err) {
      console.error('Failed to update order status:', err);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'error',
        title: err?.message || 'Failed to update order status',
      });
      return false;
    }
  };

  const cancelOrder = async (id, userId, reason, currentParams = {}) => {
    try {
      await orderService.cancel(id, userId, reason);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Order cancelled successfully',
      });
      fetchOrders(currentParams);
      return true;
    } catch (err) {
      console.error('Failed to cancel order:', err);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'error',
        title: err?.message || 'Failed to cancel order',
      });
      return false;
    }
  };

  return {
    orders,
    loading,
    error,
    totalPages,
    totalElements,
    summary,
    fetchOrders,
    fetchSummary,
    updateOrderStatus,
    cancelOrder,
  };
}
