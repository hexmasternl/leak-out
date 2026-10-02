// Generates the proposed achievement icons (512x512 SVG) in the Deep Flow style.
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const OUT = process.argv[2];
mkdirSync(OUT, { recursive: true });

const T = {
  abyss: '#07111C', deep: '#0C1A2A', base: '#122538', raised: '#19324A', overlay: '#21405C',
  glow: '#9DF3FF', l300: '#5EE7FF', l500: '#19C6F2', l700: '#0A8DC2', l900: '#065A82',
  b100: '#FFE9B0', b300: '#FFCF5C', b500: '#F5A623', b700: '#C97A12', b900: '#7A4708',
  leak: '#FF5A5F', ok: '#3DDC97', ok7: '#1FA971', r300: '#C7B5FF', r500: '#A78BFA', r700: '#7C5CE6',
  s100: '#C9D6E2', s300: '#8FA3B8', s500: '#5C7189', s700: '#3A4B60', s900: '#22303F',
  tp: '#F2F7FB', ts: '#A9BACB', tm: '#6E8296',
};

const RING = {
  progress: T.l500, skill: T.b500, hidden: T.r500,
  tier1: T.s300, tier2: T.l300, tier3: T.b300,
};

const W = 56; // pipe body width

const defs = `
<defs>
  <radialGradient id="bg" cx="50%" cy="45%" r="70%">
    <stop offset="0" stop-color="${T.base}"/><stop offset="0.7" stop-color="${T.deep}"/><stop offset="1" stop-color="${T.abyss}"/>
  </radialGradient>
  <linearGradient id="disc" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="${T.raised}"/><stop offset="1" stop-color="${T.base}"/>
  </linearGradient>
  <linearGradient id="brass" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="${T.b300}"/><stop offset="1" stop-color="${T.b500}"/>
  </linearGradient>
  <linearGradient id="liquid" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="${T.l300}"/><stop offset="0.5" stop-color="${T.l500}"/><stop offset="1" stop-color="${T.l700}"/>
  </linearGradient>
  <linearGradient id="res" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="${T.r500}"/><stop offset="1" stop-color="${T.r700}"/>
  </linearGradient>
  <filter id="blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="12"/></filter>
  <filter id="blurS" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
  <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
    <feDropShadow dx="0" dy="10" stdDeviation="14" flood-color="#000" flood-opacity="0.5"/>
  </filter>
</defs>`;

const glowed = (inner, color, op = 0.45, f = 'blur') =>
  `<g opacity="${op}" filter="url(#${f})">${inner.replaceAll('__C__', color)}</g>${inner.replaceAll('__C__', color)}`;

function frame(ring, glyph) {
  const ringEl = `<circle cx="256" cy="256" r="196" fill="none" stroke="__C__" stroke-width="14"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">${defs}
<rect width="512" height="512" fill="url(#bg)"/>
<circle cx="256" cy="256" r="196" fill="url(#disc)" filter="url(#shadow)"/>
${glowed(ringEl, ring, 0.5)}
<circle cx="256" cy="256" r="186" fill="none" stroke="#FFFFFF" stroke-opacity="0.08" stroke-width="2"/>
<path d="M 108 200 A 160 160 0 0 1 404 200" fill="none" stroke="#FFFFFF" stroke-opacity="0.10" stroke-width="3" stroke-linecap="round"/>
${glyph}
</svg>`;
}

// A pipe along path d, drawn with the design system's pipe recipe; liquid fills it when wet.
function pipe(d, { wet = true, w = W, glow = true } = {}) {
  const p = (stroke, sw, extra = '') =>
    `<path d="${d}" fill="none" stroke="${stroke}" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" ${extra}/>`;
  let s = p(T.s700, w, 'transform="translate(3 4)"') + p(T.s500, w) +
    p(T.s100, w * 0.1, `stroke-opacity="0.55" transform="translate(${-w * 0.14} ${-w * 0.14})"`) +
    p(T.s900, w * 0.7);
  if (wet) {
    if (glow) s += `<g opacity="0.5" filter="url(#blurS)">${p(T.glow, w * 0.6)}</g>`;
    s += p(T.l700, w * 0.56) + p(T.l500, w * 0.42) + p(T.l300, w * 0.16, 'stroke-opacity="0.9"');
  }
  return s;
}

// Flange band across a pipe end at (x, y), for a pipe running horizontally (h) or vertically.
const flange = (x, y, h = true, w = W) => h
  ? `<rect x="${x - 7}" y="${y - w / 2 - 4}" width="14" height="${w + 8}" rx="4" fill="${T.s300}" stroke="${T.s700}" stroke-width="3"/>`
  : `<rect x="${x - w / 2 - 4}" y="${y - 7}" width="${w + 8}" height="14" rx="4" fill="${T.s300}" stroke="${T.s700}" stroke-width="3"/>`;

function starPath(cx, cy, R, r = R * 0.48) {
  const pts = [];
  for (let i = 0; i < 10; i++) {
    const a = -Math.PI / 2 + (i * Math.PI) / 5;
    const rad = i % 2 ? r : R;
    pts.push(`${(cx + rad * Math.cos(a)).toFixed(1)},${(cy + rad * Math.sin(a)).toFixed(1)}`);
  }
  return `M ${pts.join(' L ')} Z`;
}
const star = (cx, cy, R, fill = 'url(#brass)', glow = true) => {
  const d = starPath(cx, cy, R);
  return (glow ? `<path d="${d}" fill="${T.b500}" opacity="0.45" filter="url(#blurS)"/>` : '') +
    `<path d="${d}" fill="${fill}" stroke="${T.b700}" stroke-width="${Math.max(3, R * 0.08)}" stroke-linejoin="round"/>` +
    `<path d="${starPath(cx, cy - R * 0.08, R * 0.55)}" fill="${T.b100}" opacity="0.35"/>`;
};

const drop = (cx, cy, s = 1, fill = 'url(#liquid)') =>
  `<path transform="translate(${cx} ${cy}) scale(${s})" d="M0,-60 C14,-38 40,-14 40,14 A40,40 0 0 1 -40,14 C-40,-14 -14,-38 0,-60 Z" fill="${fill}"/>` +
  `<path transform="translate(${cx} ${cy}) scale(${s})" d="M-18,2 A22,22 0 0 0 -8,26" fill="none" stroke="${T.glow}" stroke-width="6" stroke-linecap="round" opacity="0.8"/>`;

const check = (cx, cy, s = 1, color = T.ok) => glowed(
  `<path transform="translate(${cx} ${cy}) scale(${s})" d="M-30,0 L-8,22 L32,-22" fill="none" stroke="__C__" stroke-width="16" stroke-linecap="round" stroke-linejoin="round"/>`,
  color, 0.4, 'blurS');

const badge = (cx, cy, inner, fill = T.raised, stroke = T.ok) =>
  `<circle cx="${cx}" cy="${cy}" r="44" fill="${fill}" stroke="${stroke}" stroke-width="6"/>${inner}`;

// Tier pips: n of 3 filled small stars at the bottom of the medallion.
function pips(n, color = T.b300) {
  let s = '';
  for (let i = 0; i < 3; i++) {
    const x = 256 + (i - 1) * 40;
    const d = starPath(x, 392, 15);
    s += i < n
      ? `<path d="${d}" fill="${color}" stroke="${T.b700}" stroke-width="2" stroke-linejoin="round"/>`
      : `<path d="${d}" fill="none" stroke="#FFFFFF" stroke-opacity="0.16" stroke-width="3" stroke-linejoin="round"/>`;
  }
  return s;
}

// Roman numeral bars (I, II, III) drawn as rounded rectangles.
function numeral(n, cx, cy, color = T.b100) {
  let s = '';
  const gap = 22;
  for (let i = 0; i < n; i++) {
    const x = cx + (i - (n - 1) / 2) * gap;
    s += `<rect x="${x - 6}" y="${cy - 24}" width="12" height="48" rx="6" fill="${color}"/>`;
  }
  return s;
}

function wrench(cx, cy, s = 1) {
  return `<g transform="translate(${cx} ${cy}) rotate(-45) scale(${s})">
    <rect x="-22" y="-10" width="44" height="190" rx="22" fill="${T.s700}" transform="translate(3 4)"/>
    <rect x="-22" y="-10" width="44" height="190" rx="22" fill="${T.s500}"/>
    <rect x="-14" y="10" width="6" height="150" rx="3" fill="${T.s100}" opacity="0.55"/>
    <mask id="jaw"><rect x="-100" y="-200" width="200" height="200" fill="#fff"/><rect x="-22" y="-160" width="44" height="90" rx="6" fill="#000"/></mask>
    <circle cx="0" cy="-48" r="66" fill="${T.s700}" transform="translate(3 4)" mask="url(#jaw)"/>
    <circle cx="0" cy="-48" r="66" fill="${T.s300}" mask="url(#jaw)"/>
    <path d="M -50,-88 A 66 66 0 0 1 -30,-106" fill="none" stroke="${T.s100}" stroke-width="6" stroke-linecap="round" opacity="0.7"/>
  </g>`;
}

const valve = (cx, cy, r = 54) => {
  let spokes = '';
  for (let i = 0; i < 4; i++) {
    const a = (i * Math.PI) / 2 + Math.PI / 4;
    spokes += `<line x1="${cx}" y1="${cy}" x2="${cx + r * Math.cos(a)}" y2="${cy + r * Math.sin(a)}" stroke="${T.b700}" stroke-width="12" stroke-linecap="round"/>`;
  }
  return `<circle cx="${cx}" cy="${cy}" r="${r + 14}" fill="${T.b500}" opacity="0.4" filter="url(#blurS)"/>` +
    `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="url(#brass)" stroke-width="16"/>` + spokes +
    `<circle cx="${cx}" cy="${cy}" r="16" fill="${T.b300}" stroke="${T.b700}" stroke-width="4"/>`;
};

// A glass gauge tube filled to [level] (0-1); later levels-finished tiers fill it further.
function gauge(level) {
  const top = 122, h = 250, y = top + h * (1 - level);
  return `<rect x="206" y="112" width="100" height="270" rx="50" fill="${T.s900}" stroke="${T.s300}" stroke-width="10"/>` +
    `<clipPath id="tube"><rect x="216" y="122" width="80" height="250" rx="40"/></clipPath>` +
    `<g clip-path="url(#tube)"><rect x="200" y="${y}" width="120" height="${h}" fill="url(#liquid)"/>` +
    `<path d="M 200 ${y} Q 230 ${y - 10} 256 ${y} T 320 ${y}" fill="none" stroke="${T.glow}" stroke-width="6"/></g>` +
    [0.25, 0.5, 0.75].map(f => `<line x1="316" y1="${top + h * f}" x2="340" y2="${top + h * f}" stroke="${T.b300}" stroke-width="6" stroke-linecap="round"/>`).join('') +
    `<circle cx="240" cy="330" r="7" fill="${T.glow}" opacity="0.8"/><circle cx="268" cy="350" r="5" fill="${T.glow}" opacity="0.7"/>`;
}

const threeStars = () => star(160, 280, 62) + star(352, 280, 62) + star(256, 220, 84) +
  `<path d="M 256 90 l 8 20 l 20 8 l -20 8 l -8 20 l -8 -20 l -20 -8 l 20 -8 Z" fill="${T.glow}"/>` +
  `<path d="M 376 160 l 5 12 l 12 5 l -12 5 l -5 12 l -5 -12 l -12 -5 l 12 -5 Z" fill="${T.glow}" opacity="0.8"/>`;

// A brass trophy full of liquid with the pack number as roman bars on its base.
function trophy(pack) {
  let bars = '';
  for (let i = 0; i < pack; i++) {
    const x = 256 + (i - (pack - 1) / 2) * 16;
    bars += `<rect x="${x - 4}" y="${355}" width="8" height="24" rx="4" fill="${T.b900}"/>`;
  }
  return `<path d="M 166 150 L 346 150 L 334 252 A 78 78 0 0 1 178 252 Z" fill="url(#brass)" stroke="${T.b700}" stroke-width="6" stroke-linejoin="round"/>` +
    `<path d="M 186 196 L 326 196 L 320 252 A 64 64 0 0 1 192 252 Z" fill="url(#liquid)"/>` +
    `<path d="M 186 196 Q 221 186 256 196 T 326 196" fill="none" stroke="${T.glow}" stroke-width="5"/>` +
    `<path d="M 166 170 C 116 170 116 236 186 244" fill="none" stroke="${T.b500}" stroke-width="14" stroke-linecap="round"/>` +
    `<path d="M 346 170 C 396 170 396 236 326 244" fill="none" stroke="${T.b500}" stroke-width="14" stroke-linecap="round"/>` +
    `<rect x="236" y="322" width="40" height="22" fill="${T.b700}"/>` +
    `<rect x="186" y="342" width="140" height="48" rx="12" fill="url(#brass)" stroke="${T.b700}" stroke-width="5"/>` + bars;
}

const icons = {
  first_leak_fixed: ['progress',
    pipe('M 130 250 L 382 250') + flange(130, 250) + flange(382, 250) +
    badge(330, 336, check(330, 338, 0.9))],

  levels_finished_1: ['tier1', gauge(0.4)],

  star_collector_1: ['tier1', star(256, 236, 110) + numeral(1, 256, 372)],
  star_collector_2: ['tier2', star(256, 236, 110) + numeral(2, 256, 372)],

  perfectionist_1: ['tier1', threeStars()],

  pack_1_complete: ['progress', trophy(1) + star(256, 112, 22)],
  pack_1_perfect: ['progress', trophy(1) + star(196, 118, 22) + star(256, 98, 26) + star(316, 118, 22)],

  zero_waste: ['skill',
    pipe('M 256 140 A 116 116 0 1 1 255.9 140 Z', { w: 60 }) +
    check(256, 260, 1.1, T.ok)],

  double_cross: ['skill',
    pipe('M 256 110 L 256 402') +
    `<path d="M 110 256 L 402 256" stroke="${T.base}" stroke-width="${W + 14}" stroke-linecap="round"/>` +
    pipe('M 110 256 L 402 256') +
    flange(256, 112, false) + flange(256, 400, false) + flange(112, 256) + flange(400, 256)],

  crossroads_king: ['skill',
    pipe('M 256 196 L 256 392', { w: 50 }) +
    `<path d="M 130 285 L 382 285" stroke="${T.base}" stroke-width="64" stroke-linecap="round"/>` +
    pipe('M 130 285 L 382 285', { w: 50 }) +
    `<g transform="translate(256 168) scale(0.85) translate(-256 -130)">` +
    `<path d="M 186 156 L 176 92 L 218 122 L 256 76 L 294 122 L 336 92 L 326 156 Z" fill="${T.b500}" opacity="0.4" filter="url(#blurS)"/>` +
    `<path d="M 186 156 L 176 92 L 218 122 L 256 76 L 294 122 L 336 92 L 326 156 Z" fill="url(#brass)" stroke="${T.b700}" stroke-width="6" stroke-linejoin="round"/>` +
    `<circle cx="256" cy="134" r="9" fill="${T.l300}"/></g>`],

  ahead_of_the_flow: ['skill',
    `<circle cx="256" cy="270" r="112" fill="${T.s900}" stroke="${T.s300}" stroke-width="12"/>` +
    `<rect x="236" y="128" width="40" height="30" rx="8" fill="${T.s300}"/>` +
    `<rect x="222" y="108" width="68" height="26" rx="10" fill="url(#brass)" stroke="${T.b700}" stroke-width="4"/>` +
    glowed(`<path d="M 256 178 A 92 92 0 0 1 348 270" fill="none" stroke="__C__" stroke-width="22" stroke-linecap="round"/>`, T.l500, 0.5, 'blurS') +
    `<line x1="256" y1="270" x2="256" y2="200" stroke="${T.tp}" stroke-width="12" stroke-linecap="round"/>` +
    `<circle cx="256" cy="270" r="14" fill="${T.b300}"/>` +
    badge(352, 360, check(352, 362, 0.9))],

  full_throttle: ['skill',
    pipe('M 110 300 L 402 300', { w: 50 }) +
    glowed(`<path d="M 170 130 L 256 210 L 170 290 Z M 262 130 L 348 210 L 262 290 Z" fill="__C__" stroke="__C__" stroke-width="14" stroke-linejoin="round"/>`, T.l300, 0.6) +
    `<path d="M 170 130 L 256 210 L 170 290 Z M 262 130 L 348 210 L 262 290 Z" fill="url(#liquid)" stroke="${T.l300}" stroke-width="14" stroke-linejoin="round"/>`],

  measure_twice: ['skill',
    `<g transform="rotate(-30 256 256)">` +
    `<rect x="116" y="200" width="280" height="96" rx="14" fill="url(#brass)" stroke="${T.b700}" stroke-width="6"/>` +
    [0, 1, 2, 3, 4, 5, 6, 7, 8].map(i => `<rect x="${142 + i * 28}" y="206" width="8" height="${i % 2 ? 26 : 44}" rx="4" fill="${T.b900}"/>`).join('') +
    `</g>` + badge(340, 350, check(340, 352, 0.9))],

  steady_hands: ['skill',
    `<rect x="186" y="150" width="50" height="190" rx="18" fill="${T.s500}" stroke="${T.s700}" stroke-width="6"/>` +
    `<rect x="276" y="150" width="50" height="190" rx="18" fill="${T.s500}" stroke="${T.s700}" stroke-width="6"/>` +
    `<line x1="150" y1="370" x2="362" y2="120" stroke="${T.base}" stroke-width="40" stroke-linecap="round"/>` +
    glowed(`<line x1="150" y1="370" x2="362" y2="120" stroke="__C__" stroke-width="22" stroke-linecap="round"/>`, T.b500, 0.5, 'blurS')],

  long_way_round: ['skill',
    pipe('M 120 150 L 330 150 A 50 50 0 0 1 330 250 L 182 250 A 50 50 0 0 0 182 350 L 392 350', { w: 44 }) +
    flange(122, 150, true, 44) + flange(390, 350, true, 44)],

  big_spender: ['skill',
    [3, 2, 1, 0].map(i => {
      const y = 340 - i * 44;
      return `<ellipse cx="256" cy="${y + 12}" rx="100" ry="34" fill="${T.b700}"/>` +
        `<rect x="156" y="${y - 10}" width="200" height="22" fill="${T.b700}"/>` +
        `<ellipse cx="256" cy="${y - 10}" rx="100" ry="34" fill="url(#brass)" stroke="${T.b700}" stroke-width="4"/>` +
        `<ellipse cx="256" cy="${y - 10}" rx="60" ry="18" fill="none" stroke="${T.b100}" stroke-width="4" opacity="0.6"/>`;
    }).join('') + drop(256, 180, 0.6)],

  apprentice: ['tier1', wrench(256, 236, 0.95) + pips(1)],
  journeyman: ['tier2', wrench(256, 236, 0.95) + pips(2)],
  guildmaster: ['tier3', wrench(256, 236, 0.95) + pips(3)],

  never_give_up: ['progress',
    pipe('M 110 260 L 402 260') + flange(112, 260) + flange(400, 260) +
    `<rect x="226" y="214" width="60" height="92" rx="12" fill="url(#brass)" stroke="${T.b700}" stroke-width="5"/>` +
    `<line x1="236" y1="236" x2="276" y2="236" stroke="${T.b700}" stroke-width="5"/><line x1="236" y1="284" x2="276" y2="284" stroke="${T.b700}" stroke-width="5"/>` +
    `<circle cx="210" cy="208" r="7" fill="${T.l300}" opacity="0.6"/><circle cx="300" cy="200" r="5" fill="${T.l300}" opacity="0.5"/>` +
    glowed(`<path d="M 256 180 L 256 110 M 226 138 L 256 108 L 286 138" fill="none" stroke="__C__" stroke-width="16" stroke-linecap="round" stroke-linejoin="round"/>`, T.ok, 0.4, 'blurS')],

  personal_best: ['progress',
    `<rect x="150" y="300" width="64" height="70" rx="10" fill="${T.s500}"/>` +
    `<rect x="224" y="250" width="64" height="120" rx="10" fill="${T.s300}"/>` +
    `<rect x="298" y="200" width="64" height="170" rx="10" fill="url(#liquid)"/>` +
    `<rect x="298" y="200" width="64" height="170" rx="10" fill="${T.l500}" opacity="0.3" filter="url(#blurS)"/>` +
    star(330, 140, 44)],

  wet_feet: ['progress',
    `<ellipse cx="256" cy="350" rx="140" ry="36" fill="${T.l700}" opacity="0.6"/>` +
    `<ellipse cx="256" cy="344" rx="104" ry="22" fill="${T.l500}" opacity="0.7"/>` +
    `<ellipse cx="226" cy="338" rx="40" ry="6" fill="${T.glow}" opacity="0.5"/>` +
    drop(256, 230, 1.35) + drop(170, 196, 0.5) + drop(346, 176, 0.42)],

  instant_flood: ['hidden',
    valve(256, 286, 62) +
    [[-150, 0.45], [-110, 0.55], [-70, 0.45], [-30, 0.4], [-200, 0.4], [-240, 0.35]].map(([deg, s], i) => {
      const a = (deg * Math.PI) / 180;
      const r = 130 + (i % 2) * 14;
      return `<g transform="translate(${256 + r * Math.cos(a)} ${286 + r * Math.sin(a)}) rotate(${deg + 90})">${drop(0, 0, s)}</g>`;
    }).join('')],

  wall_to_wall: ['hidden',
    (() => {
      let s = '';
      for (let r = 0; r < 3; r++) for (let c = 0; c < 3; c++)
        s += `<rect x="${146 + c * 76}" y="${146 + r * 76}" width="68" height="68" rx="10" fill="${T.overlay}" opacity="0.7" stroke="#FFFFFF" stroke-opacity="0.08" stroke-width="2"/>`;
      return s;
    })() +
    pipe('M 140 180 L 332 180 A 38 38 0 0 1 332 256 L 180 256 A 38 38 0 0 0 180 332 L 372 332', { w: 36 })],

  photo_finish: ['hidden',
    pipe('M 110 256 L 236 256', { w: 52 }) +
    `<circle cx="320" cy="256" r="90" fill="${T.ok}" opacity="0.35" filter="url(#blur)"/>` +
    `<circle cx="320" cy="256" r="66" fill="${T.s900}" stroke="${T.ok}" stroke-width="14"/>` +
    [-24, 0, 24].map(dy => `<rect x="284" y="${250 + dy}" width="72" height="12" rx="6" fill="${T.s300}"/>`).join('') +
    `<circle cx="238" cy="256" r="16" fill="${T.glow}"/>` +
    `<path d="M 380 150 l 12 30 l 30 12 l -30 12 l -12 30 l -12 -30 l -30 -12 l 30 -12 Z" fill="#FFFFFF"/>` +
    `<path d="M 380 150 l 12 30 l 30 12 l -30 12 l -12 30 l -12 -30 l -30 -12 l 30 -12 Z" fill="${T.glow}" opacity="0.6" filter="url(#blurS)"/>`],

  hidden: ['hidden',
    `<path d="M 206 200 A 50 50 0 1 1 286 240 C 266 254 256 264 256 296" fill="none" stroke="${T.r300}" stroke-width="34" stroke-linecap="round" stroke-linejoin="round"/>` +
    `<circle cx="256" cy="356" r="22" fill="${T.r300}"/>`],
};

for (const [key, [ring, glyph]] of Object.entries(icons)) {
  writeFileSync(join(OUT, `achievement_${key}.svg`), frame(RING[ring], glyph));
}
console.log(Object.keys(icons).length, 'icons written');
