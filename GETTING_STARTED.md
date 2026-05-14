# 🚀 E_Shop Bakong Setup - Getting Started Guide

## Your Bakong Account Information

```
✅ Account ID: senghour_soeurng@bkrt
✅ Email: seanghour097328@gmail.com
✅ API Base URL: https://api-bakong.nbc.gov.kh
⏳ Auth Token: (Get from Bakong Dashboard)
```

---

## 🎯 Five Minute Quick Start

### Option A: Automatic Setup (PowerShell)

**1. Open PowerShell as Administrator**

**2. Run the setup script:**
```powershell
cd 'D:\spring boot\E_Shop'
.\setup-bakong-env.ps1
```

**3. When prompted, get your Bakong auth token:**
- Log in to: https://api-bakong.nbc.gov.kh
- Account: senghour_soeurng@bkrt
- Settings → API Keys → Generate Token
- Copy the token

**4. Set the auth token:**
```powershell
[Environment]::SetEnvironmentVariable("BAKONG_AUTH_TOKEN", "your_token_here", "User")
```

**5. Close and reopen PowerShell**

**6. Start the application:**
```powershell
cd 'D:\spring boot\E_Shop'
mvn clean compile
mvn spring-boot:run
```

**7. Access the app:**
```
http://localhost:8083/swagger-ui.html
```

---

### Option B: Manual Setup (Command Prompt)

**1. Open Command Prompt as Administrator**

**2. Run the batch script:**
```cmd
cd D:\spring boot\E_Shop
setup-bakong-env.bat
```

**3. Close and reopen Command Prompt**

**4. Set Bakong auth token:**
```cmd
setx BAKONG_AUTH_TOKEN "your_token_here"
```

**5. Start the application:**
```cmd
cd D:\spring boot\E_Shop
mvn clean compile
mvn spring-boot:run
```

**6. Access the app:**
```
http://localhost:8083/swagger-ui.html
```

---

### Option C: Manual Environment Variable Setup

**1. Go to Settings → Environment Variables**
   - Windows Key → Type "Environment Variables"
   - Click "Edit the system environment variables"

**2. Add these variables:**

| Variable Name | Value |
|---|---|
| `BAKONG_ACCOUNT_ID` | `senghour_soeurng@bkrt` |
| `BAKONG_EMAIL` | `seanghour097328@gmail.com` |
| `BAKONG_AUTH_TOKEN` | `your_token_from_dashboard` |
| `JWT_SECRET` | `66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576` |
| `SERVER_PORT` | `8083` |

**3. Click OK and restart your terminal**

**4. Run the application:**
```bash
cd D:\spring boot\E_Shop
mvn clean compile
mvn spring-boot:run
```

---

## 📋 Complete Setup Checklist

```
Step 1: Get Your Bakong Auth Token
  ☐ Visit: https://api-bakong.nbc.gov.kh
  ☐ Log in with: senghour_soeurng@bkrt
  ☐ Go to: Settings → API Keys
  ☐ Click: Generate New Token
  ☐ Copy: The generated token

Step 2: Set Environment Variables
  ☐ Use Option A, B, or C above
  ☐ Verify variables are set:
    $env:BAKONG_ACCOUNT_ID           # PowerShell
    echo %BAKONG_ACCOUNT_ID%         # Command Prompt

Step 3: Compile Project
  ☐ cd D:\spring boot\E_Shop
  ☐ mvn clean compile

Step 4: Run Application
  ☐ mvn spring-boot:run
  ☐ Wait for: "Started LearningSpringSecurityApplication"

Step 5: Test Bakong Connection
  ☐ Open: http://localhost:8083/swagger-ui.html
  ☐ Click: bakong-controller
  ☐ Try: GET /api/v1/bakong/health
  ☐ Should see: "Bakong service is healthy"

Step 6: Test Payment + QR
  ☐ Go to: POST /api/v1/bakong/payment/initiate-with-qr
  ☐ Fill in test data:
    {
      "referenceId": "TEST-001",
      "amount": 10000,
      "email": "test@example.com"
    }
  ☐ Should get: Payment + QR code response

✅ You're Ready!
```

---

## 🔐 Getting Your Bakong Auth Token (Detailed Steps)

### Step 1: Access Bakong Dashboard
```
URL: https://api-bakong.nbc.gov.kh
```

### Step 2: Log In
```
Account ID: senghour_soeurng@bkrt
```

### Step 3: Navigate to API Settings
- Click **Settings** (top menu)
- Select **API Keys** or **Merchant API**

### Step 4: Generate Token
- Click **Generate New Token**
- Or **Create API Key**

### Step 5: Copy Token
```
Token Format: Usually starts with "ba_"
Example: ba_sk_live_abc123def456...
```

### Step 6: Set Environment Variable

**PowerShell:**
```powershell
[Environment]::SetEnvironmentVariable("BAKONG_AUTH_TOKEN", "your_token_here", "User")
```

**Command Prompt:**
```cmd
setx BAKONG_AUTH_TOKEN "your_token_here"
```

**Temporary (PowerShell):**
```powershell
$env:BAKONG_AUTH_TOKEN = "your_token_here"
# Only valid for current session
```

---

## 🧪 Verify Your Setup

### Test 1: Check Environment Variables
```powershell
# PowerShell
echo $env:BAKONG_ACCOUNT_ID
echo $env:BAKONG_AUTH_TOKEN
echo $env:BAKONG_EMAIL

# Command Prompt
echo %BAKONG_ACCOUNT_ID%
echo %BAKONG_AUTH_TOKEN%
echo %BAKONG_EMAIL%
```

Should show:
```
senghour_soeurng@bkrt
ba_sk_live_xxxxx
seanghour097328@gmail.com
```

### Test 2: Compile Project
```bash
cd D:\spring boot\E_Shop
mvn clean compile
```

Expected output ends with:
```
BUILD SUCCESS
```

### Test 3: Run Application
```bash
mvn spring-boot:run
```

Expected output contains:
```
Started LearningSpringSecurityApplication in X seconds
```

### Test 4: Test API Endpoints
```bash
# Health check
curl -X GET "http://localhost:8083/api/v1/bakong/health"

# Should respond with:
# Bakong service is healthy
```

### Test 5: Test QR Generation
```bash
curl -X GET "http://localhost:8083/api/v1/bakong/qr-code/health"

# Should respond with:
# QR Code Service is running
```

---

## 📊 Project Configuration Summary

### Application Properties (Updated)
```properties
# Your Bakong Account Configuration
bakong.api.base-url=https://api-bakong.nbc.gov.kh
bakong.api.account-id=${BAKONG_ACCOUNT_ID}
bakong.api.auth-token=${BAKONG_AUTH_TOKEN}
bakong.contact.email=${BAKONG_EMAIL}

# QR Code Configuration
bakong.qr.default-width=300
bakong.qr.default-height=300
bakong.qr.expires-in-minutes=30
```

### Environment Variables (Set)
```bash
BAKONG_ACCOUNT_ID=senghour_soeurng@bkrt
BAKONG_EMAIL=seanghour097328@gmail.com
BAKONG_AUTH_TOKEN=your_token_here  # ← You need to set this
```

---

## 🔗 Available Endpoints

### Bakong Payment Endpoints
```
POST /api/v1/bakong/payment/initiate
  └─ Create payment transaction

POST /api/v1/bakong/payment/initiate-with-qr  [NEW]
  └─ Create payment + generate QR code

POST /api/v1/bakong/payment/check-status
  └─ Check payment status

POST /api/v1/bakong/payment/refund
  └─ Process refund

GET /api/v1/bakong/merchant/info
  └─ Get merchant details

GET /api/v1/bakong/health
  └─ Health check
```

### QR Code Endpoints
```
POST /api/v1/bakong/qr-code/generate
  └─ Generate QR from request

GET /api/v1/bakong/qr-code/generate/{id}
  └─ Generate with parameters

GET /api/v1/bakong/qr-code/quick/{id}
  └─ Quick generate

POST /api/v1/bakong/qr-code/generate-file
  └─ Save QR to file

GET /api/v1/bakong/qr-code/health
  └─ Health check
```

---

## 🛠️ Project Structure

```
D:\spring boot\E_Shop\
│
├── ✅ pom.xml                              (ZXing deps)
├── ✅ application.properties                (Bakong config)
├── ✅ setup-bakong-env.bat                  (Windows batch script)
├── ✅ setup-bakong-env.ps1                  (PowerShell script)
├── ✅ .env.example                          (Variable reference)
│
├── src/main/java/com/example/.../Bakong/
│   ├── config/BakongJacksonConfig.java
│   ├── controller/
│   │   ├── BakongController.java
│   │   └── QRCodeController.java
│   ├── dto/
│   │   ├── BakongPaymentRequest.java
│   │   ├── BakongPaymentResponse.java
│   │   ├── QRCodeGenerateRequest.java
│   │   └── QRCodeResponse.java
│   └── service/
│       ├── BakongService.java
│       ├── BakongTokenService.java
│       ├── QRCodeService.java
│       └── impl/
│           ├── BakongServiceImpl.java
│           ├── BakongTokenServiceImpl.java
│           └── QRCodeServiceImpl.java
│
└── Documentation/
    ├── BAKONG_ACCOUNT_SETUP.md
    ├── BAKONG_QR_QUICK_REFERENCE.md
    ├── QR_CODE_DOCUMENTATION.md
    ├── QR_CODE_API_TESTING_GUIDE.md
    └── This file
```

---

## ⚡ Quick Commands Reference

### Setup
```bash
# PowerShell setup
.\setup-bakong-env.ps1

# Or batch setup
setup-bakong-env.bat

# Or manual setup
setx BAKONG_ACCOUNT_ID senghour_soeurng@bkrt
setx BAKONG_AUTH_TOKEN your_token_here
```

### Build & Run
```bash
# Compile
mvn clean compile

# Run
mvn spring-boot:run

# Run with output
mvn spring-boot:run -X
```

### Test
```bash
# Health check
curl http://localhost:8083/api/v1/bakong/health

# QR health
curl http://localhost:8083/api/v1/bakong/qr-code/health

# Access Swagger
http://localhost:8083/swagger-ui.html
```

---

## ❓ Troubleshooting

### Q: "Cannot resolve BAKONG_ACCOUNT_ID"
**A:** Ensure environment variable is set:
```powershell
setx BAKONG_ACCOUNT_ID "senghour_soeurng@bkrt"
# Then close and reopen terminal
```

### Q: Connection refused error
**A:** Check Bakong URL is correct:
```
https://api-bakong.nbc.gov.kh
(Not: https://api.bakong.com.kh which is old)
```

### Q: Invalid auth token error
**A:** Get new token from Bakong dashboard:
1. Visit: https://api-bakong.nbc.gov.kh
2. Account: senghour_soeurng@bkrt
3. Settings → API Keys
4. Generate new token

### Q: Port 8083 already in use
**A:** Change port in application.properties or kill process:
```bash
netstat -ano | findstr :8083
taskkill /PID {PID} /F
```

### Q: "BUILD FAILURE" during compile
**A:** Check Java version and dependencies:
```bash
java -version  # Should be 17 or higher
mvn dependency:tree
```

---

## 📞 Support & Resources

**Your Account Details:**
- Account: senghour_soeurng@bkrt
- Email: seanghour097328@gmail.com
- Bakong URL: https://api-bakong.nbc.gov.kh

**Documentation Files:**
1. BAKONG_ACCOUNT_SETUP.md - Detailed setup guide
2. BAKONG_QR_QUICK_REFERENCE.md - Quick reference
3. QR_CODE_DOCUMENTATION.md - Feature details
4. QR_CODE_API_TESTING_GUIDE.md - API testing

**External Resources:**
- Bakong API: https://api-bakong.nbc.gov.kh/docs
- ZXing Library: https://zxing.org/
- Spring Boot: https://spring.io/projects/spring-boot

---

## ✅ You're All Set!

Follow the 5-minute quick start above and you'll have:
- ✅ Bakong payment integration
- ✅ QR code generation
- ✅ Full REST API
- ✅ Ready to test and deploy

**Next Step:** Run the setup script and start coding! 🚀

---

**Setup Date**: May 14, 2026  
**Account**: senghour_soeurng@bkrt  
**Status**: Ready to Deploy  
**Version**: 1.0.0

