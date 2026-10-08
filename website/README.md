# Leak Out website

The public website of Leak Out: home, rules, FAQ and leaderboard, in English, Dutch, French, German and Spanish. It is a static [Hugo](https://gohugo.io) site with its own theme, hosted on an Azure Static Web App.

```
website/
├── src/              the Hugo site (configuration, content per language, layouts, styles, scripts, images)
└── infrastructure/   Bicep for the Azure Static Web App
```

## Build and run locally

Install **Hugo extended 0.165.0**, the version the workflows use, then from the repository root:

```sh
hugo server --source website/src                 # live preview on http://localhost:1313
hugo --source website/src --panicOnWarning       # production build into website/src/public
```

The site works without JavaScript, except for the leaderboard.

## Layout of `src/`

| Path | Holds |
| --- | --- |
| `hugo.toml` | Languages and site parameters: Play Store URL, issue form URLs, score server URL, `levelCount` |
| `content/<lang>/` | The pages per language: `_index.md` (home), `rules.md`, `faq.md`, `leaderboard.md` |
| `i18n/<lang>.yaml` | Shared UI strings (navigation, footer, leaderboard states) |
| `layouts/` | The site's own templates; no third-party theme |
| `assets/css/main.css` | Styles, with the design system tokens as CSS custom properties |
| `assets/js/leaderboard.js` | The leaderboard script, the only JavaScript on the site |
| `assets/images/` | Raster images Hugo resizes to WebP at build time |
| `static/` | Files served as they are: SVG images and fonts |

When library levels are added to the game, update `levelCount` in `hugo.toml` so the leaderboard can choose them.

## Where the assets come from

Every asset is a **copy**, so the site never depends on files outside `website/`. When an original changes, copy it again.

| File in `website/src/` | Copied from |
| --- | --- |
| `static/images/app-icon.svg` | `assets/icon/app-icon.svg` |
| `static/images/app-icon-512.png` | `assets/icon/app-icon-512.png` |
| `assets/images/feature-graphic-1024x500.png` | `assets/store/feature-graphic-1024x500.png` |
| `assets/images/screenshot-1.png` | `assets/store/Screenshot_20261001_144054.png` (title screen) |
| `assets/images/screenshot-2.png` | `assets/store/Screenshot_20261001_144106.png` (level 8) |
| `static/images/pipes/{h,lt,x,res}.svg` | `src/app/src/main/assets/pipes/` |
| `static/fonts/wendy_one_regular.ttf`, `static/fonts/fredoka.ttf` | `src/app/src/main/res/font/` (both SIL Open Font License) |

Colours, type and spacing follow [docs/design-system.md](../docs/design-system.md). The body font is Fredoka, the font the app uses.
