// Video 1 – "Midnight Drop": the clock hits 00:00 UTC, a new pipe drops in, it gets solved.
(() => {
  const { W, H, C, DISPLAY, clamp, easeOut, easeInOut, pop, text, withAlpha, background, Board, headAt, chip, calendarIcon, endCard, caption, burst, rr, fmt } = LO;
  const L = LEVEL, c = 150;
  const board = new Board({ ...L, finishCell: true, c, x: (W - L.cols * c) / 2, y: 640 });
  const sch = schedule(L.route, 6.3, 0.27, 7.5, 4.2);
  const WIN = sch.end, END = 11.9;
  // points popups as the liquid passes each section
  L.route.forEach(([col, row], i) => { if (sch.placeT[i] != null) board.popups.push({ col, row, t: sch.tFlow + (i + 1) / sch.speed, label: '+10' }); });
  const appear = (col, row) => 3.45 + (row * 0.07 + col * 0.045);

  function clock(ctx, t) {
    // 23:59:57 -> 00:00:00 at t = 3.0
    const sec = t < 0.2 ? 57 : t < 1.2 ? 57 : t < 2.2 ? 58 : t < 3.0 ? 59 : 0;
    const str = sec === 0 ? '00:00:00' : `23:59:${sec}`;
    const shrink = easeInOut(clamp((t - 3.4) / 0.7));
    const cy = 860 - shrink * 560, size = 200 - shrink * 110;
    const flash = clamp(1 - (t - 3.0) / 0.8);
    const col = t >= 3.0 ? C.b300 : C.text;
    // tick pulse
    const tickP = [0.2, 1.2, 2.2, 3.0].reduce((m, tk) => (t >= tk && t - tk < 0.25 ? 1 - (t - tk) / 0.25 : m), 0);
    ctx.save(); ctx.translate(W / 2, cy); ctx.scale(1 + 0.04 * tickP, 1 + 0.04 * tickP);
    // fixed-width digits (3.2)
    const dw = size * 0.62, colonW = size * 0.3;
    const chars = str.split(''); const total = chars.reduce((s, ch) => s + (ch === ':' ? colonW : dw), 0);
    let x = -total / 2;
    for (const ch of chars) {
      const w = ch === ':' ? colonW : dw;
      text(ctx, ch, x + w / 2, 0, { font: DISPLAY, size, color: col, glow: t >= 3.0 ? 'rgba(245,166,35,' + (0.4 + 0.5 * flash) + ')' : 'rgba(157,243,255,0.25)', glowBlur: 40 });
      x += w;
    }
    ctx.restore();
    withAlpha(ctx, 1 - shrink, () => text(ctx, 'UTC', W / 2, cy + 150, { font: DISPLAY, size: 60, color: C.text2, spacing: 8 }));
    if (t >= 3.0) burst(ctx, W / 2, cy, 3.0, t, { n: 22, cols: [C.b300, C.glow, C.b500], dist: 420, size: 14, dur: 0.9, seed: 21 });
  }

  window.SCENE = {
    meta: {
      file: 'daily-pipe-01-midnight-drop.mp4',
      music: { file: 'bg_music_0001.mp3', start: 6, vol: 0.5 },
      sfx: [
        ...[0.2, 1.2, 2.2].map((t) => ({ t, file: 'drop.mp3', vol: 0.55 })),
        { t: 3.0, file: 'section_placement_003.mp3', vol: 1 },
        ...sch.places.map((t, i) => ({ t, file: `section_placement_00${(i % 3) + 1}.mp3`, vol: 0.55 })),
        { t: WIN, file: 'level_complete.mp3', vol: 0.8 },
        { t: END + 2.6, file: 'section_placement_001.mp3', vol: 0.6 },
      ],
    },
    draw(ctx, t) {
      background(ctx, t, { seed: 1 });
      // intro copy
      caption(ctx, t, 0.0, 2.95, ['Every night at'], 520, { size: 84, color: C.text2 });
      caption(ctx, t, 0.6, 2.95, ['midnight…'], 1130, { size: 110 });
      clock(ctx, t);
      if (t >= 3.4) {
        withAlpha(ctx, clamp((t - 3.6) / 0.3), () => chip(ctx, 'DAILY PIPE  ·  NOV 23', W / 2, 440, { size: 38, icon: (c2, x, y, s) => calendarIcon(c2, x, y, s) }));
        const head = headAt(t, sch.tFlow, sch.speed, L.route.length);
        board.draw(ctx, t, { placeT: sch.placeT, head, won: t >= WIN, appear });
        if (t >= WIN) burst(ctx, board.x + 5.5 * c, board.y + 6.5 * c, WIN, t, { n: 18, cols: [C.ok, C.b300, C.glow], dist: 220, size: 12, seed: 4 });
        // live score
        const passed = Math.max(0, Math.min(14, Math.floor((t - sch.tFlow) * sch.speed - 0.0001)));
        withAlpha(ctx, clamp((t - 7.3) / 0.3), () => {
          text(ctx, 'SCORE', W - 110, 1790, { size: 34, color: C.text2, align: 'right', spacing: 3 });
          text(ctx, fmt(passed * 10), W - 110, 1850, { font: DISPLAY, size: 64, color: C.b300, align: 'right' });
        });
        caption(ctx, t, 4.0, 6.9, ['A brand-new pipe', 'drops.'], 1760, { size: 78, accent: C.l300 });
        caption(ctx, t, 7.2, END, ['Solve it.'], 1800, { size: 84, accent: C.text });
      }
      endCard(ctx, t, END, { headline: ['New pipe.', 'Every day.'], cta: 'Play Today’s Pipe' });
    },
  };
})();
