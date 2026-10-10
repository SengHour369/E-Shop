@echo off
REM ============================================
REM E_Shop Bakong Configuration Setup Script
REM ============================================
REM This script sets all required environment variables for Bakong payment integration

echo.
echo ========================================
echo E_Shop Bakong Configuration Setup
echo ========================================
echo.

REM Bakong Configuration
echo Setting Bakong Configuration...
setx BAKONG_ACCOUNT_ID "senghour_soeurng@bkrt"
setx BAKONG_EMAIL "seanghour097328@gmail.com"
echo [OK] Bakong Account ID set
echo [OK] Bakong Email set

REM JWT Configuration
echo.
echo Setting JWT Configuration...
setx JWT_SECRET "66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"
setx JWT_EXPIRATION "12000"
echo [OK] JWT Secret set
echo [OK] JWT Expiration set

REM Server Configuration
echo.
echo Setting Server Configuration...
setx SERVER_PORT "8083"
echo [OK] Server Port set to 8083

REM Cloudinary Configuration
echo.
echo Setting Cloudinary Configuration...
setx CLOUDINARY_CLOUD_NAME "dr9z2x9s1"
setx CLOUDINARY_API_KEY "647172284186222"
setx CLOUDINARY_API_SECRET "qpwO-rP5bQg8rc6GfYyDzrgejfQ"
echo [OK] Cloudinary credentials set

REM Email Configuration
echo.
echo Setting Email Configuration...
setx EMAIL_HOST "smtp.gmail.com"
setx EMAIL_PORT "587"
setx EMAIL_USERNAME "seanghour097328@gmail.com"
setx EMAIL_PASSWORD "0973286236"
echo [OK] Email configuration set

REM QR Code Configuration
echo.
echo Setting QR Code Configuration...
setx QR_DEFAULT_WIDTH "300"
setx QR_DEFAULT_HEIGHT "300"
setx QR_EXPIRES_IN_MINUTES "30"
echo [OK] QR Code configuration set

REM Database Configuration
echo.
echo Setting Database Configuration...
setx SPRING_DATASOURCE_URL "jdbc:postgresql://dpg-d7nl4g1o3t8c73eo92t0-a.oregon-postgres.render.com:5432/senghour369_x209"
setx SPRING_DATASOURCE_USERNAME "senghour"
setx SPRING_DATASOURCE_PASSWORD "7ePHLUZNSAan2gJQTe75FA3b4QDObTox"
echo [OK] Database configuration set

REM Important: Auth Token (User should set this manually from Bakong dashboard)
echo.
echo ========================================
echo IMPORTANT: Manual Setup Required
echo ========================================
echo.
echo You need to set BAKONG_AUTH_TOKEN manually:
echo.
echo 1. Log in to: https://api-bakong.nbc.gov.kh
echo 2. Account: senghour_soeurng@bkrt
echo 3. Go to Settings ^> API Keys
echo 4. Generate and copy the auth token
echo 5. Run: setx BAKONG_AUTH_TOKEN "your_token_here"
echo.
echo Or run in PowerShell:
echo $env:BAKONG_AUTH_TOKEN="your_token_here"
echo.

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Environment variables have been set as system variables.
echo.
echo Next Steps:
echo 1. Close and reopen terminal/PowerShell for changes to take effect
echo 2. Set BAKONG_AUTH_TOKEN with your actual token
echo 3. Run: cd D:\spring boot\E_Shop
echo 4. Run: mvn clean compile
echo 5. Run: mvn spring-boot:run
echo 6. Access: http://localhost:8083/swagger-ui.html
echo.
echo For more information, see BAKONG_ACCOUNT_SETUP.md
echo.
pause

