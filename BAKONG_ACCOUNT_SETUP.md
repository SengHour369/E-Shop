# E_Shop Bakong Configuration Setup Guide

## Your Bakong Account Details

```
Account ID:    senghour_soeurng@bkrt
Base URL:      https://api-bakong.nbc.gov.kh
Contact Email: seanghour097328@gmail.com
```

---

## 🔧 Environment Variables Setup

Set these environment variables on your system:

### Windows (PowerShell)
```powershell
$env:BAKONG_ACCOUNT_ID="senghour_soeurng@bkrt"
$env:BAKONG_BASE_URL="https://api-bakong.nbc.gov.kh"
$env:BAKONG_EMAIL="seanghour097328@gmail.com"
$env:BAKONG_AUTH_TOKEN="your_auth_token_here"
```

### Windows (Command Prompt)
```cmd
set BAKONG_ACCOUNT_ID=senghour_soeurng@bkrt
set BAKONG_BASE_URL=https://api-bakong.nbc.gov.kh
set BAKONG_EMAIL=seanghour097328@gmail.com
set BAKONG_AUTH_TOKEN=your_auth_token_here
```

### Windows (Set as System Variables)
1. Open `Environment Variables`
2. Click `New` under `User variables` or `System variables`
3. Add each variable:

| Variable Name | Value |
|---|---|
| `BAKONG_ACCOUNT_ID` | `senghour_soeurng@bkrt` |
| `BAKONG_BASE_URL` | `https://api-bakong.nbc.gov.kh` |
| `BAKONG_EMAIL` | `seanghour097328@gmail.com` |
| `BAKONG_AUTH_TOKEN` | `your_token_here` |

### Linux/Mac
```bash
export BAKONG_ACCOUNT_ID="senghour_soeurng@bkrt"
export BAKONG_BASE_URL="https://api-bakong.nbc.gov.kh"
export BAKONG_EMAIL="seanghour097328@gmail.com"
export BAKONG_AUTH_TOKEN="your_auth_token_here"
```

---

## 📝 application.properties Configuration

Your `application.properties` file has been updated with:

```properties
# Bakong Configuration
bakong.api.base-url=https://api-bakong.nbc.gov.kh
bakong.api.account-id=${BAKONG_ACCOUNT_ID}
bakong.api.merchant-name=E_Shop
bakong.api.auth-token=${BAKONG_AUTH_TOKEN}
bakong.api.timeout=30000
bakong.api.retry-attempts=3

# Bakong QR Code Configuration
bakong.qr.default-width=300
bakong.qr.default-height=300
bakong.qr.expires-in-minutes=30
bakong.qr.storage-path=qr-codes/

# Bakong Contact Email
bakong.contact.email=${BAKONG_EMAIL}

spring.main.allow-bean-definition-overriding=true
```

---

## 🔐 Getting Your Auth Token

### Step 1: Access Bakong Dashboard
```
URL: https://api-bakong.nbc.gov.kh
Account ID: senghour_soeurng@bkrt
```

### Step 2: Generate API Token
1. Log in with your account
2. Go to **Settings** → **API Keys**
3. Click **Generate New Token**
4. Copy the generated token

### Step 3: Set Environment Variable
```bash
export BAKONG_AUTH_TOKEN="your_generated_token_here"
```

---

## 🚀 Quick Start Commands

### 1. Set Environment Variables (PowerShell)
```powershell
$env:BAKONG_ACCOUNT_ID="senghour_soeurng@bkrt"
$env:BAKONG_AUTH_TOKEN="your_auth_token"
$env:BAKONG_EMAIL="seanghour097328@gmail.com"
```

### 2. Navigate to Project
```bash
cd D:\spring boot\E_Shop
```

### 3. Compile Project
```bash
mvn clean compile
```

### 4. Run Application
```bash
mvn spring-boot:run
```

### 5. Access Swagger UI
```
http://localhost:8083/swagger-ui.html
```

### 6. Test Bakong Health Check
```bash
curl -X GET "http://localhost:8083/api/v1/bakong/health"
```

---

## 📊 Configuration Variables Reference

| Variable | Value | Purpose |
|----------|-------|---------|
| `BAKONG_ACCOUNT_ID` | `senghour_soeurng@bkrt` | Your Bakong merchant account ID |
| `BAKONG_BASE_URL` | `https://api-bakong.nbc.gov.kh` | Bakong API endpoint |
| `BAKONG_EMAIL` | `seanghour097328@gmail.com` | Contact/support email |
| `BAKONG_AUTH_TOKEN` | Generated token | API authentication token |

---

## 🔄 Project Structure with Your Config

```
E_Shop/
├── src/main/resources/
│   └── application.properties  ✅ Updated with your config
│
├── src/main/java/.../Bakong/
│   ├── config/
│   │   └── BakongJacksonConfig.java
│   │       └── Uses: bakong.api.base-url
│   │
│   ├── service/
│   │   ├── BakongTokenService.java
│   │   │   └── Uses: bakong.api.auth-token
│   │   │
│   │   ├── BakongService.java
│   │   │   └── Uses: bakong.api.account-id
│   │   │
│   │   └── QRCodeService.java
│   │       └── Uses: bakong.qr.* settings
│   │
│   └── controller/
│       ├── BakongController.java
│       └── QRCodeController.java

└── Application Endpoints
    ├── /api/v1/bakong/payment/initiate
    │   Post payment to: https://api-bakong.nbc.gov.kh
    │
    ├── /api/v1/bakong/payment/initiate-with-qr
    │   Combine payment + QR code
    │
    └── /api/v1/bakong/qr-code/generate
        Generate QR codes locally
```

---

## 🧪 Test Your Configuration

### Test 1: Health Check
```bash
curl -X GET "http://localhost:8083/api/v1/bakong/health" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```
Bakong service is healthy
```

### Test 2: Check Configuration
Logs will show:
```
INFO - Bakong API Base URL: https://api-bakong.nbc.gov.kh
INFO - Account ID: senghour_soeurng@bkrt
INFO - Connected to Bakong successfully
```

### Test 3: Initiate Test Payment
```bash
curl -X POST "http://localhost:8083/api/v1/bakong/payment/initiate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "referenceId": "TEST-001",
    "amount": 1000,
    "email": "test@example.com",
    "phoneNumber": "+855987654321"
  }'
```

---

## 📝 Configuration Checklist

Before running the application:

```
☐ Set BAKONG_ACCOUNT_ID environment variable
☐ Set BAKONG_AUTH_TOKEN environment variable
☐ Set BAKONG_EMAIL environment variable
☐ Verify application.properties has updated URLs
☐ Restart any running terminals (to pick up env vars)
☐ Run: mvn clean compile
☐ Run: mvn spring-boot:run
☐ Access: http://localhost:8083/swagger-ui.html
☐ Test health check endpoint
☐ Verify logs show correct Bakong URL
```

---

## 🛠️ Troubleshooting

### Issue: "Cannot resolve variable BAKONG_ACCOUNT_ID"
**Solution**: Ensure environment variables are set before running the application
```bash
# Check if variable is set
echo $env:BAKONG_ACCOUNT_ID  # PowerShell
echo %BAKONG_ACCOUNT_ID%      # Command Prompt
```

### Issue: "Connection refused to api-bakong.nbc.gov.kh"
**Solution**: 
- Verify internet connection
- Check firewall allows outbound connections
- Verify URL is correct: `https://api-bakong.nbc.gov.kh`
- Check auth token is valid

### Issue: "Invalid auth token"
**Solution**:
- Log in to Bakong dashboard
- Generate new token
- Update `BAKONG_AUTH_TOKEN` environment variable

### Issue: "Account ID not recognized"
**Solution**:
- Verify account ID: `senghour_soeurng@bkrt`
- Check with Bakong support if account is active
- Ensure no extra spaces in account ID

---

## 📞 Contact Information

**Your Bakong Account Email:**
```
seanghour097328@gmail.com
```

**Use this email for:**
- Support requests
- Payment confirmations
- Account notifications

---

## 🔐 Security Notes

⚠️ **Important:**
- Never commit auth tokens to version control
- Use environment variables for sensitive data
- Rotate tokens regularly
- Keep account ID secure

✅ **Best Practices:**
- Use `.env` file locally (add to `.gitignore`)
- Use system environment variables for production
- Use secret management for CI/CD pipelines
- Audit API access logs regularly

---

## 🚀 Next Steps

1. **Set all environment variables**
   ```bash
   $env:BAKONG_ACCOUNT_ID="senghour_soeurng@bkrt"
   $env:BAKONG_AUTH_TOKEN="your_token"
   $env:BAKONG_EMAIL="seanghour097328@gmail.com"
   ```

2. **Start application**
   ```bash
   cd D:\spring boot\E_Shop
   mvn spring-boot:run
   ```

3. **Access Swagger**
   ```
   http://localhost:8083/swagger-ui.html
   ```

4. **Test endpoints**
   - Health check: `/api/v1/bakong/health`
   - Payment: `/api/v1/bakong/payment/initiate`
   - QR Code: `/api/v1/bakong/qr-code/generate`

5. **Review logs**
   - Should see successful connection to Bakong
   - Monitor for any authentication errors

---

## 📚 Related Documentation

- **BAKONG_INTEGRATION_README.md** - Full integration guide
- **QR_CODE_DOCUMENTATION.md** - QR code features
- **QR_CODE_API_TESTING_GUIDE.md** - API testing procedures
- **BAKONG_QR_QUICK_REFERENCE.md** - Quick reference

---

**Configuration Date**: May 14, 2026  
**Account**: senghour_soeurng@bkrt  
**Status**: ✅ Ready for Setup  
**Next**: Set environment variables and run application

