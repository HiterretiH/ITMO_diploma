"""CLI for DOCX form prep: runs JOBS from config. Placeholder → SDT logic lives in converter.py."""

from __future__ import annotations

import sys
import time
from pathlib import Path

_REPO = Path(__file__).resolve().parents[2]
if str(_REPO) not in sys.path:
    sys.path.insert(0, str(_REPO))

from utilities.docx_form_prep.config import JOBS
from utilities.docx_form_prep.converter import ConversionStats, run_on_paths


def _resolve(path_str: str) -> Path:
    p = Path(path_str)
    return p if p.is_absolute() else _REPO / p


def _print_stats(title: str, d: dict[str, int]) -> None:
    keys = ("body_text", "tables", "headers_footers", "footnotes", "endnotes")
    print(title)
    for k in keys:
        print(f"  {k}: {d.get(k, 0)}")
    for k in sorted(d):
        if k not in keys:
            print(f"  {k}: {d[k]}")


def _rel_to_repo(path: Path) -> str:
    try:
        return str(path.relative_to(_REPO))
    except ValueError:
        return str(path)


def _merge_stats(total: ConversionStats, part: ConversionStats) -> None:
    for k, v in part.found_by_bucket.items():
        total.found_by_bucket[k] += v
    for k, v in part.replaced_by_bucket.items():
        total.replaced_by_bucket[k] += v


def main() -> None:
    t_all = time.perf_counter()
    grand = ConversionStats()
    for i, job in enumerate(JOBS, 1):
        inp = _resolve(job["input"])
        out = _resolve(job["output"])
        if not inp.is_file():
            print(f"Missing input file: {inp}", file=sys.stderr)
            sys.exit(1)
        print(f"[{i}/{len(JOBS)}] {_rel_to_repo(inp)}")
        t0 = time.perf_counter()
        stats = run_on_paths(str(inp), str(out), job["fields"])
        t1 = time.perf_counter()
        _merge_stats(grand, stats)
        _print_stats("  found:", dict(stats.found_by_bucket))
        _print_stats("  replaced:", dict(stats.replaced_by_bucket))
        print(f"  time: {(t1 - t0) * 1000:.1f} ms -> {_rel_to_repo(out)}")
        print()
    print("All jobs — totals:")
    _print_stats("  found:", dict(grand.found_by_bucket))
    _print_stats("  replaced:", dict(grand.replaced_by_bucket))
    print(f"Total wall time: {(time.perf_counter() - t_all) * 1000:.1f} ms")


if __name__ == "__main__":
    main()
