# Tasks

## 1. Hugo site skeleton

- [x] 1.1 Create `website/src/` with `hugo.toml` (five languages, English default at `/`, site params for the Play Store URL, the leak-out-game issue URLs, the score server base URL and `levelCount`), pin the Hugo extended version in `website/README.md`; verify `hugo --source website/src --panicOnWarning` builds an empty site
- [x] 1.2 Write the base layout, header navigation, language switcher (using `.Translations`), footer with "Report a problem" / "Request a feature" links, and `i18n/{en,nl,fr,de,es}.yaml` UI strings; verify the build and that every generated page has `<html lang>` matching its language and the feedback links point to `hexmasternl/leak-out-game`
- [ ] 1.3 Add the design system colour and type tokens as CSS custom properties and responsive styles; verify in a browser at 360, 768 and 1280 px wide that no page scrolls horizontally

## 2. Assets and content

- [x] 2.1 Copy the icon, feature graphic, store screenshots and pipe artwork into `website/src/` and list their origin in `website/README.md`; verify `grep -rn "\.\./" website/src --include=*.html --include=*.toml --include=*.md` finds no references outside `website/`, and a build from a copy of only `website/` succeeds
- [x] 2.2 Write the home page in all five languages (hero, tagline, screenshots, selling points, Get it on Google Play link); verify each language's home page renders and the Play Store link is correct
- [x] 2.3 Write the rules page in all five languages using the game's terms (playing field, stack, sections, start and finish pipes, liquid, points, stars); verify each language has the page and the switcher moves between them
- [x] 2.4 Write the FAQ page in all five languages; verify each language has the page with questions and answers

## 3. Leaderboard page

- [ ] 3.1 Add the leaderboard page in all five languages with the level chooser (number input, previous/next, bounded by `levelCount`) and an empty table, plus `assets/js/leaderboard.js` that fetches `/api/public/levels/{level}/scores/top` and renders the entries and the loading, empty and error (Try again) states with translated strings; verify with unit tests for the rendering and state logic (for example with Node's built-in test runner) that pass
- [ ] 3.2 Verify the page in a browser against a stubbed endpoint (a local JSON file or mock server): level 1 loads on open, choosing another level reloads, 25 entries max, empty and error states and Try again work

## 4. Server public top scores (after `repo-merge-server`)

- [ ] 4.1 Add `GET /api/public/levels/{level}/scores/top` as an anonymous minimal API endpoint that reuses the level ranking with a limit of 25 and returns level plus entries (position, display name, score) without player ids; verify with integration tests for 40 scores, no scores, level 0 and a request without a token
- [ ] 4.2 Add the CORS policy (allowed origins from configuration, set in Bicep app settings) and per-client rate limiting plus `Cache-Control: public, max-age=60` to that endpoint; verify with tests for an allowed origin, a disallowed origin and a 429 after exceeding the limit
- [ ] 4.3 Document the endpoint in the server's docs; verify `dotnet test` on `server/src/` passes and the coverage gate holds

## 5. Infrastructure and deployment

- [ ] 5.1 Write `website/infrastructure/main.bicep` and `main.bicepparam` for the Static Web App (Free tier, optional custom domain parameter); verify `az bicep build --file website/infrastructure/main.bicep` reports no errors
- [ ] 5.2 Add `.github/workflows/website-pull-request.yml` (changes job, Hugo build with pinned version and `--panicOnWarning`, Bicep build, always reports a result); verify with `actionlint` if available and with a pull request that changes `website/`
- [ ] 5.3 Add `.github/workflows/website-deploy.yml` (push to `main` on `website/**` and the workflow, `workflow_dispatch`; OIDC Azure login, Bicep deployment, deployment token fetched at run time, `Azure/static-web-apps-deploy` with `skip_app_build`); verify with `actionlint` if available and that no token appears in the repository
- [ ] 5.4 Document the Azure federated credential, the GitHub variables and secrets, and the first manual run in `website/README.md`; add `website/` to `docs/repository-layout.md` and `README.md`; verify every secret and variable used by the workflows is documented

## 6. Integration

- [ ] 6.1 Run an automated accessibility check (for example `pa11y-ci` or axe) over the built site and fix what it reports; verify it reports no missing alternatives, titles, language attributes or contrast failures
- [ ] 6.2 After merging to `main`, run `website-deploy.yml`, then verify the live site in all five languages, the feedback links, and the leaderboard against the deployed server endpoint
