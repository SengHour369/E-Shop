import { useState, useEffect } from 'react';
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';
import { salesChartData as fallbackSales, ordersChartData as fallbackOrders } from '../services/dashboard.service';

export function useDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let isMounted = true;

    const fetchDashboardData = async () => {
      try {
        setLoading(true);

        // Fetch in parallel
        const [ordersSummaryRes, recentOrdersRes, inventorySummaryRes] = await Promise.allSettled([
          axiosClient.get(API_ENDPOINTS.ORDERS.SUMMARY),
          // Use POST with body like useOrders.js does
          axiosClient.post(API_ENDPOINTS.ORDERS.GET_ALL_ALT, {
            page: 1,
            size: 6,
            criteria_type: 'ALL',
            criteria_value: '',
          }),
          axiosClient.get(API_ENDPOINTS.INVENTORY.SUMMARY),
        ]);

        if (!isMounted) return;

        // ── Parse Orders Summary ─────────────────────────────────────────────
        let ordersSummary = null;
        if (ordersSummaryRes.status === 'fulfilled') {
          const raw = ordersSummaryRes.value.data;
          // unwrap: payload > data > raw
          ordersSummary = raw?.payload || raw?.data || raw;
          console.log('[Dashboard] orders summary:', ordersSummary);
        }

        // ── Parse Inventory Summary ──────────────────────────────────────────
        let inventorySummary = null;
        if (inventorySummaryRes.status === 'fulfilled') {
          const raw = inventorySummaryRes.value.data;
          inventorySummary = raw?.payload || raw?.data || raw;
          console.log('[Dashboard] inventory summary:', inventorySummary);
        }

        // ── Parse Recent Orders ──────────────────────────────────────────────
        let recentOrders = [];
        if (recentOrdersRes.status === 'fulfilled') {
          const raw = recentOrdersRes.value.data;
          // Based on useOrders.js: data.payload is array
          if (raw?.payload && Array.isArray(raw.payload)) {
            recentOrders = raw.payload;
          } else if (Array.isArray(raw)) {
            recentOrders = raw;
          } else if (raw?.data && Array.isArray(raw.data)) {
            recentOrders = raw.data;
          } else if (raw?.content && Array.isArray(raw.content)) {
            recentOrders = raw.content;
          }
          console.log('[Dashboard] recent orders:', recentOrders);
        }

        // ── Map stats from real API ──────────────────────────────────────────
        // The summary can come in multiple field name formats depending on backend version
        const stats = {
          totalSales:
            ordersSummary?.total_revenue ?? ordersSummary?.totalRevenue ??
            ordersSummary?.total_amount ?? ordersSummary?.totalAmount ?? 0,

          totalOrders:
            ordersSummary?.total_orders ?? ordersSummary?.totalOrders ??
            ordersSummary?.total ?? 0,

          totalRevenue:
            ordersSummary?.total_revenue ?? ordersSummary?.totalRevenue ??
            ordersSummary?.revenue ?? 0,

          totalCustomers:
            ordersSummary?.total_customers ?? ordersSummary?.totalCustomers ??
            ordersSummary?.customers ?? 0,

          // Growth percentages (may not exist in summary — show 0 if absent)
          salesGrowth:
            ordersSummary?.revenue_growth ?? ordersSummary?.revenueGrowth ??
            ordersSummary?.sales_growth ?? ordersSummary?.salesGrowth ?? 0,

          ordersGrowth:
            ordersSummary?.orders_growth ?? ordersSummary?.ordersGrowth ?? 0,

          revenueGrowth:
            ordersSummary?.revenue_growth ?? ordersSummary?.revenueGrowth ?? 0,

          customersGrowth:
            ordersSummary?.customers_growth ?? ordersSummary?.customersGrowth ?? 0,
        };

        setData({
          stats,
          charts: { sales: fallbackSales, orders: fallbackOrders },
          recentOrders,
        });
        setError(null);
      } catch (err) {
        if (!isMounted) return;
        console.error('[Dashboard] fetch error:', err);
        setError(err?.message || 'Failed to load dashboard data');
        // Still show page with zeroed stats rather than blank screen
        setData({
          stats: {
            totalSales: 0, totalOrders: 0, totalRevenue: 0, totalCustomers: 0,
            salesGrowth: 0, ordersGrowth: 0, revenueGrowth: 0, customersGrowth: 0,
          },
          charts: { sales: fallbackSales, orders: fallbackOrders },
          recentOrders: [],
        });
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchDashboardData();
    return () => { isMounted = false; };
  }, []);

  return { data, loading, error };
}