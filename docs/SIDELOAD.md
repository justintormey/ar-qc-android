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

```bash
cd argo-qc-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### B) Download from GitHub Actions

Every push to `main` triggers a GHA build that uploads `app-debug.apk` as an artifact. Go to the **Actions** tab → latest workflow run → **Artifacts** → download `app-debug-<sha>.zip`, unzip, then:

```bash
adb install -r app-debug.apk
```

`-r` reinstalls without uninstalling first, preserving the room PIN in DataStore.

## Launching

```bash
adb shell am start -n com.arqcdemo.app/.MainActivity
```

Or from the Argo's app drawer — look for **AR QC Demo** (cyan corner-brackets icon).

## First-launch behavior

1. App requests `android.permission.CAMERA` — tap **Allow** on the Argo. To pre-grant without the prompt:
   ```bash
   adb shell pm grant com.arqcdemo.app android.permission.CAMERA
   ```
2. App writes the baked-in room PIN (`471471`) into DataStore.
3. App opens the WebRTC connection to Portal signaling.
4. App shows the **Welcome** screen.
5. Hand the headset to the participant. Use the laptop controller to drive scenes.

## Tailing logs

```bash
adb logcat "ARQC:V" "ARQC.VM:V" "ARQC.Cam:V" "ARQC.QR:V" "ARQC.Bus:V" "ARQC.Signal:V" "ARQC.RTC:V" "AndroidRuntime:E" "*:S"
```

Useful tag log lines:
- `ARQC.Signal: ws open` — Portal WebSocket connected
- `ARQC.Bus: signal <- peer-count` — Portal acknowledged the join
- `ARQC.Bus: signal <- peer-joined` — laptop controller joined the room
- `ARQC.RTC: dc state -> OPEN` — peer-to-peer DataChannel open; bus is live
- `ARQC.QR: QR detected: A` — ML Kit decoded a part QR

## Resetting state

To wipe the room PIN + counts + permissions:

```bash
adb shell pm clear com.arqcdemo.app
```

Then re-launch. The default PIN will be re-baked on next start.

## Uninstall

```bash
adb uninstall com.arqcdemo.app
```
