// Generates the Google Play feature graphic (1024x500) in the Deep Flow design system and
// rasterises it with the globally installed Playwright Chromium, like assets/icon/render-icon.js.
//   node generate-feature-graphic.js
// Outputs: feature-graphic.svg (source, fonts by name) and feature-graphic-1024x500.png
const fs = require('fs');
const path = require('path');

const W = 1024, H = 500;
const C = {
  abyss:'#07111C', deep:'#0C1A2A', base:'#122538', raised:'#19324A', overlay:'#21405C',
  lglow:'#9DF3FF', l300:'#5EE7FF', l500:'#19C6F2', l700:'#0A8DC2', l900:'#065A82',
  b100:'#FFE9B0', b300:'#FFCF5C', b500:'#F5A623', b700:'#C97A12', b900:'#7A4708',
  s100:'#C9D6E2', s300:'#8FA3B8', s500:'#5C7189', s700:'#3A4B60', s900:'#22303F',
  text:'#F2F7FB', text2:'#A9BACB', success:'#3DDC97',
};
const out = [];
const p = s => out.push(s);

// ---------------- board geometry ----------------
const COLS = 7, ROWS = 5, CELL = 64;
const BX = 532, BY = 90;
const cx = c => BX + CELL / 2 + c * CELL;
const cy = r => BY + CELL / 2 + r * CELL;
const PIPE_W = 30;

// polyline -> path with rounded elbows
function route(pts, r) {
  let d = `M${pts[0][0]},${pts[0][1]}`;
  for (let i = 1; i < pts.length - 1; i++) {
    const [x0, y0] = pts[i - 1], [x1, y1] = pts[i], [x2, y2] = pts[i + 1];
    const a = Math.atan2(y0 - y1, x0 - x1), b = Math.atan2(y2 - y1, x2 - x1);
    d += ` L${x1 + Math.cos(a) * r},${y1 + Math.sin(a) * r}`;
    d += ` Q${x1},${y1} ${x1 + Math.cos(b) * r},${y1 + Math.sin(b) * r}`;
  }
  const [xe, ye] = pts[pts.length - 1];
  return d + ` L${xe},${ye}`;
}

// the first `fraction` of a polyline, measured along its straight segments
function truncate(pts, fraction) {
  const lens = pts.slice(1).map((q, i) => Math.hypot(q[0] - pts[i][0], q[1] - pts[i][1]));
  let left = lens.reduce((a, b) => a + b, 0) * fraction;
  const res = [pts[0]];
  for (let i = 0; i < lens.length; i++) {
    if (left >= lens[i]) { res.push(pts[i + 1]); left -= lens[i]; continue; }
    const t = left / lens[i];
    res.push([pts[i][0] + (pts[i + 1][0] - pts[i][0]) * t, pts[i][1] + (pts[i + 1][1] - pts[i][1]) * t]);
    break;
  }
  return res;
}

const stroke = (d, color, width, extra = '') =>
  `<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="butt" stroke-linejoin="round" ${extra}/>`;

function steel(pts, w, radius) {
  const d = route(pts, radius);
  p(stroke(d, C.abyss, w + 8, `opacity="0.6" transform="translate(4,7)" filter="url(#blur4)"`));
  p(stroke(d, C.s700, w));
  p(stroke(d, C.s500, w - 6, `transform="translate(-1,-2)"`));
  p(stroke(d, C.s100, 2, `opacity="0.45" transform="translate(${-(w / 2) + 6},${-(w / 2) + 6})"`));
  p(stroke(d, C.s900, w * 0.46));
}

function liquid(pts, w, radius) {
  const d = route(pts, radius);
  const wl = w * 0.36;
  p(stroke(d, C.lglow, wl + 10, `opacity="0.55" filter="url(#blur6)"`));
  p(stroke(d, C.l700, wl));
  p(stroke(d, C.l500, wl - 4, `transform="translate(-0.5,-1)"`));
  p(stroke(d, C.l300, 2, `opacity="0.9" transform="translate(${-(wl / 2) + 3},${-(wl / 2) + 3})"`));
}

function rivet(x, y, r = 4) {
  p(`<circle cx="${x}" cy="${y}" r="${r + 1}" fill="${C.abyss}" opacity="0.6"/><circle cx="${x}" cy="${y - 1}" r="${r}" fill="${C.s700}"/><circle cx="${x - 1.2}" cy="${y - 2.2}" r="${r * 0.4}" fill="${C.s300}" opacity="0.8"/>`);
}

function valve(x, y, r) {
  const spokes = [0, 45, 90, 135].map(a => `<line x1="${-r}" y1="0" x2="${r}" y2="0" stroke="${C.b700}" stroke-width="6" stroke-linecap="round" transform="rotate(${a})"/><line x1="${-r + 3}" y1="-0.5" x2="${r - 3}" y2="-0.5" stroke="${C.b500}" stroke-width="3.5" stroke-linecap="round" transform="rotate(${a})"/>`).join('');
  p(`<g transform="translate(${x},${y}) rotate(12)">
    <circle r="${r + 8}" fill="${C.b500}" opacity="0.45" filter="url(#blur10)"/>
    <circle r="${r + 2}" fill="${C.b900}" opacity="0.7" transform="translate(2,4)"/>
    <circle r="${r}" fill="none" stroke="${C.b700}" stroke-width="7"/>
    <circle r="${r}" fill="none" stroke="url(#brass)" stroke-width="5"/>
    ${spokes}
    <circle r="${r * 0.34}" fill="${C.b700}"/><circle r="${r * 0.27}" fill="url(#brassR)"/>
    <circle cx="${-r * 0.08}" cy="${-r * 0.1}" r="${r * 0.1}" fill="${C.b100}" opacity="0.9"/>
  </g>`);
}

function drain(x, y, r) {
  p(`<g transform="translate(${x},${y})">
    <circle r="${r + 10}" fill="${C.success}" opacity="0.28" filter="url(#blur10)"/>
    <circle r="${r + 2}" fill="${C.abyss}" opacity="0.6" transform="translate(2,4)"/>
    <circle r="${r}" fill="${C.s700}"/>
    <circle r="${r - 3}" fill="${C.s900}"/>
    <circle r="${r}" fill="none" stroke="${C.success}" stroke-width="3.5"/>
    ${[0, 60, 120].map(a => `<line x1="${-(r - 6)}" y1="0" x2="${r - 6}" y2="0" stroke="${C.s500}" stroke-width="3" transform="rotate(${a})"/>`).join('')}
    <circle r="${r - 13}" fill="${C.s900}"/>
  </g>`);
}

function star(x, y, r, on) {
  const pts = [];
  for (let i = 0; i < 10; i++) {
    const a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 ? r * 0.48 : r;
    pts.push(`${(x + Math.cos(a) * rr).toFixed(1)},${(y + Math.sin(a) * rr).toFixed(1)}`);
  }
  const poly = pts.join(' ');
  if (on) {
    p(`<polygon points="${poly}" fill="${C.b500}" opacity="0.5" filter="url(#blur6)"/>
       <polygon points="${poly}" fill="${C.b700}" transform="translate(0,3)" stroke="${C.b700}" stroke-width="4" stroke-linejoin="round"/>
       <polygon points="${poly}" fill="url(#brass)" stroke="${C.b300}" stroke-width="4" stroke-linejoin="round"/>
       <ellipse cx="${x - r * 0.22}" cy="${y - r * 0.28}" rx="${r * 0.22}" ry="${r * 0.12}" fill="${C.b100}" opacity="0.8" transform="rotate(-25 ${x} ${y})"/>`);
  } else {
    p(`<polygon points="${poly}" fill="${C.raised}" stroke="${C.overlay}" stroke-width="4" stroke-linejoin="round"/>`);
  }
}

function drop(x, y, s, o = 1) {
  const d = 'M0,-18 C6,-6 10,0 10,6 A10,10 0 1,1 -10,6 C-10,0 -6,-6 0,-18Z';
  p(`<g transform="translate(${x},${y}) scale(${s})" opacity="${o}">
    <path d="${d}" fill="${C.lglow}" opacity="0.5" filter="url(#blur6)"/>
    <path d="${d}" fill="url(#liquidV)"/>
    <circle cx="-3.5" cy="4" r="2.2" fill="${C.lglow}" opacity="0.9"/>
  </g>`);
}

// ---------------- scene ----------------
p(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.base}"/><stop offset="0.5" stop-color="${C.deep}"/><stop offset="1" stop-color="${C.abyss}"/></linearGradient>
  <linearGradient id="plate" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.base}"/><stop offset="1" stop-color="${C.deep}"/></linearGradient>
  <linearGradient id="liquidV" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.l300}"/><stop offset="0.5" stop-color="${C.l500}"/><stop offset="1" stop-color="${C.l700}"/></linearGradient>
  <linearGradient id="title" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.lglow}"/><stop offset="0.55" stop-color="${C.l300}"/><stop offset="1" stop-color="${C.l500}"/></linearGradient>
  <linearGradient id="brass" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b500}"/></linearGradient>
  <radialGradient id="brassR" cx="0.35" cy="0.3" r="0.8"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b700}"/></radialGradient>
  <radialGradient id="boardGlow" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="${C.l500}" stop-opacity="0.20"/><stop offset="1" stop-color="${C.l500}" stop-opacity="0"/></radialGradient>
  <radialGradient id="titleGlow" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="${C.l500}" stop-opacity="0.16"/><stop offset="1" stop-color="${C.l500}" stop-opacity="0"/></radialGradient>
  <radialGradient id="edgeDark" cx="0.5" cy="0.5" r="0.8"><stop offset="0.6" stop-color="${C.abyss}" stop-opacity="0"/><stop offset="1" stop-color="#03080E" stop-opacity="0.75"/></radialGradient>
  <filter id="blur4" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="4"/></filter>
  <filter id="blur6" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="6"/></filter>
  <filter id="blur10" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="10"/></filter>
  <filter id="blur14" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="14"/></filter>
</defs>
<rect width="${W}" height="${H}" fill="url(#bg)"/>`);

// faint workshop grid
let grid = '';
for (let x = 32; x < W; x += 64) grid += `<path d="M${x},0 V${H}"/>`;
for (let y = 26; y < H; y += 64) grid += `<path d="M0,${y} H${W}"/>`;
p(`<g stroke="#ffffff" stroke-opacity="0.035" stroke-width="1.5">${grid}</g>`);

// dim background pipes along the edges
steel([[-40, 440], [300, 440], [300, 540]], 26, 30);
steel([[160, -40], [160, 40], [-40, 40]], 22, 26);
p(`<rect x="0" y="0" width="${W}" height="${H}" fill="${C.deep}" opacity="0.45"/>`);

// glows behind title and board
p(`<ellipse cx="250" cy="230" rx="300" ry="190" fill="url(#titleGlow)"/>`);
p(`<ellipse cx="${BX + COLS * CELL / 2}" cy="${BY + ROWS * CELL / 2}" rx="330" ry="250" fill="url(#boardGlow)"/>`);

// board plate
const PAD = 16;
const px = BX - PAD, py = BY - PAD, pw = COLS * CELL + 2 * PAD, ph = ROWS * CELL + 2 * PAD;
p(`<rect x="${px + 6}" y="${py + 12}" width="${pw}" height="${ph}" rx="20" fill="#03080E" opacity="0.6" filter="url(#blur14)"/>
   <rect x="${px}" y="${py}" width="${pw}" height="${ph}" rx="20" fill="url(#plate)"/>
   <rect x="${px}" y="${py}" width="${pw}" height="${ph}" rx="20" fill="none" stroke="#ffffff" stroke-opacity="0.08" stroke-width="2"/>
   <path d="M${px + 20},${py + 1} H${px + pw - 20}" stroke="#ffffff" stroke-opacity="0.2" stroke-width="2" stroke-linecap="round"/>`);
[[px + 9, py + 9], [px + pw - 9, py + 9], [px + 9, py + ph - 9], [px + pw - 9, py + ph - 9]].forEach(([a, b]) => rivet(a, b, 3));

// cells
const blocked = new Set(['1,0', '6,3', '0,4']);
for (let r = 0; r < ROWS; r++) for (let c = 0; c < COLS; c++) {
  const x = BX + c * CELL + 3, y = BY + r * CELL + 3, s = CELL - 6;
  if (blocked.has(`${c},${r}`)) {
    p(`<rect x="${x}" y="${y}" width="${s}" height="${s}" rx="10" fill="${C.s700}"/>
       <rect x="${x + 2}" y="${y + 2}" width="${s - 4}" height="${s - 4}" rx="8" fill="${C.s900}"/>
       <path d="M${x + 12},${y + s - 12} L${x + s - 12},${y + 12} M${x + 12},${y + 12} L${x + s - 12},${y + s - 12}" stroke="${C.s700}" stroke-width="5" stroke-linecap="round"/>`);
  } else {
    p(`<rect x="${x}" y="${y}" width="${s}" height="${s}" rx="10" fill="${C.overlay}" opacity="0.55"/>
       <path d="M${x + 10},${y + 0.8} H${x + s - 10}" stroke="#ffffff" stroke-opacity="0.12" stroke-width="1.5" stroke-linecap="round"/>`);
  }
}

// The route: start (0,2) right through a cross at (3,2), down, back left, up through the same
// cross vertically, and on to the finish at (6,0). The liquid is part of the way along.
const R = [[cx(0), cy(2)], [cx(5), cy(2)], [cx(5), cy(4)], [cx(3), cy(4)], [cx(3), cy(0)], [cx(6), cy(0)]];
const RADIUS = 30;
steel(R, PIPE_W, RADIUS);
// cross hub at (3,2)
p(`<rect x="${cx(3) - 20}" y="${cy(2) - 20}" width="40" height="40" rx="8" fill="${C.s700}"/>
   <rect x="${cx(3) - 17}" y="${cy(2) - 18}" width="34" height="34" rx="6" fill="${C.s500}"/>
   <path d="M${cx(3) - 9},${cy(2) - 16.5} H${cx(3) + 9}" stroke="${C.s100}" stroke-opacity="0.5" stroke-width="2" stroke-linecap="round"/>
   <path d="M${cx(3) - 22},${cy(2)} H${cx(3) + 22} M${cx(3)},${cy(2) - 22} V${cy(2) + 22}" stroke="${C.s900}" stroke-width="${PIPE_W * 0.46}"/>`);
// flanges between cells
const flange = (x, y, vertical) => {
  const rw = vertical ? PIPE_W + 8 : 9, rh = vertical ? 9 : PIPE_W + 8;
  p(`<rect x="${x - rw / 2}" y="${y - rh / 2}" width="${rw}" height="${rh}" rx="2.5" fill="${C.s700}"/><rect x="${x - rw / 2 + 1.5}" y="${y - rh / 2 + 1.5}" width="${rw - 3}" height="${rh - 3}" rx="2" fill="${C.s300}"/>`);
};
[1, 2, 4].forEach(c => flange(BX + c * CELL, cy(2), false));
flange(BX + 3 * CELL, cy(2), false); flange(BX + 4 * CELL, cy(2), false);
flange(cx(5), BY + 3 * CELL, true); flange(cx(5), BY + 4 * CELL, true);
flange(BX + 4 * CELL, cy(4), false);
flange(cx(3), BY + 4 * CELL, true); flange(cx(3), BY + 1 * CELL, true);
flange(BX + 4 * CELL, cy(0), false); flange(BX + 5 * CELL, cy(0), false); flange(BX + 6 * CELL, cy(0), false);

// liquid: horizontal through the cross, round the loop, and climbing back up towards it
const filled = truncate(R, 0.62);
liquid(filled, PIPE_W, RADIUS);
const [hx, hy] = filled[filled.length - 1];
p(`<circle cx="${hx}" cy="${hy}" r="14" fill="${C.lglow}" opacity="0.75" filter="url(#blur6)"/>
   <circle cx="${hx}" cy="${hy + 1}" r="6" fill="${C.lglow}"/>
   <circle cx="${hx - 1}" cy="${hy + 20}" r="2.2" fill="${C.lglow}" opacity="0.8"/><circle cx="${hx + 1.5}" cy="${hy + 36}" r="1.6" fill="${C.lglow}" opacity="0.7"/>`);

valve(cx(0), cy(2), 21);
drain(cx(6), cy(0), 21);

// the next section, being dragged in from the stack: a brass-framed corner tile
const tx = cx(6) + 4, ty = cy(4) - 8;
p(`<g transform="translate(${tx},${ty}) rotate(-8)">
  <rect x="-34" y="-30" width="68" height="68" rx="12" fill="#03080E" opacity="0.6" filter="url(#blur10)"/>
  <rect x="-34" y="-34" width="68" height="68" rx="12" fill="${C.b500}" opacity="0.5" filter="url(#blur10)"/>
  <rect x="-32" y="-32" width="64" height="64" rx="11" fill="${C.raised}" stroke="${C.b500}" stroke-width="3.5"/>`);
{
  const d = 'M-32,0 L-14,0 Q0,0 0,-14 L0,-32';
  p(stroke(d, C.s700, PIPE_W) + stroke(d, C.s500, PIPE_W - 6, 'transform="translate(-1,-2)"') + stroke(d, C.s900, PIPE_W * 0.46));
}
p(`</g>`);

// ---------------- title ----------------
const TX = 64;
p(`<text x="${TX + 3}" y="214" font-family="Wendy One" font-size="88" fill="${C.l900}" opacity="0.9">LEAK OUT</text>
   <text x="${TX}" y="208" font-family="Wendy One" font-size="88" fill="${C.lglow}" opacity="0.35" filter="url(#blur10)">LEAK OUT</text>
   <text x="${TX}" y="208" font-family="Wendy One" font-size="88" fill="url(#title)">LEAK OUT</text>`);
// a drip falling from the title
drop(TX + 268, 232, 0.62);

p(`<text x="${TX + 2}" y="276" font-family="Fredoka" font-weight="600" font-size="31" fill="${C.text}">Race the flow. Stop the leak.</text>`);
p(`<text x="${TX + 2}" y="314" font-family="Fredoka" font-weight="500" font-size="19" fill="${C.text2}">Build the pipeline before the liquid catches up.</text>`);

[0, 1, 2].forEach(i => star(TX + 26 + i * 54, 372, 20, true));

p(`<rect width="${W}" height="${H}" fill="url(#edgeDark)"/>`);

// bokeh bubbles
[[470, 70, 4], [990, 420, 5], [40, 300, 3], [500, 440, 6], [860, 30, 3.5], [420, 330, 2.5], [1000, 120, 3]]
  .forEach(([x, y, r]) => p(`<circle cx="${x}" cy="${y}" r="${r * 2.2}" fill="${C.lglow}" opacity="0.10" filter="url(#blur6)"/><circle cx="${x}" cy="${y}" r="${r}" fill="none" stroke="${C.lglow}" stroke-opacity="0.35" stroke-width="1.2"/><circle cx="${x - r * 0.35}" cy="${y - r * 0.35}" r="${r * 0.25}" fill="${C.lglow}" opacity="0.6"/>`));

p(`</svg>`);
const svg = out.join('\n');
fs.writeFileSync(path.join(__dirname, 'feature-graphic.svg'), svg);
console.log('wrote feature-graphic.svg', svg.length, 'bytes');

// ---------------- rasterise ----------------
const fontDir = path.join(__dirname, '..', '..', 'src', 'app', 'src', 'main', 'res', 'font');
const font = (family, file) => `@font-face{font-family:'${family}';src:url(data:font/ttf;base64,${fs.readFileSync(path.join(fontDir, file)).toString('base64')}) format('truetype');font-weight:100 900;}`;
const pw_ = require(path.join(process.env.APPDATA, 'npm', 'node_modules', 'playwright'));
(async () => {
  const browser = await pw_.chromium.launch();
  const page = await browser.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: 1 });
  await page.setContent(`<!doctype html><html><head><style>${font('Wendy One', 'wendy_one_regular.ttf')}${font('Fredoka', 'fredoka.ttf')}
    html,body{margin:0;background:${C.abyss}}svg{display:block}</style></head><body>${svg}</body></html>`);
  await page.evaluate(() => document.fonts.ready);
  const file = path.join(__dirname, 'feature-graphic-1024x500.png');
  await page.screenshot({ path: file, clip: { x: 0, y: 0, width: W, height: H } });
  console.log('wrote', file, fs.statSync(file).size, 'bytes');
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
