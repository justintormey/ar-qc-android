# Changelog

All notable changes to this project will be documented in this file. The project follows [Semantic Versioning](https://semver.org/).

## [0.1.0] - 2026-08-18

SemVer baseline established. `versionName` in `app/build.gradle.kts` was already `0.1.0`; adopted as-is.

- 2026-05-20 — Initial commit: native Android port of the `ar-qc-demo` HTML app (Kotlin, Jetpack Compose, CameraX, ML Kit barcode scanning, WebRTC transport) for the DigiLens Argo headset.
- 2026-05-20 — Added the AR Builder sibling app (second product flavor) alongside QC, plus Argo scroll-wheel input support and QC flow refresh.
- 2026-05-20 — Shipped both flavor APKs with wheel-input fix, dedupe, and polish for Argo demos.
- 2026-05-20 — Re-themed demo parts from sheet metal to 3D-printed props; refreshed Builder procedure to 4 angle brackets / 3 assembly steps.
- 2026-05-21 — Parked orientation-alignment CV as a documented future enhancement.
- 2026-05-21 — Added CI workflow to build and upload both flavor APKs.
