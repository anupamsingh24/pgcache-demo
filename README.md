# pgcache-demo

A side-by-side demo of a Spring Boot app with and without [PgCache](https://www.pgcache.com) — a
transparent caching proxy for PostgreSQL. Both app instances run the **exact same jar**; the only
difference is which JDBC URL they're pointed at.

```
                       ┌────────────────┐
                       │ Load generator │
                       │      (k6)      │
                       └───────┬────────┘
                  ┌────────────┴────────────┐
                  ▼                         ▼
         ┌──────────────────┐     ┌──────────────────┐
         │  app-baseline     │     │  app-cached       │
         │  (direct JDBC)    │     │  (via PgCache)    │
         └─────────┬─────────┘     └─────────┬─────────┘
                   │                         ▼
                   │                ┌──────────────────┐
                   │                │  PgCache proxy    │
                   │                │  (caches + CDC)   │
                   │                └─────────┬─────────┘
                   ▼                          ▼
              ┌───────────────────────────────────┐
              │        PostgreSQL (origin)          │
              └───────────────────────────────────┘
```

## What's in here

- `app/` — one Spring Boot service (Java 17, plain JDBC via `JdbcTemplate` — deliberately not JPA/Hibernate,
  so the SQL hitting Postgres is exactly what's written below, matching PgCache's documented cacheable
  query patterns).
- `db/init.sql` — schema + seed data (~50k products, ~5k customers, ~200k orders, ~400k order items).
- `docker-compose.yml` — origin Postgres, PgCache proxy, two app instances, Prometheus, Grafana,
  `postgres_exporter`.
- `monitoring/prometheus.yml` — scrape config for both app instances, PgCache's metrics endpoint, and
  the origin DB's exporter.
- `loadtest/` — k6 scripts (single-endpoint hammer test + a realistic mixed workload) and a bash script
  that demonstrates cache invalidation via CDC in real time.

## Endpoints (identical on both instances, different port)

| Endpoint | Query shape it exercises |
|---|---|
| `GET /api/products/{id}` | single-table SELECT |
| `GET /api/products?category=&sort=&page=&size=` | WHERE + ORDER BY + LIMIT/OFFSET |
| `GET /api/products/search?q=` | ILIKE pattern match |
| `GET /api/orders/{id}` | INNER JOIN |
| `GET /api/orders/{id}/items` | INNER JOIN |
| `GET /api/analytics/revenue-by-category` | multi-way JOIN + GROUP BY + SUM |
| `GET /api/analytics/top-products?limit=` | window function (`RANK() OVER`) |
| `PATCH /api/products/{id}/price` | write — triggers CDC invalidation on the cached path |
| `POST /api/orders` | write |

`app-baseline` → `http://localhost:8081`
`app-cached` → `http://localhost:8082`

## Prerequisites

- Docker + Docker Compose
- [k6](https://k6.io/docs/get-started/installation/) installed locally (or run it via
  `docker run --rm -i grafana/k6 run -` piping in a script)
- `psql` and `python3` on your host if you want to run `loadtest/consistency-check.sh` as written

## Running it

```bash
docker compose up -d --build
```

First boot seeds the origin database (a few hundred thousand rows) — give it 30–60s. Check:

```bash
curl http://localhost:8081/api/products/1   # app-baseline, direct Postgres
curl http://localhost:8082/api/products/1   # app-cached, via PgCache
```

Confirm PgCache is actually caching:

```bash
curl http://localhost:9090/metrics | grep pgcache_queries
```

## Running the load tests

```bash
# single endpoint, latency distribution
k6 run --env BASE_URL=http://localhost:8081 loadtest/hammer-single-endpoint.js   # baseline
k6 run --env BASE_URL=http://localhost:8082 loadtest/hammer-single-endpoint.js   # cached

# realistic mixed workload
k6 run --env BASE_URL=http://localhost:8081 loadtest/mixed-workload.js
k6 run --env BASE_URL=http://localhost:8082 loadtest/mixed-workload.js
```

Watch `postgres-exporter`'s metrics (via Prometheus/Grafana, see below) during the baseline run vs the
cached run — that's the "cut read-replica load" story.

## The consistency demo

This is the one that actually differentiates PgCache from a TTL cache — it stays fresh via logical
replication instead of going stale for a fixed window:

```bash
chmod +x loadtest/consistency-check.sh
./loadtest/consistency-check.sh 42 199.99
```

It reads product 42's price through the cached path, updates it directly on the origin (bypassing
PgCache entirely), then polls the cached path until the new price shows up — typically sub-second.

## Metrics

```bash
docker compose up -d prometheus grafana
```

- Prometheus: http://localhost:9091
- Grafana: http://localhost:3000 (anonymous access enabled, admin/admin if you want to log in)

Useful metrics to graph side by side:
- `http_server_requests_seconds` (tagged by `application=app-baseline` vs `app-cached`) — latency
- `pgcache_queries_cache_hit` / `pgcache_queries_cache_miss` — cache effectiveness
- `pgcache_cdc_lag_seconds` — how fresh the cache is
- `pg_stat_activity_count` (from `postgres_exporter`) — origin DB load, baseline vs cached

## Next steps / stretch goals

- **Add a naive third leg.** Same app, `@Cacheable` + Caffeine with a TTL, pointed at origin Postgres
  directly. It'll look just as fast as PgCache in the latency chart — until you run the consistency
  demo against it and watch it serve a stale price for the length of the TTL. That contrast is the
  sharpest part of the story for a write-up.
- **RLS / view tables** aren't cached by PgCache yet — worth a callout if you write this up, along with
  noting PgCache is a fairly young project.
- Import a Grafana dashboard JSON with the panels above pre-built (not included here — build it once,
  export it, and it becomes reusable for future demos).
