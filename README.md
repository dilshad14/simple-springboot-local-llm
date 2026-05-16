# simple-springboot-local-llm
This simple POC demonstrate usage of Springboot API's integration with LOcally running LLM model, Ollama runtime with gemma3:1b model



## Setup ollama and get it running

1. visit the site and download the binary : https://ollama.com/download/linux

2. Commands for execution
```
ollama serve

ollama run gemma3:1b

```
3. This would start ollama runtime on http://localhost:11434

## Observability (Grafana + Prometheus + OTel Collector + Tempo)

| Store | Purpose | Grafana datasource |
|-------|---------|-------------------|
| **Prometheus** | Metrics (latency, JVM, HTTP counts) | **Prometheus** (default) |
| **Tempo** | Trace spans (waterfall, `@Observed` tools) | **Tempo** |
| **OTel Collector** | Receives OTLP traces from the app → forwards to Tempo | (not queried directly) |

Prometheus does **not** store spans. The collector does **not** expose app metrics unless you scrape Spring Boot (configured below).

### 1. Start the stack

```bash
docker compose -f observability/docker-compose.yml up -d
```

| Service | URL |
|---------|-----|
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| OTLP traces | http://localhost:4318/v1/traces |

### 2. Verify Prometheus targets

Open http://localhost:9090/targets. All three jobs should show **UP (1/1)**:

| Job | Endpoint | Requires |
|-----|----------|----------|
| **spring-boot** | `host.docker.internal:8082/actuator/prometheus` | App running on port **8082** with `observability` profile |
| **otel-collector** | `otel-collector:8888/metrics` | Collector config binds metrics to `0.0.0.0` (see below) |
| **prometheus** | `localhost:9090/metrics` | Prometheus container running |

**otel-collector DOWN** (`connection refused` on `:8888`):

OTel Collector v0.111+ exposes self-metrics on **`localhost:8888` by default**, which other Docker containers cannot reach. This repo sets `service.telemetry.metrics.readers` in `observability/otel-collector-config.yaml` to listen on **`0.0.0.0:8888`**.

After changing that file, recreate the collector (config is read only at startup):

```bash
docker compose -f observability/docker-compose.yml up -d --force-recreate otel-collector
```

Confirm in logs (`docker logs observability-otel-collector-1`):

```
Serving metrics  {"address": "0.0.0.0:8888", "metrics level": "Detailed"}
```

If you still see `localhost:8888` and **Normal**, the running container is using an old config — run the recreate command above.

### 3. Run the app (with Prometheus + tracing)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=observability
```

Verify metrics locally: http://localhost:8082/actuator/prometheus (should show `jvm_`, `http_server_`, etc.)

### 4. Generate traffic

```bash
curl "http://localhost:8082/ollama-assistant?message=What%20time%20is%20it%3F"
```

### 5. Metrics in Grafana

**Use the provisioned dashboard (recommended):**

1. **Dashboards** → folder **Observability** → **Spring Boot LLM (Prometheus)**.
2. Datasource must be **Prometheus** (panels use `job="spring-boot"` and `application="langchain4j-llm"`).

**Tempo dashboards showing “0 series returned”:** built-in Tempo / RED dashboards query span metrics such as `traces_spanmetrics_calls_total`. Those are produced by **Tempo’s metrics-generator** (remote-written to Prometheus), not by Spring Boot directly. This stack enables that in `observability/tempo.yaml`. You still need **traces** in Tempo first (see §6): restart the app after adding `opentelemetry-exporter-otlp`, run with `observability` profile, send a few requests, wait ~1 minute, then refresh.

**Explore → Prometheus** (not Tempo) for ad-hoc PromQL:

```promql
up{job="spring-boot"}
```

```promql
rate(http_server_requests_seconds_count{job="spring-boot",application="langchain4j-llm",uri!="/actuator/prometheus"}[5m])
```

Percentiles need histogram buckets (enabled in `application-observability.properties`):

```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job="spring-boot",application="langchain4j-llm"}[5m])) by (le, uri))
```

If `up{job="spring-boot"}` is empty → see troubleshooting below.

### 6. Traces in Grafana (Tempo)

1. Rebuild and run with **`observability` profile** (OTLP export requires `opentelemetry-exporter-otlp` on the classpath).
2. Generate traffic (`curl` to `/ollama-assistant`).
3. **Explore** → datasource **Tempo** → **Search** → service: `LangChain4j Spring Boot Example`.

Check traces exist: http://localhost:3200/api/search?limit=5 (should list trace IDs).

### Troubleshooting “0 series returned”

| Cause | Fix |
|-------|-----|
| **Tempo** dashboard / RED panels with PromQL | Use **Spring Boot LLM (Prometheus)** dashboard, or send traces and wait for `traces_spanmetrics_*` in Prometheus |
| Querying **Tempo** datasource with PromQL | Use **Prometheus** datasource for metrics |
| `http_server_requests_seconds_bucket` empty | Use `observability` profile (enables percentile histograms); restart the app |
| App run without **`observability` profile** | `./mvnw spring-boot:run -Dspring-boot.run.profiles=observability` or VS Code: `--spring.profiles.active=observability` |
| No traces in Tempo | Add dependency `opentelemetry-exporter-otlp`; OTLP endpoint `http://localhost:4318/v1/traces` |
| **Prometheus** container missing / not running | `docker compose -f observability/docker-compose.yml up -d` (includes `prometheus` service) |
| Target **spring-boot** DOWN in Prometheus | Start app on port **8082** with `observability` profile before scraping |
| Target **otel-collector** DOWN (`connection refused` on `:8888`) | Ensure `otel-collector-config.yaml` binds `0.0.0.0:8888`; recreate collector (see **Verify targets** above) |
| No `/actuator/prometheus` | Use `observability` profile; dependency `micrometer-registry-prometheus` |
| `host.docker.internal` fails (Linux) | `extra_hosts` is in compose; or set target to your host IP in `prometheus.yml` |

### 7. Stop stack

```bash
docker compose -f observability/docker-compose.yml down
```