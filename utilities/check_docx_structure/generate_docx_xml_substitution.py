#!/usr/bin/env python3
from __future__ import annotations

import io
import re
import sys
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

from fields import DOCX_CONTEXT

TEMPLATE_NAMES = ("contract_application.docx", "waybill.docx", "act_of_work.docx")


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def template_dir(root: Path) -> Path:
    return root / "backend" / "src" / "main" / "resources" / "templates" / "documents"


def escape_for_wt_text(value: str) -> str:
    t = value.replace("\r\n", "\n").replace("\r", "\n")
    t = escape(t, entities={"'": "&apos;", '"': "&quot;"})
    return t.replace("\n", "&#10;")


def substitute_document_xml(xml: str, context: dict[str, str]) -> tuple[str, list[str]]:
    out = xml
    for key in sorted(context.keys(), key=len, reverse=True):
        val = context[key]
        if val is None:
            continue
        pat = re.compile(r"\{\{\s*" + re.escape(key) + r"\s*\}\}")
        out = pat.sub(escape_for_wt_text(str(val)), out)
    remaining = re.findall(r"\{\{\s*([^}]+?)\s*\}\}", out)
    warnings = [f"Unresolved placeholder: {x.strip()!r}" for x in remaining]
    return out, warnings


def generate_one(template_path: Path, out_path: Path, context: dict[str, str]) -> list[str]:
    buf = io.BytesIO()
    warnings: list[str] = []
    with zipfile.ZipFile(template_path, "r") as zin:
        with zipfile.ZipFile(buf, "w", compression=zipfile.ZIP_DEFLATED) as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == "word/document.xml":
                    text = data.decode("utf-8")
                    text, w = substitute_document_xml(text, context)
                    warnings.extend(w)
                    data = text.encode("utf-8")
                zout.writestr(info, data)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_bytes(buf.getvalue())
    return warnings


def main() -> int:
    root = repo_root()
    tdir = template_dir(root)
    out_dir = Path(__file__).resolve().parent / "out"
    print("XML substitution DOCX generator (POC)")
    print(f"Templates: {tdir}")
    print(f"Output:    {out_dir}")
    print()

    any_warn = False
    for name in TEMPLATE_NAMES:
        src = tdir / name
        if not src.is_file():
            print(f"ERROR: missing {src}", file=sys.stderr)
            return 1
        dst = out_dir / f"{src.stem}.xml-subst.docx"
        w = generate_one(src, dst, DOCX_CONTEXT)
        print(f"Wrote {dst.name} ({dst.stat().st_size} bytes)")
        if w:
            any_warn = True
            for line in w:
                print(f"  WARN {name}: {line}")
        else:
            print(f"  OK: no unresolved {{{{...}}}} in document.xml")
        print()

    if any_warn:
        print("Done with warnings (unresolved placeholders may be intentional if not in template).")
    else:
        print("Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
