// Generates the Google Play feature graphic (1024x500) in the style of the app icon (Deep Flow design system v2).
//
//   node generate-feature-graphic.js   -> writes feature-graphic.svg and feature-graphic-1024x500.png
//
// The logo and tagline sit on the left; on the right a pipe runs in through a brass valve, climbs,
// turns and heads down out of frame, leaking at the top elbow. The fonts are embedded in the SVG so
// it renders the same anywhere. The PNG is opaque (Play rejects feature graphics with alpha) and is
// rasterised with the globally installed Playwright Chromium, like render-icon.js.
const fs = require('fs');
const path = require('path');
const W = 1024, H = 500;
const C = {
  abyss:'#07111C', deep:'#0C1A2A', base:'#122538', raised:'#19324A', overlay:'#21405C',
  lglow:'#9DF3FF', l300:'#5EE7FF', l500:'#19C6F2', l700:'#0A8DC2', l900:'#065A82',
  b100:'#FFE9B0', b300:'#FFCF5C', b500:'#F5A623', b700:'#C97A12', b900:'#7A4708',
  s100:'#C9D6E2', s300:'#8FA3B8', s500:'#5C7189', s700:'#3A4B60', s900:'#22303F',
  text:'#F2F7FB', text2:'#A9BACB',
};
const fontDir = path.join(__dirname, '..', '..', 'src', 'app', 'src', 'main', 'res', 'font');
const font = file => `data:font/ttf;base64,${fs.readFileSync(path.join(fontDir, file)).toString('base64')}`;

const out = [];
const p = s => out.push(s);

// polyline -> path with rounded elbows (same helper as the icon)
function route(pts, r) {
  let d = `M${pts[0][0]},${pts[0][1]}`;
  for (let i = 1; i < pts.length - 1; i++) {
    const [x0,y0] = pts[i-1], [x1,y1] = pts[i], [x2,y2] = pts[i+1];
    const a = Math.atan2(y0-y1, x0-x1), b = Math.atan2(y2-y1, x2-x1);
    d += ` L${x1+Math.cos(a)*r},${y1+Math.sin(a)*r}`;
    d += ` Q${x1},${y1} ${x1+Math.cos(b)*r},${y1+Math.sin(b)*r}`;
  }
  const [xe,ye] = pts[pts.length-1];
  return d + ` L${xe},${ye}`;
}

function bezierLength(a, c, b) {
  let len = 0, [px, py] = a;
  for (let i = 1; i <= 200; i++) {
    const t = i/200, u = 1-t;
    const x = u*u*a[0] + 2*u*t*c[0] + t*t*b[0], y = u*u*a[1] + 2*u*t*c[1] + t*t*b[1];
    len += Math.hypot(x-px, y-py); px = x; py = y;
  }
  return len;
}

// length of route(pts, r), optionally only up to `until` on the last segment
function routeLength(pts, r, until) {
  let len = 0, from = pts[0];
  for (let i = 1; i < pts.length; i++) {
    const end = i === pts.length - 1 ? (until ?? pts[i]) : pts[i];
    if (i < pts.length - 1) {
      const [x0,y0] = pts[i-1], [x1,y1] = pts[i], [x2,y2] = pts[i+1];
      const a = Math.atan2(y0-y1, x0-x1), b = Math.atan2(y2-y1, x2-x1);
      const inn = [x1+Math.cos(a)*r, y1+Math.sin(a)*r], outp = [x1+Math.cos(b)*r, y1+Math.sin(b)*r];
      len += Math.hypot(inn[0]-from[0], inn[1]-from[1]) + bezierLength(inn, pts[i], outp);
      from = outp;
    } else {
      len += Math.hypot(end[0]-from[0], end[1]-from[1]);
    }
  }
  return len;
}

function pipe({pts, w, fill=1, radius}) {
  const d = route(pts, radius);
  const wl = w*0.36, wb = w*0.46;          // liquid width, bore width
  const k = w / 64;
  const st = (color, width, extra='') => `<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="butt" stroke-linejoin="round" ${extra}/>`;
  let s = '';
  s += st(C.abyss, w+24*k, `opacity="0.6" transform="translate(${14*k},${24*k})" filter="url(#blur14)"`);
  s += st(C.s700, w);
  s += st(C.s500, w-16*k, `transform="translate(${-3*k},${-5*k})"`);
  s += st(C.s100, 6*k, `opacity="0.5" transform="translate(${-(w/2)+16*k},${-(w/2)+16*k})"`);
  s += st(C.s900, wb);
  const dash = fill < 1 ? `pathLength="1" stroke-dasharray="${fill} 1"` : '';
  s += st(C.lglow, wl+14*k, `opacity="0.4" filter="url(#blur14)" ${dash}`);
  s += st(C.l700, wl, dash);
  s += st(C.l500, wl-8*k, `transform="translate(${-2*k},${-4*k})" ${dash}`);
  s += st(C.l300, 6*k, `opacity="0.85" transform="translate(${-(wl/2)+8*k},${-(wl/2)+8*k})" ${dash}`);
  p(`<g>${s}</g>`);
}

function flange(x, y, vertical, w) {
  const k = w / 64;
  const L = 22*k, Wd = w + 14*k;
  const rw = vertical ? Wd : L, rh = vertical ? L : Wd;
  const bolt = (cx, cy) => `<circle cx="${cx}" cy="${cy}" r="${3.5*k}" fill="${C.s700}"/><circle cx="${cx-1*k}" cy="${cy-1*k}" r="${1.6*k}" fill="${C.s100}" opacity="0.7"/>`;
  const bolts = [-1,1].map(q => vertical ? bolt(q*(rw/2-9*k), 0) : bolt(0, q*(rh/2-9*k))).join('');
  p(`<g transform="translate(${x},${y})">
    <rect x="${-rw/2+3*k}" y="${-rh/2+5*k}" width="${rw}" height="${rh}" rx="${4*k}" fill="${C.abyss}" opacity="0.5" filter="url(#blur6)"/>
    <rect x="${-rw/2}" y="${-rh/2}" width="${rw}" height="${rh}" rx="${4*k}" fill="${C.s700}"/>
    <rect x="${-rw/2+2*k}" y="${-rh/2+2*k}" width="${rw-4*k}" height="${rh-4*k}" rx="${3*k}" fill="${C.s300}"/>
    <rect x="${-rw/2+2*k}" y="${-rh/2+2*k}" width="${vertical? rw-4*k : 3*k}" height="${vertical? 3*k : rh-4*k}" fill="${C.s100}" opacity="0.6"/>
    ${bolts}
  </g>`);
}

// Brass valve wheel: the single warm accent ("brass means touch me").
function valve(x, y, r) {
  const k = r / 56;
  const spokes = [0,45,90,135].map(a =>
    `<line x1="${-r}" y1="0" x2="${r}" y2="0" stroke="${C.b700}" stroke-width="${10*k}" stroke-linecap="round" transform="rotate(${a})"/>` +
    `<line x1="${-r+4*k}" y1="${-1*k}" x2="${r-4*k}" y2="${-1*k}" stroke="${C.b500}" stroke-width="${6*k}" stroke-linecap="round" transform="rotate(${a})"/>`).join('');
  p(`<g transform="translate(${x},${y})">
    <circle r="${r+14*k}" fill="${C.b500}" opacity="0.45" filter="url(#blur14)"/>
    <circle r="${r+4*k}" fill="${C.b900}" opacity="0.75" transform="translate(${4*k},${7*k})"/>
    <circle r="${r}" fill="none" stroke="${C.b700}" stroke-width="${12*k}"/>
    <circle r="${r}" fill="none" stroke="url(#brass)" stroke-width="${9*k}"/>
    <circle r="${r-4*k}" fill="none" stroke="${C.b100}" stroke-opacity="0.35" stroke-width="${2*k}"/>
    ${spokes}
    <circle r="${r*0.32}" fill="${C.b700}"/><circle r="${r*0.26}" fill="url(#brassR)"/>
    <circle cx="${-r*0.08}" cy="${-r*0.1}" r="${r*0.09}" fill="${C.b100}" opacity="0.9"/>
  </g>`);
}

function drop(x, y, s, rot=0, o=1) {
  const d = 'M0,-18 C6,-6 10,0 10,6 A10,10 0 1,1 -10,6 C-10,0 -6,-6 0,-18Z';
  p(`<g transform="translate(${x},${y}) rotate(${rot}) scale(${s})" opacity="${o}">
    <path d="${d}" fill="${C.lglow}" opacity="0.5" filter="url(#blur6)"/>
    <path d="${d}" fill="url(#liquidV)"/>
    <circle cx="-3.5" cy="4" r="2.2" fill="${C.lglow}" opacity="0.9"/>
  </g>`);
}

function defs() {
  return `<defs>
  <style>
    @font-face { font-family: 'Wendy One'; src: url(${font('wendy_one_regular.ttf')}) format('truetype'); }
    @font-face { font-family: 'Fredoka'; src: url(${font('fredoka.ttf')}) format('truetype'); }
  </style>
  <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.base}"/><stop offset="0.5" stop-color="${C.deep}"/><stop offset="1" stop-color="${C.abyss}"/></linearGradient>
  <linearGradient id="liquidV" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.l300}"/><stop offset="0.5" stop-color="${C.l500}"/><stop offset="1" stop-color="${C.l700}"/></linearGradient>
  <linearGradient id="liquidT" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.lglow}"/><stop offset="0.55" stop-color="${C.l300}"/><stop offset="1" stop-color="${C.l500}"/></linearGradient>
  <linearGradient id="brass" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b500}"/></linearGradient>
  <radialGradient id="brassR" cx="0.35" cy="0.3" r="0.8"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b700}"/></radialGradient>
  <radialGradient id="floorGlow" cx="0.7" cy="1" r="0.8"><stop offset="0" stop-color="${C.l500}" stop-opacity="0.24"/><stop offset="1" stop-color="${C.l500}" stop-opacity="0"/></radialGradient>
  <radialGradient id="edgeDark" cx="0.5" cy="0.5" r="0.75"><stop offset="0.6" stop-color="${C.abyss}" stop-opacity="0"/><stop offset="1" stop-color="#03080E" stop-opacity="0.7"/></radialGradient>
  <filter id="blur6" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="6"/></filter>
  <filter id="blur14" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="14"/></filter>
  <filter id="textShadow" x="-10%" y="-20%" width="120%" height="150%"><feDropShadow dx="0" dy="6" stdDeviation="6" flood-color="#03080E" flood-opacity="0.7"/></filter>
</defs>`;
}

function background() {
  p(`<rect width="${W}" height="${H}" fill="url(#bg)"/>`);
  // faint board grid so the plate reads as the game board
  const cell = 64;
  let g = '';
  for (let x = cell; x < W; x += cell) g += `<path d="M${x},0 V${H}"/>`;
  for (let y = H - cell; y > 0; y -= cell) g += `<path d="M0,${y} H${W}"/>`;
  p(`<g stroke="#ffffff" stroke-opacity="0.045" stroke-width="2">${g}</g>`);
  p(`<rect width="${W}" height="${H}" fill="url(#floorGlow)"/>`);
  p(`<rect width="${W}" height="${H}" fill="url(#edgeDark)"/>`);
}

// Liquid enters from the left through the valve, climbs behind the logo's right edge, crosses the
// top and drops out of frame. The top-right elbow is leaking.
function subject() {
  const w = 92, r = 80;
  const lowY = 412, riseX = 640, topY = 150, dropX = 884;
  const pts = [[-60, lowY], [riseX, lowY], [riseX, topY], [dropX, topY], [dropX, H + 60]];
  const headY = 352;
  const fill = routeLength(pts, r, [dropX, headY]) / routeLength(pts, r);
  pipe({pts, w, fill, radius: r});
  flange(96, lowY, false, w);
  flange(riseX, 290, true, w);
  valve(372, lowY, 62);

  // liquid head
  p(`<circle cx="${dropX}" cy="${headY}" r="30" fill="${C.lglow}" opacity="0.7" filter="url(#blur14)"/>
     <circle cx="${dropX-1}" cy="${headY-3}" r="17" fill="${C.lglow}"/>`);

  // the leak: spray from the outer bend of the elbow, up and to the right
  const lx = dropX - 4, ly = topY - 12;
  p(`<circle cx="${lx-4}" cy="${ly+4}" r="28" fill="${C.lglow}" opacity="0.65" filter="url(#blur14)"/>
     <path d="M${lx-10},${ly+10} L${lx+14},${ly-14}" stroke="${C.l300}" stroke-width="11" stroke-linecap="round"/>
     <path d="M${lx-8},${ly+6} L${lx+9},${ly-11}" stroke="${C.lglow}" stroke-width="4" stroke-linecap="round"/>`);
  drop(lx+42, ly-44, 1.7, 40, 1);
  drop(lx+72, ly-6, 1.3, 66, 0.95);
  drop(lx+14, ly-72, 1.1, 22, 0.85);
  drop(lx+100, ly-58, 0.8, 50, 0.7);
}

function logo() {
  const x = 60;
  p(`<g filter="url(#textShadow)">
    <text x="${x}" y="200" font-family="Wendy One" font-size="118" letter-spacing="1" fill="${C.text}">Leak
      <tspan fill="url(#liquidT)">Out</tspan></text>
  </g>`);
  p(`<text x="${x+4}" y="262" font-family="Fredoka" font-weight="500" font-size="34" fill="${C.text2}">Connect the pipes before it leaks!</text>`);
}

p(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">\n${defs()}`);
background();
subject();
logo();
p(`</svg>`);

const svg = out.join('\n');
const svgFile = path.join(__dirname, 'feature-graphic.svg');
fs.writeFileSync(svgFile, svg);
console.log('wrote', path.basename(svgFile), svg.length, 'bytes');

(async () => {
  const pw = require(path.join(process.env.APPDATA, 'npm', 'node_modules', 'playwright'));
  const browser = await pw.chromium.launch();
  const page = await browser.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: 1 });
  await page.setContent(`<!doctype html><html><body style="margin:0;background:${C.abyss}">${svg}</body></html>`);
  await page.evaluate(() => document.fonts.ready);
  const file = path.join(__dirname, 'feature-graphic-1024x500.png');
  await page.screenshot({ path: file, clip: { x: 0, y: 0, width: W, height: H } });
  console.log('wrote', path.basename(file), fs.statSync(file).size, 'bytes');
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
