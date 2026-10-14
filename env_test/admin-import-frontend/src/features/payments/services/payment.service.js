// src/features/payments/services/payment.service.js
import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const paymentService = {
  /**
   * Get all payments (paginated and filtered)
   */
  getAll: ({ page = 1, size = 10, criteria_type = 0, criteria_value = '' } = {}) =>
    axiosClient.post(API_ENDPOINTS.PAYMENTS.GET_ALL, {
      page,
      size,
      criteria_type,
      criteria_value,
    }),

  /**
   * Get payment by ID
   */
  getById: (id) =>
    axiosClient.get(`${API_ENDPOINTS.PAYMENTS.BY_ID}${id}`),

  /**
   * Get all payments for a user
   */
  getByUser: (userId) =>
    axiosClient.post(`${API_ENDPOINTS.PAYMENTS.USER_ALL}?userId=${userId}`),

  /**
   * Get payment detail for a specific user and payment
   */
  getUserDetail: (userId, paymentId) =>
    axiosClient.post(`${API_ENDPOINTS.PAYMENTS.USER_DETAIL}?userId=${userId}&paymentId=${paymentId}`),

  /**
   * Get payment history for a user (with optional filters)
   */
  getUserHistory: (userId, { status, startDate, endDate, page = 0, size = 10 } = {}) => {
    const params = new URLSearchParams({ userId, page, size });
    if (status) params.append('status', status);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    return axiosClient.post(`${API_ENDPOINTS.PAYMENTS.USER_HISTORY}?${params.toString()}`);
  },

  /**
   * Get payment by transaction ID
   */
  getByTransaction: (transactionId) =>
    axiosClient.post(`${API_ENDPOINTS.PAYMENTS.BY_TRANSACTION}?transactionId=${encodeURIComponent(transactionId)}`),

  /**
   * Get payment by order ID
   */
  getByOrder: (orderId) =>
    axiosClient.post(`${API_ENDPOINTS.PAYMENTS.BY_ORDER}?orderId=${orderId}`),
};

export const paymentApi = paymentService;

export default paymentService;



// Extended mock data for the Payment Details page

export const paymentDetails = [
  {
    id: 'PAY-8D7F9A1K', orderId: 'ORD-250526-001',
    customer: { name: 'Sarah Johnson', email: 'sarah.j@example.com', avatar: 'https://placehold.co/36x36/6366f1/fff?text=SJ' },
    amount: 129.99,
    method: 'visa', methodDetail: '•••• 4242',
    status: 'paid',
    datetime: '2024-05-26T10:24:00',
  },
  {
    id: 'PAY-8D7F9A1L', orderId: 'ORD-250526-002',
    customer: { name: 'Michael Brown', email: 'michael.b@example.com', avatar: 'https://placehold.co/36x36/10b981/fff?text=MB' },
    amount: 59.49,
    method: 'mastercard', methodDetail: '•••• 5555',
    status: 'paid',
    datetime: '2024-05-26T09:15:00',
  },
  {
    id: 'PAY-8D7F9A1M', orderId: 'ORD-250525-019',
    customer: { name: 'Emily Davis', email: 'emily.d@example.com', avatar: 'https://placehold.co/36x36/ec4899/fff?text=ED' },
    amount: 79.00,
    method: 'paypal', methodDetail: 'PayPal',
    status: 'pending',
    datetime: '2024-05-25T08:47:00',
  },
  {
    id: 'PAY-8D7F9A1N', orderId: 'ORD-250525-018',
    customer: { name: 'David Wilson', email: 'david.w@example.com', avatar: 'https://placehold.co/36x36/f59e0b/fff?text=DW' },
    amount: 199.00,
    method: 'visa', methodDetail: '•••• 1111',
    status: 'paid',
    datetime: '2024-05-25T11:32:00',
  },
  {
    id: 'PAY-8D7F9A1P', orderId: 'ORD-250525-017',
    customer: { name: 'Jessica Taylor', email: 'jessica.t@example.com', avatar: 'https://placehold.co/36x36/8b5cf6/fff?text=JT' },
    amount: 49.99,
    method: 'upi', methodDetail: 'UPI',
    status: 'failed',
    datetime: '2024-05-25T10:08:00',
  },
  {
    id: 'PAY-8D7F9A1Q', orderId: 'ORD-250525-016',
    customer: { name: 'Daniel Anderson', email: 'daniel.a@example.com', avatar: 'https://placehold.co/36x36/22d3ee/fff?text=DA' },
    amount: 85.90,
    method: 'mastercard', methodDetail: '•••• 8888',
    status: 'paid',
    datetime: '2024-05-25T07:54:00',
  },
  {
    id: 'PAY-8D7F9A1R', orderId: 'ORD-250525-015',
    customer: { name: 'Laura Martinez', email: 'laura.m@example.com', avatar: 'https://placehold.co/36x36/f43f5e/fff?text=LM' },
    amount: 120.00,
    method: 'paypal', methodDetail: 'PayPal',
    status: 'pending',
    datetime: '2024-05-25T08:31:00',
  },
  {
    id: 'PAY-8D7F9A1S', orderId: 'ORD-250524-014',
    customer: { name: 'Kevin Thomas', email: 'kevin.t@example.com', avatar: 'https://placehold.co/36x36/f97316/fff?text=KT' },
    amount: 344.50,
    method: 'visa', methodDetail: '•••• 3399',
    status: 'paid',
    datetime: '2024-05-24T14:10:00',
  },
  {
    id: 'PAY-8D7F9A1T', orderId: 'ORD-250524-013',
    customer: { name: 'Natalie Scott', email: 'natalie.s@example.com', avatar: 'https://placehold.co/36x36/84cc16/fff?text=NS' },
    amount: 210.75,
    method: 'applepay', methodDetail: 'Apple Pay',
    status: 'paid',
    datetime: '2024-05-24T17:25:00',
  },
  {
    id: 'PAY-8D7F9A1U', orderId: 'ORD-250524-012',
    customer: { name: 'Ryan Clark', email: 'ryan.c@example.com', avatar: 'https://placehold.co/36x36/a78bfa/fff?text=RC' },
    amount: 67.00,
    method: 'stripe', methodDetail: 'Stripe',
    status: 'failed',
    datetime: '2024-05-24T06:40:00',
  },
  {
    id: 'PAY-8D7F9A1V', orderId: 'ORD-250523-011',
    customer: { name: 'Amanda Lewis', email: 'amanda.l@example.com', avatar: 'https://placehold.co/36x36/0ea5e9/fff?text=AL' },
    amount: 455.00,
    method: 'mastercard', methodDetail: '•••• 2020',
    status: 'paid',
    datetime: '2024-05-23T12:00:00',
  },
  {
    id: 'PAY-8D7F9A1W', orderId: 'ORD-250523-010',
    customer: { name: 'Brian Hall', email: 'brian.h@example.com', avatar: 'https://placehold.co/36x36/e879f9/fff?text=BH' },
    amount: 33.99,
    method: 'upi', methodDetail: 'UPI',
    status: 'paid',
    datetime: '2024-05-23T09:22:00',
  },
  {
    id: 'PAY-8D7F9A1X', orderId: 'ORD-250522-009',
    customer: { name: 'Christina Young', email: 'chris.y@example.com', avatar: 'https://placehold.co/36x36/fb7185/fff?text=CY' },
    amount: 189.99,
    method: 'paypal', methodDetail: 'PayPal',
    status: 'pending',
    datetime: '2024-05-22T15:35:00',
  },
  {
    id: 'PAY-8D7F9A1Y', orderId: 'ORD-250522-008',
    customer: { name: 'Frank Harris', email: 'frank.h@example.com', avatar: 'https://placehold.co/36x36/34d399/fff?text=FH' },
    amount: 92.50,
    method: 'visa', methodDetail: '•••• 7777',
    status: 'paid',
    datetime: '2024-05-22T11:48:00',
  },
  {
    id: 'PAY-8D7F9A1Z', orderId: 'ORD-250521-007',
    customer: { name: 'Grace Allen', email: 'grace.a@example.com', avatar: 'https://placehold.co/36x36/fbbf24/fff?text=GA' },
    amount: 729.00,
    method: 'applepay', methodDetail: 'Apple Pay',
    status: 'paid',
    datetime: '2024-05-21T16:05:00',
  },
  {
    id: 'PAY-8D7F9A2A', orderId: 'ORD-250521-006',
    customer: { name: 'Henry Walker', email: 'henry.w@example.com', avatar: 'https://placehold.co/36x36/6366f1/fff?text=HW' },
    amount: 155.25,
    method: 'mastercard', methodDetail: '•••• 4411',
    status: 'failed',
    datetime: '2024-05-21T08:17:00',
  },
  {
    id: 'PAY-8D7F9A2B', orderId: 'ORD-250520-005',
    customer: { name: 'Isabella King', email: 'isabella.k@example.com', avatar: 'https://placehold.co/36x36/10b981/fff?text=IK' },
    amount: 310.00,
    method: 'stripe', methodDetail: 'Stripe',
    status: 'paid',
    datetime: '2024-05-20T14:55:00',
  },
  {
    id: 'PAY-8D7F9A2C', orderId: 'ORD-250520-004',
    customer: { name: 'Jack Wright', email: 'jack.w@example.com', avatar: 'https://placehold.co/36x36/f59e0b/fff?text=JW' },
    amount: 77.40,
    method: 'upi', methodDetail: 'UPI',
    status: 'paid',
    datetime: '2024-05-20T10:20:00',
  },
];

// ── Summary stats ──────────────────────────────────────────────────────────────
export const paymentStats = {
  total: { count: 18, growth: 18.6 },
  successful: { count: 14, growth: 16.4 },
  pending: { count: 3, growth: 8.7 },
  failed: { count: 1, growth: -5.3 },
  totalAmount: { value: 45231.89, growth: 12.2 } // Matches stats.totalAmount.value
};
