# Leak Out – Design System

Version 2.0 · Supersedes `design-system-slim.md` · Companion style guide: `design-system.html`

Leak Out is a pipe-building puzzle for Android. The design system defines how the game looks, moves and sounds so that every screen, tile and dialog feels like it came from the same workshop. It is written for a Jetpack Compose implementation, and every value below maps to a Compose token.

---

## 1. Direction: *Deep Flow*

The old system was a flat cartoon: white cards, thick ink outlines, primary-colour buttons. The new direction keeps the playfulness but moves it somewhere more atmospheric: **a dark, wet, glowing workshop**. Liquid is the hero. It is the brightest thing on screen, it glows, and everything else recedes into layered slate so the eye is always on the flow.

### 1.1 Principles

1. **Liquid is light.** The aqua liquid is the only fully saturated, luminous element in the interface. Nothing else competes with it. Panels, pipes and text are desaturated slate and steel.
2. **Depth through glow and layering, not outlines.** No 3 px ink borders. Surfaces stack with soft shadows, one-pixel translucent highlights on their top edge, and coloured glows where something is alive (flowing liquid, the primary button, the next stack piece).
3. **Chunky where it matters.** Wendy One gives titles, level numbers and buttons their weight. Everything else uses the calmer Varela Round so the screen never shouts.
4. **Motion behaves like water.** Springs for placing tiles, linear fill for liquid, easing that decelerates into place. Nothing snaps instantly; nothing bounces for the sake of it.
5. **Brass means "touch me".** The single warm accent is reserved for the primary action, the current stack piece and the start valve. If it is brass, the player can act on it.

### 1.2 Mood references

Underwater engine rooms, backlit aquarium glass, copper fittings under blue light, arcade cabinet glow. Not: flat material cards, white space, pastel.

---

## 2. Colour

All colours are defined once as tokens and never used as raw hex in components. Values are given for the default **Deep** (dark) theme; the **Daylight** (light) theme remaps the surface and text tokens only. Liquid, brass, status and steel are the same in both.

### 2.1 Surfaces (Deep theme)

| Token | Hex | Usage |
|-------|-----|-------|
| `surface.abyss` | `#07111C` | System bars, behind-everything background |
| `surface.deep` | `#0C1A2A` | Game screen background |
| `surface.base` | `#122538` | Panels, the board plate, bottom sheets |
| `surface.raised` | `#19324A` | HUD chips, cards, level tiles, stack rail |
| `surface.overlay` | `#21405C` | Pressed/hover states, secondary buttons, empty cell fill |
| `surface.scrim` | `#07111C` at 72 % | Behind dialogs |
| `line.subtle` | `#FFFFFF` at 8 % | Panel borders, grid lines |
| `line.strong` | `#FFFFFF` at 16 % | Secondary button border, focused cell |
| `line.highlight` | `#FFFFFF` at 22 % | One-pixel top-edge highlight on raised surfaces |

### 2.2 Surfaces (Daylight theme)

| Token | Hex |
|-------|-----|
| `surface.abyss` | `#DCE9F3` |
| `surface.deep` | `#E8F1F8` |
| `surface.base` | `#FFFFFF` |
| `surface.raised` | `#F5F9FD` |
| `surface.overlay` | `#E1ECF5` |
| `line.subtle` | `#0F2233` at 10 % |
| `line.strong` | `#0F2233` at 18 % |
| `line.highlight` | `#FFFFFF` at 80 % |

### 2.3 Liquid (hero colour)

| Token | Hex | Usage |
|-------|-----|-------|
| `liquid.glow` | `#9DF3FF` | Meniscus head, sparkle highlights, glow colour |
| `liquid.300` | `#5EE7FF` | Liquid top highlight, links, tertiary button text |
| `liquid.500` | `#19C6F2` | Liquid body, progress bars, active indicators |
| `liquid.700` | `#0A8DC2` | Liquid shadow edge, pressed liquid buttons |
| `liquid.900` | `#065A82` | Liquid in disabled/inactive state, deep tint |
| `liquid.tint` | `#19C6F2` at 14 % | Selected cell wash, info chip background |

Liquid is always rendered as a vertical gradient `liquid.300 → liquid.500 → liquid.700` with a `liquid.glow` outer glow of 12–24 dp when it is flowing (see 7.3).

### 2.4 Brass (action accent)

| Token | Hex | Usage |
|-------|-----|-------|
| `brass.100` | `#FFE9B0` | Text/icon on dark brass, star highlight |
| `brass.300` | `#FFCF5C` | Primary button top of gradient, active star |
| `brass.500` | `#F5A623` | Primary button base, valve wheel, current stack piece frame |
| `brass.700` | `#C97A12` | Primary button lip (bottom edge), pressed state |
| `brass.900` | `#7A4708` | Brass shadow, text on brass.100 |
| `brass.tint` | `#F5A623` at 16 % | Warning chip background, brass focus ring |

### 2.5 Status

| Token | Hex | Usage |
|-------|-----|-------|
| `leak.500` | `#FF5A5F` | Leak splash, lose dialog title, destructive button |
| `leak.700` | `#C9353B` | Destructive button lip, leak shadow |
| `leak.tint` | `#FF5A5F` at 16 % | Locked-cell flash, error chip background |
| `success.500` | `#3DDC97` | Finish drain ring, win dialog title, complete tick |
| `success.700` | `#1FA971` | Success button lip |
| `success.tint` | `#3DDC97` at 16 % | Completed level tile wash |
| `reservoir.300` | `#C7B5FF` | Reservoir liquid highlight |
| `reservoir.500` | `#A78BFA` | Reservoir tank tint, "slow" badge |
| `reservoir.700` | `#7C5CE6` | Reservoir tank shadow |

The reservoir never recolours the liquid. Liquid stays aqua; the *tank* is violet so the player learns "violet = slow" without the flow ever changing colour.

### 2.6 Steel (pipes)

| Token | Hex | Usage |
|-------|-----|-------|
| `steel.100` | `#C9D6E2` | Top-edge specular highlight on pipe body |
| `steel.300` | `#8FA3B8` | Flange faces, secondary highlight |
| `steel.500` | `#5C7189` | Pipe body |
| `steel.700` | `#3A4B60` | Pipe body shadow side, flange edges |
| `steel.900` | `#22303F` | Pipe bore (the hollow the liquid flows through) |

### 2.7 Text

| Token | Deep | Daylight | Usage |
|-------|------|----------|-------|
| `text.primary` | `#F2F7FB` | `#0F2233` | Headings, body |
| `text.secondary` | `#A9BACB` | `#4A5F74` | Descriptions, HUD labels |
| `text.muted` | `#6E8296` | `#7C8FA2` | Captions, disabled |
| `text.onBrass` | `#2A1A03` | same | Primary button label |
| `text.onLiquid` | `#04202E` | same | Label on liquid-filled chips |
| `text.onStatus` | `#FFFFFF` | same | Label on leak/success buttons |

### 2.8 Contrast

Minimum ratios (WCAG AA): 4.5:1 for body text, 3:1 for display text at 20 sp and above and for UI boundaries. Verified pairs:

| Pair | Ratio |
|------|-------|
| `text.primary` on `surface.base` | 14.6:1 |
| `text.secondary` on `surface.base` | 8.1:1 |
| `text.onBrass` on `brass.500` | 8.9:1 |
| `liquid.500` on `surface.deep` | 8.2:1 |
| `leak.500` on `surface.deep` | 5.6:1 |
| `success.500` on `surface.deep` | 10.4:1 |

---

## 3. Typography

Two families, both available on Google Fonts under the SIL Open Font License. Bundle them in `res/font/` as `wendy_one_regular.ttf` and `varela_round_regular.ttf`; do not rely on downloadable fonts for a game.

- **Wendy One** – display. Single weight (400). Chunky, rounded, high impact. Used for the logo, headings, level numbers, button labels and big numbers. Hierarchy comes from size, colour and casing, never from weight.
- **Varela Round** – UI. Single weight (400). Friendly, even, very legible at small sizes. Used for body copy, HUD labels, captions and any running text.

### 3.1 Scale

| Token | Family | Size | Line height | Letter spacing | Case | Usage |
|-------|--------|------|-------------|----------------|------|-------|
| `display.hero` | Wendy One | 56 sp | 1.00 | 0.5 sp | Title | Title screen logo |
| `display.xl` | Wendy One | 40 sp | 1.05 | 0.25 sp | Title | Win / lose headline |
| `display.lg` | Wendy One | 28 sp | 1.10 | 0 | Title | Dialog titles, screen titles |
| `display.md` | Wendy One | 22 sp | 1.15 | 0 | Title | Section headers, level tile number |
| `display.sm` | Wendy One | 18 sp | 1.20 | 0.25 sp | Title | Button labels, HUD values |
| `display.xs` | Wendy One | 14 sp | 1.20 | 0.5 sp | UPPER | Chips, tabs, eyebrow labels |
| `body.lg` | Varela Round | 18 sp | 1.50 | 0 | Sentence | Onboarding, dialog body |
| `body.md` | Varela Round | 16 sp | 1.50 | 0 | Sentence | Default body |
| `body.sm` | Varela Round | 14 sp | 1.45 | 0 | Sentence | Secondary copy, list items |
| `label.md` | Varela Round | 13 sp | 1.30 | 0.4 sp | UPPER | HUD labels ("LEVEL", "NEXT") |
| `caption` | Varela Round | 12 sp | 1.35 | 0.2 sp | Sentence | Hints, footers |
| `numeric.lg` | Wendy One | 32 sp | 1.00 | 0 | – | Countdown, score |
| `numeric.md` | Wendy One | 20 sp | 1.00 | 0 | – | HUD counters |

### 3.2 Rules

- Wendy One is never set below 14 sp and never used for sentences longer than about six words.
- Countdown and score use fixed-width layout (each digit in an equal-width box) so numbers do not jitter as they change. Wendy One has no tabular figures.
- Uppercase is reserved for `display.xs` and `label.md`. Buttons are Title Case.
- Text on liquid or brass is always the dark `onX` token; white text on those colours fails contrast.

---

## 4. Spacing, shape and depth

### 4.1 Spacing

A 4 dp base grid. Named steps: `space.1 = 4`, `space.2 = 8`, `space.3 = 12`, `space.4 = 16`, `space.5 = 20`, `space.6 = 24`, `space.8 = 32`, `space.10 = 40`, `space.12 = 48`.

- Screen edge gutter: 16 dp phones, 24 dp tablets.
- Inside panels: 16 dp.
- Between grid cells: 4 dp.
- Between stacked buttons: 12 dp.

### 4.2 Shape

| Token | Radius | Usage |
|-------|--------|-------|
| `radius.xs` | 6 dp | Chips, small badges |
| `radius.sm` | 10 dp | Grid cells, stack pieces, inputs |
| `radius.md` | 16 dp | Buttons, HUD chips, level tiles |
| `radius.lg` | 24 dp | Panels, bottom sheets, dialogs |
| `radius.xl` | 32 dp | Board plate, phone-edge containers |
| `radius.pill` | 999 dp | Toggles, progress tracks, count badges |

### 4.3 Depth ("glow stack")

| Level | Name | Recipe | Used for |
|-------|------|--------|----------|
| 0 | Flat | none | Text, inline chips |
| 1 | Panel | 1 dp `line.highlight` on top edge · shadow `0 4 12` black 35 % | HUD bar, board plate, stack rail |
| 2 | Raised | level 1 + 1 dp `line.subtle` border · shadow `0 8 24` black 45 % | Cards, level tiles, buttons at rest |
| 3 | Floating | shadow `0 24 64` black 60 % · scrim behind | Dialogs, pause sheet |
| G | Glow | `0 0 24` in the element's colour at 45 % | Flowing liquid, primary button, current stack piece, finish ring |

Glows are additive to a level, never a replacement. A raised primary button is level 2 + G in brass.

### 4.4 The tactile lip

Buttons keep a hint of the old "juicy" press physics without outlines. A **lip** is a 4 dp solid band on the bottom edge in the darker shade of the button colour (`brass.700`, `leak.700`, `success.700`, or `surface.abyss` for glass). On press the button translates down 2 dp and the lip shrinks to 2 dp. The lip is the only place a hard edge appears in the UI.

---

## 5. Components

### 5.1 Buttons

| Variant | Fill | Label | Lip | Glow | Use |
|---------|------|-------|-----|------|-----|
| Primary | gradient `brass.300 → brass.500` | `text.onBrass`, `display.sm` | `brass.700` | brass | One per screen: Play, Next Level, Retry |
| Secondary (glass) | `surface.overlay`, 1 dp `line.strong` | `text.primary` | `surface.abyss` | none | Level Select, Settings, Replay |
| Tertiary | none | `liquid.300` | none | none | Skip, Not now |
| Destructive | gradient `leak.500 → leak.700` | `text.onStatus` | `leak.700` | none | Give up, Reset progress |
| Success | gradient `success.500 → success.700` | `text.onStatus` | `success.700` | success | Next Level on win dialog (replaces primary there) |

- Height 56 dp (large), 48 dp (medium), 40 dp (small). Radius `radius.md`. Horizontal padding 24 dp.
- Pressed: translate 2 dp down, lip 2 dp, glow +30 %. Duration 90 ms.
- Disabled: fill `surface.overlay`, label `text.muted`, no lip, no glow, 60 % opacity.
- Icon buttons: 48 dp circle, glass variant, icon 24 dp `text.primary`.

### 5.2 Chips and badges

- **Chip**: 32 dp tall, `radius.pill`, `surface.raised`, `display.xs` label, optional 16 dp leading icon. Tinted variants use the status tints with matching 500 text.
- **Count badge**: 20 dp circle, `brass.500`, `display.xs` in `text.onBrass`.
- **"Slow" badge** on reservoir tiles: `reservoir.500` pill with an hourglass icon.

### 5.3 HUD bar

A level 1 panel across the top of the game screen that is clearly set apart from the stack rail and the board below it.

**Panel**

- Height 48 dp. The stack column and the board get the height below it, so this is fixed; it must still let the largest stack fit on a 360 dp tall screen.
- Fill: vertical gradient `surface.raised` (top) → `surface.deep` (bottom).
- Depth level 1: 1 dp `line.highlight` on the top edge, shadow `0 4 12` black 35 %. The bottom edge gets a 1 dp `line.subtle` line so the panel reads as its own surface over `surface.deep`.
- `radius.lg` on the bottom corners only; the top corners run into the status bar.
- 16 dp (`space.4`) horizontal padding, 12 dp (`space.3`) between items.

Left to right, all items centred vertically:

1. **Level** – "Level 7" in `display.sm`, `text.primary`. Against the left padding.
2. **Flow speed** – the 20 dp speed icon in `liquid.300` and the speed as a whole number in `display.sm`, `liquid.300`, 4 dp apart, with no unit. The number is the speed in cells per second × 10, rounded half up (0.2 → 2, 0.8 → 8, 1.25 → 13). The engine and level files keep cells per second.
3. **Countdown bar** – takes all free space between the flow speed and the score bar, at least 96 dp wide.
   - Track: `radius.pill`, 8 dp tall, `line.subtle`.
   - Fill: from the left, the remaining countdown divided by the level's `flow.initialDelayMs`. Full when the attempt starts, empty when the liquid starts. It follows flow time, so it freezes on pause and drains 10× in fast forward.
   - Colour by time left (flow time): `liquid.500` above 5 s, `brass.500` at 5 s or less, `leak.500` at 2 s or less. The fill carries a `G` glow in the same colour at 45 %, with the blur reduced to 8 dp so it stays inside the 48 dp panel.
   - Once the liquid flows, the bar fades out (see 7.2) and its slot stays empty, so no other item moves. A level with `flow.initialDelayMs` 0 never shows it.
4. **Score bar** – directly left of the pause button, fixed 200 dp wide, so it never changes size with the score.
   - Track: `radius.pill`, 20 dp tall, `surface.abyss` with a 1 dp `line.subtle` border.
   - Fill: from the left, horizontal gradient `brass.500 → brass.300`, the score divided by the level's `stars.three`, clipped to the track's pill shape. Full at or above `stars.three`.
   - Markers: 2 dp vertical lines in `line.strong`, the full track height, at `stars.one / stars.three` and `stars.two / stars.three` along the bar. The three-star threshold is the end of the bar.
   - Number: the actual score (also past `stars.three`), centred on the bar, `numeric.md` in `text.primary`, with the device's digit grouping ("1,250"). For legibility over both the brass fill and the dark track it has a 1 dp `surface.abyss` text shadow (offset 0, 1 dp; blur 2 dp). It grows from its centre.
   - Reaching each threshold sparkles at that point (see 7.2). There are no separate HUD stars; the level complete dialog keeps its stars.
5. **Pause** – 48 dp touch target with the 24 dp cog in `text.primary` (`text.muted` while disabled). Against the right padding.

Screen reader: "Level 7", "Flow speed 8", "Flow starts in 7 seconds" (whole seconds, rounded up; not read once the bar has gone), "Score 150, 1 of 3 stars" (the score bar is one node including its markers) and "Pause menu".

### 5.4 Stack rail

A vertical level 1 panel on the trailing edge of the board (portrait) or the trailing side of the screen (landscape). Width equals one cell plus 16 dp padding.

- Holds `visibleSize` pieces, top to bottom. The **top piece** is the current one: it is 8 % larger, framed in a 2 dp `brass.500` ring with brass glow, and labelled "NEXT" in `label.md` above it.
- Pieces below fade: opacity 100 %, 80 %, 65 %, 50 %, 40 %.
- When a piece is placed, the remaining pieces slide up with `motion.settle` and a new piece fades in at the bottom.

### 5.5 Level tile (level select)

88 × 104 dp, level 2 raised, `radius.md`.

- Number in `display.md` centred; three 14 dp stars below.
- **Locked**: `surface.base`, number in `text.muted`, small lock icon, no stars.
- **Unlocked**: `surface.raised`, number `text.primary`, empty star outlines.
- **Completed**: `success.tint` wash, number `success.500`, earned stars in `brass.300`.
- **Current** (highest unlocked): additional brass glow and a 2 dp `brass.500` ring.

### 5.6 Dialogs and sheets

Level 3 floating panel, `radius.lg`, `surface.base`, 24 dp padding, max width 360 dp, centred over `surface.scrim`. Entry: scale 0.92 → 1 with `motion.pop`; scrim fades in over 160 ms.

- **Win**: eyebrow "LEVEL COMPLETE" in `display.xs` `success.500`; three stars (earned in `brass.300` with a pop-in stagger of 80 ms, missed in `line.strong`); headline `display.xl`; score row; buttons: Success "Next Level" + glass "Replay" + tertiary "Levels".
- **Leak**: eyebrow "LEAK!" in `leak.500`; a short splash animation; headline `display.xl` `text.primary`; body `body.md` `text.secondary` telling the player where it leaked; Primary "Try Again" + tertiary "Levels".
- **Pause**: title "Paused" `display.lg`; glass "Resume" as primary emphasis, glass "Restart", destructive "Quit to Levels".

### 5.7 Toast

Bottom-anchored, 48 dp tall, `surface.raised`, `radius.pill`, `body.sm`. Slides up 24 dp and fades. Used for "Can't replace a filled pipe" and similar feedback. Never blocks input.

### 5.8 Settings row

56 dp, label `body.md`, trailing switch. Switch track `radius.pill`: off `surface.overlay`, on `liquid.500` with a `liquid.glow` thumb.

---

## 6. Game board

### 6.1 Board plate

The grid sits on a level 1 panel (`surface.base`, `radius.xl`, 12 dp padding). Grid lines are `line.subtle`, 1 dp. The plate has a subtle radial vignette (`surface.deep` at the edges) so the centre reads brighter.

### 6.2 Cells

| State | Fill | Border | Extra |
|-------|------|--------|-------|
| Empty | `surface.overlay` at 60 % | `line.subtle` | Tiny centred "+" in `text.muted` at 40 % |
| Empty, valid target (finger down) | `liquid.tint` | `line.strong` | Ghost of the top stack piece at 50 % |
| Pipe, dry | transparent | none | Pipe render (6.3) |
| Pipe, filled | transparent | none | Pipe render with liquid |
| Locked (tap on filled/flowing) | flash `leak.tint` 240 ms | – | Toast |
| Blocked | `surface.abyss` | none | Diagonal hatch in `line.subtle`, `radius.sm` |

Cell size is computed from the available width; the minimum is 44 dp so tap targets stay accessible.

### 6.3 Pipe rendering recipe

Every pipe section is drawn from the same layers, in this order. Sizes are relative to cell size *c*. The steel layers and the reservoir tank are SVG assets in `app/src/main/assets/pipes/`, one per section (format in [pipe-assets.md](pipe-assets.md)). The liquid (layer 4) is drawn in code along the channels marked in those files, between the asset's `under` and `over` groups. Its gradient across the bore is drawn as three concentric strokes (`liquid.700` edge, `liquid.500` body, `liquid.300` core), so it looks the same on straights and elbows.

1. **Body** – stroke `steel.500`, width 0.36 c, round joins.
2. **Shadow side** – same path offset 1 dp down-right, stroke `steel.700`, width 0.36 c, drawn before the body (visible as a thin dark edge).
3. **Bore** – stroke `steel.900`, width 0.25 c. This is the hollow.
4. **Liquid** – stroke gradient (2.3), width 0.21 c, clipped by progress (7.3). Glow when this segment is the active one.
5. **Specular** – stroke `steel.100` at 55 %, width 0.04 c, offset 0.12 c toward the top-left, drawn on top of the body but under the bore.
6. **Flanges** – at every open port, a `steel.300` band 0.1 c long across the pipe width plus 0.04 c, with a `steel.700` edge line.
7. **Glass** – the section's glass overlay (`<id>-glass.svg`), translucent white tint, rim lines and a specular streak, drawn above everything else, liquid and flanges included.

### 6.4 The eight sections plus fixed cells

| Section | Path | Notes |
|---------|------|-------|
| `H` | left edge → right edge | – |
| `V` | top edge → bottom edge | – |
| `LT` `LB` `RT` `RB` | edge → centre → edge with round join | The round join gives a natural elbow |
| `X` | vertical drawn first, then horizontal on top | A 0.05 c `surface.deep` gap around the horizontal body creates a "bridge" so it reads as two channels. Each channel has its own liquid layer |
| `RES` | left edge → right edge, plus a centred tank | Always a horizontal section (L↔R, two-way). No top or bottom ports. Tank: circle radius 0.3 c, fill `reservoir.700 → reservoir.500` gradient, rim `steel.300`. Liquid fills the tank bottom-up during progress 0.1–0.9. A small "slow" badge sits at the top-right of the cell |
| Start valve | stub from centre to the exit port | Brass wheel: circle radius 0.24 c, `brass.500`, four spokes, brass glow pulsing on 1.6 s while waiting. An arrow chevron on the stub points in the exit direction |
| Finish drain | stub from the entry port to centre | Ring radius 0.24 c stroke `success.500`, grate of three `steel.300` bars inside, success glow pulsing 2 s. Ring flashes solid on win |

### 6.5 Liquid rendering

- The liquid is a stroke with the vertical gradient from 2.3 and, for the segment currently filling, an outer glow of 16 dp `liquid.glow` at 45 %.
- The **head** (leading edge) has a 0.08 c rounded cap in `liquid.glow`, and two or three 2–3 dp bubbles trail behind it drifting toward the top of the bore.
- Filled segments lose the glow after 400 ms and keep a faint 2 dp `liquid.300` top highlight so a long path still reads as "wet".
- On a **leak** the head bursts at the port: 10–14 droplets in `liquid.500`/`liquid.glow` fly out over 400 ms and a puddle blob (`liquid.700` at 60 %) spreads on the neighbouring cell.

---

## 7. Motion

### 7.1 Tokens

| Token | Duration | Easing | Use |
|-------|----------|--------|-----|
| `motion.instant` | 90 ms | linear | Press feedback, lip compression |
| `motion.quick` | 160 ms | ease-out | Chip states, toast fade, scrim |
| `motion.settle` | 240 ms | spring (stiffness medium, damping 0.75) | Stack slide, cell state change |
| `motion.pop` | 320 ms | spring (stiffness medium-low, damping 0.6) | Tile placement, dialog entry, win dialog star pop |
| `motion.drift` | 1 600 ms | ease-in-out, loop | Valve glow pulse |
| `motion.pulse` | 2 000 ms | ease-in-out, loop | Finish ring pulse, current stack piece glow |
| Liquid fill | continuous | linear | Driven by the simulation, never by an animator |

### 7.2 Specific animations

- **Place tile**: scale 1.15 → 1 with `motion.pop`, 6 % overshoot, a 40 ms haptic tick at the start.
- **Replace tile**: old tile scales down to 0.7 and fades over `motion.quick`, new tile pops in.
- **Locked tap**: cell shakes 3 dp left-right twice over 240 ms and flashes `leak.tint`.
- **Countdown bar fade**: when the liquid starts, the countdown bar fades from 100 % to 0 % opacity over `motion.quick`. Only its alpha changes; its slot stays laid out.
- **Score bar fill**: when the score changes, the fill grows from where it is to the new length over 600 ms ease-out (`FastOutSlowInEasing`). A change while it is still moving continues from the current length. A new attempt empties the fill at once, without animating. The number shows the new score at once.
- **Threshold sparkle**: when the moving fill reaches `stars.one`, `stars.two` or `stars.three` (the end of the bar), a 600 ms procedural burst at that point: 6–8 `liquid.glow` and `brass.300` particles flying out 6–14 dp and fading, over a brief `brass.300` glow flash (radius 16 dp, fading out in the first 300 ms). Each threshold sparkles once per attempt; a jump past several thresholds sparkles each one as the fill passes it. Points beyond `stars.three` do not sparkle.
- **Win**: finish ring flashes, board dims to 60 %, dialog pops in, stars stagger 80 ms each with a small brass particle burst.
- **Leak**: droplet burst, board shakes 2 dp for 200 ms, then the dialog pops in after 500 ms so the player sees where it went wrong.

### 7.3 Liquid progress

Progress is a 0–1 value per segment from the engine. The renderer clips the liquid stroke by path length. For corners the join point is at 0.5. For the reservoir, 0–0.1 is the inlet stub, 0.1–0.9 fills the tank bottom-up, 0.9–1.0 is the outlet stub. The renderer never eases progress; smoothness comes from the engine ticking at frame rate.

### 7.4 Reduced motion

When the system "remove animations" setting is on: no springs (replace with 160 ms ease-out), no glow pulses (static glow at 60 %), no board shake, no droplet burst (a single static splash sprite). In the HUD the countdown bar disappears at once instead of fading, the score bar fill changes over 160 ms ease-out instead of 600 ms, and there is no threshold sparkle. Liquid fill is gameplay, so it is unaffected.

---

## 8. Iconography

Rounded-stroke icons, 24 dp grid, 2 dp stroke, round caps and joins, drawn as `ImageVector`s. Colour follows `text.primary` unless specified.

Required set: play, pause, restart, next, back, close, settings, sound-on, sound-off, music-on, music-off, lock, star (filled + outline), trophy, fast-forward (start flow now), hourglass (slow badge), wave (flow speed), drop (liquid), warning, info, check.

---

## 9. Sound

Kept from the original brief with tone adjusted to the new mood: lower, wetter, less slapstick.

- **Place**: short metallic clunk with a small wet tail.
- **Replace**: the clunk plus a quick wrench ratchet.
- **Locked**: dull thud.
- **Countdown last 3 s**: soft sonar ping per second.
- **Flow**: low continuous gurgle, filtered brighter as speed increases.
- **Reservoir**: deeper glug with a slow bubble loop while filling.
- **Leak**: pressurised spray then a splat; no slide whistle.
- **Win**: rising three-note chime with a bubbly sparkle tail.
- **UI tap**: soft pop.

---

## 10. Accessibility

- All tap targets ≥ 48 dp; grid cells ≥ 44 dp with 4 dp gaps.
- Contrast pairs verified in 2.8. Never place `text.primary` on brass or liquid.
- Liquid (aqua) and leak (coral) are distinguishable by shape and motion as well as colour: leaks always burst outward; flow is always contained in a bore.
- The reservoir is marked by both colour (violet tank) and the hourglass badge.
- The countdown ring is paired with a number; colour changes are never the only signal.
- Stack pieces have content descriptions ("Next: corner, left to top").
- Supports font scaling up to 130 % for body text; display sizes are fixed to protect layout, which is acceptable because they never carry essential information without a body-text or icon counterpart.
- Respects reduced motion (7.4).

---

## 11. Compose implementation map

```kotlin
object LeakOutColors {                      // one instance per theme
    val surfaceAbyss: Color; val surfaceDeep: Color; val surfaceBase: Color
    val surfaceRaised: Color; val surfaceOverlay: Color; val scrim: Color
    val lineSubtle: Color; val lineStrong: Color; val lineHighlight: Color
    val liquidGlow: Color; val liquid300: Color; val liquid500: Color; val liquid700: Color; val liquid900: Color
    val brass100: Color; val brass300: Color; val brass500: Color; val brass700: Color; val brass900: Color
    val leak500: Color; val leak700: Color; val success500: Color; val success700: Color
    val reservoir300: Color; val reservoir500: Color; val reservoir700: Color
    val steel100: Color; val steel300: Color; val steel500: Color; val steel700: Color; val steel900: Color
    val textPrimary: Color; val textSecondary: Color; val textMuted: Color
    val textOnBrass: Color; val textOnLiquid: Color; val textOnStatus: Color
}

object LeakOutType {                        // TextStyle per row of 3.1
    val displayHero: TextStyle; val displayXl: TextStyle; /* … */ val caption: TextStyle
}

object LeakOutShapes { val xs = 6.dp; val sm = 10.dp; val md = 16.dp; val lg = 24.dp; val xl = 32.dp }
object LeakOutSpace  { val s1 = 4.dp; val s2 = 8.dp; /* … */ val s12 = 48.dp }
object LeakOutMotion { val instant = 90; val quick = 160; val settle = 240; val pop = 320 }

@Composable
fun LeakOutTheme(daylight: Boolean = false, content: @Composable () -> Unit)
```

- `LeakOutTheme` provides the token objects through `CompositionLocal`s and wraps `MaterialTheme` only for components that need it (switches, ripples). Colours in `MaterialTheme.colorScheme` are mapped from the tokens so Material components inherit the look.
- The pipe renderer (`render/PipeDrawer.kt`) reads `LeakOutColors` and the cell size; it must not hard-code any colour.
- Fonts: `FontFamily(Font(R.font.wendy_one_regular))` and `FontFamily(Font(R.font.varela_round_regular))`.

---

## 12. What changed from the slim version

| Area | Slim (v1) | Deep Flow (v2) |
|------|-----------|----------------|
| Mood | Light cartoon, white cards | Dark, glowing, layered slate |
| Outlines | 3–4 px ink borders on everything | None; depth via glow, shadow and 1 dp highlights. Only buttons keep a bottom "lip" |
| Display font | Fredoka | **Wendy One** |
| UI font | Nunito | **Varela Round** |
| Primary colour | Hydro blue for both liquid and buttons | Liquid aqua is reserved for liquid; **brass** is the action colour |
| Reservoir | Purple liquid | Violet **tank**, liquid stays aqua |
| Themes | Light only | Deep (default) and Daylight |
| Pipe art | Flat rectangles | Five-layer procedural stroke recipe with bore, specular and flanges |
