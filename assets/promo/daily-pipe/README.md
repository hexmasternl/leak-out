# Daily Pipe promo videos

Five 15-second vertical promos (1080 × 1920, 30 fps, H.264 + AAC) that push players to try the Daily Pipe. They are drawn in code on a canvas, following the Deep Flow design system (`docs/design-system.md`): Wendy One and Varela Round, liquid as the only luminous element, brass for actions, the pipe recipe of 6.3 and the daily leaderboard of 5.6. The rendered files go to `docs/video/`.

| # | File | Hook | Story | End card |
|---|------|------|-------|----------|
| 1 | `daily-pipe-01-midnight-drop.mp4` | "Every night at midnight…" | The clock ticks 23:59:57 → 00:00:00 UTC, a fresh level drops in tile by tile and gets solved | New pipe. Every day. · Play Today's Pipe |
| 2 | `daily-pipe-02-climb-the-top-5.mp4` | "Today's top 5. Is your name on it?" | The daily leaderboard: the player's row climbs from #76 to #1 over four attempts | Climb today's top 5. |
| 3 | `daily-pipe-03-24-hours.mp4` | "You have 24 hours." | A speedrun with the HUD and stack while the day's timer races to 00:00:00, "Then it's gone." | 24 hours. Your best shot. |
| 4 | `daily-pipe-04-same-pipe-for-everyone.mp4` | "One pipe." | Nine players race the same level side by side; some leak, five make the top 5, with an empty "You" row | Will you make the top 5? |
| 5 | `daily-pipe-05-one-more-try.mp4` | "So close…" | The first attempt leaks, Restart, the retry takes the +50 cross, "New best!", #3 today | Today's pipe is waiting. |

Every end card shows the logo, the DAILY PIPE chip, a brass call to action, "A new Daily Pipe drops at 00:00 UTC" and "Free on Android".

## Files

- `lib.js` – tokens, pipe and liquid rendering, endpoints, board, HUD pieces, dialogs, leaderboard, end card.
- `levels.js` – the shared 6 × 7 level and its three routes (clean, with a cross, leaking).
- `v1.js` … `v5.js` – one scene each: a `draw(ctx, t)` function plus the music and sound-effect cues.
- `page.html` – hosts a scene (`page.html?scene=v3`); opened in a browser it loops the scene live.
- `render.mjs` – drives headless Chromium frame by frame and pipes the frames into ffmpeg.

## Rendering

Needs Node, Playwright with Chromium, and an ffmpeg build with libx264 and aac.

```sh
FFMPEG=/path/to/ffmpeg node render.mjs            # all five into docs/video/
node render.mjs v2 v4                             # just these
node render.mjs --stills v3 1 6.5 12              # preview PNGs into stills/
```

`PLAYWRIGHT_MODULE` can point at a global Playwright install. Varela Round comes from Google Fonts, so the first render needs a network connection; Wendy One is loaded from `app/src/app/src/main/res/font/`.

## Audio

Music beds are tracks from `assets/audio/` (credits in `assets/audio/audio_credits.json`): "Ebb and Flow" (Fabian Measures), "RF-Silly Saturday" (legacyAlli), "Breaking Bad" (Jahzzar), "Decade Of Upsets" (Doctor Turtle) and "Everyday" (Nicolaas Stulting). Sound effects are the game's own from `assets/sfx/`. Check the licence terms of each track before paid advertising.

The player names are the sample names from the design system's top 10 sketch, not real players.
