// Video 2 – "Climb the Top 5": the player's row climbs today's leaderboard from #76 to #1.
(() => {
  const { W, H, C, DISPLAY, clamp, lerp, easeOut, easeInOut, pop, text, withAlpha, background, Board, leaderboard, chip, endCard, caption, sparkle, burst, trophyIcon, fmt } = LO;
  const L = LEVEL, bc = 150;
  const bgBoard = new Board({ ...L, finishCell: true, c: bc, x: (W - L.cols * bc) / 2, y: 520 });
  const END = 11.7;
  // slots per phase; phase changes at 5.6 and 8.0 (each move takes 0.6 s)
  const players = [
    { name: 'Pipewizard', pts: 512, slots: [0, 0, 1] },
    { name: 'Ada', pts: 498, slots: [1, 1, 2] },
    { name: 'Sam', pts: 471, slots: [2, 2, 3] },
    { name: 'Fleur', pts: 455, slots: [3, 4, 4] },
    { name: 'Mo', pts: 430, slots: [4, 5, 5], out: 1 },
    { name: 'Jamie', you: true, slots: [5, 3, 0] },
  ];
  const T1 = 5.6, T2 = 8.0, MOVE = 0.6;
  const slotAt = (p, t) => {
    const a = easeInOut(clamp((t - T1) / MOVE)), b = easeInOut(clamp((t - T2) / MOVE));
    return lerp(lerp(p.slots[0], p.slots[1], a), p.slots[2], b);
  };
  const yourPts = (t) => t < 3.2 ? 125 : t < 4.2 ? lerp(125, 310, easeOut((t - 3.2) / 1)) : t < 5.0 ? 310 : t < 5.8 ? lerp(310, 460, easeOut((t - 5.0) / 0.8)) : t < 7.4 ? 460 : t < 8.0 ? lerp(460, 530, easeOut((t - 7.4) / 0.6)) : 530;
  const yourPos = (t) => t < 3.2 ? 76 : t < 4.4 ? Math.round(lerp(76, 23, easeOut((t - 3.2) / 1.2))) : t < T1 + 0.3 ? 23 : t < T2 + 0.3 ? 4 : 1;
  const attempts = [[2.9, 'ATTEMPT 2'], [4.8, 'ATTEMPT 3'], [7.1, 'ATTEMPT 4']];

  window.SCENE = {
    meta: {
      file: 'daily-pipe-02-climb-the-top-5.mp4',
      music: { file: 'bg_music_0007.mp3', start: 18, vol: 0.5 },
      sfx: [
        { t: 0.8, file: 'drop.mp3', vol: 0.6 },
        { t: 3.2, file: 'section_placement_001.mp3', vol: 0.8 },
        { t: 5.0, file: 'section_placement_002.mp3', vol: 0.8 },
        { t: 5.6, file: 'drop.mp3', vol: 0.5 },
        { t: 7.4, file: 'section_placement_003.mp3', vol: 0.8 },
        { t: 8.2, file: 'level_complete.mp3', vol: 0.8 },
        { t: END + 2.6, file: 'section_placement_001.mp3', vol: 0.6 },
      ],
    },
    draw(ctx, t) {
      background(ctx, t, { seed: 2 });
      // paused board behind the see-through scrim (the leaderboard opens over it)
      bgBoard.draw(ctx, t, { placeT: [], head: -1 });
      ctx.fillStyle = 'rgba(7,17,28,0.72)'; ctx.fillRect(0, 0, W, H);

      caption(ctx, t, 0.1, 2.7, ['Today’s top 5.'], 250, { size: 104 });
      caption(ctx, t, 0.9, 2.7, ['Is your name on it?'], 1640, { size: 84, color: C.b300, accent: C.b300 });
      caption(ctx, t, 3.0, 4.7, ['Play again.'], 300, { size: 104 });
      caption(ctx, t, 4.9, 7.1, ['Find a better route.'], 300, { size: 96 });
      caption(ctx, t, 7.2, END, ['Take the crown.'], 300, { size: 104, accent: C.b300, color: C.b300 });

      // panel
      const p = pop(t, 0.5, 0.4);
      if (t > 0.5) {
        const pw = 1000, px = (W - pw) / 2, py = 520;
        ctx.save(); ctx.globalAlpha = clamp((t - 0.5) / 0.16);
        ctx.translate(W / 2, py + 420); ctx.scale(0.92 + 0.08 * p, 0.92 + 0.08 * p); ctx.translate(-W / 2, -(py + 420));
        const rows = players.map((pl) => {
          const slot = slotAt(pl, t);
          const alpha = pl.out != null && t > T1 ? 1 - clamp((t - T1) / 0.4) : (pl.name === 'Fleur' && t > T2 ? 1 - clamp((t - T2) / 0.4) : 1);
          return pl.you
            ? { pos: yourPos(t), name: pl.name, pts: yourPts(t), you: true, slot, alpha: 1 }
            : { pos: Math.round(slot) + 1, name: pl.name, pts: pl.pts, slot, alpha };
        });
        // draw others first, you on top
        rows.sort((a, b) => (a.you ? 1 : 0) - (b.you ? 1 : 0));
        const s = 2.85;
        leaderboard(ctx, px, py, pw, rows, { s });
        // celebration at #1
        if (t > T2 + 0.3) {
          const ty = py + (24 + 17 + 4 + 31 + 12 + 22 + 15) * s;
          sparkle(ctx, px + 24 * s + 30 * s, ty, T2 + 0.3, t, 3, 8);
          burst(ctx, W / 2, ty, T2 + 0.35, t, { n: 26, cols: [C.b300, C.glow, C.b500], dist: 520, size: 12, dur: 1.0, seed: 9 });
        }
        ctx.restore();
        // "+points" popups beside the panel
        [[3.2, '+185'], [5.0, '+150'], [7.4, '+70']].forEach(([t0, lbl]) => {
          const q = (t - t0) / 1.1; if (q < 0 || q > 1) return;
          const a = q < 0.15 ? q / 0.15 : q > 0.7 ? (1 - q) / 0.3 : 1;
          const yy = py + (24 + 17 + 4 + 31 + 12 + 22) * s + slotAt(players[5], t) * 30 * s - 40 - q * 80;
          withAlpha(ctx, a, () => text(ctx, lbl, W / 2 + 190, yy, { font: DISPLAY, size: 72, color: C.text, glow: C.glow, glowBlur: 22 }));
        });
        // attempt chip
        attempts.forEach(([t0, lbl], i) => {
          const t1 = i < attempts.length - 1 ? attempts[i + 1][0] : T2 + 0.5;
          if (t < t0 || t > t1) return;
          withAlpha(ctx, Math.min(clamp((t - t0) / 0.2), clamp((t1 - t) / 0.2)), () => chip(ctx, lbl, W / 2, 1500, { size: 36, color: C.l300, bg: C.lTint }));
        });
        if (t > T2 + 0.6) {
          withAlpha(ctx, clamp((t - T2 - 0.6) / 0.3), () => {
            trophyIcon(ctx, W / 2, 1530, 110);
            text(ctx, '#1 for today', W / 2, 1660, { font: DISPLAY, size: 80, color: C.b300, glow: 'rgba(245,166,35,0.5)', glowBlur: 30 });
          });
        }
      }
      endCard(ctx, t, END, { headline: ['Climb today’s', 'top 5.'], cta: 'Play Now' });
    },
  };
})();
