# Backend API Extension Plan

This plan extends `pcast-api-kt` so a Kotlin Multiplatform player can use the backend as its subscription, episode,
auth, and progress-sync service.

## Principles

- Preserve existing password login, refresh-token rotation, authenticated passkey management, and OPML import behavior.
- Make passkeys the primary onboarding path for new app users.
- Keep route ownership checks strict: every feed, episode, and progress lookup must be scoped to the authenticated user.
- Keep the client-facing feed identifier as `nanoId`; use episode UUIDs for episode routes.
- Treat remote RSS fetching as an untrusted network operation with SSRF protections.
- Return JSON for all new REST endpoints except WebAuthn ceremonies that already exchange raw WebAuthn JSON.

## Dependencies

Add ROME for JVM RSS parsing:

```kotlin
val romeVersion: String by project

dependencies {
    implementation("com.rometools:rome:$romeVersion")
    implementation("com.rometools:rome-modules:$romeVersion")
}
```

Add to `gradle.properties`:

```properties
romeVersion=2.1.0
```

`com.rometools:rome` version `2.1.0` is the current Maven Central release at the time this handoff was written.
Check Maven Central before implementation if dependency policy requires the newest available patch.

## New And Changed Public API

### Public Passkey Signup

`POST /api/auth/passkeys/signup/options`

Request:

```json
{
  "email": "listener@example.com"
}
```

Success response: `200 OK`, `Content-Type: application/json`, body is the raw WebAuthn credential creation options
JSON returned by the Yubico library.

Failure responses:

| Status | Body | Cause |
| --- | --- | --- |
| `400` | `{ "message": "email: invalid format" }` | Invalid email. |
| `409` | `{ "message": "User already exists" }` | Email already belongs to a user. |

`POST /api/auth/passkeys/signup/finish`

Request: raw WebAuthn registration credential JSON from the platform passkey API.

Success response: `201 Created`

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Failure responses:

| Status | Body | Cause |
| --- | --- | --- |
| `400` | `{ "message": "Invalid passkey registration response" }` | Malformed or failed registration response. |
| `401` | `{ "message": "Passkey challenge expired or already used" }` | Challenge not found, expired, consumed, or replayed. |
| `409` | `{ "message": "User already exists" }` | Email was claimed after the options step. |

Implementation details:

- Add `PasskeySignupOptionsRequest(email: String)`.
- Add `HttpError.Conflict`.
- Normalize email before storage and lookup by trimming and lowercasing.
- Add `PasskeyChallengeType.SignupRegistration`.
- Extend `passkey_challenges` with nullable `email` and `passkey_user_handle` columns for signup challenges.
- Do not create the user during the options step. Generate a passkey user handle, build the WebAuthn creation options,
  and store email, handle, challenge, request JSON, type, and expiry in `passkey_challenges`.
- During finish, consume the signup challenge, validate the WebAuthn response, create a passkey-only user, create the
  credential, and return `TokenResponse`.
- Make `users.password_hash` nullable so passkey-only users can exist. Password login must only verify users whose
  `password_hash` is not null.
- Keep existing authenticated passkey registration endpoints for adding more passkeys to an existing account.

### Feed Changes

`GET /api/feeds`

Change empty-list behavior from `204 No Content` to `200 OK` with `[]`.

`DELETE /api/feeds/{id}`

Deletes a feed owned by the authenticated user. `{id}` is the feed `nanoId` for the KMP client.

Success response: `204 No Content`.

Failure response: `404` when the feed is missing or belongs to a different user.

`POST /api/feeds/{id}/sync`

Fetches the feed URL, parses RSS/Atom, upserts episodes, updates `feeds.synchronized_at`, and returns a summary.
`{id}` is the feed `nanoId`.

Success response: `200 OK`

```json
{
  "feed": {
    "id": "018f72b2-9b5b-7cc2-bf4b-c04d97f61231",
    "nanoId": "V1StGXR8_Z5jdHi6B-myT",
    "title": "Example Podcast",
    "url": "https://example.com/feed.xml",
    "synchronizedAt": "2026-05-23T14:05:00"
  },
  "createdCount": 8,
  "updatedCount": 2,
  "skippedCount": 0
}
```

Failure responses:

| Status | Body | Cause |
| --- | --- | --- |
| `400` | `{ "message": "Feed URL must use http or https" }` | Invalid feed URL scheme. |
| `400` | `{ "message": "Feed URL host is not allowed" }` | Host resolves to a private, loopback, multicast, link-local, or local address. |
| `400` | `{ "message": "Invalid podcast feed" }` | RSS/Atom parsing failed or no playable episodes were found. |
| `404` | `{ "message": "Feed not found" }` | Feed is missing or owned by another user. |

### Episodes

`GET /api/episodes?feedId=<nanoId>&limit=50&before=<iso-local-date-time>`

Lists episodes owned by the authenticated user. `feedId` is optional; when present it filters to one feed by `nanoId`.
`limit` defaults to `50`, has a maximum of `100`, and must be positive. `before` filters episodes older than the supplied
timestamp using `publishedAt` when available and `createdAt` otherwise.

Success response: `200 OK`

```json
[
  {
    "id": "018f72b2-9b5b-7cc2-bf4b-c04d97f61231",
    "feedNanoId": "V1StGXR8_Z5jdHi6B-myT",
    "feedTitle": "Example Podcast",
    "guid": "episode-123",
    "title": "Episode 123",
    "description": "Episode summary",
    "mediaUrl": "https://cdn.example.com/episode-123.mp3",
    "mediaType": "audio/mpeg",
    "durationSeconds": 3600,
    "publishedAt": "2026-05-22T10:00:00",
    "imageUrl": "https://example.com/artwork.jpg",
    "progress": {
      "positionSeconds": 123,
      "durationSeconds": 3600,
      "completed": false,
      "updatedAt": "2026-05-23T14:05:00"
    }
  }
]
```

`GET /api/episodes/{id}`

Gets one episode by episode UUID if it belongs to one of the authenticated user's feeds.

Success response: `200 OK` with the same `EpisodeResponse` shape used by the list endpoint.

Failure response: `404` when the episode is missing or belongs to another user.

`PUT /api/episodes/{id}/progress`

Upserts progress for one user-owned episode.

Request:

```json
{
  "positionSeconds": 123,
  "durationSeconds": 3600,
  "completed": false
}
```

Success response: `204 No Content`.

Validation:

- `positionSeconds` must be `>= 0`.
- `durationSeconds` is nullable, but when present must be `> 0`.
- If both values are present, `positionSeconds` must be less than or equal to `durationSeconds`.
- `completed=true` is allowed only when `durationSeconds` is present or the stored episode has a duration.
- The service should clamp small player drift by accepting `positionSeconds` up to `durationSeconds + 5`.

## Database Changes

Create the next Flyway migrations after the current latest migration.

### Auth Migration

```sql
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE passkey_challenges
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN passkey_user_handle VARCHAR(86);

CREATE INDEX passkey_challenges_email_idx ON passkey_challenges(email);
```

### Episode Migration

```sql
CREATE TABLE episodes (
    id UUID PRIMARY KEY,
    feed_id UUID NOT NULL REFERENCES feeds(id) ON DELETE CASCADE,
    guid VARCHAR(1024) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    media_url TEXT NOT NULL,
    media_type VARCHAR(255),
    duration_seconds BIGINT,
    published_at TIMESTAMP,
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (feed_id, guid)
);

CREATE INDEX episodes_feed_published_idx ON episodes(feed_id, published_at DESC);
CREATE INDEX episodes_feed_created_idx ON episodes(feed_id, created_at DESC);

CREATE TABLE episode_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    position_seconds BIGINT NOT NULL,
    duration_seconds BIGINT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, episode_id)
);

CREATE INDEX episode_progress_user_updated_idx ON episode_progress(user_id, updated_at DESC);
```

## Suggested Kotlin Structure

Auth changes:

- Modify `io.pcast.error.HttpError`.
- Modify `io.pcast.plugins.Validation`.
- Modify `io.pcast.module.auth.model.UsersTable` and `User`.
- Modify `io.pcast.module.auth.model.UserRepository`.
- Modify `io.pcast.module.auth.model.PasskeyChallenge` and `PasskeyChallengeRepository`.
- Modify `io.pcast.module.auth.passkey.PasskeyService`.
- Modify `io.pcast.module.auth.api.AuthRouter`.
- Create `io.pcast.module.auth.passkey.request.PasskeySignupOptionsRequest`.

Feed changes:

- Modify `io.pcast.module.feed.api.FeedRouter`.
- Modify `io.pcast.module.feed.FeedService`.
- Modify `io.pcast.module.feed.model.FeedRepository`.
- Create `io.pcast.module.feed.response.FeedSyncResponse`.
- Create `io.pcast.module.feed.sync.FeedFetcher`.
- Create `io.pcast.module.feed.sync.FeedParser`.
- Create `io.pcast.module.feed.sync.FeedSyncService`.

Episode module:

- Create `io.pcast.module.episode.api.EpisodeRouter`.
- Create `io.pcast.module.episode.EpisodeService`.
- Create `io.pcast.module.episode.model.Episode`.
- Create `io.pcast.module.episode.model.EpisodeProgress`.
- Create `io.pcast.module.episode.model.EpisodeRepository`.
- Create `io.pcast.module.episode.model.EpisodeProgressRepository`.
- Create `io.pcast.module.episode.request.EpisodeProgressRequest`.
- Create `io.pcast.module.episode.response.EpisodeResponse`.
- Create `io.pcast.module.episode.response.EpisodeProgressResponse`.
- Register episode routes inside the authenticated block in `io.pcast.plugins.configureRouting`.

## Feed Fetching And SSRF Guardrails

RSS sync must not fetch arbitrary local or private resources. Implement a dedicated fetcher instead of scattering URL
logic through services.

Rules:

- Accept only `http` and `https`.
- Reject URLs with user info.
- Resolve the hostname before connecting.
- Reject loopback, link-local, site-local/private, multicast, wildcard, and documentation/test networks.
- Reject `localhost`, `.localhost`, and hostnames without a public DNS result.
- Set connect timeout to 5 seconds and read timeout to 10 seconds.
- Limit response body to 5 MiB.
- Follow at most 3 redirects.
- Re-run the same validation after every redirect before fetching the next URL.
- Prefer HTTPS but do not require it, because many podcast feeds still publish HTTP URLs.
- Reuse the existing XML hardening assumptions from `Application.hardenXmlParser`.

Episode extraction rules:

- Prefer RSS item GUID as `guid`.
- If GUID is missing, use the media enclosure URL as `guid`.
- Skip items without an audio enclosure URL.
- Prefer the first `audio/*` enclosure; if no type is present, accept enclosures ending in common audio extensions such
  as `.mp3`, `.m4a`, `.aac`, `.ogg`, `.opus`, or `.wav`.
- Map item title to `"Untitled Episode"` only if the source title is blank.
- Store item description or summary when available.
- Parse duration from common RSS/iTunes duration fields into seconds.
- Use item published date when available.
- Use item image, iTunes image, or channel image in that order.

## Bruno And README Updates

Add Bruno requests under `api-docs` for:

- Passkey signup options.
- Passkey signup finish.
- Feed delete.
- Feed sync.
- Episode list.
- Episode detail.
- Episode progress update.

Update the root `README.md` API overview after implementation so it includes the new routes and no longer says there is
no public signup route.

