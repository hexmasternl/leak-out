# Leak Out — Game Design System Specification

## 1. Overview & Visual Identity

**Leak Out** is a vibrant, fast-paced 2D mobile pipe-puzzle game built natively for Android. The design system emphasizes a **tactile, juicy, high-energy cartoon aesthetic**. Key visual hallmarks include bold ink outlines, thick tactile drop shadows, glossy highlights, smooth spring animations, and playful liquid effects.

### Core Visual Principles

1. **Bold & Chunky Shapes**: All UI elements use generous corner radii ($12\text{dp}$ to $24\text{dp}$) and thick dark outlines ($3\text{px}$ to $4\text{px}$) to evoke a hand-drawn cartoon feel.

2. **Tactile Depth ("Juiciness")**: Buttons, tiles, and HUD panels feature physical 3D drop-offsets. When tapped, elements visibly compress downwards.

3. **High Contrast & Vibrancy**: Saturated primary tones combined with clean dark ink borders ensure crisp readability on mobile screens.

4. **Fluid Motion**: Liquid flow is represented by bubbly animated fill lines, particle splashes, and dynamic pressure gauges.

## 2. Color Palette & Token System

### 2.1 Primary Color — Hydro Blue (`#0277BD`)

The foundational color for liquid, primary buttons, title branding, and water-themed UI elements.

| 

| **Token** | **Hex Code** | **Usage** | 
| `color-primary-dark` | `#013C60` | Heavy ink outlines, button bottom shadows | 
| `color-primary-base` | `#0277BD` | Main brand color, liquid standard flow, top-level bars | 
| `color-primary-mid` | `#0288D1` | Button face highlights, active grid cell background | 
| `color-primary-light` | `#03A9F4` | Bright liquid highlights, dynamic fill heads | 
| `color-primary-soft` | `#B2EBF2` | Inactive pipe background, soft badge tints | 
| `color-primary-bg` | `#E0F7FA` | Game screen background, modal backdrops | 

### 2.2 Secondary Color — Caution / Brass Yellow (`#FBC02D`)

The high-visibility accent color used for play buttons, pipe stack borders, level stars, warning badges, and valve handles.

| **Token** | **Hex Code** | **Usage** | 
| `color-secondary-dark` | `#F57F17` | Secondary button shadows, brass rim shading | 
| `color-secondary-base` | `#FBC02D` | Play buttons, valves, queue stack highlight | 
| `color-secondary-light` | `#FDD835` | Button hover/active highlights, badge stars | 
| `color-secondary-soft` | `#FFF59D` | Toast message light fill, secondary highlights | 
| `color-secondary-bg` | `#FFFDE7` | Warm card container backgrounds | 

### 2.3 Structural & Neutral Colors

| **Token** | **Hex Code** | **Usage** | 
| `color-ink-dark` | `#1E293B` | Main outlines ($3\text{px}$ - $4\text{px}$ stroke), primary text | 
| `color-ink-light` | `#475569` | Secondary text, subheaders, inactive state strokes | 
| `color-steel-base` | `#546E7A` | Base metallic color for dry pipe bodies | 
| `color-steel-dark` | `#37474F` | Steel pipe outline shading and flange grooves | 
| `color-steel-light` | `#90A4AE` | Metal reflection lines and pipe top-highlights | 
| `color-surface-card` | `#FFFFFF` | Modal dialog cards, white panel overlays | 

### 2.4 Functional & Status Colors

| **Token** | **Hex Code** | **Usage** | 
| `color-reservoir-purple` | `#8E24AA` | Reservoir pipe liquid delay indicator | 
| `color-success-green` | `#43A047` | Win banner, connected pipe glow, level complete | 
| `color-error-red` | `#E53935` | Burst pipe leak, game over alert, reset button | 

## 3. Typography System

The typography uses friendly, heavy-rounded cartoon fonts to match the playful game mood.

* **Primary Display Font**: *Fredoka* (or *Titan One*) — Bold, rounded, impactful.

* **Secondary UI Font**: *Nunito* — Clean, legibly rounded sans-serif for numbers, descriptions, and HUD stats.

| **Style** | **Font Family** | **Size (sp)** | **Weight** | **Text Transform** | **Line Height** | 
| **Title / Hero** | Fredoka | $36\text{sp} - 48\text{sp}$ | Bold ($700$) | UPPERCASE | $1.1$ | 
| **Header 1 (Modals)** | Fredoka | $28\text{sp}$ | Bold ($700$) | UPPERCASE | $1.2$ | 
| **Header 2 (HUD)** | Fredoka | $20\text{sp}$ | SemiBold ($600$) | Mixed | $1.25$ | 
| **Button Text** | Fredoka | $18\text{sp}$ | Bold ($700$) | UPPERCASE | $1.0$ | 
| **Body / Labels** | Nunito | $14\text{sp} - 16\text{sp}$ | Bold ($700$) | Mixed | $1.4$ | 
| **Caption / Stat** | Nunito | $12\text{sp}$ | Medium ($500$) | Mixed | $1.3$ | 

## 4. UI Elements & Stylistic Rules

### 4.1 Cartoon Outline & Shadow Rules

Every interactive component or game card must follow the **3-Layer Cartoon Recipe**:

1. **Border**: `3px solid #1E293B`

2. **Bottom Offset Shadow**: `box-shadow: 0px 6px 0px #013C60` (or matching dark shade)

3. **Inner High Shine**: `box-shadow: inset 0px 3px 0px rgba(255, 255, 255, 0.4)`

### 4.2 Button States & Physics

* **Resting State**: Upward offset, full $6\text{px}$ bottom shadow.

* **Pressed State**: `transform: translateY(4px);`, shadow shrinks to $2\text{px}$.

* **Disabled State**: Desaturated gray fill `#94A3B8`, shadow `#64748B`, opacity $0.7$.

## 5. Game Board & Pipe Sections Architecture

### 5.1 Grid Cell Blueprint

* Default cell dimensions: $64 \times 64\text{dp}$ (scales dynamically with Android screen width).

* Grid background: Soft grid lines over `#E0F7FA` with subtle drop-shadow inset.

### 5.2 Pipe Visual Taxonomy

Pipes consist of a dark gray steel body (`#546E7A`), darker joints (`#37474F`), and an inner hollow channel where the water flows.

```
       [ TOP PORT ]
            ||
 [LEFT] === [  ] === [RIGHT]
            ||
      [ BOTTOM PORT ]

```

1. **Straight Horizontal**: Connects LEFT $\leftrightarrow$ RIGHT.

2. **Straight Vertical**: Connects TOP $\leftrightarrow$ BOTTOM.

3. **Corners (x4)**:

   * Left-Top, Left-Bottom, Right-Top, Right-Bottom.

4. **Cross Section**:

   * Independent dual channels (Horizontal tunnel bridges over Vertical tunnel).

5. **Reservoir Section**:

   * Expanded spherical or bulbous center container with speed modifier ($\times 0.35$).

6. **Start Valve**:

   * Brass valve handle (`#FBC02D`) with arrow showing initial flow direction.

7. **Finish Drain**:

   * Golden drain grate with pulsing green target ring.

## 6. Motion & Animation Principles

| **Animation** | **Duration** | **Easing Curve** | **Description** | 
| **Pipe Drop** | $250\text{ms}$ | `overshoot(1.4)` | Pipe snaps onto grid with a bouncy squish. | 
| **Liquid Flow Fill** | Linear / Continuous | `linear` | Clip path or progress percentage filling the pipe. | 
| **Reservoir Swirl** | $1200\text{ms}$ | `ease-in-out` continuous | Bubbles rotate slowly while filling the reservoir. | 
| **Leak / Spill** | $400\text{ms}$ | `bounce` | Liquid pops out of open pipe end into a cartoon splash puddle. | 
| **Level Win Pop** | $600\text{ms}$ | `elastic(1.2)` | Star dialog pops up onto screen with confetti explosion. | 

## 7. Audio & Sound Design Guidelines

* **Pipe Placement**: Tactile metal *Clank-Snap!* sound.

* **Liquid Flow**: Continuous soft gurgling / water stream loop.

* **Reservoir Fill**: Deeper glug-glug-glug pitching down.

* **Timer Countdown**: Mechanical ticking clock.

* **Spill / Game Over**: Loud cartoon *SPLASH-SQUIRT!* followed by a comical slide-whistle.

* **Level Clear**: Energetic brass fan-fare tune with water drops sparkle.