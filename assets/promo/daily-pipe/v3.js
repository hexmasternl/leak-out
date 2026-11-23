// Video 3 – "24 Hours. One Pipe.": a speedrun with the HUD while the day's timer races to zero.
(() => {
  const { W, H, C, DISPLAY, BODY, clamp, lerp, easeOut, easeInOut, pop, text, withAlpha, background, Board, headAt, chip, endCard, caption,
    sparkle, burst, rr, fmt, pipeSteel, liquid, flange, strokePath, calendarIcon } = LO;
  const L = LEVEL, c = 140;
  const route = L.crossRoute;
  const board = new Board({ ...L, route, finishCell: true, c, x: (W - L.cols * c) / 2, y: 560 });
  const sch = schedule(route, 2.0, 0.24, 5.0, 3.6);
  const WIN = sch.end, ZERO = 11.5, END = 12.3;
  const crossIdx = route.findIndex((p, i) => i > 0 && route.slice(0, i).some((q) => q[0] === p[0] && q[1] === p[1]));
  // scoring: +10 per section, +50 for the second pass through the cross
  const awards = [];
  route.forEach(([col, row], i) => {
    if (i === 0 || i === route.length - 1) return;
    const pts = i === crossIdx ? 50 : 10;
    const tt = sch.tFlow + (i + 1) / sch.speed;
    awards.push({ t: tt, pts }); board.popups.push({ col, row, t: tt, label: '+' + pts, big: pts === 50 });
  });
  const scoreAt = (t) => awards.reduce((s, a) => s + (t >= a.t ? a.pts : 0), 0);
  const STARS = [80, 150, 220];
  // the stack: pieces in placement order
  const pieces = [];
  sch.placeT.forEach((pt, i) => { if (pt == null) return; const k = route[i].join(','); pieces.push({ t: pt, chans: board.cells.get(k).steps.map((s) => board.chan[s]) }); });

  function drawPiece(ctx, x, y, s, chans) {
    for (const ch of chans) {
      if (chans.length === 2 && (ch.a === 'L' || ch.a === 'R')) strokePath(ctx, x, y, s, ch.a, ch.b, 0.46, C.deep);
      pipeSteel(ctx, x, y, s, ch.a, ch.b); flange(ctx, x, y, s, ch.a); flange(ctx, x, y, s, ch.b);
    }
  }
  function stack(ctx, t) {
    const placed = pieces.filter((p) => t >= p.t).length;
    const rest = pieces.slice(placed);
    if (!rest.length) return;
    const y = board.y + board.height + 70, s0 = 120, s1 = 96, gap = 18;
    // slide progress since the last placement (motion.fall-ish, sideways)
    const last = placed > 0 ? pieces[placed - 1].t : -9;
    const sl = 1 - easeOut(clamp((t - last) / 0.2));
    rr(ctx, 60, y - 16, W - 120, s0 + 32, 36); ctx.fillStyle = 'rgba(12,26,42,0.9)'; ctx.fill(); ctx.strokeStyle = C.lineStrong; ctx.lineWidth = 2; ctx.stroke();
    ctx.save(); rr(ctx, 60, y - 16, W - 120, s0 + 32, 36); ctx.clip();
    let x = 90 + sl * (s1 + gap);
    rest.slice(0, 9).forEach((p, i) => {
      const size = i === 0 ? s0 : s1, yy = y + (s0 - size) / 2;
      if (i === 0) {
        ctx.save(); ctx.shadowColor = 'rgba(245,166,35,0.6)'; ctx.shadowBlur = 30 * (0.7 + 0.3 * Math.sin(t * Math.PI));
        rr(ctx, x, yy, size, size, 26); ctx.fillStyle = 'rgba(245,166,35,0.28)'; ctx.fill(); ctx.restore();
        rr(ctx, x, yy, size, size, 26); ctx.strokeStyle = C.b500; ctx.lineWidth = 5; ctx.stroke();
      } else {
        rr(ctx, x, yy, size, size, 22); ctx.fillStyle = 'rgba(25,50,74,0.6)'; ctx.fill(); ctx.strokeStyle = C.lineStrong; ctx.lineWidth = 2; ctx.stroke();
      }
      drawPiece(ctx, x, yy, size, p.chans);
      x += size + gap;
    });
    ctx.restore();

  }
  function hud(ctx, t) {
    const s = 2.3, x = 40, w = W - 80, y = board.y - 210, h = 48 * s;
    const g = ctx.createLinearGradient(0, y, 0, y + h); g.addColorStop(0, C.raised); g.addColorStop(1, C.deep);
    ctx.save(); ctx.shadowColor = 'rgba(0,0,0,0.35)'; ctx.shadowBlur = 24; ctx.shadowOffsetY = 8; rr(ctx, x, y, w, h, 24 * s); ctx.fillStyle = g; ctx.fill(); ctx.restore();
    ctx.strokeStyle = C.lineHi; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(x + 50, y + 1); ctx.lineTo(x + w - 50, y + 1); ctx.stroke();
    const cy = y + h / 2;
    text(ctx, 'Daily Pipe', x + 16 * s, cy + 3, { font: DISPLAY, size: 18 * s, align: 'left' });
    // flow speed
    const sx = x + 16 * s + 120 * s;
    ctx.save(); ctx.strokeStyle = C.l300; ctx.lineWidth = 2 * s; ctx.lineCap = 'round'; ctx.beginPath();
    for (let k = 0; k <= 20; k++) { const xx = sx - 22 + k * 2.2, yy = cy - 6 + Math.sin(k / 3) * 6; k ? ctx.lineTo(xx, yy) : ctx.moveTo(xx, yy); }
    ctx.stroke(); ctx.restore();
    text(ctx, '36', sx + 34, cy + 3, { font: DISPLAY, size: 18 * s, color: C.l300, align: 'left' });
    // countdown bar
    const cbx = sx + 34 + 34 * s, sbw = 200 * s * 0.85, sbx = x + w - 16 * s - 30 * s - sbw - 12 * s;
    const cbw = sbx - 12 * s - cbx;
    const rem = clamp(1 - (t - 2.0) / (sch.tFlow - 2.0)), remS = (sch.tFlow - t) * 2;
    const fade = 1 - clamp((t - sch.tFlow) / 0.16);
    if (fade > 0) withAlpha(ctx, fade, () => {
      rr(ctx, cbx, cy - 4 * s, cbw, 8 * s, 4 * s); ctx.fillStyle = C.lineSubtle; ctx.fill();
      const col = remS > 5 ? C.l500 : remS > 2 ? C.b500 : C.leak;
      ctx.save(); ctx.shadowColor = col; ctx.shadowBlur = 16; rr(ctx, cbx, cy - 4 * s, Math.max(8 * s, cbw * rem), 8 * s, 4 * s); ctx.fillStyle = col; ctx.fill(); ctx.restore();
    });
    // score bar
    const score = scoreAt(t);
    ctx.save(); rr(ctx, sbx, cy - 10 * s, sbw, 20 * s, 10 * s); ctx.fillStyle = C.abyss; ctx.fill(); ctx.strokeStyle = C.lineSubtle; ctx.lineWidth = 2; ctx.stroke(); ctx.clip();
    const fillP = clamp(score / STARS[2]);
    const fg = ctx.createLinearGradient(sbx, 0, sbx + sbw, 0); fg.addColorStop(0, C.b500); fg.addColorStop(1, C.b300);
    ctx.fillStyle = fg; ctx.fillRect(sbx, cy - 10 * s, sbw * fillP, 20 * s);
    ctx.fillStyle = C.lineStrong; for (const m of STARS.slice(0, 2)) ctx.fillRect(sbx + sbw * m / STARS[2] - 2, cy - 10 * s, 4, 20 * s);
    ctx.restore();
    text(ctx, fmt(score), sbx + sbw / 2, cy + 3, { font: DISPLAY, size: 20 * s, shadow: C.abyss });
    STARS.forEach((m, i) => { const at = awards.find((a) => scoreAt(a.t) >= m); if (at) sparkle(ctx, sbx + sbw * m / STARS[2], cy, at.t, t, 1.6, 30 + i); });
    // pause cog
    const px = x + w - 16 * s - 12 * s; ctx.save(); ctx.strokeStyle = C.text; ctx.lineWidth = 2 * s; ctx.beginPath(); ctx.arc(px, cy, 8 * s, 0, Math.PI * 2); ctx.stroke();
    for (let k = 0; k < 8; k++) { const an = k * Math.PI / 4; ctx.beginPath(); ctx.moveTo(px + Math.cos(an) * 8 * s, cy + Math.sin(an) * 8 * s); ctx.lineTo(px + Math.cos(an) * 11 * s, cy + Math.sin(an) * 11 * s); ctx.stroke(); }
    ctx.restore();
  }
  function dayTimer(ctx, t) {
    const remain = Math.max(0, Math.round(86400 * (1 - Math.pow(clamp((t - 1.5) / (ZERO - 1.5)), 1.4))));
    const hh = String(Math.floor(remain / 3600)).padStart(2, '0'), mm = String(Math.floor(remain / 60) % 60).padStart(2, '0'), ss = String(remain % 60).padStart(2, '0');
    const zero = remain === 0;
    const big = 1 - easeInOut(clamp((t - 1.5) / 0.5)) + (zero ? easeOut(clamp((t - ZERO) / 0.3)) * 0 : 0);
    const cy = lerp(160, 760, big), size = lerp(96, 200, big);
    const col = zero ? C.leak : remain < 3600 ? C.b300 : C.text;
    const str = `${hh}:${mm}:${ss}`, dw = size * 0.62, colonW = size * 0.3;
    let x = -[...str].reduce((s, ch) => s + (ch === ':' ? colonW : dw), 0) / 2;
    ctx.save(); ctx.translate(W / 2, cy);
    if (zero) { const k = 1 + 0.08 * Math.max(0, 1 - (t - ZERO) / 0.3); ctx.scale(k, k); }
    for (const ch of str) { const w = ch === ':' ? colonW : dw; text(ctx, ch, x + w / 2, 0, { font: DISPLAY, size, color: col, glow: zero ? 'rgba(255,90,95,0.6)' : 'rgba(157,243,255,0.25)', glowBlur: 36 }); x += w; }
    ctx.restore();
    withAlpha(ctx, 1 - big, () => text(ctx, zero ? 'TODAY’S PIPE HAS EXPIRED' : 'TODAY’S PIPE EXPIRES IN', W / 2, cy - 82, { size: 34, color: zero ? C.leak : C.text2, spacing: 4 }));
  }

  window.SCENE = {
    meta: {
      file: 'daily-pipe-03-24-hours.mp4',
      music: { file: 'bg_music_0009.mp3', start: 24, vol: 0.5 },
      sfx: [
        ...sch.places.map((t, i) => ({ t, file: `section_placement_00${(i % 3) + 1}.mp3`, vol: 0.5 })),
        { t: WIN, file: 'level_complete.mp3', vol: 0.8 },
        { t: ZERO, file: 'section_placement_failed.mp3', vol: 0.9 },
        { t: 9.6, file: 'heartbeat.mp3', vol: 0.5 },
        { t: END + 2.6, file: 'section_placement_001.mp3', vol: 0.6 },
      ],
    },
    draw(ctx, t) {
      background(ctx, t, { seed: 3 });
      caption(ctx, t, 0.0, 1.55, ['You have'], 520, { size: 96, color: C.text2 });
      caption(ctx, t, 0.25, 1.55, ['24 hours.'], 1000, { size: 150, color: C.b300, accent: C.b300 });
      if (t > 1.6) {
        const a = clamp((t - 1.6) / 0.3);
        const fadeOut = t > ZERO ? 1 - 0.75 * clamp((t - ZERO) / 0.4) : 1;
        withAlpha(ctx, a * fadeOut, () => {
          hud(ctx, t);
          board.draw(ctx, t, { placeT: sch.placeT, head: headAt(t, sch.tFlow, sch.speed, route.length), won: t >= WIN, appear: (col, row) => 1.6 + row * 0.04 + col * 0.03 });
          if (t >= WIN) burst(ctx, board.x + 5.5 * c, board.y + 6.5 * c, WIN, t, { n: 18, cols: [C.ok, C.b300, C.glow], dist: 220, size: 12, seed: 4 });
          stack(ctx, t);
        });
        caption(ctx, t, 1.9, 4.9, ['One pipe.'], 1845, { size: 92 });
        caption(ctx, t, 5.0, 8.1, ['Same one for everyone.'], 1845, { size: 84 });
        caption(ctx, t, 8.2, ZERO - 0.1, ['Make it count.'], 1845, { size: 92, color: C.b300, accent: C.b300 });
        caption(ctx, t, ZERO + 0.05, END + 0.2, ['Then it’s gone.'], 1080, { size: 120, color: C.leak, accent: C.leak });
      }
      dayTimer(ctx, t);
      endCard(ctx, t, END, { headline: ['24 hours.', 'Your best shot.'], cta: 'Play Now' });
    },
  };
})();
