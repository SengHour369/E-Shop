# E_Shop Bakong Configuration Setup - PowerShell Script
# Run as Administrator from PowerShell

# Set execution policy if needed
# Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "E_Shop Bakong Configuration Setup" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "WARNING: This script should be run as Administrator!" -ForegroundColor Yellow
    Write-Host "Press any key to continue anyway, or Ctrl+C to exit..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

# Function to set environment variables
function Set-EnvVariable {
    param(
        [string]$Name,
        [string]$Value,
        [string]$Scope = "User"
    )

    try {
        [Environment]::SetEnvironmentVariable($Name, $Value, $Scope)
        Write-Host "[OK] $Name set successfully" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Host "[ERROR] Failed to set $Name : $_" -ForegroundColor Red
        return $false
    }
}

Write-Host ""
Write-Host "Setting Environment Variables..." -ForegroundColor Yellow
Write-Host ""

# Bakong Configuration
Write-Host "Setting Bakong Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "BAKONG_ACCOUNT_ID" -Value "senghour_soeurng@bkrt" -Scope "User"
Set-EnvVariable -Name "BAKONG_EMAIL" -Value "seanghour097328@gmail.com" -Scope "User"

# JWT Configuration
Write-Host ""
Write-Host "Setting JWT Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "JWT_SECRET" -Value "66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576" -Scope "User"
Set-EnvVariable -Name "JWT_EXPIRATION" -Value "12000" -Scope "User"

# Server Configuration
Write-Host ""
Write-Host "Setting Server Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "SERVER_PORT" -Value "8083" -Scope "User"

# Cloudinary Configuration
Write-Host ""
Write-Host "Setting Cloudinary Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "CLOUDINARY_CLOUD_NAME" -Value "dr9z2x9s1" -Scope "User"
Set-EnvVariable -Name "CLOUDINARY_API_KEY" -Value "647172284186222" -Scope "User"
Set-EnvVariable -Name "CLOUDINARY_API_SECRET" -Value "qpwO-rP5bQg8rc6GfYyDzrgejfQ" -Scope "User"

# Email Configuration
Write-Host ""
Write-Host "Setting Email Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "EMAIL_HOST" -Value "smtp.gmail.com" -Scope "User"
Set-EnvVariable -Name "EMAIL_PORT" -Value "587" -Scope "User"
Set-EnvVariable -Name "EMAIL_USERNAME" -Value "seanghour097328@gmail.com" -Scope "User"
Set-EnvVariable -Name "EMAIL_PASSWORD" -Value "0973286236" -Scope "User"

# QR Code Configuration
Write-Host ""
Write-Host "Setting QR Code Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "QR_DEFAULT_WIDTH" -Value "300" -Scope "User"
Set-EnvVariable -Name "QR_DEFAULT_HEIGHT" -Value "300" -Scope "User"
Set-EnvVariable -Name "QR_EXPIRES_IN_MINUTES" -Value "30" -Scope "User"

# Database Configuration
Write-Host ""
Write-Host "Setting Database Configuration..." -ForegroundColor Cyan
Set-EnvVariable -Name "SPRING_DATASOURCE_URL" -Value "jdbc:postgresql://dpg-d7nl4g1o3t8c73eo92t0-a.oregon-postgres.render.com:5432/senghour369_x209" -Scope "User"
Set-EnvVariable -Name "SPRING_DATASOURCE_USERNAME" -Value "senghour" -Scope "User"
Set-EnvVariable -Name "SPRING_DATASOURCE_PASSWORD" -Value "7ePHLUZNSAan2gJQTe75FA3b4QDObTox" -Scope "User"

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "IMPORTANT: Manual Setup Required" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "You need to set BAKONG_AUTH_TOKEN manually:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Log in to: https://api-bakong.nbc.gov.kh" -ForegroundColor White
Write-Host "   Account: senghour_soeurng@bkrt" -ForegroundColor White
Write-Host ""
Write-Host "2. Navigate to Settings > API Keys" -ForegroundColor White
Write-Host ""
Write-Host "3. Generate a new auth token" -ForegroundColor White
Write-Host ""
Write-Host "4. Copy the token and run:" -ForegroundColor White
Write-Host '   [Environment]::SetEnvironmentVariable("BAKONG_AUTH_TOKEN", "your_token_here", "User")' -ForegroundColor Green
Write-Host ""
Write-Host "   Or simply:" -ForegroundColor White
Write-Host '   `$env:BAKONG_AUTH_TOKEN = "your_token_here"' -ForegroundColor Green
Write-Host ""

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Close and reopen PowerShell for changes to take effect" -ForegroundColor White
Write-Host ""
Write-Host "2. Set BAKONG_AUTH_TOKEN with your actual token:" -ForegroundColor White
Write-Host '   [Environment]::SetEnvironmentVariable("BAKONG_AUTH_TOKEN", "your_token_here", "User")' -ForegroundColor Green
Write-Host ""
Write-Host "3. Navigate to project:" -ForegroundColor White
Write-Host "   cd 'D:\spring boot\E_Shop'" -ForegroundColor Green
Write-Host ""
Write-Host "4. Compile project:" -ForegroundColor White
Write-Host "   mvn clean compile" -ForegroundColor Green
Write-Host ""
Write-Host "5. Run application:" -ForegroundColor White
Write-Host "   mvn spring-boot:run" -ForegroundColor Green
Write-Host ""
Write-Host "6. Access Swagger UI:" -ForegroundColor White
Write-Host "   http://localhost:8083/swagger-ui.html" -ForegroundColor Green
Write-Host ""
Write-Host "For more information, see BAKONG_ACCOUNT_SETUP.md" -ForegroundColor Cyan
Write-Host ""

# Optionally display current environment variables
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Current Bakong Variables:" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "BAKONG_ACCOUNT_ID: $([Environment]::GetEnvironmentVariable('BAKONG_ACCOUNT_ID', 'User'))" -ForegroundColor White
Write-Host "BAKONG_EMAIL: $([Environment]::GetEnvironmentVariable('BAKONG_EMAIL', 'User'))" -ForegroundColor White
Write-Host "BAKONG_AUTH_TOKEN: $(if ([Environment]::GetEnvironmentVariable('BAKONG_AUTH_TOKEN', 'User')) { 'SET' } else { 'NOT SET - Please set manually!' })" -ForegroundColor Yellow
Write-Host ""

Read-Host "Press Enter to exit"

