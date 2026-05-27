# KofeinoTracker — Wear OS + Android Caffeine Tracker

**KofeinoTracker** is a modern caffeine intake monitoring app built for **Wear OS** smartwatches with an optional **Android phone companion app**. The watch app works fully standalone and can optionally synchronise data with the phone app via the Wearable Data Layer. The app fully supports Polish locale and leverages the latest Android UI and architectural patterns — Jetpack Compose, MVVM, Hilt DI, and Room database.

---

## Features

- **Home Dashboard** — circular progress indicator showing your daily caffeine total (mg), a list of today's drinks, and a quick-add button.
- **Quick Add** — pick from 9 predefined drinks (espresso, cappuccino, black coffee, latte, tea, green tea, energy drink, cola) with automatic caffeine conversion.
- **Official Drinks Browser** — search and import real-world caffeine data from [Open Food Facts](https://world.openfoodfacts.org/), an open-source food database. Works offline via Room cache.
- **Drink Management** — create, edit, and delete custom drinks with custom caffeine and volume values.
- **History** — full consumption history with per-day breakdown, date navigation, and edit/delete support.
- **Limit Warning** — a red alert triggers when you exceed 400 mg (the safe daily limit recommended by EFSA).
- **Cross-Device Sync** — optional real-time synchronisation between your Wear OS watch and Android phone via the Wearable Data Layer. Offline-first with automatic recovery.
- **Agile Testing** — comprehensive unit tests (64+ passing), UI rendering tests, and E2E test infrastructure.

---

## Cross-Device Synchronisation

KofeinoTracker supports **optional** data synchronisation between the Wear OS watch and an Android phone companion app. Both apps are standalone — the sync layer is additive and never blocks core functionality.

### Feature Comparison

| Feature                                | Watch Standalone | With Phone Companion |
| -------------------------------------- | ---------------- | -------------------- |
| Track caffeine intake                  | ✅               | ✅                   |
| Predefined drinks (9 types)           | ✅               | ✅                   |
| Custom drink management (CRUD)         | ✅               | ✅                   |
| Official Drinks Browser (Open Food Facts) | ✅            | ✅                   |
| Consumption history                     | ✅               | ✅                   |
| Daily limit warning (400 mg)           | ✅               | ✅                   |
| Settings & preferences                 | ✅               | ✅                   |
| **Data sync between devices**          | ❌               | ✅                   |
| **View & manage data on phone**        | ❌               | ✅                   |
| **Cross-device conflict resolution**   | ❌               | ✅                   |
| **Sync status dashboard**              | ❌               | ✅                   |

### How It Works

The sync protocol uses the **Wearable Data Layer** (Google Play Services for Wear) to communicate between the watch and phone:

1. **Real-Time Propagation** — every local mutation (add/edit/delete intake or drink) is immediately serialised as a JSON payload and sent to the paired device via `MessageClient.sendMessage`. The `RealTimeSyncService` handles fire-and-forget delivery.

2. **Offline-First Queue** — if no paired device is reachable, the change is persisted in a Room-backed `PendingSyncQueue`. The queue automatically retries with **exponential backoff between retries** (2s → 4s → 8s → 16s) and marks as FAILED after 5 attempts.

3. **Full & Delta Sync** — when a device reconnects, `FullSyncManager` computes a SHA-256 state hash of all local data. If the hash differs from the last synced state, a delta sync transfers only entities modified since the last sync. First-time connections perform a full data exchange.

4. **Conflict Resolution** — concurrent edits on both devices are resolved with **Last-Write-Wins (LWW)** using entity-level timestamps. When timestamps are equal (within 1ms tolerance), the phone wins as the tiebreaker. Clock skew exceeding 60s is logged but does not block sync. Delete operations always win over updates.

5. **Bidirectional Exchange** — the protocol ensures both devices exchange their pending changes in a single session. Each session has a 30-second timeout to prevent hanging.

### Sync Protocol Overview

```
┌─────────────────────┐         ┌─────────────────────┐
│   Wear OS Watch     │         │   Phone Companion   │
│                     │         │                     │
│  ┌───────────────┐  │         │  ┌───────────────┐  │
│  │ User modifies │  │         │  │ Sync Service  │  │
│  │ data locally  │  │         │  │ (foreground)  │  │
│  └───────┬───────┘  │         │  └───────┬───────┘  │
│          │          │         │          │          │
│          ▼          │         │          ▼          │
│  ┌───────────────┐  │         │  ┌───────────────┐  │
│  │  RealTime     │  │ ──────► │  │  RealTime     │  │
│  │  SyncService  │  │ Message │  │  SyncService  │  │
│  └───────┬───────┘  │   API   │  └───────┬───────┘  │
│          │          │ ◄────── │          │          │
│          ▼          │         │          ▼          │
│  ┌───────────────┐  │         │  ┌───────────────┐  │
│  │  PendingSync  │  │         │  │  PendingSync  │  │
│  │  Queue (Room) │  │         │  │  Queue (Room) │  │
│  └───────┬───────┘  │         │  └───────┬───────┘  │
│          │          │         │          │          │
│          ▼          │         │          ▼          │
│  ┌───────────────┐  │         │  ┌───────────────┐  │
│  │   FullSync    │  │ ◄─────► │  │   FullSync    │  │
│  │   Manager     │  │  Delta  │  │   Manager     │  │
│  └───────┬───────┘  │  Sync   │  └───────┬───────┘  │
│          │          │         │          │          │
│          ▼          │         │          ▼          │
│  ┌───────────────┐  │         │  ┌───────────────┐  │
│  │  Conflict     │  │         │  │  Conflict     │  │
│  │  Resolver     │  │         │  │  Resolver     │  │
│  │  (LWW+Phone)  │  │         │  │  (LWW+Phone)  │  │
│  └───────────────┘  │         │  └───────────────┘  │
└─────────────────────┘         └─────────────────────┘
```

### Key Components

| Component | Description |
|-----------|-------------|
| `RealTimeSyncService` | Immediate fire-and-forget propagation of local mutations to paired device |
| `PendingSyncQueue` | Room-backed offline queue with dedup, exponential backoff, max 5 retries |
| `FullSyncManager` | Orchestrates full/delta sync sessions with SHA-256 state hash comparison |
| `IncomingSyncProcessor` | Processes incoming sync payloads and writes to local Room database |
| `ConflictResolver` | LWW conflict resolution with phone tiebreaker for equal timestamps |
| `WearableSyncService` | Android foreground service maintaining Wearable Data Layer listeners |
| `SyncStateStore` | DataStore-backed sync metadata (last sync timestamp, state hash, device ID) |
| `SyncStatusTracker` | StateFlow-based sync status observable by UI (`Synced`, `AwaitingDevice`, `Syncing`, `Error`) |

---

## Tech Stack

| Layer              | Technology                                                                              |
| ------------------ | --------------------------------------------------------------------------------------- |
| UI                 | **Jetpack Compose** + **Wear Compose Material 3**                                      |
| Navigation         | **SwipeDismissableNavHost** (native Wear OS swipe-to-dismiss gestures)                  |
| Architecture       | **MVVM** + **Repository Pattern** + **Unidirectional Data Flow**                       |
| DI                 | **Hilt**                                                                                |
| Database           | **Room** (local SQLite with coroutines)                                                 |
| Networking         | **Retrofit** + **OkHttp** + **Gson**                                                   |
| Reactivity         | **Kotlin Coroutines** + **StateFlow**                                                   |
| Cross-Device Sync  | **Wearable Data Layer** (MessageAPI, CapabilityAPI) + **Google Play Services for Wear** |
| CI                 | **GitHub Actions** (build, lint, unit tests on every push)                              |
| Unit Tests         | JUnit 4 + **MockK** + **Turbine** + **Robolectric**                                    |
| UI / E2E Tests     | Compose UI Test + **Espresso** + **Hilt Android Testing**                               |

---

## Project Structure

```
KofeinoTracker/
├── app/                          # Phone module (placeholder, ready for expansion)
├── wear/                         # Wear OS module (main smartwatch app)
│   ├── src/main/java/...
│   │   ├── data/
│   │   │   ├── local/            # Room DAOs, entities, database
│   │   │   ├── remote/           # Retrofit API, DTOs, CustomDns, ConnectivityObserver
│   │   │   └── repository/       # Repository implementations
│   │   ├── domain/model/         # Domain models (CaffeineIntake, DrinkEntity, OfficialDrink)
│   │   ├── di/                   # Hilt modules (DatabaseModule, NetworkModule)
│   │   └── presentation/
│   │       ├── navigation/       # Screens sealed class + NavHost
│   │       ├── screens/          # Jetpack Compose screens
│   │       ├── theme/            # Color scheme, typography, dynamic theming
│   │       └── viewmodel/        # ViewModels with StateFlow
│   ├── src/test/                 # Unit tests (Repository, ViewModel)
│   └── src/androidTest/          # UI & E2E instrumented tests
├── gradle/libs.versions.toml     # Version catalog
└── README.md
```

---

## Wear OS Emulator Setup

### Prerequisites

- **Android Studio Narwhal** or newer (required for Wear OS 6 emulator)
- SDK API 36 (Wear OS 6) or API 33+ (Wear OS 4/5)

### Quick Steps

1. **Open Android Studio** → `Tools` → `SDK Manager`
2. Under **SDK Tools**, ensure these are installed:
   - `Android Emulator` (latest)
   - `Android SDK Platform-Tools`
3. Under **SDK Platforms**, install:
   - **Android 16.0 ("Baklava")** — API 36 (Wear OS 6.0)
4. Open **Device Manager** (`Tools` → `Device Manager`)
5. Click **Create Device** → choose **Wear OS** category
6. Pick a hardware profile (e.g. **Wear OS Small Round** or **Wear OS Large Round**)
7. Choose the **Wear OS 6 (API 36)** system image and download if needed
8. Complete the wizard and launch the emulator

> **Tip:** Wear OS 6 uses a signed build — root access is unavailable. Developer debug mode is sufficient.

For a fully automated CLI-based setup (no Android Studio), see [`docs/setup-guide.md`](docs/setup-guide.md).

---

## Running the App

```bash
# Build and install on the Wear OS emulator
./gradlew :wear:installDebug

# Or from Android Studio: select the 'wear' run configuration and click Run
```

The app runs as a **standalone Wear OS app** (`com.google.android.wearable.standalone = true`) — no phone pairing required.

When running with the companion app, start the sync service in Settings → Cross-Device Sync. See [`docs/setup-guide.md`](docs/setup-guide.md) for the full development setup guide.

---

## Testing

### Unit Tests (JVM + Robolectric)

```bash
./gradlew :wear:testDebugUnitTest
```

- `CaffeineRepositoryImplTest` — Room in-memory database tests
- `CaffeineViewModelTest` — StateFlow tests with MockK and Turbine
- `OfficialDrinkRepositoryImplTest` — API search + cache fallback tests
- `OfficialDrinkViewModelTest` — search state, browse mode, error handling

### UI / E2E Tests (instrumented)

```bash
# Requires a running Wear OS emulator
./gradlew :wear:connectedDebugAndroidTest
```

> **Note:** Compose UI Test Framework has limited support for Wear OS emulators (round screen clipping). Tests are structurally correct and serve as verification-quality code, but may not execute on all emulator configurations.

---

## Open Food Facts Integration

KofeinoTracker integrates with the [Open Food Facts](https://world.openfoodfacts.org/) public API — an open-source, crowd-sourced food database with no API key required.

- **Search endpoint**: Full-text search via v1 API (`/cgi/search.pl` with `search_terms`, `lc=pl`, `cc=PL`)
- **Browse endpoint**: Category-based lookup via v2 API (`/api/v2/search` with `categories_tags_en`, `caffeine_100g>0`)
- **Offline cache**: Room database with 1-hour TTL
- **Custom DNS**: UDP-based DNS resolver bypass for emulator network restrictions
- Rate limits: 10 req/min (search), 15 req/min (product read)

---

## Requirements

### Wear OS Watch
- **OS**: Wear OS 3+ (API 30+, recommended API 33+)
- **Google Play Services**: Required for Wearable Data Layer
- **Storage**: ~10 MB for app + Room database

### Phone Companion App (Optional)
- **OS**: Android 11+ (API 30+)
- **Google Play Services**: Required for Wearable Data Layer
- **Storage**: ~15 MB for app + Room database + sync queue

### Development
- **Android Studio**: Narwhal or newer
- **JDK**: 21 (Eclipse Temurin or Oracle)
- **Android SDK**: API 36
- **Gradle**: 9.x (9.5.1 bundled in wrapper)
- **Emulator**: Wear OS 6 (API 36) system image for watch development

---

## Best Practices

1. **Wear Compose Material 3** over Material 2 — modern `AppScaffold` + `ScreenScaffold`
2. **TransformingLazyColumn** replaces `ScalingLazyColumn` — animated, performant lists
3. **EdgeButton** for primary actions — native Wear OS UX pattern
4. **Dynamic color scheme** — automatically adapts to watch theme (Android 14+)
5. **Swipe-to-dismiss navigation** — native watch gesture
6. **Flow + StateFlow** — unidirectional data flow, reactive UI
7. **Room + Coroutines** — thread-safe database operations
8. **Hilt** — declarative DI, testability
9. **Defensive programming** — DST-safe day boundaries (Calendar, not `+86400000`), double-click guards, `onError` callbacks in all CRUD operations
10. **Open-source data** — caffeine values sourced from Open Food Facts, not hardcoded estimates
11. **Offline-first sync** — Room-backed pending queue with exponential backoff ensures no data loss when devices disconnect

---

## License

MIT License — see [`LICENSE`](LICENSE) for details.

Built as an educational project. Contributions welcome — see [`CONTRIBUTING.md`](CONTRIBUTING.md).
