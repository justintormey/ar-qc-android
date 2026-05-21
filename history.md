# ar-qc-android — design history & decisions

A short record of how this APK got to its current shape. Read alongside the [HTML sibling's history](https://github.com/justintormey/ar-qc-demo/blob/main/history.md) for the full picture.

## Why a native APK at all

The HTML version ([`ar-qc-html`](https://github.com/justintormey/ar-qc-demo)) ships the same flow as a static web app. It works on any modern browser and is the lowest-friction way to demo on a phone, laptop, or headset that has a WebXR or WebRTC-capable browser.

On the Digilens Argo specifically, the default browser is Firefox 105 and its `getUserMedia` implementation hangs indefinitely (no prompt, no resolve, no reject — confirmed via the HTML version's `diag.html` probe across four constraint shapes). That means the browser path can't show a live camera feed or run any on-device CV.

The native Android app exists to unlock those two capabilities:

1. **Live CameraX preview** as the scanning-scene background — the participant sees themselves looking at the part, framed by the HUD overlays.
2. **Real on-device ML Kit Barcode scanning** — QR codes stuck to each 3D-printed part (QC) and to each angle-bracket face (Builder) are decoded in ~30 ms; the matching verdict fires automatically, no operator-tap-in-the-loop required. Builder's step 3 takes the full Set of QRs visible per frame and applies a compound rule (`BP` AND `CP` both required for PASS).

The operator controller remains the manual-override path. Either input (QR detection or operator tap) drives the same state machine.

## Architecture

| Concern | Implementation |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material 3) |
| Camera | CameraX 1.3.4 (`Preview` + `ImageAnalysis` + `LifecycleOwner`) |
| CV | Google ML Kit Barcode Scanning 17.3.0, bundled variant (no Play Services dependency) |
| Cross-device transport | `io.github.webrtc-sdk:android` (libwebrtc) + OkHttp WebSocket for signaling |
| Persistence | DataStore Preferences (room PIN only) |
| Min/target SDK | 28 / 34 |
| Architecture | arm64-v8a only (matches Snapdragon XR2 in the Argo; rebuild for armeabi-v7a if needed) |

The Kotlin code mirrors the HTML version's structure 1:1:

| HTML file | Kotlin counterpart |
|---|---|
| `src/demo.js` (state machine) | `DemoViewModel.kt` |
| `src/hud/scenes.js` | `ui/{Welcome,Scanning,Verdict,Complete}Screen.kt` |
| `src/transport/state-bus.js` | `transport/StateBus.kt` |
| `src/transport/webrtc-transport.js` (signaling half) | `transport/PortalSignaling.kt` |
| `src/transport/webrtc-transport.js` (WebRTC half) | `transport/WebRtcClient.kt` |
| `src/hud/scenes.js` (BusMessage shapes) | `transport/BusMessage.kt` |
| `src/hud/l-profile.js` | (not ported — the AR see-through view + a flat verdict panel is sufficient) |

## Decisions worth recording

### Same JSON protocol as the HTML version

`BusMessage.toJson()` and `parseBusMessage()` emit/accept the exact same shapes as the HTML state-bus. Result: the HTML controller (on a laptop) can drive this Android client (on a headset), or vice versa. A web client and a native client can sit in the same WebRTC room and talk to each other. Useful for mixed deployments.

### AWS API Gateway WebSocket envelope

When sending signaling messages to the Portal endpoint, every payload is wrapped as `{action: 'sendmessage', data: {...}}`. API Gateway routes on `$request.body.action`; sending a raw `{type: 'join'}` returns `{message: 'Forbidden'}` because no route matches. This was a foot-gun discovered during the HTML build that's now baked into both clients.

### ICE candidate buffering

ICE candidates arriving over signaling before `setRemoteDescription` completes will throw `InvalidStateError` from `addIceCandidate`. We buffer them in `WebRtcClient._pendingCandidates` and flush after `setRemoteDescription` resolves on either the offer or answer path. Mirrors the same fix in the HTML version's `webrtc-transport.js`.

### Baked-in room PIN

`MainActivity.DEFAULT_ROOM_PIN = "471471"` is written to DataStore on first launch if no PIN has been set. The Argo has no convenient way to type into a text field (the scroll wheel doesn't reach Compose `OutlinedTextField`s), so the SetupScreen is functionally unreachable on the device. The default lets the app start working immediately. If you need a different PIN, `adb shell pm clear com.arqcdemo.app` and the app will re-bake the default — or temporarily attach a Bluetooth keyboard.

### lifecycleOwner deprecation warning

`androidx.lifecycle.compose.LocalLifecycleOwner` is the preferred source for the lifecycle owner now (we currently use the deprecated import from `androidx.lifecycle.LocalLifecycleOwner`). It still compiles and works, but you'll see one deprecation warning per build. Easy to fix by adding the `androidx.lifecycle:lifecycle-runtime-compose` dependency and swapping the import — left as a follow-up.

### Native libs are big

The debug APK is ~50 MB because `libjingle_peerconnection_so.so` (~25 MB) and `libbarhopper_v3.so` (the ML Kit native scanner) account for most of it. Restricting the build to `arm64-v8a` already cuts this from ~120 MB. A release build with R8 + resource shrinking would knock further but isn't critical for sideload distribution.

## Unfinished work

- **End-to-end WebRTC handshake against the real HTML controller** — verified at the signaling layer (`peer-count`, `peer-joined`, `should-initiate` all flow) but not yet against a real browser peer.
- **CameraX preview rendering** — visually unverified on the Argo specifically. Standard CameraX wiring; should work, but DigiOS quirks are possible.
- **Move to `androidx.lifecycle.compose.LocalLifecycleOwner`** — drop the deprecation warning.
- **Release build signing** — currently only `assembleDebug`. Add a release signing config + `assembleRelease` when distribution beyond sideload is needed.

### Designed but not yet implemented

Detailed designs live under [`docs/future-enhancements/`](docs/future-enhancements/) — pick one up when its turn comes.

- **Orientation-alignment CV** ([`docs/future-enhancements/cv-orientation-alignment.md`](docs/future-enhancements/cv-orientation-alignment.md)) — extract per-QR rotation from ML Kit's existing `cornerPoints`, introduce a `StepRule` abstraction in `BuilderViewModel`, and let any Builder step opt into a "QRs must be aligned within N°" check. Also documents text-OCR + custom-TFLite-icon paths as research-only future alternatives to QR codes.

## Related

- [`ar-qc-html`](https://github.com/justintormey/ar-qc-demo) — HTML/Three.js + WebRTC sibling shipped at `demo.justintormey.com/ar-qc/`.
