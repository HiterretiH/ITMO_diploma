from __future__ import annotations

import json
import re
import sys
import uuid
from collections import defaultdict
from copy import deepcopy
from pathlib import Path
from typing import Any, Callable, Iterator, List, Sequence, Tuple

from docx import Document
from docx.document import Document as DocumentObject
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.opc.constants import RELATIONSHIP_TYPE as RT

ROOT = Path(__file__).resolve().parents[2]
W_TBL = qn("w:tbl")
W_TC = qn("w:tc")
W_TR = qn("w:tr")
W_R = qn("w:r")
W_T = qn("w:t")
W_RPR = qn("w:rPr")
W_RFONTS = qn("w:rFonts")
W_B = qn("w:b")
W_I = qn("w:i")
W_SZ = qn("w:sz")
W_COLOR = qn("w:color")
W_U = qn("w:u")
W_FOOTNOTE = qn("w:footnote")
W_ENDNOTE = qn("w:endnote")
TAG_P = "LO_FORM_FIELD:"
_WD_H, _WD_V, _WD_CS, _WD_IN_TABLE = 5, 6, 1, 12


def _extract_font_info_from_rpr(r_pr: Any) -> dict:
    if r_pr is None:
        return {}
    info = {}
    rfonts = r_pr.find(W_RFONTS)
    if rfonts is not None:
        ascii_font = rfonts.get(qn("w:ascii"))
        if ascii_font:
            info["font_name"] = str(ascii_font)
        h_ansi = rfonts.get(qn("w:hAnsi"))
        if h_ansi:
            info["font_name"] = str(h_ansi)
    sz_el = r_pr.find(W_SZ)
    if sz_el is not None:
        val = sz_el.get(qn("w:val"))
        if val:
            info["font_size"] = float(val) / 2.0
    if r_pr.find(W_B) is not None:
        info["bold"] = True
    if r_pr.find(W_I) is not None:
        info["italic"] = True
    color_el = r_pr.find(W_COLOR)
    if color_el is not None:
        val = color_el.get(qn("w:val"))
        if val:
            info["color"] = f"#{str(val)}"
    u_el = r_pr.find(W_U)
    if u_el is not None:
        info["underline"] = True
    return info


def _encode_font_info(font_info: dict) -> str:
    return json.dumps(font_info, ensure_ascii=False, separators=(",", ":"))


def _decode_font_info(tag: str) -> dict:
    try:
        prefix = TAG_P + "FONT:"
        if tag.startswith(prefix):
            return json.loads(tag[len(prefix):])
        return {}
    except Exception:
        return {}


def _make_run(text: str, r_pr: Any | None) -> OxmlElement:
    r = OxmlElement("w:r")
    if r_pr is not None:
        r.append(deepcopy(r_pr))
    t = OxmlElement("w:t")
    t.text = text or ""
    if text and (text[0].isspace() or text[-1].isspace() or "  " in text):
        t.set(qn("xml:space"), "preserve")
    r.append(t)
    return r


def make_sdt_element(
    field_name: str, default_text: str | None, r_pr: Any | None, sdt_id: int, in_table: bool
) -> OxmlElement:
    font_info = _extract_font_info_from_rpr(r_pr)
    font_tag = f"{TAG_P}FONT:{_encode_font_info(font_info)}"
    
    sdt = OxmlElement("w:sdt")
    pr = OxmlElement("w:sdtPr")
    id_el = OxmlElement("w:id")
    id_el.set(qn("w:val"), str(sdt_id))
    pr.append(id_el)
    tag = OxmlElement("w:tag")
    tag.set(qn("w:val"), f"{TAG_P}{field_name}|{font_tag}")
    pr.append(tag)
    al = OxmlElement("w:alias")
    al.set(qn("w:val"), field_name)
    pr.append(al)
    if in_table:
        pr.append(OxmlElement("w:text"))
        ml = OxmlElement("w:multiLine")
        ml.set(qn("w:val"), "1")
        pr.append(ml)
    else:
        pr.append(OxmlElement("w:richText"))
    sdt.append(pr)
    c = OxmlElement("w:sdtContent")
    c.append(_make_run("" if default_text is None else default_text, r_pr))
    sdt.append(c)
    return sdt


def max_sdt_id(root) -> int:
    m = 0
    for sdt in root.iter(qn("w:sdt")):
        p = sdt.find(qn("w:sdtPr"))
        if p is None:
            continue
        id_el = p.find(qn("w:id"))
        if id_el is None:
            continue
        v = id_el.get(qn("w:val"))
        if v is None:
            continue
        try:
            m = max(m, int(v))
        except ValueError:
            pass
    return m


def _compile(fields: Sequence[dict]) -> List[Tuple[re.Pattern[str], dict]]:
    o: List[Tuple[re.Pattern[str], dict]] = []
    for fd in fields:
        ph = fd["placeholder"]
        if fd.get("is_regex"):
            o.append((re.compile(ph, re.DOTALL if fd.get("regex_dotall") else 0), fd))
        else:
            o.append((re.compile(re.escape(str(ph))), fd))
    return o


def _chunks(p_el: Any) -> List[dict[str, Any]]:
    ch: List[dict[str, Any]] = []
    for r_el in p_el.iterdescendants():
        if r_el.tag != W_R:
            continue
        for t_el in r_el.findall(W_T):
            ch.append({"r": r_el, "t": t_el, "text": t_el.text or ""})
    return ch


def _full(ch: Sequence[dict[str, Any]]) -> str:
    return "".join(c["text"] for c in ch)


def _rpr_at(ch: Sequence[dict[str, Any]], index: int) -> Any:
    pos = 0
    for c in ch:
        ln = len(c["text"])
        if pos + ln > index:
            return c["r"].find(W_RPR)
        pos += ln
    return ch[-1]["r"].find(W_RPR) if ch else None


def _slice_specs(ch: Sequence[dict[str, Any]], start: int, end: int) -> List[Tuple[str, Any]]:
    sp: List[Tuple[str, Any]] = []
    pos = 0
    for c in ch:
        text = c["text"]
        ln = len(text)
        lo, hi = max(0, start - pos), min(ln, end - pos)
        if lo < hi:
            sp.append((text[lo:hi], c["r"].find(W_RPR)))
        pos += ln
        if pos >= end:
            break
    return sp


def _matches(full: str, compiled: Sequence[Tuple[re.Pattern[str], dict]]) -> List[Tuple[int, int, dict]]:
    chosen: List[Tuple[int, int, dict]] = []
    for cre, fd in compiled:
        for m in cre.finditer(full):
            s, e = m.start(), m.end()
            if s >= e:
                continue
            if all(e <= s2 or s >= e2 for s2, e2, _ in chosen):
                chosen.append((s, e, fd))
    chosen.sort(key=lambda x: x[0])
    return chosen


def _clear_p(p_el: Any) -> None:
    for child in list(p_el):
        if child.tag != qn("w:pPr"):
            p_el.remove(child)


def _append_specs(p_el: Any, specs: Sequence[Tuple[str, Any]]) -> None:
    for sub, r_pr in specs:
        p_el.append(_make_run(sub, r_pr))


def _def_txt(fd: dict) -> str | None:
    return fd.get("default_text") if "default_text" in fd else None


def _find_parent_table_context(p_el: Any) -> tuple[bool, Any | None]:
    parent = p_el.getparent()
    while parent is not None:
        if parent.tag == W_TC:
            return True, parent
        elif parent.tag == W_TBL:
            return False, None
        parent = parent.getparent()
    return False, None


def _replace_p(
    p_el: Any,
    ms: Sequence[Tuple[int, int, dict]],
    ch: Sequence[dict[str, Any]],
    flen: int,
    next_id: Callable[[], int],
    *,
    in_tbl_cell: bool,
) -> int:
    _clear_p(p_el)
    last, n = 0, 0
    for s, e, fd in sorted(ms, key=lambda x: x[0]):
        _append_specs(p_el, _slice_specs(ch, last, s))
        sdt = make_sdt_element(
            fd["field_name"], _def_txt(fd), _rpr_at(ch, s), next_id(), in_table=in_tbl_cell
        )
        p_el.append(sdt)
        last, n = e, n + 1
    _append_specs(p_el, _slice_specs(ch, last, flen))
    return n


def _proc_p(
    p_el: Any, ctx: str, in_table: bool, compiled: Sequence[Tuple[re.Pattern[str], dict]], next_id: Callable[[], int]
) -> int:
    ch = _chunks(p_el)
    if not ch:
        return 0
    full = _full(ch)
    if not full:
        return 0
    ms = _matches(full, compiled)
    if not ms:
        return 0
    flen = len(full)
    is_in_cell, tc = _find_parent_table_context(p_el)
    in_tbl_cell = in_table and is_in_cell
    return _replace_p(p_el, ms, ch, flen, next_id, in_tbl_cell=in_tbl_cell)


def _walk(
    container: Any,
    ctx: str,
    in_table: bool,
    compiled: Sequence[Tuple[re.Pattern[str], dict]],
    next_id: Callable[[], int],
) -> int:
    tot = 0
    for child in list(container):
        tag = child.tag
        if tag == qn("w:p"):
            tot += _proc_p(child, ctx, in_table, compiled, next_id)
        elif tag == W_TBL:
            for tr in child.findall(W_TR):
                for tc in tr.findall(W_TC):
                    tot += _walk(tc, ctx, True, compiled, next_id)
        elif tag == W_TC:
            tot += _walk(child, ctx, True, compiled, next_id)
        elif tag in (W_FOOTNOTE, W_ENDNOTE):
            tot += _walk(child, ctx, in_table, compiled, next_id)
        else:
            tot += _walk(child, ctx, in_table, compiled, next_id)
    return tot


def _hf_roots(doc: DocumentObject) -> Iterator[Tuple[str, Any]]:
    seen: set[str] = set()
    for sec in doc.sections:
        for label, hf in (("HEADER", sec.header), ("FOOTER", sec.footer)):
            key = str(hf.part.partname)
            if key not in seen:
                seen.add(key)
                yield label, hf.part.element
        if sec.different_first_page_header_footer:
            for label, hf in (("HEADER", sec.first_page_header), ("FOOTER", sec.first_page_footer)):
                key = str(hf.part.partname)
                if key not in seen:
                    seen.add(key)
                    yield label, hf.part.element
        for label, hf in (("HEADER", sec.even_page_header), ("FOOTER", sec.even_page_footer)):
            key = str(hf.part.partname)
            if key not in seen:
                seen.add(key)
                yield label, hf.part.element


def _fn_en_roots(doc: DocumentObject) -> Iterator[Tuple[str, Any]]:
    for rel in doc.part.rels.values():
        if rel.reltype == RT.FOOTNOTES:
            el = getattr(rel.target_part, "element", None)
            if el is not None:
                yield "FOOTNOTE", el
        elif rel.reltype == RT.ENDNOTES:
            el = getattr(rel.target_part, "element", None)
            if el is not None:
                yield "ENDNOTE", el


def _global_max_sdt(doc: DocumentObject) -> int:
    m = 0
    roots: List[Any] = [doc.element.body]
    for _, r in _hf_roots(doc):
        roots.append(r)
    for _, r in _fn_en_roots(doc):
        roots.append(r)
    for r in roots:
        m = max(m, max_sdt_id(r))
    return m


def convert_document(doc: DocumentObject, fields: Sequence[dict]) -> None:
    compiled = _compile(fields)
    base = _global_max_sdt(doc)
    ctr = {"n": base}

    def next_id() -> int:
        ctr["n"] += 1
        return ctr["n"]

    for _ in range(1000):
        rr = _walk(doc.element.body, "BODY", False, compiled, next_id)
        for label, root in _hf_roots(doc):
            rr += _walk(root, label, False, compiled, next_id)
        for kind, root in _fn_en_roots(doc):
            ctx = "FOOTNOTE" if kind == "FOOTNOTE" else "ENDNOTE"
            tag = W_FOOTNOTE if kind == "FOOTNOTE" else W_ENDNOTE
            for fn in root.findall(tag):
                if fn.get(qn("w:type")) in ("separator", "continuationSeparator"):
                    continue
                rr += _walk(fn, ctx, False, compiled, next_id)
        if rr == 0:
            break


def run_on_paths(inp: str, outp: str, fields: Sequence[dict]) -> None:
    out = Path(outp)
    out.parent.mkdir(parents=True, exist_ok=True)
    doc = Document(inp)
    convert_document(doc, fields)
    doc.save(str(out))


def _cc_bbox(word_doc, rng, page_h: float) -> tuple[float, float, float, float]:
    r0 = rng.Duplicate
    r0.Collapse(_WD_CS)
    ls, ts = float(r0.Information(_WD_H)), float(r0.Information(_WD_V))
    if rng.Start >= rng.End:
        w, h = 120.0, 16.0
        return (ls, page_h - ts - h, ls + w, page_h - ts)
    r1 = rng.Duplicate
    r1.Collapse(0)
    le, te = float(r1.Information(_WD_H)), float(r1.Information(_WD_V))
    llx = min(ls, le) - 2.0
    urx = max(ls, le) + 8.0
    top_y = min(ts, te)
    bottom_y = max(ts, te)
    height = max(bottom_y - top_y, 14.0) + 2.0
    return (llx, page_h - top_y - height, urx, page_h - top_y)


def _cell_ctx(word_doc, rng):
    try:
        if int(rng.Information(_WD_IN_TABLE)) == 0:
            return None
        cell = rng.Cells(1)
        inner = cell.Range.Duplicate
        if int(inner.End) > int(inner.Start):
            inner.End = int(inner.End) - 1
        return cell, inner
    except Exception:
        return None


def _cc_font_info(word_doc, cc) -> dict:
    try:
        rng = cc.Range.Duplicate
        if rng.Start >= rng.End:
            return {"font_name": "Calibri", "font_size": 11.0}
        info = {}
        font = rng.Font
        info["font_name"] = str(font.Name)
        info["font_size"] = float(font.Size)
        info["bold"] = bool(font.Bold)
        info["italic"] = bool(font.Italic)
        try:
            color_val = int(font.Color)
            if color_val >= 0:
                info["color"] = f"#{color_val:06X}"
        except Exception:
            pass
        info["underline"] = bool(font.Underline)
        return info
    except Exception:
        return {"font_name": "Calibri", "font_size": 11.0}


def _cell_real_bottom(word_doc, cell, cell_top: float) -> float:
    height = float(cell.Height)
    if height < 9999998.0:
        return cell_top + height
    try:
        col_idx = cell.ColumnIndex
        tbl = cell.Range.Tables(1)
        all_cells = tbl.Range.Cells
        best_bottom = None
        for i in range(1, all_cells.Count + 1):
            try:
                c = all_cells(i)
                if c.ColumnIndex != col_idx:
                    continue
                h = float(c.Height)
                if h >= 9999998.0:
                    continue
                rng = c.Range.Duplicate
                rng.Collapse(_WD_CS)
                top = float(rng.Information(_WD_V))
                if top > cell_top + 1.0:
                    if best_bottom is None or top < best_bottom:
                        best_bottom = top
            except Exception:
                continue
        if best_bottom is not None:
            return best_bottom
        tbl_range = tbl.Range.Duplicate
        tbl_range.Collapse(0)
        return float(tbl_range.Information(_WD_V))
    except Exception:
        return cell_top + 20.0


def _widget_rect_pdf(word_doc, cc_rng, page_h: float) -> tuple[float, float, float, float]:
    f = _cc_bbox(word_doc, cc_rng, page_h)
    ctx = _cell_ctx(word_doc, cc_rng)
    if ctx is None:
        llx, lly, urx, ury = f
        page_width = float(word_doc.Sections(1).PageSetup.PageWidth)
        right_margin = float(word_doc.Sections(1).PageSetup.RightMargin)
        right_boundary = page_width - right_margin
        urx = right_boundary - 4.0
        if urx - llx < 40.0:
            urx = llx + 40.0
        return (llx, lly, urx, ury)
    cell, inner = ctx
    cell_range = cell.Range.Duplicate
    cell_range.Collapse(_WD_CS)
    cell_left = float(cell_range.Information(_WD_H))
    cell_top = float(cell_range.Information(_WD_V))
    cell_width = float(cell.Width)
    cell_real_bottom = _cell_real_bottom(word_doc, cell, cell_top)
    cell_right = cell_left + cell_width
    cell_bottom_pdf = page_h - cell_real_bottom
    field_left, field_bottom, field_right, field_top = f
    new_left = field_left
    new_bottom = min(field_bottom, cell_bottom_pdf + 1.0)
    new_right = cell_right - 4.0
    new_top = field_top
    if new_bottom >= new_top:
        new_bottom = new_top - 1.0
    if new_right <= new_left:
        new_right = new_left + 40.0
    return (new_left, new_bottom, new_right, new_top)


def _cc_rects(word_doc) -> dict[str, list[tuple[float, float, float, float]]]:
    ph = float(word_doc.Sections(1).PageSetup.PageHeight)
    by: dict[str, list[tuple[float, float, float, float]]] = defaultdict(list)
    total_cc = int(word_doc.ContentControls.Count)
    our_cc = []
    for i in range(1, total_cc + 1):
        cc = word_doc.ContentControls(i)
        t = str(cc.Tag or "")
        if t.startswith(TAG_P):
            our_cc.append((i, cc, t))
    
    print(f"  Content Controls всего: {total_cc}, наших: {len(our_cc)}", flush=True)
    
    for idx, (i, cc, t) in enumerate(our_cc):
        field_name = t[len(TAG_P):]
        if "|" in field_name:
            field_name = field_name.split("|")[0]
        print(f"  [{idx+1}/{len(our_cc)}] Поле: {field_name}", flush=True)
        rect = _widget_rect_pdf(word_doc, cc.Range, ph)
        by[field_name].append(rect)
    
    return dict(by)


def _cc_rects(word_doc) -> dict[str, list[tuple[float, float, float, float]]]:
    ph = float(word_doc.Sections(1).PageSetup.PageHeight)
    by: dict[str, list[tuple[float, float, float, float]]] = defaultdict(list)
    for i in range(1, int(word_doc.ContentControls.Count) + 1):
        cc = word_doc.ContentControls(i)
        t = str(cc.Tag or "")
        if not t.startswith(TAG_P):
            continue
        field_name = t[len(TAG_P):]
        if "|" in field_name:
            field_name = field_name.split("|")[0]
        rect = _widget_rect_pdf(word_doc, cc.Range, ph)
        by[field_name].append(rect)
    return dict(by)


def _cc_fonts(word_doc) -> dict[str, dict]:
    result = {}
    for i in range(1, int(word_doc.ContentControls.Count) + 1):
        cc = word_doc.ContentControls(i)
        t = str(cc.Tag or "")
        if not t.startswith(TAG_P):
            continue
        field_name = t[len(TAG_P):]
        font_info = {}
        if "|" in field_name:
            parts = field_name.split("|", 1)
            field_name = parts[0]
            font_info = _decode_font_info(parts[1]) if len(parts) > 1 else {}
        if not font_info:
            font_info = _cc_font_info(word_doc, cc)
        if field_name not in result:
            result[field_name] = font_info
    return result


def _union(rs: list[tuple[float, float, float, float]]) -> tuple[float, float, float, float]:
    return (
        min(x[0] for x in rs),
        min(x[1] for x in rs),
        max(x[2] for x in rs),
        max(x[3] for x in rs),
    )


def _hex_to_rgb(hex_color: str) -> tuple[float, float, float]:
    hex_color = hex_color.lstrip("#")
    if len(hex_color) == 6:
        r = int(hex_color[0:2], 16) / 255.0
        g = int(hex_color[2:4], 16) / 255.0
        b = int(hex_color[4:6], 16) / 255.0
        return (r, g, b)
    return (0.0, 0.0, 0.0)


def _add_widgets(
    pdf: Path,
    rects: dict[str, list[tuple[float, float, float, float]]],
    fonts: dict[str, dict],
    need: frozenset[str],
) -> None:
    import fitz

    d = fitz.open(pdf)
    try:
        pg, H = d[0], float(d[0].rect.height)
        for w in list(pg.widgets() or []):
            try:
                pg.delete_widget(w)
            except Exception:
                pass
        for name in sorted(need):
            rs = rects.get(name)
            if not rs:
                raise ValueError(f"Не найдены координаты для поля: {name}")
            fi = fonts.get(name, {})
            a, b, c, e = _union(rs)
            w = fitz.Widget()
            w.field_name = name
            w.field_type = fitz.PDF_WIDGET_TYPE_TEXT
            w.rect = fitz.Rect(float(a), H - float(e), float(c), H - float(b))
            font_name = fi.get("font_name", "Calibri")
            font_size = fi.get("font_size", 11.0)
            w.text_font = font_name
            w.text_fontsize = font_size
            font_flags = 0
            if fi.get("bold"):
                font_flags |= 2 ** 0
            if fi.get("italic"):
                font_flags |= 2 ** 1
            if font_flags:
                w.text_font_flags = font_flags
            if fi.get("color"):
                r, g, b_val = _hex_to_rgb(fi["color"])
                w.text_color = (r, g, b_val)
            pg.add_widget(w)
        tmp = pdf.with_suffix(f".{uuid.uuid4().hex}.tmp.pdf")
        d.save(tmp, garbage=4, deflate=True)
    finally:
        d.close()
    tmp.replace(pdf)


def _export_word(wapp, docx: Path, pdf: Path) -> tuple[dict[str, list[tuple[float, float, float, float]]], dict[str, dict]]:
    print(f"  Открытие: {docx.name}", flush=True)
    d = wapp.Documents.Open(
        str(docx.resolve()),
        ConfirmConversions=False,
        ReadOnly=True,
        AddToRecentFiles=False,
        Visible=False,
    )
    try:
        print(f"  Сбор координат полей...", flush=True)
        rects = _cc_rects(d)
        print(f"  Сбор информации о шрифтах...", flush=True)
        fonts = _cc_fonts(d)
        print(f"  Экспорт в PDF...", flush=True)
        d.ExportAsFixedFormat(
            str(pdf.resolve()), 17, False, 0, BitmapMissingFonts=True, DocStructureTags=True
        )
        print(f"  Готово: {pdf.name}", flush=True)
        return rects, fonts
    finally:
        d.Close(SaveChanges=False)


def _expected_map(jobs: list) -> dict[str, frozenset[str]]:
    m: dict[str, frozenset[str]] = {}
    for job in jobs:
        key = Path(job["output"]).stem.replace(".form", "") + ".pdf"
        m[key] = frozenset(f["field_name"] for f in job["fields"])
    return m


def _verify(docs: Path, exp: dict[str, frozenset[str]]) -> int:
    from pypdf import PdfReader

    for name, need in sorted(exp.items()):
        p = docs / name
        if not p.is_file():
            print("missing", p, file=sys.stderr)
            return 1
        r = PdfReader(str(p))
        flds = r.get_fields()
        if not flds:
            print("no fields", p, file=sys.stderr)
            return 1
        got = frozenset(flds.keys())
        for x in sorted(need - got):
            print("missing field", name, x, file=sys.stderr)
        for x in sorted(got - need):
            print("extra field", name, x, file=sys.stderr)
        for k in need & got:
            ft = (flds.get(k) or {}).get("/FT")
            if ft is not None and str(ft) != "/Tx":
                print("bad FT", name, k, ft, file=sys.stderr)
                return 1
        if got != need:
            return 1
    return 0


def main() -> int:
    sys.path.insert(0, str(ROOT))
    if sys.platform != "win32":
        print("Windows only", file=sys.stderr)
        return 2
    try:
        import pythoncom
        import win32com.client
    except ImportError:
        print("pip install pywin32", file=sys.stderr)
        return 2
    try:
        import fitz  # noqa: F401
    except ImportError:
        print("pip install pymupdf", file=sys.stderr)
        return 2

    from utilities.docx_to_pdf_template.config import JOBS

    docs = ROOT / "backend/src/main/resources/templates/documents"
    exp = _expected_map(JOBS)

    print("DOCX: замена плейсхолдеров на SDT (python-docx)", flush=True)
    for job in JOBS:
        inp, outp = ROOT / job["input"], ROOT / job["output"]
        if not inp.is_file():
            print("missing", inp, file=sys.stderr)
            return 2
        run_on_paths(str(inp), str(outp), job["fields"])

    print("PDF: экспорт Word.Application → ExportAsFixedFormat (PDF)", flush=True)
    pythoncom.CoInitialize()
    wapp = None
    cache_rects: dict[str, dict[str, list[tuple[float, float, float, float]]]] = {}
    cache_fonts: dict[str, dict[str, dict]] = {}
    try:
        wapp = win32com.client.Dispatch("Word.Application")
        wapp.Visible, wapp.DisplayAlerts = False, 0
        for job in JOBS:
            stem = Path(job["output"]).stem.replace(".form", "")
            fdoc, fpdf = docs / f"{stem}.form.docx", docs / f"{stem}.pdf"
            if not fdoc.is_file():
                return 2
            rects, fonts = _export_word(wapp, fdoc, fpdf)
            cache_rects[f"{stem}.pdf"] = rects
            cache_fonts[f"{stem}.pdf"] = fonts
        for job in JOBS:
            stem = Path(job["output"]).stem.replace(".form", "")
            key, pdfp = f"{stem}.pdf", docs / f"{stem}.pdf"
            need = exp[key]
            _add_widgets(pdfp, cache_rects[key], cache_fonts[key], need)
        print("проверка: имена полей AcroForm (pypdf)", flush=True)
        if _verify(docs, exp):
            return 1
    finally:
        if wapp is not None:
            try:
                wapp.Quit()
            except Exception:
                pass
        pythoncom.CoUninitialize()

    return 0


if __name__ == "__main__":
    sys.exit(main())
