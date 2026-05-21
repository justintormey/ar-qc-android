# Future enhancements

Parking spot for designed-but-not-implemented feature plans. Each entry is a
single Markdown file with enough detail to pick up cold — context, design,
files to modify, execution order, verification.

When a plan ships, leave the doc here as historical record (don't delete it)
and mention the shipping commit in `history.md` so the connection is searchable.

## Index

| Plan | Filed | Status | Summary |
|---|---|---|---|
| [cv-orientation-alignment.md](cv-orientation-alignment.md) | 2026-05-21 | Designed | Builder app: turn `BuilderViewModel.onBuilderFrame` into a first-class `StepRule` evaluator that can require QRs to be **oriented in the same direction** (via ML Kit `Barcode.cornerPoints`), not just co-visible. Also documents text-OCR and custom-icon paths as research-only future alternatives to QR codes. |
