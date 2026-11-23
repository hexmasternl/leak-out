// Video 5 – "One More Try": the first attempt leaks, the retry finds the +50 cross and sets a new best.
(() => {
  const { W, H, C, DISPLAY, BODY, clamp, lerp, easeOut, pop, text, withAlpha, background, Board, headAt, panel, button, star, endCard, caption,
    burst, sparkle, trophyIcon, fmt, chip } = LO;
  const L = LEVEL, c = 150, bx = (W - L.cols * c) / 2, by = 560;
  // attempt 1: everything placed already, the liquid runs into the edge
  const leakBoard = new Board({ ...L, route: L.leakRoute, finishCell: false, c, x: bx, y: by });
  const leakPlace = L.leakRoute.map((_, i) => (i === 0 ? undefined : -1));
  const LEAK = (L.leakRoute.length - 3) / 3.2, LEAK_DLG = LEAK + 0.6, RESTART = 4.05, CLOSE = 4.25;
  L.leakRoute.forEach(([col, row], i) => { if (i > 0 && i < L.leakRoute.length - 1) { const tt = (i + 1 - 3) / 3.2; if (tt > 0) leakBoard.popups.push({ col, row, t: tt, label: '+10' }); } });
  // attempt 2
  const route = L.crossRoute;
  const board = new Board({ ...L, route, finishCell: true, c, x: bx, y: by });
  const sch = schedule(route, 4.4, 0.16, 5.4, 5.2);
  const crossIdx = route.findIndex((p, i) => i > 0 && route.slice(0, i).some((q) => q[0] === p[0] && q[1] === p[1]));
  let total = 0;
  route.forEach(([col, row], i) => {
    if (i === 0 || i === route.length - 1) return;
    const pts = i === crossIdx ? 50 : 10; total += pts;
    board.popups.push({ col, row, t: sch.tFlow + (i + 1) / sch.speed, label: '+' + pts, big: pts === 50 });
  });
  const crossT = sch.tFlow + (crossIdx + 1) / sch.speed;
  const WIN = sch.end, WIN_DLG = WIN + 0.35, END = 12.4;

  function leakDialog(ctx, t) {
    if (t < LEAK_DLG || t > CLOSE) return;
    const s = 2.6, w = 940, x = (W - w) / 2, y = 620, h = 760;
    ctx.fillStyle = `rgba(7,17,28,${0.72 * clamp((t - LEAK_DLG) / 0.16)})`; ctx.fillRect(0, 0, W, H);
    const p = pop(t, LEAK_DLG, 0.32);
    ctx.save(); ctx.translate(W / 2, y + h / 2); ctx.scale(0.92 + 0.08 * p, 0.92 + 0.08 * p); ctx.translate(-W / 2, -(y + h / 2));
    panel(ctx, x, y, w, h, 24 * s);
    text(ctx, 'DAILY PIPE', W / 2, y + 90, { font: DISPLAY, size: 14 * s, color: C.leak, spacing: 0.5 * s });
    // splash: blob + 6 droplets flying out once
    const sp = easeOut(clamp((t - LEAK_DLG) / 0.6)), scx = W / 2, scy = y + 200;
    ctx.fillStyle = C.leak; ctx.beginPath(); ctx.arc(scx, scy, 34, 0, Math.PI * 2); ctx.fill();
    for (let i = 0; i < 6; i++) { const an = -Math.PI / 2 + (i - 2.5) * 0.55; ctx.fillStyle = i % 2 ? C.glow : C.l500; ctx.beginPath(); ctx.arc(scx + Math.cos(an) * 80 * sp, scy + Math.sin(an) * 70 * sp + 30, 12, 0, Math.PI * 2); ctx.fill(); }
    text(ctx, 'Leaked!', W / 2, y + 330, { font: DISPLAY, size: 40 * s, color: C.text });
    text(ctx, 'The pipe is leaking. Level lost.', W / 2, y + 420, { size: 16 * s, color: C.text2 });
    text(ctx, 'SCORE', W / 2, y + 490, { size: 13 * s, color: C.text2, spacing: 0.4 * s });
    text(ctx, '70', W / 2, y + 550, { font: DISPLAY, size: 32 * s, color: C.b300 });
    const press = t > RESTART && t < RESTART + 0.12 ? 1 : 0;
    button(ctx, 'Restart', W / 2, y + 660, w - 48 * s, { h: 130, size: 54, press });
    ctx.restore();
  }
  function winDialog(ctx, t) {
    if (t < WIN_DLG) return;
    const s = 2.6, w = 940, x = (W - w) / 2, y = 560, h = 900;
    ctx.fillStyle = `rgba(7,17,28,${0.72 * clamp((t - WIN_DLG) / 0.16)})`; ctx.fillRect(0, 0, W, H);
    const p = pop(t, WIN_DLG, 0.32);
    ctx.save(); ctx.translate(W / 2, y + h / 2); ctx.scale(0.92 + 0.08 * p, 0.92 + 0.08 * p); ctx.translate(-W / 2, -(y + h / 2));
    panel(ctx, x, y, w, h, 24 * s);
    text(ctx, 'DAILY PIPE', W / 2, y + 90, { font: DISPLAY, size: 14 * s, color: C.ok, spacing: 0.5 * s });
    text(ctx, 'Level Complete!', W / 2, y + 180, { font: DISPLAY, size: 40 * s, color: C.text });
    const tallyEnd = WIN_DLG + 1.0;
    if (t < tallyEnd) text(ctx, 'You kept the water flowing.', W / 2, y + 265, { size: 16 * s, color: C.text2 });
    else text(ctx, 'New best!', W / 2, y + 265, { font: DISPLAY, size: 18 * s * (1 + 0.15 * (1 - clamp((t - tallyEnd) / 0.3))), color: C.b300, glow: 'rgba(245,166,35,0.5)', glowBlur: 24 });
    for (let i = 0; i < 3; i++) {
      const st = tallyEnd + 0.1 + i * 0.08, sc = t > st ? 0.4 + 0.6 * pop(t, st, 0.32) : 1;
      const sx = W / 2 + (i - 1) * (40 + 12) * s, sy = y + 375;
      ctx.save(); ctx.translate(sx, sy); ctx.scale(sc, sc); star(ctx, 0, 0, 20 * s, t > st, t > st); ctx.restore();
      if (t > st) burst(ctx, sx, sy, st, t, { n: 8, cols: [C.b300, C.b100], dist: 90, size: 7, dur: 0.5, seed: 40 + i });
    }
    // score row
    const cols = [['POINTS', fmt(total), C.text], ['PENALTY', '0', C.muted], ['SCORE', fmt(lerp(0, total, easeOut(clamp((t - WIN_DLG - 0.3) / 0.7)))), C.b300]];
    cols.forEach(([cap, val, col], i) => {
      const cx = x + w * (i + 0.5) / 3;
      text(ctx, cap, cx, y + 480, { size: 13 * s, color: C.text2, spacing: 0.4 * s });
      text(ctx, val, cx, y + 545, { font: DISPLAY, size: 32 * s, color: col, glow: i === 2 && t > tallyEnd ? 'rgba(245,166,35,0.5)' : null, glowBlur: 30 });
    });
    button(ctx, 'Next', W / 2, y + 700, w - 48 * s, { variant: 'success', h: 130, size: 54 });
    ctx.restore();
    if (t > tallyEnd + 0.7) {
      const a = clamp((t - tallyEnd - 0.7) / 0.25);
      withAlpha(ctx, a, () => {
        trophyIcon(ctx, W / 2 - 250, 1600, 90);
        text(ctx, '#3 today', W / 2 + 40, 1600, { font: DISPLAY, size: 96, color: C.b300, glow: 'rgba(245,166,35,0.5)', glowBlur: 30 });
      });
      sparkle(ctx, W / 2 - 250, 1600, tallyEnd + 0.7, t, 2.5, 12);
    }
  }

  window.SCENE = {
    meta: {
      file: 'daily-pipe-05-one-more-try.mp4',
      music: { file: 'bg_music_0006.mp3', start: 12, vol: 0.45 },
      sfx: [
        { t: LEAK, file: 'level_failed.mp3', vol: 0.9 },
        { t: RESTART, file: 'section_placement_001.mp3', vol: 0.7 },
        ...sch.places.map((t, i) => ({ t, file: `section_placement_00${(i % 3) + 1}.mp3`, vol: 0.45 })),
        { t: crossT, file: 'drop.mp3', vol: 0.8 },
        { t: WIN, file: 'level_complete.mp3', vol: 0.85 },
        { t: END + 2.6, file: 'section_placement_001.mp3', vol: 0.6 },
      ],
    },
    draw(ctx, t) {
      background(ctx, t, { seed: 5 });
      if (t < CLOSE) {
        leakBoard.draw(ctx, t, { placeT: leakPlace, head: Math.min(L.leakRoute.length, 3 + t * 3.2), leakT: LEAK, shakeT: LEAK });
        caption(ctx, t, 0.0, LEAK_DLG, ['So close…'], 300, { size: 120 });
        leakDialog(ctx, t);
      } else {
        board.draw(ctx, t, { placeT: sch.placeT, head: headAt(t, sch.tFlow, sch.speed, route.length), won: t >= WIN, appear: (col, row) => CLOSE + 0.02 * (row + col) - 0.3 });
        if (t >= WIN) burst(ctx, bx + 5.5 * c, by + 6.5 * c, WIN, t, { n: 18, cols: [C.ok, C.b300, C.glow], dist: 220, size: 12, seed: 4 });
        if (t > crossT) burst(ctx, bx + 2.5 * c, by + 2.5 * c, crossT, t, { n: 16, cols: [C.b300, C.glow], dist: 200, size: 10, seed: 14 });
      }
      caption(ctx, t, RESTART - 0.6, 6.0, ['One more try.'], 300, { size: 120, color: C.b300, accent: C.b300 });
      caption(ctx, t, 6.1, WIN_DLG, ['Go for the cross.'], 300, { size: 104 });
      withAlpha(ctx, Math.min(clamp((t - 6.3) / 0.3), clamp((WIN_DLG - t) / 0.2)), () => chip(ctx, 'SECOND PASS  +50', W / 2, 440, { size: 34, color: C.b300, bg: C.bTint }));
      winDialog(ctx, t);
      endCard(ctx, t, END, { headline: ['Today’s pipe', 'is waiting.'], cta: 'Play Now' });
    },
  };
})();
