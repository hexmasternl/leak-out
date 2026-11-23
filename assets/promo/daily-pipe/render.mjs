// Renders the Daily Pipe promo scenes to MP4 (1080x1920, 30 fps, 15 s) with music and sound effects.
//
// Usage:
//   node render.mjs [v1 v2 ...]              render videos into docs/video/
//   node render.mjs --stills v1 1.5 6 12     write preview PNGs for the given seconds
//
// Needs Playwright (Chromium) and an ffmpeg with libx264 + aac. Set FFMPEG to its path,
// or have `ffmpeg` on the PATH; PLAYWRIGHT_MODULE can point at a global playwright install.
import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '../../..');
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const FFMPEG = process.env.FFMPEG || 'ffmpeg';
const FPS = 30, DURATION = 15;

const args = process.argv.slice(2);
const stills = args[0] === '--stills';
const scenes = stills ? [args[1]] : (args.length ? args : ['v1', 'v2', 'v3', 'v4', 'v5']);

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 540, height: 960 } });

for (const scene of scenes) {
  await page.goto(pathToFileURL(path.join(here, 'page.html')).href + '?scene=' + scene);
  await page.waitForFunction(() => window.ready, null, { timeout: 30000 });
  const info = await page.evaluate(() => ({ ready: window.ready, meta: window.SCENE.meta }));
  console.log(scene, 'fonts:', info.ready.fonts.join(', '));

  if (stills) {
    const outDir = path.join(here, 'stills'); fs.mkdirSync(outDir, { recursive: true });
    for (const s of args.slice(2).map(Number)) {
      const data = await page.evaluate((t) => window.frameData(t), s);
      fs.writeFileSync(path.join(outDir, `${scene}-${s.toFixed(2)}.png`), Buffer.from(data.split(',')[1], 'base64'));
    }
    continue;
  }

  const meta = info.meta;
  const out = path.join(repo, 'docs', 'video', meta.file);
  const inputs = ['-f', 'image2pipe', '-framerate', String(FPS), '-i', '-'];
  const filters = [];
  const mixes = [];
  // music bed
  inputs.push('-ss', String(meta.music.start), '-t', String(DURATION), '-i', path.join(repo, 'assets/audio', meta.music.file));
  filters.push(`[1:a]volume=${meta.music.vol ?? 0.55},afade=t=in:st=0:d=0.6,afade=t=out:st=${DURATION - 1.2}:d=1.2[m]`);
  mixes.push('[m]');
  meta.sfx.forEach((s, i) => {
    inputs.push('-i', path.join(repo, 'assets/sfx', s.file));
    const ms = Math.round(s.t * 1000);
    filters.push(`[${i + 2}:a]volume=${s.vol ?? 0.9},adelay=${ms}|${ms}[s${i}]`);
    mixes.push(`[s${i}]`);
  });
  filters.push(`${mixes.join('')}amix=inputs=${mixes.length}:normalize=0:duration=first,alimiter=limit=0.95[a]`);
  const ff = spawn(FFMPEG, ['-y', '-hide_banner', '-loglevel', 'error', ...inputs,
    '-filter_complex', filters.join(';'), '-map', '0:v', '-map', '[a]',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-r', String(FPS),
    '-c:a', 'aac', '-b:a', '192k', '-t', String(DURATION), '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const done = new Promise((res, rej) => ff.on('close', (code) => (code === 0 ? res() : rej(new Error('ffmpeg exited ' + code)))));
  const frames = FPS * DURATION;
  for (let f = 0; f < frames; f++) {
    const data = await page.evaluate((t) => window.frameData(t), f / FPS);
    const buf = Buffer.from(data.split(',')[1], 'base64');
    if (!ff.stdin.write(buf)) await new Promise((r) => ff.stdin.once('drain', r));
    if (f % 90 === 0) process.stdout.write(`  ${scene} frame ${f}/${frames}\r`);
  }
  ff.stdin.end();
  await done;
  console.log(`  ${scene} -> ${path.relative(repo, out)}            `);
}
await browser.close();
