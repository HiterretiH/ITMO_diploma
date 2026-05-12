# Manual-review document samples

Generated PDF and DOCX files for visual checks are written under **`build/manual-review-docs/`** in this module (Gradle output tree). That path is under `build/`, which is **gitignored** by `backend/.gitignore`; do not commit binaries from there.

## Regenerate

From the `backend` directory (Java 17 toolchain as in `build.gradle`):

```bash
./gradlew generateDocumentFixtures
```

This runs `DocumentFixtureGenerator` with output path **`backend/build/manual-review-docs/`**. It writes, per `DocumentType` and format:

- `act_of_work_sample.pdf`, `act_of_work_sample.docx`
- `contract_application_sample.pdf`, `contract_application_sample.docx`
- `waybill_sample.pdf`, `waybill_sample.docx`
- `context-preview.txt` (placeholder map dump)

Data source: `OrderPrintSnapshots.manualReviewDemo()` (same semantics as the Angular `order-print-demo.fixture.ts` trip-form demo).

## Timing report

`./gradlew test --tests DocumentGenerationTimingReportTest` writes `build/reports/document-generation-timing.txt` with per-step nanoseconds (machine-dependent; not tracked in git). Templates are preloaded via `DocumentTemplateCache` and `PdfFormTemplateCache`; the timed window is render/overlay only (no per-step classpath read).

## Default when running `main` without Gradle

If you run `DocumentFixtureGenerator` without arguments, output defaults to **`build/manual-review-docs`** relative to the process working directory. Prefer `./gradlew generateDocumentFixtures` from this module so files always land under `backend/build/manual-review-docs/`.
