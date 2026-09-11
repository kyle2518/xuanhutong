"""诊断业务编排：start / detail / update / sign / reject / convert。"""
from datetime import datetime

from app.agents.agent import run_diagnosis
from app.core.errors import AppError, NOT_FOUND
from app.db import diagnosis_repo
from app.db.read_sql import get_patient, get_user
from app.services import pdf_service


def start_diagnosis(user_id: int, patient_id: int, chief_complaint: str | None) -> dict:
    patient = get_patient(patient_id, user_id)
    if not patient:
        raise AppError(NOT_FOUND, "病人不存在或无权访问")

    # 主诉为空时回退用病人档案里的主诉，保证列表页有可读内容
    effective_complaint = chief_complaint or patient.get("chief_complaint")

    result = run_diagnosis(user_id, patient_id, effective_complaint)
    diag_id = diagnosis_repo.insert_diagnosis(
        user_id,
        patient_id,
        effective_complaint,
        result["draft"],
        result["evidence"],
        result["agent_trace"],
        result["compatibility_warnings"],
    )
    return get_diagnosis(user_id, diag_id)


def get_diagnosis(user_id: int, diag_id: int) -> dict:
    d = diagnosis_repo.get_diagnosis(diag_id, user_id)
    if not d:
        raise AppError(NOT_FOUND, "诊断记录不存在")
    return d


def list_diagnoses(user_id: int, patient_id: int | None, page: int, size: int) -> dict:
    records, total = diagnosis_repo.list_diagnoses(user_id, patient_id, page, size)
    return {"records": records, "total": total, "page": page, "size": size}


def update_diagnosis(user_id: int, diag_id: int, draft: dict) -> dict:
    if not diagnosis_repo.update_draft(diag_id, user_id, draft):
        raise AppError(NOT_FOUND, "诊断记录不存在")
    return get_diagnosis(user_id, diag_id)


def sign_diagnosis(user_id: int, diag_id: int, signature_text: str) -> bytes:
    d = get_diagnosis(user_id, diag_id)
    if d["status"] != "DRAFT":
        raise AppError(6003, "仅草稿状态可签字")
    patient = get_patient(d["patient_id"], user_id)
    doctor = get_user(user_id)
    signed_at_str = datetime.now().strftime("%Y年%m月%d日")

    pdf = pdf_service.generate_treatment_plan_pdf(
        d["draft_content"],
        patient.get("name") if patient else "—",
        doctor.get("name") if doctor else "—",
        doctor.get("clinic_name") if doctor else None,
        signature_text,
        signed_at_str,
    )
    diagnosis_repo.sign_diagnosis(diag_id, user_id, signature_text)
    return pdf


def reject_diagnosis(user_id: int, diag_id: int) -> dict:
    if not diagnosis_repo.set_status(diag_id, user_id, "REJECTED"):
        raise AppError(NOT_FOUND, "诊断记录不存在")
    return get_diagnosis(user_id, diag_id)


def convert_diagnosis(user_id: int, diag_id: int, total_doses: int = 1) -> dict:
    d = get_diagnosis(user_id, diag_id)
    if d["status"] != "SIGNED":
        raise AppError(6004, "仅已签字的诊断可转处方")
    if d.get("converted_prescription_id"):
        raise AppError(6005, "该诊断已转过处方")
    pid = diagnosis_repo.convert_to_prescription(
        user_id, d["patient_id"], d["draft_content"], total_doses
    )
    diagnosis_repo.set_converted(diag_id, pid)
    return {"prescriptionId": pid}
