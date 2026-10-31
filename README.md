# Aegis Runtime

Runtime intelligence and operational analytics for the Aegis platform.

Aegis Runtime consumes telemetry events from Kafka, persists the event stream in PostgreSQL, reconstructs request timelines and service relationships, calculates runtime metrics, analyzes retries, and detects runtime anomalies. It exposes a REST API and dashboard-oriented endpoints for operational investigation.

Aegis Runtime is the analytics side of the Aegis system. Aegis Telemetry produces the events; Aegis Runtime consumes them. The systems are intentionally decoupled and communicate through the shared Kafka event contract.

## Release Status

| Item | Value |
| --- | --- |
| Current development version | `0.0.1-SNAPSHOT` |
| Target release | `1.0.0` |
| Java | 25 |
| Spring Boot | 4.0.5 |
| Persistence | PostgreSQL with JPA and Flyway |
| Messaging | Apache Kafka through Spring Kafka |
| Current Maven coordinates | `dev.nith:demo` |
| Package registry | GitHub Packages |
| Default application name | `demo` |

The current artifact id is `demo` for historical compatibility. Rename the coordinates to an intentional v1 identity, such as `com.aegis.runtime:aegis-runtime`, before publishing the first stable release if existing consumers do not depend on the current coordinates.

## Capabilities

- Kafka ingestion for runtime lifecycle, error, retry, and heartbeat events.
- Durable PostgreSQL storage for received runtime events.
- Trace timeline reconstruction by trace id.
- Service latency and aggregate runtime metrics.
- Service dependency and execution graph reconstruction.
- Runtime topology discovery.
- Retry amplification and retry behavior analysis.
- Configurable anomaly detection and anomaly persistence.
- Dashboard summaries and trend data.
- Runtime health reporting.
- Flyway-managed database migrations.
- Thin dependency JAR publication and a separate executable Spring Boot JAR.

## Architecture

```text
Aegis Telemetry
      |
      | Kafka events
      v
+-------------------+
| Kafka topics      |
| runtime.events    |
| runtime.errors    |
| runtime.retries   |
| runtime.heartbeats|
+---------+---------+
          |
          v
+-------------------+
| Kafka consumers   |
+---------+---------+
          |
          v
+-------------------+
| Event persistence |
| PostgreSQL        |
+---------+---------+
          |
          v
+-------------------------------+
| Analytics and intelligence    |
| metrics, traces, topology,    |
| graphs, retries, anomalies    |
+---------------+---------------+
                |
                v
        REST and dashboard APIs
```

### System boundary

Aegis Runtime does not require direct REST communication with Aegis Telemetry. It consumes the event contracts from Kafka. The producer and consumer must agree on event fields, event types, trace identifiers, and topic names.

## Main Components

| Component | Responsibility |
| --- | --- |
| `ingestor` | Kafka consumers for events, errors, retries, and heartbeats |
| `storage` | JPA entities, repositories, persistence services, and database access |
| `analytics` | Runtime-wide metrics and per-service latency analysis |
| `trace` | Request timeline queries by trace id |
| `graph` | Execution graph reconstruction by trace id |
| `topology` | Service dependency discovery |
| `retry` | Retry and retry-amplification analysis |
| `anomaly` | Threshold-based anomaly detection and persistence |
| `dashboard` | Summary, trend, and runtime health responses |
| `config` | Kafka and application configuration |
| `contracts` | Runtime-side event model used by consumers |

## Requirements

- JDK 25.
- Maven 3.9 or newer, or `mvnw.cmd` on Windows.
- PostgreSQL 14 or newer recommended.
- Apache Kafka compatible with the configured Spring Kafka client.
- Docker Desktop for the repository's local infrastructure.
- GitHub Packages credentials when publishing or consuming the Maven artifact.

Verify the Java and Maven installations:

```powershell
java -version
mvn -version
```

## Build and Test

Run the complete Maven build:

```powershell
mvn clean test
```

Build the application artifacts:

```powershell
mvn clean package
```

The build produces two useful runtime artifacts:

```text
target/demo-0.0.1-SNAPSHOT.jar
    Thin dependency artifact. Suitable as the main Maven artifact.

target/demo-0.0.1-SNAPSHOT-boot.jar
    Executable Spring Boot artifact for deployment.
```

The `boot` classifier is intentional. It keeps application classes visible in the main dependency JAR while attaching the repackaged executable separately.

## Local Infrastructure

Start the repository's infrastructure with Docker Compose:

```powershell
docker compose up --build -d
```

Stop the infrastructure:

```powershell
docker compose down
```

Before starting the application, verify that PostgreSQL and Kafka are reachable at the configured addresses. The current development configuration uses PostgreSQL at `localhost:5432` and Kafka at `localhost:19092`.

## Database Setup and Migrations

Flyway runs automatically at startup and applies migrations from:

```text
classpath:db/migration
```

Current migrations create:

- `runtime_events`: persisted telemetry events, trace identifiers, service identity, event type, status, latency, and payload.
- `runtime_anomalies`: detected anomaly id, trace id, service name, anomaly type, severity, description, detection time, and metadata.

The application validates the JPA schema against the Flyway-managed database. Do not edit an already-applied migration in a deployed environment. Add a new versioned migration instead.

## Configuration

The primary configuration file is `src/main/resources/application.yml`. Use environment-specific configuration or environment variables for deployment. Do not use the development database password from the checked-in local configuration in production.

Example production-oriented configuration:

```yaml
spring:
  application:
    name: aegis-runtime
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:aegis_runtime}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:19092}
    listener:
      auto-startup: ${KAFKA_LISTENER_AUTO_STARTUP:true}
      missing-topics-fatal: false
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP:runtime-intelligence-group}
      auto-offset-reset: earliest

server:
  port: ${SERVER_PORT:8080}

anomaly:
  high-latency-threshold-ms: ${ANOMALY_HIGH_LATENCY_THRESHOLD_MS:500}
  retry-amplification-threshold: ${ANOMALY_RETRY_AMPLIFICATION_THRESHOLD:1.5}
  thread-timeout-seconds: ${ANOMALY_THREAD_TIMEOUT_SECONDS:60}
  service-saturation-request-threshold: ${ANOMALY_SERVICE_SATURATION_REQUEST_THRESHOLD:1000}
```

### Important configuration guidance

- Set `DB_PASSWORD` through a secret manager or deployment secret, not source control.
- Keep `spring.jpa.hibernate.ddl-auto=validate` in production so schema drift fails visibly.
- Let Flyway own schema changes.
- Use a unique Kafka consumer group when creating an independent analytics deployment.
- Set `KAFKA_LISTENER_AUTO_STARTUP=false` when running API-only tests that do not have Kafka available.
- Tune anomaly thresholds using observed traffic and business service-level objectives.
- Disable SQL logging in production unless temporarily troubleshooting a controlled issue.

## Kafka Ingestion

The application consumes the following topics with the configured consumer group:

| Topic | Consumer | Purpose |
| --- | --- | --- |
| `runtime.events` | `RuntimeEventConsumer` | Request and service lifecycle events |
| `runtime.errors` | `ErrorEventConsumer` | Error and exception events |
| `runtime.retries` | `RetryEventConsumer` | Retry and recovery events |
| `runtime.heartbeats` | `HeartbeatConsumer` | Service liveness events |

Each consumer persists the received runtime event before it becomes available to the analytics services.

The expected event stream is produced by Aegis Telemetry. Configure both systems with compatible topic names and event contract versions.

## Run the Application

Run directly with Maven:

```powershell
mvn spring-boot:run
```

Build and run the executable artifact:

```powershell
mvn clean package
java -jar target\demo-0.0.1-SNAPSHOT-boot.jar
```

Override the port for a local instance:

```powershell
java -jar target\demo-0.0.1-SNAPSHOT-boot.jar --server.port=8081
```

The default HTTP port is `8080` unless overridden by configuration.

## REST API

The runtime API is currently exposed under `/runtime`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/runtime/metrics` | Returns aggregate runtime metrics |
| `GET` | `/runtime/services/{serviceName}/latency` | Returns latency metrics for one service |
| `GET` | `/runtime/anomalies` | Runs anomaly detection and returns persisted anomaly results |
| `GET` | `/runtime/health` | Returns runtime health information |
| `GET` | `/runtime/timeline/{traceId}` | Returns the ordered timeline for a trace |
| `GET` | `/runtime/graph/{traceId}` | Returns the execution graph for a trace |
| `GET` | `/runtime/topology` | Returns discovered service dependencies |
| `GET` | `/runtime/retries` | Returns retry analysis |
| `GET` | `/runtime/dashboard` | Returns the dashboard summary |
| `GET` | `/runtime/dashboard/trends` | Returns trend data; accepts an optional `window` query parameter |

Examples:

```powershell
Invoke-RestMethod http://localhost:8080/runtime/metrics
Invoke-RestMethod http://localhost:8080/runtime/health
Invoke-RestMethod http://localhost:8080/runtime/topology
Invoke-RestMethod http://localhost:8080/runtime/dashboard/trends?window=24h
Invoke-RestMethod http://localhost:8080/runtime/timeline/<trace-id>
```

A trace id must exist in the persisted event stream before timeline and execution graph queries can return useful results. The anomaly endpoint evaluates the current persisted data when requested.

## Operational Behavior

### Metrics and latency

Runtime metrics aggregate data from persisted events. Per-service latency queries isolate service behavior and return `404 Not Found` when the service has no known metrics.

### Timelines and execution graphs

Timeline and graph queries use the trace id and the event relationships emitted by Aegis Telemetry. Correct trace and span propagation is required for complete reconstruction.

### Topology

The topology service infers service dependencies from observed service-call events. An empty topology can be a valid result when no service-call events have been ingested.

### Anomaly detection

Anomalies are evaluated from configured thresholds such as:

- High-latency threshold.
- Retry amplification threshold.
- Thread timeout.
- Service saturation request threshold.

Detected anomalies are persisted in PostgreSQL so they can be inspected with the rest of the runtime intelligence data.

## Health and Operations

For production deployments:

1. Run PostgreSQL and Kafka as highly available services.
2. Provide credentials through secret management and rotate them according to organizational policy.
3. Restrict the runtime API to authenticated operators or an internal network.
4. Add authentication and authorization before exposing analytics endpoints outside a trusted boundary.
5. Monitor Kafka consumer lag, listener errors, database connectivity, Flyway status, event ingestion rate, and anomaly persistence failures.
6. Use a separate Kafka consumer group for each independently operated analytics deployment.
7. Back up PostgreSQL and test restoration of both runtime events and anomalies.
8. Treat event schema changes and database migrations as compatibility-sensitive releases.
9. Keep the executable `-boot.jar` separate from the thin dependency artifact in deployment automation.
10. Establish retention and deletion policies for telemetry payloads and trace data.

## API Security Note

The current controllers expose analytics endpoints without an authentication layer in this application module. They are suitable for trusted local or internal deployments only until authentication, authorization, rate limiting, and audit logging are added.

## Package Installation

Aegis Runtime is published to GitHub Packages:

```text
https://maven.pkg.github.com/Nithish-1622/Aegis-Runtime
```

Current development coordinates:

```text
dev.nith:demo:0.0.1-SNAPSHOT
```

Configure `%USERPROFILE%\.m2\settings.xml` on Windows with a GitHub server whose id is `github`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

Add the package repository and dependency to another Maven project:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Nithish-1622/Aegis-Runtime</url>
    </repository>
</repositories>

<dependency>
    <groupId>dev.nith</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The main artifact is the thin JAR. The executable artifact uses the `boot` classifier and should be run as an application, not used as a compile dependency:

```xml
<dependency>
    <groupId>dev.nith</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <classifier>boot</classifier>
</dependency>
```

In most projects, depend on the thin artifact or deploy the `-boot.jar`; do not use the `boot` classifier as an ordinary library dependency.

## Publishing

Local publication requires GitHub Packages credentials and a clean working tree:

```powershell
Set-Location K:\Aegis-runtime
mvn clean deploy
```

The repository workflow in `.github/workflows/publish.yml` publishes on:

- Pushes to `main`.
- Version tags such as `v1.0.0`.
- Manual `workflow_dispatch` runs.

GitHub Actions uses `GITHUB_TOKEN` with package write permission and verifies that the uploaded thin artifact can be resolved from an empty Maven repository.

For a release:

```powershell
git add .
git commit -m "Prepare Aegis Runtime release"
git push origin main
git tag v1.0.0
git push origin v1.0.0
```

Before the first stable release:

1. Decide whether to retain `dev.nith:demo` or rename the coordinates.
2. Align the Runtime event contract with the published Aegis Telemetry contracts.
3. Replace development database credentials with environment-backed secrets.
4. Run `mvn clean test`.
5. Run an integration test with Kafka and PostgreSQL.
6. Verify all documented endpoints against representative telemetry data.
7. Test a clean Maven consumer project against the published artifact.
8. Publish release notes covering API, event, schema, and configuration changes.

## Compatibility

Aegis Runtime currently uses Spring Boot 4.0.5 and Java 25. Aegis Telemetry currently uses Spring Boot 3.5.0 and Java 25. The systems interoperate through Kafka contracts, but their application dependency graphs should not be merged casually. Keep each application on its own Spring Boot dependency management unless a deliberate compatibility upgrade has been completed.

## Repository Layout

```text
src/main/java/com/aegis/runtime/
  analytics/       Runtime metrics and service latency
  anomaly/         Anomaly rules, detection, and persistence
  config/          Kafka and application configuration
  contracts/       Runtime-side event contract model
  dashboard/       Dashboard summary, trends, and health
  graph/           Trace execution graph reconstruction
  ingestor/        Kafka event consumers
  retry/           Retry analytics
  storage/         JPA persistence and repositories
  topology/        Service dependency discovery
  trace/           Trace timeline queries

src/main/resources/
  application.yml
  db/migration/    Flyway schema migrations
```

## License and Ownership

This project is part of the Aegis platform suite. Package ownership, licensing, and redistribution terms should be finalized before the `v1.0.0` public release.
