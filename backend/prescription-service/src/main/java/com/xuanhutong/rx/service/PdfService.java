package com.xuanhutong.rx.service;
import com.xuanhutong.rx.entity.*;
import com.xuanhutong.common.entity.User;
import java.util.List;

public interface PdfService {
    byte[] generatePrescriptionPdf(Prescription rx, List<PrescriptionItem> items, User doctor, String patientName, String sig);
    byte[] generatePrescriptionPreview(Prescription rx, List<PrescriptionItem> items, User doctor, String patientName);
}
