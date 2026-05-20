#!/usr/bin/env node
// Generate the printable QR codes for both demos:
//   QC Inspector:  'A', 'B', 'C'                     — three codes
//   AR Builder:    'AP','AF','BP','BF','CP','CF'     — six codes (PASS / FAIL × component)
//
// ML Kit's BarcodeScanner (configured for FORMAT_QR_CODE) reads either
// vocabulary and the app's QrAnalyzer dispatches into QC verdicts or
// Builder verdicts based on the code length.
//
// Usage:  node tools/generate-qr-codes.mjs
// Output: tools/qr-codes/qr-<code>.png  for every code (each ~512x512)
//
// Print at ~25mm square for a sticker-friendly size that the camera reads
// reliably at arm's-length distance.

import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import qrcode from 'qrcode';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUTDIR = join(__dirname, 'qr-codes');

const QC_PARTS = ['A', 'B', 'C'];
const BUILDER_PARTS = ['AP', 'AF', 'BP', 'BF', 'CP', 'CF'];
const ALL = [...QC_PARTS, ...BUILDER_PARTS];

for (const p of ALL) {
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
console.log('');
console.log('done. print at 25mm square; attach as follows:');
console.log('  QC:      one sticker per part (A, B, C)');
console.log('  Builder: TWO stickers per part, one per mountable face');
console.log('           Part A: AP on the correct-orientation face, AF on the inverted face');
console.log('           Part B: BP on correct, BF on inverted');
console.log('           Part C: CP on correct, CF on inverted');
