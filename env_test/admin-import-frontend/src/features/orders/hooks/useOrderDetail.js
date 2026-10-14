import { useState, useEffect, useCallback } from 'react';
import orderService from '../services/order.service';
import Swal from 'sweetalert2';

export function useOrderDetail(orderId, userId = 1) {
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchOrder = useCallback(async () => {
    if (!orderId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await orderService.getUserDetail(userId, orderId);
      // Depending on API response, unwrap the actual order object
      setOrder(data.data || data);
    } catch (err) {
      // Fallback: if user-specific fetch fails (due to wrong/missing userId from router state),
      // try to fetch it generically by ID or Number
      try {
        const fallbackData = await orderService.getById(orderId);
        setOrder(fallbackData.data || fallbackData);
        setError(null);
      } catch (fallbackErr) {
        try {
          const numberFallback = await orderService.getByNumber(orderId);
          setOrder(numberFallback.data || numberFallback);
          setError(null);
        } catch (numErr) {
          try {
            // Ultimate fallback: Fetch all and filter (since getById might be 404 on backend)
            const allOrdersData = await orderService.getAll({ page: 1, size: 200 });
            const allOrders = Array.isArray(allOrdersData) ? allOrdersData : (allOrdersData?.payload || allOrdersData?.data || []);
            const found = allOrders.find(o => String(o.id) === String(orderId) || String(o.order_number) === String(orderId) || String(o.codeOrder) === String(orderId));
            
            if (found) {
              setOrder(found);
              setError(null);
            } else {
              throw new Error('Order not found in list');
            }
          } catch (listErr) {
            const allErrors = `User fetch: ${err?.response?.data?.message || err?.message}. ID fetch: ${fallbackErr?.response?.data?.message || fallbackErr?.message}. Num fetch: ${numErr?.response?.data?.message || numErr?.message}. List fetch: ${listErr.message}`;
            console.error('All fetch attempts failed:', allErrors);
            setError(allErrors);
            Swal.fire({
              toast: true,
              position: 'top-end',
              showConfirmButton: false,
              timer: 3000,
              icon: 'error',
              title: 'Failed to load order details',
            });
          }
        }
      }
    } finally {
      setLoading(false);
    }
  }, [orderId, userId]);

  useEffect(() => {
    fetchOrder();
  }, [fetchOrder]);

  const updateStatus = async (status) => {
    try {
      await orderService.updateStatus(orderId, status);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Order status updated successfully',
      });
      // Refresh order data
      fetchOrder();
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

  const cancelOrder = async (userId, reason) => {
    try {
      await orderService.cancel(orderId, userId, reason);
      Swal.fire({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        icon: 'success',
        title: 'Order cancelled successfully',
      });
      // Refresh order data
      fetchOrder();
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
    order,
    loading,
    error,
    fetchOrder,
    updateStatus,
    cancelOrder,
  };
}
