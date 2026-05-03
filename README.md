# RentFlow Backend

Spring Boot backend for RentFlow, organized as a multi-module Maven project.

## Requirements

- Java 21
- Docker Desktop
- Maven wrapper from this repository (`./mvnw`)

## Run Locally

Start Postgres:

```bash
docker compose up -d postgres
```

Build and install the local modules:

```bash
./mvnw -DskipTests install
```

Run the application from the bootstrap module:

```bash
cd rentflow-bootstrap
../mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

## Database

The local Postgres container is published on host port `15432` to avoid collisions with a native Postgres running on `5432`.

```text
Host: 127.0.0.1
Port: 15432
Database: rentflow
Username: rentflow
Password: rentflow
```

The Spring datasource is configured in `rentflow-bootstrap/src/main/resources/application.yml`:

```text
jdbc:postgresql://127.0.0.1:15432/rentflow
```

Flyway runs automatically at application startup.

## Smoke Test

Login with the default development admin:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@rentflow.com","password":"changeme"}'
```

## Insomnia Collection

Import `insomnia-rentflow-backend.json` into Insomnia to get the full backend API collection.

The collection includes a `Local Development` environment with:

- `base_url`: `http://localhost:8080`
- `admin_email`: `admin@rentflow.com`
- `admin_password`: `changeme`
- `access_token`: used by every protected endpoint

Run the `Auth / Login` request first. Recent Insomnia versions should copy `accessToken` into `access_token` automatically; otherwise copy it from the login response into the environment variable manually.

## Useful Commands

Check Postgres:

```bash
docker compose ps
```

Stop Postgres:

```bash
docker compose down
```

Reset the local database:

```bash
docker compose down -v
docker compose up -d postgres
```

Run the Maven build without tests:

```bash
./mvnw -pl rentflow-bootstrap -am -DskipTests package
```
