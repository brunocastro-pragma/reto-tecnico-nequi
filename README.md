# Franchise API

Reactive REST API to manage a network of franchises, their branches, and the products of each branch.

Built with **Spring WebFlux** (RouterFunctions, no controllers), **hexagonal architecture**, and **R2DBC** over PostgreSQL. Every path through the application is non-blocking, from the HTTP socket down to the database driver.

---

## Table of contents

- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Trying the API](#trying-the-api)
- [API](#api)
- [Tests](#tests)
- [Resilience](#resilience)
- [Deploying to AWS](#deploying-to-aws)
- [Design decisions](#design-decisions)

---

## Architecture

Dependencies point inward, always. The domain knows nothing about Spring, about R2DBC, or about HTTP.

```
        ┌──────────────────── infrastructure ────────────────────┐
        │                                                        │
        │   entry-points/reactive-web      driven-adapters/      │
        │   Router + Handlers + DTOs       r2dbc-postgresql      │
        │            │                            │              │
        └────────────┼────────────────────────────┼──────────────┘
                     │ calls                      │ implements
                     ▼                            ▼
              ┌──────────────────────────────────────────┐
              │  domain/usecase       business rules     │
              │        │ uses                            │
              │        ▼                                 │
              │  domain/model         entities + PORTS   │
              │  (zero framework dependencies)           │
              └──────────────────────────────────────────┘
```

The rule is enforced by the build, not by discipline: `domain/model` only declares `reactor-core` in its `pom.xml`, so a domain entity *cannot* import Spring — the class is not on its classpath.

A request travels like this:

```
HTTP → Netty → RouterFunction → Handler → UseCase → Port ─┐
                                                          │
     ← ServerResponse ← Mono<Dto> ← Mono<Domain> ←────────┤
                                                          ▼
                                           RepositoryAdapter (implements the port)
                                                 │ CircuitBreaker + Retry + Timeout
                                                 ▼
                                           R2DBC → PostgreSQL
```

The whole trip is a single `Mono`/`Flux` chain. Nothing is computed until Netty subscribes, and there is no `.block()` anywhere in the codebase.

### Repository layout

```
domain/            the business core: entities, ports, use cases
infrastructure/    the adapters of the hexagon -- Java code
applications/      the runnable Spring Boot application
coverage-report/   aggregated coverage and the 70% gate
deployment/        the Dockerfile
terraform/         the AWS infrastructure
docs/              Postman collection
```

`infrastructure/` is the hexagonal layer — handlers and repository adapters. The cloud
infrastructure is a different thing entirely and lives in `terraform/`.

### Module layout

| Module | What lives there | May depend on |
|---|---|---|
| `domain/model` | Entities (`Franchise`, `Branch`, `Product`), ports, domain exceptions | Reactor only |
| `domain/usecase` | The nine use cases — the business rules | `model` |
| `infrastructure/entry-points/reactive-web` | `RouterFunction`, handlers, DTOs, validation, error handling | `usecase` |
| `infrastructure/driven-adapters/r2dbc-postgresql` | Repository adapters, R2DBC entities, resilience | `model` |
| `applications/app-service` | Composition root: bean wiring, configuration | all |
| `coverage-report` | Aggregated JaCoCo report and the coverage gate | all |

---

## Tech stack

| | |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.5 (WebFlux, Netty) |
| Persistence | PostgreSQL 16 over **R2DBC** (never JDBC — it blocks) |
| Resilience | Resilience4j 2.2.0 (reactive operators) |
| Docs | springdoc-openapi 2.6.0 |
| Tests | JUnit 5, Mockito, **StepVerifier**, WebTestClient, Testcontainers |
| Coverage | JaCoCo, gate at 90% lines |
| Build | Maven (multi-module) |
| Infrastructure | Docker, ECR, ECS Fargate, ALB, RDS, Secrets Manager, Terraform |

---

## Trying the API

The service runs on AWS. It has no local mode: the database is an RDS instance in a private
subnet, reachable only from the ECS tasks, so there is nothing to point a laptop at. To stand up
your own copy, follow [Deploying to AWS](#deploying-to-aws) — it takes one `terraform apply`.

Once deployed, `terraform output api_url` prints the base URL. Everything below assumes it is in
`$BASE`:

```bash
BASE=$(terraform -chdir=terraform/environments/dev output -raw api_url)/api/v1/franchises

# 1. Create a franchise
FRANCHISE=$(curl -s -X POST $BASE \
  -H 'Content-Type: application/json' \
  -d '{"name":"Nequi Foods"}' | jq -r .id)

# 2. Add two branches
CENTRO=$(curl -s -X POST $BASE/$FRANCHISE/branches \
  -H 'Content-Type: application/json' -d '{"name":"Centro"}' | jq -r .id)
POBLADO=$(curl -s -X POST $BASE/$FRANCHISE/branches \
  -H 'Content-Type: application/json' -d '{"name":"Poblado"}' | jq -r .id)

# 3. Add products
curl -s -X POST $BASE/$FRANCHISE/branches/$CENTRO/products \
  -H 'Content-Type: application/json' -d '{"name":"Espresso","stock":40}'
curl -s -X POST $BASE/$FRANCHISE/branches/$POBLADO/products \
  -H 'Content-Type: application/json' -d '{"name":"Mocha","stock":150}'

# 6. The product with the highest stock of each branch
curl -s $BASE/$FRANCHISE/top-stock-products | jq
```

Swagger UI is at `/swagger-ui.html` on the same host. The Postman collection in
[`docs/`](docs/postman_collection.json) has the same calls: set its `baseUrl` variable to the
deployed URL.

### Building the code

The build needs nothing but a JDK 17+ — the tests bring their own PostgreSQL through
Testcontainers, so they do not need the deployed database:

```bash
./mvnw clean verify
```

---

## API

Base path: `/api/v1`

| # | Method | Path | Description |
|---|---|---|---|
| 1 | `POST` | `/franchises` | Create a franchise |
| 2 | `POST` | `/franchises/{franchiseId}/branches` | Add a branch to a franchise |
| 3 | `POST` | `/franchises/{franchiseId}/branches/{branchId}/products` | Add a product to a branch |
| 4 | `DELETE` | `/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Remove a product |
| 5 | `PATCH` | `/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock` | Update a product's stock |
| 6 | `GET` | `/franchises/{franchiseId}/top-stock-products` | Highest-stock product of each branch |
| 7 | `PATCH` | `/franchises/{franchiseId}` | Rename a franchise |
| 8 | `PATCH` | `/franchises/{franchiseId}/branches/{branchId}` | Rename a branch |
| 9 | `PATCH` | `/franchises/{franchiseId}/branches/{branchId}/products/{productId}` | Rename a product |

Also: `GET /actuator/health` (used by the load balancer), `GET /swagger-ui.html`, `GET /v3/api-docs`.

### Errors

Every failure returns the same shape:

```json
{
  "timestamp": "2026-07-13T20:55:13.365Z",
  "status": 404,
  "error": "Not Found",
  "message": "Franchise with id [abc] was not found",
  "path": "/api/v1/franchises/abc"
}
```

| Status | When |
|---|---|
| 400 | Blank name, negative stock, malformed JSON |
| 404 | The franchise, branch or product does not exist — or does not belong to its parent |
| 409 | A franchise with that name already exists |
| 503 | The circuit breaker is open: the database is failing |
| 504 | The database did not answer within the timeout |

Nested routes are checked for ownership: renaming branch `B` under franchise `F` returns **404** if `B` belongs to a different franchise. Without that check, anyone could rename any branch in the system by guessing its id.

---

## Tests

```bash
./mvnw clean verify
```

| Layer | How it is tested |
|---|---|
| Use cases | JUnit 5 + Mockito with `StepVerifier` — 18 tests, happy paths and every error |
| Routers and handlers | `WebTestClient` against the real `RouterFunction` — 15 tests: status codes, JSON, error translation |
| R2DBC adapter | Testcontainers with a **real PostgreSQL** — the top-stock query uses `DISTINCT ON`, which only PostgreSQL has |
| Resilience | The circuit actually opens, the retry actually retries, the timeout actually fires |

No test calls `.block()`. Reactive flows are asserted with `StepVerifier`, which is the only way to assert on the *signals* — including "this Mono is empty" and "this Mono errors with X", neither of which `.block()` can express.

The aggregated coverage report lands in `coverage-report/target/site/jacoco-aggregate/index.html`, and the build **fails** below 90% line coverage.

> The Testcontainers test **skips itself** if it cannot reach a Docker daemon, so a machine without Docker still gets a green build. In CI, where Docker is always present, it runs for real.

---

## Resilience

All four patterns, applied at the boundary that can actually fail — the database.

They are composed as reactive operators in [`ResilienceDecorator`](infrastructure/driven-adapters/r2dbc-postgresql/src/main/java/co/com/nequi/franchise/r2dbc/resilience/ResilienceDecorator.java), not as annotations, so the behaviour is visible in the code instead of woven in by a proxy:

```java
return source
    .transformDeferred(TimeLimiterOperator.of(timeLimiter))     // bounds each attempt
    .transformDeferred(RetryOperator.of(retry))                 // retries with backoff
    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)); // records the final outcome
```

**The order is the point.** The circuit breaker is outermost so it records the outcome of the *call*, after retries. Invert the last two and it counts each retried attempt as a separate failure — and trips three times too early.

| Pattern | Configuration | Why those numbers |
|---|---|---|
| **Circuit Breaker** | window 20, min 10 calls, 50% failure rate, 10s open | A minimum sample so one failure during a deploy does not look like a 100% failure rate. Half the calls failing means the database is not slow, it is down. |
| **Retry** | 3 attempts, 200ms with exponential backoff | Only *transient* errors. Retrying immediately in a loop adds load to a database that is already struggling — that is how a slowdown becomes an outage. |
| **Timeout** | 2s | These are indexed lookups. A hung connection without a timeout holds the request open until the client gives up. |
| **Backpressure** | native | Reactive Streams: the subscriber calls `request(n)` and the publisher never emits more than that. It is preserved by not breaking the chain — the moment you `.block()` or collect everything into a list, it is gone. |

---

## Deploying to AWS

```
Internet → ALB (public subnets)
              │  health check /actuator/health
              ▼
         ECS Fargate (private subnets, 2–6 tasks, auto scaling)
              │  image from ECR · credentials from Secrets Manager
              ▼
         RDS PostgreSQL (private subnets, reachable only from the tasks)
```

The database password is never in the code, in the image, or in the task definition. RDS generates it, stores it in Secrets Manager and rotates it; the task reads it at startup with an IAM role scoped to that one secret ARN. **Terraform never sees it either** — which matters, because anything Terraform sees ends up in the state file.

### 1. Create the state backend (once per account)

```bash
terraform -chdir=terraform/bootstrap init
terraform -chdir=terraform/bootstrap apply
```

Copy the bucket name it prints into the `bucket` field of `terraform/environments/dev/backend.tf`.

### 2. Provision

```bash
cd terraform/environments/dev
terraform init
terraform apply -var="image_tag=1.0.0"
```

### 3. Push the image

```bash
ECR=$(terraform -chdir=terraform/environments/dev output -raw ecr_repository_url)
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR
docker build -f deployment/Dockerfile -t $ECR:1.0.0 .
docker push $ECR:1.0.0
```

### 4. Roll it out

```bash
aws ecs update-service \
  --cluster  $(terraform -chdir=terraform/environments/dev output -raw ecs_cluster_name) \
  --service  $(terraform -chdir=terraform/environments/dev output -raw ecs_service_name) \
  --force-new-deployment
```

The API is then at the URL printed by `terraform output api_url`.

### Terraform layout

```
terraform/
├── bootstrap/            S3 state bucket + DynamoDB lock table (run once)
├── modules/
│   ├── networking/       VPC, public/private subnets, NAT, chained security groups
│   ├── database/         RDS PostgreSQL, private, password managed by Secrets Manager
│   ├── ecr/              image registry, immutable tags, lifecycle policy
│   ├── iam/              execution role and task role, least privilege
│   ├── alb/              load balancer, target group, health check
│   └── ecs/              cluster, task definition, service, auto scaling
└── environments/
    ├── dev/              small, one NAT, no deletion protection
    └── prod/             Multi-AZ, NAT per AZ, bigger tasks, deletion protection
```

Both environments call the **same modules** with different numbers. Everything that differs between dev and prod is visible in one file per environment.

---

## Design decisions

Each of these was a fork in the road. The reasoning matters more than the choice.

**R2DBC, not JPA.** JDBC blocks the calling thread. WebFlux runs on a handful of event-loop threads (roughly one per core), so blocking one of them does not just slow down that request — it removes a slice of the whole application's concurrency. JPA on WebFlux is the contradiction the reactive stack exists to avoid. The price is real: R2DBC has no lazy loading and no dirty checking, so the aggregates are composed explicitly with `flatMap` and the ports distinguish `create` from `update`.

**`create` and `update` as separate port methods.** R2DBC has no persistence context, so a single `save()` has to guess whether the row exists — and it guesses from the id. Since ids are generated in the domain, every insert would look like an update and silently affect zero rows. Two methods, two explicit SQL statements. There is an integration test that fails if this is ever collapsed back into one.

**`DISTINCT ON` in SQL, not a reduction in Java.** Fetching every product to find the maximum per branch would mean either `java.util.stream` (blocking, and forbidden here) or dragging the entire table over the wire to answer a question the database answers with an index.

**Schema over R2DBC, not Flyway.** Flyway is JDBC. It would only run at startup, outside any request, so it would not block the event loop — but it would drag a JDBC driver and a `DataSource` into a service whose entire claim is that it has none. The honest trade-off: no migration history, no rollback. For three tables that is the right call; for a schema that evolves over years it would not be, and the answer there is a separate migration job, not a blocking driver inside the reactive service.

**`collectList()` in the top-stock handler.** Streaming a `Flux` straight into the response body commits the 200 status line before the first row is known — and the "franchise does not exist" error then arrives too late to become a 404. Collecting first lets the error surface while the response can still change. The cost is memory, and here it is bounded: one row per branch. For an unbounded stream the right answer is the opposite.

**Use cases wired in `UseCaseConfig`, not annotated with `@Service`.** Annotating them would put Spring on the domain's classpath, and the domain would no longer be testable — or reusable — without it. The cost is one configuration file. The benefit is that `domain/usecase` depends on exactly one thing: the model.

**A directory per environment, not Terraform workspaces.** Prod is not dev with a different name; it has different topology (Multi-AZ, a NAT per AZ, deletion protection). Workspaces express that through conditionals on `terraform.workspace` scattered across the code. Separate directories make the difference explicit and reviewable, at the cost of one more `main.tf`.

**Fargate, not EC2.** No instances to patch, scale or right-size, and billing is per vCPU-second. EC2 is cheaper per vCPU under sustained load and lets you keep a warm pool — for a service whose traffic is unknown, not operating a fleet is worth the premium.
