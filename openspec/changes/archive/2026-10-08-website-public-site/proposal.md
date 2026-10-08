# Proposal

## Why

Leak Out has no home outside the Play Store: there is nowhere to send players to learn the rules, read answers to common questions, see how others score, or report a problem. Issues and feature requests can already be filed in the public [leak-out-game](https://github.com/hexmasternl/leak-out-game) repository, but nothing points players there. A public, attractive website promotes the game and gives players one place for all of this. Tracked in [#116](https://github.com/hexmasternl/leak-out/issues/116).

## What Changes

- A new static **Hugo** website in `website/src/`, in five languages: English (default), Dutch, French, German and Spanish, with a language switcher on every page.
- Pages: a **home** page that promotes the game (hero with the feature graphic, screenshots, the main selling points, a Get it on Google Play link), a **rules** page that explains how to play (the playing field, the stack, sections, start and finish pipes, the flowing liquid, points and stars), an **FAQ** page, and a **leaderboard** page.
- **Feedback links**: every page links to "Report a problem" and "Request a feature", which open a new issue (bug or feature request) in the public `hexmasternl/leak-out-game` repository.
- **Leaderboard**: the visitor picks a level and sees the top 25 scores of that level (rank, player display name, score), loaded in the browser from the score server.
- **Server**: a new anonymous, read-only endpoint on the score server that returns the top 25 scores of a level (position, display name, score; no player ids), rate limited and cacheable, with CORS allowed only for the website's origins.
- **Visuals follow the Leak Out design system** (`docs/design-system.md`): colours, typography and the pipe artwork. Every asset the site uses (icon, feature graphic, screenshots, pipe images) is **copied** into `website/src/`, so the site never depends on files outside `website/`.
- **Hosting**: the site is deployed to an **Azure Static Web App**; its infrastructure is described in **Bicep** in `website/infrastructure/`.
- **Deployment**: a GitHub Actions workflow builds the site with Hugo and deploys the infrastructure and the site on pushes to `main` that change `website/`; pull requests that change `website/` get a build check.

## Capabilities

### New Capabilities
- `public-website`: the website's pages and content, the five languages and the language switcher, the links to the Play Store and to the public issue tracker, the look and feel, self-contained assets and accessibility.
- `website-leaderboard`: choosing a level on the website and showing its top 25 scores, with loading, empty and error states.
- `website-deployment`: where the website's sources and infrastructure live, and how the website is built, checked and deployed to Azure Static Web Apps.
- `server-public-top-scores`: the score server's anonymous, read-only top 25 of a level for the website.

### Modified Capabilities
- (none)

## Impact

- New `website/src/` (Hugo site: configuration, content per language, layouts, styles, a small leaderboard script, copied images) and `website/infrastructure/` (Bicep for the Static Web App).
- New workflows `.github/workflows/website-deploy.yml` and `website-pull-request.yml`; new GitHub secrets/variables for the Azure deployment, documented in `website/README.md`.
- Server (`server/src/`, after `repo-merge-server`): a new anonymous endpoint, a CORS policy and rate limiting, with tests.
- `docs/repository-layout.md` (from `repo-merge-server`) and `README.md` mention the `website/` folder.
- **Depends on** `repo-merge-server` (#115) for the server endpoint, since the server must live in this repository first. The website itself can be built before that; until the endpoint is deployed the leaderboard page shows its error state.
- No change to the Android app.
