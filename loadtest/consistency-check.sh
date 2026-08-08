#!/usr/bin/env bash
# Demonstrates that PgCache invalidates via CDC in near real time, rather than
# serving stale data for a fixed TTL window like a naive cache would.
#
# Usage: ./consistency-check.sh [product_id] [new_price]

set -euo pipefail

CACHED_URL="${CACHED_URL:-http://localhost:8082}"
PRODUCT_ID="${1:-42}"
NEW_PRICE="${2:-199.99}"

echo "Reading product $PRODUCT_ID through the cached path ($CACHED_URL)..."
curl -s "$CACHED_URL/api/products/$PRODUCT_ID" | python3 -m json.tool

echo
echo "Updating price directly on the origin Postgres (bypassing PgCache)..."
docker compose exec -T postgres psql -U demo -d pgcache_demo \
  -c "UPDATE products SET price = $NEW_PRICE WHERE id = $PRODUCT_ID;"

echo
echo "Polling the cached path until the new price shows up..."
START=$(date +%s.%N)
for _ in $(seq 1 50); do
  PRICE=$(curl -s "$CACHED_URL/api/products/$PRODUCT_ID" \
    | python3 -c "import sys, json; print(json.load(sys.stdin)['price'])")
  NOW=$(date +%s.%N)
  ELAPSED=$(echo "$NOW - $START" | bc)
  echo "  t=+${ELAPSED}s  price=$PRICE"
  if [ "$PRICE" = "$NEW_PRICE" ]; then
    echo
    echo "Price updated after ~${ELAPSED}s via CDC -- no cache eviction code required."
    exit 0
  fi
  sleep 0.1
done

echo "Price did not update within the polling window -- check pgcache logs."
exit 1
