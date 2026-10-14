import axiosClient from '../../../api/axiosClient';
import { API_ENDPOINTS } from '../../../api/endpoints';

export const dashboardAPI = {
  getOrdersSummary: async () => {
    try {
      const res = await axiosClient.get(API_ENDPOINTS.ORDERS.SUMMARY);
      return res.data;
    } catch (e) {
      console.error('Error fetching orders summary:', e);
      return null;
    }
  },
  getInventorySummary: async () => {
    try {
      const res = await axiosClient.get(API_ENDPOINTS.INVENTORY.SUMMARY);
      return res.data;
    } catch (e) {
      console.error('Error fetching inventory summary:', e);
      return null;
    }
  },
  getRecentOrders: async () => {
    try {
      const res = await axiosClient.get(API_ENDPOINTS.ORDERS.GET_ALL, { params: { size: 6, page: 0 } });
      return res.data;
    } catch (e) {
      console.error('Error fetching recent orders:', e);
      return null;
    }
  }
};

// ─── Dashboard Stats ─────────────────────────────────────────────────────────
export const dashboardStats = {
  totalSales: 48295,
  totalOrders: 1429,
  totalRevenue: 32180,
  totalCustomers: 2847,
  salesGrowth: 12.5,
  ordersGrowth: 8.3,
  revenueGrowth: 15.2,
  customersGrowth: 6.7,
};

export const salesChartData = [
  { month: 'Jan', sales: 28400, revenue: 18200 },
  { month: 'Feb', sales: 32100, revenue: 22400 },
  { month: 'Mar', sales: 27800, revenue: 19100 },
  { month: 'Apr', sales: 41200, revenue: 28600 },
  { month: 'May', sales: 38500, revenue: 26300 },
  { month: 'Jun', sales: 48295, revenue: 32180 },
  { month: 'Jul', sales: 43100, revenue: 29400 },
  { month: 'Aug', sales: 52300, revenue: 35800 },
  { month: 'Sep', sales: 47600, revenue: 31200 },
  { month: 'Oct', sales: 55400, revenue: 38100 },
  { month: 'Nov', sales: 62800, revenue: 42500 },
  { month: 'Dec', sales: 71200, revenue: 48900 },
];

export const ordersChartData = [
  { day: 'Mon', orders: 142 },
  { day: 'Tue', orders: 187 },
  { day: 'Wed', orders: 163 },
  { day: 'Thu', orders: 221 },
  { day: 'Fri', orders: 198 },
  { day: 'Sat', orders: 265 },
  { day: 'Sun', orders: 143 },
];

export const categoryChartData = [
  { name: 'Electronics', value: 38 },
  { name: 'Clothing', value: 22 },
  { name: 'Accessories', value: 15 },
  { name: 'Home', value: 13 },
  { name: 'Sports', value: 8 },
  { name: 'Other', value: 4 },
];

// ─── Products ────────────────────────────────────────────────────────────────
export const products = [
  { id: 1, name: 'iPhone 15 Pro Max', image: 'https://placehold.co/48x48/6366f1/fff?text=IP', price: 1199.99, stock: 45, category: 'Electronics', brand: 'Apple', status: 'active', sku: 'IP15PM-256', description: 'Latest flagship iPhone with titanium design.' },
  { id: 2, name: 'Samsung Galaxy S24 Ultra', image: 'https://placehold.co/48x48/22d3ee/fff?text=SG', price: 1099.99, stock: 32, category: 'Electronics', brand: 'Samsung', status: 'active', sku: 'SGS24U-512', description: 'Premium Android flagship with S Pen.' },
  { id: 3, name: 'Nike Air Max 270', image: 'https://placehold.co/48x48/f59e0b/fff?text=NK', price: 149.99, stock: 120, category: 'Sports', brand: 'Nike', status: 'active', sku: 'NAM270-10', description: 'Iconic Air Max cushioning for all-day comfort.' },
  { id: 4, name: 'MacBook Pro 14"', image: 'https://placehold.co/48x48/8b5cf6/fff?text=MB', price: 1999.99, stock: 18, category: 'Electronics', brand: 'Apple', status: 'active', sku: 'MBP14-M3', description: 'M3 chip powered professional laptop.' },
  { id: 5, name: 'Levi\'s 501 Jeans', image: 'https://placehold.co/48x48/3b82f6/fff?text=LV', price: 69.99, stock: 0, category: 'Clothing', brand: 'Levi\'s', status: 'out_of_stock', sku: 'LV501-32', description: 'Classic straight-leg denim jeans.' },
  { id: 6, name: 'Sony WH-1000XM5', image: 'https://placehold.co/48x48/10b981/fff?text=SO', price: 349.99, stock: 67, category: 'Electronics', brand: 'Sony', status: 'active', sku: 'SWHXM5-BLK', description: 'Industry-leading noise canceling headphones.' },
  { id: 7, name: 'Adidas Ultraboost 23', image: 'https://placehold.co/48x48/f43f5e/fff?text=AD', price: 189.99, stock: 84, category: 'Sports', brand: 'Adidas', status: 'active', sku: 'AUB23-9', description: 'Responsive running shoes with Boost cushioning.' },
  { id: 8, name: 'Ray-Ban Aviator Classic', image: 'https://placehold.co/48x48/eab308/fff?text=RB', price: 179.99, stock: 56, category: 'Accessories', brand: 'Ray-Ban', status: 'active', sku: 'RBA-3025', description: 'Iconic metal aviator sunglasses.' },
  { id: 9, name: 'iPad Pro 12.9"', image: 'https://placehold.co/48x48/6366f1/fff?text=ID', price: 1099.99, stock: 29, category: 'Electronics', brand: 'Apple', status: 'active', sku: 'IPP129-M2', description: 'Most powerful iPad with M2 chip.' },
  { id: 10, name: 'Zara Wool Coat', image: 'https://placehold.co/48x48/64748b/fff?text=ZR', price: 129.99, stock: 0, category: 'Clothing', brand: 'Zara', status: 'out_of_stock', sku: 'ZRWC-M', description: 'Elegant structured wool blend coat.' },
  { id: 11, name: 'Instant Pot Duo 7-in-1', image: 'https://placehold.co/48x48/f97316/fff?text=IP', price: 99.99, stock: 145, category: 'Home', brand: 'Instant Pot', status: 'active', sku: 'IPD7-6QT', description: 'Multi-use pressure cooker.' },
  { id: 12, name: 'Kindle Paperwhite', image: 'https://placehold.co/48x48/84cc16/fff?text=KN', price: 139.99, stock: 73, category: 'Electronics', brand: 'Amazon', status: 'draft', sku: 'KPW5-8G', description: 'Waterproof e-reader with adjustable warm light.' },
  { id: 13, name: 'Louis Vuitton Wallet', image: 'https://placehold.co/48x48/a78bfa/fff?text=LV', price: 489.99, stock: 12, category: 'Accessories', brand: 'Louis Vuitton', status: 'active', sku: 'LVMW-MON', description: 'Classic monogram canvas bifold wallet.' },
  { id: 14, name: 'DJI Mini 3 Pro', image: 'https://placehold.co/48x48/06b6d4/fff?text=DJ', price: 759.99, stock: 21, category: 'Electronics', brand: 'DJI', status: 'active', sku: 'DJM3P-RC', description: 'Lightweight drone with 4K/60fps camera.' },
  { id: 15, name: 'Patagonia Fleece Jacket', image: 'https://placehold.co/48x48/16a34a/fff?text=PT', price: 229.99, stock: 38, category: 'Clothing', brand: 'Patagonia', status: 'active', sku: 'PTFJ-M', description: 'Recycled-polyester synchilla snap-t.' },
];

// ─── Categories ──────────────────────────────────────────────────────────────
export const categories = [
  { id: 1, name: 'Electronics', slug: 'electronics', description: 'Gadgets, devices and tech accessories', products: 42, status: 'active', image: 'https://placehold.co/48x48/6366f1/fff?text=EL' },
  { id: 2, name: 'Clothing', slug: 'clothing', description: 'Fashion apparel for men and women', products: 128, status: 'active', image: 'https://placehold.co/48x48/f59e0b/fff?text=CL' },
  { id: 3, name: 'Accessories', slug: 'accessories', description: 'Bags, wallets, sunglasses and more', products: 67, status: 'active', image: 'https://placehold.co/48x48/10b981/fff?text=AC' },
  { id: 4, name: 'Home & Garden', slug: 'home-garden', description: 'Furniture, decor and garden essentials', products: 53, status: 'active', image: 'https://placehold.co/48x48/f43f5e/fff?text=HG' },
  { id: 5, name: 'Sports', slug: 'sports', description: 'Equipment and activewear for sports', products: 89, status: 'active', image: 'https://placehold.co/48x48/22d3ee/fff?text=SP' },
  { id: 6, name: 'Books', slug: 'books', description: 'Fiction, non-fiction and educational', products: 210, status: 'active', image: 'https://placehold.co/48x48/8b5cf6/fff?text=BK' },
  { id: 7, name: 'Beauty', slug: 'beauty', description: 'Skincare, makeup and personal care', products: 94, status: 'active', image: 'https://placehold.co/48x48/ec4899/fff?text=BT' },
  { id: 8, name: 'Toys', slug: 'toys', description: 'Toys and games for all ages', products: 47, status: 'inactive', image: 'https://placehold.co/48x48/f97316/fff?text=TY' },
];

// ─── Brands ──────────────────────────────────────────────────────────────────
export const brands = [
  { id: 1, name: 'Apple', logo: 'https://placehold.co/64x64/1e293b/fff?text=🍎', country: 'USA', products: 24, status: 'active', website: 'https://apple.com', description: 'Technology company known for iPhone, Mac, and iPad.' },
  { id: 2, name: 'Samsung', logo: 'https://placehold.co/64x64/1428a0/fff?text=S', country: 'South Korea', products: 31, status: 'active', website: 'https://samsung.com', description: 'Global electronics leader.' },
  { id: 3, name: 'Nike', logo: 'https://placehold.co/64x64/111827/fff?text=✓', country: 'USA', products: 56, status: 'active', website: 'https://nike.com', description: 'World\'s leading athletic brand.' },
  { id: 4, name: 'Adidas', logo: 'https://placehold.co/64x64/000000/fff?text=AD', country: 'Germany', products: 48, status: 'active', website: 'https://adidas.com', description: 'German multinational sportswear manufacturer.' },
  { id: 5, name: 'Sony', logo: 'https://placehold.co/64x64/0f0f0f/fff?text=S', country: 'Japan', products: 19, status: 'active', website: 'https://sony.com', description: 'Electronics and entertainment giant.' },
  { id: 6, name: 'Louis Vuitton', logo: 'https://placehold.co/64x64/8b6914/fff?text=LV', country: 'France', products: 12, status: 'active', website: 'https://louisvuitton.com', description: 'French luxury fashion house.' },
  { id: 7, name: 'Amazon', logo: 'https://placehold.co/64x64/f90/000?text=A', country: 'USA', products: 8, status: 'active', website: 'https://amazon.com', description: 'E-commerce and cloud computing company.' },
  { id: 8, name: 'Zara', logo: 'https://placehold.co/64x64/1c1c1c/fff?text=Z', country: 'Spain', products: 37, status: 'inactive', website: 'https://zara.com', description: 'Fast-fashion global retailer.' },
];

// ─── Customers ───────────────────────────────────────────────────────────────
export const customers = [
  { id: 1, name: 'Emma Johnson', email: 'emma.j@email.com', phone: '+1 555-0101', city: 'New York', orders: 14, totalSpent: 2840.50, status: 'active', joinDate: '2024-03-15', avatar: 'https://placehold.co/40x40/6366f1/fff?text=EJ' },
  { id: 2, name: 'James Williams', email: 'james.w@email.com', phone: '+1 555-0102', city: 'Los Angeles', orders: 8, totalSpent: 1290.00, status: 'active', joinDate: '2024-05-20', avatar: 'https://placehold.co/40x40/10b981/fff?text=JW' },
  { id: 3, name: 'Sophia Brown', email: 'sophia.b@email.com', phone: '+1 555-0103', city: 'Chicago', orders: 22, totalSpent: 5640.75, status: 'active', joinDate: '2023-11-08', avatar: 'https://placehold.co/40x40/f59e0b/fff?text=SB' },
  { id: 4, name: 'Michael Davis', email: 'michael.d@email.com', phone: '+1 555-0104', city: 'Houston', orders: 3, totalSpent: 349.99, status: 'inactive', joinDate: '2025-01-12', avatar: 'https://placehold.co/40x40/f43f5e/fff?text=MD' },
  { id: 5, name: 'Isabella Miller', email: 'isa.m@email.com', phone: '+1 555-0105', city: 'Phoenix', orders: 17, totalSpent: 3210.25, status: 'active', joinDate: '2024-07-03', avatar: 'https://placehold.co/40x40/8b5cf6/fff?text=IM' },
  { id: 6, name: 'Lucas Wilson', email: 'lucas.w@email.com', phone: '+1 555-0106', city: 'Philadelphia', orders: 5, totalSpent: 890.00, status: 'active', joinDate: '2024-09-18', avatar: 'https://placehold.co/40x40/22d3ee/fff?text=LW' },
  { id: 7, name: 'Olivia Moore', email: 'olivia.m@email.com', phone: '+1 555-0107', city: 'San Antonio', orders: 31, totalSpent: 7820.00, status: 'active', joinDate: '2023-06-22', avatar: 'https://placehold.co/40x40/ec4899/fff?text=OM' },
  { id: 8, name: 'Ethan Taylor', email: 'ethan.t@email.com', phone: '+1 555-0108', city: 'San Diego', orders: 0, totalSpent: 0, status: 'banned', joinDate: '2025-02-28', avatar: 'https://placehold.co/40x40/64748b/fff?text=ET' },
  { id: 9, name: 'Ava Anderson', email: 'ava.a@email.com', phone: '+1 555-0109', city: 'Dallas', orders: 9, totalSpent: 1640.50, status: 'active', joinDate: '2024-08-11', avatar: 'https://placehold.co/40x40/f97316/fff?text=AA' },
  { id: 10, name: 'Noah Jackson', email: 'noah.j@email.com', phone: '+1 555-0110', city: 'San Jose', orders: 12, totalSpent: 2180.75, status: 'active', joinDate: '2024-04-05', avatar: 'https://placehold.co/40x40/84cc16/fff?text=NJ' },
];

// ─── Orders ──────────────────────────────────────────────────────────────────
export const orders = [
  { id: '#ORD-1001', customerId: 1, customer: 'Emma Johnson', date: '2026-06-12', status: 'completed', total: 1349.98, items: 2, paymentStatus: 'paid' },
  { id: '#ORD-1002', customerId: 3, customer: 'Sophia Brown', date: '2026-06-12', status: 'processing', total: 349.99, items: 1, paymentStatus: 'paid' },
  { id: '#ORD-1003', customerId: 5, customer: 'Isabella Miller', date: '2026-06-11', status: 'pending', total: 229.99, items: 1, paymentStatus: 'pending' },
  { id: '#ORD-1004', customerId: 2, customer: 'James Williams', date: '2026-06-11', status: 'shipped', total: 189.99, items: 1, paymentStatus: 'paid' },
  { id: '#ORD-1005', customerId: 7, customer: 'Olivia Moore', date: '2026-06-10', status: 'completed', total: 2299.97, items: 3, paymentStatus: 'paid' },
  { id: '#ORD-1006', customerId: 9, customer: 'Ava Anderson', date: '2026-06-10', status: 'cancelled', total: 99.99, items: 1, paymentStatus: 'refunded' },
  { id: '#ORD-1007', customerId: 6, customer: 'Lucas Wilson', date: '2026-06-09', status: 'completed', total: 489.99, items: 1, paymentStatus: 'paid' },
  { id: '#ORD-1008', customerId: 10, customer: 'Noah Jackson', date: '2026-06-09', status: 'processing', total: 759.99, items: 1, paymentStatus: 'paid' },
  { id: '#ORD-1009', customerId: 4, customer: 'Michael Davis', date: '2026-06-08', status: 'pending', total: 149.99, items: 1, paymentStatus: 'pending' },
  { id: '#ORD-1010', customerId: 1, customer: 'Emma Johnson', date: '2026-06-08', status: 'completed', total: 1099.99, items: 1, paymentStatus: 'paid' },
  { id: '#ORD-1011', customerId: 3, customer: 'Sophia Brown', date: '2026-06-07', status: 'shipped', total: 179.99, items: 1, paymentStatus: 'paid' },
  { id: '#ORD-1012', customerId: 5, customer: 'Isabella Miller', date: '2026-06-07', status: 'completed', total: 69.99, items: 1, paymentStatus: 'paid' },
];

export const orderDetail = {
  id: '#ORD-1001',
  customer: { name: 'Emma Johnson', email: 'emma.j@email.com', phone: '+1 555-0101', avatar: 'https://placehold.co/48x48/6366f1/fff?text=EJ' },
  shippingAddress: '123 Main Street, New York, NY 10001, USA',
  items: [
    { id: 1, name: 'iPhone 15 Pro Max', image: 'https://placehold.co/48x48/6366f1/fff?text=IP', price: 1199.99, qty: 1, sku: 'IP15PM-256' },
    { id: 6, name: 'Sony WH-1000XM5', image: 'https://placehold.co/48x48/10b981/fff?text=SO', price: 149.99, qty: 1, sku: 'SWHXM5-BLK' },
  ],
  subtotal: 1349.98,
  shipping: 0,
  tax: 108.00,
  total: 1457.98,
  paymentMethod: 'Visa •••• 4242',
  status: 'completed',
  paymentStatus: 'paid',
  date: '2026-06-12',
  timeline: [
    { status: 'Order Placed', date: '2026-06-12 08:14', done: true },
    { status: 'Payment Confirmed', date: '2026-06-12 08:15', done: true },
    { status: 'Processing', date: '2026-06-12 09:30', done: true },
    { status: 'Shipped', date: '2026-06-12 14:20', done: true },
    { status: 'Delivered', date: '2026-06-12 17:45', done: true },
  ],
};

// ─── Payments ────────────────────────────────────────────────────────────────
export const payments = [
  { id: 'TXN-7001', orderId: '#ORD-1001', customer: 'Emma Johnson', amount: 1457.98, method: 'Visa ••4242', date: '2026-06-12', status: 'paid' },
  { id: 'TXN-7002', orderId: '#ORD-1002', customer: 'Sophia Brown', amount: 349.99, method: 'Mastercard ••8321', date: '2026-06-12', status: 'paid' },
  { id: 'TXN-7003', orderId: '#ORD-1003', customer: 'Isabella Miller', amount: 229.99, method: 'PayPal', date: '2026-06-11', status: 'pending' },
  { id: 'TXN-7004', orderId: '#ORD-1004', customer: 'James Williams', amount: 189.99, method: 'Visa ••5678', date: '2026-06-11', status: 'paid' },
  { id: 'TXN-7005', orderId: '#ORD-1005', customer: 'Olivia Moore', amount: 2299.97, method: 'Amex ••3456', date: '2026-06-10', status: 'paid' },
  { id: 'TXN-7006', orderId: '#ORD-1006', customer: 'Ava Anderson', amount: 99.99, method: 'Stripe', date: '2026-06-10', status: 'refunded' },
  { id: 'TXN-7007', orderId: '#ORD-1007', customer: 'Lucas Wilson', amount: 489.99, method: 'Visa ••9012', date: '2026-06-09', status: 'paid' },
  { id: 'TXN-7008', orderId: '#ORD-1008', customer: 'Noah Jackson', amount: 759.99, method: 'Mastercard ••2345', date: '2026-06-09', status: 'paid' },
  { id: 'TXN-7009', orderId: '#ORD-1009', customer: 'Michael Davis', amount: 149.99, method: 'PayPal', date: '2026-06-08', status: 'failed' },
  { id: 'TXN-7010', orderId: '#ORD-1010', customer: 'Emma Johnson', amount: 1099.99, method: 'Apple Pay', date: '2026-06-08', status: 'paid' },
];

// ─── Returns ─────────────────────────────────────────────────────────────────
export const returns = [
  { id: 'RET-001', orderId: '#ORD-0987', customer: 'Sophia Brown', product: 'iPhone 15 Pro Max', reason: 'Defective screen', date: '2026-06-10', status: 'approved', amount: 1199.99, type: 'return' },
  { id: 'RET-002', orderId: '#ORD-1006', customer: 'Ava Anderson', product: 'Instant Pot Duo', reason: 'Changed mind', date: '2026-06-10', status: 'completed', amount: 99.99, type: 'refund' },
  { id: 'RET-003', orderId: '#ORD-0845', customer: 'Lucas Wilson', product: 'Nike Air Max 270', reason: 'Wrong size', date: '2026-06-09', status: 'requested', amount: 149.99, type: 'exchange' },
  { id: 'RET-004', orderId: '#ORD-0923', customer: 'Michael Davis', product: 'Zara Wool Coat', reason: 'Not as described', date: '2026-06-08', status: 'rejected', amount: 129.99, type: 'return' },
  { id: 'RET-005', orderId: '#ORD-0811', customer: 'Isabella Miller', product: 'Ray-Ban Aviator', reason: 'Damaged in shipping', date: '2026-06-07', status: 'requested', amount: 179.99, type: 'refund' },
];

// ─── Users (Admin) ───────────────────────────────────────────────────────────
export const adminUsers = [
  { id: 1, name: 'Alex Carter', email: 'alex@shopadmin.com', role: 'Super Admin', status: 'active', lastLogin: '2026-06-12', avatar: 'https://placehold.co/40x40/6366f1/fff?text=AC' },
  { id: 2, name: 'Jordan Lee', email: 'jordan@shopadmin.com', role: 'Manager', status: 'active', lastLogin: '2026-06-11', avatar: 'https://placehold.co/40x40/10b981/fff?text=JL' },
  { id: 3, name: 'Morgan Patel', email: 'morgan@shopadmin.com', role: 'Editor', status: 'active', lastLogin: '2026-06-10', avatar: 'https://placehold.co/40x40/f59e0b/fff?text=MP' },
  { id: 4, name: 'Casey Rivera', email: 'casey@shopadmin.com', role: 'Viewer', status: 'inactive', lastLogin: '2026-05-28', avatar: 'https://placehold.co/40x40/f43f5e/fff?text=CR' },
  { id: 5, name: 'Riley Thompson', email: 'riley@shopadmin.com', role: 'Manager', status: 'active', lastLogin: '2026-06-12', avatar: 'https://placehold.co/40x40/8b5cf6/fff?text=RT' },
];

export const roles = [
  {
    id: 1,
    name: 'Super Admin',
    description: 'Full access to all modules and settings',
    userCount: 1,
    permissions: {
      Dashboard: { view: true, create: true, edit: true, delete: true, export: true },
      Products: { view: true, create: true, edit: true, delete: true, export: true },
      Categories: { view: true, create: true, edit: true, delete: true, export: true },
      Brands: { view: true, create: true, edit: true, delete: true, export: true },
      Orders: { view: true, create: true, edit: true, delete: true, export: true },
      Customers: { view: true, create: true, edit: true, delete: true, export: true },
      Payments: { view: true, create: false, edit: true, delete: false, export: true },
      Returns: { view: true, create: true, edit: true, delete: true, export: true },
      Reports: { view: true, create: false, edit: false, delete: false, export: true },
      Settings: { view: true, create: true, edit: true, delete: true, export: false },
      Users: { view: true, create: true, edit: true, delete: true, export: true },
    },
  },
  {
    id: 2,
    name: 'Manager',
    description: 'Manage products, orders, and customers',
    userCount: 2,
    permissions: {
      Dashboard: { view: true, create: false, edit: false, delete: false, export: true },
      Products: { view: true, create: true, edit: true, delete: false, export: true },
      Categories: { view: true, create: true, edit: true, delete: false, export: false },
      Brands: { view: true, create: true, edit: true, delete: false, export: false },
      Orders: { view: true, create: false, edit: true, delete: false, export: true },
      Customers: { view: true, create: false, edit: true, delete: false, export: true },
      Payments: { view: true, create: false, edit: false, delete: false, export: true },
      Returns: { view: true, create: false, edit: true, delete: false, export: false },
      Reports: { view: true, create: false, edit: false, delete: false, export: true },
      Settings: { view: false, create: false, edit: false, delete: false, export: false },
      Users: { view: false, create: false, edit: false, delete: false, export: false },
    },
  },
  {
    id: 3,
    name: 'Editor',
    description: 'View and edit content only',
    userCount: 1,
    permissions: {
      Dashboard: { view: true, create: false, edit: false, delete: false, export: false },
      Products: { view: true, create: true, edit: true, delete: false, export: false },
      Categories: { view: true, create: false, edit: true, delete: false, export: false },
      Brands: { view: true, create: false, edit: true, delete: false, export: false },
      Orders: { view: true, create: false, edit: false, delete: false, export: false },
      Customers: { view: true, create: false, edit: false, delete: false, export: false },
      Payments: { view: false, create: false, edit: false, delete: false, export: false },
      Returns: { view: true, create: false, edit: false, delete: false, export: false },
      Reports: { view: true, create: false, edit: false, delete: false, export: false },
      Settings: { view: false, create: false, edit: false, delete: false, export: false },
      Users: { view: false, create: false, edit: false, delete: false, export: false },
    },
  },
  {
    id: 4,
    name: 'Viewer',
    description: 'Read-only access to all modules',
    userCount: 1,
    permissions: {
      Dashboard: { view: true, create: false, edit: false, delete: false, export: false },
      Products: { view: true, create: false, edit: false, delete: false, export: false },
      Categories: { view: true, create: false, edit: false, delete: false, export: false },
      Brands: { view: true, create: false, edit: false, delete: false, export: false },
      Orders: { view: true, create: false, edit: false, delete: false, export: false },
      Customers: { view: true, create: false, edit: false, delete: false, export: false },
      Payments: { view: false, create: false, edit: false, delete: false, export: false },
      Returns: { view: true, create: false, edit: false, delete: false, export: false },
      Reports: { view: true, create: false, edit: false, delete: false, export: false },
      Settings: { view: false, create: false, edit: false, delete: false, export: false },
      Users: { view: false, create: false, edit: false, delete: false, export: false },
    },
  },
];
