"""ai_diagnoses 持久化 + 转处方写回（raw SQL + 手动 JSON 序列化）。"""
import json
from datetime import datetime

from sqlalchemy import text

from app.db.engine import SessionLocal, engine

_JSON_FIELDS = ("draft_content", "evidence", "agent_trace", "compatibility_warnings")


def _dump(v) -> str:
    return json.dumps(v, ensure_ascii=False, default=str)


def _loads(v):
    if v is None:
        return None
    if isinstance(v, (dict, list)):
        return v
    try:
        return json.loads(v)
    except (TypeError, ValueError):
        return v


def _snake_to_camel(s: str) -> str:
    parts = s.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def _row_to_dict(row) -> dict:
    d = dict(row)
    for f in _JSON_FIELDS:
        if f in d:
            d[f] = _loads(d[f])
    # snake_case → camelCase（与 Java 服务 MyBatis-Plus 的 map-underscore-to-camel-case 保持一致）
    return {_snake_to_camel(k): v for k, v in d.items()}


def insert_diagnosis(
    user_id: int,
    patient_id: int,
    chief_complaint: str | None,
    draft: dict,
    evidence: list,
    trace: list,
    warnings: list,
) -> int:
    with SessionLocal() as s:
        result = s.execute(
            text(
                "INSERT INTO ai_diagnoses "
                "(user_id, patient_id, status, chief_complaint, draft_content, evidence, agent_trace, compatibility_warnings) "
                "VALUES (:uid, :pid, 'DRAFT', :cc, :draft, :ev, :trace, :warn)"
            ),
            {
                "uid": user_id,
                "pid": patient_id,
                "cc": chief_complaint,
                "draft": _dump(draft),
                "ev": _dump(evidence),
                "trace": _dump(trace),
                "warn": _dump(warnings),
            },
        )
        s.commit()
        return result.lastrowid


def get_diagnosis(diag_id: int, user_id: int) -> dict | None:
    with SessionLocal() as s:
        row = s.execute(
            text(
                "SELECT * FROM ai_diagnoses WHERE id=:id AND user_id=:uid AND is_deleted=0"
            ),
            {"id": diag_id, "uid": user_id},
        ).mappings().first()
        return _row_to_dict(row) if row else None


def list_diagnoses(user_id: int, patient_id: int | None, page: int, size: int) -> tuple[list[dict], int]:
    with SessionLocal() as s:
        where = "user_id=:uid AND is_deleted=0"
        params: dict = {"uid": user_id}
        if patient_id:
            where += " AND patient_id=:pid"
            params["pid"] = patient_id
        total = s.execute(
            text(f"SELECT COUNT(*) FROM ai_diagnoses WHERE {where}"), params
        ).scalar()
        rows = s.execute(
            text(
                f"SELECT * FROM ai_diagnoses WHERE {where} "
                "ORDER BY id DESC LIMIT :lim OFFSET :off"
            ),
            {**params, "lim": size, "off": (page - 1) * size},
        ).mappings().all()
        return [_row_to_dict(r) for r in rows], int(total)


def update_draft(diag_id: int, user_id: int, draft: dict) -> bool:
    with SessionLocal() as s:
        result = s.execute(
            text(
                "UPDATE ai_diagnoses SET draft_content=:draft "
                "WHERE id=:id AND user_id=:uid AND is_deleted=0"
            ),
            {"draft": _dump(draft), "id": diag_id, "uid": user_id},
        )
        s.commit()
        return result.rowcount > 0


def sign_diagnosis(diag_id: int, user_id: int, signature_text: str) -> dict | None:
    with SessionLocal() as s:
        result = s.execute(
            text(
                "UPDATE ai_diagnoses SET status='SIGNED', signature_text=:sig, signed_at=:now "
                "WHERE id=:id AND user_id=:uid AND is_deleted=0 AND status='DRAFT'"
            ),
            {"sig": signature_text, "now": datetime.now(), "id": diag_id, "uid": user_id},
        )
        s.commit()
        if result.rowcount == 0:
            return None
        return get_diagnosis(diag_id, user_id)


def set_status(diag_id: int, user_id: int, status: str) -> bool:
    with SessionLocal() as s:
        result = s.execute(
            text(
                "UPDATE ai_diagnoses SET status=:st "
                "WHERE id=:id AND user_id=:uid AND is_deleted=0"
            ),
            {"st": status, "id": diag_id, "uid": user_id},
        )
        s.commit()
        return result.rowcount > 0


def set_converted(diag_id: int, prescription_id: int) -> bool:
    with SessionLocal() as s:
        result = s.execute(
            text(
                "UPDATE ai_diagnoses SET converted_prescription_id=:rid "
                "WHERE id=:id AND is_deleted=0"
            ),
            {"rid": prescription_id, "id": diag_id},
        )
        s.commit()
        return result.rowcount > 0


def convert_to_prescription(user_id: int, patient_id: int, draft: dict, total_doses: int) -> int:
    """镜像 Java PrescriptionServiceImpl.createPrescription 的插入语义。"""
    diagnosis = "；".join(
        x for x in (draft.get("syndrome_pattern"), draft.get("treatment_principle")) if x
    )
    notes = draft.get("usage_instructions") or ""
    with engine.begin() as conn:
        result = conn.execute(
            text(
                "INSERT INTO prescriptions (patient_id, user_id, diagnosis, notes, total_doses, is_signed, is_deleted) "
                "VALUES (:pid, :uid, :diag, :notes, :doses, 0, 0)"
            ),
            {"pid": patient_id, "uid": user_id, "diag": diagnosis, "notes": notes, "doses": total_doses},
        )
        prescription_id = result.lastrowid
        for i, herb in enumerate(draft.get("herbs", [])):
            name = herb.get("herb_name", "")
            herb_id = _resolve_herb_id(conn, name)
            conn.execute(
                text(
                    "INSERT INTO prescription_items "
                    "(prescription_id, herb_id, herb_name, dosage_grams, notes, sort_order, is_deleted) "
                    "VALUES (:rid, :hid, :name, :dosage, :notes, :sort, 0)"
                ),
                {
                    "rid": prescription_id,
                    "hid": herb_id,
                    "name": name,
                    "dosage": herb.get("dosage_grams", 0),
                    "notes": herb.get("notes"),
                    "sort": i,
                },
            )
        return prescription_id


def _resolve_herb_id(conn, name: str) -> int:
    row = conn.execute(
        text("SELECT id FROM herbs WHERE (chinese_name=:n OR pinyin_name=:n) AND is_deleted=0 LIMIT 1"),
        {"n": name},
    ).first()
    return row[0] if row else 0
