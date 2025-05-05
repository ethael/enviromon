#!/bin/bash

SERVER_URL="http://localhost:8080"

# Test upgrade endpoint
echo "Testing /upgrade..."
curl -X GET -s -w "%{http_code}\n" "$SERVER_URL/upgrade" -H "User-Agent: ENVIROMON/101 OFFICE"

# Test creating a sensor entry
echo -e "\nTesting /sensors (POST)..."
curl -X POST -s -w "%{http_code}\n" "$SERVER_URL/sensors" -H "Content-Type: application/json" -H "User-Agent: ENVIROMON/101 OFFICE" -d '{
    "device": "OFFICE",
    "temperature": 22.5,
    "pressure": 1012,
    "humidity": 45.2,
    "light": 150,
    "power": 220,
    "battery": 90
}'

# Test retrieving sensors since a certain date
echo -e "\nTesting /sensors (GET)..."
curl -X GET "$SERVER_URL/sensors?since=2025-05-01T00:00:00" -H "User-Agent: ENVIROMON/101 OFFICE"

# Test retrieving last sensor entry for a specific device
echo -e "\n\nTesting /sensors/last (GET)..."
curl -X GET "$SERVER_URL/sensors/last?uid=OFFICE" -H "User-Agent: ENVIROMON/101 OFFICE"

echo -e "\n\nAll tests executed!"

