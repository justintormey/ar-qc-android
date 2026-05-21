# AR QC Demo + AR Builder — native Android APK

Two Kotlin/Compose AR demonstrations packaged in one APK with two launcher icons. Sideloaded onto a headset (built and tested on a Digilens Argo, but should work on any AOSP Android 12+ device with a back-facing camera). Pair each with an operator's laptop/phone controller over WebRTC.

- **AR QC Demo** (icon: `QC`) — quality-control inspection station. Three 3D-printed parts on the table represent the canonical PASS / REWORK / SCRAP examples. Operator or QR detection fires the matching verdict per part.
- **AR Builder** (icon: `B`) — assembly training. Four 3D-printed angle brackets (A, B, C, D) joined across three steps using velcro and string. Step orientation determines which QR is exposed; the camera reads it and scores PASS or FAIL with rework guidance. Step 3 uses a compound QR check (both `BP` and `CP` must be visible at once).

Two installable packages are produced from the same source tree via Gradle product flavors: `com.arqcdemo.qc` and `com.arqcdemo.builder`. They share the WebRTC transport, camera infrastructure, ML Kit pipeline, and HUD primitives — their state machines, scene content, and Compose trees are separate. Default room PINs: **QC = 471471, Builder = 526526**.

There's an [HTML sibling project](https://github.com/justintormey/ar-qc-demo) (`ar-qc-html`) that ships the same flows as a static site. The two clients can interoperate over the same WebRTC room PIN — you can run controller on one and headset on the other.

---

## What you see

### AR QC — inspection (5-scene state machine)

| Scene | What's on screen |
|---|---|
| **Welcome** | Title panel — "AR QC Station 3 — Job 471471", BEGIN button. Wearer scroll-clicks BEGIN, or operator taps SCAN on the controller. |
| **Scanning** | Live CameraX preview + 4 cyan corner brackets + sweeping scan line. ML Kit decodes QR codes in every frame; matching `A`/`B`/`C` auto-fires the corresponding verdict. |
| **Verdict A / B / C** | Inspection-result panel (PASS / REWORK / SCRAP) with measured spec rows (surface finish, vent openings, warp, layer adhesion, defect count) + a "place in zone" cue + Accept/Reject/End-session buttons. The REWORK scene also renders a rotating 3D reference-geometry hologram via a small Three.js-style canvas. |
| **Complete** | Summary card — counts of pass/rework/scrap + total cycle time. Reset button. |

### AR Builder — assembly (6-scene state machine)

| Scene | What's on screen |
|---|---|
| **Welcome** | "AR Assembly Trainer — Job 526526" + BEGIN button. |
| **Instructions** | Per-step assembly card (3 steps) — text changes based on which step the participant is on (A+B velcro, C-onto-B string, D velcro opposing C). |
| **Scanning** | Same as QC's Scanning scene but watching for the 2-char Builder QR vocabulary (`AP`/`AF`/`BP`/`BF`/`CP`/`CF`). The QrAnalyzer emits the full set of visible Builder QRs per frame; the ViewModel applies step-aware rules (step 3 requires `BP` AND `CP` together). |
| **Verdict Pass** | Step-specific "step complete to spec" panel with check rows + Next step / Finish button. |
| **Verdict Fail** | Step-specific recovery cue (peel velcro / untie string / flip 180° / re-attach) + Rework / End session buttons. |
| **Complete** | Session summary (N of 3 steps passed) + Reset button. |

Flow (both apps):

1. Open the controller in a browser on a laptop or phone:
   - QC: `https://demo.justintormey.com/ar-qc/?transport=webrtc&room=471471`
   - Builder: `https://demo.justintormey.com/ar-qc/builder-control.html?transport=webrtc&room=526526`
2. Launch this APK on the headset (the launcher shows two icons — `QC` and `B`). Top-right pill shows the WebRTC handshake state (`connecting…` → `peers:2` → `connected`).
3. Wearer drives the demo via the Argo's scroll wheel + click — scroll moves focus between on-screen actions, click activates the focused button.
4. Operator can also drive the demo via the controller, useful as override or for cross-device demos. Both input paths fire the same `BusMessage` shapes.
5. QR-driven verdicts fire automatically when the camera sees a matching QR. Operator-driven verdicts override.

---

## Killer feature: real CV detection

### QC parts (3 single-letter QRs)

Print the QC QR codes in [`tools/qr-codes/`](tools/qr-codes/) (or open [`print-sheet.html`](tools/qr-codes/print-sheet.html) for a 3-up print layout). Attach one to the back / edge of each 3D-printed part:

- `qr-A.png` → Part A (smooth finish, vent openings present, no warping) → PASS
- `qr-B.png` → Part B (missing vent openings, warped at the base) → REWORK
- `qr-C.png` → Part C (extruder blobs, terrible layer adhesion, stringing) → SCRAP

### Builder brackets (6 two-character QRs)

Builder uses six 2-char QRs covering the three steps of the angle-bracket assembly. Each of brackets A, B, C gets one QR sticker on each opposing face — orientation determines which face is exposed:

- `qr-AP.png` / `qr-AF.png` → step 1 pass / fail (A+B velcro, T-form)
- `qr-BP.png` / `qr-BF.png` → step 2 pass / fail (C tied onto B, flange away from center)
- `qr-CP.png` / `qr-CF.png` → step 3 pass / fail (D velcro opposing C). Step 3's PASS requires `BP` AND `CP` both visible.

See [`docs/BUILDER-PROP-KIT.md`](docs/BUILDER-PROP-KIT.md) for the physical setup, parts list, sticker placement, and pacing guidance.

### Latency

When the camera sees a matching QR, ML Kit decodes it in ~30 ms and the matching verdict overlay fires on the headset within ~100 ms. No operator-controller round-trip required.

---

## Repo layout

```
ar-qc-android/
├── settings.gradle.kts                # Gradle settings + Maven repos
├── build.gradle.kts                   # top-level
├── gradle.properties
├── gradle/libs.versions.toml          # version catalog (single source of truth)
├── app/
│   ├── build.gradle.kts               # app module — Kotlin + Compose + CameraX + ML Kit + WebRTC; declares two product flavors
│   └── src/
│       ├── main/                            # shared code compiled into both flavor APKs
│       │   ├── AndroidManifest.xml          # declares both Activities (no LAUNCHER filter — flavors add those)
│       │   ├── kotlin/com/arqcdemo/app/
│       │   │   ├── MainActivity.kt              # QC entry, permissions, wheel + motion dispatch
│       │   │   ├── DemoViewModel.kt             # QC scene state machine + counts + bus wiring
│       │   │   ├── input/
│       │   │   │   └── WheelInput.kt            # Argo scroll-wheel + click → WheelEvent flow (DPAD_LEFT/RIGHT mapped)
│       │   │   ├── ui/                          # QC Compose scenes + reusable HUD components
│       │   │   │   ├── DemoApp.kt
│       │   │   │   ├── WelcomeScreen.kt
│       │   │   │   ├── ScanningScreen.kt
│       │   │   │   ├── VerdictScreen.kt
│       │   │   │   ├── CompleteScreen.kt
│       │   │   │   ├── SetupScreen.kt
│       │   │   │   ├── theme/Theme.kt
│       │   │   │   └── components/{Chips,CornerBrackets,ScanLine,FocusableButton}.kt
│       │   │   ├── builder/                     # Builder Activity, ViewModel, Compose scenes
│       │   │   │   ├── BuilderActivity.kt
│       │   │   │   ├── BuilderViewModel.kt      # 6-scene state machine + onBuilderFrame(Set) compound logic
│       │   │   │   └── ui/{BuilderApp,BuilderWelcome,BuilderInstructions,BuilderVerdictPass,BuilderVerdictFail,BuilderComplete}Screen.kt
│       │   │   ├── camera/
│       │   │   │   ├── CameraXController.kt     # lifecycle wiring + lens selection
│       │   │   │   └── QrAnalyzer.kt            # ML Kit: onQc(Char) for QC, onBuilderFrame(Set<String>) per frame for Builder
│       │   │   ├── transport/                   # StateBus + PortalSignaling + WebRtcClient + BusMessage shapes
│       │   │   └── settings/Prefs.kt            # DataStore (per-flavor room PIN persistence)
│       │   └── res/                             # theme, strings, launcher icons (ic_launcher_qc + ic_launcher_builder)
│       ├── qc/AndroidManifest.xml               # qc flavor manifest fragment — adds LAUNCHER filter to MainActivity
│       └── builder/AndroidManifest.xml          # builder flavor manifest fragment — adds LAUNCHER filter to BuilderActivity + overrides app icon
├── tools/
│   ├── generate-qr-codes.mjs                # Node script: emit qr-A/B/C.png + qr-AP/AF/BP/BF/CP/CF.png
│   └── qr-codes/
│       ├── qr-A/B/C.png                     # 512x512 single-letter QC QRs
│       ├── qr-AP/AF/BP/BF/CP/CF.png         # 512x512 two-char Builder QRs
│       ├── print-sheet.html                 # QC 3-up print layout
│       └── builder-print-sheet.html         # Builder 3-component-by-2-face print layout
├── docs/
│   ├── BUILDER-PROP-KIT.md                  # Builder physical setup, parts list, step procedure, sticker placement
│   ├── SIDELOAD.md                          # ADB install + launch + logcat
│   └── DEVELOPER-SETUP.md                   # macOS toolchain (JDK 17, Android SDK)
└── .github/workflows/build.yml              # GHA: assembleQcDebug + assembleBuilderDebug on push → APK artifacts
```

### How the layers fit together

- **`MainActivity` (QC)** and **`builder.BuilderActivity` (Builder)** are the entry points for their respective flavors. Each requests `CAMERA` permission, ensures a Room PIN is set in DataStore (defaults to `471471` for QC / `526526` for Builder on first launch), kicks off the ViewModel, and hosts the Compose tree. Both Activities also forward `KeyEvent`s and `MotionEvent`s into `WheelInput` so the Argo's scroll-wheel input drives focus/click.

- **`DemoViewModel` (QC)** and **`builder.BuilderViewModel`** are the source of truth for which scene the headset is showing. Transitions arrive from two sources, both go through `handle(BusMessage)`:
  1. The state-bus, which delivers messages from the operator's laptop controller over WebRTC.
  2. The QR analyzer — `onQc(part)` for QC, `onBuilderFrame(codes: Set<String>)` for Builder. The Builder side applies step-aware rules keyed on `currentPart` (A/B/C); step 3 needs BOTH `BP` and `CP` visible in the same frame to fire PASS.

- **`input/WheelInput`** captures the Argo's rotary wheel (which the OEM surfaces as `KEYCODE_DPAD_LEFT` / `KEYCODE_DPAD_RIGHT`) and its click (`KEYCODE_ENTER`) and exposes them as a `WheelEvent` flow. The active ViewModel maps scroll → focused-index changes and click → activation of the focused on-screen action.

- **`transport/`** is a near-port of the HTML repo's `src/transport/`:
  - `PortalSignaling` wraps an OkHttp WebSocket connection to the AWS API Gateway signaling relay. Sends messages wrapped as `{action: 'sendmessage', data: {...}}` (the route key API Gateway expects).
  - `WebRtcClient` is a thin Kotlin wrapper around `io.github.webrtc-sdk:android` — sets up the PeerConnection, handles offer/answer/ICE, and creates the DataChannel.
  - `StateBus` orchestrates both: opens signaling, becomes initiator if the room shows 2 peers, exchanges SDP/ICE, then surfaces a simple `send(BusMessage)` / `onMessage(handler)` interface.

- **`camera/`**: `CameraXController` binds Preview + ImageAnalysis to the back-facing camera (falls back to front if back fails). `QrAnalyzer` runs ML Kit on every frame; for QC it dispatches the first matching single-letter code; for Builder it collects the full Set of visible 2-char codes per frame so the ViewModel can apply compound rules. Throttled to ~500 ms repeats.

- **`ui/`** + **`builder/ui/`**: Jetpack Compose. QC has 5 screen composables, Builder has 6. Three reusable HUD components (`CornerBrackets`, `ScanLine`, `Chips`) plus `FocusableButton` (a button that pulses cyan when the ViewModel's `focusedIndex` points at it). One Material 3 theme with a cyan accent, shared between both apps.

---

## Default room PINs

Baked per-flavor:

- **QC = `471471`** ([`MainActivity.kt`](app/src/main/kotlin/com/arqcdemo/app/MainActivity.kt) → `DEFAULT_ROOM_PIN`).
- **Builder = `526526`** ([`builder/BuilderActivity.kt`](app/src/main/kotlin/com/arqcdemo/app/builder/BuilderActivity.kt) → `DEFAULT_BUILDER_PIN`).

Each app writes its PIN into DataStore on first launch and uses it for all subsequent sessions. To use a different PIN: `adb shell pm clear com.arqcdemo.qc` (or `com.arqcdemo.builder`) and the SetupScreen will appear on next launch — text entry requires a Bluetooth keyboard since the Argo's scroll wheel doesn't reach text inputs.

The matching PIN must be set on the operator's controller page for the WebRTC handshake to complete. The headset Welcome scene displays the PIN as the job number (`Job 471471`, `Job 526526`) so the wearer can read it back to the operator if needed.

---

## Build + install

See [`docs/DEVELOPER-SETUP.md`](docs/DEVELOPER-SETUP.md) for one-time toolchain setup (JDK 17 + Android SDK) and [`docs/SIDELOAD.md`](docs/SIDELOAD.md) for ADB install + launch + logcat commands.

Quickest path:

```bash
./gradlew assembleQcDebug assembleBuilderDebug
adb install -r app/build/outputs/apk/qc/debug/app-qc-debug.apk
adb install -r app/build/outputs/apk/builder/debug/app-builder-debug.apk
adb shell am start -n com.arqcdemo.qc/com.arqcdemo.app.MainActivity         # QC
adb shell am start -n com.arqcdemo.builder/com.arqcdemo.app.builder.BuilderActivity  # Builder
```

Both APKs install side-by-side (different applicationIds) and show two distinct icons in the launcher. CI builds both flavors on every push to `main` and uploads the APKs as workflow artifacts — download from the **Actions** tab if you'd rather not build locally.

---

## State-bus message protocol

Same JSON shape as the HTML sibling. The two clients can swap places over a shared WebRTC room. Builder messages co-exist with QC messages; clients that don't recognize a `kind` simply ignore it.

```js
{ kind: 'scan' }                                              // operator pressed SCAN
{ kind: 'verdict', part: 'A'|'B'|'C' }                        // QC: operator-pressed OR QR-detected
{ kind: 'builder-verdict', part: 'A'|'B'|'C', result: 'pass'|'fail' }  // Builder verdict
{ kind: 'next' }                                              // Builder: advance to next step
{ kind: 'complete' }                                          // operator pressed End Demo
{ kind: 'reset' }                                             // operator pressed Reset
{ kind: 'ready' }                                             // either client just loaded
{ kind: 'scene', name: '...' }                                // telemetry: scene transition
{ kind: 'verdict-shown', part: '...', result?: 'pass'|'fail' } // telemetry: verdict displayed
```

Every message also carries a `_seq` field (timestamp-derived) so retries can be deduped.

---

## License

Internal demo. Not for redistribution.
