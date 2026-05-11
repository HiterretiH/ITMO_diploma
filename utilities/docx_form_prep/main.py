from __future__ import annotations

import sys
import time
from pathlib import Path

_REPO = Path(__file__).resolve().parents[2]
if str(_REPO) not in sys.path:
    sys.path.insert(0, str(_REPO))

from docx import Document

from utilities.docx_form_prep.config import FIELDS, INPUT_FILE, OUTPUT_FILE
from utilities.docx_form_prep.converter import convert_document


def _print_stats(title: str, d: dict[str, int]) -> None:
    keys = ("body_text", "tables", "headers_footers", "footnotes", "endnotes")
    print(title)
    for k in keys:
        print(f"  {k}: {d.get(k, 0)}")
    for k in sorted(d):
        if k not in keys:
            print(f"  {k}: {d[k]}")


def main() -> None:
    t0 = time.perf_counter()
    doc = Document(INPUT_FILE)
    t1 = time.perf_counter()
    stats = convert_document(doc, FIELDS)
    t2 = time.perf_counter()
    out = Path(OUTPUT_FILE)
    out.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(out))
    t3 = time.perf_counter()
    print(f"Input:  {INPUT_FILE}")
    print(f"Output: {OUTPUT_FILE}")
    print()
    _print_stats("Placeholders found (by location):", dict(stats.found_by_bucket))
    print()
    _print_stats("Fields inserted (by location):", dict(stats.replaced_by_bucket))
    print()
    print(f"Load document:   {(t1 - t0) * 1000:.1f} ms")
    print(f"Convert in RAM:  {(t2 - t1) * 1000:.1f} ms")
    print(f"Save DOCX:       {(t3 - t2) * 1000:.1f} ms")
    print(f"Total:           {(t3 - t0) * 1000:.1f} ms")


if __name__ == "__main__":
    main()
