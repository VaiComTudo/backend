# Run all k6 tests sequentially
#!/bin/bash

set -e

echo "======================================"
echo "Running k6 Performance Test Suite"
echo "======================================"
echo ""

BASE_URL=${BASE_URL:-"http://localhost:8080"}
echo "Testing against: $BASE_URL"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "Error: k6 is not installed. Please install k6 first."
    echo "Visit: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# Check if backend is running
echo "Checking if backend is available..."
if ! curl -s --fail "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "Warning: Backend at $BASE_URL might not be running."
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "======================================"
echo "1/5 Running Smoke Test (1 min)"
echo "======================================"
k6 run -e BASE_URL="$BASE_URL" k6/smoke-test.js
echo ""

read -p "Continue to Load Test? (Y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    exit 0
fi

echo ""
echo "======================================"
echo "2/5 Running Load Test (16 min)"
echo "======================================"
k6 run -e BASE_URL="$BASE_URL" k6/load-test.js
echo ""

read -p "Continue to Stress Test? (Y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    exit 0
fi

echo ""
echo "======================================"
echo "3/5 Running Stress Test (26 min)"
echo "======================================"
k6 run -e BASE_URL="$BASE_URL" k6/stress-test.js
echo ""

read -p "Continue to Spike Test? (Y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Nn]$ ]]; then
    exit 0
fi

echo ""
echo "======================================"
echo "4/5 Running Spike Test (3 min)"
echo "======================================"
k6 run -e BASE_URL="$BASE_URL" k6/spike-test.js
echo ""

read -p "Run Soak Test (40 min)? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "======================================"
    echo "5/5 Running Soak Test (40 min)"
    echo "======================================"
    k6 run -e BASE_URL="$BASE_URL" k6/soak-test.js
    echo ""
fi

echo ""
echo "======================================"
echo "Performance Test Suite Complete!"
echo "======================================"
