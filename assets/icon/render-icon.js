// Rasterises the icon SVGs to PNG using the globally installed Playwright Chromium.
//   node render-icon.js
// Outputs: app-icon-1024.png, app-icon-512.png, adaptive-foreground-432.png, adaptive-background-432.png,
// and the launcher mipmaps in src/app/src/main/res/mipmap-*/ (adaptive layers, plus the legacy
// square and round icons composed from the visible 72 of 108 dp of those layers).
const fs = require('fs');
const path = require('path');
const pw = require(path.join(process.env.APPDATA, 'npm', 'node_modules', 'playwright'));

const res = path.join(__dirname, '..', '..', 'src', 'app', 'src', 'main', 'res');
const densities = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

const read = name => `data:image/svg+xml;charset=utf-8,${encodeURIComponent(fs.readFileSync(path.join(__dirname, name), 'utf8'))}`;
const img = (name, size, offset = 0) =>
  `<img src="${read(name)}" width="${size}" height="${size}" style="position:absolute;left:${offset}px;top:${offset}px">`;

// a single SVG at full size
const plain = (svgName, size) => img(svgName, size);
// background + foreground cropped to the visible 72/108 and clipped to a launcher shape
const legacy = radius => size => {
  const full = size * 108 / 72, off = -(full - size) / 2;
  return `<div style="position:absolute;inset:0;overflow:hidden;border-radius:${radius}">
    ${img('adaptive-background.svg', full, off)}${img('adaptive-foreground.svg', full, off)}</div>`;
};

const jobs = [
  [path.join(__dirname, 'app-icon-1024.png'), 1024, s => plain('app-icon.svg', s)],
  [path.join(__dirname, 'app-icon-512.png'), 512, s => plain('app-icon.svg', s)],
  [path.join(__dirname, 'adaptive-foreground-432.png'), 432, s => plain('adaptive-foreground.svg', s)],
  [path.join(__dirname, 'adaptive-background-432.png'), 432, s => plain('adaptive-background.svg', s)],
];
for (const [d, f] of Object.entries(densities)) {
  const dir = path.join(res, `mipmap-${d}`);
  jobs.push([path.join(dir, 'ic_launcher_foreground.png'), 108 * f, s => plain('adaptive-foreground.svg', s)]);
  jobs.push([path.join(dir, 'ic_launcher_background.png'), 108 * f, s => plain('adaptive-background.svg', s)]);
  jobs.push([path.join(dir, 'ic_launcher.png'), 48 * f, legacy('18%')]);
  jobs.push([path.join(dir, 'ic_launcher_round.png'), 48 * f, legacy('50%')]);
}

(async () => {
  const browser = await pw.chromium.launch();
  for (const [file, size, body] of jobs) {
    const page = await browser.newPage({ viewport: { width: size, height: size }, deviceScaleFactor: 1 });
    await page.setContent(`<!doctype html><html><body style="margin:0;background:transparent">
      <div style="position:relative;width:${size}px;height:${size}px">${body(size)}</div>
    </body></html>`);
    await page.waitForLoadState('load');
    await page.screenshot({ path: file, omitBackground: true, clip: { x: 0, y: 0, width: size, height: size } });
    console.log('wrote', path.relative(path.join(__dirname, '..', '..'), file), fs.statSync(file).size, 'bytes');
    await page.close();
  }
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
