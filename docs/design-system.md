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
| `brass.500` | `#F5A623` | Primary button base, start valve wheel, current stack piece frame |
| `brass.700` | `#C97A12` | Primary button lip (bottom edge), pressed state |
| `brass.900` | `#7A4708` | Brass shadow, text on brass.100 |
| `brass.tint` | `#F5A623` at 16 % | Warning chip background, brass focus ring |

### 2.5 Status

| Token | Hex | Usage |
|-------|-----|-------|
| `leak.500` | `#FF5A5F` | Leak splash, lose dialog title, destructive button |
| `leak.700` | `#C9353B` | Destructive button lip, leak shadow |
| `leak.tint` | `#FF5A5F` at 16 % | Locked-cell flash, error chip background |
| `success.500` | `#3DDC97` | Finish ring and checker, win dialog title, complete tick |
| `success.700` | `#1FA971` | Success button lip |
| `success.tint` | `#3DDC97` at 16 % | Completed level tile wash |
| `reservoir.300` | `#C7B5FF` | Reservoir liquid highlight |
| `reservoir.500` | `#A78BFA` | Reservoir tank tint, "slow" badge |
| `reservoir.700` | `#7C5CE6` | Reservoir tank shadow |
| `portal.1` | `#FF6FD8` | Ring of portal pair 1, symbol: circle |
| `portal.2` | `#B8F25C` | Ring of portal pair 2, symbol: triangle |
| `portal.3` | `#FF8A3D` | Ring of portal pair 3, symbol: square |

The reservoir never recolours the liquid. Liquid stays aqua; the *tank* is violet so the player learns "violet = slow" without the flow ever changing colour.

The portal colours tell the pairs of a level apart: both portals of a pair share their pair's colour, and pair *n* (the *n*-th entry of the level's `portals`) always takes `portal.n`. Each pair also has its own symbol, so colour is never the only cue. The three hues stay clear of the liquid aqua, the success green, the leak red and the reservoir violet. Like the rest of the board they are only drawn in the Deep palette.

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
| `portal.1` on `surface.deep` | 7.1:1 |
| `portal.2` on `surface.deep` | 13.3:1 |
| `portal.3` on `surface.deep` | 7.5:1 |
| `text.primary` (portal symbol) on `surface.deep` | 16.3:1 |

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
| 3 | Floating | shadow `0 24 64` black 60 % · scrim behind | Dialogs, pause dialog |
| G | Glow | `0 0 24` in the element's colour at 45 % | Flowing liquid, primary button, current stack piece, finish ring |

Glows are additive to a level, never a replacement. A raised primary button is level 2 + G in brass.

### 4.4 The tactile lip

Buttons keep a hint of the old "juicy" press physics without outlines. A **lip** is a 4 dp solid band on the bottom edge in the darker shade of the button colour (`brass.700`, `leak.700`, `success.700`, or `surface.abyss` for glass). On press the button translates down 2 dp and the lip shrinks to 2 dp. The lip is the only place a hard edge appears in the UI.

---

## 5. Components

### 5.1 Buttons

| Variant | Fill | Label | Lip | Glow | Use |
|---------|------|-------|-----|------|-----|
| Primary | gradient `brass.300 → brass.500` | `text.onBrass`, `display.sm` | `brass.700` | brass | One per screen: Play, Restart on the leak dialog |
| Secondary (glass) | `surface.overlay`, 1 dp `line.strong` | `text.primary` | `surface.abyss` | none | Level Select, Settings, Restart on the win dialog, Restart and Top 10 on the pause dialog |
| Tertiary | none | `liquid.300` | none | none | Skip, Not now, Top 10 and Exit on the win and leak dialogs |
| Destructive | gradient `leak.500 → leak.700` | `text.onStatus` | `leak.700` | none | Give up, Reset progress |
| Success | gradient `success.500 → success.700` | `text.onStatus` | `success.700` | success | Next Level on the win dialog (replaces primary there) |

- Height 56 dp (large), 48 dp (medium), 40 dp (small): the face. The 4 dp lip comes below it, so a large button takes 60 dp and a medium one 52 dp. Radius `radius.md`. Horizontal padding 24 dp.
- Tertiary: a text button with no lip, 48 dp high (the minimum touch target) whatever the size.
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

A vertical level 1 panel (`surface.deep` at 90 %, 24 dp radius, `line.strong` edge, 8 dp padding) along the left edge of the game screen, in landscape, next to the board. Its width is the next slot plus 16 dp.

- The stack holds `visibleSize + 10` pieces, ordered top to bottom. The **bottom piece** is the next one: 1.25 × the preview size, framed in a 2 dp `brass.500` ring on `brass.500` at 28 %. It is the only piece that can be dragged. The preview pieces above it sit on `surface.raised` at 8 % with a 1 dp `line.strong` edge.
- Slot sizes are chosen so that the bottom `visibleSize` slots fit the rail height (preview 20–40 dp, gap 2–6 dp); those slots are always fully visible, even with `visibleSize` 10 on a 360 dp tall screen. The ten extra pieces continue upwards at the preview size as far as the rail has room.
- A piece that does not fit is **clipped at the top edge** of the rail: a partly fitting piece shows only its lower part, a piece above the edge is not drawn. The rail never scrolls. A taller screen shows more look-ahead at the same slot size.
- When the board accepts a placement, the bottom slot empties and every piece **falls one slot down under gravity** with `motion.fall`: it accelerates, lands with one small bounce and rests. The pieces start from the bottom up, 24 ms apart, so the column collapses like a pile. The piece that becomes the next one grows to the next size and takes on the brass frame while it falls. A new piece falls in from above the top edge into the top slot. A placement during a fall makes every piece continue from where it is, never jumping back.
- The next piece can be dragged while it is still falling; the drag empties its slot at once.
- Only fully visible pieces are exposed to accessibility services; clipped pieces are hidden.

### 5.5 Level tile (level select)

88 × 104 dp, level 2 raised, `radius.md`.

- Number in `display.md` centred; three 14 dp stars below.
- **Locked**: `surface.base`, number in `text.muted`, small lock icon, no stars.
- **Unlocked**: `surface.raised`, number `text.primary`, empty star outlines.
- **Completed**: `success.tint` wash, number `success.500`, earned stars in `brass.300`.
- **Current** (highest unlocked): additional brass glow and a 2 dp `brass.500` ring.

### 5.6 Dialogs and sheets

Level 3 floating panel, `radius.lg`, `surface.base`, 1 dp `line.highlight` top edge, no outline, 24 dp padding, max width 360 dp, centred over `surface.scrim` with a 16 dp gutter to the window edge. Entry: scale 0.92 → 1 with `motion.pop`; scrim fades in over `motion.quick` (see 7.2 and 7.4).

- **Win** and **Leak**: the result dialogs, specified below.
- **Pause**: the panel sits on an **opaque backdrop** instead of the scrim: a diagonal linear gradient from `surface.abyss` (top left) to `surface.deep` (bottom right) covering the whole screen, system bar and cutout areas included, so nothing of the frozen board, stack or HUD can be studied while paused. The backdrop fades in over `motion.quick` and disappears at once when the dialog closes; a tap on it does nothing. Title "Paused" in Title Case, `display.lg`, `text.primary`. Medium buttons (48 dp face + 4 dp lip) 12 dp apart, so the panel fits a 360 dp high screen: brass Primary "Resume" (the one main action), a row of two equal glass Secondary buttons "Restart" and "Top 10" 12 dp apart, a 1 dp `line.subtle` separator, Destructive "Exit" (back to the main menu). Back works as Resume. "Top 10" shares Restart's row so the panel keeps its height (48 padding + 31 title + 16 + 52 + 12 + 52 + 12 + 1 + 12 + 52 ≈ 288 dp, within the 328 dp of a 360 dp screen; a third stacked button would need ≈ 352 dp). When the top 10 is not available (a test play from the level designer, no score server in the build), Restart fills the row alone.
- **Top 10**: the global top 10 of the level, opened from the pause, win and leak dialogs; specified below.
- **Play menu**: the choice shown after Play on the welcome screen, over the see-through `surface.scrim` (drawn by the dialog, platform dim off). Title "Play" in Title Case, `display.lg`, `text.primary`. Medium buttons (48 dp face + 4 dp lip), full panel width, 12 dp apart: brass Primary "Continue" (the one main action) and glass Secondary "Select Level". A tap on the scrim outside the panel or Back closes it without navigating; a tap inside the panel does nothing.

#### Result dialogs: Win and Leak

The two dialogs shown when an attempt ends. Both sit over the see-through `surface.scrim` (`#07111C` at 72 %, drawn by the dialog, platform dim off). The see-through scrim is on purpose: during the win tally the unused sections are removed from the field behind the dialog, and the leak dialog relies on the player seeing where it leaked. Once the attempt has ended there is nothing to hide.

**Win** ("Level Complete!")

| Element | Style | Notes |
|---------|-------|-------|
| Eyebrow | `display.xs` `success.500` | "LEVEL 7": the level number |
| Headline | `display.xl` `text.primary` | "Level Complete!" |
| Message | `body.md` `text.secondary` | "You kept the water flowing." When the win beat the stored best, "New best!" in `display.sm` `brass.300` takes its place once the tally is done, so the panel does not grow |
| Stars | 40 dp, 12 dp apart | Earned in `brass.300` with a pop-in stagger of 80 ms and a small brass burst, missed in `line.strong` |
| Score row | three equal columns | See below |
| Buttons | stacked | Success "Next Level", glass "Restart", then one row of two equal tertiary buttons "Top 10" and "Exit" (Exit alone, centred, when the top 10 is not available) |

The **score row** follows the game's scoring (the points earned during play, minus 3 per unused section, never below 0). Three equal columns, each a `label.md` caption in `text.secondary` above its value:

| Column | Caption | Value | Screen reader |
|--------|---------|-------|---------------|
| Points | "POINTS" | `numeric.lg` `text.primary`: "120" | "Points 120" |
| Penalty | "PENALTY" | `numeric.lg` `leak.500`: "−12"; "0" in `text.muted` while there is no penalty | "Penalty minus 12" |
| Score | "SCORE" | `numeric.lg` `brass.300`: "108", with a brass `G` glow once the tally is done | "Score 108" |

Values use the device's digit grouping. Each column is one screen-reader item. Points shows at once, Penalty counts up while the unused sections are removed, then Score counts up, then the stars pop. The tally is gameplay feedback, so reduced motion does not change it.

**Leak** ("Leaked!")

| Element | Style | Notes |
|---------|-------|-------|
| Eyebrow | `display.xs` `leak.500` | "LEVEL 7" |
| Splash | 72 dp | A `leak.500` centre blob with 6 droplets flying out once (7.2); a still sprite with reduced motion (7.4) |
| Headline | `display.xl` `text.primary` | "Leaked!" |
| Message | `body.md` `text.secondary` | "The pipe is leaking. Level lost." |
| Score | one centred column | "SCORE" caption in `label.md` `text.secondary`, value in `numeric.lg` `brass.300` |
| Buttons | stacked | Primary "Restart", then one row of two equal tertiary buttons "Top 10" and "Exit" (Exit alone, centred, when the top 10 is not available) |

"Restart" and "Exit" are named after what they do in the game (Exit goes to the main menu). Exit is tertiary, not destructive: leaving after a finished attempt loses nothing.

#### Result dialog layouts: Regular and Compact

The game is landscape only and runs down to 640 × 360 dp, where one tall column does not fit. Each result dialog has two layouts and picks one by measuring instead of by a fixed breakpoint, so the choice stays right under font scaling and on tablets:

1. Measure the Regular content at its maximum width (360 dp) with its final values (the final score, the full penalty, "New best!" when it will show), once per window size and font scale.
2. If that height fits in the window height minus 2 × 16 dp gutter, use **Regular**; otherwise use **Compact**. The choice holds for the dialog's lifetime, so the tally never flips the layout.
3. In Compact, if the information column is still taller than the space it has (for example 130 % font on a 360 dp screen), that column scrolls vertically and the buttons column stays fully visible.

**Regular**: one centred column, max 360 dp wide, 24 dp padding, the elements in the order of the tables above, large buttons (56 dp face + 4 dp lip) stacked full width with 12 dp spacing, the tertiary Top 10 and Exit two 48 dp text buttons side by side in the last row. Sharing that row keeps every height in the table below unchanged.

```
            Win                                Leak
┌────────────────────────────┐    ┌────────────────────────────┐
│          LEVEL 7           │    │          LEVEL 7           │
│      Level Complete!       │    │          (splash)          │
│ You kept the water flowing.│    │          Leaked!           │
│         ★   ★   ☆          │    │   The pipe is leaking.     │
│  POINTS  PENALTY  SCORE    │    │        Level lost.         │
│   120      −12     108     │    │           SCORE            │
│ ┌────────────────────────┐ │    │             85             │
│ │       Next Level       │ │    │ ┌────────────────────────┐ │
│ └────────────────────────┘ │    │ │        Restart         │ │
│ ┌────────────────────────┐ │    │ └────────────────────────┘ │
│ │        Restart         │ │    │    Top 10        Exit      │
│ └────────────────────────┘ │    └────────────────────────────┘
│    Top 10        Exit      │
└────────────────────────────┘
```

**Compact**: max 600 dp wide, 24 dp padding, two columns.

- Left, the information (weight 1, content centred, scrolls if needed): the eyebrow; the headline in `display.lg` instead of `display.xl`. Win: "New best!" in the message's place or nothing (the message line is dropped), stars 32 dp. Leak: the splash at 48 dp beside the headline, the message in `body.sm`. Then the score row.
- A 1 dp `line.subtle` vertical divider, 24 dp from each column.
- Right, the buttons (fixed 200 dp wide, vertically centred): the same buttons stacked, medium (48 dp face + 4 dp lip) with 12 dp spacing, Top 10 and Exit two 48 dp text buttons of 100 dp each in the last row.

```
                               Win
┌──────────────────────────────────┬─────────────────────┐
│             LEVEL 7              │  ┌───────────────┐  │
│         Level Complete!          │  │  Next Level   │  │
│            ★  ★  ☆               │  └───────────────┘  │
│  POINTS     PENALTY     SCORE    │  ┌───────────────┐  │
│   120         −12        108     │  │    Restart    │  │
│                                  │  └───────────────┘  │
│                                  │   Top 10    Exit    │
└──────────────────────────────────┴─────────────────────┘
                               Leak
┌──────────────────────────────────┬─────────────────────┐
│             LEVEL 7              │  ┌───────────────┐  │
│        (splash) Leaked!          │  │    Restart    │  │
│ The pipe is leaking. Level lost. │  └───────────────┘  │
│              SCORE               │   Top 10    Exit    │
│               85                 │                     │
└──────────────────────────────────┴─────────────────────┘
```

Heights at 100 % font (a text box is its size × line height; a button is face + lip):

| Layout | Sum (dp) | Height |
|--------|----------|--------|
| Regular win | 48 padding + 17 eyebrow + 8 + 42 headline + 8 + 24 message + 16 + 40 stars + 16 + 53 score row (17 caption + 4 + 32 value) + 24 + 60 + 12 + 60 + 12 + 48 buttons | ≈ 488 dp |
| Regular leak | 48 + 17 + 8 + 72 splash + 12 + 42 + 8 + 24 + 16 + 53 + 24 + 60 + 12 + 48 | ≈ 444 dp |
| Compact win, information | 48 + 17 + 4 + 31 headline + 8 + 22 "New best!" (or the 20 dp message in its place) + 8 + 32 stars + 12 + 53 | ≈ 235 dp |
| Compact leak, information | 48 + 17 + 4 + 48 splash beside the headline + 8 + 20 message + 12 + 53 | ≈ 210 dp |
| Compact win, buttons | 48 + 52 + 12 + 52 + 12 + 48 | ≈ 224 dp |
| Compact leak, buttons | 48 + 52 + 12 + 48 | ≈ 160 dp |

On a 640 × 360 dp screen there is 360 − 32 = 328 dp: both Regular layouts are too tall, so phones in landscape get Compact, and every Compact column fits within 328 dp. On a 1280 × 800 dp tablet (768 dp available) both dialogs use Regular.

#### Top 10 dialog

The global top 10 of the level, from the Leak Out score server. It opens from the pause dialog during play and from the win and leak dialogs after an attempt.

**Backdrop.** The opaque pause backdrop (the `surface.abyss` → `surface.deep` diagonal gradient over the whole screen), so neither the game nor the dialog it came from shows through. Opened from the pause dialog, the backdrop is already there and stays put; opened from a result dialog, it fades in over `motion.quick`. A tap on it does nothing. Closing returns to the dialog it came from at once.

**Panel.** Level 3 floating panel, `radius.lg`, `surface.base`, 1 dp `line.highlight` top edge, no outline, 24 dp padding.

| Element | Style | Notes |
|---------|-------|-------|
| Eyebrow | `display.xs` `liquid.300` | "LEVEL 7" |
| Title | `display.lg` `text.primary`, 24 dp `trophy` icon in `brass.300` before it, 8 dp apart | "Top 10" |
| Close | icon button (5.1): 48 dp glass circle, 24 dp `close` icon | Top right, aligned with the title block. Screen reader "Close" |
| List | rows, 12 dp below the header | See below; the header stays fixed when the list scrolls |

**Rows.** 40 dp high, 12 dp horizontal padding, separated by 1 dp `line.subtle` lines.

| Column | Style | Notes |
|--------|-------|-------|
| Rank | 32 dp wide, `numeric.md`, left aligned | Ranks 1–3 in `brass.300`, 4–10 in `text.secondary` |
| Name | `body.md` `text.primary`, one line, ellipsized | The display name from the server. The player's own row adds "YOU" in `display.xs` `liquid.300`, 8 dp after the name, which never ellipsizes |
| Score | `numeric.md` `text.primary`, right aligned, fixed-width digits | Uses the device's digit grouping ("1,450") |

The player's own row has a `liquid.tint` fill, a 1 dp `liquid.500` border and `radius.sm`, and no separator lines touching it. Each row is one screen-reader item: "Rank 4, Ada, 1,450 points", with ", your score" added on the own row.

**States.** Loading, empty, signed out and error replace the rows in the same space, so the panel does not change size when the scores arrive. Each is a centred column: a 40 dp icon or indicator, 12 dp, a message in `body.md` `text.secondary` (centred, max 280 dp wide, up to two lines), and for some a medium glass button 16 dp below.

| State | Indicator | Message | Button |
|-------|-----------|---------|--------|
| Loading | 32 dp indeterminate ring, 3 dp stroke, `liquid.500` on a `line.subtle` track | "Loading scores…" | none |
| Empty | `trophy` in `text.muted` | "No scores yet. Win this level to be the first on the board." | none |
| Signed out | `trophy` in `text.muted` | "Sign in to Google Play Games to see the top 10." | glass "Sign In"; left out when Play Games is not available, the message stays |
| Error | `warning` in `leak.500` | "Couldn't load the scores. Check your connection and try again." | glass "Try Again" |

A level with fewer than 10 scores shows only those rows, top aligned; the list space keeps its size.

**Layouts.** Like the result dialogs, the top 10 picks a layout by measuring the Regular panel against the window height minus 2 × 16 dp, once per window size and font scale.

- **Regular**: max 360 dp wide, one column of 10 rows.
- **Compact**: max 600 dp wide, two columns of 5 rows (ranks 1–5 left, 6–10 right), 24 dp apart with a 1 dp `line.subtle` vertical divider between them. The screen reader still reads 1 to 10 in order. When the rows still do not fit (a large font scale), both columns scroll together under the fixed header.

The own row is marked `▐ ▌` in the sketches.

```
              Regular
┌──────────────────────────────────┐
│ LEVEL 7                      (×) │
│ (trophy) Top 10                  │
│  1  Ada                    2,140 │
│  2  Pipewizard             1,980 │
│  3  Sam                    1,450 │
│▐ 4  Jamie  YOU             1,300▌│
│  5  Fleur                  1,210 │
│  6  Mo                     1,120 │
│  7  Linde                    990 │
│  8  Kees                     870 │
│  9  Joy                      640 │
│ 10  Ravi                     510 │
└──────────────────────────────────┘

                          Compact
┌─────────────────────────────────────────────────────────┐
│ LEVEL 7                                             (×) │
│ (trophy) Top 10                                         │
│  1  Ada              2,140  │   6  Mo               1,120 │
│  2  Pipewizard       1,980  │   7  Linde              990 │
│  3  Sam              1,450  │   8  Kees               870 │
│▐ 4  Jamie  YOU       1,300▌ │   9  Joy                640 │
│  5  Fleur            1,210  │  10  Ravi               510 │
└─────────────────────────────────────────────────────────┘
```

| Layout | Sum (dp) | Height |
|--------|----------|--------|
| Regular | 48 padding + 52 header (17 eyebrow + 4 + 31 title) + 12 + 10 × 40 rows | ≈ 512 dp |
| Compact | 48 + 52 + 12 + 5 × 40 rows | ≈ 312 dp |
| A state, in either layout | 40 indicator + 12 + 48 message + 16 + 52 button | ≈ 168 dp, inside the 200 dp (Compact) or 400 dp (Regular) list space |

On a 640 × 360 dp screen (328 dp available) the top 10 uses Compact and all ten rows are visible without scrolling; on a 1280 × 800 dp tablet it uses Regular.

### 5.7 Toast

Bottom-anchored, 48 dp tall, `surface.raised`, `radius.pill`, `body.sm`. Slides up 24 dp and fades. Used for "Can't replace a filled pipe" and similar feedback. Never blocks input.

### 5.8 Settings row

56 dp, label `body.md`, trailing switch. Switch track `radius.pill`: off `surface.overlay`, on `liquid.500` with a `liquid.glow` thumb.

### 5.9 Tip overlay

The one-time guided tips on the game screen (spec `game-play-onboarding`): the intro tour on the first level played, and single tips for the cross, the reservoir, mandatory sections, portals and leaky tiles. Unlike a dialog (5.6), a tip points at the real element it explains, so it is drawn over the whole game screen instead of centred.

**Layers** (bottom to top, all inside the game screen, above the HUD, stack and field, below the pause dialog):

1. **Scrim** – `surface.scrim` (`#07111C` at 72 %) over the whole screen, with a **spotlight** cut out around every target. The overlay consumes all pointer input outside its buttons, so nothing can be dragged, pinched or panned underneath, and taps on the scrim do nothing.
2. **Spotlight** – a cut-out per target: the target's on-screen bounds grown by `space.2` (8 dp) on every side, corners `radius.md` (16 dp). A stack slot, a field cell and the whole stack column are each one rectangle; the mandatory tip has one cut-out per mandatory cell, and the leaky tile tip one per leaky tile. Inside the cut-out the game is seen undimmed.
3. **Glow ring** – around each cut-out a 2 dp `liquid.glow` stroke with a `G` glow in `liquid.glow` at 45 % (blur 24 dp). It pulses between 45 % and 70 % on `motion.pulse`.
4. **Callout card** – a level 3 floating panel (shadow `0 24 64` black 60 %), `surface.base`, `radius.lg`, 1 dp `line.highlight` top edge, 16 dp (`space.4`) padding, max width 296 dp, so it fits beside the stack and a centre cell on a 640 × 360 dp screen.
5. **Pointer** – a 16 × 8 dp triangle in `surface.base` on the card edge facing the target, centred on the target's centre and kept at least `radius.lg` from the card's corners. No pointer when no side has room (see Placement, last rule).

**Callout content**, top to bottom:

| Element | Style | Notes |
|---------|-------|-------|
| Step counter | `display.xs` `liquid.300` | "1 / 6"; intro tour only. Single tips show an eyebrow instead: "TIP" in `display.xs` `brass.300` |
| Title | `display.md` `text.primary` | At most about four words (3.2) |
| Text | `body.md` `text.secondary` | Two or three lines at 296 dp; `body.md` rather than `body.lg` so the card fits a 360 dp tall screen at 130 % font |
| Buttons | stacked, 16 dp below the text, right-aligned, `space.3` (12 dp) apart | See below |

| Tip | Buttons (top → bottom) |
|-----|------------------------|
| Intro steps 1–5 | primary small "Next" at full card width, tertiary "Skip tour" at full card width below it |
| Intro step 6 | primary small "Let's go" |
| Cross, reservoir, mandatory, portal, leaky tile | primary small "Got it" |

The bright brass "Next" sits on its own line so it is never squeezed by "Skip tour" and clearly reads as the way to the next tip. Primary small is 40 dp face + 4 dp lip = 48 dp and the tertiary is 48 dp. Card height at 100 % font on intro steps 1–5: 32 padding + 17 counter + 4 + 25 title + 8 + 72 text (three lines) + 16 + 48 + 12 + 48 buttons ≈ 282 dp, which fits in 360 − 2 × 16 dp.

**Placement** (pure, JVM-tested): given the window size, the 16 dp screen gutter and the spotlight rect,

1. Measure the free space to the right of, left of, below and above the spotlight, minus a 12 dp (`space.3`) gap to the spotlight.
2. Take the side with the most room that fits the card; ties go right, left, below, above in that order.
3. Along that side, centre the card on the target's centre, then shift it so it stays inside the gutter.
4. If no side fits the card, put it in the screen half away from the target's centre, inside the gutter, without a pointer.

The stack column sits on the left of the screen, so stack tips open to the right; a bottom-right cell opens to the left. On a 640 × 360 dp screen the field sits right of the stack, so a cell in the centre of the field (about x 342–392 dp) has room on its left only and opens to the left; on larger screens it opens to the right.

**Drag hint** (intro step 4): a ghost of the next section at 50 % opacity, the size of a field cell, travels from the centre of the next stack slot to the centre of the hint cell (the nearest empty, non-blocked cell to the start's exit). While the ghost is on the cell, the cell shows the "valid target" look of 6.2 (`liquid.tint` fill, `line.strong` border). Both the next slot and the hint cell are spotlighted.

**Tap hint** (intro step 5): the pro tip that a tap places the next section too. Only the hint cell of the drag hint is spotlighted, so the spotlight visibly moves from the stack to the field, and the cell keeps its "valid target" look. On the cell's centre a 5 dp `liquid.glow` dot marks the touch point, and a 2 dp `liquid.glow` ring grows from a quarter of its size to a fingertip's width (22 dp radius) while it fades from 90 % to 0, once per `motion.pulse`. With reduced motion the ring stands still at full size and alpha. No drag ghost on this step.

**Off-screen targets**: when a field target (the finish on a large level, a mandatory cell) is outside the visible field, the field pans just far enough to show it, within the existing pan limits, before the spotlight moves there. When the tour ends, the field returns to the view the level starts with.

**Texts** (English strings resource; the reservoir percentage is the level's `flow.reservoirSpeedFactor` × 100, rounded half up, shown here for 0.3):

| Tip | Target | Title | Text |
|-----|--------|-------|------|
| Intro 1 / 6 | start tile | The Start | The liquid comes out of this valve when the countdown runs out. |
| Intro 2 / 6 | finish tile | The Finish | Lead the liquid into this drain to complete the level. |
| Intro 3 / 6 | stack column | Your Stack | These are the sections you get, in order. The next one to place is at the bottom. |
| Intro 4 / 6 | next slot + hint cell | Placing Sections | Drag the next section from the stack onto an empty cell. Build a pipe from the start to the finish. |
| Intro 5 / 6 | hint cell | Pro Tip: Tap to Place | You can also tap an empty cell to place the next section there. It is much faster than dragging. |
| Intro 6 / 6 | the field | Zoom and Pan | Pinch to zoom in or out, and drag the field to move around a larger level. |
| Cross | the `X` slot in the stack | Cross Section | A cross has two separate channels and the liquid goes straight through. The first pass scores 10 points, a second pass through the other channel 50. Each channel can be used only once. |
| Reservoir | the `RES` slot in the stack | Reservoir | A reservoir only connects left and right. While it fills, the liquid flows at 30 % speed, which buys you time. It scores 25 points. |
| Mandatory | every mandatory cell | Mandatory Sections | These sections are already placed and can't be removed or replaced. The liquid must pass through every channel of each of them before it reaches the finish, or the level is lost. |
| Leaky tile | every leaky tile | Leaky Tiles | You can place any section on a leaky tile, but the liquid that passes it drains away. Each time the liquid flows through a section on a leaky tile it scores nothing and costs you 20 points, so a detour can be worth more. |

The leaky tile tip uses the single-tip layout as it is; it comes after the portal tip and before the cross tip, and like the mandatory tip it pans the first leaky tile into view on a large level.

**Screen reader**: when a tip appears it is announced as a polite live region: the title, the text, the target ("Highlighted: start valve") and, in the tour, "Step 1 of 6". The card is one pane (`paneTitle` = the title) and its buttons follow the text in reading order. The scrim and the game under it are hidden from accessibility while the tip is shown.

### 5.10 Startup version label

When the app starts, the welcome screen shows the installed version as "v<versionName> (<versionCode>)" (for example "v1.4.0 (57)") in its lower-right corner, so testers can see the build without opening Settings > About. It sits inside the safe area with the same 16 dp horizontal and 8 dp vertical margin as the Play / Settings / Exit row, in `caption` and `text.secondary` on the art, with no background. It is decorative: it takes no touches and does not move the title or the buttons. It is shown once per app process: returning to the welcome screen from another screen does not show it again, also when the player left before it faded. Timing is in 7.2.

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
| Mandatory section | `leak.tint` | 1 dp `leak.500` | Pre-placed by the level, never removable. The usual pipe render (6.3) on top; the tint stays while and after the liquid runs through it |
| Locked (tap on filled/flowing) | flash `leak.tint` 240 ms | – | Toast |
| Blocked | `surface.abyss` | none | Diagonal hatch in `line.subtle`, `radius.sm` |
| Start, Finish | as Empty, without the "+" | as Empty | Start valve or checkered finish (6.4) drawn on the plain empty-cell tile. No colour wash: the colour lives only in the valve wheel and the finish ring |
| Portal | as Empty, without the "+" | as Empty | Portal stub and ring (6.4) drawn on the plain empty-cell tile. Placed by the level, never removable. No colour wash: the pair colour lives only in the ring |
| Leaky tile | as Empty | as Empty | Cracks and a damp stain under any section, and the drip badge (6.4) on top. Cracks: three short polylines in `line.strong` at 70 %, width 0.025 c, from the top-left corner, the bottom-right corner and the bottom-left edge towards the centre. Stain: two overlapping ovals low in the cell in `liquid.700` at 25 % (damp), 45 % once the liquid has passed (wet, 6.5). A section placed on it gets the usual pipe render (6.3) between the stain and the badge, so the cracks show around the pipe |

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
| Start valve | stub from centre to the exit port | See *Start valve and checkered finish* below. Brass wheel radius 0.24 c with four spokes and a hub, a brass chevron on the stub pointing out of the exit port, brass glow pulsing on `motion.drift` while waiting |
| Finish drain ("Checkered finish") | stub from the entry port to centre | See *Start valve and checkered finish* below. Ring radius 0.24 c stroke `success.500` filled with a 6 × 6 checker, success glow pulsing on `motion.pulse` until the win. Ring flashes solid on win |
| Drip badge (leaky tile) | none | A `surface.deep` disc, radius 0.12 c, centred at (0.82 c, 0.18 c) from the top-left corner, the place of the reservoir's "slow" badge, holding a drop in `leak.500` (round bottom radius 0.075 c, tip 0.15 c above it). Drawn above the pipe art so it stays visible on a placed section |
| Portal | stub from the open side to centre | See *Portal* below. Ring radius 0.24 c stroke in the pair colour (`portal.1` to `portal.3`) on a `surface.deep` disc, with the pair symbol in `text.primary` and a swirl turning on `motion.drift` |

#### Start valve and checkered finish

Both endpoints are drawn on the empty-cell tile (6.2) in this order: glow (7.2), stub, symbol. Sizes are in cell units *c* relative to the cell centre; *y* grows downwards.

**Stub.** The stub is the real pipe art of the straight section, not a separate drawing: the `H` asset for an open left or right side, the `V` asset for an open top or bottom side, clipped to the half of the cell between the centre and the open side. Body, shadow, bore, specular, flange and glass (6.3 layers 1–7) are therefore identical to a placed straight, and a section placed against the open side joins the stub exactly like two sections join. The symbol covers the cut end at the centre (radius 0.24 c is larger than half the 0.36 c body). The start's stub carries liquid (6.5); the finish's stub stays dry.

**Start valve wheel** (brass only, no success or leak colour):

| Part | Geometry | Colour |
|------|----------|--------|
| Wheel disc | circle, radius 0.24 c | radial gradient `brass.300` (centre) → `brass.500` (edge) |
| Rim | stroke 0.03 c on the disc edge | `brass.700` |
| Spokes | a cross of two strokes (four spokes), each from −0.20 c to +0.20 c through the centre, width 0.045 c, round caps | `brass.900` |
| Hub | circle radius 0.08 c, edge stroke 0.02 c | `brass.700`, edge `brass.900` |
| Highlight | circle radius 0.05 c at (−0.08 c, −0.10 c) | `brass.100` at 45 % |
| Chevron | round-capped stroke 0.04 c, two legs forming a "›" 0.16 c wide across the stub and 0.08 c deep, tip 0.32 c from the centre towards the exit port (between the wheel and the flange) | `brass.300` |

**Finish ring and checker** (success and checker only, no brass, no flag or pole):

| Part | Geometry | Colour |
|------|----------|--------|
| Checker | the inner disc (radius 0.215 c) filled with a flat 6 × 6 grid of squares, each 0.43 c / 6 ≈ 0.072 c, spanning the inner diameter and aligned on the cell centre (the centre is a corner shared by four squares); clipped to the disc | the grid's top-left square `text.primary` (Deep `#F2F7FB`, in both themes), alternating with `steel.900` |
| Ring | stroke 0.05 c centred on radius 0.24 c (inner radius 0.215 c), drawn over the checker so it covers the clipped edge squares | `success.500` |

At the 44 dp minimum cell a square is about 3.2 dp; the 6 × 6 checker was checked at 44 dp in the gallery and reads, so no 4 × 4 fallback is used. The same geometry is the `finish-checkered` icon (8). Where the finish is named to people (the gallery, onboarding) it is the **checkered finish**; in code and specs it remains the finish drain or `Cell.Finish`.

#### Portal

A portal is drawn like an endpoint, on the empty-cell tile in this order: stub, ring, swirl, symbol. The stub is the straight's art clipped to the half between the centre and the open side, exactly as for the start and finish, so a section against the open side joins it.

| Part | Geometry | Colour |
|------|----------|--------|
| Disc | circle, radius 0.24 c | `surface.deep` |
| Ring | stroke 0.05 c centred on radius 0.24 c, the size of the finish ring | the pair colour: `portal.1`, `portal.2` or `portal.3` |
| Swirl | two arcs on the ring, 70° each and 180° apart, stroke 0.025 c, round caps, turned one full turn per `motion.drift` (7.2) | `text.primary` at 55 % |
| Symbol | pair 1: a filled circle, radius 0.075 c; pair 2: a filled triangle pointing up, circumradius 0.1 c; pair 3: a filled square, side 0.13 c; all centred | `text.primary` (Deep `#F2F7FB`) |

Both portals of a pair look the same, and every pair of a level differs from the others in both colour and symbol. The liquid passes a pair in the time of one cell (6.5, 7.3): the entry portal's stub fills from its open side to the ring during the first half, the exit portal's stub from the ring to its open side during the second half, and both stay full afterwards.

### 6.5 Liquid rendering

- The liquid is a stroke with the vertical gradient from 2.3 and, for the segment currently filling, an outer glow of 16 dp `liquid.glow` at 45 %.
- The **head** (leading edge) has a 0.08 c rounded cap in `liquid.glow`, and two or three 2–3 dp bubbles trail behind it drifting toward the top of the bore.
- Filled segments lose the glow after 400 ms and keep a faint 2 dp `liquid.300` top highlight so a long path still reads as "wet".
- On a **leaky tile** (6.2), while the head is in the section on it, three drops in `liquid.500` fall from the bore (0.56 c down) to the cell's bottom edge, each over 600 ms and 200 ms apart, fading to 40 % as they fall, at x 0.40 c, 0.56 c and 0.47 c. They loop on the game clock, so they freeze on pause, and stop as soon as the head leaves; from then on the stain is the darker wet stain for the rest of the attempt. A restart shows the tile damp again.
- On a **leak** the head bursts at the port: 10–14 droplets in `liquid.500`/`liquid.glow` fly out over 400 ms and a puddle blob (`liquid.700` at 60 %) spreads on the neighbouring cell.

---

## 7. Motion

### 7.1 Tokens

| Token | Duration | Easing | Use |
|-------|----------|--------|-----|
| `motion.instant` | 90 ms | linear | Press feedback, lip compression |
| `motion.quick` | 160 ms | ease-out | Chip states, toast fade, scrim |
| `motion.settle` | 240 ms | spring (stiffness medium, damping 0.75) | Cell state change |
| `motion.pop` | 320 ms | spring (stiffness medium-low, damping 0.6) | Tile placement, dialog entry, win dialog star pop |
| `motion.fall` | 120–260 ms + 120 ms | quadratic ease-in fall, then one bounce of at most 4 dp; 24 ms stagger per piece from the bottom up | Stack pieces dropping a slot |
| `motion.drift` | 1 600 ms | ease-in-out, loop | Valve glow pulse, portal swirl |
| `motion.pulse` | 2 000 ms | ease-in-out, loop | Finish ring pulse, current stack piece glow |
| Liquid fill | continuous | linear | Driven by the simulation, never by an animator |

### 7.2 Specific animations

- **Place tile**: scale 1.15 → 1 with `motion.pop`, 6 % overshoot, a 40 ms haptic tick at the start.
- **Stack drop** (5.4): `motion.fall`. A piece falling a distance `d` follows `d · (t / T)²` with `T = sqrt(d / 8 dp) · 90 ms`, clamped to 120–260 ms (one slot takes about 200 ms), then rebounds `min(0.12 · d, 4 dp)` up and back over 120 ms (a half sine) and rests. Each piece above the bottom one starts 24 ms later, at most 8 steps, and the stagger is shortened where needed so that every one-slot drop rests within 400 ms of the placement.
- **Replace tile**: old tile scales down to 0.7 and fades over `motion.quick`, new tile pops in.
- **Locked tap**: cell shakes 3 dp left-right twice over 240 ms and flashes `leak.tint`.
- **Countdown bar fade**: when the liquid starts, the countdown bar fades from 100 % to 0 % opacity over `motion.quick`. Only its alpha changes; its slot stays laid out.
- **Score below 0**: leak penalties can take the running score below 0. The number then shows a real minus sign ("−20") and the fill stays empty; it never goes left of the track's start.
- **Score bar fill**: when the score changes, the fill grows from where it is to the new length over 600 ms ease-out (`FastOutSlowInEasing`). A change while it is still moving continues from the current length. A new attempt empties the fill at once, without animating. The number shows the new score at once.
- **Threshold sparkle**: when the moving fill reaches `stars.one`, `stars.two` or `stars.three` (the end of the bar), a 600 ms procedural burst at that point: 6–8 `liquid.glow` and `brass.300` particles flying out 6–14 dp and fading, over a brief `brass.300` glow flash (radius 16 dp, fading out in the first 300 ms). Each threshold sparkles once per attempt; a jump past several thresholds sparkles each one as the fill passes it. Points beyond `stars.three` do not sparkle.
- **Points popup**: when a section scores, its points ("+10", "+15", "+25", "+50") appear centred on its tile, above the section and the liquid. Text: Fredoka Bold at 0.33 c (the slim popup used 0.4 c, which read as too large). It is drawn in this order: a `liquid.glow` glow (blur 6 dp at 70 %), then the glyphs filled solid with `text.primary` on top. The fill is opaque, so the pipe and liquid never show through the digits. There is no outline or border of any kind: no dark ink outline, no `surface.deep` fill and no stroke. Timing: fade in over 150 ms (`FastOutSlowInEasing`) while growing from 80 % to 100 %, hold 500 ms, fade out over 350 ms (`FastOutLinearInEasing`), rising 0.25 c over the full 1 000 ms. Glow and fill both take the popup's alpha. Each award has its own popup. Game time, so it freezes on pause. The board is always drawn in the Deep palette, so the popup uses the Deep `text.primary` value in both themes. A **leak penalty** on a leaky tile shows "−20" (a real minus sign) with the same recipe and timing, but its glow is `leak.500` instead of `liquid.glow`.
- **Endpoint glows**: a soft circle behind the valve wheel and behind the finish ring, radius 0.24 c + 12 dp, radial gradient from the element's colour (`brass.500`, `success.500`) at the glow alpha to transparent. The alpha is `0.45 + 0.45 × (1 − cos(2π t / T)) / 2`, easing between 45 % and 90 %. The valve pulses with *T* = `motion.drift` (1 600 ms) while the countdown runs and settles at a static 60 % the moment the liquid flows. The finish ring pulses with *T* = `motion.pulse` (2 000 ms) until the level is won; then the finish flash takes over and the ring stays solid. Both follow game time, so they freeze on pause.
- **Portal swirl**: the two arcs on every portal ring (6.4) turn clockwise at a steady speed, one full turn per `motion.drift` (1 600 ms), linear. Game time, so the swirl freezes on pause.
- **Win**: finish ring flashes, board dims to 60 %, dialog pops in, stars stagger 80 ms each with a small brass particle burst.
- **Leak**: droplet burst, board shakes 2 dp for 200 ms, then the dialog pops in after 500 ms so the player sees where it went wrong.
- **Missed mandatory section**: when the liquid reaches the finish with a mandatory section unused, the leak spurt comes out of the finish's open side, and every unused mandatory tile pulses twice in `leak.500` during the 1 s before the dialog: a fill at up to 35 % and a 3 dp edge, alpha `(1 − cos(4π t / 1 s)) / 2`. Game time, so it freezes on pause.
- **Dialog entry** (win, leak and play menu, 5.6): the scrim fades from 0 to its 72 % over `motion.quick` (160 ms ease-out) while the panel scales 0.92 → 1 with `motion.pop`. Closing is immediate.
- **Top 10 entry** (5.6): the panel scales 0.92 → 1 with `motion.pop`. Opened from a result dialog, the opaque backdrop fades in over `motion.quick` at the same time; opened from the pause dialog it is already shown and does not change. Closing is immediate.
- **Top 10 rows** (5.6): when the scores arrive, the loading state is replaced and the rows fade in from 0 to 100 % while rising 8 dp, each over `motion.quick`, starting 30 ms apart in rank order (the last starts 270 ms after the first). A state that replaces the list (empty, signed out, error) cross-fades over `motion.quick`.
- **Leak splash** (leak dialog, 5.6): when the dialog opens, 6 droplets in `liquid.500` with `liquid.glow` highlights fly out of a `leak.500` centre blob towards the edge of the splash box over 600 ms ease-out, once, and rest there. It does not loop.
- **Tip entry** (5.9): the scrim with its spotlight fades from 0 to 72 % over `motion.quick` while the callout card scales 0.92 → 1 with `motion.pop`, as a dialog does. The glow ring fades in with the scrim, then pulses on `motion.pulse`. Closing the last tip fades the scrim and card out over `motion.quick`.
- **Tip step change** (5.9): the spotlight cut-out and glow ring move and resize from the old target to the new one with `motion.settle`; the card moves to its new place with `motion.settle` while its content cross-fades over `motion.quick`. When the field has to pan to show the target, the pan runs first with `motion.settle`, then the spotlight moves.
- **Startup version label** (5.10): fully visible for 5 s after the welcome screen first appears, then fades from 100 % to 0 % opacity over 300 ms ease-out and leaves the screen.
- **Drag hint** (5.9, intro step 4): one loop takes `motion.pulse` (2 000 ms): the ghost fades in on the next slot over `motion.quick`, travels to the hint cell over 1 200 ms ease-in-out, rests there 400 ms with the cell in its valid target look, fades out over `motion.quick`, and the loop restarts. It runs while the step is shown.
- **Tap hint** (5.9, intro step 5): one loop takes `motion.pulse` (2 000 ms): the ring grows from a quarter of its size to full over the first 1 000 ms ease-out while its alpha falls from 90 % to 0, then rests unseen for 1 000 ms, and the loop restarts. The dot stays. With reduced motion nothing moves: the ring is drawn at full size.

### 7.3 Liquid progress

Progress is a 0–1 value per segment from the engine. The renderer clips the liquid stroke by path length. For corners the join point is at 0.5. For the reservoir, 0–0.1 is the inlet stub, 0.1–0.9 fills the tank bottom-up, 0.9–1.0 is the outlet stub. For a portal pair, 0–0.5 fills the entry portal's stub from its open side to the ring and 0.5–1 the exit portal's stub from the ring to its open side; the reservoir factor never applies to a pair. The renderer never eases progress; smoothness comes from the engine ticking at frame rate.

### 7.4 Reduced motion

When the system "remove animations" setting is on: no springs (replace with 160 ms ease-out), no glow pulses (static glow at 60 %), no board shake, no droplet burst (a single static splash sprite), the portal rings do not swirl, and no drops fall on a leaky tile: its wet stain shows as soon as the liquid enters the section on it. In the HUD the countdown bar disappears at once instead of fading, the score bar fill changes over 160 ms ease-out instead of 600 ms, and there is no threshold sparkle. The result dialogs and the play menu fade in over 160 ms ease-out without the scale, and the leak splash is a still sprite (the blob with its droplets at rest). The pause backdrop appears at full opacity at once and the pause panel fades in over 160 ms without the scale. The top 10 backdrop also appears at once and its panel fades in over 160 ms without the scale; its rows all fade in together over 160 ms without rising or staggering, and the loading ring is a still quarter arc instead of spinning. The stack pieces do not accelerate, bounce or stagger: they all slide to their new slot together over 160 ms ease-out, and the new top piece fades in in its slot over 160 ms. The startup version label still holds for 5 s, then fades out over 160 ms ease-out instead of 300 ms. Liquid fill and the win tally are gameplay, so they are unaffected.

---

## 8. Iconography

Rounded-stroke icons, 24 dp grid, 2 dp stroke, round caps and joins, drawn as `ImageVector`s. Colour follows `text.primary` unless specified.

Required set: play, pause, restart, next, back, close, settings, sound-on, sound-off, music-on, music-off, lock, star (filled + outline), trophy, fast-forward (start flow now), hourglass (slow badge), wave (flow speed), drop (liquid), warning, info, check, finish-checkered (finish).

`finish-checkered` is the checkered finish of 6.4 on the 24 dp grid: a 2 dp ring of radius 9 dp in the icon colour around a disc filled with a 6 × 6 checker spanning the ring's inner diameter (squares of about 2.7 dp), clipped to the disc. It is the only filled icon in the set: the light squares use the icon colour and the dark squares stay transparent.

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
| Points popup | White fill under a 3 dp `InkDark` outline | `text.primary` fill and a soft `liquid.glow` glow, no border |
