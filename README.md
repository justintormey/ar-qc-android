# AR QC Demo + AR Builder — native Android APK

Two Kotlin/Compose AR demonstrations packaged in one APK with two launcher icons. Sideloaded onto a headset (built and tested on a Digilens Argo, but should work on any AOSP Android 12+ device with a back-facing camera). Pair each with an operator's laptop/phone controller over WebRTC.

- **AR QC Demo** (icon: `QC`) — quality-control inspection station. Operator or QR detection fires the matching verdict per part. PASS / REWORK / SCRAP, sorting metaphor.
- **AR Builder** (icon: `B`) — assembly training. Participant uses a screwdriver to attach a sub-piece to each base component. The orientation determines which QR is exposed; the camera reads it and scores PASS or FAIL with rework guidance.

Both apps live in the same APK at package `com.arqcdemo.app`. They share the WebRTC transport, camera infrastructure, and HUD primitives. Their state machines, scene content, and Compose trees are separate. Default room PINs: **QC = 471471, Builder = 526526**.

There's an [HTML sibling project](https://github.com/justintormey/ar-qc-demo) (`ar-qc-html`) that ships the same flows as a static site. The two clients can interoperate over the same WebRTC room PIN — you can run controller on one and headset on the other.

---

## What you see

The app boots into a five-scene state machine:

| Scene | What's on screen |
|---|---|
| **Welcome** | Title panel — "AR QC Station 3 — Job 4471". The operator's controller drives the next move. |
| **Scanning** | Live CameraX preview + 4 cyan corner brackets + sweeping cyan scan line. ML Kit decodes QR codes in every frame; matching `A`/`B`/`C` auto-fires the corresponding verdict. |
| **Verdict A / B / C** | Inspection-result panel (PASS / REWORK / SCRAP) with measured values + a "place in zone" cue. The Part B (REWORK) scene also renders a rotating 3D L-profile reference via a small Three.js canvas. |
| **Complete** | Summary card — counts of pass/rework/scrap + total cycle time. |

Flow:

1. Open the controller in a browser on a laptop or phone: `https://demo.justintormey.com/ar-qc/?transport=webrtc&room=471471`
2. Launch this APK on the headset. Top-right pill shows the WebRTC handshake state (`connecting…` → `peers:2` → `connected`).
3. Operator taps **SCAN** on the controller → headset enters Scanning.
4. Participant picks up a QR-coded part. The camera sees it, ML Kit decodes the letter, the matching verdict appears automatically.
5. Operator can also manually fire A / B / C from the controller (override path) — useful if the QR is occluded or you want to test the verdict UI directly.
6. Operator taps **End Demo** → Complete scene with totals.

---

## Killer feature: real CV detection

Print the QR codes in [`tools/qr-codes/`](tools/qr-codes/) (or open [`print-sheet.html`](tools/qr-codes/print-sheet.html) for a 3-up print layout). Attach one to the back / edge of each metal part:

- `qr-A.png` → Part A (90° bend, no defects) → PASS
- `qr-B.png` → Part B (~75° underbend, in tolerance bounds) → REWORK
- `qr-C.png` → Part C (bent + dented + scratched) → SCRAP

When the camera sees one, ML Kit decodes it in ~30 ms and the matching verdict overlay fires on the headset within ~100 ms. No operator-controller round-trip required.

---

## Repo layout

```
ar-qc-android/
├── settings.gradle.kts                # Gradle settings + Maven repos
├── build.gradle.kts                   # top-level
├── gradle.properties
├── gradle/libs.versions.toml          # version catalog (single source of truth)
├── app/
│   ├── build.gradle.kts               # app module — Kotlin + Compose + CameraX + ML Kit + WebRTC
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/arqcdemo/app/
│       │   ├── MainActivity.kt              # entry, permissions, baked PIN
│       │   ├── DemoViewModel.kt             # Scene state machine + counts + bus wiring
│       │   ├── ui/
│       │   │   ├── DemoApp.kt               # scene router + status chip + connection pill
│       │   │   ├── SetupScreen.kt           # first-run PIN entry (rarely reached)
│       │   │   ├── WelcomeScreen.kt
│       │   │   ├── ScanningScreen.kt        # CameraX preview + brackets + scan line
│       │   │   ├── VerdictScreen.kt         # A/B/C result panels
│       │   │   ├── CompleteScreen.kt        # summary
│       │   │   ├── theme/Theme.kt           # cyan accent, dark scheme
│       │   │   └── components/
│       │   │       ├── Chips.kt             # StatusChip + ConnectionPill
│       │   │       ├── CornerBrackets.kt    # cyan corner overlay (Canvas)
│       │   │       └── ScanLine.kt          # animated cyan sweep (Canvas + animateFloat)
│       │   ├── camera/
│       │   │   ├── CameraXController.kt     # lifecycle wiring + lens selection
│       │   │   └── QrAnalyzer.kt            # ML Kit Barcode (QR-only, 500ms debounced)
│       │   ├── transport/
│       │   │   ├── StateBus.kt              # facade: signaling + WebRTC
│       │   │   ├── PortalSignaling.kt       # OkHttp WebSocket → AWS API Gateway
│       │   │   ├── WebRtcClient.kt          # libwebrtc peer + DataChannel
│       │   │   ├── BusMessage.kt            # JSON message shapes (same as HTML)
│       │   │   └── TransportState.kt
│       │   └── settings/Prefs.kt            # DataStore (room PIN persistence)
│       └── res/                              # theme, strings, launcher icons
├── tools/
│   ├── generate-qr-codes.mjs                # Node script: emit qr-A/B/C.png
│   └── qr-codes/
│       ├── qr-A.png  qr-B.png  qr-C.png     # 512x512 print-ready QRs
│       └── print-sheet.html                  # 3-up print layout
├── docs/
│   ├── SIDELOAD.md                          # ADB install + launch + logcat
│   └── DEVELOPER-SETUP.md                   # macOS toolchain (JDK 17, Android SDK)
└── .github/workflows/build.yml              # GHA: assembleDebug on push → APK artifact
```

### How the layers fit together

- **`MainActivity`** is the only Activity. It requests `CAMERA` permission, ensures a Room PIN is set in DataStore (defaults to `471471` on first launch — see "Default room PIN" below), kicks off the ViewModel, and hosts the Compose tree.

- **`DemoViewModel`** is the source of truth for which scene the headset is showing and how many of each verdict have fired. Transitions arrive from two sources, both go through `handle(BusMessage)`:
  1. The state-bus, which delivers messages from the operator's laptop controller over WebRTC.
  2. The QR analyzer, which calls `onQrDetected(part)` when ML Kit decodes a QR in a camera frame.

- **`transport/`** is a near-port of the HTML repo's `src/transport/`:
  - `PortalSignaling` wraps an OkHttp WebSocket connection to the AWS API Gateway signaling relay. Sends messages wrapped as `{action: 'sendmessage', data: {...}}` (the route key API Gateway expects).
  - `WebRtcClient` is a thin Kotlin wrapper around `io.github.webrtc-sdk:android` — sets up the PeerConnection, handles offer/answer/ICE, and creates the DataChannel.
  - `StateBus` orchestrates both: opens signaling, becomes initiator if the room shows 2 peers, exchanges SDP/ICE, then surfaces a simple `send(BusMessage)` / `onMessage(handler)` interface.

- **`camera/`**: `CameraXController` binds Preview + ImageAnalysis to the back-facing camera (falls back to front if back fails). `QrAnalyzer` runs ML Kit on every frame and debounces 500 ms so we don't fire repeatedly while the participant holds a part still.

- **`ui/`**: Jetpack Compose. Five screen composables, three reusable HUD components, one Material 3 theme with a cyan accent.

---

## Default room PIN

Baked at **`471471`** ([`MainActivity.kt`](app/src/main/kotlin/com/arqcdemo/app/MainActivity.kt) → `DEFAULT_ROOM_PIN`). The app writes this into DataStore on first launch and uses it for all subsequent sessions. To use a different PIN, `adb shell pm clear com.arqcdemo.app` to reset; the SetupScreen will appear on next launch (note: text entry requires a Bluetooth keyboard since the Argo's scroll wheel doesn't reach text inputs).

The same PIN must be set on the operator's controller page for the WebRTC handshake to complete.

---

## Build + install

See [`docs/DEVELOPER-SETUP.md`](docs/DEVELOPER-SETUP.md) for one-time toolchain setup (JDK 17 + Android SDK) and [`docs/SIDELOAD.md`](docs/SIDELOAD.md) for ADB install + launch + logcat commands.

Quickest path:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.arqcdemo.app/.MainActivity
```

CI builds an APK on every push to `main` and uploads it as a workflow artifact — download from the **Actions** tab if you'd rather not build locally.

---

## State-bus message protocol

Same JSON shape as the HTML sibling. The two clients can swap places over a shared WebRTC room.

```kotlin
data class BusMessage { object Ready; object Scan; data class Verdict(part); object Complete; object Reset; ... }
```

```js
{ kind: 'scan' }                         // operator pressed SCAN
{ kind: 'verdict', part: 'A'|'B'|'C' }   // either: operator pressed a verdict, OR QR detected
{ kind: 'complete' }                     // operator pressed End Demo
{ kind: 'reset' }                        // operator pressed Reset
{ kind: 'ready' }                        // either client just loaded
{ kind: 'scene', name: '...' }           // telemetry
{ kind: 'verdict-shown', part: '...' }   // telemetry
```

---

## License

Internal demo. Not for redistribution.
