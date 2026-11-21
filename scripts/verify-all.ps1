# ==============================================================================
# AI Test Agent — Automated System Health & End-to-End Verification Script
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\scripts\verify-all.ps1
# ==============================================================================

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "         AI Test Agent - System Verification & Smoke Test         " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

$allPassed = $true
$token = ""

# ------------------------------------------------------------------------------
# 1. Health Checks
# ------------------------------------------------------------------------------
Write-Host "`n[STEP 1/4] Checking Health of All Services..." -ForegroundColor Yellow

$services = @(
    @{ Name = "PostgreSQL (5432)";         Port = 5432; Type = "TCP" },
    @{ Name = "Service Registry (8761)";   Url = "http://localhost:8761/actuator/health"; Type = "HTTP" },
    @{ Name = "API Gateway (8080)";        Url = "http://localhost:8080/actuator/health"; Type = "HTTP" },
    @{ Name = "Auth Service (8081)";       Url = "http://localhost:8081/actuator/health"; Type = "HTTP" },
    @{ Name = "Exploration Service (8082)";Url = "http://localhost:8082/actuator/health"; Type = "HTTP" },
    @{ Name = "Test Service (8083)";       Url = "http://localhost:8083/actuator/health"; Type = "HTTP" },
    @{ Name = "Execution Service (8084)";  Url = "http://localhost:8084/actuator/health"; Type = "HTTP" },
    @{ Name = "Bug Service (8085)";        Url = "http://localhost:8085/actuator/health"; Type = "HTTP" },
    @{ Name = "React Frontend (5173)";     Url = "http://localhost:5173";                 Type = "HTTP" },
    @{ Name = "Demo Target App (3000)";    Url = "http://localhost:3000";                 Type = "HTTP" }
)

foreach ($s in $services) {
    if ($s.Type -eq "TCP") {
        $tcp = Test-NetConnection -ComputerName localhost -Port $s.Port -WarningAction SilentlyContinue
        if ($tcp.TcpTestSucceeded) {
            Write-Host "  [OK] $($s.Name) is accepting connections." -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] $($s.Name) is unreachable!" -ForegroundColor Red
            $allPassed = $false
        }
    } else {
        try {
            $resp = Invoke-WebRequest -Uri $s.Url -Method Get -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
            if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400) {
                Write-Host "  [OK] $($s.Name) responded with HTTP $($resp.StatusCode)." -ForegroundColor Green
            } else {
                Write-Host "  [FAIL] $($s.Name) returned HTTP $($resp.StatusCode)." -ForegroundColor Red
                $allPassed = $false
            }
        } catch {
            Write-Host "  [FAIL] $($s.Name) is unreachable! ($($_.Exception.Message))" -ForegroundColor Red
            $allPassed = $false
        }
    }
}

# ------------------------------------------------------------------------------
# 2. Service Discovery Verification
# ------------------------------------------------------------------------------
Write-Host "`n[STEP 2/4] Verifying Eureka Service Registration..." -ForegroundColor Yellow
try {
    $eurekaXml = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Method Get -Headers @{ "Accept" = "application/json" } -TimeoutSec 5 -ErrorAction Stop
    $appNames = @($eurekaXml.applications.application | ForEach-Object { $_.name })
    Write-Host "  Registered applications in Eureka: $($appNames -join ', ')" -ForegroundColor Cyan

    $expectedApps = @("API-GATEWAY", "AUTH-SERVICE", "EXPLORATION-SERVICE", "TEST-SERVICE", "EXECUTION-SERVICE", "BUG-SERVICE")
    foreach ($app in $expectedApps) {
        if ($appNames -contains $app) {
            Write-Host "  [OK] $app registered in Eureka." -ForegroundColor Green
        } else {
            Write-Host "  [WARN] $app not yet registered in Eureka (may still be starting up)." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  [FAIL] Could not query Eureka service registry." -ForegroundColor Red
}

# ------------------------------------------------------------------------------
# 3. Gateway Routing & Auth Flow Verification (FR1)
# ------------------------------------------------------------------------------
Write-Host "`n[STEP 3/4] Testing Gateway Routing & Authentication (FR1)..." -ForegroundColor Yellow

$testEmail = "smoke_test_" + (Get-Random) + "@example.com"
$testPass = "Password123!"

# Test Registration
try {
    $regBody = @{
        name = "Smoke Test User"
        email = $testEmail
        password = $testPass
        role = "USER"
    } | ConvertTo-Json

    $regResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $regBody -ContentType "application/json" -TimeoutSec 5 -ErrorAction Stop
    if ($regResp.token) {
        Write-Host "  [OK] Gateway routed POST /api/auth/register successfully." -ForegroundColor Green
        $token = $regResp.token
    } else {
        Write-Host "  [FAIL] Register returned without token." -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "  [FAIL] Register via Gateway failed: $($_.Exception.Message)" -ForegroundColor Red
    $allPassed = $false
}

# Test Login
if ($token -eq "") {
    try {
        $loginBody = @{
            email = $testEmail
            password = $testPass
        } | ConvertTo-Json

        $loginResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -TimeoutSec 5 -ErrorAction Stop
        $token = $loginResp.token
        Write-Host "  [OK] Gateway routed POST /api/auth/login successfully." -ForegroundColor Green
    } catch {
        Write-Host "  [FAIL] Login via Gateway failed: $($_.Exception.Message)" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test Protected Endpoint with JWT
if ($token -ne "") {
    try {
        $meResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/me" -Method Get -Headers @{ "Authorization" = "Bearer $token" } -TimeoutSec 5 -ErrorAction Stop
        if ($meResp.email -eq $testEmail) {
            Write-Host "  [OK] Authenticated request to /api/auth/me succeeded (User: $($meResp.name))." -ForegroundColor Green
        }
    } catch {
        Write-Host "  [FAIL] Authenticated request to /api/auth/me failed: $($_.Exception.Message)" -ForegroundColor Red
        $allPassed = $false
    }

    # Test Unauthorized Access (Should be blocked by Gateway)
    try {
        $unauth = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/me" -Method Get -TimeoutSec 5 -ErrorAction Stop
        Write-Host "  [FAIL] Unauthenticated request to /api/auth/me was NOT blocked!" -ForegroundColor Red
        $allPassed = $false
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            Write-Host "  [OK] Unauthenticated request to /api/auth/me was properly rejected with 401 Unauthorized." -ForegroundColor Green
        } else {
            Write-Host "  [WARN] Unexpected response status: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# ------------------------------------------------------------------------------
# 4. Summary Verdict
# ------------------------------------------------------------------------------
Write-Host "`n==================================================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "           ALL CHECKS PASSED: SYSTEM IS HEALTHY!           " -ForegroundColor Green
} else {
    Write-Host "   SOME CHECKS FAILED: Review the red error logs above.    " -ForegroundColor Red
}
Write-Host "==================================================================" -ForegroundColor Cyan
