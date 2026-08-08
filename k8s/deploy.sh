#!/usr/bin/env bash
# Spin up the whole pgcache-demo stack on a local kind cluster.
#
# Steps:
#   1. create the kind cluster (with host port mappings)
#   2. build the Spring Boot app image
#   3. load that image into the kind node (kind can't pull local images)
#   4. apply all manifests
#   5. wait for everything to become ready
#
# Usage: ./k8s/deploy.sh

set -euo pipefail

CLUSTER_NAME="pgcache-demo"
IMAGE="pgcache-demo-app:latest"
NAMESPACE="pgcache-demo"

# Resolve repo paths relative to this script so it works from anywhere.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "==> Checking prerequisites"
for bin in kind kubectl docker; do
  command -v "$bin" >/dev/null 2>&1 || { echo "ERROR: '$bin' is required but not installed."; exit 1; }
done

echo "==> Creating kind cluster '${CLUSTER_NAME}' (if it doesn't exist)"
if ! kind get clusters | grep -qx "${CLUSTER_NAME}"; then
  kind create cluster --config "${SCRIPT_DIR}/cluster/kind-cluster.yaml"
else
  echo "    cluster already exists, reusing it"
fi

echo "==> Building app image '${IMAGE}'"
docker build -t "${IMAGE}" "${REPO_ROOT}/app"

echo "==> Loading image into kind"
kind load docker-image "${IMAGE}" --name "${CLUSTER_NAME}"

echo "==> Applying manifests"
kubectl apply -f "${SCRIPT_DIR}/00-namespace.yaml"
kubectl apply -f "${SCRIPT_DIR}/"

echo "==> Waiting for Postgres (this seeds ~650k rows on first boot, be patient)"
kubectl -n "${NAMESPACE}" rollout status deployment/postgres --timeout=300s

echo "==> Waiting for the rest of the stack"
for dep in pgcache postgres-exporter app-baseline app-cached prometheus grafana; do
  kubectl -n "${NAMESPACE}" rollout status "deployment/${dep}" --timeout=300s
done

cat <<'EOF'

==> Done. Endpoints (mapped to the host via kind):

  app-baseline : http://localhost:8081
  app-cached   : http://localhost:8082
  prometheus   : http://localhost:9091
  grafana      : http://localhost:3000   (anonymous access; admin/admin)

  origin postgres : localhost:5433  (psql -h localhost -p 5433 -U demo -d pgcache_demo)
  cached path     : localhost:5432  (via pgcache)

Quick check:
  curl http://localhost:8081/api/products/1
  curl http://localhost:8082/api/products/1

Tear it all down with:
  ./k8s/teardown.sh
EOF

