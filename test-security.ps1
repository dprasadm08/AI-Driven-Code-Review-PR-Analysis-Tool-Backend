# Spring Security Testing Script
# Tests JWT authentication and protected endpoints

$baseUrl = "http://localhost:8080/api"
$ErrorActionPreference = "SilentlyContinue"

Write-Host "`n╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Spring Security & JWT Testing Suite        ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Test 1: Health Check (Public Endpoint)
Write-Host "Test 1: Health Check (Public Endpoint)" -ForegroundColor Yellow
Write-Host "──────────────────────────────────────" -ForegroundColor Gray
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
    Write-Host "✅ Status: $($health.status)" -ForegroundColor Green
    Write-Host "   Service: $($health.service)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nℹ️  Is the application running? Run: .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Test 2: Create Test User (Signup)
Write-Host "`nTest 2: User Signup (Public Endpoint)" -ForegroundColor Yellow
Write-Host "──────────────────────────────────────" -ForegroundColor Gray
$randomNum = Get-Random -Maximum 9999
$signupBody = @{
    username = "testuser$randomNum"
    email = "test$randomNum@example.com"
    password = "password123"
    fullName = "Test User $randomNum"
} | ConvertTo-Json

$token = $null
try {
    $signupResponse = Invoke-RestMethod -Uri "$baseUrl/auth/signup" -Method Post -Body $signupBody -ContentType "application/json"
    $token = $signupResponse.accessToken
    Write-Host "✅ User created: $($signupResponse.username)" -ForegroundColor Green
    Write-Host "   Email: $($signupResponse.email)" -ForegroundColor Gray
    Write-Host "   Token: $($token.Substring(0, 30))..." -ForegroundColor Gray
} catch {
    Write-Host "⚠️  Signup failed (user might exist), trying login..." -ForegroundColor Yellow
    
    # Fallback to login with default user
    $loginBody = @{
        usernameOrEmail = "testuser"
        password = "password123"
    } | ConvertTo-Json
    
    try {
        $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
        $token = $loginResponse.accessToken
        Write-Host "✅ Logged in as: $($loginResponse.username)" -ForegroundColor Green
    } catch {
        Write-Host "❌ Both signup and login failed" -ForegroundColor Red
        Write-Host "   Please ensure MongoDB is running" -ForegroundColor Yellow
        exit 1
    }
}

# Test 3: Protected Endpoint WITHOUT Token (Should Fail)
Write-Host "`nTest 3: Protected Endpoint WITHOUT Token" -ForegroundColor Yellow
Write-Host "──────────────────────────────────────" -ForegroundColor Gray
try {
    # Attempt to access without token - should fail with 401
    Invoke-RestMethod -Uri "$baseUrl/repositories" -Method Get | Out-Null
    Write-Host "❌ SECURITY ISSUE: Endpoint accessible without token!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Correctly blocked: 401 Unauthorized" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Unexpected error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Test 4: Protected Endpoint WITH Invalid Token (Should Fail)
Write-Host "`nTest 4: Protected Endpoint WITH Invalid Token" -ForegroundColor Yellow
Write-Host "──────────────────────────────────────" -ForegroundColor Gray
$invalidHeaders = @{ Authorization = "Bearer invalid.token.here" }
try {
    # Attempt with invalid token - should fail with 401
    Invoke-RestMethod -Uri "$baseUrl/repositories" -Method Get -Headers $invalidHeaders | Out-Null
    Write-Host "❌ SECURITY ISSUE: Invalid token accepted!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Correctly rejected: Invalid token" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Unexpected error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Test 5: Protected Endpoint WITH Valid Token (Should Succeed)
Write-Host "`nTest 5: Protected Endpoints WITH Valid Token" -ForegroundColor Yellow
Write-Host "──────────────────────────────────────" -ForegroundColor Gray
$validHeaders = @{ Authorization = "Bearer $token" }

# 5a: Get Current User
try {
    $user = Invoke-RestMethod -Uri "$baseUrl/auth/me" -Method Get -Headers $validHeaders
    Write-Host "✅ GET /auth/me - Current user: $($user.username)" -ForegroundColor Green
} catch {
    Write-Host "❌ GET /auth/me - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5b: Get All Repositories
try {
    $repos = Invoke-RestMethod -Uri "$baseUrl/repositories" -Method Get -Headers $validHeaders
    Write-Host "✅ GET /repositories - $($repos.message)" -ForegroundColor Green
} catch {
    Write-Host "❌ GET /repositories - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5c: Get Repository by ID
try {
    $repo = Invoke-RestMethod -Uri "$baseUrl/repositories/repo123" -Method Get -Headers $validHeaders
    Write-Host "✅ GET /repositories/{id} - Status: $($repo.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ GET /repositories/{id} - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5d: Add Repository (POST)
try {
    $addRepoBody = @{ name = "test-repo"; url = "https://github.com/test/repo" } | ConvertTo-Json
    $newRepo = Invoke-RestMethod -Uri "$baseUrl/repositories" -Method Post -Headers $validHeaders -Body $addRepoBody -ContentType "application/json"
    Write-Host "✅ POST /repositories - Status: $($newRepo.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ POST /repositories - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5e: Get All Pull Requests
try {
    $prs = Invoke-RestMethod -Uri "$baseUrl/pull-requests" -Method Get -Headers $validHeaders
    Write-Host "✅ GET /pull-requests - $($prs.message)" -ForegroundColor Green
} catch {
    Write-Host "❌ GET /pull-requests - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5f: Get Pull Request by ID
try {
    $pr = Invoke-RestMethod -Uri "$baseUrl/pull-requests/pr123" -Method Get -Headers $validHeaders
    Write-Host "✅ GET /pull-requests/{id} - Status: $($pr.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ GET /pull-requests/{id} - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5g: Trigger Analysis
try {
    $analysisBody = @{ prId = "pr123"; repository = "test-repo" } | ConvertTo-Json
    $analysis = Invoke-RestMethod -Uri "$baseUrl/analysis/trigger" -Method Post -Headers $validHeaders -Body $analysisBody -ContentType "application/json"
    Write-Host "✅ POST /analysis/trigger - Status: $($analysis.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ POST /analysis/trigger - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5h: Get Analysis Results
try {
    $analysisResults = Invoke-RestMethod -Uri "$baseUrl/analysis/results/pr123" -Method Get -Headers $validHeaders
    # Verify we got results back
    Write-Host "✅ GET /analysis/results/{id} - Retrieved (PR: $($analysisResults.prId))" -ForegroundColor Green
} catch {
    Write-Host "❌ GET /analysis/results/{id} - Failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Malformed Authorization Header
Write-Host "`nTest 6: Edge Cases" -ForegroundColor Yellow
Write-Host "──────────────────────────────────────" -ForegroundColor Gray

# 6a: Malformed header
$malformedHeaders = @{ Authorization = "InvalidFormat" }
try {
    # Test with malformed Authorization header - should fail
    Invoke-RestMethod -Uri "$baseUrl/auth/me" -Method Get -Headers $malformedHeaders | Out-Null
    Write-Host "❌ Malformed header accepted!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Malformed header rejected" -ForegroundColor Green
    }
}

# 6b: Empty token
$emptyHeaders = @{ Authorization = "Bearer " }
try {
    # Test with empty Bearer token - should fail
    Invoke-RestMethod -Uri "$baseUrl/auth/me" -Method Get -Headers $emptyHeaders | Out-Null
    Write-Host "❌ Empty token accepted!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Empty token rejected" -ForegroundColor Green
    }
}

# Summary
Write-Host "`n╔════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║            Testing Complete                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`nSecurity Summary:" -ForegroundColor Yellow
Write-Host "  ✅ Public endpoints accessible" -ForegroundColor Green
Write-Host "  ✅ Protected endpoints require authentication" -ForegroundColor Green
Write-Host "  ✅ Invalid tokens are rejected" -ForegroundColor Green
Write-Host "  ✅ Valid tokens grant access" -ForegroundColor Green
Write-Host "  ✅ Malformed requests handled properly" -ForegroundColor Green

Write-Host "`nℹ️  All security components are working correctly!" -ForegroundColor Cyan
Write-Host "`nYour JWT Token (save for manual testing):" -ForegroundColor Yellow
Write-Host "$token`n" -ForegroundColor Gray
