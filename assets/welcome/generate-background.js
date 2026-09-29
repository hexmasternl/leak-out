// Generates the Leak Out welcome background as SVG (Deep Flow design system v2).
const fs = require('fs');
const OUT = process.argv[2];
const W = 1920, H = 1080;
const C = {
  abyss:'#07111C', deep:'#0C1A2A', base:'#122538', raised:'#19324A', overlay:'#21405C',
  lglow:'#9DF3FF', l300:'#5EE7FF', l500:'#19C6F2', l700:'#0A8DC2', l900:'#065A82',
  b100:'#FFE9B0', b300:'#FFCF5C', b500:'#F5A623', b700:'#C97A12', b900:'#7A4708',
  s100:'#C9D6E2', s300:'#8FA3B8', s500:'#5C7189', s700:'#3A4B60', s900:'#22303F',
  r300:'#C7B5FF', r500:'#A78BFA', r700:'#7C5CE6',
};
let out = [];
const p = s => out.push(s);

// polyline -> path with rounded elbows
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

function pipe({pts, w, liquid=true, fill=1, dim=1, radius}) {
  const d = route(pts, radius ?? w*1.2);
  const wl = w*0.36, wb = w*0.46;
  let s = '';
  const st = (color, width, extra='') => `<path d="${d}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="butt" stroke-linejoin="round" ${extra}/>`;
  s += st(C.abyss, w+12, `opacity="0.55" transform="translate(8,14)" filter="url(#blur8)"`);
  s += st(C.s700, w);
  s += st(C.s500, w-8, `transform="translate(-2,-3)"`);
  s += st(C.s100, 3, `opacity="0.5" transform="translate(${-(w/2)+9},${-(w/2)+9})"`);
  s += st(C.s900, wb);
  if (liquid) {
    const dash = fill < 1 ? `pathLength="1" stroke-dasharray="${fill} 1"` : '';
    s += st(C.lglow, wl+14, `opacity="0.5" filter="url(#blur12)" ${dash}`);
    s += st(C.l700, wl, dash);
    s += st(C.l500, wl-5, `transform="translate(-1,-2)" ${dash}`);
    s += st(C.l300, 3, `opacity="0.85" transform="translate(${-(wl/2)+4},${-(wl/2)+4})" ${dash}`);
  }
  p(`<g opacity="${dim}">${s}</g>`);
}

function flange(x, y, vertical, w, dim=1) {
  const L = 22, Wd = w + 14;
  const rw = vertical ? Wd : L, rh = vertical ? L : Wd;
  const bolts = [-1,1].map(k => vertical
      ? `<circle cx="${k*(rw/2-9)}" cy="0" r="3.5" fill="${C.s700}"/><circle cx="${k*(rw/2-9)-1}" cy="-1" r="1.6" fill="${C.s100}" opacity="0.7"/>`
      : `<circle cx="0" cy="${k*(rh/2-9)}" r="3.5" fill="${C.s700}"/><circle cx="-1" cy="${k*(rh/2-9)-1}" r="1.6" fill="${C.s100}" opacity="0.7"/>`).join('');
  p(`<g opacity="${dim}" transform="translate(${x},${y})">
    <rect x="${-rw/2+3}" y="${-rh/2+5}" width="${rw}" height="${rh}" rx="4" fill="${C.abyss}" opacity="0.5" filter="url(#blur4)"/>
    <rect x="${-rw/2}" y="${-rh/2}" width="${rw}" height="${rh}" rx="4" fill="${C.s700}"/>
    <rect x="${-rw/2+2}" y="${-rh/2+2}" width="${rw-4}" height="${rh-4}" rx="3" fill="${C.s300}"/>
    <rect x="${-rw/2+2}" y="${-rh/2+2}" width="${vertical? rw-4 : 3}" height="${vertical? 3 : rh-4}" fill="${C.s100}" opacity="0.6"/>
    ${bolts}
  </g>`);
}

function rivet(x, y) {
  p(`<circle cx="${x}" cy="${y}" r="6" fill="${C.abyss}" opacity="0.6"/><circle cx="${x}" cy="${y-1}" r="5" fill="${C.s700}"/><circle cx="${x-1.5}" cy="${y-2.5}" r="2" fill="${C.s300}" opacity="0.8"/>`);
}

function plate(x, y, w, h) {
  p(`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18" fill="url(#plate)"/>
     <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="18" fill="none" stroke="#ffffff" stroke-opacity="0.08" stroke-width="2"/>
     <path d="M${x+18},${y+1} H${x+w-18}" stroke="#ffffff" stroke-opacity="0.16" stroke-width="2" stroke-linecap="round"/>`);
  [[x+30,y+30],[x+w-30,y+30],[x+30,y+h-30],[x+w-30,y+h-30]].forEach(([a,b]) => rivet(a,b));
}

function valve(x, y, r) {
  const spokes = [0,45,90,135].map(a => `<line x1="${-r}" y1="0" x2="${r}" y2="0" stroke="${C.b700}" stroke-width="10" stroke-linecap="round" transform="rotate(${a})"/><line x1="${-r+4}" y1="-1" x2="${r-4}" y2="-1" stroke="${C.b500}" stroke-width="6" stroke-linecap="round" transform="rotate(${a})"/>`).join('');
  p(`<g transform="translate(${x},${y})">
    <circle r="${r+10}" fill="${C.b500}" opacity="0.45" filter="url(#blur16)"/>
    <circle r="${r+4}" fill="${C.b900}" opacity="0.7" transform="translate(3,5)"/>
    <circle r="${r}" fill="none" stroke="${C.b700}" stroke-width="12"/>
    <circle r="${r}" fill="none" stroke="url(#brass)" stroke-width="9"/>
    <circle r="${r-4}" fill="none" stroke="${C.b100}" stroke-opacity="0.35" stroke-width="2"/>
    ${spokes}
    <circle r="${r*0.32}" fill="${C.b700}"/><circle r="${r*0.26}" fill="url(#brassR)"/>
    <circle cx="${-r*0.08}" cy="${-r*0.1}" r="${r*0.09}" fill="${C.b100}" opacity="0.9"/>
  </g>`);
}

function tank(x, y, r) {
  const lugs = [0,90,180,270].map(a => `<rect x="-14" y="${-r-14}" width="28" height="16" rx="3" fill="${C.s300}" stroke="${C.s700}" stroke-width="2" transform="rotate(${a})"/>`).join('');
  p(`<g transform="translate(${x},${y})">
    <circle r="${r+8}" fill="${C.abyss}" opacity="0.6" transform="translate(6,10)" filter="url(#blur8)"/>
    ${lugs}
    <circle r="${r+6}" fill="${C.s700}"/>
    <circle r="${r}" fill="url(#tank)"/>
    <clipPath id="tankclip"><circle r="${r-4}"/></clipPath>
    <g clip-path="url(#tankclip)">
      <rect x="${-r}" y="${r*0.05}" width="${2*r}" height="${r}" fill="url(#liquidV)"/>
      <path d="M${-r},${r*0.05} q${r*0.25},-14 ${r*0.5},0 t${r*0.5},0 t${r*0.5},0 t${r*0.5},0 V${r} H${-r}Z" fill="${C.l300}" opacity="0.55"/>
      <circle cx="${-r*0.35}" cy="${r*0.5}" r="6" fill="${C.lglow}" opacity="0.7"/>
      <circle cx="${r*0.2}" cy="${r*0.35}" r="4" fill="${C.lglow}" opacity="0.6"/>
      <circle cx="${r*0.45}" cy="${r*0.7}" r="8" fill="${C.lglow}" opacity="0.5"/>
    </g>
    <circle r="${r}" fill="none" stroke="${C.s300}" stroke-width="5"/>
    <circle r="${r+3}" fill="none" stroke="${C.s700}" stroke-width="2"/>
    <ellipse cx="${-r*0.3}" cy="${-r*0.45}" rx="${r*0.35}" ry="${r*0.16}" fill="#ffffff" opacity="0.12" transform="rotate(-30)"/>
  </g>`);
}

function drop(x, y, s, o=1) {
  const d = 'M0,-18 C6,-6 10,0 10,6 A10,10 0 1,1 -10,6 C-10,0 -6,-6 0,-18Z';
  p(`<g transform="translate(${x},${y}) scale(${s})" opacity="${o}">
    <path d="${d}" fill="${C.lglow}" opacity="0.5" filter="url(#blur8)"/>
    <path d="${d}" fill="url(#liquidV)"/>
    <circle cx="-3.5" cy="4" r="2.2" fill="${C.lglow}" opacity="0.9"/>
  </g>`);
}

// ---------------- scene ----------------
p(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.deep}"/><stop offset="0.55" stop-color="${C.abyss}"/><stop offset="1" stop-color="#050C14"/></linearGradient>
  <linearGradient id="plate" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.base}" stop-opacity="0.85"/><stop offset="1" stop-color="${C.deep}" stop-opacity="0.85"/></linearGradient>
  <linearGradient id="liquidV" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.l300}"/><stop offset="0.5" stop-color="${C.l500}"/><stop offset="1" stop-color="${C.l700}"/></linearGradient>
  <linearGradient id="brass" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b500}"/></linearGradient>
  <radialGradient id="brassR" cx="0.35" cy="0.3" r="0.8"><stop offset="0" stop-color="${C.b300}"/><stop offset="1" stop-color="${C.b700}"/></radialGradient>
  <radialGradient id="tank" cx="0.4" cy="0.35" r="0.8"><stop offset="0" stop-color="${C.r500}"/><stop offset="1" stop-color="${C.r700}"/></radialGradient>
  <radialGradient id="vignette" cx="0.5" cy="0.5" r="0.62"><stop offset="0" stop-color="${C.abyss}" stop-opacity="0.55"/><stop offset="0.6" stop-color="${C.abyss}" stop-opacity="0.25"/><stop offset="1" stop-color="${C.abyss}" stop-opacity="0"/></radialGradient>
  <radialGradient id="floorGlow" cx="0.5" cy="1" r="0.7"><stop offset="0" stop-color="${C.l500}" stop-opacity="0.22"/><stop offset="1" stop-color="${C.l500}" stop-opacity="0"/></radialGradient>
  <radialGradient id="edgeDark" cx="0.5" cy="0.5" r="0.75"><stop offset="0.6" stop-color="${C.abyss}" stop-opacity="0"/><stop offset="1" stop-color="#03080E" stop-opacity="0.8"/></radialGradient>
  <filter id="blur4" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="4"/></filter>
  <filter id="blur8" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="8"/></filter>
  <filter id="blur12" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="12"/></filter>
  <filter id="blur16" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="16"/></filter>
</defs>
<rect width="${W}" height="${H}" fill="url(#bg)"/>`);

// bulkhead plates
const pw = 616, ph = 516, gap = 20;
for (let r = 0; r < 2; r++) for (let c = 0; c < 3; c++) plate(gap/2 + c*(pw+gap+2), gap/2 + r*(ph+gap+2), pw, ph);

// aqua light spilling up from the floor
p(`<rect width="${W}" height="${H}" fill="url(#floorGlow)"/>`);

// far background pipes (dim, dry)
pipe({pts:[[900,-60],[900,90],[1460,90],[1460,-60]], w:28, liquid:false, dim:0.45});
pipe({pts:[[1830,-60],[1830,1140]], w:26, liquid:false, dim:0.4});
flange(1830, 300, true, 26, 0.4); flange(1830, 820, true, 26, 0.4);
pipe({pts:[[-60,620],[80,620],[80,1140]], w:26, liquid:false, dim:0.4});

// Route A: top-left, filled
pipe({pts:[[-60,190],[430,190],[430,-60]], w:64});
flange(200, 190, false, 64); flange(430, 60, true, 64);
// Route B: right U, filled
pipe({pts:[[1980,150],[1560,150],[1560,540],[1980,540]], w:60});
flange(1760, 150, false, 60); flange(1560, 340, true, 60); flange(1760, 540, false, 60);
// Route D: left vertical with valve, filled
pipe({pts:[[150,-60],[150,760],[-60,760]], w:52});
flange(150, 120, true, 52); flange(150, 600, true, 52);
valve(150, 400, 56);
// Route E: right lower with reservoir tank
pipe({pts:[[1980,780],[1700,780],[1700,960]], w:52});
flange(1700, 880, true, 52);
const tankLater = () => tank(1840, 780, 54);
// Route C: long floor pipe, liquid still flowing (72 %)
const fillC = 0.72, cx0 = -60, cx1 = 1980, headX = cx0 + fillC*(cx1-cx0);
pipe({pts:[[cx0,960],[cx1,960]], w:72, fill:fillC});
// liquid head + trailing bubbles
p(`<circle cx="${headX}" cy="960" r="20" fill="${C.lglow}" opacity="0.7" filter="url(#blur12)"/>
   <circle cx="${headX-2}" cy="960" r="12" fill="${C.lglow}"/>
   <circle cx="${headX-40}" cy="953" r="3" fill="${C.lglow}" opacity="0.8"/><circle cx="${headX-70}" cy="951" r="2.2" fill="${C.lglow}" opacity="0.7"/><circle cx="${headX-100}" cy="954" r="2.6" fill="${C.lglow}" opacity="0.6"/>`);
[380, 760, 1140].forEach(x => flange(x, 960, false, 72));
tankLater();

// leak at flange x=760: drops + puddle
p(`<ellipse cx="770" cy="1062" rx="70" ry="9" fill="${C.l500}" opacity="0.45" filter="url(#blur8)"/>
   <ellipse cx="770" cy="1062" rx="46" ry="6" fill="url(#liquidV)"/>
   <ellipse cx="760" cy="1060" rx="18" ry="2" fill="${C.lglow}" opacity="0.7"/>`);
drop(772, 1010, 1.0); drop(766, 1040, 0.6, 0.9); drop(785, 1030, 0.45, 0.8);

// vignette to calm the centre for title + buttons, dark edges
p(`<rect width="${W}" height="${H}" fill="url(#vignette)"/>`);
p(`<rect width="${W}" height="${H}" fill="url(#edgeDark)"/>`);

// bokeh bubbles drifting up (on top, soft)
const seed = [[310,880,9],[520,700,5],[640,300,7],[1240,250,6],[1380,700,10],[1500,860,4],[990,120,5],[1650,420,6],[260,520,4],[1120,830,7],[1810,650,5],[420,420,3],[1330,470,3.5],[700,150,3]];
seed.forEach(([x,y,r]) => p(`<circle cx="${x}" cy="${y}" r="${r*2.2}" fill="${C.lglow}" opacity="0.10" filter="url(#blur8)"/><circle cx="${x}" cy="${y}" r="${r}" fill="none" stroke="${C.lglow}" stroke-opacity="0.35" stroke-width="1.5"/><circle cx="${x-r*0.35}" cy="${y-r*0.35}" r="${r*0.25}" fill="${C.lglow}" opacity="0.6"/>`));

p(`</svg>`);
fs.writeFileSync(OUT, out.join('\n'));
console.log('wrote', OUT, out.join('\n').length, 'bytes');
