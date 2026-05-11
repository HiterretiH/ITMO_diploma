from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Iterator, List, Optional, Sequence, Tuple

from docx import Document
from docx.document import Document as DocumentObject
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.opc.constants import RELATIONSHIP_TYPE as RT

from utilities.docx_form_prep.sdt import _make_run, make_sdt_element, max_sdt_id as max_sdt_id_in_root

W_TBL = qn("w:tbl")
W_TC = qn("w:tc")
W_TR = qn("w:tr")
W_R = qn("w:r")
W_T = qn("w:t")
W_FOOTNOTE = qn("w:footnote")
W_ENDNOTE = qn("w:endnote")


@dataclass
class ConversionStats:
    found_by_bucket: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    replaced_by_bucket: dict[str, int] = field(default_factory=lambda: defaultdict(int))

    def add_found(self, bucket: str, n: int = 1) -> None:
        self.found_by_bucket[bucket] += n

    def add_replaced(self, bucket: str, n: int = 1) -> None:
        self.replaced_by_bucket[bucket] += n


def _stat_bucket(ctx: str, in_table: bool) -> str:
    if ctx in ("HEADER", "FOOTER"):
        return "headers_footers"
    if ctx == "FOOTNOTE":
        return "footnotes"
    if ctx == "ENDNOTE":
        return "endnotes"
    if in_table:
        return "tables"
    return "body_text"


def _compile_field_patterns(fields: Sequence[dict]) -> List[Tuple[re.Pattern[str], dict]]:
    out: List[Tuple[re.Pattern[str], dict]] = []
    for fd in fields:
        ph = fd["placeholder"]
        if fd.get("is_regex"):
            flags = re.DOTALL if fd.get("regex_dotall") else 0
            cre = re.compile(ph, flags)
        else:
            cre = re.compile(re.escape(str(ph)))
        out.append((cre, fd))
    return out


def _collect_w_t_chunks(p_el: Any) -> List[dict[str, Any]]:
    chunks: List[dict[str, Any]] = []
    for r_el in p_el.iterdescendants():
        if r_el.tag != W_R:
            continue
        for t_el in r_el.findall(W_T):
            txt = t_el.text if t_el.text is not None else ""
            chunks.append({"r": r_el, "t": t_el, "text": txt})
    return chunks


def _full_text(chunks: Sequence[dict[str, Any]]) -> str:
    return "".join(c["text"] for c in chunks)


def _rpr_at_char_index(chunks: Sequence[dict[str, Any]], index: int) -> Any:
    pos = 0
    for ch in chunks:
        ln = len(ch["text"])
        if pos + ln > index:
            return ch["r"].find(qn("w:rPr"))
        pos += ln
    if chunks:
        return chunks[-1]["r"].find(qn("w:rPr"))
    return None


def _slice_to_run_specs(
    chunks: Sequence[dict[str, Any]], start: int, end: int
) -> List[Tuple[str, Any]]:
    specs: List[Tuple[str, Any]] = []
    pos = 0
    for ch in chunks:
        text = ch["text"]
        ln = len(text)
        lo = max(0, start - pos)
        hi = min(ln, end - pos)
        if lo < hi:
            sub = text[lo:hi]
            r_pr = ch["r"].find(qn("w:rPr"))
            specs.append((sub, r_pr))
        pos += ln
        if pos >= end:
            break
    return specs


def _collect_non_overlapping_matches(
    full_text: str, compiled: Sequence[Tuple[re.Pattern[str], dict]]
) -> List[Tuple[int, int, dict]]:
    chosen: List[Tuple[int, int, dict]] = []
    for cre, fd in compiled:
        for m in cre.finditer(full_text):
            s, e = m.start(), m.end()
            if s >= e:
                continue
            if all(e <= s2 or s >= e2 for s2, e2, _ in chosen):
                chosen.append((s, e, fd))
    chosen.sort(key=lambda x: x[0])
    return chosen


def _clear_p_children_except_ppr(p_el: Any) -> None:
    for child in list(p_el):
        if child.tag != qn("w:pPr"):
            p_el.remove(child)


def _append_run_specs(p_el: Any, specs: Sequence[Tuple[str, Any]]) -> None:
    for sub, r_pr in specs:
        p_el.append(_make_run(sub, r_pr))


def _default_for_field(fd: dict) -> str | None:
    if "default_text" not in fd:
        return None
    return fd.get("default_text")


def _replace_paragraph_inline(
    p_el: Any,
    matches: Sequence[Tuple[int, int, dict]],
    chunks: Sequence[dict[str, Any]],
    full_len: int,
    next_sdt_id: Callable[[], int],
    *,
    sdt_in_table: bool,
) -> int:
    _clear_p_children_except_ppr(p_el)
    last = 0
    replaced = 0
    for s, e, fd in sorted(matches, key=lambda x: x[0]):
        _append_run_specs(p_el, _slice_to_run_specs(chunks, last, s))
        p_el.append(
            make_sdt_element(
                fd["field_name"],
                _default_for_field(fd),
                _rpr_at_char_index(chunks, s),
                next_sdt_id(),
                in_table=sdt_in_table,
            )
        )
        last = e
        replaced += 1
    _append_run_specs(p_el, _slice_to_run_specs(chunks, last, full_len))
    return replaced


def _process_paragraph_element(
    p_el: Any,
    ctx: str,
    in_table: bool,
    compiled: Sequence[Tuple[re.Pattern[str], dict]],
    next_sdt_id: Callable[[], int],
    stats: ConversionStats,
) -> int:
    chunks = _collect_w_t_chunks(p_el)
    if not chunks:
        return 0
    full = _full_text(chunks)
    if not full:
        return 0
    matches = _collect_non_overlapping_matches(full, compiled)
    if not matches:
        return 0
    bucket = _stat_bucket(ctx, in_table)
    stats.add_found(bucket, len(matches))
    full_len = len(full)
    tc_el = None
    if in_table:
        tc_el = p_el.getparent()
        while tc_el is not None and tc_el.tag != W_TC:
            tc_el = tc_el.getparent()
    sdt_in_table = in_table and tc_el is not None
    n = _replace_paragraph_inline(
        p_el, matches, chunks, full_len, next_sdt_id, sdt_in_table=sdt_in_table
    )
    stats.add_replaced(bucket, n)
    return n


def _walk_block_elements(
    container: Any,
    ctx: str,
    in_table: bool,
    compiled: Sequence[Tuple[re.Pattern[str], dict]],
    next_sdt_id: Callable[[], int],
    stats: ConversionStats,
) -> int:
    total = 0
    for child in list(container):
        tag = child.tag
        if tag == qn("w:p"):
            total += _process_paragraph_element(child, ctx, in_table, compiled, next_sdt_id, stats)
        elif tag == W_TBL:
            total += _walk_table(child, ctx, compiled, next_sdt_id, stats)
        elif tag == W_TC:
            total += _walk_block_elements(child, ctx, True, compiled, next_sdt_id, stats)
        elif tag in (W_FOOTNOTE, W_ENDNOTE):
            total += _walk_block_elements(child, ctx, in_table, compiled, next_sdt_id, stats)
        else:
            total += _walk_block_elements(child, ctx, in_table, compiled, next_sdt_id, stats)
    return total


def _walk_table(
    tbl: Any,
    ctx: str,
    compiled: Sequence[Tuple[re.Pattern[str], dict]],
    next_sdt_id: Callable[[], int],
    stats: ConversionStats,
) -> int:
    total = 0
    for tr in tbl.findall(W_TR):
        for tc in tr.findall(W_TC):
            total += _walk_block_elements(tc, ctx, True, compiled, next_sdt_id, stats)
    return total


def _iter_header_footer_roots(doc: DocumentObject) -> Iterator[Tuple[str, Any]]:
    seen: set[str] = set()
    for section in doc.sections:
        for label, hf in (
            ("HEADER", section.header),
            ("FOOTER", section.footer),
        ):
            part = hf.part
            key = str(part.partname)
            if key not in seen:
                seen.add(key)
                yield label, part.element
        if section.different_first_page_header_footer:
            for label, hf in (
                ("HEADER", section.first_page_header),
                ("FOOTER", section.first_page_footer),
            ):
                part = hf.part
                key = str(part.partname)
                if key not in seen:
                    seen.add(key)
                    yield label, part.element
        for label, hf in (
            ("HEADER", section.even_page_header),
            ("FOOTER", section.even_page_footer),
        ):
            part = hf.part
            key = str(part.partname)
            if key not in seen:
                seen.add(key)
                yield label, part.element


def _footnote_endnote_roots(doc: DocumentObject) -> Iterator[Tuple[str, Any]]:
    for rel in doc.part.rels.values():
        if rel.reltype == RT.FOOTNOTES:
            part = rel.target_part
            el = getattr(part, "element", None)
            if el is not None:
                yield "FOOTNOTE", el
        elif rel.reltype == RT.ENDNOTES:
            part = rel.target_part
            el = getattr(part, "element", None)
            if el is not None:
                yield "ENDNOTE", el


def _global_max_sdt_id(doc: DocumentObject) -> int:
    m = 0
    roots: List[Any] = [doc.element.body]
    for _, root in _iter_header_footer_roots(doc):
        roots.append(root)
    for _, root in _footnote_endnote_roots(doc):
        roots.append(root)
    for root in roots:
        m = max(m, max_sdt_id_in_root(root))
    return m


def convert_document(
    doc: DocumentObject,
    fields: Sequence[dict],
    stats: Optional[ConversionStats] = None,
) -> ConversionStats:
    stats = stats or ConversionStats()
    compiled = _compile_field_patterns(fields)
    base_id = _global_max_sdt_id(doc)
    counter = {"n": base_id}

    def next_sdt_id() -> int:
        counter["n"] += 1
        return counter["n"]

    for _ in range(1000):
        round_repl = _walk_block_elements(doc.element.body, "BODY", False, compiled, next_sdt_id, stats)
        for label, root in _iter_header_footer_roots(doc):
            round_repl += _walk_block_elements(root, label, False, compiled, next_sdt_id, stats)
        for kind, root in _footnote_endnote_roots(doc):
            ctx = "FOOTNOTE" if kind == "FOOTNOTE" else "ENDNOTE"
            tag = W_FOOTNOTE if kind == "FOOTNOTE" else W_ENDNOTE
            for fn in root.findall(tag):
                if fn.get(qn("w:type")) in ("separator", "continuationSeparator"):
                    continue
                round_repl += _walk_block_elements(fn, ctx, False, compiled, next_sdt_id, stats)
        if round_repl == 0:
            break
    return stats


def run_on_paths(input_path: str, output_path: str, fields: Sequence[dict]) -> ConversionStats:
    out = Path(output_path)
    out.parent.mkdir(parents=True, exist_ok=True)
    doc = Document(input_path)
    stats = convert_document(doc, fields)
    doc.save(str(out))
    return stats
