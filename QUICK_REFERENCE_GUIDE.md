# Order Management - Quick Reference Guide

## For Backend Developers

### Current Status of Implementation

```
✓ = Completed  |  🔄 = In Progress  |  ❌ = Not Started
```

| Feature | Status | File | Notes |
|---------|--------|------|-------|
| Order Entity & JPA | ✓ | `Model/OrderDetail.java` | With all relationships |
| Order Repository | ✓ | `Repository/OrderRepository.java` | Custom queries included |
| OrderService Interface | ✓ | `Service/ServiceStructure/OrderService.java` | 8 methods defined |
| OrderService Implementation | ✓ | `Service/ServiceImplement/OrderServiceImpl.java` | Base methods working |
| Bakong Token Service | ✓ | `Bakong/service/impl/service/BakongTokenService.java` | Token management |
| Bakong DTOs | ✓ | `Bakong/service/impl/dto/` | All request/response models |
| Bakong Service Interface | 🔄 | `Bakong/service/impl/service/BakongService.java` | Return type issue |
| Bakong Service Implementation | 🔄 | `Bakong/service/impl/service/impl/BakongServiceImpl.java` | QR generation logic |
| Order Payment Methods | 🔄 | `OrderServiceImpl` (lines 192-380) | Compilation errors |
| Order Controller Endpoints | ❌ | Not created yet | REST API endpoints |

### Key Files to Know

#### 1. Order Domain Models
```
src/main/java/com/example/learning_spring_security/Model/
  ├── OrderDetail.java (Order metadata)
  ├── OrderItem.java (Line items)
  ├── Payment.java (Payment info)
  └── User.java (Order owner)
```

#### 2. Bakong Integration
```
src/main/java/com/example/learning_spring_security/Bakong/
  ├── service/impl/dto/ (Request/Response models)
  ├── service/impl/service/ (Interfaces)
  │   ├── BakongService.java
  │   └── BakongTokenService.java
  └── service/impl/service/impl/ (Implementations)
      ├── BakongServiceImpl.java
      └── BakongTokenServiceImpl.java
```

#### 3. Order Service
```
src/main/java/com/example/learning_spring_security/Service/
  ├── ServiceStructure/
  │   └── OrderService.java (Interface - 8 methods)
  └── ServiceImplement/
      └── OrderServiceImpl.java (Implementation - 340+ lines)
```

### Implementing New Features

#### 1. Create Order Controller
```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Transactional
public class OrderController {
  
  private final OrderService orderService;
  private final UserService userService;
  
  @PostMapping("/create-from-cart")
  public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
    // Get authenticated user ID from JWT token
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(orderService.createOrderFromCart(userId, request));
  }
  
  @PostMapping("/create-with-bakong")
  public ResponseEntity<?> createOrderWithBakong(@RequestBody OrderRequest request) {
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(orderService.createOrderWithBakongPayment(userId, request));
  }
  
  // ... more endpoints
}
```

#### 2. Fix Bakong Service Return Type
Current issue: BakongService.generateQR() returning KHQRResponse instead of BakongResponse

**Solution**:
```java
@Override
public BakongResponse generateQR(BakongRequest bakongRequest) {
  // ... existing code ...
  
  var khqrResponse = BakongKHQR.generateMerchant(merchantInfo);
  
  // Convert to BakongResponse
  return BakongResponse.builder()
    .responseCode(khqrResponse.getKHQRStatus() != null ? 200 : 400)
    .responseMessage("SUCCESS") // or "FAILED"
    .qrCode(khqrResponse.getData() != null ? 
            khqrResponse.getData().getQr() : null)
    .status("SUCCESS") // or appropriate status
    .build();
}
```

#### 3. Add Error Handling
```java
// Create custom exceptions
public class OrderException extends RuntimeException {
  public OrderException(String message) {
    super(message);
  }
}

public class BakongPaymentException extends RuntimeException {
  public BakongPaymentException(String message, Throwable cause) {
    super(message, cause);
  }
}

// Use in service
try {
  // Payment logic
} catch (Exception e) {
  throw new BakongPaymentException("Payment processing failed", e);
}
```

### Testing the Payment Flow

#### Step 1: Create Order with Bakong
```bash
POST http://localhost:8080/api/v1/orders/create-with-bakong
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "addressId": 1,
  "paymentMethod": "BAKONG"
}

# Response:
{
  "id": 1,
  "orderNumber": "ORD-ABC12345",
  "totalAmount": 50000,
  "qrCode": "00020101021229...",
  "paymentUrl": "bakong://payment?qr=00020101021229..."
}
```

#### Step 2: Verify Payment
```bash
POST http://localhost:8080/api/v1/orders/1/bakong/verify
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "transactionId": "BAKONG-ORD-ABC12345"
}
```

#### Step 3: Process Callback
```bash
POST http://localhost:8080/api/v1/orders/bakong/callback
Content-Type: application/json

{
  "orderNumber": "ORD-ABC12345",
  "transactionId": "BAKONG-TXN-12345",
  "status": "SUCCESS"
}
```

---

## For Frontend Developers

### UI Component Structure

```
src/components/
├── Orders/
│   ├── OrderList.tsx (Paginated list)
│   ├── OrderCard.tsx (Individual order card)
│   ├── OrderDetail.tsx (Full order view)
│   └── OrderStatusBadge.tsx (Status display)
├── Payment/
│   ├── BakongQRDisplay.tsx (QR code display)
│   ├── PaymentMethodSelector.tsx (Payment method choice)
│   ├── ExpirationTimer.tsx (QR expiration timer)
│   └── PaymentStatus.tsx (Status polling)
├── Checkout/
│   ├── CheckoutStepper.tsx (Multi-step flow)
│   ├── CartReview.tsx (Cart summary)
│   ├── AddressSelector.tsx (Address selection)
│   └── OrderConfirmation.tsx (Final confirmation)
└── Shared/
    ├── StatusTimeline.tsx (Order timeline)
    └── LoadingSpinner.tsx (Loading indicators)
```

### API Hooks for React

```typescript
// hooks/useOrder.ts
import { useQuery, useMutation } from '@tanstack/react-query';

export const useGetOrders = (page: number, size: number) => {
  return useQuery({
    queryKey: ['orders', page, size],
    queryFn: async () => {
      const res = await fetch(
        `/api/v1/orders/my-orders?page=${page}&size=${size}`,
        { headers: { Authorization: `Bearer ${getToken()}` } }
      );
      return res.json();
    },
  });
};

export const useGetOrder = (orderId: number) => {
  return useQuery({
    queryKey: ['order', orderId],
    queryFn: async () => {
      const res = await fetch(`/api/v1/orders/${orderId}`, {
        headers: { Authorization: `Bearer ${getToken()}` }
      });
      return res.json();
    },
  });
};

export const useCreateOrderWithBakong = () => {
  return useMutation({
    mutationFn: async (request: { addressId: number }) => {
      const res = await fetch('/api/v1/orders/create-with-bakong', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getToken()}`
        },
        body: JSON.stringify(request),
      });
      return res.json();
    },
  });
};

export const useVerifyPayment = () => {
  return useMutation({
    mutationFn: async (data: { orderId: number; transactionId: string }) => {
      const res = await fetch(`/api/v1/orders/${data.orderId}/bakong/verify`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getToken()}`
        },
        body: JSON.stringify({ transactionId: data.transactionId }),
      });
      return res.json();
    },
  });
};
```

### Component Examples

#### OrderList Component
```typescript
export const OrderList: React.FC = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useGetOrders(page, 10);

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">My Orders</h1>
      <div className="space-y-3">
        {data?.content?.map((order) => (
          <OrderCard key={order.id} order={order} />
        ))}
      </div>
      <Pagination 
        currentPage={page} 
        totalPages={data?.totalPages}
        onPageChange={setPage}
      />
    </div>
  );
};
```

#### BakongQRDisplay Component
```typescript
interface BakongQRDisplayProps {
  orderId: number;
  qrCode: string;
  amount: number;
  expiresIn: number; // seconds
}

export const BakongQRDisplay: React.FC<BakongQRDisplayProps> = ({
  orderId,
  qrCode,
  amount,
  expiresIn,
}) => {
  const [timeLeft, setTimeLeft] = useState(expiresIn);
  const [paymentVerified, setPaymentVerified] = useState(false);
  const verifyPayment = useVerifyPayment();

  // Poll for payment status
  useEffect(() => {
    const interval = setInterval(async () => {
      const res = await fetch(`/api/v1/orders/${orderId}`, {
        headers: { Authorization: `Bearer ${getToken()}` }
      });
      const order = await res.json();
      
      if (order.payment?.status === 'COMPLETED') {
        setPaymentVerified(true);
        clearInterval(interval);
      }
    }, 5000);

    return () => clearInterval(interval);
  }, [orderId]);

  // Countdown timer
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((t) => {
        if (t <= 1) {
          clearInterval(timer);
          return 0;
        }
        return t - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  if (paymentVerified) {
    return (
      <div className="p-4 bg-green-100 text-green-800 rounded">
        ✓ Payment received! Your order is confirmed.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2>Complete Your Payment</h2>
      
      <div className="p-4 border rounded">
        <img 
          src={`data:image/png;base64,${qrCode}`}
          alt="QR Code"
          className="w-64 h-64"
        />
      </div>

      <div className="text-center">
        <p className="text-lg font-bold">Amount: {amount.toLocaleString()} KHR</p>
        <ExpirationTimer timeLeft={timeLeft} />
      </div>

      <div className="flex gap-2">
        <button 
          className="flex-1 bg-blue-500 text-white p-2 rounded"
          onClick={() => {
            // Open Bakong app with deep link
            window.location.href = `bakong://payment?qr=${qrCode}`;
          }}
        >
          Open Bakong App
        </button>
        <button 
          className="flex-1 bg-gray-500 text-white p-2 rounded"
          onClick={() => {
            // Copy QR code to clipboard
            navigator.clipboard.writeText(qrCode);
          }}
        >
          Copy QR Code
        </button>
      </div>
    </div>
  );
};
```

### Payment Status Polling

```typescript
export const usePollingPayment = (
  orderId: number,
  enabled: boolean = true
) => {
  return useQuery({
    queryKey: ['order-payment', orderId],
    queryFn: async () => {
      const res = await fetch(`/api/v1/orders/${orderId}`, {
        headers: { Authorization: `Bearer ${getToken()}` }
      });
      return res.json();
    },
    refetchInterval: 5000, // Poll every 5 seconds
    refetchOnWindowFocus: true,
    enabled,
    staleTime: 0,
  });
};
```

### Form Validation Example

```typescript
import { z } from 'zod';

const OrderSchema = z.object({
  addressId: z.number().min(1, 'Select a delivery address'),
  paymentMethod: z.enum(['BAKONG', 'COD'], {
    errorMap: () => ({ message: 'Select a valid payment method' })
  }),
});

type OrderFormData = z.infer<typeof OrderSchema>;

export const CheckoutForm: React.FC = () => {
  const form = useForm<OrderFormData>({
    resolver: zodResolver(OrderSchema),
  });

  const onSubmit = async (data: OrderFormData) => {
    try {
      const response = await fetch('/api/v1/orders/create-with-bakong', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getToken()}`
        },
        body: JSON.stringify(data),
      });
      
      if (!response.ok) throw new Error('Order creation failed');
      
      const order = await response.json();
      // Redirect to payment page
      navigate(`/orders/${order.id}/payment`);
    } catch (error) {
      form.setError('root', { message: error.message });
    }
  };

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
      {/* Form fields */}
      <button type="submit">Create Order</button>
    </form>
  );
};
```

---

## Debugging Tips

### Backend

1. **Enable Bakong Debug Logging**
   ```properties
   logging.level.com.example.learning_spring_security.Bakong=DEBUG
   ```

2. **Check Payment Status in Database**
   ```sql
   SELECT o.id, o.order_number, o.status, p.transaction_id, p.status 
   FROM order_details o 
   JOIN payments p ON o.id = p.order_id 
   WHERE o.id = ?;
   ```

3. **View Live Logs**
   ```bash
   tail -f logs/application.log | grep -i bakong
   ```

### Frontend

1. **Check Network Requests**
   - Open DevTools → Network
   - Filter by `/api/v1/orders`
   - Check request/response bodies

2. **Console Debugging**
   ```javascript
   // In component
   console.log('Order:', order);
   console.log('Payment Status:', payment?.status);
   console.log('QR Code:', qrCode);
   ```

3. **React DevTools**
   - Inspect component state
   - Check props being passed
   - Track re-renders

---

## Common Issues & Solutions

### Backend

| Issue | Cause | Solution |
|-------|-------|----------|
| "Bean not found" error | Service not annotated with @Service | Add @Service annotation |
| MockMvc returns null | Endpoint not mapped | Create controller with @PostMapping |
| QR code is null | Bakong API failure | Check credentials in application.properties |
| Transaction verification fails | Invalid MD5 | Ensure correct transaction ID format |

### Frontend

| Issue | Cause | Solution |
|-------|-------|----------|
| QR code not rendering | Image data format wrong | Use base64 encoded PNG |
| Polling not updating | Stale time too high | Set staleTime: 0 |
| Deep link not working | Bakong app not installed | Show fallback QR code option |
| Authorization error | Token expired | Implement token refresh |

---

## Environment Setup

### Backend Configuration
```properties
# application.properties
bakong.account-id=senghour_soeurng@bkrt
bakong.base-url=https://api-bakong.nbc.gov.kh
bakong.email=seanghour097328@gmail.com

# Logging
logging.level.root=INFO
logging.level.com.example.learning_spring_security=DEBUG
```

### Frontend Environment
```bash
# .env
REACT_APP_API_URL=http://localhost:8080/api/v1
REACT_APP_IMAGE_URL=http://localhost:8080
```

---

## Performance Considerations

### Backend
- Add database indexes on frequently queried fields (order_number, user_id, status)
- Cache Bakong token to reduce API calls
- Implement pagination for order lists (default 10 items/page)

### Frontend
- Lazy load order components
- Implement virtual scrolling for large order lists
- Cache order data with React Query

---

## Resources

- [Bakong KHQR Documentation](https://bakong.nbc.gov.kh)
- [Spring Boot Guide](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [GitHub Project Repository](https://github.com/tongbora/Bakong-API-Integration-with-Spring-Boot)

---

**Last Updated**: May 16, 2026  
**For Questions**: Refer to TASKS_ORDER_MANAGEMENT.md for detailed task breakdowns

