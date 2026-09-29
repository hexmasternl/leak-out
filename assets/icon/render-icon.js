// Rasterises the icon SVGs to PNG using the globally installed Playwright Chromium.
//   node render-icon.js
// Outputs: app-icon-1024.png, app-icon-512.png, adaptive-foreground-432.png, adaptive-background-432.png
const fs = require('fs');
const path = require('path');
const pw = require(path.join(process.env.APPDATA, 'npm', 'node_modules', 'playwright'));

const jobs = [
  ['app-icon.svg', 'app-icon-1024.png', 1024],
  ['app-icon.svg', 'app-icon-512.png', 512],
  ['adaptive-foreground.svg', 'adaptive-foreground-432.png', 432],
  ['adaptive-background.svg', 'adaptive-background-432.png', 432],
];

(async () => {
  const browser = await pw.chromium.launch();
  for (const [svgName, pngName, size] of jobs) {
    const svg = fs.readFileSync(path.join(__dirname, svgName), 'utf8');
    const page = await browser.newPage({ viewport: { width: size, height: size }, deviceScaleFactor: 1 });
    await page.setContent(`<!doctype html><html><body style="margin:0;background:transparent">
      <img src="data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}" width="${size}" height="${size}" style="display:block">
    </body></html>`);
    await page.waitForLoadState('load');
    const file = path.join(__dirname, pngName);
    await page.screenshot({ path: file, omitBackground: true, clip: { x: 0, y: 0, width: size, height: size } });
    console.log('wrote', file, fs.statSync(file).size, 'bytes');
    await page.close();
  }
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
