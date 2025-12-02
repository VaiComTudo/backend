#!/bin/bash

# Export Cucumber features from Xray Cloud
# Usage: ./export-features.sh [TEST_KEYS]
# Example: ./export-features.sh "VCT-1;VCT-2;VCT-3"

set -e

XRAY_CLIENT_ID="${XRAY_CLIENT_ID}"
XRAY_CLIENT_SECRET="${XRAY_CLIENT_SECRET}"
BASE_URL="https://xray.cloud.getxray.app"
FEATURES_DIR="src/test/resources/features"

# Check if credentials are set
if [ -z "$XRAY_CLIENT_ID" ] || [ -z "$XRAY_CLIENT_SECRET" ]; then
    echo "Error: XRAY_CLIENT_ID and XRAY_CLIENT_SECRET environment variables must be set"
    echo "Usage: XRAY_CLIENT_ID=xxx XRAY_CLIENT_SECRET=yyy ./export-features.sh [TEST_KEYS]"
    exit 1
fi

# Get test keys from argument or use default
TEST_KEYS="${1:-VCT-80}"
echo "Exporting features for test keys: $TEST_KEYS"

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

# Export features
echo "Downloading features..."
TEMP_ZIP=$(mktemp --suffix=.zip)
trap "rm -f $AUTH_FILE $TEMP_ZIP" EXIT

HTTP_CODE=$(curl -s -w "%{http_code}" -H "Content-Type: application/json" \
    -X GET -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/v2/export/cucumber?keys=$TEST_KEYS" \
    -o "$TEMP_ZIP")

if [ "$HTTP_CODE" != "200" ]; then
    echo "Failed to download features (HTTP $HTTP_CODE)"
    exit 1
fi

# Create features directory if it doesn't exist
mkdir -p "$FEATURES_DIR"

# Backup existing features
if [ "$(ls -A $FEATURES_DIR/*.feature 2>/dev/null)" ]; then
    echo "Backing up existing features..."
    BACKUP_DIR="${FEATURES_DIR}_backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    cp "$FEATURES_DIR"/*.feature "$BACKUP_DIR/" 2>/dev/null || true
    echo "Backup created at: $BACKUP_DIR"
fi

# Extract features
echo "Extracting features to $FEATURES_DIR..."
unzip -o "$TEMP_ZIP" -d "$FEATURES_DIR"

echo "Features exported successfully!"
echo "Location: $FEATURES_DIR"
ls -lh "$FEATURES_DIR"/*.feature
