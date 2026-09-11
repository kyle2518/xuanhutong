"""只读查询：患者 / 病历 / 药材 / 经典方（均按 user_id 做所有权过滤）。"""
from sqlalchemy import text

from app.db.engine import SessionLocal


def get_patient(patient_id: int, user_id: int) -> dict | None:
    with SessionLocal() as s:
        row = s.execute(
            text(
                "SELECT id, name, phone, gender, age, chief_complaint "
                "FROM patients WHERE id=:pid AND user_id=:uid AND is_deleted=0"
            ),
            {"pid": patient_id, "uid": user_id},
        ).mappings().first()
        return dict(row) if row else None


def get_medical_records(patient_id: int, user_id: int, limit: int = 10) -> list[dict]:
    with SessionLocal() as s:
        rows = s.execute(
            text(
                "SELECT visit_date, diagnosis, symptoms, treatment_method, notes "
                "FROM medical_records WHERE patient_id=:pid AND user_id=:uid AND is_deleted=0 "
                "ORDER BY visit_date DESC LIMIT :lim"
            ),
            {"pid": patient_id, "uid": user_id, "lim": limit},
        ).mappings().all()
        return [dict(r) for r in rows]


def get_herb(name: str) -> dict | None:
    with SessionLocal() as s:
        row = s.execute(
            text(
                "SELECT chinese_name, pinyin_name, latin_name, category, properties, "
                "meridian_tropism, efficacy, indications, dosage_range, contraindications "
                "FROM herbs WHERE (chinese_name=:n OR pinyin_name=:n) AND is_deleted=0 LIMIT 1"
            ),
            {"n": name},
        ).mappings().first()
        return dict(row) if row else None


def get_classic_prescription(name: str) -> dict | None:
    with SessionLocal() as s:
        row = s.execute(
            text(
                "SELECT name, source, category, composition, efficacy, indications, "
                "usage_method, source_text, notes "
                "FROM classic_prescriptions WHERE name=:n AND is_deleted=0 LIMIT 1"
            ),
            {"n": name},
        ).mappings().first()
        return dict(row) if row else None


def get_herb_id_by_name(name: str) -> int | None:
    """转处方时按中文名/拼音名解析 herb_id。"""
    with SessionLocal() as s:
        row = s.execute(
            text(
                "SELECT id FROM herbs WHERE (chinese_name=:n OR pinyin_name=:n) AND is_deleted=0 LIMIT 1"
            ),
            {"n": name},
        ).first()
        return row[0] if row else None


def get_user(user_id: int) -> dict | None:
    """查询医生信息（姓名、诊所名），供 PDF 签名使用。"""
    with SessionLocal() as s:
        row = s.execute(
            text("SELECT id, name, clinic_name FROM users WHERE id=:uid AND is_deleted=0"),
            {"uid": user_id},
        ).mappings().first()
        return dict(row) if row else None
