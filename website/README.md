# Leak Out website

The public website of Leak Out: home, rules, FAQ and leaderboard, in English, Dutch, French, German and Spanish. It is a static [Hugo](https://gohugo.io) site with its own theme, hosted on an Azure Static Web App.

```
website/
├── src/              the Hugo site (configuration, content per language, layouts, styles, scripts, images)
├── infrastructure/   Bicep for the Azure Static Web App
└── tests/            unit tests for the leaderboard script (Node's built-in test runner)
```

## Build, run and test locally

Install **Hugo extended 0.165.0**, the version the workflows use (change it in both `website-*.yml` workflows and here together), and Node 20 or newer. From the repository root:

```sh
hugo server --source website/src                 # live preview on http://localhost:1313
hugo --source website/src --panicOnWarning       # production build into website/src/public
cd website && npm test                           # leaderboard tests; no npm install needed
```

The site works without JavaScript, except for the leaderboard. To try the leaderboard against a local score server, override its address: `HUGO_PARAMS_SCORESERVERURL=http://localhost:5000 hugo server --source website/src`.

The translations were written by one contributor; native speakers are welcome to review `content/<lang>/` and `i18n/<lang>.yaml`.

## Deployment

Two workflows handle the website, separately from the app and the server:

| Workflow | Runs on | Does |
| --- | --- | --- |
| `.github/workflows/website-pull-request.yml` | every pull request into `development` | Reports the check **website / build**: builds the site with `--panicOnWarning`, runs `npm test` and builds the Bicep. Skipped (counts as passed) when the pull request does not change `website/` or a `website-*` workflow. |
| `.github/workflows/website-deploy.yml` | a push to `main` that changes `website/` or the workflow, or by hand (`workflow_dispatch`) | Builds and tests the site, deploys `infrastructure/main.bicep` at subscription scope, reads the Static Web App's deployment token, uploads the built site and smoke-tests every language's home page. |

The infrastructure is the resource group `rg-leakout-web-prod-weu` with the Static Web App `stapp-leakout-prod-weu` (Free tier, West Europe). It is separate from the score server's resource group, so either can be changed or removed on its own.

### Secrets and variables

The deploy job signs in to Azure with OpenID Connect, using **the same app registration and federated credential as the server deployment**: it runs in the GitHub environment `production`, so the existing credential with subject `repo:hexmasternl/leak-out:environment:production` covers it.

| Name | Kind | Required | Used for |
| --- | --- | --- | --- |
| `AZURE_PROD_CLIENT_ID` | secret | yes | Client id of the app registration with the federated credential (shared with the server) |
| `AZURE_PROD_TENANT_ID` | secret | yes | Its tenant id (shared with the server) |
| `AZURE_PROD_SUBSCRIPTION_ID` | secret | yes | The subscription to deploy into (shared with the server) |
| `WEBSITE_BASE_URL` | variable | no | The site's public address, for example `https://leakout.hexmaster.nl/`; gives the `hreflang` links absolute URLs. Without it links are root-relative. |
| `LEAKOUT_SCORE_SERVER_URL` | variable | no | The score server the leaderboard reads; defaults to `scoreServerUrl` in `src/hugo.toml` |

**No deployment token is stored anywhere.** The workflow reads it with `az staticwebapp secrets list` during each run and masks it in the log. The app registration needs permission to create resource groups and deploy at subscription scope, which it already has for the server (Contributor on the subscription).

If the website should ever use a different app registration: create one, give it Contributor on the subscription, add a federated credential for the `production` environment of `hexmasternl/leak-out` (Entra ID > App registrations > Certificates & secrets > Federated credentials > GitHub Actions deploying Azure resources), and point the three secrets at it.

### First deployment

1. Merge the website into `main` (through `development`), or run **website-deploy** by hand from the Actions tab (`workflow_dispatch`). The first run creates the resource group and the Static Web App.
2. The run's summary shows the default address, `https://<name>.azurestaticapps.net`. Add that origin (and the custom domain, once set) to the score server's allowed CORS origins so the leaderboard can load scores.
3. Make **website / build** a required status check for `development` in the branch protection rules.

### Custom domain (optional)

1. Create a CNAME record from the domain (for example `leakout.hexmaster.nl`) to the Static Web App's default host name.
2. Set `customDomain` in `infrastructure/main.bicepparam` and push to `main`; Azure validates the CNAME and issues the certificate.
3. Set the `WEBSITE_BASE_URL` variable to the new address and add the origin to the score server's CORS settings.

### Rolling back

The website is independent of the app and the server: disable `website-deploy.yml` or delete the resource group `rg-leakout-web-prod-weu`.

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
