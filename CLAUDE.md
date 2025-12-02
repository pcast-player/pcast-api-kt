# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

pcast-api-kt is a podcast feed management API built with Kotlin and Ktor. The service provides RESTful endpoints for managing podcast feeds, OPML import/export, and cross-device synchronization using BIP39 mnemonic phrases.

## Development Commands

### Build & Run
```bash
./gradlew build              # Build the project
./gradlew run                # Run the application (localhost:8080)
./gradlew runShadow          # Run using shadow jar
```

### Code Quality
```bash
./gradlew spotlessCheck      # Check code formatting
./gradlew spotlessApply      # Auto-format code (runs ktlint)
./gradlew detekt             # Run static analysis
./gradlew check              # Run all checks (spotless + detekt + tests)
```

### Testing
```bash
./gradlew test               # Run all tests
./gradlew test --tests "io.pcast.module.feed.api.FeedRouterTest"  # Run specific test class
./gradlew test --tests "*FeedRouterTest.testGetFeed"              # Run specific test method
```

### Database
The application uses Flyway for automatic migrations on startup. PostgreSQL runs via Docker Compose on port 5433.

## Architecture

### Module Organization

The codebase follows a modular feature-based architecture under `io.pcast`:

- **`module/`** - Feature modules (feed, sync)
  - Each module contains: `Service`, `api/Router`, `model/`, `request/`, `response/`, `error/`
- **`plugins/`** - Ktor plugin configurations (Database, Router, Monitoring, Error)
- **`config/`** - Configuration data classes
- **`helpers/`** - ID generation utilities (UUIDv7, NanoId, FriendlyId)
- **`extensions/`** - Kotlin extension functions
- **`serializer/`** - Custom serializers for UUID and LocalDateTime
- **`error/`** - Global error handling (HttpError sealed class, AbortError)

### Dependency Injection (Koin)

The application uses three Koin modules defined in `module/Modules.kt`:

1. **`configModule`** - Loads hierarchical configuration (app.prod.conf → app.local.conf → app.conf)
2. **`dbModule`** - Configures database with HikariCP + Flyway migrations
3. **`appModule`** - Registers repositories and services

For tests, replace `dbModule` with `testDbModule` (uses H2 in-memory database).

### Configuration System

Configuration uses Hoplite with HOCON format. Files are loaded in order of precedence:
1. `app.prod.conf` (production overrides, optional)
2. `app.local.conf` (local development, optional, gitignored)
3. `app.conf` (base configuration)

Create `src/main/resources/app.local.conf` for local database settings:
```hocon
database {
  jdbcUrl = "jdbc:postgresql://localhost:5433/pcast"
  user = "your_user"
  password = "your_password"
}
```

### Database Layer

**Exposed ORM** with repository pattern:
- Repositories expose `save()` methods that handle both insert and update (upsert logic)
- Database connection configured in `plugins/Database.kt`
- Flyway migrations in `src/main/resources/db/migration/{postgres|h2}/`
- Separate migration paths for PostgreSQL (production) and H2 (testing)

### Routing Pattern

Routes are registered in `plugins/Router.kt` which calls each module's router:
```kotlin
fun Application.configureRouting() {
    routing {
        feedRouter(get())
        syncRouter(get())
    }
}
```

Each feature module has an `api/Router.kt` file with route definitions.

### ID Strategy

The application uses a **dual ID system**:
- **UUID v7** (time-ordered) - Primary keys for better database indexing
- **NanoId** (18 chars, URL-safe) - Public API identifiers

Generate IDs using helpers:
```kotlin
import io.pcast.helpers.generateUuidV7
import io.pcast.helpers.generateNanoId
```

### Error Handling

Use `AbortError` to throw HTTP errors from anywhere in the application:
```kotlin
throw AbortError(HttpError.NotFound("Feed not found"))
```

The global `StatusPages` plugin (configured in `plugins/Error.kt`) catches these and returns consistent JSON error responses.

### Testing Patterns

Integration tests use `testApplication {}` with:
- `testDbModule` instead of `dbModule` (H2 in-memory)
- Test data seeding via repository
- HTTP client with content negotiation
- Custom `.expect {}` extension for assertions

Example test structure:
```kotlin
@Test
fun testGetFeed() = testApplication {
    val client = configureServerAndGetClient()  // Sets up Koin + test data

    client.get("/api/feeds/${feed.nanoId}").expect {
        assertEquals(HttpStatusCode.OK, status)
        assertEquals(expectedResponse, body<FeedResponse>())
    }
}
```

## Key Dependencies

- **Ktor 3.3.3** - Async web framework
- **Exposed 0.61.0** - SQL ORM
- **Koin 4.1.1** - Dependency injection
- **Hoplite 2.9.0** - Configuration management
- **Flyway 11.18.0** - Database migrations
- **XML Util 0.91.3** - OPML (XML) serialization
- **kotlin-bip39 1.0.9** - Mnemonic phrase generation for sync

## Code Style

- **ktlint** enforced via Spotless (120 char line length, 4-space indentation)
- **detekt** for static analysis with custom config in `config/detekt/detekt.yml`
- Pre-commit hooks run detekt and spotlessCheck automatically

Always run `./gradlew spotlessApply` before committing to auto-format code.

## Feature Module Pattern

When adding new features, follow the established module pattern:

1. Create module directory under `module/`
2. Add `Service` class with business logic
3. Create `api/Router.kt` with route definitions
4. Define `model/` domain classes and repository
5. Create `request/` and `response/` DTOs
6. Register service in `appModule` (in `module/Modules.kt`)
7. Register router in `plugins/Router.kt`

Refer to `module/feed/` or `module/sync/` as examples.
