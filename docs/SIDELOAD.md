# Sideloading the APK onto a Digilens Argo

## One-time Argo setup

If `adb devices` does NOT show the Argo, you need to enable developer mode + USB debugging on the device first:

1. On the Argo: **Settings → About → Build number** — tap 7 times. "You are now a developer" toast appears.
2. **Settings → System → Developer options → USB debugging** — toggle ON.
3. Plug the Argo into the Mac via USB-C.
4. On the Argo, an "Allow USB debugging from this computer?" dialog appears — tap **Always allow**.
5. From the Mac: `adb devices` — Argo should list as `device` (not `unauthorized`).

If the Argo enumerates as `KONA-MTP` in `system_profiler SPUSBDataType`, that means it's in MTP-only mode (no ADB). The toggle above flips it to ADB-enabled.

## Install the APK

Two ways:

### A) Build from source

Both apps build from the same source tree via Gradle product flavors:

```bash
cd argo-qc-android
./gradlew assembleQcDebug assembleBuilderDebug
adb install -r app/build/outputs/apk/qc/debug/app-qc-debug.apk
adb install -r app/build/outputs/apk/builder/debug/app-builder-debug.apk
```

Each flavor produces its own APK with a different `applicationId` (`com.arqcdemo.qc` and `com.arqcdemo.builder`). They install side-by-side and show as two separate icons in the launcher.

### B) Download from GitHub Actions

Every push to `main` triggers a GHA build that uploads both APKs as artifacts. Go to the **Actions** tab → latest workflow run → **Artifacts** → download `app-qc-debug-<sha>.zip` and `app-builder-debug-<sha>.zip`, unzip, then:

```bash
adb install -r app-qc-debug.apk
adb install -r app-builder-debug.apk
```

`-r` reinstalls without uninstalling first, preserving the room PINs in DataStore.

## Launching

```bash
adb shell am start -n com.arqcdemo.qc/com.arqcdemo.app.MainActivity                # QC
adb shell am start -n com.arqcdemo.builder/com.arqcdemo.app.builder.BuilderActivity # Builder
```

Or from the Argo's app drawer — look for **AR QC** (cyan corner-brackets + `QC` glyph) or **BUILD** (cyan corner-brackets + `B` glyph).

## First-launch behavior

1. App requests `android.permission.CAMERA` — tap **Allow** on the Argo. To pre-grant without the prompt:
   ```bash
   adb shell pm grant com.arqcdemo.qc android.permission.CAMERA
   adb shell pm grant com.arqcdemo.builder android.permission.CAMERA
   ```
2. App writes the baked-in room PIN into DataStore — `471471` for QC, `526526` for Builder.
3. App opens the WebRTC connection to Portal signaling.
4. App shows the **Welcome** screen.
5. Hand the headset to the participant. The wearer can drive scenes locally via the scroll wheel + click, or use the operator's laptop controller as override.

## Tailing logs

```bash
adb logcat "ARQC:V" "ARQC.VM:V" "ARQC.Builder.VM:V" "ARQC.Cam:V" "ARQC.QR:V" "ARQC.Wheel:V" "ARQC.Bus:V" "ARQC.Signal:V" "ARQC.RTC:V" "AndroidRuntime:E" "*:S"
```

Useful tag log lines:
- `ARQC.Signal: ws open` — Portal WebSocket connected
- `ARQC.Bus: signal <- peer-count` — Portal acknowledged the join
- `ARQC.Bus: signal <- peer-joined` — laptop controller joined the room
- `ARQC.RTC: dc state -> OPEN` — peer-to-peer DataChannel open; bus is live
- `ARQC.QR: QC code: A` — ML Kit decoded a QC part QR
- `ARQC.QR: Builder frame: [AP, BP]` — ML Kit's per-frame Set of Builder QRs (step-3 compound check feeds off this)
- `ARQC.Wheel: keyCode=21 ... mapped=UP` — scroll-wheel event made it into WheelInput

## Resetting state

To wipe room PIN + counts + permissions per flavor:

```bash
adb shell pm clear com.arqcdemo.qc        # QC
adb shell pm clear com.arqcdemo.builder   # Builder
```

Then re-launch. The default PIN will be re-baked on next start.

## Uninstall

```bash
adb uninstall com.arqcdemo.qc
adb uninstall com.arqcdemo.builder
```
