# Running pgcache-demo on Kubernetes (kind)

This is a Kubernetes port of the `docker-compose.yml` stack, targeted at a local
[kind](https://kind.sigs.k8s.io/) cluster. Every Compose service maps to a
Deployment + Service in the `pgcache-demo` namespace.

| Compose service     | K8s workload            | In-cluster DNS         | Host port (via kind) |
|---------------------|-------------------------|------------------------|----------------------|
| `postgres`          | Deployment + PVC        | `postgres:5432`        | `localhost:5433`     |
| `pgcache`           | Deployment              | `pgcache:5432` / `:9090` | `localhost:5432`   |
| `postgres-exporter` | Deployment              | `postgres-exporter:9187` | –                  |
| `app-baseline`      | Deployment              | `app-baseline:8080`    | `localhost:8081`     |
| `app-cached`        | Deployment              | `app-cached:8080`      | `localhost:8082`     |
| `prometheus`        | Deployment + ConfigMap  | `prometheus:9090`      | `localhost:9091`     |
| `grafana`           | Deployment + ConfigMaps | `grafana:3000`         | `localhost:3000`     |

Host ports are wired up through `NodePort` services plus `extraPortMappings` in
`cluster/kind-cluster.yaml`, so the endpoints stay identical to the Compose setup.

## Prerequisites

- Docker
- [kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- `kubectl`

## Deploy

```bash
./k8s/deploy.sh
```

This creates the cluster, builds the Spring Boot image, loads it into the kind
node (kind can't pull `pgcache-demo-app:latest` from a registry — it has to be
loaded), applies every manifest, and waits for rollout. First boot seeds ~650k
rows into Postgres, so give it a minute.

Then, exactly like the Compose version:

```bash
curl http://localhost:8081/api/products/1   # app-baseline, direct Postgres
curl http://localhost:8082/api/products/1   # app-cached, via PgCache
```

Grafana: http://localhost:3000 · Prometheus: http://localhost:9091

## Load tests

The k6 scripts work unchanged — they read `BASE_URL`:

```bash
k6 run --env BASE_URL=http://localhost:8081 loadtest/hammer-single-endpoint.js
k6 run --env BASE_URL=http://localhost:8082 loadtest/hammer-single-endpoint.js
```

## Consistency demo

`loadtest/consistency-check.sh` uses `docker compose exec` to update the origin
DB, which doesn't apply here. Run the update against the origin Postgres directly
instead (mapped to `localhost:5433`):

```bash
# read through the cached path
curl -s http://localhost:8082/api/products/42

# update the origin directly (bypassing pgcache)
PGPASSWORD=demo psql -h localhost -p 5433 -U demo -d pgcache_demo \
  -c "UPDATE products SET price = 199.99 WHERE id = 42;"

# poll the cached path until CDC refreshes it
watch -n0.2 'curl -s http://localhost:8082/api/products/42'
```

Or run the update via `kubectl exec` into the postgres pod:

```bash
kubectl -n pgcache-demo exec deploy/postgres -- \
  psql -U demo -d pgcache_demo -c "UPDATE products SET price = 199.99 WHERE id = 42;"
```

## Rebuilding the app after a code change

```bash
docker build -t pgcache-demo-app:latest ./app
kind load docker-image pgcache-demo-app:latest --name pgcache-demo
kubectl -n pgcache-demo rollout restart deployment/app-baseline deployment/app-cached
```

## Tear down

```bash
./k8s/teardown.sh                 # deletes the whole kind cluster
./k8s/teardown.sh --keep-cluster  # only removes the pgcache-demo namespace
```

## Manifest layout

Files are numbered so `kubectl apply -f k8s/` applies them in dependency order:

```
00-namespace.yaml          namespace
05-secrets.yaml            DB credentials (Secret)
10-postgres.yaml           init.sql ConfigMap + PVC + Deployment + Service
20-pgcache.yaml            caching proxy
30-postgres-exporter.yaml  DB metrics exporter
40-app-baseline.yaml       app pointed straight at Postgres
50-app-cached.yaml         app pointed at pgcache
60-prometheus.yaml         scrape config + Deployment + Service
70-grafana.yaml            datasource/dashboard provisioning + Deployment + Service
cluster/kind-cluster.yaml  kind cluster definition (host port mappings; not a kubectl resource)
deploy.sh / teardown.sh    lifecycle helpers
```

> **Note:** `cluster/kind-cluster.yaml` is a *kind CLI* config consumed by
> `kind create cluster --config`, **not** a Kubernetes manifest. It lives in a
> subdirectory on purpose so that `kubectl apply -f k8s/` (which is non-recursive)
> never tries to apply it — doing so fails with
> `no matches for kind "Cluster" in version "kind.x-k8s.io/v1alpha4"`.

