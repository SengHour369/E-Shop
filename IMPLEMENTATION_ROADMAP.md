# Implementation Roadmap - Order Management with Bakong Payment

## Executive Summary

The E-Shop application has a solid foundation for order management and Bakong payment integration. The core entities, repositories, and service interfaces are in place. However, there are compilation issues in the `OrderServiceImpl` that need to be resolved before the payment integration can function properly.

**Current Status**: 70% Complete (Backend) | 0% Complete (Frontend)

---

## Phase Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROJECT PHASES                               │
├─────────────────────────────────────────────────────────────────┤
│ Phase 1: Core Order Management        ✓ COMPLETED              │
│ Phase 2: Bakong Integration           🔄 90% COMPLETE          │
│ Phase 3: Order Payment Methods        🔄 70% COMPLETE          │
│ Phase 4: REST API Controller          ❌ NOT STARTED           │
│ Phase 5: Frontend Components          ❌ NOT STARTED           │
│ Phase 6: Testing & Deployment         ❌ NOT STARTED           │
└─────────────────────────────────────────────────────────────────┘
```

---

## CRITICAL ISSUES TO RESOLVE

### Issue #1: BakongService Return Type Mismatch
**Severity**: 🔴 CRITICAL (Blocks compilation)

**Problem**:
```java
// File: BakongService.java (Line 13)
// Current (external library type):
KHQRResponse<KHQRData> generateQR(BakongRequest request);

// Expected (custom DTO):
BakongResponse generateQR(BakongRequest request);
```

**Impact**: 
- OrderServiceImpl cannot compile
- All payment flow methods blocked
- Bakong payment feature unavailable

**Solution**:
1. Update interface return type to `BakongResponse`
2. Modify `BakongServiceImpl.generateQR()` to convert `KHQRResponse` to `BakongResponse`
3. Update all callers to use correct response object

**Status**: Ready to fix - just need to apply changes

---

### Issue #2: OrderServiceImpl Compilation Errors
**Severity**: 🔴 CRITICAL (8 errors)

**Problems**:
```
Error 1: Line 213 - Incompatible type assignment
  KHQRResponse<KHQRData> != BakongResponse

Error 2: Line 215 - Accessing wrong method
  bakongResponse.getResponseMessage() should return String

Error 3-4: Duplicate in initiateBakongPayment()
  Same type mismatch issues

Error 5: Line 223 - getKHQRStatus() from wrong type
Error 6: Line 269 - getKHQRStatus() from wrong type
Error 7: Line 277 - getKHQRStatus() from wrong type
Error 8: Line 298 - Unnecessary toString() call
```

**Solution Steps**:
1. Fix imports (remove KHQRResponse, KHQRData imports)
2. Update createOrderWithBakongPayment() method
3. Update initiateBakongPayment() method
4. Update verifyBakongPayment() method
5. Update processBakongPaymentCallback() method
6. Run unit tests to verify

**Estimated Time**: 1-2 hours

---

### Issue #3: Unused Service Dependencies
**Severity**: 🟡 MEDIUM (Warnings)

**Problems**:
- `CartItemRepository` not used (can remove)
- `PaymentRepository` not used (can remove)
- `BakongTokenService` not used in OrderServiceImpl (can remove)

**Solution**: Clean up imports and remove unused fields

---

## Detailed Implementation Steps

### Step 1: Fix BakongService Interface (5 mins)

```java
// File: src/main/java/com/example/learning_spring_security/Bakong/service/impl/service/BakongService.java

// BEFORE:
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongService {
    KHQRResponse<KHQRData> generateQR(BakongRequest request);
}

// AFTER:
public interface BakongService {
    BakongResponse generateQR(BakongRequest request);
}
```

---

### Step 2: Fix BakongServiceImpl.generateQR() (15 mins)

```java
// File: src/main/java/com/example/learning_spring_security/Bakong/service/impl/service/impl/BakongServiceImpl.java

@Override
public BakongResponse generateQR(BakongRequest bakongRequest) {
    MerchantInfo merchantInfo = new MerchantInfo();
    
    // ... set all merchant info properties ...
    
    // Generate KHQR using Bakong library
    var khqrResponse = BakongKHQR.generateMerchant(merchantInfo);
    
    // Convert KHQR response to BakongResponse
    BakongResponse response = BakongResponse.builder()
            .responseCode(khqrResponse.getKHQRStatus() != null ? 200 : 400)
            .responseMessage(khqrResponse.getKHQRStatus() != null ? "SUCCESS" : "FAILED")
            .status(khqrResponse.getKHQRStatus() != null ? "SUCCESS" : "FAILED")
            .message(khqrResponse.getKHQRStatus() != null 
                    ? "QR Code generated successfully" 
                    : "Failed to generate QR Code")
            .qrCode(khqrResponse.getData() != null ? khqrResponse.getData().getQr() : null)
            .data(khqrResponse.getData())
            .build();
    
    return response;
}
```

**Key Points**:
- Extract QR string from `khqrResponse.getData().getQr()`
- Map status from KHQR enum to String
- Return custom BakongResponse object

---

### Step 3: Fix OrderServiceImpl Imports (3 mins)

```java
// File: src/main/java/com/example/learning_spring_security/Service/ServiceImplement/OrderServiceImpl.java

// REMOVE these imports:
// import kh.gov.nbc.bakong_khqr.model.KHQRData;
// import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

// ADD this import:
import com.example.learning_spring_security.Bakong.service.impl.dto.CheckTransactionRequest;

// REMOVE unused field:
// private final CartItemRepository cartItemRepository;
// private final PaymentRepository paymentRepository;
// private final BakongTokenService bakongTokenService;

// KEEP ONLY:
private final BakongService bakongService;
```

---

### Step 4: Fix createOrderWithBakongPayment() (10 mins)

```java
@Override
public ResponseErrorTemplate createOrderWithBakongPayment(Long userId, OrderRequest request) {
    if (!"BAKONG".equalsIgnoreCase(request.getPaymentMethod())) {
        throw new BadRequestException("This method is only for Bakong payments");
    }

    ResponseErrorTemplate orderResponse = createOrderFromCart(userId, request);
    OrderResponse orderData = (OrderResponse) orderResponse.object();

    try {
        BakongRequest bakongRequest = BakongRequest.builder()
                .currency("KHR")
                .amount(orderData.getTotalAmount().doubleValue())
                .merchantName("E_Shop")
                .merchantCity("PHNOM PENH")
                .merchantId("ESHOP001")
                .acquiringBank("NBC")
                .billNumber(orderData.getOrderNumber())
                .storeLabel("E_SHOP_STORE")
                .terminalLabel("TERMINAL_01")
                .mobileNumber("012345678")
                .purposeOfTransaction("Payment for Order " + orderData.getOrderNumber())
                .expirationTimestamp(15)
                .build();

        // NOW RETURNS BakongResponse (not KHQRResponse)
        BakongResponse bakongResponse = bakongService.generateQR(bakongRequest);

        if ("SUCCESS".equals(bakongResponse.getStatus())) {
            OrderDetail order = orderRepository.findByOrderNumber(orderData.getOrderNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found after creation"));

            Payment payment = order.getPayment();
            payment.setTransactionId("BAKONG-" + orderData.getOrderNumber());
            payment.setPaymentProvider("BAKONG");
            payment.setPaymentProviderResponse(bakongResponse.getQrCode());

            orderRepository.save(order);

            orderData.setQrCode(bakongResponse.getQrCode());
            orderData.setPaymentUrl("bakong://payment?qr=" + bakongResponse.getQrCode());
        }
    } catch (Exception e) {
        System.err.println("Failed to generate Bakong QR: " + e.getMessage());
    }

    return orderResponse;
}
```

---

### Step 5: Fix initiateBakongPayment() (10 mins)

Similar to Step 4, update to use `BakongResponse` instead of `KHQRResponse<KHQRData>`:

```java
@Override
public ResponseErrorTemplate initiateBakongPayment(Long orderId) {
    // ... validation code ...

    try {
        BakongRequest bakongRequest = /* ... */;

        // Use BakongResponse
        BakongResponse bakongResponse = bakongService.generateQR(bakongRequest);

        if ("SUCCESS".equals(bakongResponse.getStatus())) {
            // ... update payment ...
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to initiate Bakong payment: " + e.getMessage(), e);
    }
}
```

---

### Step 6: Fix verifyBakongPayment() (5 mins)

```java
@Override
public ResponseErrorTemplate verifyBakongPayment(Long orderId, String transactionId) {
    // ... validation code ...

    try {
        String md5 = transactionId.contains("-") ?
                transactionId.split("-")[1] : transactionId;

        // Use new import
        BakongResponse bakongResponse = bakongService.checkTransactionByMD5(
                new CheckTransactionRequest(md5)
        );

        if ("SUCCESS".equals(bakongResponse.getStatus())) {
            // ... update order and payment ...
        }
    } catch (Exception e) {
        // ... error handling ...
    }
}
```

---

### Step 7: Fix processBakongPaymentCallback() (5 mins)

```java
@Override
public ResponseErrorTemplate processBakongPaymentCallback(String orderNumber, String transactionId, String status) {
    OrderDetail order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    if (!"BAKONG".equalsIgnoreCase(order.getPayment().getPaymentMethod())) {
        throw new BadRequestException("Order does not use Bakong payment method");
    }

    Payment payment = order.getPayment();
    payment.setTransactionId(transactionId);

    switch (status.toUpperCase()) {
        case "SUCCESS":
        case "COMPLETED":
            order.setStatus("CONFIRMED");
            payment.setStatus("COMPLETED");
            break;
        case "FAILED":
        case "CANCELLED":
            order.setStatus("CANCELLED");
            payment.setStatus("FAILED");
            order.getOrderItems().forEach(item ->
                    productSkuRepository.increaseStock(item.getProductSku().getId(), item.getQuantity())
            );
            break;
        default:
            order.setStatus("PENDING");
            payment.setStatus("PENDING");
    }

    orderRepository.save(order);

    return ResponseErrorTemplate.builder()
            .message("Payment callback processed successfully")
            .object(Map.of(
                    "orderNumber", orderNumber,
                    "transactionId", transactionId,
                    "orderStatus", order.getStatus(),
                    "paymentStatus", payment.getStatus()
            ))
            .build();
}
```

---

## Next: Create REST Controller

### Step 8: Create OrderController

```java
// File: src/main/java/com/example/learning_spring_security/controller/OrderController.java

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    private Long getCurrentUserId() {
        // Extract from JWT token
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserDetails) auth.getPrincipal()).getId();
    }

    // Create order from cart
    @PostMapping("/create-from-cart")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(orderService.createOrderFromCart(userId, request));
    }

    // Create order with Bakong payment
    @PostMapping("/create-with-bakong")
    public ResponseEntity<?> createOrderWithBakong(@Valid @RequestBody OrderRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(orderService.createOrderWithBakongPayment(userId, request));
    }

    // Get order by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // Get order by number
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    // Get user's orders (paginated)
    @GetMapping("/my-orders")
    public ResponseEntity<?> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
    }

    // Cancel order
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }

    // Initiate Bakong payment
    @PostMapping("/{id}/bakong/initiate")
    public ResponseEntity<?> initiateBakongPayment(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.initiateBakongPayment(id));
    }

    // Verify Bakong payment
    @PostMapping("/{id}/bakong/verify")
    public ResponseEntity<?> verifyBakongPayment(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, String> request) {
        String transactionId = request.get("transactionId");
        return ResponseEntity.ok(orderService.verifyBakongPayment(id, transactionId));
    }

    // Bakong payment callback
    @PostMapping("/bakong/callback")
    public ResponseEntity<?> processBakongCallback(@Valid @RequestBody Map<String, String> request) {
        String orderNumber = request.get("orderNumber");
        String transactionId = request.get("transactionId");
        String status = request.get("status");
        
        return ResponseEntity.ok(orderService.processBakongPaymentCallback(orderNumber, transactionId, status));
    }

    // Admin: Get all orders
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    // Admin: Update order status
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}
```

---

## Frontend Implementation Plan

### Phase 2.1: Core Components (Week 2)

1. **OrderList Component** - Display user's orders
2. **OrderCard Component** - Individual order preview
3. **OrderDetail Component** - Full order information
4. **StatusBadge Component** - Order status display

### Phase 2.2: Payment Components (Week 3)

1. **BakongQRDisplay Component** - Show QR code and countdown
2. **PaymentMethodSelector Component** - Choose payment method
3. **ExpirationTimer Component** - Countdown to expiration
4. **PaymentStatus Component** - Real-time status polling

### Phase 2.3: Checkout Flow (Week 3-4)

1. **CheckoutStepper Component** - Multi-step process
2. **CartReview Component** - Items summary
3. **AddressSelector Component** - Delivery address
4. **OrderConfirmation Component** - Final confirmation

---

## Testing Strategy

### Backend Unit Tests

```java
@SpringBootTest
public class OrderServiceTest {
    
    @Test
    public void testCreateOrderWithBakong() {
        // Arrange
        Long userId = 1L;
        OrderRequest request = new OrderRequest(1L, "BAKONG");
        
        // Act
        ResponseErrorTemplate response = orderService.createOrderWithBakongPayment(userId, request);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.object() instanceof OrderResponse);
        OrderResponse order = (OrderResponse) response.object();
        assertNotNull(order.getQrCode());
    }
    
    @Test
    public void testVerifyBakongPayment() {
        // Arrange
        Long orderId = 1L;
        String transactionId = "BAKONG-TXN123";
        
        // Act
        ResponseErrorTemplate response = orderService.verifyBakongPayment(orderId, transactionId);
        
        // Assert
        assertNotNull(response);
        Map<String, Object> data = (Map) response.object();
        assertEquals("CONFIRMED", data.get("status"));
    }
}
```

### Integration Tests

```java
@SpringBootTest
public class BakongPaymentIntegrationTest {
    
    @Test
    @Transactional
    public void testCompletePaymentFlow() {
        // 1. Create order with Bakong
        // 2. Verify QR code generated
        // 3. Simulate payment callback
        // 4. Verify order status updated to CONFIRMED
    }
}
```

### API Tests (Postman)

```json
[
  {
    "name": "Create Order with Bakong",
    "method": "POST",
    "url": "http://localhost:8080/api/v1/orders/create-with-bakong",
    "body": { "addressId": 1 }
  },
  {
    "name": "Verify Payment",
    "method": "POST",
    "url": "http://localhost:8080/api/v1/orders/1/bakong/verify",
    "body": { "transactionId": "BAKONG-ORD123" }
  },
  {
    "name": "Process Callback",
    "method": "POST",
    "url": "http://localhost:8080/api/v1/orders/bakong/callback",
    "body": {
      "orderNumber": "ORD-ABC123",
      "transactionId": "BAKONG-TXN123",
      "status": "SUCCESS"
    }
  }
]
```

---

## Timeline

```
Week 1 (Current):
├─ Day 1-2: Fix all compilation errors ✓ (2-3 hours)
├─ Day 3-4: Create OrderController (4 hours)
└─ Day 5: Unit tests for order service (4 hours)

Week 2:
├─ Day 1-2: Create core order UI components (8 hours)
├─ Day 3-4: Create payment UI components (8 hours)
└─ Day 5: Integration testing (4 hours)

Week 3:
├─ Day 1-3: Checkout flow implementation (12 hours)
├─ Day 4: Mobile optimization (4 hours)
└─ Day 5: E2E testing (4 hours)

Week 4:
├─ Day 1-2: Bug fixes & refinement (8 hours)
├─ Day 3: Documentation updates (4 hours)
├─ Day 4: Performance optimization (4 hours)
└─ Day 5: Deployment prep (4 hours)
```

---

## Success Criteria

- [ ] All compilation errors resolved
- [ ] All 12 order service methods working
- [ ] All REST endpoints tested and working
- [ ] Bakong QR code generated successfully
- [ ] Payment status updates after callback
- [ ] Order cancellation restores inventory
- [ ] Frontend displays all order information
- [ ] Payment flow works end-to-end
- [ ] All unit tests passing (>80% code coverage)
- [ ] API response times < 500ms
- [ ] Zero security vulnerabilities

---

## Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|-----------|
| Bakong API rate limiting | Payment processing blocked | Low | Implement exponential backoff |
| Network timeout during payment | Transaction incomplete | Medium | Add retry mechanism + polling |
| QR code expiration | User can't pay | Low | Display countdown timer |
| Database transaction failure | Order partial state | Low | Use @Transactional rollback |
| Frontend app switching issues | User confusion | Medium | Phone app deep link handling |

---

## Estimated Effort

- **Backend Implementation**: 24 hours
  - Compilation fixes: 2-3 hours
  - Controller creation: 4 hours
  - Unit tests: 4 hours
  - Integration tests: 8 hours
  - Documentation: 2 hours

- **Frontend Implementation**: 40 hours
  - Components: 24 hours
  - Integration: 8 hours
  - Testing: 8 hours

- **Total**: ~64 hours (~10 business days)

---

## Conclusion

The E-Shop order management system is nearly complete at the backend level. With focused effort on resolving the 3 critical compilation issues, the Bakong payment integration will be fully functional within 1-2 days. Frontend implementation can then proceed in parallel over the following 2-3 weeks.

**Recommendation**: Start with the 7 backend fix steps immediately to unblock further development.

---

**Document Version**: 1.0  
**Last Updated**: May 16, 2026  
**Next Review**: After Phase 2 completion

