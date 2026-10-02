// Rasterises the achievement icon SVGs to 512x512 PNG using the globally installed Playwright Chromium.
//   node generate-icons.mjs .     (writes achievement_<key>.svg next to this script)
//   node render-icons.js          (renders every achievement_*.svg to png/achievement_<key>.png)
//   node render-icons.js <key>…   (renders only the given keys, e.g. pack_2_complete)
// Copy the PNGs to src/app/src/main/res/drawable-nodpi/ and upload them to the Play Console.
const fs = require('fs');
const path = require('path');
const pw = require(path.join(process.env.APPDATA, 'npm', 'node_modules', 'playwright'));

const SIZE = 512;
const outDir = path.join(__dirname, 'png');
const keys = process.argv.slice(2);
const svgs = fs.readdirSync(__dirname)
  .filter(f => /^achievement_.+\.svg$/.test(f))
  .filter(f => keys.length === 0 || keys.includes(f.slice('achievement_'.length, -'.svg'.length)));

(async () => {
  fs.mkdirSync(outDir, { recursive: true });
  const browser = await pw.chromium.launch();
  for (const svgName of svgs) {
    const svg = fs.readFileSync(path.join(__dirname, svgName), 'utf8');
    const page = await browser.newPage({ viewport: { width: SIZE, height: SIZE }, deviceScaleFactor: 1 });
    await page.setContent(`<!doctype html><html><body style="margin:0;background:transparent">
      <img src="data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}" width="${SIZE}" height="${SIZE}" style="display:block">
    </body></html>`);
    await page.waitForLoadState('load');
    const file = path.join(outDir, svgName.replace(/\.svg$/, '.png'));
    await page.screenshot({ path: file, omitBackground: true, clip: { x: 0, y: 0, width: SIZE, height: SIZE } });
    console.log('wrote', file, fs.statSync(file).size, 'bytes');
    await page.close();
  }
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
