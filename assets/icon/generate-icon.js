// Generates the Leak Out app icon as SVG (Deep Flow design system v2).
//
//   node generate-icon.js            -> writes app-icon.svg, adaptive-foreground.svg, adaptive-background.svg
//   node render-icon.js              -> rasterises them to PNG with Playwright's Chromium
//
// Canvas is 1024x1024. Android adaptive icons are 108 dp, of which the outer
// 72 dp may be shown and the inner 66 dp is always safe. On this canvas that is
// a safe circle of radius 313 around the centre. Everything that carries
// meaning (valve wheel, elbow, leak drops, liquid head) sits inside it; only
// the pipe ends run off the edge, which is intended.
const fs = require('fs');
const path = require('path');
const S = 1024, CX = 512, CY = 512;
const C = {
  abyss:'#07111C', deep:'#0C1A2A', base:'#122538', raised:'#19324A', overlay:'#21405C',
  lglow:'#9DF3FF', l300:'#5EE7FF', l500:'#19C6F2', l700:'#0A8DC2', l900:'#065A82',
  b100:'#FFE9B0', b300:'#FFCF5C', b500:'#F5A623', b700:'#C97A12', b900:'#7A4708',
  s100:'#C9D6E2', s300:'#8FA3B8', s500:'#5C7189', s700:'#3A4B60', s900:'#22303F',
};

let out;
const p = s => out.push(s);

// polyline -> path with rounded elbows (same helper as the welcome background)
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

function pipe({pts, w, fill=1, radius}) {
  const d = route(pts, radius ?? w*1.2);
  const wl = w*0.36, wb = w*0.46;          // liquid width, bore width
  const k = w / 64;                        // scale of offsets relative to the background art
  const st = (color, width, extra='') => `<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="butt" stroke-linejoin="round" ${extra}/>`;
  let s = '';
  s += st(C.abyss, w+24*k, `opacity="0.6" transform="translate(${14*k},${24*k})" filter="url(#blur20)"`);
  s += st(C.s700, w);
  s += st(C.s500, w-16*k, `transform="translate(${-3*k},${-5*k})"`);
  s += st(C.s100, 6*k, `opacity="0.5" transform="translate(${-(w/2)+16*k},${-(w/2)+16*k})"`);
  s += st(C.s900, wb);
  const dash = fill < 1 ? `pathLength="1" stroke-dasharray="${fill} 1"` : '';
  s += st(C.lglow, wl+14*k, `opacity="0.4" filter="url(#blur24)" ${dash}`);
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
    <rect x="${-rw/2+3*k}" y="${-rh/2+5*k}" width="${rw}" height="${rh}" rx="${4*k}" fill="${C.abyss}" opacity="0.5" filter="url(#blur8)"/>
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
    <circle r="${r+14*k}" fill="${C.b500}" opacity="0.45" filter="url(#blur24)"/>
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
    <path d="${d}" fill="${C.lglow}" opacity="0.5" filter="url(#blur8)"/>
    <path d="${d}" fill="url(#liquidV)"/>
    <circle cx="-3.5" cy="4" r="2.2" fill="${C.lglow}" opacity="0.9"/>
  </g>`);
}

function defs() {
  return `<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.base}"/><stop offset="0.5" stop-color="${C.deep}"/><stop offset="1" stop-color="${C.abyss}"/></linearGradient>
  <linearGradient id="liquidV" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.l300}"/><stop offset="0.5" stop-color="${C.l500}"/><stop offset="1" stop-color="${C.l700}"/></linearGradient>
  <linearGradient id="brass" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b500}"/></linearGradient>
  <radialGradient id="brassR" cx="0.35" cy="0.3" r="0.8"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b700}"/></radialGradient>
  <radialGradient id="floorGlow" cx="0.5" cy="0.95" r="0.75"><stop offset="0" stop-color="${C.l500}" stop-opacity="0.28"/><stop offset="1" stop-color="${C.l500}" stop-opacity="0"/></radialGradient>
  <radialGradient id="edgeDark" cx="0.5" cy="0.5" r="0.72"><stop offset="0.55" stop-color="${C.abyss}" stop-opacity="0"/><stop offset="1" stop-color="#03080E" stop-opacity="0.75"/></radialGradient>
  <filter id="blur8" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="8"/></filter>
  <filter id="blur20" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="20"/></filter>
  <filter id="blur24" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="24"/></filter>
  <clipPath id="squircle"><rect width="${S}" height="${S}" rx="228"/></clipPath>
</defs>`;
}

function background(rounded) {
  const rx = rounded ? 228 : 0;
  p(`<rect width="${S}" height="${S}" rx="${rx}" fill="url(#bg)"/>`);
  // faint board grid so the plate reads as the game board
  let g = '';
  for (let i = 1; i < 6; i++) {
    const t = Math.round(i * S / 6);
    g += `<path d="M${t},0 V${S}" /><path d="M0,${t} H${S}" />`;
  }
  p(`<g stroke="#ffffff" stroke-opacity="0.045" stroke-width="3" clip-path="url(#squircle)">${g}</g>`);
  p(`<rect width="${S}" height="${S}" rx="${rx}" fill="url(#floorGlow)"/>`);
  p(`<rect width="${S}" height="${S}" rx="${rx}" fill="url(#edgeDark)"/>`);
  if (rounded) {
    p(`<path d="M${rx},2 H${S-rx}" stroke="#ffffff" stroke-opacity="0.18" stroke-width="3" stroke-linecap="round"/>`);
    p(`<rect x="1.5" y="1.5" width="${S-3}" height="${S-3}" rx="${rx-1.5}" fill="none" stroke="#ffffff" stroke-opacity="0.08" stroke-width="3"/>`);
  }
}

// The subject, zoomed in on what matters: liquid enters from the left through a
// brass valve, rounds an elbow and heads down and out of frame. The elbow is
// leaking - that is the game. The parts are drawn large and the fine detail
// (second flange, trailing bubbles, small drops) is left out, so the valve,
// the bend and the leak still read on a 48 dp launcher icon.
function subject() {
  const w = 220;
  const ex = 640, ey = 400, br = 200;      // elbow corner and bend radius
  const hy = 730;                          // liquid head, a little down the vertical leg
  // dash fraction that puts the end of the liquid at the head
  const bend = bezierLength([ex-br, ey], [ex, ey], [ex, ey+br]);
  const total = (ex-br+80) + bend + (S+80-ey-br);
  const fill = ((ex-br+80) + bend + (hy-ey-br)) / total;
  pipe({pts:[[-80, ey], [ex, ey], [ex, S+80]], w, fill, radius:br});
  flange(70, ey, false, w);
  valve(345, ey, 135);

  // liquid head
  p(`<circle cx="${ex}" cy="${hy}" r="58" fill="${C.lglow}" opacity="0.7" filter="url(#blur24)"/>
     <circle cx="${ex-3}" cy="${hy-6}" r="38" fill="${C.lglow}"/>`);

  // the leak: spray from the outer bend of the elbow, up and to the right
  const lx = ex + 20, ly = ey - 26;
  p(`<circle cx="${lx-6}" cy="${ly+8}" r="52" fill="${C.lglow}" opacity="0.65" filter="url(#blur24)"/>
     <path d="M${lx-20},${ly+20} L${lx+26},${ly-26}" stroke="${C.l300}" stroke-width="22" stroke-linecap="round"/>
     <path d="M${lx-16},${ly+12} L${lx+18},${ly-22}" stroke="${C.lglow}" stroke-width="8" stroke-linecap="round"/>`);
  drop(lx+78,  ly-80, 3.4, 40, 1);
  drop(lx+128, ly-10, 2.6, 66, 0.95);
  drop(lx+30,  ly-132, 2.2, 22, 0.85);
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

function svgOpen() {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${S} ${S}" width="${S}" height="${S}">\n${defs()}`;
}

function write(name, body) {
  out = [];
  p(svgOpen());
  body();
  p(`</svg>`);
  const file = path.join(__dirname, name);
  fs.writeFileSync(file, out.join('\n'));
  console.log('wrote', file, out.join('\n').length, 'bytes');
}

// Launchers show only the central 72 of 108 dp of an adaptive icon. The full icon (Play Store
// listing) shows the whole plate, so there the subject is scaled up by about 108/72 to match the launcher.
const zoom = (z, body) => { p(`<g transform="translate(${CX},${CY}) scale(${z}) translate(${-CX},${-CY})">`); body(); p(`</g>`); };

// Full icon: rounded plate + subject. Use for the Play Store listing (512 px).
write('app-icon.svg', () => { background(true); p(`<g clip-path="url(#squircle)">`); zoom(1.3, subject); p(`</g>`); });
// Adaptive layers: background is the plate edge-to-edge, foreground is the subject on transparency.
write('adaptive-background.svg', () => background(false));
write('adaptive-foreground.svg', () => zoom(0.86, subject));
