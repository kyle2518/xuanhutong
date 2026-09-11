"""AI 辅助诊断接口。"""
from fastapi import APIRouter, Depends, Query, Response
from pydantic import BaseModel

from app.api.deps import get_current_user_id
from app.core.response import ok
from app.services import diagnosis_service

router = APIRouter()


class StartDiagnosisRequest(BaseModel):
    patientId: int
    chiefComplaint: str | None = None
    includeRecords: bool = True


class UpdateDiagnosisRequest(BaseModel):
    draft: dict


class SignRequest(BaseModel):
    signatureText: str


class ConvertRequest(BaseModel):
    totalDoses: int = 1


@router.post("/diagnoses")
def start_diagnosis(req: StartDiagnosisRequest, user_id: int = Depends(get_current_user_id)):
    """发起一次 AI 辅助诊断（同步执行 Agent，落库并返回完整对象）。"""
    return ok(diagnosis_service.start_diagnosis(user_id, req.patientId, req.chiefComplaint))


@router.get("/diagnoses")
def list_diagnoses(
    patientId: int | None = None,
    p: int = Query(1, ge=1),
    s: int = Query(10, ge=1, le=100),
    user_id: int = Depends(get_current_user_id),
):
    return ok(diagnosis_service.list_diagnoses(user_id, patientId, p, s))


@router.get("/diagnoses/{diag_id}")
def get_diagnosis(diag_id: int, user_id: int = Depends(get_current_user_id)):
    return ok(diagnosis_service.get_diagnosis(user_id, diag_id))


@router.put("/diagnoses/{diag_id}")
def update_diagnosis(
    diag_id: int,
    req: UpdateDiagnosisRequest,
    user_id: int = Depends(get_current_user_id),
):
    return ok(diagnosis_service.update_diagnosis(user_id, diag_id, req.draft))


@router.post("/diagnoses/{diag_id}/sign")
def sign_diagnosis(
    diag_id: int,
    req: SignRequest,
    user_id: int = Depends(get_current_user_id),
):
    """签字并返回治疗方案 PDF（裸 bytes，与 Java 侧 sign 行为一致）。"""
    pdf = diagnosis_service.sign_diagnosis(user_id, diag_id, req.signatureText)
    return Response(content=pdf, media_type="application/pdf")


@router.post("/diagnoses/{diag_id}/convert")
def convert_diagnosis(
    diag_id: int,
    req: ConvertRequest,
    user_id: int = Depends(get_current_user_id),
):
    """签字后的诊断一键转为正式处方。"""
    return ok(diagnosis_service.convert_diagnosis(user_id, diag_id, req.totalDoses))


@router.post("/diagnoses/{diag_id}/reject")
def reject_diagnosis(diag_id: int, user_id: int = Depends(get_current_user_id)):
    return ok(diagnosis_service.reject_diagnosis(user_id, diag_id))
