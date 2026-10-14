import { useState, useCallback, useEffect, useMemo } from 'react';
import inventoryService from '../services/inventory.service';

function deriveStatus(availableQty, lowStockThreshold) {
  if (availableQty <= 0) return 'Out of Stock';
  if (availableQty <= lowStockThreshold) return 'Low Stock';
  return 'In Stock';
}

function parseApiDate(isoString) {
  if (!isoString) return null;
  // Append Z only when there is no timezone info already
  const str = /[Zz]$|[+-]\d{2}:\d{2}$/.test(isoString) ? isoString : `${isoString}Z`;
  const d = new Date(str);
  return isNaN(d.getTime()) ? null : d;
}

/** Format a Date object to a readable date-time string including seconds. */
function fmtDateTime(date) {
  if (!date) return '—';
  return date.toLocaleString('en-US', {
    month:   'short',
    day:     'numeric',
    year:    'numeric',
    hour:    '2-digit',
    minute:  '2-digit',
    second:  '2-digit',
  });
}

/** Format a Date object to date-only string. */
function fmtDate(date) {
  if (!date) return '—';
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day:   'numeric',
    year:  'numeric',
  });
}

function normalizeItem(item) {
  const stockQty          = item.stock_qty          ?? 0;
  const reservedQty       = item.reserved_qty       ?? 0;
  const availableQty      = item.available_qty      ?? Math.max(0, stockQty - reservedQty);
  const lowStockThreshold = item.low_stock_threshold ?? 10;

  const name = item.product_name ?? `SKU: ${item.sku ?? item.id}`;

  // Use last_restocked_at first, fall back to updated_at for "last updated" column
  const lastRestockedDate = parseApiDate(item.last_restocked_at);
  const updatedAtDate     = parseApiDate(item.updated_at);
  const createdAtDate     = parseApiDate(item.created_at);

  return {
    id:               item.id,
    name,
    sku:              item.sku ?? '—',
    product_sku_id:   item.product_sku_id ?? '',
    warehouse:        item.warehouse_location ?? '—',
    stockQty,
    reservedQty,
    availableQty,
    lowStockThreshold,
    status:           deriveStatus(availableQty, lowStockThreshold),
    // Formatted for display — use real server timestamps
    lastUpdated:      fmtDateTime(lastRestockedDate ?? updatedAtDate),
    createdAt:        fmtDate(createdAtDate),
    // Raw ISO strings kept for tooltip / debugging
    lastRestockedAtRaw: item.last_restocked_at ?? null,
    updatedAtRaw:       item.updated_at       ?? null,
    createdAtRaw:       item.created_at       ?? null,
  };
}

export function useInventory(initialPage = 0, initialPageSize = 10) {
  const [rawData, setRawData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(initialPage);
  const [pageSize] = useState(initialPageSize);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchInventory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await inventoryService.getAll(page, pageSize);
      const contentArray = res?.content ?? [];
      console.log("Res Intven",res);
      const raw = contentArray.map(entry => entry?.data).filter(Boolean);
      setRawData(raw);
      setTotalPages(res?.totalPages ?? 1);
      setTotalElements(res?.totalElements ?? raw.length);
    } catch (err) {
      console.error('Failed to fetch inventory:', err);
      setError('Failed to load inventory. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    void fetchInventory();
  }, [fetchInventory]);

  const data = useMemo(() => rawData.map(normalizeItem), [rawData]);

  const statSummary = useMemo(() => ({
    total:      totalElements,
    inStock:    data.filter(d => d.status === 'In Stock').length,
    lowStock:   data.filter(d => d.status === 'Low Stock').length,
    outOfStock: data.filter(d => d.status === 'Out of Stock').length,
  }), [data, totalElements]);

  return {
    data,
    loading,
    error,
    page,
    setPage,
    pageSize,
    totalPages,
    totalElements,
    statSummary,
    refetch: fetchInventory,
  };
}
