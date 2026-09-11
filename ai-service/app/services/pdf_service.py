"""治疗方案 PDF 生成（reportlab + 内置 STSong-Light CID 字体，零字体文件依赖）。"""
import io

from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

FONT = "STSong-Light"
pdfmetrics.registerFont(UnicodeCIDFont(FONT))

PAGE_W, PAGE_H = A4


def _wrap(text: str, max_chars: int) -> list[str]:
    return [text[i : i + max_chars] for i in range(0, len(text), max_chars)]


def generate_treatment_plan_pdf(
    draft: dict,
    patient_name: str,
    doctor_name: str,
    clinic_name: str,
    signature_text: str,
    signed_at_str: str,
) -> bytes:
    buf = io.BytesIO()
    c = canvas.Canvas(buf, pagesize=A4)
    y = PAGE_H - 60

    def line(text: str, size: int = 11, gap: int = 18):
        nonlocal y
        c.setFont(FONT, size)
        c.drawString(60, y, text)
        y -= gap

    def center(text: str, size: int = 16):
        nonlocal y
        c.setFont(FONT, size)
        c.drawCentredString(PAGE_W / 2, y, text)
        y -= size + 12

    center(clinic_name or "悬壶通中医诊所", 14)
    center("AI 辅助诊断治疗方案", 18)
    y -= 10

    line(f"病人：{patient_name or '—'}")
    line(f"辨证：{draft.get('syndrome_pattern') or '—'}")
    line(f"治法：{draft.get('treatment_principle') or '—'}")
    line(f"推荐方剂：{draft.get('recommended_formula') or '—'}")
    y -= 6

    line("【方剂组成】", 12)
    for i, herb in enumerate(draft.get("herbs", []), 1):
        dosage = herb.get("dosage_grams")
        notes = f"（{herb.get('notes')}）" if herb.get("notes") else ""
        line(f"{i}. {herb.get('herb_name')}  {dosage}g {notes}")
    y -= 6

    line("【用药说明】", 12)
    for seg in _wrap(draft.get("usage_instructions") or "—", 40):
        line(seg, 10, 14)
    y -= 6

    evidence = draft.get("classic_evidence") or []
    if evidence:
        line("【古籍依据】", 12)
        for ev in evidence:
            label = f"《{ev.get('book')}》{ev.get('chapter') or ''}"
            line(label, 10, 14)
            for seg in _wrap(ev.get("source_text") or "", 40):
                line(seg, 10, 14)
    y -= 6

    cautions = draft.get("cautions") or []
    if cautions:
        line("【注意事项】", 12)
        for ca in cautions:
            for seg in _wrap(ca, 40):
                line(seg, 10, 14)

    y = max(y, 130)
    line(f"医师签名：{signature_text or '______'}")
    line(f"签署日期：{signed_at_str or '____年__月__日'}")

    c.showPage()
    c.save()
    return buf.getvalue()
