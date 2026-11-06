# Pedometer Companion

Desktop/web companion for the Pedometer Android app. Compare your steps, distance and
calories **daily, monthly and yearly** on a Chromebook, Windows PC, Mac or Linux machine.
Fully offline — no account, no server, no analytics. Your data never leaves the device.

## Quick start

**Option A — just open it**
Double-click `index.html` (or drag it into Chrome/Edge). Everything works from `file://`.

**Option B — serve it (enables offline PWA install)**

```bash
cd companion
python3 -m http.server 8080   # or: npx serve .
# open http://localhost:8080
```

In Chrome/Edge: *Install Pedometer Companion* (address-bar install icon) to get a standalone app window.

## Using your Android data

1. On the phone: **Homebase → Export & Share Data** (or the share icon in the top bar).
2. Save the `pedometer-export-YYYY-MM-DD.json` somewhere you can reach from the computer
   (Drive, email to yourself, shared folder, USB).
3. Drop it onto the companion window (or *Import export*). Stats persist in this browser.

## Tabs

| Tab | What you get |
| --- | --- |
| Daily | Per-day bars for any month, % of goal, best day, CSV download of the month |
| Monthly | 12-month totals, YoY delta for the same span, best month, CSV download |
| Yearly | All recorded years: totals, avg/active day, active days |
| Compare | Pick any Day/Month/Year × 2 and see steps, distance, calories, active days and best day side by side with deltas |

Toggle **km/mi** any time (defaults to whatever the export profile uses). Print or *Save as PDF* from the footer.

## Sample data

*Load sample* loads a deterministic 499-day dataset (Jun 2025 → Nov 2026, 4.24M steps) so the
ui is explorable without a phone.

## Tests

```bash
cd companion
npm install
npm test    # jsdom integration suite (48 assertions)
```

## Export format (pedometer-export/v1)

```jsonc
{
  "app": "pedometer",
  "schemaVersion": 1,
  "exportedAt": "2026-11-06T07:30:00Z",
  "profile": { "dailyGoalSteps": 10000, "strideLengthCm": 75, "bodyWeightKg": 70, "useMetric": true, "backgroundTrackingEnabled": true },
  "summary": { "lifetimeSteps": 4240427, "...": "..." },
  "days": [ { "date": "2026-10-12", "steps": 21340, "calories": 871.2, "distanceMeters": 16005.0 } ]
}
```

`days[].date` is `YYYY-MM-DD`; `calories` is kcal; `distanceMeters` is meters.
