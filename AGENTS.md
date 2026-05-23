# AGENTS.md - Agentic Coding Guidelines

This document provides guidelines for AI coding agents working in this Kotlin/Ktor API codebase.

## Project Overview

- **Language**: Kotlin 2.3.21
- **Framework**: Ktor 3.5.0 (REST API)
- **Build System**: Gradle 9.5.1 with Kotlin DSL
- **JVM/CI Target**: Java 25
- **DI Framework**: Koin
- **Database**: Exposed ORM with PostgreSQL (local/production) / Testcontainers PostgreSQL (testing)
- **Migrations**: Flyway

## Build Commands

```bash
./gradlew build              # Build the project (includes tests)
./gradlew run                # Run the application
./gradlew shadowJar          # Build fat JAR for deployment
./gradlew clean              # Clean build artifacts
```

## Testing

```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "io.pcast.module.feed.api.FeedRouterTest"

# Run a single test method
./gradlew test --tests "io.pcast.module.feed.api.FeedRouterTest.testGetFeeds"

# Run tests matching a pattern
./gradlew test --tests "*FeedRouter*"
```

Tests use:
- JUnit 5 with `kotlin-test` assertions
- Ktor test host (`testApplication { }`)
- Koin test support (extend `KoinTest`, use `by inject<T>()`)
- Testcontainers PostgreSQL via `testDbModule`

## Linting & Formatting

```bash
./gradlew spotlessCheck      # Check code formatting (ktlint)
./gradlew spotlessApply      # Auto-fix formatting issues
# Detekt config exists, but the Gradle plugin is currently commented out.
# Re-enable the plugin in build.gradle.kts before relying on these commands:
# ./gradlew detekt           # Run static analysis
# ./gradlew detektBaseline   # Generate baseline for existing issues
```

Pre-commit hooks may run project checks, but `detekt` is currently disabled in Gradle. Confirm local hook behavior before assuming it matches CI.

## Code Style Guidelines

### Formatting (enforced by ktlint + .editorconfig)

- **Indent**: 4 spaces (no tabs)
- **Max line length**: 120 characters
- **Line endings**: LF (Unix-style)
- **Final newline**: Required
- **Charset**: UTF-8
- **Star imports**: Disabled (use explicit imports)

### Imports

- Use explicit imports, never wildcard/star imports
- Group imports: standard library, third-party, project
- Remove unused imports (enforced by ktlint)

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `FeedService`, `FeedResponse` |
| Functions | camelCase | `getFeeds()`, `registerRoutes()` |
| Variables | camelCase | `feedRepository`, `nanoId` |
| Constants | SCREAMING_SNAKE_CASE | `BASE_DATE`, `CONFIG_FILES` |
| Packages | lowercase | `io.pcast.module.feed.api` |
| Enums | PascalCase entries | `HttpError.NotFound` |

### Type Safety

- Prefer non-nullable types; use `?` only when null is a valid state
- Use `val` over `var` where possible
- Avoid platform types - always specify explicit types for Java interop
- Use sealed classes for restricted hierarchies (see `HttpError`)

### Data Classes & DTOs

- Use `data class` for models and DTOs
- Request DTOs: `*Request` suffix, include `toModel()` method
- Response DTOs: `*Response` suffix, include constructor from domain model
- Mark with `@Serializable` for JSON/XML serialization

```kotlin
@Serializable
data class FeedRequest(val title: String, val url: String) {
    fun toModel(id: UUID = generateUuidV7()) = Feed(id = id, ...)
}

@Serializable
data class FeedResponse(val id: UUID, val title: String) {
    constructor(f: Feed) : this(id = f.id, title = f.title)
}
```

### Error Handling

- Use sealed class `HttpError` for HTTP status codes
- Throw `AbortError(HttpError.*, "message")` to abort with status
- Catch specific exceptions, not generic `Exception`/`Throwable` where possible
- Never swallow exceptions silently (covered by the Detekt config when Detekt is enabled)

```kotlin
throw AbortError(HttpError.NotFound, "Feed not found for ID $id")
throw AbortError(HttpError.BadRequest, "Feed ID must be provided")
```

### Dependency Injection (Koin)

- Uses **Koin Annotations** (compiler plugin via KSP) for compile-time DI wiring
- Annotate classes with `@Single` for singletons or `@Factory` for transient instances
- The `AppModule` class in `src/main/kotlin/io/pcast/module/AppModule.kt` uses `@Module` + `@ComponentScan("io.pcast.module")` to auto-discover annotated classes
- Manual DSL modules (`configModule`, `dbModule`, `testDbModule`) remain in `Modules.kt` for definitions that use factory functions
- Use the generated module via `AppModule().module` (from `org.koin.ksp.generated.module`)
- Inject in routes: `val service by inject<FeedService>()`
- In tests: extend `KoinTest` and use `by inject<T>()`

```kotlin
// Annotate classes directly — no manual wiring needed
@Single
class FeedService(private val repository: FeedRepository)

// Use in Application.kt or tests
install(Koin) {
    modules(configModule, dbModule, AppModule().module)
}
```

### Routing (Ktor)

- Group routes in `Route.register*Routes()` extension functions
- Place in `module/<feature>/api/<Feature>Router.kt`
- Use path parameters: `/feeds/{id}` with `call.parameters["id"]`

```kotlin
fun Route.registerFeedRoutes() {
    val service by inject<FeedService>()
    
    get("/feeds") { ... }
    post("/feeds") { ... }
    get("/feeds/{id}") { ... }
}
```

### Project Structure

```
src/main/kotlin/io/pcast/
├── Application.kt           # Entry point, Ktor module setup
├── config/                  # Configuration classes
├── error/                   # Error types (HttpError, AbortError)
├── extensions/              # Extension functions
├── helpers/                 # Utility functions (UUID, NanoId)
├── module/                  # Feature modules
│   ├── AppModule.kt         # Koin @Module with @ComponentScan
│   ├── Modules.kt           # Manual Koin DSL modules (config, db)
│   └── <feature>/          # Feature-specific code
│       ├── api/            # Route handlers (*Router.kt)
│       ├── error/          # Feature-specific errors
│       ├── model/          # Domain models + repositories
│       ├── request/        # Request DTOs
│       └── response/       # Response DTOs
├── plugins/                 # Ktor plugins (routing, DB, errors)
└── serializer/             # Custom kotlinx serializers
```

### Complexity Limits (detekt config)

Detekt is currently disabled in Gradle, but `config/detekt/detekt.yml` records the intended limits:

- Max method length: 60 lines
- Max class size: 600 lines
- Max cyclomatic complexity: 15
- Max function parameters: 6 (constructors: 7)
- Max nested block depth: 4
- Max return statements per function: 2

### Git Commits

Conventional commits are enforced by pre-commit hooks:
- `feat:` new feature
- `fix:` bug fix
- `refactor:` code refactoring
- `test:` adding/updating tests
- `docs:` documentation
- `chore:` maintenance tasks

## CI Pipeline

The GitHub Actions workflow runs on push/PR to main with Java 25:
1. `docker version` - Confirm Docker is available for Testcontainers
2. `./gradlew spotlessCheck` - Code formatting
3. `./gradlew build` - Build and test

The Detekt CI step is currently commented out. Ensure the active workflow checks pass before pushing.

## Environment Variables

| Variable | Values | Description |
|----------|--------|-------------|
| `PCAST_ENV` | `development` (default), `test`, `production` | Controls startup validation. When set to `production`, the application requires `/app.prod.conf` to be present on the classpath and will refuse to boot without it. |

### Production deployment checklist

1. Mount `app.prod.conf` as a secret (e.g. Kubernetes secret, Docker secret).
2. Set `PCAST_ENV=production`.
3. Ensure `jwt.secret` is at least 32 characters (validated at startup).
4. Ensure `database.user` and `database.password` are set (validated at startup).
5. Place the application behind a TLS-terminating reverse proxy (nginx, Caddy, etc.).
6. Configure `cors.allowedOrigins` with the exact origins of your web clients.
