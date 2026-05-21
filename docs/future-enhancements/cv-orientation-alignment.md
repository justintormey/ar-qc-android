# Future enhancement — orientation-alignment CV + text/icon research

**Status:** Designed, not implemented. Captured as a future-enhancement plan.
**Date filed:** 2026-05-21
**Scope:** AR Builder app (Android primary, HTML secondary). No QC app changes.

## Context

The Builder app today drives verdicts off **presence** of QR codes — `BP` visible → pass, `CF` visible → fail, step 3 needs `BP` AND `CP` both visible. We have no information about *how* those QRs are oriented relative to each other in the frame, so a participant could attach bracket D at a right angle to C and step 3 would still pass as long as `BP` and `CP` happen to both face the camera.

Two extensions that strengthen the CV story without changing the physical prop kit:

1. **Multi-QR orientation alignment** — when two or more QR codes are required to be co-visible (currently just step 3, more in the future), additionally require them to be oriented within a small tolerance of each other. Catches misalignments that presence-only checks miss.
2. **Alternative recognition surfaces** — text labels (OCR) and icons could one day substitute for or augment the QR codes. Research only in this pass — document the trade-offs, no code changes.

Decisions captured during planning:

- Make orientation alignment a **first-class feature**: any Builder step can opt into it, not just step 3.
- Icons/text: **research only** — document the options, no implementation yet.
- Roll out on **both platforms, both clients** (Android Builder + HTML headset where the browser supports `getUserMedia`/`BarcodeDetector`; Argo Firefox stays operator-driven as today).

## Goal 1 — Orientation alignment as a first-class step rule

### Data flow

ML Kit's `Barcode` object already carries `cornerPoints: Array<Point>` (4 corners) and `boundingBox: Rect` per detected barcode. Today `QrAnalyzer` reads only `rawValue` and throws the rest away — see `app/src/main/kotlin/com/arqcdemo/app/camera/QrAnalyzer.kt`. The fix is to keep the spatial data and pass it up to the ViewModel alongside the code string.

Per-QR rotation is computed from any pair of adjacent corner points:

```kotlin
fun rotationDeg(corners: Array<Point>): Float {
    // top-left → top-right vector defines the QR's local x-axis
    val dx = (corners[1].x - corners[0].x).toFloat()
    val dy = (corners[1].y - corners[0].y).toFloat()
    return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()  // -180..180
}
```

Alignment between two QRs is then `abs(rotationDeg(a) - rotationDeg(b))` normalized to `[0, 180]` (rotations 180° apart are physically equivalent for a square QR, so we fold). Within tolerance → aligned.

### Step rule abstraction (Android)

Replace the hand-rolled `when (currentPart)` block in `BuilderViewModel.onBuilderFrame` with a declarative per-step rule:

```kotlin
data class StepRule(
    val requiredCodes: Set<String>,              // all must be present in the frame
    val failCodes: Set<String> = emptySet(),     // any present → FAIL
    val alignmentTolerance: Float? = null,       // degrees; null disables the alignment gate
    val alignmentCodes: Set<String> = emptySet(),// subset of requiredCodes that must co-align
)

private val STEP_RULES = mapOf(
    'A' to StepRule(requiredCodes = setOf("AP"), failCodes = setOf("AF")),
    'B' to StepRule(requiredCodes = setOf("BP"), failCodes = setOf("BF")),
    'C' to StepRule(
        requiredCodes = setOf("BP", "CP"),
        failCodes = setOf("CF"),
        alignmentTolerance = 15f,
        alignmentCodes = setOf("BP", "CP"),
    ),
)
```

Evaluation order (when scene is `Scanning`):

1. Any fail code in frame → `goVerdict(part, false)`. Short-circuits everything.
2. All `requiredCodes` present? No → keep scanning.
3. `alignmentTolerance` set? Check that every pair in `alignmentCodes` is within tolerance. Misaligned → keep scanning (user re-positions). Aligned → `goVerdict(part, true)`.
4. No alignment requirement and all required present → pass.

Keep-scanning (not auto-fail) on misalignment matches the existing "CP alone → keep scanning" UX — encourages re-positioning rather than punishing camera angle.

### QrAnalyzer refactor

Change the Builder callback from `(Set<String>) -> Unit` to `(Map<String, Float>) -> Unit` — a map from code to its rotation angle. Per-frame debouncing stays the same; we key the "did the set change" check on code names so small rotation jitter doesn't fire 30 times/sec.

```kotlin
private val onBuilderFrame: (codes: Map<String, Float>) -> Unit = {}
```

QC's `onQc(Char)` stays unchanged — QC is single-code and doesn't need alignment.

### HTML side

The HTML Builder headset's current camera pipeline is silent best-effort — `src/hud/camera.js` tries `getUserMedia` and gives up quietly when it fails (which it does on Argo Firefox). Wire up `BarcodeDetector` for browsers that support it:

- **Chrome / Edge / Android Chrome** — `new BarcodeDetector({ formats: ['qr_code'] })` returns `{rawValue, cornerPoints}` per detection. Same alignment math as Android.
- **Argo Firefox** — `getUserMedia` hangs; no video frames available. No CV path. The operator's button presses remain the only verdict source on this hardware (unchanged behavior).
- **Safari / older browsers** — `BarcodeDetector` is not supported. Falls back to operator-driven.

Mirror the Android `StepRule` shape in `src/builder.js` so both clients enforce identical logic. Same `builder-verdict {part, result}` bus message shape on the wire — alignment is computed and judged locally, then the verdict fires.

## Goal 2 — Text/icon recognition (research only, no code)

### Text labels via OCR

**Best fit if we ever migrate off QR codes.** Print word labels like `PASS A`, `FAIL A`, `PASS B`, etc. on each face of each bracket. The CV pipeline recognizes the text and uses it the same way it uses QR `rawValue` today.

- **Android**: `com.google.mlkit:text-recognition` (bundled, on-device, ~4 MB APK bump on top of the existing ~6 MB barcode model). Exposes `Text.TextBlock.boundingBox` AND `angle` directly — no atan2 math needed, rotation comes for free.
- **HTML**: Tesseract.js (~1 MB gzipped, slower than ML Kit by ~5-10×). Returns text + bounding box; rotation must be derived from box corners.

**Pros over QRs:** human-readable (operators can debug at a glance), no QR-generator step, larger labels = readable from further away.
**Cons:** lighting/contrast sensitive; stylized fonts confuse OCR; can confuse "P" / "F" / "PASS" / "FAIL" under poor conditions; latency 2–3× a barcode scan.

### Icons

Generic image labeling models (ML Kit Image Labeling, MobileNet) don't expose orientation and classify against generic ImageNet categories — useless for custom assembly-state glyphs. Would require a **custom TFLite classifier** trained on the specific iconography we choose (✓, ✗, →, ↑, etc.).

**Pros:** tiniest stickers, language-independent, can encode orientation by stylizing the glyph asymmetrically.
**Cons:** training-data + iteration loop required; one model per icon set; less robust to lighting than QR/text. Not recommended unless we hit a hard wall with QR + text.

### Recommendation

QRs are the right baseline. Text labels are a strong drop-in for any future demo where human-readability matters more than information density. Icons are a research path only.

## Files to modify (when implemented)

### Android (`ar-qc-android`)

- `app/src/main/kotlin/com/arqcdemo/app/camera/QrAnalyzer.kt` — emit `Map<String, Float>` (code → rotation angle) for Builder frames; preserve QC single-code path.
- `app/src/main/kotlin/com/arqcdemo/app/builder/BuilderViewModel.kt` — introduce `StepRule` data class + `STEP_RULES` map; replace `onBuilderFrame` evaluation with rule-driven logic; switch parameter from `Set<String>` to `Map<String, Float>`.
- `app/src/main/kotlin/com/arqcdemo/app/builder/ui/BuilderApp.kt` — update the Scanning-screen wiring to the new analyzer signature.
- `docs/CV-ALTERNATIVES.md` — NEW, short doc capturing the text-vs-icons research.
- `docs/BUILDER-PROP-KIT.md` — note the alignment-tolerance addition in step 3's troubleshooting section.
- `README.md` — mention `StepRule` + alignment in the "How the layers fit together" section.

### HTML (`ar-qc-html`)

- `src/hud/camera.js` — wire up `BarcodeDetector` per-frame loop (feature-detected), emit `{code, angle}` for each visible QR.
- `src/builder.js` — port `STEP_RULES` shape from Android; apply alignment-aware evaluation on `BarcodeDetector` output.
- `README.md` — note the in-browser CV path (Chrome/Edge headsets get auto-detection; Argo Firefox stays operator-driven).

## Execution order (when implemented)

1. Android `QrAnalyzer` refactor → emit `Map<String, Float>`.
2. Android `BuilderViewModel` `StepRule` abstraction + rule evaluator.
3. Android `BuilderApp` wiring update for the new analyzer signature.
4. HTML `src/hud/camera.js` `BarcodeDetector` wiring (feature-detected, no-op on Argo Firefox).
5. HTML `src/builder.js` port of `StepRule` shape + alignment-aware evaluation.
6. Docs touch-up — `BUILDER-PROP-KIT.md` step-3 troubleshooting note, both READMEs.
7. Build, sideload Builder APK, deploy HTML, verify end-to-end per the section below.

## Verification (when implemented)

- **Android log check**: tail `adb logcat -s ARQC.QR:V ARQC.Builder.VM:V` while running Builder. The new analyzer should log per-frame Builder maps like `Builder frame: {BP=12.4°, CP=-3.1°}`. Mis-aligning the brackets by ~30° should NOT pass step 3 — the log should show `step 3: required present, alignment 15.5° > tol 15° → keep scanning`. Aligning them within tolerance should pass.
- **Android demo run**: physical assembly on the Argo. Complete step 3 with D perpendicular to C (BP+CP both visible but at 90° rotation difference) → expect keep-scanning. Rotate the assembly so BP and CP top-edges are parallel → expect PASS.
- **HTML demo run**: open `/ar-qc/builder.html?transport=webrtc&room=526526` in Chrome on a laptop with a webcam. Hold a Builder bracket up to the webcam during step 3. Console should log the same alignment evaluation; verdict should fire identically.
- **Cross-platform parity**: Android headset + HTML controller in one room, then HTML-headset-Chrome + HTML controller in another — same physical assembly, same verdicts.
- **Backwards compat**: steps 1 and 2 (single-QR, no alignment) still fire normally on a single AP/AF or BP/BF sighting.
- **Bundle/perf**: APK size delta < 100 KB (no new dependencies — using existing `cornerPoints`). HTML bundle delta < 5 KB (BarcodeDetector is native).
