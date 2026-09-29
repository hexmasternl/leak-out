# Leak Out

A 2D pipe-building puzzle game for Android.

## Repository layout

**All application source code lives in the `src/` directory.** The Gradle build (settings, wrapper, version catalog) and every module (`core`, `app`) sit under `src/`. Nothing else in the repository is part of the app build.

```
leak-out/
├── src/          the app: Gradle root, core (pure Kotlin) and app (Android) modules
├── docs/         project plan and design system
└── openspec/     specs and change proposals
```

Run Gradle commands from `src/`:

```sh
cd src
./gradlew :core:test          # Windows: .\gradlew.bat :core:test
```

See [docs/project-plan.md](docs/project-plan.md) for the full design and roadmap.
