# Pedometer

A production-grade Android pedometer built from scratch with **Kotlin**, **Jetpack Compose** and **Room** —
plus an offline **desktop companion** for comparing your stats daily, monthly and yearly on ChromeOS,
Windows, macOS or Linux. Everything is local: no accounts, no backend, no analytics.

```
┌────────────────────────────────────────────────────────────┐
│  Android app (this repo, :app)                             │
│  TYPE_STEP_COUNTER → foreground service → Room (per day)   │
│  Compose dashboard · Homebase analytics hub · JSON export   │
└───────────────────────────────┬────────────────────────────┘
                                 │  pedometer-export-YYYY-MM-DD.json
                                 ▼
┌────────────────────────────────────────────────────────────┐
│  companion/ — desktop web app                              │
│  Daily / Monthly / Yearly views · period comparison · CSV   │
└────────────────────────────────────────────────────────────┘
```

## Features

### Android app
- **Continuous background tracking** — `Sensor.TYPE_STEP_COUNTER` inside a health-typed
  **foreground service** with a persistent notification (tap to open, Stop to pause).
- **Reboot & reset resilience** — the counter zeroes on reboot and never resets at midnight,
  so daily rows are built from deltas against a persisted raw baseline, folded into Room
  inside a **single transaction** with the daily step row.
- **Material 3 dashboard** — animated daily goal ring, distance (from your stride length),
  MET-based active calories (from body weight), live cadence-based **speedometer**
  (0–12 km/h / 0–8 mph), monthly totals via SQL `SUM` aggregates.
- **Homebase hub** — rolling 7-day average with week-over-week delta, like-for-like
  month-to-date comparison vs. last month, personal bests (steps/distance with dates),
  goal streak, lifetime totals — plus a prominent **Export & Share Data** action
  (system share sheet, `FileProvider`).
- **Settings** — stride length (cm/in), body weight (kg/lb), daily goal, km/mi units,
  background-tracking toggle. All persisted in **Jetpack DataStore**; derived metrics are
  recomputed across history when stride/weight change.
- Dark & light theme, edge-to-edge, adaptive launcher icon, vector notification icon.

### Desktop companion (`companion/`)
- Daily / Monthly / Yearly tabs, pick-any-two-periods **Compare** (day/month/year)
- KPI strip, SVG charts with goal line, CSV download, print-to-PDF report
- Installable PWA when served over http(s); fully functional from `file://`
- See [companion/README.md](companion/README.md)

## Repository layout

```
├── app/                                   # Android application module
│   ├── build.gradle.kts
│   └── src/main/java/com/pedometer/app/
│       ├── PedometerApplication.kt        # DI bootstrap
│       ├── MainActivity.kt                # Shell: top bar, bottom nav, permissions, share sheet
│       ├── di/AppGraph.kt                 # Manual DI container
│       ├── data/
│       │   ├── local/                     # Room: StepEntity, SensorStateEntity, StepDao,
│       │   │                              #       MonthlyTotals, BestDay/BestDistance, LifetimeTotals
│       │   ├── settings/                  # Jetpack DataStore user preferences
│       │   ├── repository/                # FitnessRepository (sensor fold-in) · HomebaseRepository
│       │   └── export/ExportJsonBuilder.kt
│       ├── domain/                        # Pure Kotlin, unit-tested:
│       │                                  #   FitnessCalculator · RollingSpeedTracker
│       │                                  #   HomebaseStatsCalculator · HomebaseStats
│       ├── service/                       # StepCounterService (foreground) · BootReceiver
│       ├── ui/                            # StepViewModel · HomebaseViewModel · screens · components
│       └── util/                          # DateUtils (ISO keys) · DateTicker · FormatUtils
├── companion/                             # Desktop web companion (zero-dependency)
│   ├── index.html · styles.css · app.js   #   the whole app
│   ├── sample-data.js                     #   499-day deterministic sample dataset
│   ├── test/integration.test.js           #   jsdom suite (48 assertions)
│   └── manifest.webmanifest · icon.svg · sw.js
├── gradle/libs.versions.toml              # single source of truth for versions
└── settings.gradle.kts
```

## Requirements

| Tool | Version |
| --- | --- |
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17 |
| Android SDK | Platform **35**, Build Tools 35 |
| Gradle | 8.9 (pinned via wrapper — downloads on first run) |

## Build & run

```bash
# Android Studio: File → Open, select this directory, answer the SDK-35 install prompt.
# Or from the command line (SDK must be present):
./gradlew :app:assembleDebug          # builds the debug APK
./gradlew :app:testDebugUnitTest      # runs the JVM unit tests
```

> Note: the sandbox CI where this was authored could not reach Google's Maven mirrors,
> so the first build on your machine will download AGP 8.7.3, Gradle 8.9 and the
> androidx dependencies. Everything else is already in the repo.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions & Play Console

| Permission | Type | Why |
| --- | --- | --- |
| `ACTIVITY_RECOGNITION` | dangerous (runtime) | Step counter sensor, API 29+ |
| `POST_NOTIFICATIONS` | dangerous (runtime, API 33+) | Foreground notification |
| `FOREGROUND_SERVICE` | normal | Run the step service |
| `FOREGROUND_SERVICE_HEALTH` | normal (Android 14+) | Health-typed FGS — step tracking is a documented health use case |
| `RECEIVE_BOOT_COMPLETED` | normal | Resume tracking after reboot |

Play Console notes: the **health** FGS type must be declared under *App content →
Foreground service permissions* (expect a video + justification). Runtime prerequisites
are satisfied via `ACTIVITY_RECOGNITION`, so no extra body-sensor permissions are needed.

**Android 15 note:** `BOOT_COMPLETED` may no longer launch every FGS type. Starts are
best-effort (`BootReceiver` wraps them), respect the user's background-tracking toggle,
and tracking also re-arms on each app launch — so a blocked boot start self-heals at the
next open. Aggressive OEM battery managers may still kill the service; see
[Don't kill my app](https://dontkillmyapp.com/) for device-specific guidance.

## How the sensor math survives reboots

`TYPE_STEP_COUNTER` reports **cumulative steps since boot**; it never resets at midnight
and zeroes on reboot. `FitnessRepository.recordSensorReading()` therefore:

1. reads the last persisted raw value from Room (`sensor_state`),
2. computes `delta = raw − lastRaw` (or `delta = raw` when the counter moved backwards → device rebooted),
3. adds the delta to **today's** row (`steps` keyed by ISO date — also handles midnight rollover for free),
4. persists the new raw baseline **in the same transaction**, so a process kill between steps
   can never double-count.

First run after install adopts the current counter as baseline; steps from before the
install are intentionally not counted.

## Export → desktop companion workflow

1. **Android:** Homebase → **Export & Share Data** (or the share icon in the top bar).
   The app compiles `pedometer-export-YYYY-MM-DD.json` (profile + lifetime summary + every
   recorded day) and opens the share sheet — send it anywhere you can read on the computer.
2. **Companion:** open `companion/index.html`, drop the file in, get daily/monthly/yearly
   stats, period-vs-period comparison, CSV downloads and a print-ready report.

Schema: `app=pedometer, schemaVersion=1`; `days[] = { date: "YYYY-MM-DD", steps, calories(kcal), distanceMeters }`.
Field-level docs: [companion/README.md](companion/README.md#export-format-pedometer-exportv1).

## Testing

- **Unit (JVM)** — `./gradlew :app:testDebugUnitTest`
  `FitnessCalculatorTest`, `RollingSpeedTrackerTest`, `HomebaseStatsCalculatorTest`, `ExportJsonBuilderTest`
- **Companion integration** — `cd companion && npm install && npm test` (jsdom; 48 assertions covering
  import validation, persistence, tabs, charts, compare logic, unit toggle, CSV, clear)
- **Manual QA checklist**
  1. Fresh install → permission prompt appears; grant → tracking pill flips to *Tracking*
  2. Walk ~20 steps → ring/cards/speedometer move; kill the app; reopen → count kept
  3. Reboot device → counts resume without losing the day's progress
  4. Deny `ACTIVITY_RECOGNITION` → amber banner + *Allow access / Open system settings*; app never crashes
  5. Change stride from 75→80 cm → distance everywhere rescales; history rows are rewritten
  6. Change date across midnight with the app open → dashboard rolls to the new day
  7. Homebase → Export & Share → JSON received; load it in the companion → totals match

## Known limitations & honest notes

- Steps are attributed to the day the batch was processed; a sensor batch crossing midnight
  lands ±1 event on the wrong day (inherent to batched delivery).
- A hard process kill loses at most the steps since the last sensor event (usually seconds).
- Calorie/distance figures are estimates (MET table + stride assumption); calibrate stride
  against a known track for best accuracy.
- First install does not back-fill steps already taken before installing.
- Some OEM battery managers (Xiaomi/Oppo/…) aggressively kill FGS; the app recovers on
  next launch but cannot override the OEM policy.
- Export is manual (share-sheet); there is no automatic sync — by design, for privacy.

## Version pins

| Component | Version |
| --- | --- |
| AGP | 8.7.3 (Gradle 8.9, JDK 17) |
| Kotlin | 2.0.21 (+ Compose compiler plugin) |
| Compose BOM | 2024.12.01 (Material 3) |
| Room | 2.6.1 (KSP) |
| DataStore | 1.1.1 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |

Bump everything via `gradle/libs.versions.toml` only.

## License

Not specified — add one before publishing or open-sourcing this repository.
