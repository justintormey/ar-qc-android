# AR Builder prop kit

What you need on the table to run the assembly-training demo end-to-end. Total cost ~$50 from a hardware store.

## What the participant does

1. Picks up one of three components (a base plate + an unmounted sub-piece).
2. Reads the on-screen Assembly instructions while picking up the bolts + screwdriver.
3. Decides which way to orient the sub-piece relative to the base, then bolts it on.
   - **Correct orientation** → the **PASS QR** sticker faces outward.
   - **Inverted orientation** → the **FAIL QR** sticker faces outward.
4. Holds the assembled component up to the headset camera.
5. The app reads whichever QR is now visible and shows PASS or FAIL.
6. On FAIL, scrolls to **Rework** → re-reads instructions → un-bolts → flips → re-bolts → re-scans.
7. On PASS, scrolls to **Next part** → does it for B and C.

## Parts list

| Item | Qty | Approx cost | Notes |
|---|---|---|---|
| Steel or aluminum base plates, ~6"×4", drilled with 2 bolt holes on the front face | 3 | $15 | Pre-drilled hardware works; otherwise drill 5mm holes 60mm apart |
| Smaller sub-pieces (~3"×2"), drilled to match | 3 | $9 | Should fit flush against the base; two opposite faces both work as the mounting surface |
| M4 or 10-32 bolts + nuts + washers | 6+ sets | $5 | Cap-head Allen or Philips; keep a spare set in the box |
| Small Philips or Allen screwdriver | 1 | $5 | Whichever matches your bolt head |
| Dark fabric or rubber work mat (~18"×24") | 1 | $10 | Defines the workspace; black or dark gray reads cleanly through the lenses |
| Printable laminated Instructions card (optional) | 1 | $1 | Backup if the headset's Instructions scene isn't visible |
| Nitrile or safety gloves | 1 box | $5 | Optional; the user can opt in if they want |
| **Total** | | **~$50** | |

## QR stickers

Generated in `tools/qr-codes/` (run `node tools/generate-qr-codes.mjs` if missing). Six codes:

- `qr-AP.png`, `qr-AF.png` — Part A pass / fail
- `qr-BP.png`, `qr-BF.png` — Part B pass / fail
- `qr-CP.png`, `qr-CF.png` — Part C pass / fail

Open `tools/qr-codes/builder-print-sheet.html` in a browser for a 3-component-by-2-face print layout. Print at 25mm square per QR, cut, peel-and-stick.

**Sticker placement:**

For each sub-piece, stick **two QRs — one on each opposite face**:

- The face that will be exposed when the sub-piece is mounted in the **correct orientation** → stick the **P** sticker (AP, BP, or CP for parts A/B/C respectively).
- The face that will be exposed when the sub-piece is mounted **inverted** (flipped 180°) → stick the **F** sticker (AF, BF, CF).

If you make a mistake, peel + re-stick. The camera reads whichever side is visible after the participant assembles, so the orientation they choose is what determines the verdict.

## Setup before the participant arrives

1. Put the mat down on the table.
2. Lay out the three base plates, three sub-pieces (with QRs already attached), the bolts, and the screwdriver.
3. Power on the Argo. Open the **AR Builder** app (the launcher icon with the "B" letters).
4. On a laptop or phone, open `https://demo.justintormey.com/ar-qc/builder-control.html?transport=webrtc&room=526526`. Verify the connection pill goes green.
5. Hand the headset to the participant.

## Pacing

- Each component takes ~30–60 seconds for a first-time user (read instructions, install bolts, lift to scan).
- Plan ~5 minutes for a full three-component run with at least one FAIL→rework loop.
- The Argo's wheel-click drives Begin → Instructions → Scan → verdict; the participant doesn't need to touch the laptop at any point.

## Operator overrides (controller)

The operator's controller has six manual verdict buttons (PASS A/B/C + FAIL A/B/C) for when QR detection isn't available — for example, when running the HTML version in a plain Chrome browser without ML Kit, or when the QR is occluded by a finger. Either input path (QR or operator-tap) fires the same `BuilderVerdict` bus message, so the headset behaves identically.

## Troubleshooting

- **QR not reading at arm's length**: try printing larger (30–35mm square) or moving the headset closer.
- **Both stickers visible at once**: that shouldn't happen with proper orientation, but if it does the camera fires whichever it sees first (ML Kit picks one barcode per frame). Re-orient the piece so only one face is showing.
- **Verdict fires but wearer hadn't finished assembling**: the wearer was probably already holding the part too close. Scroll to Rework, re-scan once they're ready.
- **Wheel scroll not moving focus**: tail `adb logcat -s ARQC.Wheel:V` — should print `UP` / `DOWN` / `CLICK`. If nothing prints, the Argo's wheel isn't surfacing as standard d-pad KeyEvents on this build and the operator's controller is the fallback.
