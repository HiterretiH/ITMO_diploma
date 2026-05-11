from __future__ import annotations

from copy import deepcopy
from typing import Any

from docx.oxml import OxmlElement
from docx.oxml.ns import qn


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
    field_name: str,
    default_text: str | None,
    r_pr: Any | None,
    sdt_id: int,
    in_table: bool,
) -> OxmlElement:
    sdt = OxmlElement("w:sdt")
    sdt_pr = OxmlElement("w:sdtPr")
    id_el = OxmlElement("w:id")
    id_el.set(qn("w:val"), str(sdt_id))
    sdt_pr.append(id_el)
    tag = OxmlElement("w:tag")
    tag.set(qn("w:val"), f"LO_FORM_FIELD:{field_name}")
    sdt_pr.append(tag)
    alias = OxmlElement("w:alias")
    alias.set(qn("w:val"), field_name)
    sdt_pr.append(alias)
    if in_table:
        sdt_pr.append(OxmlElement("w:text"))
        ml = OxmlElement("w:multiLine")
        ml.set(qn("w:val"), "1")
        sdt_pr.append(ml)
    else:
        sdt_pr.append(OxmlElement("w:richText"))
    sdt.append(sdt_pr)
    content = OxmlElement("w:sdtContent")
    inner = "" if default_text is None else default_text
    content.append(_make_run(inner, r_pr))
    sdt.append(content)
    return sdt


def max_sdt_id(root) -> int:
    m = 0
    for sdt in root.iter(qn("w:sdt")):
        pr = sdt.find(qn("w:sdtPr"))
        if pr is None:
            continue
        id_el = pr.find(qn("w:id"))
        if id_el is None:
            continue
        val = id_el.get(qn("w:val"))
        if val is None:
            continue
        try:
            m = max(m, int(val))
        except ValueError:
            pass
    return m
