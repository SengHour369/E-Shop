# Returns Module Task Breakdown

## Module
Returns Management

## Objective
Implement the **Returns Management** module for Admin to manage customer **Return / Refund / Exchange** requests, including:
- Returns dashboard summary
- Returns listing page
- Return request detail
- Approve / Reject return requests
- Return status tracking

---

# 1. Scope Overview

The Returns page allows Admin to:
- View return statistics summary
- View list of return/refund/exchange requests
- View request information:
    - Return ID
    - Order
    - Customer
    - Product
    - Type
    - Reason
    - Status
    - Amount
- Approve or Reject pending requests

---

# 2. Main Features

## 2.1 Dashboard Summary
Display summary cards:
- Total Returns
- Processed Returns
- Pending Returns
- Return Rate

## 2.2 Returns Listing
Display paginated return records with:
- Return ID
- Order No
- Customer Name
- Product Name
- Return Type
- Return Reason
- Status
- Amount
- Actions

## 2.3 Return Request Action
Allow admin to:
- Approve a requested return
- Reject a requested return

## 2.4 Return Detail
Allow admin to view full details of a return request.

---

# 3. Return Business Flow

## 3.1 Request Creation
A customer submits a request for one of:
- RETURN
- REFUND
- EXCHANGE

## 3.2 Initial Status
System creates return record with status:
- REQUESTED

## 3.3 Admin Review
Admin reviews:
- order information
- product information
- request type
- reason
- amount

## 3.4 Admin Decision
Admin can:
- APPROVE the request
- REJECT the request

## 3.5 Completion
After approved processing is completed, status may be updated to:
- COMPLETED

---

# 4. Return Status Definition

## Status List
- REQUESTED
- APPROVED
- REJECTED
- COMPLETED

## Status Rules
- New return request must start with `REQUESTED`
- Only `REQUESTED` record can be approved or rejected
- `APPROVED` record may later become `COMPLETED`
- `REJECTED` record is final unless future change is allowed by business

---

# 5. Return Type Definition

## Return Types
- RETURN
- REFUND
- EXCHANGE

---

# 6. Database Design Tasks

## 6.1 Create Return Request Table
Create table for storing return request information.

### Suggested table: `tbl_return_request`

Fields:
- id
- return_id
- order_id
- customer_id
- product_id
- return_type
- reason
- status
- amount
- requested_at
- requested_by
- approved_at
- approved_by
- rejected_at
- rejected_by
- completed_at
- remark
- created_at
- created_by
- updated_at
- updated_by

### Rules
- `return_id` must be unique
- `return_type` must support:
    - RETURN
    - REFUND
    - EXCHANGE
- `status` must support:
    - REQUESTED
    - APPROVED
    - REJECTED
    - COMPLETED

---

# 7. Backend Implementation Tasks

# 7.1 Entity Layer

## Task 7.1.1 Create ReturnRequest Entity
Create entity for `tbl_return_request`.

Fields:
- Long id
- String returnId
- Long orderId
- Long customerId
- Long productId
- String returnType
- String reason
- String status
- BigDecimal amount
- LocalDateTime requestedAt
- String requestedBy
- LocalDateTime approvedAt
- String approvedBy
- LocalDateTime rejectedAt
- String rejectedBy
- LocalDateTime completedAt
- String remark
- audit fields

---

# 7.2 DTO Layer

## Task 7.2.1 Create Return Summary Response DTO
Used for dashboard summary cards.

Fields:
- totalReturns
- processedReturns
- pendingReturns
- returnRate

---

## Task 7.2.2 Create Return List Request DTO
Used for search/filter listing.

Fields:
- page
- size
- returnId
- orderNo
- customerName
- productName
- returnType
- status
- fromDate
- toDate

---

## Task 7.2.3 Create Return List Response DTO
Fields:
- returnId
- orderNo
- customerName
- productName
- returnType
- reason
- status
- amount

---

## Task 7.2.4 Create Return Detail Response DTO
Fields:
- returnId
- orderNo
- customer info
- product info
- returnType
- reason
- status
- amount
- requestedAt
- requestedBy
- approvedAt
- approvedBy
- rejectedAt
- rejectedBy
- completedAt
- remark

---

## Task 7.2.5 Create Approve Return Request DTO
Fields:
- remark

---

## Task 7.2.6 Create Reject Return Request DTO
Fields:
- remark

---

# 7.3 Repository Layer

## Task 7.3.1 Create ReturnRequestRepository
Repository for return request data access.

### Required capabilities
- find by returnId
- search with filters
- count by status
- summary aggregation

---

## Task 7.3.2 Implement search query for return listing
Support filter by:
- returnId
- orderNo
- customerName
- productName
- returnType
- status
- date range

Support:
- pagination
- sorting by requestedAt desc

---

## Task 7.3.3 Implement summary query
Need to return:
- total returns
- processed returns
- pending returns
- return rate

### Definition suggestion
- total returns = count all return records
- processed returns = count status in (APPROVED, REJECTED, COMPLETED)
- pending returns = count status = REQUESTED
- return rate = total returns / total orders * 100
    - if total orders source exists in order module

---

# 7.4 Service Layer

## Task 7.4.1 Create ReturnQueryService
Responsibilities:
- get dashboard summary
- get paginated return listing
- get return detail by returnId

---

## Task 7.4.2 Create ReturnActionService
Responsibilities:
- approve return request
- reject return request
- validate current status before update

---

## Task 7.4.3 Implement getReturnSummary()
Logic:
- query total returns
- query processed returns
- query pending returns
- calculate return rate if order total exists

---

## Task 7.4.4 Implement getReturnList()
Logic:
- validate page and size
- apply search filters
- return paginated result

---

## Task 7.4.5 Implement getReturnDetail(returnId)
Logic:
- find return by returnId
- if not found return error
- map entity to detail response

---

## Task 7.4.6 Implement approveReturn(returnId, request)
Logic:
1. find return record by returnId
2. validate record exists
3. validate current status = REQUESTED
4. update status = APPROVED
5. set approvedAt
6. set approvedBy
7. save record

---

## Task 7.4.7 Implement rejectReturn(returnId, request)
Logic:
1. find return record by returnId
2. validate record exists
3. validate current status = REQUESTED
4. update status = REJECTED
5. set rejectedAt
6. set rejectedBy
7. save record

---

# 7.5 Controller Layer

## Task 7.5.1 Create ReturnController

Base path suggestion:
`/admin/returns`

---

## Task 7.5.2 Implement Get Return Summary API
### Endpoint
`GET /admin/returns/summary`

### Response
- totalReturns
- processedReturns
- pendingReturns
- returnRate

---

## Task 7.5.3 Implement Get Return List API
### Endpoint
`POST /admin/returns/list`

### Request
- page
- size
- filters

### Response
Paginated list of returns

---

## Task 7.5.4 Implement Get Return Detail API
### Endpoint
`GET /admin/returns/{returnId}`

### Response
Return detail response

---

## Task 7.5.5 Implement Approve Return API
### Endpoint
`POST /admin/returns/{returnId}/approve`

### Request
- remark

### Rules
- only REQUESTED record can be approved

---

## Task 7.5.6 Implement Reject Return API
### Endpoint
`POST /admin/returns/{returnId}/reject`

### Request
- remark

### Rules
- only REQUESTED record can be rejected

---

# 8. Validation Rules

## 8.1 Approve Validation
- returnId must exist
- status must be REQUESTED

## 8.2 Reject Validation
- returnId must exist
- status must be REQUESTED

## 8.3 Search Validation
- page must be >= 0 or 1 depending project standard
- size must be valid
- fromDate must not be greater than toDate

---

# 9. Error Handling Tasks

## Task 9.1 Return not found error
When returnId does not exist, return error:
- RETURN_NOT_FOUND

## Task 9.2 Invalid status transition error
When approve/reject is attempted on non-REQUESTED status, return error:
- INVALID_RETURN_STATUS

## Task 9.3 Invalid request validation error
For invalid search or action input, return validation error response

---

# 10. Audit / Logging Tasks

## Task 10.1 Add audit fields update
When approve/reject action occurs:
- update modified info
- store action timestamp
- store actor username/userId

## Task 10.2 Add action log
Log:
- returnId
- oldStatus
- newStatus
- actionBy
- actionTime

---

# 11. UI Mapping Reference

## Summary Cards
1. Total Returns
2. Processed Returns
3. Pending Returns
4. Return Rate

## Listing Columns
1. Return ID
2. Order
3. Customer
4. Product
5. Type
6. Reason
7. Status
8. Amount
9. Actions

## Action Buttons
- Approve
- Reject

---

# 12. Suggested API Response Example

## Return Summary Response
```json
{
  "totalReturns": 1204,
  "processedReturns": 1050,
  "pendingReturns": 154,
  "returnRate": 4.8
}