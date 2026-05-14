# Bakong E_Shop Postman Collection Guide

## Overview
This Postman collection provides a complete testing suite for the Bakong Payment Integration in the E_Shop application.

## Import Instructions

### Step 1: Import the Collection
1. Open Postman
2. Click **Import** (top-left)
3. Select **Upload Files**
4. Choose `Bakong_E_Shop.postman_collection.json`
5. Click **Import**

### Step 2: Set Environment Variables
Before running requests, set these variables in your Postman environment:

```
base_url: http://localhost:8083
token: (empty - will be filled after login)
bakong_account_id: senghour_soeurng@bkrt
bakong_merchant_id: ESHOP001
```

## Collection Structure

### 1. **Authentication** 
  - **Login with Email** - Login using email credential
  - **Login with Username** - Login using username credential
  
⚠️ **IMPORTANT**: Run one of these first to get your JWT token, which will be automatically saved to the `token` variable.

### 2. **Bakong QR Code Generation**
Generate QR codes for payments:

- **Generate QR Code** - Create KHQR string with basic information
- **Get QR Code Image** - Convert KHQR string to PNG image
- **Generate QR - Full Amount** - QR with complete details and Khmer language
- **Generate QR - Minimal** - QR with only required fields

### 3. **Bakong Transaction Management**
Verify and track payments:

- **Check Transaction Status** - Verify payment using MD5 hash
- **Check Transaction - Example 1** - Example with sample MD5

### 4. **Payment Integration Workflows**
Complete payment scenarios:

1. **Complete Payment Flow - Initialize** - Step 1: Generate QR
2. **Complete Payment Flow - Convert to Image** - Step 2: Create PNG image
3. **Complete Payment Flow - Verify Payment** - Step 3: Confirm payment

### 5. **Products**
Product management:

- **Get All Products** - Retrieve all products with pagination
- **Get Active Products** - Get only active products

### 6. **Orders**
Order management:

- **Create Order with Bakong Payment** - Create order with Bakong payment method
- **Get Order by ID** - Retrieve specific order

## Quick Start Workflow

### Scenario: Complete Bakong Payment

**Step 1: Login**
```
Run → Authentication → Login with Email
Response will contain accessToken
```

**Step 2: Generate QR Code**
```
Run → Bakong QR Code Generation → Generate QR Code
Update request body with your merchant details
Save the "qrCode" value from response
```

**Step 3: Convert to Image**
```
Run → Bakong QR Code Generation → Get QR Code Image
Paste the "qrCode" from previous response
Will return PNG image bytes
```

**Step 4: Verify Payment**
```
Run → Bakong Transaction Management → Check Transaction Status
Use MD5 hash value to verify payment completion
```

## Request Examples

### Generate QR Code
**Endpoint**: `POST /api/v1/bakong/generate-qr`

**Request Body**:
```json
{
  "currency": "KHR",
  "amount": 100000,
  "merchantName": "E_Shop",
  "merchantCity": "PHNOM PENH",
  "merchantId": "ESHOP001",
  "acquiringBank": "NBC",
  "billNumber": "BILL00001",
  "storeLabel": "E_SHOP_STORE",
  "terminalLabel": "TERMINAL_01",
  "mobileNumber": "012345678",
  "purposeOfTransaction": "Payment for Order",
  "expirationTimestamp": 15
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "message": "QR code generated successfully",
  "qrCode": "00020101021229370016A0000000727302150senghour_soeurng@bkrt0215ESHOP00105303KHR5900000100001"
}
```

### Get QR Code Image
**Endpoint**: `POST /api/v1/bakong/get-qr-image`

**Request Body**:
```json
{
  "qr": "00020101021229370016A0000000727302150senghour_soeurng@bkrt0215ESHOP00105303KHR5900000100001",
  "md5": "transaction_id_hash"
}
```

**Response**: Binary PNG image

### Check Transaction Status
**Endpoint**: `POST /api/v1/bakong/check-transaction`

**Request Body**:
```json
{
  "md5": "transaction_md5_hash_value"
}
```

**Response**:
```json
{
  "status": "SUCCESS",
  "message": "Transaction verified",
  "data": {
    "transactionId": "TXN-123456",
    "status": "COMPLETED",
    "amount": 100000
  }
}
```

## Field Descriptions

### Generate QR Request Fields

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| amount | Yes | Double | Payment amount in KHR |
| currency | No | String | Currency (default: KHR) |
| merchantId | No | String | Merchant identifier |
| merchantName | No | String | Merchant name |
| merchantCity | No | String | Merchant city (default: PHNOM PENH) |
| acquiringBank | No | String | Acquiring bank (default: NBC) |
| billNumber | No | String | Invoice/Bill number |
| storeLabel | No | String | Store name/label |
| terminalLabel | No | String | Terminal identifier |
| mobileNumber | No | String | Contact phone number |
| purposeOfTransaction | No | String | Transaction purpose |
| expirationTimestamp | No | Integer | Expiration time in minutes (default: 15) |
| merchantNameAlternateLanguage | No | String | Khmer merchant name |
| merchantCityAlternateLanguage | No | String | Khmer merchant city |

## Error Handling

### Common Error Responses

**Unauthorized (401)**:
```json
{
  "status": "ERROR",
  "message": "Unauthorized access - Token invalid or expired"
}
```
*Solution*: Re-run the login request to get a fresh token

**Bad Request (400)**:
```json
{
  "status": "ERROR",
  "message": "Failed to generate QR code: Invalid merchant ID"
}
```
*Solution*: Check all required fields are properly filled

**Server Error (500)**:
```json
{
  "status": "ERROR",
  "message": "Failed to check transaction: Connection timeout"
}
```
*Solution*: Verify Bakong API is accessible and credentials are correct

## Testing Tips

### 1. Use Variables in Requests
Reference variables using `{{variable_name}}`:
```
Authorization: Bearer {{token}}
URL: {{base_url}}/api/v1/bakong/generate-qr
```

### 2. Save Values from Responses
In request Tests tab, extract values:
```javascript
pm.environment.set('token', pm.response.json().data.accessToken);
pm.environment.set('qrCode', pm.response.json().qrCode);
```

### 3. Set Pre-request Scripts
Run code before request:
```javascript
// Example: Set timestamp
pm.environment.set('timestamp', new Date().getTime());
```

### 4. Create Mock Data
Duplicate requests with different test data:
- Different amounts (10000, 50000, 100000)
- Different merchant IDs
- Different currencies

## Authentication

The collection uses **Bearer Token Authentication**.

**Token Format**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Storage**:
- Automatically saved from login response
- Stored in `{{token}}` environment variable
- Valid for 12000 seconds (as per JWT config)

## Environment Management

### Create Test Environment
1. Click **Environments** (left sidebar)
2. Click **Create**
3. Add variables:
   - `base_url`: `http://localhost:8083`
   - `token`: (leave empty)
   - `bakong_account_id`: `senghour_soeurng@bkrt`
   - `bakong_merchant_id`: `ESHOP001`

### Switch Environments
- Select from dropdown (top-right)
- Variables will update automatically

### Use Different Environments
- Create `local` environment for development
- Create `staging` environment for testing
- Create `production` environment for live

## Troubleshooting

### Token Expired
**Error**: `{"message": "Unauthorized", "code": "401"}`
**Solution**: Run login request again to get fresh token

### Connection Refused
**Error**: `Error: connect ECONNREFUSED 127.0.0.1:8083`
**Solution**: 
1. Ensure E_Shop application is running
2. Verify port 8083 is correct
3. Check `base_url` environment variable

### Bad QR Code Response
**Error**: `Cannot convert QR data to image`
**Solution**:
1. Verify QR string is correct
2. Check MD5 field is filled
3. Ensure QR string is not empty

### Bakong API Connection Error
**Error**: `Failed to obtain Bakong token`
**Solution**:
1. Check Bakong credentials (bakong.account-id, bakong.base-url)
2. Verify internet connection to api-bakong.nbc.gov.kh
3. Check Bakong account is active

## Advanced Features

### 1. Request/Response Logging
Enable verbose logging in Postman preferences for debugging

### 2. Pre-request Scripts
Add validation before requests:
```javascript
const token = pm.environment.get('token');
if (!token) {
    throw new Error('Token not set. Please login first.');
}
```

### 3. Post-request Tests
Validate responses:
```javascript
pm.test("Status is 200", function() {
    pm.response.to.have.status(200);
});

pm.test("Response contains status field", function() {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('status');
    pm.expect(jsonData.status).to.equal('SUCCESS');
});
```

### 4. Run Collection as Test Suite
1. Click **Run** (Collection menu)
2. Select requests to run
3. Set iteration count
4. Click **Run** to execute

## Integration with CI/CD

Use Newman (Postman CLI) for automated testing:

```bash
# Install Newman
npm install -g newman

# Run collection
newman run Bakong_E_Shop.postman_collection.json \
  -e environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

## Support & Documentation

- **Bakong Official**: https://bakong.com.kh
- **Bakong API Docs**: https://api.bakong.com.kh/docs
- **Postman Docs**: https://learning.postman.com

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-05-14 | Initial release with QR generation, transaction check, and workflows |

---

**Last Updated**: May 14, 2026
**Maintained By**: E_Shop Development Team

