# API de Franquicias

API REST reactiva para gestionar franquicias, sus sucursales y los productos de cada sucursal. Hecha con Spring WebFlux (sin controladores, todo con `RouterFunctions`), arquitectura hexagonal y R2DBC sobre PostgreSQL. No hay ni un punto bloqueante en el flujo.

## Cómo probarla

Está desplegada en AWS. Lo más fácil es abrir el Swagger o usar la colección de Postman de [`docs/`](docs/postman_collection.json).

- **Swagger:** `http://franchise-api-dev-alb-1393361058.us-east-1.elb.amazonaws.com/swagger-ui.html`

Ojo que es `http`, no `https` (el balanceador solo tiene el puerto 80; no monté TLS porque necesitaría un dominio). Si el navegador te fuerza a https, escribe el `http://` a mano.

No tiene modo local: la base de datos es un RDS en subred privada, así que desde un portátil no se puede alcanzar (es a propósito, por seguridad). Para levantar tu propia copia están los pasos en [`terraform/`](terraform/) — es un `terraform apply`.

## Compilar y correr pruebas

Solo necesitas un JDK 17. Las pruebas levantan su propio PostgreSQL con Testcontainers, no hace falta nada más:

```bash
./gradlew build
```

Corre las 66 pruebas y falla si la cobertura baja del 90%. El reporte queda en `build/reports/jacoco/aggregate/index.html`.

## Los endpoints

Todo cuelga de `/api/v1`:

- `POST /franchises` — crear franquicia
- `POST /franchises/{id}/branches` — agregar sucursal
- `POST /franchises/{id}/branches/{id}/products` — agregar producto
- `DELETE /franchises/{id}/branches/{id}/products/{id}` — eliminar producto
- `PATCH .../products/{id}/stock` — cambiar stock
- `GET /franchises/{id}/top-stock-products` — el producto con más stock de cada sucursal
- `PATCH` para renombrar franquicia, sucursal o producto

## Stack

Java 17, Spring Boot 3.3 (WebFlux), R2DBC + PostgreSQL 16, Resilience4j (circuit breaker, retry, timeout), Gradle multi-módulo siguiendo el scaffold de Bancolombia, Docker, y AWS (ECS Fargate, ALB, RDS, ECR, Secrets Manager) con Terraform.

## Estructura

```
domain/           entidades, puertos y casos de uso (sin frameworks)
infrastructure/   adaptadores: web (router + handlers) y R2DBC
applications/     el arranque de Spring Boot
terraform/        la infraestructura de AWS
deployment/       el Dockerfile
```

La regla es que las dependencias apuntan hacia adentro: el dominio no sabe nada de Spring ni de la base de datos. Y no la sostiene la disciplina, la fuerza el build — `domain/model` no tiene Spring en el classpath, así que una entidad no lo puede importar aunque quisiera.
