#!/usr/bin/env node
// Generate three printable QR codes (A, B, C) encoded as single-letter
// strings. ML Kit's BarcodeScanner (configured for FORMAT_QR_CODE) reads
// these and the app fires the matching verdict.
//
// Usage:  node tools/generate-qr-codes.mjs
// Output: tools/qr-codes/qr-A.png, qr-B.png, qr-C.png  (each ~512x512)
//
// Print at ~25mm square for a sticker-friendly size that the Argo's
// camera reads reliably at arm's-length distance.

import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import qrcode from 'qrcode';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUTDIR = join(__dirname, 'qr-codes');

const PARTS = ['A', 'B', 'C'];

for (const p of PARTS) {
  const png = await qrcode.toBuffer(p, {
    errorCorrectionLevel: 'M',
    type: 'png',
    width: 512,
    margin: 4,
    color: { dark: '#000000', light: '#FFFFFF' },
  });
  const file = join(OUTDIR, `qr-${p}.png`);
  await writeFile(file, png);
  console.log(`wrote ${file} (${png.length} bytes)`);
}
console.log('done. print 25mm square; attach one to each metal part.');
