# Order Management System - Tasks & Documentation

## Overview
This document outlines all backend and frontend tasks needed to implement a complete order management system with Bakong payment gateway integration for the E-Shop application.

---

## TABLE OF CONTENTS
1. [Backend Tasks](#backend-tasks)
2. [Frontend Tasks](#frontend-tasks)
3. [API Contracts](#api-contracts)
4. [Database Schema](#database-schema)
5. [Payment Flow](#payment-flow)
6. [Testing Checklist](#testing-checklist)

---

## BACKEND TASKS

### Phase 1: Core Order Management (Completed ✓)

#### Task B1.1: Order Model & Repository Setup
- **Status**: ✓ Completed
- **Description**: Create JPA entities and repositories for order management
- **Deliverables**:
  - `OrderDetail.java` - Main order entity with relationships
  - `OrderItem.java` - Line items in order
  - `Payment.java` - Payment information entity
  - `OrderRepository.java`, `OrderItemRepository.java`, `PaymentRepository.java`

**Database Tables**:
```sql
-- Orders table
CREATE TABLE order_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(255) UNIQUE NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(50),
    total_amount DECIMAL(19,2),
    shipping_address_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (shipping_address_id) REFERENCES addresses(id)
);

-- Order Items table
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(19,2),
    total_price DECIMAL(19,2),
    FOREIGN KEY (order_id) REFERENCES order_details(id),
    FOREIGN KEY (product_sku_id) REFERENCES product_sku(id)
);

-- Payments table
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL UNIQUE,
    payment_method VARCHAR(50),
    amount DECIMAL(19,2),
    status VARCHAR(50),
    payment_date TIMESTAMP,
    transaction_id VARCHAR(255),
    payment_provider VARCHAR(50),
    payment_provider_response LONGTEXT,
    FOREIGN KEY (order_id) REFERENCES order_details(id)
);
```

---

#### Task B1.2: OrderService Interface & Implementation
- **Status**: ✓ Completed
- **Description**: Create service layer for order operations
- **Key Methods**:
  - `createOrderFromCart(userId, request)` - Create order from user's cart
  - `getOrderById(id)` - Retrieve order by ID
  - `getOrderByNumber(orderNumber)` - Retrieve order by order number
  - `getUserOrders(userId, pageable)` - Get paginated user orders
  - `getAllOrders(pageable)` - Get all orders (admin)
  - `updateOrderStatus(id, status)` - Update order status
  - `cancelOrder(id, userId)` - Cancel pending order

**Features**:
- Automatic order number generation (ORD-XXXXXXXX)
- Stock management (reduce on creation, restore on cancellation)
- Cart clearing after order creation
- Payment status synchronization

---

### Phase 2: Bakong Payment Integration (In Progress)

#### Task B2.1: Bakong Configuration & Setup
- **Status**: ✓ Completed
- **Description**: Configure Bakong API credentials and connection
- **Configuration**:
  ```properties
  # application.properties
  bakong.account-id=senghour_soeurng@bkrt
  bakong.base-url=https://api-bakong.nbc.gov.kh
  bakong.email=seanghour097328@gmail.com
  ```

---

#### Task B2.2: Bakong DTOs & Models
- **Status**: ✓ Completed
- **Description**: Create data transfer objects for Bakong integration
- **Deliverables**:
  - `BakongRequest.java` - Request DTO for QR generation
  - `BakongResponse.java` - Response DTO for API responses
  - `CheckTransactionRequest.java` - Request for transaction verification
  - `GetQRImageRequest.java` - Request for QR image generation

---

#### Task B2.3: BakongService Implementation
- **Status**: 🔄 In Progress - Need to fix method return types
- **Description**: Service layer for Bakong API operations
- **Key Methods**:
  - `generateQR(BakongRequest)` - Generate KHQR code for payment
  - `checkTransactionByMD5(CheckTransactionRequest)` - Verify transaction status
  - `getQRImage(GetQRImageRequest)` - Convert QR string to PNG image

**Current Issue**: Return type mismatch on `generateQR()` method
- Interface declares: `BakongResponse generateQR(BakongRequest request)`
- Implementation returns: `KHQRResponse<KHQRData>`
- **Fix Required**: Convert KHQRResponse to BakongResponse format

---

#### Task B2.4: BakongTokenService Implementation
- **Status**: ✓ Completed
- **Description**: Handle Bearer token management for Bakong API authentication
- **Key Methods**:
  - `getToken()` - Retrieve or refresh access token
  - Token caching and expiration management

---

### Phase 3: Order Payment Integration (In Progress)

#### Task B3.1: Create Order with Bakong Payment
- **Status**: Partially Completed
- **Description**: Implement `createOrderWithBakongPayment(userId, request)` method
- **Flow**:
  1. Validate payment method is "BAKONG"
  2. Create order from cart using `createOrderFromCart()`
  3. Generate QR code using `bakongService.generateQR()`
  4. Store QR code in payment record
  5. Return order with QR code and payment link

**Expected Response**:
```json
{
  "id": 1,
  "orderNumber": "ORD-ABC12345",
  "totalAmount": 50000,
  "status": "PENDING",
  "qrCode": "00020101021229370016A000...",
  "paymentUrl": "bakong://payment?qr=00020101021229370016A000...",
  "items": [...]
}
```

**Current Issue**: Compilation error on return type conversion
- Method expects: `BakongResponse`
- Bakong library returns: `KHQRResponse<KHQRData>`
- **Fix Required**: Need to convert response properly

---

#### Task B3.2: Initiate Bakong Payment
- **Status**: Partially Completed
- **Description**: Implement `initiateBakongPayment(orderId)` method
- **Flow**:
  1. Fetch order by ID
  2. Validate order status is "PENDING"
  3. Validate payment method is "BAKONG"
  4. Generate new QR code
  5. Update payment with transaction ID and QR code
  6. Return QR code and payment details

**Expected Response**:
```json
{
  "message": "Bakong payment initiated successfully",
  "orderId": 1,
  "orderNumber": "ORD-ABC12345",
  "qrCode": "00020101021229370016A000...",
  "paymentUrl": "bakong://payment?qr=00020101021229370016A000...",
  "amount": 50000,
  "expiresIn": "15 minutes"
}
```

---

#### Task B3.3: Verify Bakong Payment
- **Status**: Partially Completed
- **Description**: Implement `verifyBakongPayment(orderId, transactionId)` method
- **Flow**:
  1. Fetch order by ID
  2. Validate payment method is "BAKONG"
  3. Extract MD5 from transaction ID
  4. Call `checkTransactionByMD5()` on Bakong API
  5. Update order status to "CONFIRMED" on success
  6. Update payment status to "COMPLETED"

**Expected Response (Success)**:
```json
{
  "message": "Payment verified successfully",
  "orderId": 1,
  "orderNumber": "ORD-ABC12345",
  "status": "CONFIRMED",
  "paymentStatus": "COMPLETED"
}
```

---

#### Task B3.4: Process Bakong Callback
- **Status**: Partially Completed
- **Description**: Implement `processBakongPaymentCallback(orderNumber, transactionId, status)` method
- **Flow**:
  1. Fetch order by order number
  2. Validate payment method is "BAKONG"
  3. Update payment transaction ID
  4. Handle status (SUCCESS/FAILED/CANCELLED/PENDING)
  5. Update order status accordingly
  6. Restore inventory on failure/cancellation

**Status Mapping**:
```
SUCCESS/COMPLETED → Order: CONFIRMED, Payment: COMPLETED
FAILED/CANCELLED → Order: CANCELLED, Payment: FAILED (restore inventory)
PENDING → Order: PENDING, Payment: PENDING
```

---

### Phase 4: REST API Endpoints (To Do)

#### Task B4.1: Order Controller Endpoints
- **Status**: 🔄 In Progress
- **Description**: Create REST endpoints for order operations
- **Endpoints to Implement**:

**User Orders**:
```
POST /api/v1/orders/create-from-cart
  - Create order from user's cart
  - Body: { addressId, paymentMethod }
  - Response: OrderResponse

POST /api/v1/orders/create-with-bakong
  - Create order with Bakong payment
  - Body: { addressId }
  - Response: OrderResponse with QR code

GET /api/v1/orders/{id}
  - Get order by ID
  - Response: OrderResponse

GET /api/v1/orders/number/{orderNumber}
  - Get order by order number
  - Response: OrderResponse

GET /api/v1/orders/my-orders?page=0&size=10
  - Get user's orders (paginated)
  - Response: Page<OrderResponse>

PUT /api/v1/orders/{id}/cancel
  - Cancel pending order
  - Response: OrderResponse
```

**Payment Operations**:
```
POST /api/v1/orders/{id}/bakong/initiate
  - Initiate Bakong payment for order
  - Response: { qrCode, paymentUrl, expiresIn }

POST /api/v1/orders/{id}/bakong/verify
  - Verify Bakong payment
  - Body: { transactionId }
  - Response: { status, message }

POST /api/v1/orders/bakong/callback
  - Bakong payment callback webhook
  - Body: { orderNumber, transactionId, status }
  - Response: { success, message }
```

**Admin Orders**:
```
GET /api/v1/admin/orders?page=0&size=10
  - Get all orders (paginated)
  - Response: Page<OrderResponse>

PUT /api/v1/admin/orders/{id}/status
  - Update order status
  - Body: { status }
  - Response: OrderResponse
```

---

### Phase 5: Error Handling & Validation (To Do)

#### Task B5.1: Order Validation
- **Status**: To Do
- **Description**: Add validation for order operations
- **Validations**:
  - Cart not empty before order creation
  - Address belongs to user
  - Product availability and stock
  - Payment amount matches order total
  - Order status transitions (prevent invalid state changes)

#### Task B5.2: Exception Handling
- **Status**: To Do
- **Description**: Create custom exceptions for order operations
- **Exceptions**:
  - `OrderNotFoundException`
  - `InvalidPaymentMethodException`
  - `InsufficientStockException`
  - `InvalidOrderStatusException`
  - `BakongPaymentException`

---

### Phase 6: Unit Tests (To Do)

#### Task B6.1: OrderService Tests
- **Status**: To Do
- **Description**: Write unit tests for OrderService methods
- **Test Cases**:
  - Create order from valid cart
  - Create order from empty cart (should fail)
  - Get order by ID/number
  - Update order status
  - Cancel pending order
  - Cancel non-pending order (should fail)

#### Task B6.2: Bakong Service Tests
- **Status**: To Do
- **Description**: Write unit tests for Bakong integration
- **Test Cases**:
  - Generate QR code with valid request
  - Generate QR code with invalid merchant info
  - Check transaction with valid MD5
  - Check transaction with invalid MD5
  - Token refresh mechanism

#### Task B6.3: Payment Integration Tests
- **Status**: To Do
- **Description**: Integration tests for payment flow
- **Test Cases**:
  - Create order with Bakong → verify QR generated
  - Initiate payment → verify transaction ID set
  - Verify payment → verify order status updated
  - Process callback → verify correct status transitions

---

## FRONTEND TASKS

### Phase 1: Order Display & Management UI (To Do)

#### Task F1.1: Order List Page
- **Status**: To Do
- **Description**: Display user's orders in a paginated list
- **Features**:
  - Pagination (10 items per page)
  - Order status filter (PENDING, CONFIRMED, CANCELLED)
  - Search by order number
  - Sort by date (newest first)
  - Quick actions (View, Cancel, Pay)

**UI Components**:
- OrderList component
- OrderCard component (displays: order #, date, total, status, items count)
- Filters & search bar
- Pagination controls

---

#### Task F1.2: Order Detail Page
- **Status**: To Do
- **Description**: Show complete order information
- **Features**:
  - Order header (order #, date, status, total amount)
  - Shipping address details
  - Order items list (product name, qty, price, subtotal)
  - Payment information
  - Action buttons based on status

**UI Components**:
- OrderDetail component
- OrderItemTable component
- ShippingAddressCard component
- PaymentCard component
- ActionButtons component

---

#### Task F1.3: Order Creation Workflow
- **Status**: To Do
- **Description**: Multi-step order creation from cart
- **Flow**:
  1. Review cart items
  2. Select/confirm shipping address
  3. Choose payment method (BAKONG or COD)
  4. Review order total
  5. Confirm order creation
  6. Show success message with order number

**UI Components**:
- CheckoutStepper component
- CartReview component
- AddressSelector component
- PaymentMethodSelector component
- OrderConfirmation component

---

### Phase 2: Bakong Payment Integration UI (To Do)

#### Task F2.1: Bakong QR Code Display
- **Status**: To Do
- **Description**: Display Bakong QR code after order creation
- **Features**:
  - Show QR code image
  - Display order amount in KHR
  - Countdown timer (15 minutes expiration)
  - "Copy QR Code" button
  - Deep link support for Bakong app
  - Fallback text if QR not available

**UI Components**:
- BakongQRDisplay component
- QRImage component
- ExpirationTimer component
- QuickActionButtons component

---

#### Task F2.2: Payment Status Tracking
- **Status**: To Do
- **Description**: Real-time payment status updates
- **Features**:
  - Poll server for payment status (every 5 seconds initially)
  - Auto-refresh on focus
  - Timeout after 30 minutes
  - Show payment processed confirmation
  - Redirect to order detail on success

**Implementation**:
- Use WebSocket or polling for status updates
- Implement exponential backoff for polling
- Handle connection errors gracefully

---

#### Task F2.3: Payment Methods Selection
- **Status**: To Do
- **Description**: Allow user to select payment method during checkout
- **Options**:
  - Cash on Delivery (COD)
  - Bakong KHQR (for QR code payment)
  - Other payment methods (future)

**UI Components**:
- PaymentMethodSelector component
- PaymentMethodCard component (with icon and description)
- Information tooltips for each method

---

### Phase 3: Order Status & Notifications (To Do)

#### Task F3.1: Order Status Display
- **Status**: To Do
- **Description**: Show order status with visual indicators
- **Status Flow**:
  ```
  PENDING → (Payment received) → CONFIRMED → (Processing) → SHIPPED → (Delivery) → DELIVERED
  
  Any status → (User/System initiated) → CANCELLED
  ```

**Status Colors**:
- PENDING: Yellow/Amber
- CONFIRMED: Blue
- SHIPPED: Purple
- DELIVERED: Green
- CANCELLED: Red

**UI Components**:
- StatusBadge component (with color coding)
- StatusTimeline component (visual progression)
- StatusHistory component (ordered list of events)

---

#### Task F3.2: Order Notifications
- **Status**: To Do
- **Description**: Notify user of order events
- **Notifications**:
  - Order created successfully
  - Payment received (for Bakong)
  - Order confirmed
  - Order shipped
  - Order delivered
  - Payment failed (retry option)

**Implementation**:
- Toast notifications for quick actions
- Email notifications for order milestones
- In-app notification center (optional)

---

#### Task F3.3: Order Cancellation UI
- **Status**: To Do
- **Description**: Allow users to cancel pending orders
- **Features**:
  - Only show cancel button for PENDING orders
  - Confirmation dialog before cancellation
  - Show cancellation policy
  - Display estimated refund timeline

**UI Components**:
- CancelOrderDialog component
- CancellationPolicy component
- RefundTimeline component

---

### Phase 4: Cart to Order Checkout (To Do)

#### Task F4.1: Checkout Page Integration
- **Status**: To Do
- **Description**: Integrate order creation into checkout flow
- **Checkout Steps**:
  1. **Review Items** - Show cart items summary
  2. **Shipping Address** - Select/add address
  3. **Payment Method** - Choose payment method
  4. **Confirm Order** - Review and confirm

**Navigation**:
- Next/Back buttons between steps
- Progress indicator
- Show summary on each step

---

#### Task F4.2: Address Selection During Checkout
- **Status**: To Do
- **Description**: Select shipping address in checkout
- **Features**:
  - List user's saved addresses
  - Mark default address
  - Add new address option
  - Edit address modal
  - Delete address option

**Validation**:
- At least one address must exist
- Address must belong to current user
- Valid address format

---

### Phase 5: Mobile Responsiveness (To Do)

#### Task F5.1: Mobile Order UI
- **Status**: To Do
- **Description**: Optimize order pages for mobile devices
- **Improvements**:
  - Stack layout for better readability
  - Touch-friendly buttons and interact elements
  - Optimized QR code size for mobile scanning
  - Responsive tables/lists

---

#### Task F5.2: Mobile Payment Flow
- **Status**: To Do
- **Description**: Optimize Bakong payment flow for mobile
- **Features**:
  - Direct deep link to Bakong app
  - Return to app after payment
  - Handle app switching concerns
  - Larger QR code for easier scanning

---

### Phase 6: Testing & Optimization (To Do)

#### Task F6.1: E2E Tests
- **Status**: To Do
- **Description**: End-to-end testing of order flow
- **Test Scenarios**:
  - Create order from cart → verify order created
  - Initiate Bakong payment → verify QR displayed
  - Mock payment success → verify order confirmed
  - Cancel pending order → verify status updated

---

---

## API CONTRACTS

### Order Request/Response Models

```typescript
// Create Order Request
interface CreateOrderRequest {
  addressId: number;
  paymentMethod: "BAKONG" | "COD";  // Cash on Delivery or Bakong
}

// Order Response
interface OrderResponse {
  id: number;
  orderNumber: string;  // ORD-XXXXXXXX
  userId: number;
  orderDate: string;    // ISO 8601
  status: "PENDING" | "CONFIRMED" | "SHIPPED" | "DELIVERED" | "CANCELLED";
  totalAmount: number;
  qrCode?: string;      // Only for Bakong
  paymentUrl?: string;  // Only for Bakong
  shippingAddress: AddressResponse;
  items: OrderItemResponse[];
  payment: PaymentResponse;
}

// Order Item Response
interface OrderItemResponse {
  id: number;
  productSku: ProductSkuResponse;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// Payment Response
interface PaymentResponse {
  id: number;
  paymentMethod: string;
  amount: number;
  status: "PENDING" | "COMPLETED" | "FAILED";
  transactionId?: string;
  paymentProvider?: string;
  paymentDate: string;
}

// Bakong Payment Response
interface BakongPaymentResponse {
  message: string;
  orderId: number;
  orderNumber: string;
  qrCode: string;
  paymentUrl: string;
  amount: number;
  expiresIn: string;
}

// Payment Verification Request
interface VerifyPaymentRequest {
  transactionId: string;
}

// Payment Callback Request
interface PaymentCallbackRequest {
  orderNumber: string;
  transactionId: string;
  status: "SUCCESS" | "FAILED" | "CANCELLED" | "PENDING";
}
```

---

## DATABASE SCHEMA

### Tables Created

#### order_details
```sql
┌─────────────────────────────────────────┐
│ order_details                           │
├─────────────────────────────────────────┤
│ id (PK, AUTO_INCREMENT)                 │
│ user_id (FK → users)                    │
│ order_number (UNIQUE)                   │
│ order_date (TIMESTAMP)                  │
│ status (ENUM)                           │
│ total_amount (DECIMAL)                  │
│ shipping_address_id (FK → addresses)    │
│ created_at (TIMESTAMP)                  │
│ updated_at (TIMESTAMP)                  │
└─────────────────────────────────────────┘
```

#### order_items
```sql
┌─────────────────────────────────────────┐
│ order_items                             │
├─────────────────────────────────────────┤
│ id (PK, AUTO_INCREMENT)                 │
│ order_id (FK → order_details)           │
│ product_sku_id (FK → product_sku)       │
│ quantity (INT)                          │
│ unit_price (DECIMAL)                    │
│ total_price (DECIMAL)                   │
└─────────────────────────────────────────┘
```

#### payments
```sql
┌─────────────────────────────────────────┐
│ payments                                │
├─────────────────────────────────────────┤
│ id (PK, AUTO_INCREMENT)                 │
│ order_id (FK → order_details, UNIQUE)   │
│ payment_method (VARCHAR)                │
│ amount (DECIMAL)                        │
│ status (ENUM)                           │
│ payment_date (TIMESTAMP)                │
│ transaction_id (VARCHAR)                │
│ payment_provider (VARCHAR)              │
│ payment_provider_response (LONGTEXT)    │
└─────────────────────────────────────────┘
```

---

## PAYMENT FLOW

### Bakong Payment Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                     BAKONG PAYMENT FLOW                      │
└─────────────────────────────────────────────────────────────┘

1. USER CREATES ORDER
   ⤵
   [Create Order with Bakong Payment]
   - Create order from cart
   - Generate KHQR code via Bakong API
   - Store QR code in payment record
   ⤵
   Frontend: Display QR code & Payment URL

2. USER SCANS QR CODE
   ⤵
   User opens Bakong app and scans QR code
   ⤵
   Bakong app processes payment (deducts from account)

3. BAKONG API NOTIFICATION
   ⤵
   Bakong sends callback to webhook:
   POST /api/v1/orders/bakong/callback
   {
     "orderNumber": "ORD-ABC123",
     "transactionId": "BAKONG-TXN123",
     "status": "SUCCESS"
   }

4. ORDER STATUS UPDATE
   ⤵
   [Process Callback]
   - Verify callback authenticity
   - Update order status → CONFIRMED
   - Update payment status → COMPLETED
   - Send confirmation to user

5. ORDER FULFILLMENT
   ⤵
   Admin processes order
   - Pick items
   - Pack order
   - Mark as SHIPPED
   - Arrange delivery

6. ORDER DELIVERY
   ⤵
   Mark as DELIVERED
   - Show in delivery status
   - Send delivery confirmation

OPTIONAL: POLLING FOR STATUS (Frontend)
   While user is on payment screen:
   - Poll GET /api/v1/orders/{id} every 5 seconds
   - Check if payment status changed to COMPLETED
   - Show success message and redirect
```

---

## TESTING CHECKLIST

### Backend Testing

#### Unit Tests
- [ ] OrderService.createOrderFromCart() - Valid input
- [ ] OrderService.createOrderFromCart() - Empty cart
- [ ] OrderService.createOrderFromCart() - Invalid address
- [ ] OrderService.cancelOrder() - Pending order
- [ ] OrderService.cancelOrder() - Non-pending order (should fail)
- [ ] BakongService.generateQR() - Valid merchant info
- [ ] BakongService.checkTransactionByMD5() - Valid MD5
- [ ] OrderService.createOrderWithBakongPayment() - Valid payment
- [ ] OrderService.verifyBakongPayment() - Success case
- [ ] OrderService.verifyBakongPayment() - Failure case
- [ ] OrderService.processBakongPaymentCallback() - Success status
- [ ] OrderService.processBakongPaymentCallback() - Failed status

#### Integration Tests
- [ ] Create order → Bakong QR generated → Order saved
- [ ] Initiate payment → Transaction ID set → QR stored
- [ ] Poll for payment status → Auto-update on callback
- [ ] Cancel order → Stock inventory restored
- [ ] Order with multiple items → All items in order

#### API Tests (Postman)
- [ ] POST /api/v1/orders/create-with-bakong → QR generated
- [ ] GET /api/v1/orders/{id} → Order details retrieved
- [ ] GET /api/v1/orders/number/{orderNumber} → Order found
- [ ] GET /api/v1/orders/my-orders → Paginated list returned
- [ ] PUT /api/v1/orders/{id}/cancel → Order cancelled
- [ ] POST /api/v1/orders/{id}/bakong/initiate → Payment initiated
- [ ] POST /api/v1/orders/bakong/callback → Status updated

### Frontend Testing

#### Unit Tests
- [ ] OrderList component - Renders with data
- [ ] OrderCard component - Shows correct status color
- [ ] BakongQRDisplay component - Shows QR image
- [ ] StatusBadge component - Correct color for status
- [ ] ExpirationTimer component - Counts down correctly

#### Integration Tests
- [ ] Navigate to Orders → List displays
- [ ] Click order → Detail page loads
- [ ] Create order → Checkout flow completes
- [ ] Select Bakong → QR code displayed
- [ ] QR display → Shows expiration timer
- [ ] Payment success → Order status updates

#### E2E Tests (Cypress/Playwright)
- [ ] User flow: Login → Add to cart → Checkout → Bakong QR → Success
- [ ] User cancels pending order → Status reflects change
- [ ] User refreshes order detail → Latest status shown
- [ ] Mobile: Bakong deep link works

---

## CURRENT ISSUES TO RESOLVE

### Backend Issues

1. **BakongService.generateQR() Return Type Mismatch**
   - Current: Returns `KHQRResponse<KHQRData>`
   - Expected: Should return `BakongResponse`
   - Impact: OrderService Bakong methods won't compile
   - **Fix Location**: `BakongServiceImpl.java` line 51
   - **Priority**: HIGH

2. **OrderServiceImpl Imports**
   - Still importing external `KHQRResponse` and `KHQRData`
   - Should only use custom `BakongResponse`
   - **Fix Location**: `OrderServiceImpl.java` imports
   - **Priority**: HIGH

3. **Response Property Access**
   - Methods checking `.getKHQRStatus()` which doesn't exist in BakongResponse
   - Should use `.getResponseMessage()` or `.getStatus()`
   - **Fix Location**: Multiple methods in OrderServiceImpl
   - **Priority**: HIGH

### What's Working ✓

- Order creation from cart
- Order repository and entities
- Payment entity and relationships
- Bakong token service
- Bakong DTO models
- Order status management
- Cart clearing after order

---

## NEXT STEPS

### Immediate Actions (This Sprint)

**Backend**:
1. Fix BakongService return type conversion
2. Update OrderServiceImpl to use correct response objects
3. Create OrderController with REST endpoints
4. Add comprehensive error handling
5. Write unit tests for payment integration

**Frontend**:
1. Create OrderList and OrderDetail components
2. Implement Bakong QR display component
3. Add payment status polling
4. Create checkout flow UI
5. Add responsive mobile design

### Future Enhancements

- Webhook signature verification for Bakong callbacks
- Retry mechanism for failed payments
- Order tracking with real-time notifications
- Support for multiple payment methods
- Order receipt PDF generation
- Refund processing UI
- Admin order management dashboard

---

## REFERENCES

### Bakong API Documentation
- **API Base URL**: https://api-bakong.nbc.gov.kh
- **Generate KHQR**: POST /v1/merchant/generate
- **Check Transaction**: POST /v1/check_transaction_by_md5
- **Token**: Bearer token required for all requests

### GitHub Repository
- https://github.com/tongbora/Bakong-API-Integration-with-Spring-Boot

### Postman Collection
- `Bakong_E_Shop.postman_collection.json` - Ready to import

---

## CONTACT & SUPPORT

For questions about specific tasks:
- Backend Tasks: Contact Backend Lead
- Frontend Tasks: Contact Frontend Lead
- Bakong Integration: Refer to BAKONG_POSTMAN_GUIDE.md

---

**Last Updated**: May 16, 2026
**Version**: 1.0
**Status**: Active Development

