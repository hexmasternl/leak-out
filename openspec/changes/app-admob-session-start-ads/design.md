## Context

All screen changes happen in `MainActivity` (a `Screen` state). Levels start from three places: the welcome screen ("Continue"), the level library, and the Daily Pipe (downloaded via `DailyPipeViewModel`, also opened by a tapped reminder). Level-to-level moves happen inside `Screen.Game` (`onNextLevel`) and never pass through a menu screen. A start already goes through the update gate (`mayStartLevel`). The `core` module holds pure rules; `app` wraps Android and Google SDKs.

## Goals / Non-Goals

**Goals:**
- Exactly one interstitial per session start, none between levels.
- The game never waits on, or fails because of, an ad.
- Consent first, test IDs in debug, SDK hidden behind an interface.

**Non-Goals:**
- Banners, rewarded or app-open ads, mediation, a remove-ads purchase, server changes.

## Decisions

- **Session = entering a level from a non-level screen.** The ad hook sits where a menu screen switches to `Screen.Game` or `Screen.DailyPipe`. `onNextLevel` stays untouched, so level-to-level never reaches it. Returning to the menu ends the session implicitly because the next start again comes from the menu. No session counter is needed; the `core` rule `AdSessionGate.shouldShow(fromMenu, adReady)` is a pure function that keeps the intent testable.
- **Ad is shown after the tap, the level opens in the callback.** `AdProvider.showIfReady(activity, onFinished)` calls `onFinished` straight away when nothing is loaded, and after dismiss or show failure otherwise. The update gate is asked first, then the ad, then the screen switches.
- **Wrapper over the SDK.** `AdProvider` (`preload()`, `showIfReady`) has `AdMobAdProvider` for production and a fake for tests and previews. The `Activity` is passed per call, never stored, to avoid leaks.
- **Consent before init.** `ConsentManager` runs UMP in `MainActivity.onCreate` and calls `MobileAds.initialize` on a background thread only when `canRequestAds()` is true, then preloads. Stored consent from an earlier run lets ads be requested at once.
- **IDs per build type.** `manifestPlaceholders["admobAppId"]` and a `BuildConfig` field for the interstitial ID: debug uses Google's test IDs; release uses the Leak Out IDs, which are public and live in the build file like the score server client id; `LEAKOUT_ADMOB_*` override them as a pair.
- **Version pins.** `play-services-ads` goes in the version catalog like every other dependency; its transitive `fragment` and lifecycle versions are checked against the existing pins (issue #88).
- **Website file.** `app-ads.txt` goes in `website/src/static/` so Hugo serves it from the site root, one line with the AdMob publisher ID. It only matters for AdMob verification; the app never reads it.

## Risks / Trade-offs

- A slow or empty ad load means a start without an ad: accepted, play always wins over ads.
- Clicking your own live ads can suspend the AdMob account, so dev builds use only test IDs.
- Ads add the advertising ID, a Play Console Ads declaration and a privacy policy update: tracked as tasks.
- `play-services-ads` may grow the app and pull older transitive libraries that Play Console flags.

## Open Questions

- The issue says an ad "the first time the player starts a game". This change reads that as once per session (every start from the menu). Should it instead be once per app process?
- Is the game child-directed (Families policy)? If yes, child-directed treatment and a lower max ad content rating are required.
- Should a Daily Pipe opened from a tapped reminder also show an ad? This change shows it for button and reminder alike.
- Should the very first launch after install skip the ad?
