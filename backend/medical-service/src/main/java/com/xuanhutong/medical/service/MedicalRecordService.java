package com.xuanhutong.medical.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.medical.dto.request.MedicalRecordRequest;
import com.xuanhutong.medical.entity.MedicalRecord;

public interface MedicalRecordService {
    Page<MedicalRecord> listRecords(Long patientId, Long userId, int page, int size);
    MedicalRecord getRecord(Long id, Long userId);
    MedicalRecord createRecord(Long patientId, Long userId, MedicalRecordRequest request);
    MedicalRecord updateRecord(Long id, Long userId, MedicalRecordRequest request);
    void deleteRecord(Long id, Long userId);
    String uploadReport(Long id, Long userId, String filename, byte[] bytes);
    byte[] exportPatientPdf(Long patientId, Long userId);
}
