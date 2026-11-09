# Lillyhop project direction

This is the durable product handoff for future contributors and coding assistants. Read it before planning product changes. Source and tests are authoritative for implementation; verify the current branch and worktree because status below can go stale.

## Product intent

Lillyhop is a simple, friendly step-counting app being built for Cathy. It should make everyday movement easy to understand and encourage progress without pressure.

- **Name:** Lillyhop is the current working title, not a final brand lock.
- **Look and feel:** gentle frog-and-beach seaside. Sea-glass and lagoon greens, ocean blue, warm sand/cream, rounded surfaces, readable high-contrast step data, and subtle frog/beach details.
- **Dashboard phrase:** “Little hops add up.”
- **Principles:** useful first, simple setup, encouraging rather than coercive, truthful claims, and clear privacy/data control.
- Generic AI/search-result feature lists are comparison prompts, not validated requirements. Don't treat their citations or claims as verified.

## Agreed product and business decisions

- Free tier: foreground step tracking plus basic stride, weight, daily goal, and distance-unit settings.
- Premium is a one-time/lifetime Google Play purchase, product ID `premium_unlock`; intended unlocks are background tracking (including after reboot), Homebase insights/data export, and ad removal.
- Cathy should receive Premium using a Google Play promo code on her account. Never add a hardcoded master key or purchase bypass.
- Free-tier advertising is intended to be limited to a small Homebase banner, with the consent flow before ad initialization.
- Do not add social leaderboards, rewards, subscriptions, or major scope changes unless Cathy asks.

These are user-approved product decisions, not proof that Play Console, AdMob, legal, or release configuration is complete.

## Last known implementation map

- Native Android app: Kotlin, Jetpack Compose, Material 3, Room, and DataStore.
- Step counting uses Android `TYPE_STEP_COUNTER`, which is cumulative since boot. Preserve reboot/reset handling and sequential processing of sensor updates. Do not claim a numeric accuracy guarantee without documented real-device testing.
- Dashboard includes daily goal/progress, distance and calorie estimates, tracking state, speedometer, and monthly totals. Homebase provides analytics and export subject to entitlement.
- Play Billing, Google UMP consent, and Mobile Ads integration are present in the last reviewed source.
- `companion/` is an offline HTML/CSS/vanilla-JS app for locally imported data with daily/monthly/yearly/compare views, charts, and print/export. No automatic phone sync or Health Connect integration has been established.
- `mockups/lillyhop-preview/` is a static concept mockup, not production companion code.
- Health Connect/wearable integration and in-app deletion of history have not been verified as implemented. Check the code before describing these as features.

## Trust and privacy

Step history being stored locally does not mean no data ever leaves the device. Ads/consent, Google Play Billing, and user-initiated exports/shares may use network services. Explain what data stays local and what network interactions occur; avoid broad “all data is private/local” claims.

Do not promise an accuracy margin such as 5–10% based on generic articles. Accuracy varies by device, placement, and movement. A responsible validation plan compares against manual counts across multiple devices, carry locations, and walking conditions.

## Current engineering handoff (checked 2026-11-09)

- Repo path: `D:\\Step-Counter\\pedometer-project`; branch: `arena/c07bf140-app`.
- Last pushed commit known before this handoff: `2af8ba4` (Lillyhop branding and visual preview). Earlier notable commits: `1b13d53` (serialize step sensor readings), `e7a64df` (Premium and consent-aware ads), `efbb5d3` (Kotlin/KSP compatibility). Verify current Git history before relying on these IDs.
- Latest Android CI checked for `2af8ba4` failed in Room/KSP processing with `java.lang.IllegalStateException: unexpected jvm signature V`; companion integration checks passed. This is a source/build-tool failure, not a Gradle download timeout.
- The app had Kotlin/KSP 2.3.0 with Room 2.6.1. A local proposed fix now updates the Room version catalog to 2.8.5, the stable version listed on the AndroidX release page when checked. Gradle `:app:dependencies --configuration debugCompileClasspath` resolved Room 2.8.5 successfully.
- Android unit tests/compilation have **not** been verified for that change because this computer has no Android SDK (`ANDROID_HOME`, `ANDROID_SDK_ROOT`, and the default Windows SDK location were absent). The dependency update is uncommitted and must not be called fixed until CI compiles/tests it.
- The latest observed worktree before this handoff had an unrelated local edit in `app/src/main/java/com/pedometer/app/domain/SensorFoldLogic.kt`; Cathy explicitly asked to leave it alone. Do not alter/stage/commit/revert/push it without permission.
- Release readiness still requires checking Play Console product setup and promo code, real AdMob app/banner IDs, and a published/linked privacy policy. Re-check actual account/config state before claiming these are done.

## Workflow

1. Re-check repository access, branch, `git status`, commit history, and current CI before acting.
2. Keep unrelated local edits untouched; stage only intended paths. Do not commit or push unless Cathy asks.
3. Use the smallest relevant tests/build and state limitations explicitly.
4. Ask Cathy before consequential behavior, monetization, data-sharing, or scope choices.
5. When decisions or verified implementation facts change, update this document. Keep accepted decisions distinct from suggestions and unverified items.
