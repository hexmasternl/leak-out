// Generates the Google Play store listing icon (512x512, 32-bit PNG) from assets/icon/app-icon.svg
// and rasterises it with the globally installed Playwright Chromium, like assets/icon/render-icon.js.
// Play applies its own rounded mask, so the launcher icon's squircle corners and edge highlight are
// removed here and the background bleeds to the full square.
//   node generate-store-icon.js
// Outputs: store-icon.svg (full-bleed source) and store-icon-512x512.png
const fs = require('fs');
const path = require('path');
const pw = require(path.join(process.env.APPDATA, 'npm', 'node_modules', 'playwright'));

const SIZE = 512;
const source = fs.readFileSync(path.join(__dirname, '..', 'icon', 'app-icon.svg'), 'utf8');

const svg = source
  // Drop the squircle's edge highlight and top-edge sheen; they would sit just inside Play's mask.
  .replace(/<rect x="1\.5" y="1\.5"[^>]*\/>\s*/, '')
  .replace(/<path d="M228,2 H796"[^>]*\/>\s*/, '')
  // Square off the squircle background layers and clip path.
  .replace(/(<rect width="1024" height="1024") rx="228"/g, '$1');

if (/rx="22[68](\.5)?"/.test(svg)) throw new Error('app-icon.svg changed shape; update the replacements');
fs.writeFileSync(path.join(__dirname, 'store-icon.svg'), svg);

(async () => {
  const browser = await pw.chromium.launch();
  const page = await browser.newPage();
  // Draw onto a canvas and export from there: a screenshot of a fully opaque image is saved as
  // 24-bit RGB, while Play asks for a 32-bit PNG (RGBA).
  const dataUrl = await page.evaluate(async ({ svg, size }) => {
    const img = new Image();
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    await img.decode();
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = size;
    canvas.getContext('2d').drawImage(img, 0, 0, size, size);
    return canvas.toDataURL('image/png');
  }, { svg, size: SIZE });
  const file = path.join(__dirname, 'store-icon-512x512.png');
  fs.writeFileSync(file, Buffer.from(dataUrl.split(',')[1], 'base64'));
  console.log('wrote', file, fs.statSync(file).size, 'bytes');
  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
