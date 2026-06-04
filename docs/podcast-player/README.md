# KMP Podcast Player Handoff

This folder is the implementation handoff for a future Kotlin Multiplatform podcast player that uses this REST API.
It intentionally does not assume an app repository path. The next agent should keep this API repo focused on the backend
and create the KMP app in a separate repository or caller-provided location.

## Goal

Build a first-class Android, iOS, and Desktop podcast player using Compose Multiplatform. The app should let a new user
sign up and sign in with a passkey, manage podcast subscriptions, browse indexed episodes, stream audio, and sync
playback progress across devices.

## Current API Facts

The current backend is a Kotlin 2.4.0 / Ktor 3.5.0 REST API with PostgreSQL, Flyway, Exposed, Koin annotations,
JWT access tokens, refresh-token rotation, and passkey/WebAuthn support. The service currently supports authenticated
feed subscription management, OPML import, passkey management for already-authenticated users, and a few sync helper
endpoints.

Important current gaps for a podcast player:

- There is no public signup route.
- Passkey registration currently requires an existing authenticated JWT session.
- `GET /api/feeds` returns `204 No Content` when the user has no feeds.
- There is no episode model, episode API, feed-sync endpoint, media URL index, or playback-progress API.
- Feed route parameters are named `{id}`, but the implementation currently uses the feed `nanoId` for lookups.

Current public routes:

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/healthz` | Returns service health. |
| `POST` | `/api/auth/login` | Email/password login. |
| `POST` | `/api/auth/refresh` | Refresh-token rotation. |
| `POST` | `/api/auth/logout` | Revokes a refresh token and invalidates existing access tokens. |
| `POST` | `/api/auth/passkeys/authentication/options` | Starts passkey sign-in. |
| `POST` | `/api/auth/passkeys/authentication/finish` | Finishes passkey sign-in and returns tokens. |

Current JWT-protected routes:

| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/feeds` | Lists the authenticated user's feeds. |
| `POST` | `/api/feeds` | Adds one feed. |
| `GET` | `/api/feeds/{id}` | Gets one feed by current `nanoId` behavior. |
| `PUT` | `/api/feeds/{id}` | Updates one feed by current `nanoId` behavior. |
| `POST` | `/api/feeds/opml` | Imports subscriptions from OPML XML. |
| `POST` | `/api/auth/passkeys/registration/options` | Starts passkey registration for an authenticated user. |
| `POST` | `/api/auth/passkeys/registration/finish` | Finishes passkey registration. |
| `GET` | `/api/auth/passkeys` | Lists registered passkeys. |
| `DELETE` | `/api/auth/passkeys/{id}` | Deletes one passkey credential. |
| `GET` | `/api/sync/phrase` | Creates a temporary sync phrase. |
| `POST` | `/api/sync/phrase` | Validates a sync phrase. |
| `GET` | `/api/sync/friendly-id` | Creates a friendly ID. |

Existing auth payloads:

```json
{
  "email": "listener@example.com",
  "password": "testpassword123"
}
```

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Existing feed payloads:

```json
{
  "title": "Example Podcast",
  "url": "https://example.com/feed.xml"
}
```

```json
{
  "id": "018f72b2-9b5b-7cc2-bf4b-c04d97f61231",
  "nanoId": "V1StGXR8_Z5jdHi6B-myT",
  "title": "Example Podcast",
  "url": "https://example.com/feed.xml",
  "synchronizedAt": null
}
```

## Target MVP

The first playable MVP includes:

- Public passkey signup and passkey sign-in.
- Authenticated feed subscriptions.
- Server-side RSS fetch, parse, and episode indexing.
- Episode list and episode detail APIs.
- Streaming playback from indexed episode media URLs.
- Synced per-user playback position and completion state.
- Android, iOS, and Desktop as first-class targets.

Out of scope for the first MVP:

- Offline downloads.
- Podcast search directories.
- Ratings, playlists, queues, or social features.
- Push notifications.
- Server-side audio proxying or transcoding.

## Implementation Order

1. Extend backend auth with public passkey signup.
2. Add backend episode storage, RSS sync, and feed deletion.
3. Add playback-progress APIs.
4. Update backend API docs and Bruno requests.
5. Create the standalone KMP app scaffold.
6. Implement shared API client, token storage, repositories, and screens.
7. Add platform passkey implementations.
8. Add platform audio playback implementations.
9. Wire progress sync and cross-device resume behavior.

## Local Backend Commands

Create `.env` for Docker Compose:

```bash
POSTGRES_USER=pcast
POSTGRES_PASSWORD=pcast
POSTGRES_DB=pcast
```

Start PostgreSQL:

```bash
docker compose up -d db
```

Create `src/main/resources/app.local.conf`:

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

passkey {
  rpId = "localhost"
  rpName = "pcast"
  allowedOrigins = ["http://localhost:3000"]
}
```

Common commands:

```bash
./gradlew run
./gradlew spotlessCheck
./gradlew test
./gradlew build
```

Docker must be running for Testcontainers-backed tests.

## Source Files To Inspect First

Backend entry points:

- `src/main/kotlin/io/pcast/Application.kt`
- `src/main/kotlin/io/pcast/plugins/Router.kt`
- `src/main/kotlin/io/pcast/plugins/Auth.kt`
- `src/main/kotlin/io/pcast/plugins/Validation.kt`
- `src/main/kotlin/io/pcast/plugins/Error.kt`

Current auth and passkey code:

- `src/main/kotlin/io/pcast/module/auth/api/AuthRouter.kt`
- `src/main/kotlin/io/pcast/module/auth/AuthService.kt`
- `src/main/kotlin/io/pcast/module/auth/passkey/PasskeyService.kt`
- `src/main/kotlin/io/pcast/module/auth/model/UserRepository.kt`
- `src/main/kotlin/io/pcast/module/auth/model/PasskeyChallengeRepository.kt`
- `src/main/resources/db/migration/V6__PASSKEYS.sql`

Current feed code:

- `src/main/kotlin/io/pcast/module/feed/api/FeedRouter.kt`
- `src/main/kotlin/io/pcast/module/feed/FeedService.kt`
- `src/main/kotlin/io/pcast/module/feed/model/FeedRepository.kt`
- `src/main/kotlin/io/pcast/module/feed/request/FeedRequest.kt`
- `src/main/kotlin/io/pcast/module/feed/response/FeedResponse.kt`

Tests:

- `src/test/kotlin/io/pcast/module/auth/api/AuthRouterTest.kt`
- `src/test/kotlin/io/pcast/module/feed/api/FeedRouterTest.kt`
- `src/test/kotlin/io/pcast/module/TestModules.kt`

## Handoff Documents

- [01 API Extension Plan](01-api-extension-plan.md)
- [02 KMP App Plan](02-kmp-app-plan.md)
- [03 Testing And Acceptance](03-testing-and-acceptance.md)

## Official References

- [Ktor client engines](https://ktor.io/docs/client-engines.html)
- [Ktor bearer auth](https://ktor.io/docs/client-bearer-auth.html)
- [Compose Multiplatform navigation](https://kotlinlang.org/docs/multiplatform/compose-navigation-routing.html)
- [Android passkeys](https://developer.android.com/identity/passkeys/create-passkeys)
- [Apple passkeys with AuthenticationServices](https://developer.apple.com/documentation/authenticationservices/public-private-key-authentication)
- [SQLDelight](https://cashapp.github.io/sqldelight/)

