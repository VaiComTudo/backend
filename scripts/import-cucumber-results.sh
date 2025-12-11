#!/bin/bash

# Import Cucumber test results to Xray Cloud
# Usage: ./import-cucumber-results.sh [CUCUMBER_JSON_FILE] [TEST_EXEC_KEY]
# Example: ./import-cucumber-results.sh target/cucumber.json VCT-80

set -e

XRAY_CLIENT_ID="${XRAY_CLIENT_ID}"
XRAY_CLIENT_SECRET="${XRAY_CLIENT_SECRET}"
BASE_URL="https://xray.cloud.getxray.app"

# Check if credentials are set
if [ -z "$XRAY_CLIENT_ID" ] || [ -z "$XRAY_CLIENT_SECRET" ]; then
    echo "Error: XRAY_CLIENT_ID and XRAY_CLIENT_SECRET environment variables must be set"
    echo "Usage: XRAY_CLIENT_ID=xxx XRAY_CLIENT_SECRET=yyy ./import-cucumber-results.sh [JSON_FILE] [TEST_EXEC_KEY]"
    exit 1
fi

# Get parameters
CUCUMBER_JSON="${1:-target/cucumber.json}"

if [ ! -f "$CUCUMBER_JSON" ]; then
    echo "Cucumber JSON file not found: $CUCUMBER_JSON"
    exit 1
fi

echo "Importing Cucumber results from: $CUCUMBER_JSON"

# Create temporary auth file
AUTH_FILE=$(mktemp)
trap "rm -f $AUTH_FILE" EXIT

cat > "$AUTH_FILE" <<EOF
{
  "client_id": "$XRAY_CLIENT_ID",
  "client_secret": "$XRAY_CLIENT_SECRET"
}
EOF

# Authenticate and get token
echo "Authenticating with Xray..."
TOKEN=$(curl -s -H "Content-Type: application/json" -X POST --data @"$AUTH_FILE" "$BASE_URL/api/v2/authenticate" | tr -d '"')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "Failed to authenticate with Xray"
    exit 1
fi

echo "✓ Authentication successful"

# Build the import URL
IMPORT_URL="$BASE_URL/api/v2/import/execution/cucumber"

# Import results
echo "Uploading test results..."
RESPONSE=$(curl -s -w "\n%{http_code}" -H "Content-Type: application/json" \
    -X POST -H "Authorization: Bearer $TOKEN" \
    --data @"$CUCUMBER_JSON" \
    "$IMPORT_URL")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "Results imported successfully!"
    echo "Response: $BODY"
else
    echo "Failed to import results (HTTP $HTTP_CODE)"
    echo "Response: $BODY"
    exit 1
fi
