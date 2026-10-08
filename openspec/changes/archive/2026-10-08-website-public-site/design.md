# Design

## Context

See proposal.md for the motivation. Today the repository holds the Android app (moving to `app/` with `repo-merge-server`) and, after that change, the .NET score server in `server/` with its Bicep in `server/infrastructure/` and its own path-filtered workflows. The score server serves `GET /api/levels/{level}/scores/top` (top 10) only to signed-in players; a public website cannot sign in to Play Games, so it needs an anonymous endpoint. The game's library levels are bundled as `levels/level_NNN.json` app assets. Players already file issues in the public `hexmasternl/leak-out-game` repository. The design system (`docs/design-system.md`) defines the colours, type and artwork the site must reuse.

## Goals / Non-Goals

**Goals:**
- A fast, static, attractive site that works without JavaScript except for the leaderboard.
- Five languages from one set of layouts, with translations in content files and Hugo i18n strings.
- A website that is built, checked and deployed on its own, like the app and the server.

**Non-Goals:**
- Accounts, sign-in or any personal data on the website.
- Showing the visitor's own rank (needs sign-in; the app's top 10 covers that).
- A CMS, blog or news section.
- Hosting the privacy policy (see Open Questions).

## Decisions

1. **Hugo with a custom theme, no theme dependency.** Layouts, partials and CSS live in `website/src/` (`hugo.toml`, `content/<lang>/`, `i18n/<lang>.yaml`, `layouts/`, `assets/`, `static/`). A third-party theme would fight the design system and add an update burden. Hugo's version is pinned in the workflows (Hugo extended, exact version). Alternative: Astro/Next static export; rejected because the issue asks for Hugo and the site needs no component framework.
2. **Multilingual by directory.** `defaultContentLanguage = "en"` with `defaultContentLanguageInSubdir = false`, so English is at `/` and others at `/nl/`, `/fr/`, `/de/`, `/es/`. Each language has its own content directory; shared UI strings go in `i18n/`. The switcher uses Hugo's `.Translations` so it lands on the same page.
3. **Leaderboard in the browser.** The leaderboard page is static; a small vanilla JavaScript file fetches `GET <score server>/api/public/levels/{level}/scores/top` and renders the table and its states. The score server's base URL is a Hugo site parameter set per environment. The level choice is a number input plus previous/next buttons bounded by the number of library levels, which the build writes into the page as a parameter (`params.levelCount`, updated when levels are added). Alternative: build the leaderboard at deploy time; rejected because scores would be stale between deploys.
4. **Anonymous server endpoint, separate from the authenticated one.** `GET /api/public/levels/{level}/scores/top` reuses the level ranking of the `level-leaderboard` spec with a limit of 25 and a response type without the "is the requesting player" flag or any player id. It gets ASP.NET Core rate limiting (fixed window per client IP), `Cache-Control: public, max-age=60`, and a CORS policy whose allowed origins come from configuration (the Static Web App's default host name and the custom domain). Alternative: make the existing endpoint anonymous; rejected because it marks the caller's own entry and is part of the app's contract.
5. **Assets copied, not linked.** The icon, feature graphic, screenshots and pipe artwork are copied from `assets/` and the app's drawables into `website/src/static/images/` (or `assets/` when Hugo should fingerprint them). The design system's colour tokens are written into a CSS custom-property file. Copies drift from the originals; that is the price of a self-contained site and is accepted by the issue.
6. **Azure Static Web App via Bicep, deployed by OIDC.** `website/infrastructure/main.bicep` creates the resource group-scoped Static Web App (Free tier; Standard only if a feature needs it) and, optionally, its custom domain. The workflow signs in to Azure with a federated credential (`azure/login`), deploys the Bicep, reads the deployment token with `az staticwebapp secrets list` at run time and passes it to `Azure/static-web-apps-deploy` with `skip_app_build`, so no token is stored as a secret. This mirrors the server's deploy workflow.
7. **Workflows follow the server's pattern.** `website-deploy.yml`: on push to `main` with `paths: website/**` and the workflow file, plus `workflow_dispatch`. `website-pull-request.yml`: a `changes` job and a build job (Hugo build with `--panicOnWarning`, `az bicep build`) that always reports a result, so it can be a required check.
8. **Feedback links use the public repository's issue forms.** "Report a problem" opens `https://github.com/hexmasternl/leak-out-game/issues/new?template=<bug template>` and "Request a feature" the feature template. If those templates do not exist yet, they are added to `leak-out-game` (see Open Questions), otherwise the links fall back to `labels=bug` / `labels=enhancement`.

## Risks / Trade-offs

- [Public display names] Players' Play Games display names become visible to anyone on the web → confirm with the maintainer and update `docs/privacy-policy.md` before the leaderboard goes live.
- [Abuse of the anonymous endpoint] Scraping or load on the score server → rate limiting, short caching, only 25 entries, no ids.
- [Score server cold start] The first leaderboard request may be slow (see `app-score-server-cold-start-timeout`) → the loading state stays up to a generous timeout before the error state.
- [Translation quality] Five languages written by one contributor → keep copy short; mark translations for native-speaker review.
- [Asset drift] Copied images get out of date → document in `website/README.md` where each image came from.
- [Dependency on `repo-merge-server`] → the site ships first if needed; the leaderboard shows its error state until the endpoint is deployed.

## Migration Plan

1. Land the website and its workflows; create the Azure federated credential and the GitHub variables; run `website-deploy.yml` by hand once to create the Static Web App.
2. After `repo-merge-server`, land the server endpoint and configure the website origin in the server's CORS settings; deploy the server.
3. Optionally add the custom domain (DNS record, then the Bicep custom domain resource).
Rollback: the website is independent; disable the workflow or delete the Static Web App. The server endpoint can be removed without affecting the app.

## Open Questions

- Which domain should the website use (for example `leakout.hexmaster.nl`), or is the Static Web App's default host name enough for now?
- May players' Play Games display names be shown publicly on the website? If yes, the privacy policy needs a paragraph; if no, the leaderboard shows only positions and scores.
- Should the website also host the privacy policy (a public URL the Play Store listing can point to)?
- Does `hexmasternl/leak-out-game` have (or should it get) issue templates for bugs and feature requests?
- Which questions belong in the FAQ? The first version uses questions drawn from the rules, the Play Games sign-in, scores and achievements.
