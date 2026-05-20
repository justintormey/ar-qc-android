# AR Builder prop kit

The physical setup for the assembly-training demo. Four 3D-printed angle
brackets (A, B, C, D) plus a handful of velcro tabs and a length of string —
the participant assembles them across three steps and the camera reads QR
codes to validate each step.

## What the participant does

| Step | Action | Scan logic |
|---|---|---|
| **1** | Attach A and B face-to-face using the velcro tabs. Align the end channel flanges so they face AWAY from each other — the two pieces form a T-shape. | `AP` visible → PASS · `AF` visible → FAIL |
| **2** | Tie C onto B using the supplied string. Position C so its end channel flange points AWAY from the center of the assembly. | `BP` visible → PASS · `BF` visible → FAIL |
| **3** | Attach D using the velcro tabs already on it. Position D so its channel flange points AWAY from C's channel flange (opposing sides). | **`BP` AND `CP` both visible → PASS** · `CF` visible → FAIL · CP alone (no BP) → keep scanning, re-position |

On FAIL: peel the velcro / untie the string, flip the offending piece 180°,
re-attach, hold up to the lens again.

## Parts list

| Item | Qty | Notes |
|---|---|---|
| 3D-printed angle bracket A | 1 | QR stickers `AP` on one face, `AF` on the opposite face |
| 3D-printed angle bracket B | 1 | QR stickers `BP` / `BF` on opposite faces |
| 3D-printed angle bracket C | 1 | QR stickers `CP` / `CF` on opposite faces |
| 3D-printed angle bracket D | 1 | No QR — D's correctness is read indirectly via which face of C is exposed |
| Velcro tabs (pre-attached) | several pairs | Already glued to A, B, and D — no loose hardware to manage |
| Supplied string | 1 length | Used to tie C onto B |
| Dark fabric or rubber work mat (~18"×24") | 1 | Defines the workspace; black or dark gray reads cleanly through the lenses |
| Printable laminated Instructions card (optional) | 1 | Backup if the headset's Instructions scene isn't visible |

## QR stickers

Generated in `tools/qr-codes/` (run `node tools/generate-qr-codes.mjs` if missing). Six codes — two on each of brackets A, B, C:

- `qr-AP.png`, `qr-AF.png` — bracket A pass / fail (step 1 verdict)
- `qr-BP.png`, `qr-BF.png` — bracket B pass / fail (step 2 verdict, plus step 3 compound)
- `qr-CP.png`, `qr-CF.png` — bracket C pass / fail (step 3 verdict)

Open `tools/qr-codes/builder-print-sheet.html` in a browser for the print
layout. Print at 25mm square per QR, cut, peel-and-stick. Each bracket gets
two stickers — one on each opposing face — so the orientation the participant
chooses determines which sticker faces the camera.

**Step 3's compound check is the interesting one**: passing step 3 requires
the camera to see BOTH `BP` (from bracket B, exposed by correct step-2 work)
AND `CP` (from bracket C, exposed by correct step-3 work) in the same frame.
If the participant disturbed step 2 while doing step 3, `BP` won't be
visible and step 3 can't pass even if `CP` shows. That enforces a cumulative
assembly — they can't half-finish step 3 by sacrificing step 2's alignment.

## Setup before the participant arrives

1. Put the mat down on the table.
2. Lay out the four angle brackets (A, B, C, D), the supplied string, and a
   few spare velcro tabs in case the pre-attached ones come loose.
3. Power on the Argo. Open the **BUILD** app (the launcher icon with the "B"
   letters).
4. On a laptop or phone, open `https://demo.justintormey.com/ar-qc/builder-control.html?transport=webrtc&room=526526`.
   Verify the connection pill goes green.
5. Hand the headset to the participant.

## Pacing

- Each step takes ~30–60 seconds for a first-time user (read instructions,
  attach, lift to scan).
- Plan ~5 minutes for a full three-step run with at least one FAIL→rework
  loop.
- The Argo's wheel-click drives Begin → Instructions → Scan → verdict; the
  participant doesn't need to touch the laptop at any point.

## Operator overrides (controller)

The operator's controller has six manual verdict buttons (PASS A/B/C +
FAIL A/B/C) for when QR detection isn't available — for example, when
running the HTML version in a plain Chrome browser without ML Kit, or when
the QR is occluded by a finger. Either input path (QR or operator-tap)
fires the same `BuilderVerdict` bus message, so the headset behaves
identically. The compound BP+CP check for step 3 only applies on the
on-device camera path; the operator's PASS C button always pass-fires step 3
directly when used.

## Troubleshooting

- **QR not reading at arm's length**: try printing larger (30–35mm square)
  or moving the headset closer.
- **Step 3 won't pass even though D looks right**: the most common cause is
  that step 2's BP QR got covered by the participant's grip or by the
  string knot. Re-position so BOTH BP (on B) and CP (on C) are in the
  camera's view at the same time.
- **Both stickers visible at once for the same bracket**: that shouldn't
  happen with proper orientation, but if it does the camera fires whichever
  it sees first (ML Kit picks one barcode per frame, though our analyzer
  collects all of them per frame for step 3). Re-orient so only one face
  per bracket is showing.
- **Verdict fires but the participant hadn't finished**: the participant
  was probably already holding the part too close. Scroll to Rework,
  re-scan once they're ready.
- **Wheel scroll not moving focus**: tail `adb logcat -s ARQC.Wheel:V` —
  should print `UP` / `DOWN` / `CLICK`. The Argo's wheel surfaces as
  `KEYCODE_DPAD_LEFT` / `KEYCODE_DPAD_RIGHT` (mapped to up/down by
  `WheelInput`). If nothing prints, the Argo's wheel isn't routing input
  to the app and the operator's controller is the fallback.
