#!/bin/bash
# ==============================================================================
# AI Test Agent — Automated System Health & End-to-End Verification Script (Bash)
# Usage:
#   chmod +x ./scripts/verify-all.sh
#   ./scripts/verify-all.sh
# ==============================================================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}==================================================================${NC}"
echo -e "${CYAN}         AI Test Agent - System Verification & Smoke Test         ${NC}"
echo -e "${CYAN}==================================================================${NC}"

all_passed=true

# ------------------------------------------------------------------------------
# 1. Health Checks
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[STEP 1/4] Checking Health of All Services...${NC}"

check_http() {
    local name=$1
    local url=$2
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url")
    if [ "$code" -ge 200 ] && [ "$code" -lt 400 ]; then
        echo -e "  ${GREEN}[OK] $name responded with HTTP $code.${NC}"
    else
        echo -e "  ${RED}[FAIL] $name returned HTTP $code or is unreachable!${NC}"
        all_passed=false
    fi
}

check_http "Service Registry (8761)" "http://localhost:8761/actuator/health"
check_http "API Gateway (8080)" "http://localhost:8080/actuator/health"
check_http "Auth Service (8081)" "http://localhost:8081/actuator/health"
check_http "Exploration Service (8082)" "http://localhost:8082/actuator/health"
check_http "Test Service (8083)" "http://localhost:8083/actuator/health"
check_http "Execution Service (8084)" "http://localhost:8084/actuator/health"
check_http "Bug Service (8085)" "http://localhost:8085/actuator/health"
check_http "React Frontend (5173)" "http://localhost:5173"
check_http "Demo Target App (3000)" "http://localhost:3000"

# ------------------------------------------------------------------------------
# 2. Service Discovery Verification
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[STEP 2/4] Verifying Eureka Service Registration...${NC}"
apps_json=$(curl -s -H "Accept: application/json" --max-time 5 "http://localhost:8761/eureka/apps")
for app in "API-GATEWAY" "AUTH-SERVICE" "EXPLORATION-SERVICE" "TEST-SERVICE" "EXECUTION-SERVICE" "BUG-SERVICE"; do
    if echo "$apps_json" | grep -q "$app"; then
        echo -e "  ${GREEN}[OK] $app registered in Eureka.${NC}"
    else
        echo -e "  ${YELLOW}[WARN] $app not yet registered in Eureka.${NC}"
    fi
done

# ------------------------------------------------------------------------------
# 3. Gateway Routing & Auth Flow Verification (FR1)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[STEP 3/4] Testing Gateway Routing & Authentication (FR1)...${NC}"
test_email="smoke_test_${RANDOM}@example.com"
test_pass="Password123!"

reg_response=$(curl -s -X POST "http://localhost:8080/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Smoke User\",\"email\":\"$test_email\",\"password\":\"$test_pass\",\"role\":\"USER\"}")

token=$(echo "$reg_response" | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

if [ -n "$token" ]; then
    echo -e "  ${GREEN}[OK] Gateway routed POST /api/auth/register successfully. JWT acquired.${NC}"
    
    # Test Protected /me endpoint
    me_code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" "http://localhost:8080/api/auth/me")
    if [ "$me_code" -eq 200 ]; then
        echo -e "  ${GREEN}[OK] Authenticated request to /api/auth/me succeeded.${NC}"
    else
        echo -e "  ${RED}[FAIL] Authenticated request to /api/auth/me failed with HTTP $me_code.${NC}"
        all_passed=false
    fi

    # Test Unauthorized access
    unauth_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/auth/me")
    if [ "$unauth_code" -eq 401 ]; then
        echo -e "  ${GREEN}[OK] Unauthenticated request to /api/auth/me rejected with 401 Unauthorized.${NC}"
    else
        echo -e "  ${RED}[FAIL] Unauthenticated request returned HTTP $unauth_code (expected 401).${NC}"
        all_passed=false
    fi
else
    echo -e "  ${RED}[FAIL] Failed to register user via Gateway: $reg_response${NC}"
    all_passed=false
fi

# ------------------------------------------------------------------------------
# 4. Summary Verdict
# ------------------------------------------------------------------------------
echo -e "\n${CYAN}==================================================================${NC}"
if [ "$all_passed" = true ]; then
    echo -e "${GREEN}           ALL CHECKS PASSED: SYSTEM IS HEALTHY!           ${NC}"
else
    echo -e "${RED}   SOME CHECKS FAILED: Review the red error logs above.    ${NC}"
fi
echo -e "${CYAN}==================================================================${NC}"
