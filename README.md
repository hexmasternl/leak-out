# Leak Out

A 2D pipe-building puzzle game for Android.

## Repository layout

The repository holds the Android app and its online game server, each in its own folder with its own build and release. **The app's source code lives in `app/src/`**: the Gradle build (settings, wrapper, version catalog) and every module (`core`, `app`) sit there. The server's .NET solution lives in `server/src/` and its Azure templates in `server/infrastructure/`. Details: [docs/repository-layout.md](docs/repository-layout.md).

```
leak-out/
├── app/
│   └── src/      the app: Gradle root, core (pure Kotlin) and app (Android) modules
├── server/
│   ├── src/      the game server: .NET solution and projects
│   └── infrastructure/  Bicep templates for Azure
├── docs/         project plan, design system, repository layout
└── openspec/     specs and change proposals
```

Run Gradle commands from `app/src/`:

```sh
cd app/src
./gradlew :core:test          # Windows: .\gradlew.bat :core:test
```

See [docs/project-plan.md](docs/project-plan.md) for the full design and roadmap.
