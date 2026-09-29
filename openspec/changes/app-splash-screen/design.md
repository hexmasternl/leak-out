## Context

Leak Out targets min SDK 26 and uses Kotlin + Jetpack Compose (see `docs/project-plan.md`). The `app` module, `MainActivity` and `LeakOutTheme` come from change `app-welcome-screen`, which this change depends on. The Deep Flow design system (`docs/design-system.md`, v2) defines `surface.deep = #0C1A2A`, the liquid ramp, Wendy One for display text, and the logo treatment in `docs/design-system.html` (`.hero .logo`). That treatment is a 180° linear gradient `liquid.glow 0% → liquid.300 35% → liquid.500 70% → liquid.700 100%`, clipped to text, with `drop-shadow(0 6px 24px rgba(25,198,242,.35))`.

Platform constraint: since Android 12 the system always draws its own splash window on cold and warm starts, and the app cannot disable it. That window can only show a background colour plus one icon drawable. The icon is masked to a circle (about 160 dp of visible content) and cannot render arbitrary text, gradients-on-text or glows reliably. A styled two-line wordmark therefore cannot live *inside* the system splash.

## Goals / Non-Goals

**Goals:**
- A seamless dark launch: system splash → in-app wordmark → welcome screen, all on `#0C1A2A` with no colour flash.
- The wordmark "LEAK / OUT" in the design system's liquid gradient + glow, centred.
- The same behaviour on API 26–30 and 31+.
- A reusable `LiquidWordmark` composable, so the welcome title can adopt the same treatment later.

**Non-Goals:**
- Loading real assets or data during the splash (there is nothing to load yet). The duration is a fixed minimum for now.
- An animated liquid-fill logo or a sound on launch.
- Changing the launcher icon (it is a separate Play Store asset task).
- Restyling the welcome screen title (it may reuse `LiquidWordmark` in its own change).

## Decisions

- **Two stages: a blank system splash, then a Compose splash.** The system splash (via `core-splashscreen`) gets `windowSplashScreenBackground = @color/surface_deep` and a fully transparent `windowSplashScreenAnimatedIcon` (a small transparent vector). It is dismissed on the first Compose frame, and the first frame is the in-app `SplashScreen` on the same colour, so the handoff is invisible.
  - *Alternative:* bake the wordmark into the splash icon drawable. Rejected. The circular mask and size limits make the text small, the glow gets clipped, gradient-filled text in a VectorDrawable needs glyph paths exported from the font, and pre-31 devices would still need a separate path.
  - *Alternative:* a separate `SplashActivity`. Rejected. On 31+ it causes a double splash, adds an extra activity transition, and Google explicitly discourages it.
- **`core-splashscreen` over platform attributes only.** `Theme.SplashScreen` backports the API to 26–30, so one theme definition covers every level. `postSplashScreenTheme` switches `MainActivity` to the normal app theme, whose `windowBackground` is also `surface_deep`. That keeps configuration changes and activity recreation dark too.
- **In-app splash state lives in `MainActivity`'s composition as a two-state flag** (`Splash` → `Welcome`), held with `rememberSaveable`, so configuration changes and process-alive recreation never replay it. The transition runs from a `LaunchedEffect`: wait `max(0, 1200 ms − elapsed)`, then flip the state. `AnimatedContent`/`Crossfade` with a 320 ms tween does the switch (`motion.pop` duration, eased, not sprung, because it is a fade). A navigation library is not needed for a single one-way step. When a nav graph arrives later, the splash becomes its start destination with `popUpTo(inclusive = true)`.
- **Gradient text via `Brush` in `TextStyle`.** Compose's `TextStyle(brush = Brush.verticalGradient(colorStops = arrayOf(0f to liquidGlow, 0.35f to liquid300, 0.70f to liquid500, 1f to liquid700)))` fills the glyphs, matching the CSS stops exactly. The two lines are one `Text("LEAK\nOUT", textAlign = Center)`, so the gradient spans the whole block, as in the HTML logo. Line height is 0.95–1.0 of the font size.
- **Glow as a blurred copy drawn behind.** `TextStyle.shadow` only takes a solid colour and has a limited blur, but it is the cheapest route: `Shadow(color = liquid500.copy(alpha = .35f), offset = Offset(0f, 6.dp), blurRadius = 24.dp)` reproduces the CSS drop-shadow. That is the chosen approach. If it looks too weak on device, fall back to drawing the same text a second time underneath with `Modifier.blur(24.dp)` (API 31+) and keep `Shadow` on older APIs.
- **Size.** The font size is computed from `BoxWithConstraints`: `min(maxHeight * 0.30, maxWidth * 0.22)` per line, clamped to at least 56 sp (`display.hero`). It is expressed in dp→sp *without* font scaling, so the 130 % system font scale cannot overflow the layout, matching the design system's "display sizes are fixed" rule.
- **The Deep palette is forced.** `SplashScreen` reads the Deep `LeakOutColors` instance directly instead of the theme's `CompositionLocal`, and `colors.xml` holds only the deep value, so a Daylight selection never changes the splash.
- **System bars.** `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))` in `MainActivity`, before `setContent`. The splash background draws under the bars. This is compatible with the welcome screen's immersive mode.
- **Reduced motion.** Read `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`. If it is set, skip the fade-in and cross-fade (use `snap()`) and keep the static shadow glow. The minimum duration still applies so the wordmark stays readable.
- **Fonts.** Bundle `wendy_one_regular.ttf` in `res/font/` if `app-welcome-screen` has not already done so (that change predates design system v2 and mentions Fredoka). The shared `LeakOutType.displayHero` style is the base, and the wordmark overrides size and brush.

## Risks / Trade-offs

- [The 1.2 s minimum adds launch latency for returning players] → It is short, it only happens on cold and warm starts, and it can later become "until assets are ready" with the minimum lowered. A tap-to-skip can be added if testers find it slow.
- [The glow via `Shadow` may look weaker than the CSS reference] → Check on the emulator against `docs/design-system.html`. Fall back to the blurred-copy technique described above.
- [Transparent splash icon: some OEM skins may still show a default icon frame] → Also set `windowSplashScreenIconBackgroundColor` to `surface_deep`, and verify on an API 31+ emulator image.
- [`app-welcome-screen` spec says the welcome screen is "the first screen after launch"] → The splash is a transient brand screen, not an interactive screen. When archiving, update the welcome-screen scenario wording to "first interactive screen after the splash", or archive this change afterwards with a MODIFIED delta.
- [Welcome-screen change still references the v1 palette/fonts] → Out of scope here. This change only adds the Deep tokens it needs (`surfaceDeep`, `liquid*`) if they are missing.

## Open Questions

- Should the wordmark gently "breathe" (a glow pulse on `motion.pulse`) while visible, or stay static? Default: static, so it adds no motion and no cost. It is easy to add later.
