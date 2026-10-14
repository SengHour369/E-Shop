// src/features/payments/hooks/useBakongCallback.js
import { useState, useCallback } from 'react';
import Swal from 'sweetalert2';
import { processBakongCallback } from '../services/callback.service';

/**
 * Hook for Bakong callback processing.
 *
 * Supports MULTIPLE submissions — each successful call is appended to
 * `callbackHistory` so the page can display a running log.
 * The form is NEVER permanently locked; after each submission the caller
 * can immediately submit again with different parameters.
 *
 * Returns:
 *  - isProcessing    : boolean  — true while an API call is in-flight
 *  - callbackHistory : Array    — list of every processed callback entry
 *                                 { id, orderNumber, transactionId, status,
 *                                   adminNote, processedAt, response }
 *  - submitCallback  : async fn — resolves true on success, false on failure
 */
export function useBakongCallback() {
  const [isProcessing, setIsProcessing] = useState(false);

  // History of all successfully processed callbacks (most-recent first)
  const [callbackHistory, setCallbackHistory] = useState([]);

  const submitCallback = useCallback(async ({ orderNumber, transactionId, status, adminNote }) => {
    // ── 1. Validation ───────────────────────────────────────────────────────────
    const missing = [];
    if (!orderNumber?.trim())   missing.push('Order Number');
    if (!transactionId?.trim()) missing.push('Transaction ID');
    if (!status?.trim())        missing.push('Status');

    if (missing.length > 0) {
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'warning',
        title: `Required: ${missing.join(', ')}`,
        showConfirmButton: false,
        timer: 3500,
        timerProgressBar: true,
      });
      return false;
    }

    // ── 2. In-flight guard (prevents double-click) ───────────────────────────────
    if (isProcessing) return false;

    setIsProcessing(true);
    try {
      // ── 3. API — query params only, null body ───────────────────────────────────
      const res = await processBakongCallback({ orderNumber, transactionId, status });
      const response = res?.data?.payload ?? res?.data ?? {};
      console.log("=== BAKONG CALLBACK API RESPONSE ===", {
        requestParams: { orderNumber, transactionId, status },
        fullResponse: res,
        extractedData: response
      });

      // ── 4. Append to history (newest first) ─────────────────────────────────────
      const entry = {
        id:            Date.now(),          // local unique key
        orderNumber,
        transactionId,
        status,
        adminNote:     adminNote?.trim() || null,
        processedAt:   new Date().toISOString(),
        response,
      };
      setCallbackHistory((prev) => [entry, ...prev]);

      // ── 5. Success toast ─────────────────────────────────────────────────────────
      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'success',
        title: `Callback processed — ${status}`,
        showConfirmButton: false,
        timer: 3500,
        timerProgressBar: true,
      });

      return true;

    } catch (err) {
      // ── 6. Error toast — allow retry ────────────────────────────────────────────
      const message =
        err?.response?.data?.message ||
        err?.data?.message ||
        err?.message ||
        'Failed to process callback. Please try again.';

      Swal.fire({
        toast: true,
        position: 'top-end',
        icon: 'error',
        title: message,
        showConfirmButton: false,
        timer: 4000,
        timerProgressBar: true,
      });

      return false;
    } finally {
      setIsProcessing(false);
    }
  }, [isProcessing]);

  return {
    isProcessing,
    callbackHistory,
    submitCallback,
  };
}

export default useBakongCallback;
