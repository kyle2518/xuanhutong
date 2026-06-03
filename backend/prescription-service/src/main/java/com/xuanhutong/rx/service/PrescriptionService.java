package com.xuanhutong.rx.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.PrescriptionCreateRequest;
import com.xuanhutong.rx.entity.Prescription;
import java.util.Map;

public interface PrescriptionService {
    Page<Prescription> listPrescriptions(Long patientId, Long userId, int page, int size);
    Map<String, Object> getPrescriptionDetail(Long id, Long userId);
    Prescription createPrescription(Long userId, PrescriptionCreateRequest request);
    Prescription updatePrescription(Long id, Long userId, PrescriptionCreateRequest request);
    void deletePrescription(Long id, Long userId);
    byte[] previewPdf(Long id, Long userId, PrescriptionCreateRequest request);
    byte[] signPrescription(Long id, Long userId, String signatureData);
}
