// Video 4 – "Same Pipe for Everyone": nine players race the same Daily Pipe; only five make the board.
(() => {
  const { W, H, C, DISPLAY, clamp, easeOut, pop, text, withAlpha, background, Board, headAt, leaderboard, endCard, caption, burst, rr, fmt } = LO;
  const L = LEVEL, c = 46, bw = L.cols * c, bh = L.rows * c, gapX = 66, gapY = 112;
  const x0 = (W - (3 * bw + 2 * gapX)) / 2, y0 = 430;
  const P = [
    { name: 'Ada', r: 'cross', place: 1.7, gap: 0.2, flow: 3.6, speed: 4.2, score: 498 },
    { name: 'Linde', r: 'leak', place: 1.9, gap: 0.28, flow: 3.9, speed: 3.0 },
    { name: 'Sam', r: 'clean', place: 2.0, gap: 0.22, flow: 3.8, speed: 3.8, score: 471 },
    { name: 'Kees', r: 'leak', place: 1.8, gap: 0.3, flow: 4.4, speed: 2.6 },
    { name: 'Pipewizard', r: 'cross', place: 1.6, gap: 0.17, flow: 3.0, speed: 4.6, score: 512 },
    { name: 'Fleur', r: 'clean', place: 2.1, gap: 0.24, flow: 4.0, speed: 3.6, score: 455 },
    { name: 'Mo', r: 'clean', place: 2.2, gap: 0.26, flow: 4.2, speed: 3.8, score: 430 },
    { name: 'Ravi', r: 'leak', place: 2.0, gap: 0.25, flow: 4.2, speed: 3.4 },
    { name: 'Joy', r: 'clean', place: 2.3, gap: 0.3, flow: 4.4, speed: 4.0, score: 388 },
  ].map((p, i) => {
    const route = p.r === 'cross' ? L.crossRoute : p.r === 'leak' ? L.leakRoute : L.route;
    const won = p.r !== 'leak';
    const col = i % 3, row = Math.floor(i / 3);
    const board = new Board({ ...L, route, finishCell: won, c, x: x0 + col * (bw + gapX), y: y0 + row * (bh + gapY) });
    const sch = schedule(route, p.place, p.gap, p.flow, p.speed, won);
    return { ...p, i, board, sch, won, route, endT: won ? sch.end : p.flow + route.length / p.speed };
  });
  const LB = 9.0, END = 11.7;
  const top5 = P.filter((p) => p.won).sort((a, b) => b.score - a.score).slice(0, 5);

  window.SCENE = {
    meta: {
      file: 'daily-pipe-04-same-pipe-for-everyone.mp4',
      music: { file: 'bg_music_0004.mp3', start: 30, vol: 0.5 },
      sfx: [
        ...[1.6, 2.1, 2.6, 3.1, 3.6].map((t, i) => ({ t, file: `section_placement_00${(i % 3) + 1}.mp3`, vol: 0.45 })),
        ...P.filter((p) => !p.won).map((p) => ({ t: p.endT, file: 'level_failed.mp3', vol: 0.35 })),
        ...P.filter((p) => p.won && p.score > 450).map((p) => ({ t: p.endT, file: 'drop.mp3', vol: 0.5 })),
        { t: LB, file: 'level_complete.mp3', vol: 0.7 },
        { t: END + 2.6, file: 'section_placement_001.mp3', vol: 0.6 },
      ],
    },
    draw(ctx, t) {
      background(ctx, t, { seed: 4 });
      caption(ctx, t, 0.1, 2.9, ['One pipe.'], 260, { size: 110 });
      caption(ctx, t, 3.0, LB - 0.2, ['Same pipe', 'for everyone.'], 200, { size: 96, accent: C.l300 });
      const mos = 1 - 0.8 * clamp((t - LB + 0.4) / 0.4);
      withAlpha(ctx, mos, () => {
        for (const p of P) {
          const appear = 0.25 + p.i * 0.08;
          const a = clamp((t - appear) / 0.25), sc = 0.9 + 0.1 * pop(t, appear, 0.4);
          if (a <= 0) continue;
          const b = p.board, cx = b.x + bw / 2, cy = b.y + bh / 2;
          ctx.save(); ctx.globalAlpha *= a; ctx.translate(cx, cy); ctx.scale(sc, sc); ctx.translate(-cx, -cy);
          const head = headAt(t, p.flow, p.speed, p.route.length);
          const leaked = !p.won && t >= p.endT;
          const won = p.won && t >= p.endT;
          b.draw(ctx, t, { placeT: p.sch.placeT, head, won, leakT: p.won ? null : p.endT, shakeT: p.won ? null : p.endT });
          // result tint
          if (leaked) { rr(ctx, b.x - 6, b.y - 6, bw + 12, bh + 12, 16); ctx.fillStyle = `rgba(255,90,95,${0.14 * clamp((t - p.endT - 0.3) / 0.3)})`; ctx.fill(); }
          if (won) { ctx.save(); ctx.shadowColor = C.ok; ctx.shadowBlur = 30 * clamp((t - p.endT) / 0.3); rr(ctx, b.x - 6, b.y - 6, bw + 12, bh + 12, 16); ctx.strokeStyle = 'rgba(61,220,151,0.8)'; ctx.lineWidth = 3; ctx.stroke(); ctx.restore(); }
          // name tag + result
          const ty = b.y + bh + 44;
          text(ctx, p.name, cx, ty, { size: 34, color: C.text });
          const rs = t < p.endT ? '' : won ? fmt(p.score) : 'Leaked!';
          if (rs) withAlpha(ctx, clamp((t - p.endT) / 0.2), () => text(ctx, rs, cx, ty + 44, { font: DISPLAY, size: 38, color: won ? C.b300 : C.leak }));
          else withAlpha(ctx, clamp((t - p.place) / 0.3), () => text(ctx, 'playing' + '.'.repeat(1 + (Math.floor(t * 3) % 3)), cx, ty + 44, { size: 28, color: C.muted }));
          if (won) burst(ctx, b.x + 5.5 * c, b.y + 6.5 * c, p.endT, t, { n: 10, cols: [C.ok, C.b300], dist: 80, size: 6, seed: p.i });
          ctx.restore();
        }
      });
      if (t > LB) {
        const pp = pop(t, LB, 0.4), pw = 1000, px = (W - pw) / 2, py = 600, s = 2.85;
        ctx.save(); ctx.globalAlpha = clamp((t - LB) / 0.16);
        ctx.translate(W / 2, py + 450); ctx.scale(0.92 + 0.08 * pp, 0.92 + 0.08 * pp); ctx.translate(-W / 2, -(py + 450));
        const rows = top5.map((p, i) => ({ pos: i + 1, name: p.name, pts: p.score, slot: i, alpha: clamp((t - LB - 0.15 - i * 0.03) / 0.16) }));
        const blink = 0.55 + 0.45 * Math.sin((t - LB) * 6);
        rows.push({ pos: '?', name: 'You', you: true, ptsLabel: '—', slot: 5, alpha: clamp((t - LB - 0.5) / 0.2) });
        leaderboard(ctx, px, py, pw, rows, { s, highlightA: blink });
        ctx.restore();
        caption(ctx, t, LB + 0.2, END, ['Only five', 'make the board.'], 230, { size: 96, accent: C.b300 });
      }
      endCard(ctx, t, END, { headline: ['Will you', 'make the top 5?'], cta: 'Play Now' });
    },
  };
})();
