#!/usr/bin/env bash
# Delete the kind cluster created by deploy.sh.
#
# Usage: ./k8s/teardown.sh [--keep-cluster]
#   --keep-cluster   only delete the app namespace, leave the kind cluster up

set -euo pipefail

CLUSTER_NAME="pgcache-demo"
NAMESPACE="pgcache-demo"

if [[ "${1:-}" == "--keep-cluster" ]]; then
  echo "==> Deleting namespace '${NAMESPACE}' (keeping the kind cluster)"
  kubectl delete namespace "${NAMESPACE}" --ignore-not-found
else
  echo "==> Deleting kind cluster '${CLUSTER_NAME}'"
  kind delete cluster --name "${CLUSTER_NAME}"
fi

