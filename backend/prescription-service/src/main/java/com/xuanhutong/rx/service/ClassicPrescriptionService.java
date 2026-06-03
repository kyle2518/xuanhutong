package com.xuanhutong.rx.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.ClassicPrescriptionRequest;
import com.xuanhutong.rx.entity.ClassicPrescription;

public interface ClassicPrescriptionService {
    Page<ClassicPrescription> listPrescriptions(String keyword, String source, int page, int size);
    ClassicPrescription getPrescription(Long id);
    ClassicPrescription createPrescription(ClassicPrescriptionRequest request);
    ClassicPrescription updatePrescription(Long id, ClassicPrescriptionRequest request);
    void deletePrescription(Long id);
}
