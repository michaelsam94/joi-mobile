# Joi — Android app

⛵ Native Kotlin + Jetpack Compose client for the Joi backend (`../joi-backend`).

## Status

This is a real, from-scratch implementation — not a stub. 71 Kotlin files across 4 modules, every
screen wired to a real use-case wired to a real repository wired to the real backend API — not
placeholders. It cannot be compiled or run in the cloud sandbox this was written in (no Android
SDK/emulator there), so **you'll need Android Studio to build, run, and fix up any small issues**
a compiler would normally catch immediately (a renamed API, a missing import). Everything was
checked by hand for brace/paren balance and cross-file consistency (every `container.xUseCase`
a screen references genuinely exists on `AppContainer` with a matching signature), but treat the
first `./gradlew build` as the real first compile pass this code has ever had.

A handful of spots used APIs I'm confident about the *shape* of but couldn't verify against the
exact pinned library versions — if Gradle sync complains, look here first:
- `PullToRefreshBox` in `LeaderboardScreen.kt` (`androidx.compose.material3.pulltorefresh`) — the
  Material3 pull-to-refresh API went through a couple of shapes; if it doesn't resolve, the
  Compose BOM version in `gradle/libs.versions.toml` likely needs bumping (Android Studio's
  upgrade suggestion is fine to accept).
- `GmsBarcodeScanning.getClient(context)` in `CheckInScreen.kt` — Google's Code Scanner API
  (`com.google.mlkit.vision.codescanner`), deliberately used with default options (scans all
  barcode formats) to avoid an extra dependency for QR-only filtering.
- The kotlinx.serialization Retrofit converter decoding `OkResponseDto`/`Unit` for the two
  endpoints that don't return a normal JSON payload (`change-password`, `DELETE /prizes/:id`).

## Module plan (revised from the original 8-module sketch)

The original plan (see `../docs/PLAN.md`) sketched one Gradle module per feature. Since nothing
here can be compiled to catch inter-module wiring mistakes, that was traded for a **4-module
layered structure** — still genuinely Clean Architecture (strict dependency-inward rule, same as
the backend), just with features separated by *package* inside `app` rather than by *Gradle
module*. This is a common, well-proven shape for an app this size and it means far fewer
`build.gradle.kts` files that could each individually be subtly wrong.

```
joi-android/
  domain/                        pure Kotlin (no Android deps) — entities, repository interfaces
                                  ("ports"), use-cases, session contract. Mirrors the backend's
                                  domain/ + application/ layers exactly.
  data/                          Android library — Retrofit API + DTOs, repository
                                  implementations, DataStore-backed session storage.
                                  Mirrors the backend's infrastructure/ layer.
  designsystem/                  Android library — Joi's Material 3 theme (color/type/shape),
                                  shared composables (level badge, points pill, wave progress,
                                  buttons, states), an auth-aware QR image loader.
  app/                            The application module: DI composition root (AppContainer,
                                  same idea as the backend's config/container.ts), navigation,
                                  and every feature's UI + ViewModel, organized by package:
    ui/auth/, ui/leaderboard/, ui/attendance/, ui/members/, ui/prizes/, ui/profile/
```

Dependency rule: `domain` depends on nothing else in this project. `data` and `designsystem`
depend only on `domain` (designsystem barely touches it — just enough for shared model-driven
components like the level badge). `app` depends on all three. No feature package in `app` imports
another feature package directly — only through `domain` repository interfaces / use-cases.

## Setup

1. Open `joi-android/` as a project in Android Studio (Ladybug/2024.2+ recommended).
2. Let Gradle sync — it will likely prompt to update the Android Gradle Plugin/Kotlin to whatever
   is current; accept that, since the versions pinned in `gradle/libs.versions.toml` are a
   reasonable snapshot, not a hard requirement.
3. Point the app at your backend: edit `BASE_URL` in `app/src/main/java/com/joi/app/AppConfig.kt`
   — `http://10.0.2.2:3000/` reaches your machine's `localhost:3000` from the Android **emulator**;
   for a physical device, use your machine's LAN IP (and make sure the backend's `PORT` is
   reachable on your network) or a real deployed URL.
4. Run on an emulator or device (`minSdk 26`, matching the backend's target Android range).
5. Log in with the moderator account you seeded on the backend (`admin` / whatever you set via
   `SEED_MODERATOR_USERNAME`/`SEED_MODERATOR_PASSWORD`, default `admin` / `ChangeMe123`) — you'll
   be forced into the change-password screen immediately, exactly like the API expects.

## App icon / logo

The generated Joi logo (ship + wave, teal/gold/coral) was shown earlier in this conversation but
couldn't be downloaded into this sandbox (its host isn't on this environment's network
allowlist). Save that image from the conversation and run Android Studio's **Image Asset**
tool (right-click `res` → New → Image Asset) to generate the full adaptive-icon mipmap set from
it — much cleaner than me hand-writing placeholder XML you'd replace anyway.

## What's implemented (Phase 2 MVP)

- **Auth**: login, forced first-login password change, session persisted via DataStore, role read
  from the JWT-backed session and used to route into a moderator or member navigation shell.
- **Leaderboard**: ranked list, level badges (Bronze/Silver/Gold/Diamond), the signed-in member's
  own row highlighted, pull-to-refresh.
- **Attendance (moderator)**: QR scan via Google Play Services' Code Scanner (no camera-permission
  boilerplate needed — it's a modal system scanner), plus a manual token-entry fallback for
  devices without Play Services or for testing; an absentees screen with each person's all-time
  attendance count and a "send weekly report now" button that hits the same endpoint the Friday
  cron uses.
- **Members (moderator)**: list + search, register a new person (name/username/temp password),
  member detail screen with their QR code, active/inactive toggle, and an add/remove-points action
  with a required reason.
- **Prizes**: everyone can browse the catalog; moderators can add/edit/deactivate prizes and
  redeem one for a member (with the same insufficient-balance check the backend enforces).
- **Profile (member)**: own QR code, current points/level, and full points history (every
  attendance, manual adjustment, and redemption, exactly like the backend's audit trail).

## What's next (Phase 3, see docs/PLAN.md)

Attendance streaks, badges beyond levels, push notifications, member-facing Telegram bot, offline
caching, and the motion-heavy "game feel" polish (level-up confetti, animated point ticks) —
the current UI has the visual language (colors, the wave motif, level badges) but not yet the
celebratory motion layer, which is a good next increment once the core flows are confirmed working
on a real device.
