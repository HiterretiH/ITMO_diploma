#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from dataclasses import dataclass, field
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

CONTEXT_KEYS = frozenset(
    {
        "number",
        "date",
        "word_date",
        "loading_place",
        "unloading_place",
        "contact_loading",
        "contact_unloading",
        "count",
        "price",
        "total_price",
        "word_price",
        "performer_info_ws",
        "customer_info_ws",
        "performer_name",
        "performer_full_name",
        "performer_info",
        "performer_phone",
        "performer_bank",
        "performer_vehicle",
        "performer_vehicle_number",
        "performer_driver",
        "performer_driver_phone",
        "performer_vehicle_type",
        "performer_inn",
        "performer_bik",
        "performer_kpp",
        "performer_rsh",
        "performer_ksh",
        "customer_name",
        "customer_full_name",
        "customer_info",
        "customer_phone",
        "customer_bank",
        "orderId",
        "shipperName",
        "shipperInn",
        "shipperAddress",
        "consigneeName",
        "consigneeInn",
        "consigneeAddress",
        "cargoDescription",
        "cargoWeightKg",
        "routeFrom",
        "routeTo",
        "loadDate",
        "unloadDate",
        "driverName",
        "driverLicense",
        "vehiclePlate",
        "vehicleModel",
        "vehicleCapacityKg",
        "priceAmount",
        "currency",
    }
)

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
W_P = f"{{{W_NS}}}p"
W_T = f"{{{W_NS}}}t"

PLACEHOLDER_RE = re.compile(r"\{\{\s*([^}]+?)\s*\}\}")

FORBIDDEN_NAME_PREFIXES = (
    "word/header",
    "word/footer",
    "word/footnotes.xml",
    "word/endnotes.xml",
)


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def template_paths(root: Path) -> list[Path]:
    base = root / "backend" / "src" / "main" / "resources" / "templates" / "documents"
    names = ("contract_application.docx", "waybill.docx", "act_of_work.docx")
    paths = [base / n for n in names]
    for p in paths:
        if not p.is_file():
            print(f"ERROR: missing template file: {p}", file=sys.stderr)
            sys.exit(1)
    return paths


def check_no_placeholders_outside_body(z: zipfile.ZipFile) -> list[str]:
    errors: list[str] = []
    for name in z.namelist():
        if not name.endswith(".xml"):
            continue
        if name == "word/document.xml":
            continue
        if not name.startswith("word/"):
            continue
        if not any(name.startswith(pref) or name == pref for pref in FORBIDDEN_NAME_PREFIXES):
            continue
        data = z.read(name).decode("utf-8", errors="replace")
        if "{{" in data:
            errors.append(f"  Found '{{{{' in non-body part: {name}")
    return errors


def paragraph_wt_segments(paragraph: ET.Element) -> list[tuple[ET.Element, str]]:
    out: list[tuple[ET.Element, str]] = []
    for t_el in paragraph.iter(W_T):
        out.append((t_el, t_el.text or ""))
    return out


@dataclass
class DocumentCheckResult:
    multi_wt: list[tuple[int, str, str]] = field(default_factory=list)
    unknown_key: list[tuple[int, str, str]] = field(default_factory=list)
    empty_placeholder: list[tuple[int, str]] = field(default_factory=list)


def analyze_document_xml(document_xml: bytes) -> DocumentCheckResult:
    res = DocumentCheckResult()
    root = ET.fromstring(document_xml)
    body = root.find(f".//{{{W_NS}}}body")
    if body is None:
        res.empty_placeholder.append((-1, "no w:body"))
        return res

    for pi, p_el in enumerate(body.iter(W_P)):
        segments = paragraph_wt_segments(p_el)
        if not segments:
            continue
        full = "".join(t for _, t in segments)
        if "{{" not in full:
            continue
        offsets: list[tuple[int, int, int]] = []
        pos = 0
        for seg_i, (_, text) in enumerate(segments):
            offsets.append((pos, pos + len(text), seg_i))
            pos += len(text)

        for m in PLACEHOLDER_RE.finditer(full):
            inner = m.group(1).strip()
            literal = m.group(0)
            if not inner:
                res.empty_placeholder.append((pi, str(m.span())))
                continue
            if inner not in CONTEXT_KEYS:
                res.unknown_key.append((pi, inner, literal))
            ms, me = m.start(), m.end()
            seg_indices = {seg_i for (a, b, seg_i) in offsets if not (b <= ms or a >= me)}
            if len(seg_indices) > 1:
                res.multi_wt.append((pi, inner, literal))
    return res


def format_document_issues(path: Path, r: DocumentCheckResult) -> list[str]:
    lines: list[str] = []
    for pi, inner, lit in r.unknown_key:
        lines.append(f"  UNKNOWN_KEY  w:p#{pi}  key={inner!r}  {lit!r}")
    for pi, inner, lit in r.multi_wt:
        lines.append(f"  MULTI_W_T    w:p#{pi}  key={inner!r}  {lit!r}")
    for pi, span in r.empty_placeholder:
        if pi < 0:
            lines.append(f"  EMPTY_OR_XML  {span}")
        else:
            lines.append(f"  EMPTY_PLACEHOLDER  w:p#{pi}  span={span}")
    return lines


def check_document_xml(document_xml: bytes) -> tuple[list[str], list[str]]:
    r = analyze_document_xml(document_xml)
    errors: list[str] = []
    for pi, inner, lit in r.unknown_key:
        errors.append(f"  w:p#{pi}: unknown key {inner!r} (not in snapshotToContext) in {lit!r}")
    for pi, inner, lit in r.multi_wt:
        errors.append(
            f"  w:p#{pi}: placeholder {lit!r} spans multiple w:t "
            f"(key={inner!r}); not safe for single-w:t replacement"
        )
    for pi, span in r.empty_placeholder:
        if pi < 0:
            errors.append(f"  {span}")
        else:
            errors.append(f"  w:p#{pi}: empty placeholder at span {span}")
    return errors, []


def summarize_placeholders(document_xml: bytes) -> set[str]:
    found: set[str] = set()
    root = ET.fromstring(document_xml)
    body = root.find(f".//{{{W_NS}}}body")
    if body is None:
        return found
    for p_el in body.iter(W_P):
        full = "".join(t for _, t in paragraph_wt_segments(p_el))
        for m in PLACEHOLDER_RE.finditer(full):
            inner = m.group(1).strip()
            if inner:
                found.add(inner)
    return found


def main() -> int:
    root = repo_root()
    paths = template_paths(root)
    print("DOCX template placeholder check")
    print(f"Repository: {root}")
    print(f"Context keys (Java snapshotToContext): {len(CONTEXT_KEYS)}")
    print()

    any_failed = False
    all_used_keys: set[str] = set()
    consolidated: list[str] = []

    for path in paths:
        print(f"=== {path.name} ===")
        with zipfile.ZipFile(path, "r") as z:
            e1 = check_no_placeholders_outside_body(z)
            if e1:
                any_failed = True
                consolidated.extend(f"[{path.name}] HEADER_OR_FOOTER_OR_NOTE  {line.strip()}" for line in e1)
                print("FAIL: placeholders outside word/document.xml")
                for line in e1:
                    print(line)
            else:
                print("OK: no '{{' in headers/footers/footnotes/endnotes")

            doc_xml = z.read("word/document.xml")
            used = summarize_placeholders(doc_xml)
            all_used_keys |= used
            print(f"Placeholders in body: {len(used)} unique -> {sorted(used)}")

            report = analyze_document_xml(doc_xml)
            e2 = format_document_issues(path, report)
            if report.unknown_key or report.multi_wt or report.empty_placeholder:
                any_failed = True
                for pi, inner, lit in report.unknown_key:
                    consolidated.append(
                        f"[{path.name}] UNKNOWN_KEY     key={inner!r}  w:p#{pi}  literal={lit!r}"
                    )
                for pi, inner, lit in report.multi_wt:
                    consolidated.append(
                        f"[{path.name}] MULTI_W_T       key={inner!r}  w:p#{pi}  literal={lit!r}"
                    )
                for pi_e, span in report.empty_placeholder:
                    consolidated.append(f"[{path.name}] EMPTY           w:p#{pi_e}  span={span}")
                print("FAIL: document.xml structure / unknown keys")
                for line in e2:
                    print(line)
            else:
                print("OK: every {{...}} is a known key and fits in one w:t per paragraph scan")
        print()

    unused = CONTEXT_KEYS - all_used_keys
    print("--- Summary ---")
    print(f"Union of placeholder keys across three templates: {len(all_used_keys)}")
    print(f"Context keys never used in any of these three files: {len(unused)} (informational)")
    if len(unused) <= 40:
        print(f"  {sorted(unused)}")

    if consolidated:
        print()
        print("=== All fields / checks that did NOT pass ===")
        for line in consolidated:
            print(line)

    if any_failed:
        print("RESULT: FAILED")
        return 1
    print("RESULT: PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
