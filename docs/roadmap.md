# API Roadmap

Prioritized roadmap of missing features needed by a podcast-player client (mobile/web app)
consuming this API. Sequenced by **dependency order** and **value-to-effort**: the episode
work (Phase 2) is the critical path that unlocks most high-value features; everything else can
ship around it.

## Phase 0 — Quick wins (days, not weeks)

Small, self-contained changes that unblock basic real-world client usage. No new domain modeling.

| Feature | Why it's first | Effort |
|---|---|---|
| **`DELETE /feeds/{id}`** | You can subscribe but never unsubscribe — a daily-use gap. Just a router + service + repo method. | XS |
| **Signup route** | `AuthService.createUser()` is already fully implemented; it just needs wiring into `AuthRouter` + a request DTO + rate limit. Without it, clients can't onboard. | XS |
| **Pagination on `GET /feeds`** | Cheap now, painful to retrofit after episodes exist. Also drop the 204-on-empty behavior. | S |
| **OPML export** | Mirror of the existing import; lets users leave with their data. | S |

## Phase 1 — Account lifecycle (production readiness)

Needed before this is a real product, but it doesn't block the domain work — can run in parallel
with Phase 2.

- **Forgot-password / reset flow** (token issue + email)
- **Password change** (authenticated)
- **Email verification**
- **Account deletion** (GDPR — cascade feeds, tokens, passkeys)
- **Profile/settings endpoint** (playback speed, skip intervals, etc.)

> Several of these imply an email-sending dependency that doesn't exist yet — that's a
> sub-decision to make in this phase.

## Phase 2 — Episodes (the foundation)

The keystone. Everything in Phase 3 depends on it, so this is the critical path. Today `Feed`
stores only `title`, `url`, and `synchronizedAt`; nothing fetches or parses feed content, so
there is no concept of an episode anywhere.

1. **Feed fetching + RSS/Atom parsing** — a service that pulls feed XML server-side
2. **Background refresh job** — scheduled poll to keep episodes fresh (uses the currently-unused `synchronizedAt` field)
3. **Episode model + storage** — audio URL, duration, publish date, show notes, artwork, GUID
4. **Episode endpoints** — `GET /feeds/{id}/episodes`, `GET /episodes/{id}`, paginated

Largest effort in the roadmap (L–XL), but it's what makes this a podcast API rather than a
bookmark API.

## Phase 3 — Playback experience (depends on Phase 2)

The features that make a *multi-device* podcast client compelling:

- **Playback progress sync** — resume position, played/unplayed. The headline feature. (Replaces the temporary `/sync/phrase` scaffolding.)
- **Queue / "Up Next"** ordering
- **Starred / bookmarks / archive** at the episode level

## Phase 4 — Discovery & polish

- **Podcast search/discovery** (proxy PodcastIndex or iTunes) — needed to find *new* podcasts
- **OpenAPI/Swagger spec** — client codegen; worth adding once the surface stabilizes
- **API versioning** beyond the flat `/api` prefix
- **Listening stats**

---

## Key sequencing decision

Phase 1 (account lifecycle) vs. Phase 2 (episodes) — both are large and largely independent.

- For a **demoable** podcast client, do Phase 2 first (episodes + playback are the product).
- For a **shippable** product for real users, Phase 1's account/GDPR work is non-negotiable and should overlap with Phase 2.
