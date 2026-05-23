# pcast-api-kt

`pcast-api-kt` is a Kotlin/Ktor REST API for podcast feed management. It includes JWT authentication, refresh-token rotation, passkey/WebAuthn support, OPML feed import, and a few sync helper endpoints used by pcast clients.

## Tech Stack

- Kotlin 2.3.21
- Ktor 3.5.0 with Netty
- Gradle wrapper 9.5.1 with Kotlin DSL
- Koin annotations and KSP for dependency injection
- Exposed ORM with PostgreSQL
- Flyway database migrations
- kotlinx serialization for JSON and XML
- Yubico WebAuthn server library for passkeys
- Spotless with ktlint for formatting
- JUnit/kotlin-test, Ktor test host, and Testcontainers PostgreSQL for tests

## Prerequisites

- A Java 25-compatible JDK. CI currently runs on Java 25.
- Docker, used by local PostgreSQL and Testcontainers-backed tests.
- The committed Gradle wrapper. You do not need a separate Gradle install.

## Local Setup

Create a local `.env` file for Docker Compose:

```bash
POSTGRES_USER=pcast
POSTGRES_PASSWORD=pcast
POSTGRES_DB=pcast
```

Start PostgreSQL:

```bash
docker compose up -d db
```

Create `src/main/resources/app.local.conf`. This file is ignored by git and excluded from the fat JAR:

```hocon
database {
  jdbcUrl = "jdbc:postgresql://localhost:5433/pcast"
  driver = "org.postgresql.Driver"
  user = "pcast"
  password = "pcast"
}

jwt {
  secret = "replace-with-at-least-32-characters"
}

cors {
  allowedOrigins = ["http://localhost:3000"]
}
```

Run the API:

```bash
./gradlew run
```

Check the service:

```bash
curl http://localhost:8080/healthz
```

Expected response:

```json
{"status":"ok"}
```

## Common Commands

```bash
./gradlew build           # Build the project and run tests
./gradlew test            # Run tests
./gradlew spotlessCheck   # Check Kotlin formatting
./gradlew spotlessApply   # Apply Kotlin formatting
./gradlew shadowJar       # Build the deployable fat JAR
./gradlew clean           # Remove build outputs
```

Detekt configuration exists under `config/detekt`, but the Gradle plugin and CI step are currently commented out. Re-enable the plugin before relying on `./gradlew detekt`.

## API Overview

Public routes:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/healthz` | Health check. |
| `POST` | `/api/auth/login` | Exchange email/password for an access token and refresh token. |
| `POST` | `/api/auth/refresh` | Rotate a refresh token and receive a new token pair. |
| `POST` | `/api/auth/logout` | Revoke a refresh token and invalidate outstanding access tokens for that user. |
| `POST` | `/api/auth/passkeys/authentication/options` | Start a passkey authentication ceremony. |
| `POST` | `/api/auth/passkeys/authentication/finish` | Finish a passkey authentication ceremony and receive tokens. |

JWT-protected routes:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/feeds` | List the authenticated user's feeds. |
| `POST` | `/api/feeds` | Add a feed. |
| `GET` | `/api/feeds/{id}` | Get a feed by UUID or nano ID. |
| `PUT` | `/api/feeds/{id}` | Update a feed. |
| `POST` | `/api/feeds/opml` | Import feeds from an OPML XML body. |
| `POST` | `/api/auth/passkeys/registration/options` | Start passkey registration for the authenticated user. |
| `POST` | `/api/auth/passkeys/registration/finish` | Finish passkey registration. |
| `GET` | `/api/auth/passkeys` | List registered passkeys. |
| `DELETE` | `/api/auth/passkeys/{id}` | Delete a registered passkey. |
| `GET` | `/api/sync/phrase` | Create a temporary sync phrase. |
| `POST` | `/api/sync/phrase` | Validate a sync phrase. |
| `GET` | `/api/sync/friendly-id` | Create a friendly ID. |

There is no public signup route in the current router. Tests and internal setup create users through `AuthService`.

## Authentication Notes

- Access tokens are JWT bearer tokens. Send them as `Authorization: Bearer <token>`.
- Refresh tokens are random 256-bit values and are stored only as HMAC-SHA256 hashes.
- Refresh tokens rotate on every successful refresh. The previous refresh token stops working immediately.
- Logout deletes the supplied refresh token and increments the user's token version, invalidating outstanding access tokens for that user.
- Passkey routes exchange WebAuthn JSON ceremony payloads. Registration management requires an authenticated bearer token.

## Configuration

Configuration is loaded from classpath resources in this order:

1. `app.prod.conf`
2. `app.local.conf`
3. `app.conf`

`PCAST_ENV` controls startup validation:

| Value | Description |
|-------|-------------|
| `development` | Default if unset. |
| `test` | Test environment. |
| `production` | Requires `/app.prod.conf` on the classpath. |

Important validation rules:

- `jwt.secret` must be nonblank and at least 32 characters.
- PostgreSQL deployments require nonblank `database.user` and `database.password`.
- `passkey.rpId` and `passkey.rpName` must be nonblank.
- `passkey.allowedOrigins` must include at least one web client origin.
- `passkey.challengeTtlSeconds` must be between 60 and 600.
- `passkey.timeoutMillis` must be between 1000 and 300000.
- CORS is default-deny. If `cors.allowedOrigins` is empty, cross-origin requests are not allowed.

## Database and Migrations

Local and production deployments use PostgreSQL. The Compose database listens on `127.0.0.1:5433` and forwards to PostgreSQL port `5432` inside the container.

Flyway runs migrations during application startup before Exposed connects. Migration files live in:

```text
src/main/resources/db/migration
```

## Project Structure

```text
src/main/kotlin/io/pcast/
├── Application.kt           # Entry point and Ktor module setup
├── config/                  # Configuration models
├── error/                   # HTTP error types and responses
├── extensions/              # Small Ktor/JWT/date extensions
├── helpers/                 # UUID, nano ID, friendly ID, and map helpers
├── module/                  # Feature modules
│   ├── AppModule.kt         # Koin annotation component scan
│   ├── Modules.kt           # Manual config and database modules
│   └── <feature>/           # Feature code, routes, DTOs, repositories
├── plugins/                 # Ktor plugins for auth, routing, CORS, DB, validation
└── serializer/              # kotlinx serializers
```

Feature routes live in `module/<feature>/api/*Router.kt`. DTOs live beside their feature in `request` and `response` packages.

## Testing

Tests use JUnit with `kotlin-test`, Ktor's `testApplication { }`, Koin test support, and Testcontainers PostgreSQL. Docker must be running before executing the test suite.

```bash
./gradlew test
```

Run a focused test class:

```bash
./gradlew test --tests "io.pcast.module.feed.api.FeedRouterTest"
```

Run tests matching a pattern:

```bash
./gradlew test --tests "*FeedRouter*"
```

## Deployment Notes

- Set `PCAST_ENV=production`.
- Provide `app.prod.conf` as a secret or classpath resource.
- Ensure `jwt.secret` is unique, private, and at least 32 characters.
- Ensure PostgreSQL credentials are set.
- Build the deployable JAR with `./gradlew shadowJar`.
- `shadowJar` excludes local and test configuration files.
- Place the service behind a TLS-terminating reverse proxy.
- Configure `cors.allowedOrigins` with exact web client origins.
- Rate limits are in-memory and per instance. Use a shared limiter, such as Redis-backed storage, before horizontally scaling auth-heavy deployments.
