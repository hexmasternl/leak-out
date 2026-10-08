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
