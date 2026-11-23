// Leak Out – Daily Pipe promo renderer.
// Shared drawing library: tokens from docs/design-system.md (Deep Flow v2),
// pipe recipe (6.3), endpoints (6.4), liquid (6.5), dialogs (5.6), motion (7).
// Every scene is a pure function of time t (seconds), so frames are deterministic.

const W = 1080, H = 1920, FPS = 30, DURATION = 15;

const C = {
  abyss: '#07111C', deep: '#0C1A2A', base: '#122538', raised: '#19324A', overlay: '#21405C',
  lineSubtle: 'rgba(255,255,255,0.08)', lineStrong: 'rgba(255,255,255,0.16)', lineHi: 'rgba(255,255,255,0.22)',
  glow: '#9DF3FF', l300: '#5EE7FF', l500: '#19C6F2', l700: '#0A8DC2', l900: '#065A82', lTint: 'rgba(25,198,242,0.14)',
  b100: '#FFE9B0', b300: '#FFCF5C', b500: '#F5A623', b700: '#C97A12', b900: '#7A4708', bTint: 'rgba(245,166,35,0.16)',
  leak: '#FF5A5F', leak700: '#C9353B', leakTint: 'rgba(255,90,95,0.16)',
  ok: '#3DDC97', ok700: '#1FA971', okTint: 'rgba(61,220,151,0.16)',
  p1: '#FF6FD8', p2: '#B8F25C', p3: '#FF8A3D',
  s100: '#C9D6E2', s300: '#8FA3B8', s500: '#5C7189', s700: '#3A4B60', s900: '#22303F',
  text: '#F2F7FB', text2: '#A9BACB', muted: '#6E8296', onBrass: '#2A1A03',
};
const DISPLAY = '"Wendy One"';
const BODY = '"Varela Round", "Fredoka", sans-serif';

// ---------- math / easing ----------
const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
const lerp = (a, b, t) => a + (b - a) * t;
const prog = (t, a, b) => clamp((t - a) / (b - a));
const easeOut = (t) => 1 - Math.pow(1 - t, 3);
const easeIn = (t) => t * t * t;
const easeInOut = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
// motion.pop: spring with ~6 % overshoot, settles in ~320 ms
const springPop = (t) => {
  if (t <= 0) return 0; if (t >= 1) return 1;
  return 1 - Math.exp(-6.5 * t) * Math.cos(9.5 * t);
};
const pop = (t, start, dur = 0.32) => springPop(clamp((t - start) / dur));
function rng(seed) { let s = seed >>> 0; return () => ((s = (s * 1664525 + 1013904223) >>> 0) / 4294967296); }

// ---------- primitives ----------
function rr(ctx, x, y, w, h, r) {
  r = Math.min(r, w / 2, h / 2);
  ctx.beginPath();
  ctx.moveTo(x + r, y); ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath();
}
function withAlpha(ctx, a, fn) { if (a <= 0) return; ctx.save(); ctx.globalAlpha *= clamp(a); fn(); ctx.restore(); }
function text(ctx, str, x, y, { font = BODY, size = 40, color = C.text, align = 'center', base = 'middle', spacing = 0, glow = null, glowBlur = 24, shadow = null } = {}) {
  ctx.save();
  ctx.font = `${size}px ${font}`; ctx.fillStyle = color; ctx.textAlign = align; ctx.textBaseline = base;
  if (spacing) ctx.letterSpacing = `${spacing}px`;
  if (glow) { ctx.shadowColor = glow; ctx.shadowBlur = glowBlur; }
  if (shadow) { ctx.shadowColor = shadow; ctx.shadowBlur = 8; ctx.shadowOffsetY = 3; }
  ctx.fillText(str, x, y);
  ctx.restore();
}
function textWidth(ctx, str, size, font = BODY, spacing = 0) {
  ctx.save(); ctx.font = `${size}px ${font}`; if (spacing) ctx.letterSpacing = `${spacing}px`;
  const w = ctx.measureText(str).width; ctx.restore(); return w;
}

// ---------- background ----------
function background(ctx, t, { seed = 7, vignette = true } = {}) {
  const g = ctx.createLinearGradient(0, 0, W * 0.4, H);
  g.addColorStop(0, C.abyss); g.addColorStop(1, C.deep);
  ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
  // faint workshop grid
  ctx.save(); ctx.strokeStyle = 'rgba(255,255,255,0.035)'; ctx.lineWidth = 2;
  const off = (t * 12) % 90;
  for (let x = -90 + off * 0.3; x < W + 90; x += 90) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
  for (let y = -90 + off; y < H + 90; y += 90) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }
  ctx.restore();
  // aqua caustic glow from below
  const rg = ctx.createRadialGradient(W / 2, H * 0.62, 50, W / 2, H * 0.62, H * 0.6);
  rg.addColorStop(0, 'rgba(25,198,242,0.10)'); rg.addColorStop(1, 'rgba(25,198,242,0)');
  ctx.fillStyle = rg; ctx.fillRect(0, 0, W, H);
  // rising bubbles
  const r = rng(seed);
  for (let i = 0; i < 28; i++) {
    const x0 = r() * W, sp = 40 + r() * 90, sz = 3 + r() * 9, ph = r() * H;
    const y = H + 40 - ((ph + t * sp) % (H + 80));
    const x = x0 + Math.sin(t * 1.3 + i) * 14;
    ctx.beginPath(); ctx.arc(x, y, sz, 0, Math.PI * 2);
    ctx.strokeStyle = `rgba(157,243,255,${0.10 + 0.12 * r()})`; ctx.lineWidth = 2; ctx.stroke();
  }
  if (vignette) {
    const v = ctx.createRadialGradient(W / 2, H / 2, H * 0.3, W / 2, H / 2, H * 0.75);
    v.addColorStop(0, 'rgba(7,17,28,0)'); v.addColorStop(1, 'rgba(7,17,28,0.75)');
    ctx.fillStyle = v; ctx.fillRect(0, 0, W, H);
  }
}

// ---------- pipe geometry (unit cell, y down) ----------
const SIDE = { L: [0, 0.5], R: [1, 0.5], T: [0.5, 0], B: [0.5, 1] };
const OPP = { L: 'R', R: 'L', T: 'B', B: 'T' };
const DIR = { L: [-1, 0], R: [1, 0], T: [0, -1], B: [0, 1] };

// returns point at s∈[0,1] travelling from side a to side b ('C' = centre)
function cellPoint(a, b, s) {
  const pa = a === 'C' ? [0.5, 0.5] : SIDE[a], pb = b === 'C' ? [0.5, 0.5] : SIDE[b];
  if (a === 'C' || b === 'C' || OPP[a] === b) return [lerp(pa[0], pb[0], s), lerp(pa[1], pb[1], s)];
  const cx = (a === 'R' || b === 'R') ? 1 : 0, cy = (a === 'B' || b === 'B') ? 1 : 0;
  let a0 = Math.atan2(pa[1] - cy, pa[0] - cx), a1 = Math.atan2(pb[1] - cy, pb[0] - cx);
  let d = a1 - a0; while (d > Math.PI) d -= 2 * Math.PI; while (d < -Math.PI) d += 2 * Math.PI;
  const an = a0 + d * s;
  return [cx + 0.5 * Math.cos(an), cy + 0.5 * Math.sin(an)];
}
function tracePath(ctx, x, y, c, a, b, s0 = 0, s1 = 1, offset = null) {
  const n = 28; ctx.beginPath();
  for (let i = 0; i <= n; i++) {
    const s = lerp(s0, s1, i / n); let [px, py] = cellPoint(a, b, s);
    if (offset) { px += offset[0]; py += offset[1]; }
    if (i === 0) ctx.moveTo(x + px * c, y + py * c); else ctx.lineTo(x + px * c, y + py * c);
  }
}
function strokePath(ctx, x, y, c, a, b, w, col, s0 = 0, s1 = 1, cap = 'butt', offset = null) {
  ctx.save(); tracePath(ctx, x, y, c, a, b, s0, s1, offset);
  ctx.lineWidth = w * c; ctx.strokeStyle = col; ctx.lineCap = cap; ctx.lineJoin = 'round'; ctx.stroke(); ctx.restore();
}
// steel layers of one channel (6.3 layers 1,2,3,5)
function pipeSteel(ctx, x, y, c, a, b) {
  const sh = 2 / c;
  strokePath(ctx, x, y, c, a, b, 0.36, C.s700, 0, 1, 'butt', [sh, sh]);
  strokePath(ctx, x, y, c, a, b, 0.36, C.s500);
  // specular: 0.12 c toward the top-left
  const spec = [-0.085, -0.085];
  if (OPP[a] === b && (a === 'L' || a === 'R')) { spec[0] = 0; spec[1] = -0.12; }
  if (OPP[a] === b && (a === 'T' || a === 'B')) { spec[0] = -0.12; spec[1] = 0; }
  ctx.save(); ctx.beginPath(); ctx.rect(x, y, c, c); ctx.clip();
  strokePath(ctx, x, y, c, a, b, 0.04, 'rgba(201,214,226,0.55)', 0, 1, 'round', spec);
  ctx.restore();
  strokePath(ctx, x, y, c, a, b, 0.25, C.s900);
}
function liquid(ctx, x, y, c, a, b, s0, s1, glowOn) {
  if (s1 <= s0) return;
  ctx.save();
  if (glowOn) { ctx.shadowColor = 'rgba(157,243,255,0.8)'; ctx.shadowBlur = 0.35 * c; }
  strokePath(ctx, x, y, c, a, b, 0.21, C.l700, s0, s1);
  ctx.restore();
  strokePath(ctx, x, y, c, a, b, 0.15, C.l500, s0, s1);
  strokePath(ctx, x, y, c, a, b, 0.07, C.l300, s0, s1, 'butt', [-0.015, -0.015]);
}
function liquidHead(ctx, x, y, c, a, b, s, t) {
  const [px, py] = cellPoint(a, b, s);
  ctx.save(); ctx.shadowColor = C.glow; ctx.shadowBlur = 0.4 * c;
  ctx.fillStyle = C.glow; ctx.beginPath(); ctx.arc(x + px * c, y + py * c, 0.085 * c, 0, Math.PI * 2); ctx.fill(); ctx.restore();
  // trailing bubbles
  for (let i = 1; i <= 3; i++) {
    const ss = s - i * 0.09; if (ss < 0) break;
    const [bx, by] = cellPoint(a, b, ss);
    ctx.fillStyle = 'rgba(157,243,255,0.75)';
    ctx.beginPath(); ctx.arc(x + bx * c + Math.sin(t * 9 + i) * 0.02 * c, y + by * c - 0.03 * c, (0.022 + 0.008 * (i % 2)) * c, 0, Math.PI * 2); ctx.fill();
  }
}
function flange(ctx, x, y, c, side) {
  ctx.save(); ctx.fillStyle = C.s300; ctx.strokeStyle = C.s700; ctx.lineWidth = Math.max(1, 0.01 * c);
  const L = 0.1, a0 = 0.30, a1 = 0.375, b0 = 0.625, b1 = 0.70;
  const rects = side === 'L' ? [[0, a0, L, a1 - a0], [0, b0, L, b1 - b0]]
    : side === 'R' ? [[1 - L, a0, L, a1 - a0], [1 - L, b0, L, b1 - b0]]
    : side === 'T' ? [[a0, 0, a1 - a0, L], [b0, 0, b1 - b0, L]]
    : [[a0, 1 - L, a1 - a0, L], [b0, 1 - L, b1 - b0, L]];
  for (const [rx, ry, rw, rh] of rects) { ctx.fillRect(x + rx * c, y + ry * c, rw * c, rh * c); ctx.strokeRect(x + rx * c, y + ry * c, rw * c, rh * c); }
  ctx.restore();
}
function glass(ctx, x, y, c, a, b) {
  strokePath(ctx, x, y, c, a, b, 0.36, 'rgba(255,255,255,0.05)');
}

// endpoint glow (7.2)
function endpointGlow(ctx, cx, cy, c, col, alpha) {
  const r = 0.24 * c + 0.3 * c;
  const g = ctx.createRadialGradient(cx, cy, 0, cx, cy, r);
  g.addColorStop(0, col.replace(')', `,${alpha})`).replace('rgb', 'rgba')); g.addColorStop(1, 'rgba(0,0,0,0)');
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.fill();
}
function valve(ctx, x, y, c, exit, t, flowing, prog) {
  const cx = x + 0.5 * c, cy = y + 0.5 * c;
  const al = flowing ? 0.6 : 0.45 + 0.45 * (1 - Math.cos(2 * Math.PI * t / 1.6)) / 2;
  endpointGlow(ctx, cx, cy, c, 'rgb(245,166,35)', al);
  pipeSteel(ctx, x, y, c, 'C', exit);
  liquid(ctx, x, y, c, 'C', exit, 0, prog, false);
  flange(ctx, x, y, c, exit);
  // chevron
  const d = DIR[exit], tip = 0.32, px = -d[1], py = d[0];
  ctx.save(); ctx.strokeStyle = C.b300; ctx.lineWidth = 0.04 * c; ctx.lineCap = 'round'; ctx.beginPath();
  ctx.moveTo(cx + (d[0] * (tip - 0.08) + px * 0.08) * c, cy + (d[1] * (tip - 0.08) + py * 0.08) * c);
  ctx.lineTo(cx + d[0] * tip * c, cy + d[1] * tip * c);
  ctx.lineTo(cx + (d[0] * (tip - 0.08) - px * 0.08) * c, cy + (d[1] * (tip - 0.08) - py * 0.08) * c); ctx.stroke(); ctx.restore();
  // wheel
  const rot = flowing ? t * 2.5 : 0;
  ctx.save(); ctx.translate(cx, cy); ctx.rotate(rot);
  const g = ctx.createRadialGradient(0, 0, 0, 0, 0, 0.24 * c); g.addColorStop(0, C.b300); g.addColorStop(1, C.b500);
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(0, 0, 0.24 * c, 0, Math.PI * 2); ctx.fill();
  ctx.strokeStyle = C.b700; ctx.lineWidth = 0.03 * c; ctx.stroke();
  ctx.strokeStyle = C.b900; ctx.lineWidth = 0.045 * c; ctx.lineCap = 'round';
  ctx.beginPath(); ctx.moveTo(-0.2 * c, 0); ctx.lineTo(0.2 * c, 0); ctx.moveTo(0, -0.2 * c); ctx.lineTo(0, 0.2 * c); ctx.stroke();
  ctx.fillStyle = C.b700; ctx.strokeStyle = C.b900; ctx.lineWidth = 0.02 * c;
  ctx.beginPath(); ctx.arc(0, 0, 0.08 * c, 0, Math.PI * 2); ctx.fill(); ctx.stroke();
  ctx.restore();
  ctx.fillStyle = 'rgba(255,233,176,0.45)'; ctx.beginPath(); ctx.arc(cx - 0.08 * c, cy - 0.1 * c, 0.05 * c, 0, Math.PI * 2); ctx.fill();
}
function finish(ctx, x, y, c, entry, t, won, prog) {
  const cx = x + 0.5 * c, cy = y + 0.5 * c;
  const al = won ? 0.95 : 0.45 + 0.45 * (1 - Math.cos(2 * Math.PI * t / 2)) / 2;
  endpointGlow(ctx, cx, cy, c, 'rgb(61,220,151)', al);
  pipeSteel(ctx, x, y, c, entry, 'C');
  liquid(ctx, x, y, c, entry, 'C', 0, prog, false);
  flange(ctx, x, y, c, entry);
  ctx.save(); ctx.beginPath(); ctx.arc(cx, cy, 0.215 * c, 0, Math.PI * 2); ctx.clip();
  const sq = 0.43 * c / 6;
  for (let i = 0; i < 6; i++) for (let j = 0; j < 6; j++) {
    ctx.fillStyle = (i + j) % 2 === 0 ? (won ? C.ok : C.text) : C.s900;
    ctx.fillRect(cx - 3 * sq + i * sq, cy - 3 * sq + j * sq, sq + 0.5, sq + 0.5);
  }
  ctx.restore();
  ctx.save(); if (won) { ctx.shadowColor = C.ok; ctx.shadowBlur = 0.4 * c; }
  ctx.strokeStyle = C.ok; ctx.lineWidth = 0.05 * c; ctx.beginPath(); ctx.arc(cx, cy, 0.24 * c, 0, Math.PI * 2); ctx.stroke(); ctx.restore();
}
function emptyCell(ctx, x, y, c, plus = true) {
  rr(ctx, x + 2, y + 2, c - 4, c - 4, 0.12 * c); ctx.fillStyle = 'rgba(33,64,92,0.6)'; ctx.fill();
  ctx.strokeStyle = C.lineSubtle; ctx.lineWidth = 1.5; ctx.stroke();
  if (plus) {
    ctx.strokeStyle = 'rgba(110,130,150,0.4)'; ctx.lineWidth = Math.max(2, 0.025 * c); ctx.lineCap = 'round';
    const s = 0.07 * c, cx = x + c / 2, cy = y + c / 2;
    ctx.beginPath(); ctx.moveTo(cx - s, cy); ctx.lineTo(cx + s, cy); ctx.moveTo(cx, cy - s); ctx.lineTo(cx, cy + s); ctx.stroke();
  }
}
function blockedCell(ctx, x, y, c) {
  ctx.save(); rr(ctx, x + 2, y + 2, c - 4, c - 4, 0.12 * c); ctx.fillStyle = C.abyss; ctx.fill(); ctx.clip();
  ctx.strokeStyle = C.lineSubtle; ctx.lineWidth = 0.03 * c;
  for (let k = -c; k < c * 2; k += 0.16 * c) { ctx.beginPath(); ctx.moveTo(x + k, y); ctx.lineTo(x + k - c, y + c); ctx.stroke(); }
  ctx.restore();
}

// ---------- particles ----------
function burst(ctx, x, y, t0, t, { n = 12, cols = [C.l500, C.glow], dist = 120, dur = 0.6, size = 12, seed = 3 } = {}) {
  const p = (t - t0) / dur; if (p < 0 || p > 1) return;
  const r = rng(seed), e = easeOut(p);
  for (let i = 0; i < n; i++) {
    const an = r() * Math.PI * 2, d = dist * (0.5 + r() * 0.6) * e, s = size * (0.6 + r() * 0.6) * (1 - p * 0.6);
    ctx.globalAlpha = 1 - p; ctx.fillStyle = cols[i % cols.length];
    ctx.beginPath(); ctx.arc(x + Math.cos(an) * d, y + Math.sin(an) * d + p * p * dist * 0.4, s, 0, Math.PI * 2); ctx.fill();
  }
  ctx.globalAlpha = 1;
}
function sparkle(ctx, x, y, t0, t, scale = 1, seed = 5) {
  const p = (t - t0) / 0.6; if (p < 0 || p > 1) return;
  ctx.save();
  const fl = clamp(1 - p * 2);
  const g = ctx.createRadialGradient(x, y, 0, x, y, 50 * scale); g.addColorStop(0, `rgba(255,207,92,${0.8 * fl})`); g.addColorStop(1, 'rgba(255,207,92,0)');
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, 50 * scale, 0, Math.PI * 2); ctx.fill(); ctx.restore();
  burst(ctx, x, y, t0, t, { n: 8, cols: [C.glow, C.b300], dist: 42 * scale, size: 5 * scale, seed });
}

// ---------- board ----------
// route: [[col,row], ...] from start cell to the last cell (finish or a leak).
// Cells visited twice become a cross (X).
class Board {
  constructor(o) {
    Object.assign(this, { cols: 6, rows: 7, c: 140, x: 0, y: 0, blocked: [], leakAt: null, popups: [] }, o);
    this.cells = new Map();
    const r = this.route, n = r.length;
    this.chan = []; // per route step: {col,row,a,b}
    for (let i = 0; i < n; i++) {
      const [col, row] = r[i];
      const a = i === 0 ? 'C' : this.side(r[i], r[i - 1]);
      let b;
      if (i < n - 1) b = this.side(r[i], r[i + 1]);
      else b = this.finishCell ? 'C' : (this.leakExit || OPP[a]);
      this.chan.push({ col, row, a, b, i });
      const k = col + ',' + row;
      if (!this.cells.has(k)) this.cells.set(k, { col, row, steps: [] });
      this.cells.get(k).steps.push(i);
    }
    this.startKey = r[0].join(','); this.endKey = r[n - 1].join(',');
  }
  side(from, to) { const dx = to[0] - from[0], dy = to[1] - from[1]; return dx === 1 ? 'R' : dx === -1 ? 'L' : dy === 1 ? 'B' : 'T'; }
  cellXY(col, row) { return [this.x + col * this.c, this.y + row * this.c]; }
  get width() { return this.cols * this.c; } get height() { return this.rows * this.c; }
  // place times: array parallel to route (undefined for start/finish) ; flow: head position in route steps
  draw(ctx, t, { placeT = [], head = -1, won = false, appear = null, plate = true, dim = 0, leakT = null, shakeT = null } = {}) {
    const c = this.c; ctx.save();
    if (shakeT != null && t > shakeT && t < shakeT + 0.25) ctx.translate(Math.sin(t * 90) * 5, 0);
    if (plate) {
      const pad = 0.12 * c;
      ctx.save(); ctx.shadowColor = 'rgba(0,0,0,0.45)'; ctx.shadowBlur = 40; ctx.shadowOffsetY = 12;
      rr(ctx, this.x - pad, this.y - pad, this.width + 2 * pad, this.height + 2 * pad, 0.32 * c);
      const pg = ctx.createRadialGradient(this.x + this.width / 2, this.y + this.height / 2, 10, this.x + this.width / 2, this.y + this.height / 2, this.height * 0.75);
      pg.addColorStop(0, C.base); pg.addColorStop(1, '#0F2031'); ctx.fillStyle = pg; ctx.fill(); ctx.restore();
      ctx.strokeStyle = C.lineHi; ctx.lineWidth = 2; ctx.beginPath();
      ctx.moveTo(this.x - pad + 0.32 * c, this.y - pad + 1); ctx.lineTo(this.x + this.width + pad - 0.32 * c, this.y - pad + 1); ctx.stroke();
    }
    const cellAppear = (col, row) => {
      if (!appear) return { a: 1, dy: 0 };
      const at = appear(col, row); const p = clamp((t - at) / 0.42);
      if (p <= 0) return { a: 0, dy: -1 };
      // motion.fall: ease-in drop then one small bounce
      const fall = p < 0.7 ? (1 - Math.pow(p / 0.7, 2)) : -Math.sin((p - 0.7) / 0.3 * Math.PI) * 0.06;
      return { a: clamp(p * 3), dy: -fall * c * 1.4 };
    };
    // cells background
    const blockedSet = new Set(this.blocked.map((b) => b.join(',')));
    for (let row = 0; row < this.rows; row++) for (let col = 0; col < this.cols; col++) {
      const k = col + ',' + row, ap = cellAppear(col, row); if (ap.a <= 0) continue;
      const [x, y] = this.cellXY(col, row);
      withAlpha(ctx, ap.a, () => {
        if (blockedSet.has(k)) blockedCell(ctx, x, y + ap.dy, c);
        else emptyCell(ctx, x, y + ap.dy, c, !(this.cells.has(k) && (placeT[this.cells.get(k).steps[0]] ?? -1) <= t) && k !== this.startKey && !(this.finishCell && k === this.endKey));
      });
    }
    const progAt = (i) => clamp(head - i);
    // pipes
    for (const [k, cell] of this.cells) {
      const [x0, y0] = this.cellXY(cell.col, cell.row);
      const ap = cellAppear(cell.col, cell.row); if (ap.a <= 0) continue;
      const y = y0 + ap.dy;
      const i0 = cell.steps[0];
      if (k === this.startKey && i0 === 0) {
        withAlpha(ctx, ap.a, () => valve(ctx, x0, y, c, this.chan[0].b, t, head > 0, progAt(0)));
        continue;
      }
      if (this.finishCell && k === this.endKey) {
        const ch = this.chan[i0];
        withAlpha(ctx, ap.a, () => finish(ctx, x0, y, c, ch.a, t, won, progAt(i0)));
        continue;
      }
      const pt = placeT[i0]; if (pt == null || t < pt) continue;
      const s = 1.15 - 0.15 * pop(t, pt);
      const al = clamp((t - pt) / 0.08);
      ctx.save(); ctx.globalAlpha *= al * ap.a;
      ctx.translate(x0 + c / 2, y + c / 2); ctx.scale(s, s); ctx.translate(-x0 - c / 2, -y - c / 2);
      const chans = cell.steps.map((i) => this.chan[i]);
      if (chans.length === 2) {
        // cross: vertical first, horizontal on top with a deep gap
        const v = chans.find((h) => h.a === 'T' || h.a === 'B'), hz = chans.find((h) => h.a === 'L' || h.a === 'R');
        pipeSteel(ctx, x0, y, c, v.a, v.b); liquid(ctx, x0, y, c, v.a, v.b, 0, progAt(v.i), head > v.i && head < v.i + 1);
        strokePath(ctx, x0, y, c, hz.a, hz.b, 0.46, C.deep);
        pipeSteel(ctx, x0, y, c, hz.a, hz.b); liquid(ctx, x0, y, c, hz.a, hz.b, 0, progAt(hz.i), head > hz.i && head < hz.i + 1);
        for (const ch of chans) { flange(ctx, x0, y, c, ch.a); flange(ctx, x0, y, c, ch.b); }
        for (const ch of chans) if (head > ch.i && head < ch.i + 1) liquidHead(ctx, x0, y, c, ch.a, ch.b, head - ch.i, t);
      } else {
        const ch = chans[0];
        pipeSteel(ctx, x0, y, c, ch.a, ch.b);
        liquid(ctx, x0, y, c, ch.a, ch.b, 0, progAt(ch.i), head > ch.i && head < ch.i + 1);
        flange(ctx, x0, y, c, ch.a); flange(ctx, x0, y, c, ch.b);
        glass(ctx, x0, y, c, ch.a, ch.b);
        if (head > ch.i && head < ch.i + 1) liquidHead(ctx, x0, y, c, ch.a, ch.b, head - ch.i, t);
      }
      ctx.restore();
    }
    // leak burst at the open port
    if (leakT != null && t >= leakT) {
      const last = this.chan[this.chan.length - 1];
      const [x, y] = this.cellXY(last.col, last.row);
      const pp = SIDE[last.b];
      const lx = x + pp[0] * c, ly = y + pp[1] * c;
      const d = DIR[last.b];
      const pr = clamp((t - leakT) / 0.6);
      ctx.save(); ctx.globalAlpha = 0.6 * easeOut(pr); ctx.fillStyle = C.l700;
      ctx.beginPath(); ctx.ellipse(lx + d[0] * c * 0.45, ly + d[1] * c * 0.45 + 0.1 * c, 0.42 * c * easeOut(pr), 0.25 * c * easeOut(pr), 0, 0, Math.PI * 2); ctx.fill(); ctx.restore();
      burst(ctx, lx, ly, leakT, t, { n: 14, dist: c * 0.9, size: 0.06 * c, dur: 0.5, seed: 11 });
      // ongoing spray
      for (let i = 0; i < 6; i++) {
        const ph = ((t - leakT) * 2.2 + i / 6) % 1;
        const sx = lx + d[0] * ph * c * 0.6 + (i - 2.5) * 0.04 * c * ph * (d[1] !== 0 ? 1 : 0);
        const sy = ly + d[1] * ph * c * 0.6 + ph * ph * c * 0.4 + (i - 2.5) * 0.04 * c * ph * (d[0] !== 0 ? 1 : 0);
        ctx.globalAlpha = 1 - ph; ctx.fillStyle = i % 2 ? C.glow : C.l500;
        ctx.beginPath(); ctx.arc(sx, sy, 0.035 * c, 0, Math.PI * 2); ctx.fill();
      }
      ctx.globalAlpha = 1;
    }
    // points popups
    for (const pu of this.popups) {
      const p = (t - pu.t) / 1.0; if (p < 0 || p > 1) continue;
      const [x, y] = this.cellXY(pu.col, pu.row);
      const a = p < 0.15 ? easeOut(p / 0.15) : p < 0.65 ? 1 : 1 - (p - 0.65) / 0.35;
      const sc = p < 0.15 ? 0.8 + 0.2 * easeOut(p / 0.15) : 1;
      withAlpha(ctx, a, () => text(ctx, pu.label, x + c / 2, y + c / 2 - p * 0.25 * c, { font: DISPLAY, size: 0.33 * c * sc * (pu.big ? 1.5 : 1), color: C.text, glow: pu.neg ? C.leak : C.glow, glowBlur: 14 }));
    }
    if (dim > 0) { ctx.fillStyle = `rgba(7,17,28,${dim})`; ctx.fillRect(this.x - 30, this.y - 30, this.width + 60, this.height + 60); }
    ctx.restore();
  }
}
// head position from a timeline: flow starts at t0, speed in cells/second; optional slow segments
function headAt(t, t0, speed, max) { return t < t0 ? -1 : Math.min(max, (t - t0) * speed); }

// ---------- UI ----------
function chip(ctx, label, cx, cy, { color = C.ok, bg = C.okTint, size = 30, icon = null } = {}) {
  const w = textWidth(ctx, label, size, DISPLAY, 3) + 56 + (icon ? 40 : 0), h = size * 2.1;
  rr(ctx, cx - w / 2, cy - h / 2, w, h, h / 2); ctx.fillStyle = bg; ctx.fill();
  ctx.strokeStyle = color.length === 7 ? color + '55' : color; ctx.lineWidth = 2; ctx.stroke();
  if (icon) icon(ctx, cx - w / 2 + 28 + 14, cy, size * 0.8);
  text(ctx, label, cx + (icon ? 20 : 0), cy + 2, { font: DISPLAY, size, color, spacing: 3 });
}
function button(ctx, label, cx, cy, w, { variant = 'primary', h = 120, press = 0, glowA = 1, size = 52 } = {}) {
  const lip = 10 - 5 * press, dy = 5 * press;
  const x = cx - w / 2, y = cy - h / 2 + dy;
  const v = {
    primary: [C.b300, C.b500, C.b700, C.onBrass, 'rgba(245,166,35,0.55)'],
    success: [C.ok, C.ok700, C.ok700, '#FFFFFF', 'rgba(61,220,151,0.5)'],
    glass: [C.overlay, C.overlay, C.abyss, C.text, null],
  }[variant];
  ctx.save();
  if (v[4]) { ctx.shadowColor = v[4]; ctx.shadowBlur = 50 * glowA * (1 + 0.3 * press); }
  rr(ctx, x, y + lip, w, h, 36); ctx.fillStyle = v[2]; ctx.fill(); ctx.restore();
  const g = ctx.createLinearGradient(0, y, 0, y + h); g.addColorStop(0, v[0]); g.addColorStop(1, v[1]);
  rr(ctx, x, y, w, h, 36); ctx.fillStyle = g; ctx.fill();
  if (variant === 'glass') { ctx.strokeStyle = C.lineStrong; ctx.lineWidth = 2; ctx.stroke(); }
  ctx.strokeStyle = 'rgba(255,255,255,0.35)'; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(x + 36, y + 1.5); ctx.lineTo(x + w - 36, y + 1.5); ctx.stroke();
  text(ctx, label, cx, cy + dy + 3, { font: DISPLAY, size, color: v[3] });
}
function panel(ctx, x, y, w, h, r = 54) {
  ctx.save(); ctx.shadowColor = 'rgba(0,0,0,0.6)'; ctx.shadowBlur = 120; ctx.shadowOffsetY = 50;
  rr(ctx, x, y, w, h, r); ctx.fillStyle = C.base; ctx.fill(); ctx.restore();
  ctx.save(); rr(ctx, x, y, w, h, r); ctx.clip();
  ctx.strokeStyle = C.lineHi; ctx.lineWidth = 3; ctx.beginPath(); ctx.moveTo(x, y + 1.5); ctx.lineTo(x + w, y + 1.5); ctx.stroke(); ctx.restore();
}
function trophyIcon(ctx, cx, cy, s, col = C.b300) {
  ctx.save(); ctx.strokeStyle = col; ctx.lineWidth = s * 0.11; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
  ctx.beginPath(); ctx.moveTo(cx - s * 0.32, cy - s * 0.4); ctx.lineTo(cx + s * 0.32, cy - s * 0.4);
  ctx.lineTo(cx + s * 0.28, cy - s * 0.02); ctx.quadraticCurveTo(cx, cy + s * 0.22, cx - s * 0.28, cy - s * 0.02); ctx.closePath(); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(cx - s * 0.32, cy - s * 0.3); ctx.quadraticCurveTo(cx - s * 0.55, cy - s * 0.3, cx - s * 0.3, cy - s * 0.05);
  ctx.moveTo(cx + s * 0.32, cy - s * 0.3); ctx.quadraticCurveTo(cx + s * 0.55, cy - s * 0.3, cx + s * 0.3, cy - s * 0.05);
  ctx.moveTo(cx, cy + s * 0.12); ctx.lineTo(cx, cy + s * 0.32); ctx.moveTo(cx - s * 0.2, cy + s * 0.38); ctx.lineTo(cx + s * 0.2, cy + s * 0.38); ctx.stroke(); ctx.restore();
}
function dropIcon(ctx, cx, cy, s, col = C.l300) {
  ctx.save(); ctx.fillStyle = col; ctx.beginPath(); ctx.moveTo(cx, cy - s * 0.5);
  ctx.bezierCurveTo(cx + s * 0.1, cy - s * 0.25, cx + s * 0.35, cy - s * 0.05, cx + s * 0.35, cy + s * 0.15);
  ctx.arc(cx, cy + s * 0.15, s * 0.35, 0, Math.PI); ctx.bezierCurveTo(cx - s * 0.35, cy - s * 0.05, cx - s * 0.1, cy - s * 0.25, cx, cy - s * 0.5); ctx.fill(); ctx.restore();
}
function star(ctx, cx, cy, r, fill, glowOn = false) {
  ctx.save(); if (glowOn) { ctx.shadowColor = C.b500; ctx.shadowBlur = 30; }
  ctx.beginPath();
  for (let i = 0; i < 10; i++) { const an = -Math.PI / 2 + i * Math.PI / 5, rr2 = i % 2 ? r * 0.48 : r; ctx.lineTo(cx + Math.cos(an) * rr2, cy + Math.sin(an) * rr2); }
  ctx.closePath(); ctx.lineJoin = 'round';
  if (fill) { ctx.fillStyle = C.b300; ctx.fill(); } else { ctx.strokeStyle = C.lineStrong; ctx.lineWidth = r * 0.12; ctx.stroke(); }
  ctx.restore();
}
const fmt = (n) => (n < 0 ? '−' : '') + Math.abs(Math.round(n)).toLocaleString('en-US');

// Daily leaderboard (5.6). rows: [{pos, name, pts, you}] ; scaled to `s` px per dp
function leaderboard(ctx, x, y, w, rows, { s = 2.6, title = "Today's top 5", rowYOffset = null, highlightA = 1 } = {}) {
  const pad = 24 * s, headH = 17 * s + 4 * s + 31 * s + 12 * s, tableH = (22 + 6 * 30) * s;
  const h = pad * 2 + headH + tableH;
  panel(ctx, x, y, w, h, 24 * s);
  text(ctx, 'DAILY PIPE', x + w / 2, y + pad + 9 * s, { font: DISPLAY, size: 14 * s, color: C.ok, spacing: 0.5 * s });
  text(ctx, title, x + w / 2, y + pad + 21 * s + 16 * s, { font: DISPLAY, size: 28 * s, color: C.text });
  const tx = x + pad, tw = w - 2 * pad, ty = y + pad + headH;
  text(ctx, 'POSITION', tx + 8 * s, ty + 11 * s, { size: 13 * s, color: C.text2, align: 'left', spacing: 0.4 * s });
  text(ctx, 'PLAYER', tx + 8 * s + 76 * s, ty + 11 * s, { size: 13 * s, color: C.text2, align: 'left', spacing: 0.4 * s });
  text(ctx, 'POINTS', tx + tw - 8 * s, ty + 11 * s, { size: 13 * s, color: C.text2, align: 'right', spacing: 0.4 * s });
  for (const r of rows) {
    const slot = rowYOffset && r.you ? rowYOffset : r.slot;
    const ry = ty + 22 * s + slot * 30 * s;
    ctx.save(); ctx.globalAlpha *= r.alpha ?? 1;
    if (r.you) {
      ctx.save(); ctx.globalAlpha *= highlightA;
      rr(ctx, tx, ry + 1 * s, tw, 28 * s, 10 * s); ctx.fillStyle = C.lTint; ctx.fill();
      ctx.shadowColor = C.l500; ctx.shadowBlur = 24; ctx.strokeStyle = C.l500; ctx.lineWidth = 1.2 * s; ctx.stroke(); ctx.restore();
    } else if (slot > 0) {
      ctx.strokeStyle = C.lineSubtle; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(tx, ry); ctx.lineTo(tx + tw, ry); ctx.stroke();
    }
    const cy = ry + 15 * s;
    text(ctx, String(r.pos), tx + 8 * s, cy + 1.5 * s, { font: DISPLAY, size: 20 * s, color: r.pos <= 3 ? C.b300 : C.text2, align: 'left' });
    text(ctx, r.name, tx + 8 * s + 76 * s, cy, { size: 16 * s, color: C.text, align: 'left' });
    if (r.you) text(ctx, '(you)', tx + 8 * s + 76 * s + textWidth(ctx, r.name, 16 * s) + 8 * s, cy, { size: 16 * s, color: C.l300, align: 'left' });
    text(ctx, r.ptsLabel ?? fmt(r.pts), tx + tw - 8 * s, cy + 1.5 * s, { font: DISPLAY, size: 20 * s, color: C.text, align: 'right' });
    ctx.restore();
  }
  return h;
}

// Logo "Leak Out" like the feature graphic
function logo(ctx, cx, cy, size, a = 1) {
  withAlpha(ctx, a, () => {
    const w1 = textWidth(ctx, 'Leak ', size, DISPLAY), w2 = textWidth(ctx, 'Out', size, DISPLAY), x0 = cx - (w1 + w2) / 2;
    text(ctx, 'Leak ', x0, cy, { font: DISPLAY, size, color: C.text, align: 'left', shadow: 'rgba(0,0,0,0.5)' });
    ctx.save(); ctx.font = `${size}px ${DISPLAY}`; ctx.textAlign = 'left'; ctx.textBaseline = 'middle';
    const g = ctx.createLinearGradient(0, cy - size / 2, 0, cy + size / 2); g.addColorStop(0, C.glow); g.addColorStop(0.5, C.l300); g.addColorStop(1, C.l500);
    ctx.fillStyle = g; ctx.shadowColor = 'rgba(94,231,255,0.6)'; ctx.shadowBlur = size * 0.35; ctx.fillText('Out', x0 + w1, cy); ctx.restore();
  });
}
function calendarIcon(ctx, cx, cy, s, col = C.ok) {
  ctx.save(); ctx.strokeStyle = col; ctx.lineWidth = s * 0.11; ctx.lineJoin = 'round'; ctx.lineCap = 'round';
  rr(ctx, cx - s * 0.42, cy - s * 0.32, s * 0.84, s * 0.76, s * 0.14); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(cx - s * 0.42, cy - s * 0.08); ctx.lineTo(cx + s * 0.42, cy - s * 0.08);
  ctx.moveTo(cx - s * 0.2, cy - s * 0.46); ctx.lineTo(cx - s * 0.2, cy - s * 0.22); ctx.moveTo(cx + s * 0.2, cy - s * 0.46); ctx.lineTo(cx + s * 0.2, cy - s * 0.22); ctx.stroke(); ctx.restore();
}

// Shared end card. t0 = when it starts. headline: array of lines
function endCard(ctx, t, t0, { headline = ['New pipe.', 'Every day.'], cta = 'Play Now', sub = 'A new Daily Pipe drops at 00:00 UTC' } = {}) {
  if (t < t0) return;
  const p = clamp((t - t0) / 0.35);
  ctx.save(); ctx.fillStyle = `rgba(7,17,28,${0.88 * easeOut(p)})`; ctx.fillRect(0, 0, W, H); ctx.restore();
  // flowing pipe band behind the logo
  const band = clamp((t - t0) / 1.2);
  ctx.save(); ctx.globalAlpha = easeOut(p);
  const py = 560, c = 150;
  for (let i = -1; i < 8; i++) {
    const x = i * c;
    pipeSteel(ctx, x, py - c / 2, c, 'L', 'R'); flange(ctx, x, py - c / 2, c, 'L'); flange(ctx, x, py - c / 2, c, 'R');
  }
  const headX = -c + band * (W + 2 * c);
  ctx.save(); ctx.beginPath(); ctx.rect(-c, 0, headX + c, H); ctx.clip();
  for (let i = -1; i < 8; i++) liquid(ctx, i * c, py - c / 2, c, 'L', 'R', 0, 1, true);
  ctx.restore();
  if (band < 1) { ctx.save(); ctx.shadowColor = C.glow; ctx.shadowBlur = 40; ctx.fillStyle = C.glow; ctx.beginPath(); ctx.arc(headX, py, 14, 0, Math.PI * 2); ctx.fill(); ctx.restore(); }
  ctx.restore();
  const lp = pop(t, t0 + 0.15, 0.45);
  ctx.save(); ctx.translate(W / 2, 360); ctx.scale(0.8 + 0.2 * lp, 0.8 + 0.2 * lp); ctx.translate(-W / 2, -360);
  logo(ctx, W / 2, 360, 190, clamp((t - t0 - 0.1) / 0.25)); ctx.restore();
  withAlpha(ctx, clamp((t - t0 - 0.45) / 0.25), () => {
    chip(ctx, 'DAILY PIPE', W / 2, 760, { size: 40, icon: (c2, x, y, s) => calendarIcon(c2, x, y, s) });
  });
  headline.forEach((ln, i) => {
    const a = clamp((t - t0 - 0.65 - i * 0.18) / 0.25);
    withAlpha(ctx, a, () => text(ctx, ln, W / 2, 940 + i * 130 + (1 - easeOut(a)) * 30, { font: DISPLAY, size: 116, color: i === headline.length - 1 ? C.b300 : C.text, glow: i === headline.length - 1 ? 'rgba(245,166,35,0.45)' : null, glowBlur: 30 }));
  });
  const by = 940 + headline.length * 130 + 140;
  const bp = pop(t, t0 + 1.1, 0.4);
  if (t > t0 + 1.1) {
    const press = (t > t0 + 2.6 && t < t0 + 2.75) ? 1 : 0;
    const pulse = 0.75 + 0.25 * Math.sin((t - t0) * 4);
    ctx.save(); ctx.translate(W / 2, by); ctx.scale(0.92 + 0.08 * bp, 0.92 + 0.08 * bp); ctx.translate(-W / 2, -by);
    withAlpha(ctx, clamp((t - t0 - 1.1) / 0.16), () => button(ctx, cta, W / 2, by, 640, { h: 150, size: 66, press, glowA: pulse }));
    ctx.restore();
  }
  withAlpha(ctx, clamp((t - t0 - 1.4) / 0.3), () => {
    text(ctx, sub, W / 2, by + 165, { size: 40, color: C.text2 });
    text(ctx, 'Free on Android', W / 2, by + 230, { size: 36, color: C.muted });
  });
}

// caption: big centred line(s) that slide/fade in and out
function caption(ctx, t, t0, t1, lines, y, { size = 96, color = C.text, accent = null, font = DISPLAY } = {}) {
  if (t < t0 || t > t1) return;
  const a = Math.min(clamp((t - t0) / 0.25), clamp((t1 - t) / 0.2));
  lines.forEach((ln, i) => {
    const col = accent && i === lines.length - 1 ? accent : color;
    withAlpha(ctx, a, () => text(ctx, ln, W / 2, y + i * size * 1.12 + (1 - easeOut(clamp((t - t0) / 0.35))) * 40, { font, size, color: col, glow: col === color ? 'rgba(0,0,0,0.6)' : col + '88', glowBlur: 28 }));
  });
}

window.LO = { W, H, FPS, DURATION, C, DISPLAY, BODY, clamp, lerp, prog, easeOut, easeIn, easeInOut, pop, springPop, rng, rr, withAlpha, text, textWidth,
  background, Board, headAt, pipeSteel, liquid, liquidHead, flange, strokePath, valve, finish, emptyCell, burst, sparkle, chip, button, panel, trophyIcon,
  dropIcon, star, fmt, leaderboard, logo, endCard, caption, calendarIcon, cellPoint, SIDE, OPP, DIR };
