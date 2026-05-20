# Developer setup (macOS)

Tested on macOS 14+ with Apple Silicon. ~30 minutes from scratch.

## 1. Tools

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
```

Note: `openjdk@17` (formula, not cask) installs without sudo. `--cask zulu@17` / `--cask temurin` need an admin password.

## 2. Environment

Add to `~/.zshrc` (or your shell profile):

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH"
```

Reload: `source ~/.zshrc`.

## 3. SDK packages

```bash
yes | sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

Verify:

```bash
java -version       # openjdk 17
adb version         # 1.0.41+
gradle --version    # 8+ (only needed for one-time wrapper bootstrap, optional)
```

## 4. Build

```bash
cd argo-qc-android
./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`. First build downloads ~1.5GB of dependencies (Compose, CameraX, libwebrtc, ML Kit) and takes ~2 minutes. Subsequent builds are seconds.

## 5. Install + run

See [`SIDELOAD.md`](SIDELOAD.md).

## Common issues

- **`SDK location not found`** — set `ANDROID_HOME` env var (above), or add `sdk.dir=/opt/homebrew/share/android-commandlinetools` to a `local.properties` file at the project root.
- **`Could not resolve io.github.webrtc-sdk:android`** — the WebRTC SDK is on Maven Central. If the build hangs, you may be behind a Maven Central proxy. The artifact resolves at `https://repo1.maven.org/maven2/io/github/webrtc-sdk/android/`.
- **`Plugin requested with version '2.0.21' is not found`** — Kotlin 2.0 requires the `kotlin-compose` Gradle plugin (already wired in `gradle/libs.versions.toml`). Run `./gradlew --refresh-dependencies` if cache got stale.
- **`Unable to strip the following libraries`** — harmless warning, just means the build skipped stripping debug symbols from native `.so` files. Doesn't affect runtime.

## Updating dependency versions

All versions are pinned in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml). Bumping a version in one place updates every module that depends on it.

## Generating QR codes (for the part labels)

```bash
cd tools
npm install qrcode
node generate-qr-codes.mjs
```

PNGs land in `tools/qr-codes/`. Open `print-sheet.html` in a browser to print all three on one page.
