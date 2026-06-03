package com.xuanhutong.medical.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.medical.dto.request.PatientCreateRequest;
import com.xuanhutong.medical.dto.request.PatientUpdateRequest;
import com.xuanhutong.medical.entity.Patient;

public interface PatientService {
    Page<Patient> listPatients(Long userId, int page, int size, String keyword);
    Patient getPatient(Long id, Long userId);
    Patient createPatient(Long userId, PatientCreateRequest request);
    Patient updatePatient(Long id, Long userId, PatientUpdateRequest request);
    void deletePatient(Long id, Long userId);
}
