````markdown
# E_Shop Order Service - Complete Developer Guide

**Project:** E_Shop Payment Integration  
**Component:** Order Service with Bakong Payment Integration  
**Created:** May 16, 2026  
**Last Updated:** May 18, 2026  
**Status:** In Progress (Backend: 70%, Frontend: 0%)

## 🎯 Quick Start for Frontend Developers

**New to this project?** Start here:
1. [Frontend API Quick Start](#frontend-api-quick-start) ⭐
2. [API Endpoints Reference](#api-endpoints-reference) 
3. [Integration Examples](#integration-examples)
4. [Common Errors & Solutions](#common-errors--solutions)

---

## 📋 Table of Contents
1. [Frontend API Quick Start](#frontend-api-quick-start)
2. [API Endpoints Reference](#api-endpoints-reference)
3. [Integration Examples](#integration-examples)
4. [Backend Service Tasks](#backend-service-tasks)
5. [REST API Controller Tasks](#rest-api-controller-tasks)
6. [Testing Tasks](#testing-tasks)
7. [Frontend Implementation Tasks](#frontend-implementation-tasks)
8. [Deployment Tasks](#deployment-tasks)
9. [Common Errors & Solutions](#common-errors--solutions)

---

## 🚀 FRONTEND API QUICK START

### Prerequisites
- Node.js / React application running on `localhost:3000`
- Backend API running on `http://localhost:8083`
- Authentication token (JWT) from login endpoint

### Base URL
```
http://localhost:8083/api/v1
```

### Authentication
All API calls require a Bearer token in the Authorization header:
```javascript
headers: {
  'Authorization': 'Bearer YOUR_JWT_TOKEN',
  'Content-Type': 'application/json'
}
```

### Step 1: Create an Order with Bakong Payment (30 seconds)

**Endpoint:** `POST /orders/create-with-bakong`

**Frontend Code Example (React):**
```javascript
import axios from 'axios';

const createOrderWithBakong = async (userId, addressId) => {
  try {
    const response = await axios.post(
      'http://localhost:8083/api/v1/orders/create-with-bakong',
      {
        addressId: addressId,
        paymentMethod: 'BAKONG'
      },
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    const order = response.data.data;
    return {
      orderId: order.id,
      orderNumber: order.order_number,
      totalAmount: order.total_amount,
      qrCode: order.qr_code,      // KHQR string - display as QR code
      paymentUrl: order.payment_url  // For mobile redirect
    };
  } catch (error) {
    console.error('Failed to create order:', error.response?.data);
  }
};
```

**Response Structure:**
```json
{
  "message": "Successfully!",
  "code": "200",
  "data": {
    "id": 71,
    "order_number": "ORD-0CE7BA4C",
    "order_date": "2026-05-18T15:20:00.000Z",
    "status": "PENDING",
    "total_amount": 1299,
    "qr_code": "00020101021229370016A0000000727302150...",
    "payment_url": "bakong://payment?qr=00020101021229370016A0000000727302150...",
    "items": [...],
    "payment": {
      "id": 71,
      "amount": 1299,
      "status": "PENDING",
      "payment_method": "BAKONG",
      "transaction_id": "BAKONG-ORD-0CE7BA4C"
    }
  }
}
```

### Step 2: Display QR Code (60 seconds)

**Install QR Code Library:**
```bash
npm install qrcode.react
```

**React Component:**
```javascript
import QRCode from 'qrcode.react';

export const PaymentQRCode = ({ qrCode, amount, expiresIn }) => {
  return (
    <div className="payment-qr-container">
      <h2>Scan to Pay</h2>
      <QRCode 
        value={qrCode}
        size={300}
        level="H"
        includeMargin={true}
      />
      <p>Amount: {amount} KHR</p>
      <p>Expires in: {expiresIn}</p>
      
      <button onClick={() => downloadQR()}>Download QR</button>
    </div>
  );
};

const downloadQR = () => {
  const qrCanvas = document.querySelector('canvas');
  const url = qrCanvas.toDataURL('image/png');
  const link = document.createElement('a');
  link.href = url;
  link.download = 'bakong-payment.png';
  link.click();
};
```

### Step 3: Monitor Payment Status (Real-time)

**Endpoint:** `GET /orders/{orderId}`

**React Hook for Payment Monitoring:**
```javascript
import { useState, useEffect } from 'react';

export const usePaymentStatus = (orderId, statusCheckInterval = 3000) => {
  const [paymentStatus, setPaymentStatus] = useState('PENDING');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const checkPaymentStatus = async () => {
      try {
        const response = await axios.get(
          `http://localhost:8083/api/v1/orders/${orderId}`,
          {
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('authToken')}`
            }
          }
        );
        
        const newStatus = response.data.data.payment.status;
        setPaymentStatus(newStatus);
        
        // Stop polling when payment is completed
        if (newStatus === 'COMPLETED') {
          clearInterval(interval);
          onPaymentSuccess?.();
        }
      } catch (error) {
        console.error('Error checking payment status:', error);
      }
    };

    const interval = setInterval(checkPaymentStatus, statusCheckInterval);
    checkPaymentStatus(); // Check immediately

    return () => clearInterval(interval);
  }, [orderId]);

  return { paymentStatus, isLoading };
};
```

**Usage in Component:**
```javascript
const { paymentStatus } = usePaymentStatus(orderId, 3000); // Check every 3 seconds

if (paymentStatus === 'COMPLETED') {
  return <PaymentSuccessScreen />;
}
```

---

## 📡 API ENDPOINTS REFERENCE

### Order Management Endpoints

#### 1️⃣ Create Order with Bakong Payment
```
POST /orders/create-with-bakong
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "addressId": 1,
  "paymentMethod": "BAKONG"
}

Response: 200 OK
{
  "message": "Successfully!",
  "code": "200",
  "data": {
    "id": 71,
    "order_number": "ORD-ABC12345",
    "status": "PENDING",
    "total_amount": 1299,
    "qr_code": "00020101...",
    "payment_url": "bakong://...",
    "items": [
      {
        "quantity": 1,
        "unit_price": 1299,
        "total_price": 1299,
        "product_sku": {
          "id": 1,
          "sku": "007",
          "price": 1299,
          "color": "White",
          "size": "14"
        }
      }
    ],
    "payment": {
      "id": 71,
      "amount": 1299,
      "status": "PENDING",
      "payment_method": "BAKONG",
      "payment_date": "2026-05-18T15:20:00Z",
      "transaction_id": "BAKONG-ORD-ABC12345",
      "payment_provider": "BAKONG"
    }
  }
}
```

#### 2️⃣ Get Order by ID
```
GET /orders/{orderId}
Authorization: Bearer {token}

Example:
GET /orders/71

Response: 200 OK
{
  "message": "Successfully!",
  "code": "200",
  "data": {
    "id": 71,
    "order_number": "ORD-ABC12345",
    "status": "PENDING",
    ...
  }
}

Error Cases:
- 404: Order not found with id: 71
- 401: Unauthorized (invalid token)
```

#### 3️⃣ Get Order by Order Number
```
GET /orders/number/{orderNumber}
Authorization: Bearer {token}

Example:
GET /orders/number/ORD-ABC12345

Response: 200 OK
```

#### 4️⃣ Get User Orders (Paginated)
```
GET /orders/user/{userId}?page=0&size=10&sort=orderDate,desc
Authorization: Bearer {token}

Query Parameters:
- page: Page number (0-indexed), default: 0
- size: Items per page, default: 10
- sort: Sort field and direction (e.g., orderDate,desc), default: orderDate,desc

Examples:
GET /orders/user/1?page=0&size=5
GET /orders/user/1?page=1&size=20&sort=totalAmount,desc

Response: 200 OK
{
  "message": "Successfully!",
  "code": "200",
  "data": {
    "content": [
      {
        "id": 71,
        "order_number": "ORD-ABC12345",
        "status": "PENDING",
        ...
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 5,
      "totalPages": 3,
      "totalElements": 15
    }
  }
}
```

#### 5️⃣ Initiate Bakong Payment
```
POST /orders/{orderId}/bakong/initiate
Authorization: Bearer {token}

Example:
POST /orders/71/bakong/initiate

Response: 200 OK
{
  "message": "Bakong payment initiated successfully",
  "code": "200",
  "data": {
    "orderId": 71,
    "orderNumber": "ORD-ABC12345",
    "qrCode": "00020101...",
    "paymentUrl": "bakong://...",
    "amount": 1299,
    "expiresIn": "15 minutes"
  }
}

Error Cases:
- 400: Order does not use Bakong payment method
- 400: Order is not in pending status
- 404: Order not found
```

#### 6️⃣ Verify Bakong Payment
```
POST /orders/{orderId}/bakong/verify
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "transactionId": "BAKONG-ORD-ABC12345"
}

Example:
POST /orders/71/bakong/verify
{
  "transactionId": "BAKONG-ORD-ABC12345"
}

Response: 200 OK
{
  "message": "Payment verified successfully",
  "code": "200",
  "data": {
    "orderId": 71,
    "orderNumber": "ORD-ABC12345",
    "status": "CONFIRMED",
    "paymentStatus": "COMPLETED"
  }
}

Or (if payment failed):
{
  "message": "Payment verification failed",
  "code": "200",
  "data": {
    "orderId": 71,
    "status": "PAYMENT_FAILED",
    "error": "Transaction not found or expired"
  }
}
```

#### 7️⃣ Cancel Order
```
DELETE /orders/{orderId}/cancel
Authorization: Bearer {token}

Example:
DELETE /orders/71/cancel

Response: 200 OK
{
  "message": "Successfully!",
  "code": "200",
  "data": {
    "id": 71,
    "order_number": "ORD-ABC12345",
    "status": "CANCELLED",
    "payment": {
      "status": "REFUNDED"
    }
  }
}

Error Cases:
- 400: Only pending orders can be cancelled
- 403: User does not own this order
- 404: Order not found
```

#### 8️⃣ Update Order Status (Admin Only)
```
PUT /orders/{orderId}/status
Content-Type: application/json
Authorization: Bearer {admin_token}

Request Body:
{
  "status": "SHIPPED"  // or "DELIVERED", "CANCELLED"
}

Response: 200 OK
```

#### 9️⃣ Get All Orders (Admin Only)
```
GET /orders?page=0&size=20&sort=orderDate,desc
Authorization: Bearer {admin_token}

Response: 200 OK (Paginated list of all orders)
```

---

## 💻 INTEGRATION EXAMPLES

### Example 1: Complete Checkout Flow (React)

```javascript
import React, { useState } from 'react';
import axios from 'axios';
import QRCode from 'qrcode.react';

const CheckoutPage = () => {
  const [step, setStep] = useState('confirm'); // confirm, payment, success, error
  const [orderId, setOrderId] = useState(null);
  const [qrCode, setQrCode] = useState(null);
  const [paymentStatus, setPaymentStatus] = useState('PENDING');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleCreateOrder = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.post(
        'http://localhost:8083/api/v1/orders/create-with-bakong',
        {
          addressId: selectedAddressId,
          paymentMethod: 'BAKONG'
        },
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
          }
        }
      );

      const data = response.data.data;
      setOrderId(data.id);
      setQrCode(data.qr_code);
      setPaymentStatus(data.payment?.status || 'PENDING');
      setStep('payment');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create order');
      setStep('error');
    } finally {
      setLoading(false);
    }
  };

  // Poll payment status
  React.useEffect(() => {
    if (step !== 'payment' || !orderId) return;

    const interval = setInterval(async () => {
      try {
        const response = await axios.get(
          `http://localhost:8083/api/v1/orders/${orderId}`,
          {
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('authToken')}`
            }
          }
        );

        const status = response.data.data.payment?.status;
        setPaymentStatus(status);

        if (status === 'COMPLETED') {
          clearInterval(interval);
          setStep('success');
        }
      } catch (err) {
        console.error('Error checking payment:', err);
      }
    }, 3000); // Check every 3 seconds

    return () => clearInterval(interval);
  }, [orderId, step]);

  if (step === 'success') {
    return (
      <div className="success-screen">
        <h1>✅ Payment Successful!</h1>
        <p>Your order has been confirmed.</p>
        <p>Order Number: {orderId}</p>
        <button onClick={() => window.location.href = '/orders'}>
          View Orders
        </button>
      </div>
    );
  }

  if (step === 'error') {
    return (
      <div className="error-screen">
        <h1>❌ Error</h1>
        <p>{error}</p>
        <button onClick={() => { setStep('confirm'); setError(null); }}>
          Try Again
        </button>
      </div>
    );
  }

  if (step === 'payment') {
    return (
      <div className="payment-screen">
        <h2>Scan to Pay</h2>
        {qrCode && (
          <>
            <QRCode value={qrCode} size={300} />
            <p>Payment Link: {window.location.origin}/pay?order={orderId}</p>
          </>
        )}
        <p>Amount: {totalAmount} KHR</p>
        <p>Status: {paymentStatus}</p>
        {paymentStatus === 'PENDING' && (
          <p className="waiting">Waiting for payment...</p>
        )}
      </div>
    );
  }

  return (
    <div className="confirm-screen">
      <h2>Confirm Order</h2>
      <p>Total: {totalAmount} KHR</p>
      <button onClick={handleCreateOrder} disabled={loading}>
        {loading ? 'Creating...' : 'Place Order with Bakong'}
      </button>
    </div>
  );
};

export default CheckoutPage;
```

### Example 2: Order History Component

```javascript
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const OrderHistory = ({ userId }) => {
  const [orders, setOrders] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchOrders();
  }, [page]);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const response = await axios.get(
        `http://localhost:8083/api/v1/orders/user/${userId}`,
        {
          params: {
            page: page,
            size: 10,
            sort: 'orderDate,desc'
          },
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
          }
        }
      );

      setOrders(response.data.data.content);
      setTotalPages(response.data.data.pageable.totalPages);
    } catch (error) {
      console.error('Failed to fetch orders:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      'PENDING': '#ff9800',
      'CONFIRMED': '#4caf50',
      'SHIPPED': '#2196f3',
      'DELIVERED': '#4caf50',
      'CANCELLED': '#f44336'
    };
    return colors[status] || '#999';
  };

  return (
    <div className="order-history">
      <h2>My Orders</h2>
      {loading ? (
        <p>Loading...</p>
      ) : orders.length === 0 ? (
        <p>No orders yet</p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <th>Order #</th>
                <th>Date</th>
                <th>Amount</th>
                <th>Payment</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td>{order.order_number}</td>
                  <td>{new Date(order.order_date).toLocaleDateString()}</td>
                  <td>{order.total_amount} KHR</td>
                  <td>{order.payment?.payment_method}</td>
                  <td>
                    <span 
                      className="status"
                      style={{ 
                        backgroundColor: getStatusColor(order.status),
                        padding: '5px 10px',
                        borderRadius: '4px',
                        color: '#fff'
                      }}
                    >
                      {order.status}
                    </span>
                  </td>
                  <td>
                    <button onClick={() => viewOrder(order.id)}>View</button>
                    {order.status === 'PENDING' && (
                      <button onClick={() => cancelOrder(order.id)}>Cancel</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          <div className="pagination">
            <button 
              disabled={page === 0} 
              onClick={() => setPage(page - 1)}
            >
              Previous
            </button>
            <span>Page {page + 1} of {totalPages}</span>
            <button 
              disabled={page >= totalPages - 1} 
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default OrderHistory;
```

### Example 3: API Service Utility Class

```javascript
// services/orderService.js

import axios from 'axios';

const API_BASE_URL = 'http://localhost:8083/api/v1';

class OrderService {
  constructor() {
    this.api = axios.create({
      baseURL: API_BASE_URL,
      timeout: 10000
    });

    // Add auth token to every request
    this.api.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
  }

  // Create order with Bakong
  async createOrderWithBakong(addressId) {
    try {
      const response = await this.api.post('/orders/create-with-bakong', {
        addressId,
        paymentMethod: 'BAKONG'
      });
      return response.data.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Get order by ID
  async getOrderById(orderId) {
    try {
      const response = await this.api.get(`/orders/${orderId}`);
      return response.data.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Get user orders
  async getUserOrders(userId, page = 0, size = 10) {
    try {
      const response = await this.api.get(`/orders/user/${userId}`, {
        params: {
          page,
          size,
          sort: 'orderDate,desc'
        }
      });
      return response.data.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Initiate Bakong payment
  async initiateBakongPayment(orderId) {
    try {
      const response = await this.api.post(`/orders/${orderId}/bakong/initiate`);
      return response.data.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Verify payment
  async verifyPayment(orderId, transactionId) {
    try {
      const response = await this.api.post(`/orders/${orderId}/bakong/verify`, {
        transactionId
      });
      return response.data.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Cancel order
  async cancelOrder(orderId) {
    try {
      const response = await this.api.delete(`/orders/${orderId}/cancel`);
      return response.data.data;
    } catch (error) {
      throw this.handleError(error);
    }
  }

  // Error handler
  handleError(error) {
    if (error.response) {
      return new Error(error.response.data?.message || 'API Error');
    }
    return error;
  }
}

export default new OrderService();
```

---

## ⚠️ COMMON ERRORS & SOLUTIONS

### Error 1: "Cart is empty"
**Cause:** User trying to create order with empty cart  
**Solution:**
```javascript
// Check cart before creating order
if (cartItems.length === 0) {
  alert('Please add items to cart first');
  return;
}
```

### Error 2: "Address not found"  
**Cause:** Address ID doesn't exist or doesn't belong to user  
**Solution:**
```javascript
// Fetch user's addresses first
const addresses = await fetchUserAddresses(userId);
// Only use valid address IDs
const validAddresses = addresses.map(a => a.id);
if (!validAddresses.includes(selectedAddressId)) {
  alert('Invalid address selected');
  return;
}
```

### Error 3: "Order not found"
**Cause:** Order ID doesn't exist
**Solution:**
```javascript
// Verify order exists after creation
if (!response.data.data.id) {
  console.error('Order creation failed');
  return;
}
const orderId = response.data.data.id;
```

### Error 4: QR Code is null  
**Cause:** Bakong API connection issue (see logs below)  
**Current Issue:** 403 Forbidden from Bakong CloudFront  
**Workaround:**
```javascript
if (!order.qr_code) {
  console.warn('QR code not available, retrying...');
  // Retry after 2 seconds
  setTimeout(() => refetchOrder(orderId), 2000);
}
```

### Error 5: Payment never completes
**Cause:**  
- Bakong API not responding (403 errors)
- Token expiration
- Network firewall blocking

**Solution:**
```javascript
// Add timeout handling
const checkPaymentWithTimeout = async (orderId) => {
  const startTime = Date.now();
  const MAX_WAIT = 15 * 60 * 1000; // 15 minutes

  while (Date.now() - startTime < MAX_WAIT) {
    try {
      const response = await axios.get(`/orders/${orderId}`);
      if (response.data.data.payment?.status === 'COMPLETED') {
        return response.data.data;
      }
    } catch (error) {
      console.error('Error checking payment:', error);
    }
    
    await new Promise(resolve => setTimeout(resolve, 3000));
  }
  
  throw new Error('Payment verification timeout');
};
```

### Error 6: 403 Forbidden - CloudFront Blocking
**Current Issue in Logs:**
```
ERROR: Failed to obtain Bakong token: 403 Forbidden: 
Request blocked. [CloudFront]
Request blocked. We can't connect to the server for this app or website.
```

**Root Cause:** 
1. Invalid Bakong credentials
2. IP blocked by Bakong firewall
3. Invalid API endpoint
4. Missing/expired OAuth token

**Solution - Check Backend Logs:**
```bash
# View application logs
tail -f logs/application.log | grep -i bakong

# Check if credentials are correct in application.properties
grep -i bakong src/main/resources/application.properties
```

**Expected Output:**
```properties
bakong.account-id=senghour_soeurng@bkrt
bakong.base-url=https://api-bakong.nbc.gov.kh
bakong.email=seanghour097328@gmail.com
```

**Action Items:**
1. Verify credentials with Bakong support
2. Check network connectivity to api-bakong.nbc.gov.kh
3. Ensure VPN/Firewall allows outbound HTTPS to Bakong
4. Contact Bakong support if IP is blocked

### Error 7: 401 Unauthorized
**Cause:** Invalid or expired JWT token  
**Solution:**
```javascript
// Check token validity
if (error.response?.status === 401) {
  // Token expired, redirect to login
  localStorage.removeItem('authToken');
  window.location.href = '/login';
}
```

### Error 8: CORS Error - Frontend to Backend
**Error Message:** 
```
Access to XMLHttpRequest has been blocked by CORS policy
```

**Cause:** Backend CORS configuration not allowing frontend requests  
**Solution - Backend (Already Configured):**
```java
// Already in CustomCorsFilter
header("Access-Control-Allow-Origin", "http://localhost:3000");
header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
header("Access-Control-Allow-Headers", "Content-Type, Authorization");
```

**Frontend Workaround:**
```javascript
// Try using a proxy in package.json (during development)
// "proxy": "http://localhost:8083"
```

---

## 📋 Backend Service Tasks

### Phase 1: Core Order Service (✅ Completed)

- [x] **Create `OrderRepository` with custom queries** (BS-001)
- [x] **Create `Order` and `OrderItem` models** (BS-002)
- [x] **Create `OrderService` interface** (BS-003)
- [x] **Implement `OrderServiceImpl` (Basic)** (BS-004)

### Phase 2: Bakong Integration (🔄 In Progress - 85% Complete)

- [x] **Add Bakong service dependencies** (BS-005)
- [x] **Implement `createOrderWithBakongPayment()`** (BS-006) - ⚠️ Minor issues
- [x] **Implement `initiateBakongPayment()`** (BS-007) - ⚠️ Minor issues
- [x] **Implement `verifyBakongPayment()`** (BS-008)
- [x] **Implement `processBakongPaymentCallback()`** (BS-009)

### Phase 3: Critical Fixes (🔴 CRITICAL)

- [ ] **Fix `BakongService` interface return type** (BS-010)
  - Change: `KHQRResponse<KHQRData>` → `BakongResponse`
  - Impact: 15 minutes
  - File: `BakongService.java`

- [ ] **Remove unused KHQR imports** (BS-011)
  - File: `OrderServiceImpl.java` (Lines 20-21)
  - Impact: 10 minutes

- [ ] **Fix type checking logic** (BS-012)
  - File: `OrderServiceImpl.java` (Lines 183-197)
  - Impact: 20 minutes

---

## 📡 REST API Controller Tasks

### Priority: 🔴 CRITICAL - Required for frontend development

- [ ] **Create `OrderController` class** (AC-001)
  - Estimate: 1-2 hours
  - Files: `controller/OrderController.java`
  - Endpoints needed: 9 (all documented above)

- [ ] **Implement Bakong endpoints** (AC-002)
  - Estimate: 1.5 hours

- [ ] **Add request validation** (AC-003)
  - Estimate: 45 minutes

- [ ] **Add Swagger/OpenAPI docs** (AC-004)
  - Estimate: 1 hour

---

## 🧪 Testing Tasks

### Unit Tests (🔴 Not Started)

- [ ] `OrderServiceImplTest` (UT-001) - 2 hours
- [ ] `BakongPaymentTest` (UT-002) - 2.5 hours

### Integration Tests (🔴 Not Started)

- [ ] `OrderControllerIntegrationTest` (IT-001) - 3 hours
- [ ] `BakongPaymentIntegrationTest` (IT-002) - 2 hours

---

## 🎨 FRONTEND IMPLEMENTATION TASKS

### Phase 1: Order Management UI (🔴 Not Started - 0%)

- [ ] **Order List Page**
  - Display paginated orders
  - Filter by status
  - Search by order number
  - Estimate: 2 hours

- [ ] **Order Detail Page**
  - Show full order information
  - Display items with images
  - Show shipping address
  - Estimate: 2 hours

- [ ] **Checkout Page**
  - Address selection
  - Payment method selection
  - Order summary
  - Place order button
  - Estimate: 3 hours

### Phase 2: Bakong Payment UI (🔴 Not Started - 0%)

- [ ] **QR Code Display Component**
  - Display QR code image
  - Show timer
  - Download button
  - Estimate: 1.5 hours

- [ ] **Payment Status Monitor**
  - Real-time status polling
  - Auto-redirect on success
  - Timeout handling
  - Estimate: 2 hours

- [ ] **Payment Callback Handler**
  - Redirect from Bakong
  - Payment result processing
  - Status update
  - Estimate: 1 hour

### Phase 3: Form Validation (🔴 Not Started - 0%)

- [ ] **Frontend validation**
  - Address selection required
  - Form validation
  - Error messages
  - Estimate: 1 hour

- [ ] **Error handling UI**
  - Error messages
  - Toast notifications
  - Retry logic
  - Estimate: 1.5 hours

---

## 🚀 DEPLOYMENT TASKS

- [ ] Fix all backend compilation errors (DT-001) - 30 mins
- [ ] Run Maven clean package (DT-002) - 5 mins
- [ ] Run all unit tests (DT-003) - 10 mins
- [ ] Build Docker image (DT-004) - 5 mins
- [ ] Test Docker container (DT-005) - 10 mins

---

## 📊 Project Summary

### Completion Status
| Component | Completed | Total | % |
|-----------|-----------|-------|---|
| Backend Service | 9 | 13 | 69% |
| API Controller | 0 | 4 | 0% |
| Unit Tests | 0 | 2 | 0% |
| Frontend | 0 | 9 | 0% |
| Deployment | 0 | 5 | 0% |
| **TOTAL** | **9** | **33** | **27%** |

### Timeline Estimate
- **Week 1**: Backend fixes + API controller (5-8 hours)
- **Week 2**: Unit tests + integration tests (8 hours)
- **Week 3**: Frontend components (13-15 hours)
- **Week 4**: Testing + deployment (5-8 hours)

**Total Estimate: 31-39 hours**

---

## 📞 Support Resources

- **API Documentation:** This file (SERVICE_ORDER_TASKS.md)
- **Postman Collection:** `Bakong_E_Shop.postman_collection.json`
- **Bakong Docs:** `https://bakong.nbc.gov.kh`
- **Backend Logs:** `logs/application.log`

---

## 🎯 Next Steps

1. **For Frontend Devs:**
   - Use [Frontend API Quick Start](#frontend-api-quick-start) section
   - Copy code examples from [Integration Examples](#integration-examples)
   - Reference [API Endpoints Reference](#api-endpoints-reference) while coding

2. **For Backend Devs:**
   - View [Critical Fixes](#phase-3-critical-fixes-critical) section
   - Fix type mismatches (30 mins)
   - Build API controller (2 hours)

3. **For QA:**
   - Open [Common Errors & Solutions](#common-errors--solutions)
   - Use provided test cases for manual testing

````

