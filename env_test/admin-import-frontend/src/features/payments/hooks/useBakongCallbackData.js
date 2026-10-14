// src/features/payments/hooks/useBakongCallbackData.js
import { useState, useEffect, useCallback } from 'react';
import { paymentService } from '../services/payment.service';

/**
 * Fetches real payment data for the BakongCallback page.
 *
 * NOTE: The backend endpoint GET /api/v1/payments/id/{id} does NOT exist
 * (returns 404). We go directly to POST /api/v1/payments/get/all and find
 * the matching record by ID — the same working strategy used in PaymentDetailPage.
 *
 * @param {string|number} id  — the payment ID from the URL
 */
export function useBakongCallbackData(id) {
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  const fetchData = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);

    try {
      // ── Use the working list endpoint, filter client-side by id ─────────────
      const res  = await paymentService.getAll({ page: 1, size: 200 });
      const list = res?.data?.payload ?? [];

      const found = list.find(
        (p) => String(p.id) === String(id) || p.code === String(id)
      );

      if (found) {
        console.log("=== REAL PAYMENT DATA (from get/all) ===", found);
        setPayment(found);
      } else {
        setError('Payment record not found.');
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
        err?.message ||
        'Failed to load payment details.'
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { payment, loading, error, refetch: fetchData };
}

export default useBakongCallbackData;
