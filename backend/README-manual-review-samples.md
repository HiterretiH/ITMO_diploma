# Manual-review document samples

Generated PDF and DOCX files for visual checks live under `manual-review-docs/` in this module. That directory is **gitignored**; do not commit binaries from there.

## Regenerate

From the `backend` directory (Java 17 toolchain as in `build.gradle`):

```bash
./gradlew generateDocumentFixtures
```

This runs `DocumentFixtureGenerator` with output path `backend/manual-review-docs/`. It writes, per `DocumentType` and format:

- `act_of_work_sample.pdf`, `act_of_work_sample.docx`
- `contract_application_sample.pdf`, `contract_application_sample.docx`
- `waybill_sample.pdf`, `waybill_sample.docx`
- `context-preview.txt` (placeholder map dump)

Data source: `OrderPrintSnapshots.manualReviewDemo()` (same semantics as the Angular `order-print-demo.fixture.ts` trip-form demo).

## Timing baseline (committed)

`./gradlew test --tests DocumentGenerationTimingReportTest` refreshes `docs/document-generation-timing-baseline.txt` with per-step nanoseconds (machine-dependent).

## Default when running `main` without Gradle

If you run `DocumentFixtureGenerator` without arguments, output is relative to the process working directory (`manual-review-docs`); prefer the Gradle task so files always land under this module.
