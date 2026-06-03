package com.xuanhutong.medical.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.xuanhutong.medical.dto.request.MedicalRecordRequest;
import com.xuanhutong.medical.entity.Patient;
import com.xuanhutong.medical.entity.MedicalRecord;
import com.xuanhutong.medical.entity.Prescription;
import com.xuanhutong.medical.entity.PrescriptionItem;
import com.xuanhutong.common.entity.User;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.medical.repository.*;
import com.xuanhutong.medical.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {
    private final MedicalRecordRepository recordRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    public Page<MedicalRecord> listRecords(Long patientId, Long userId, int page, int size) {
        var w = new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getPatientId, patientId).eq(MedicalRecord::getUserId, userId).orderByDesc(MedicalRecord::getVisitDate);
        return recordRepository.selectPage(new Page<>(page, size), w);
    }
    public MedicalRecord getRecord(Long id, Long userId) {
        var r = recordRepository.selectOne(new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getId, id).eq(MedicalRecord::getUserId, userId));
        if (r == null) throw new BusinessException(ErrorCode.NOT_FOUND); return r;
    }
    public MedicalRecord createRecord(Long patientId, Long userId, MedicalRecordRequest req) {
        Patient p = patientRepository.selectById(patientId);
        if (p == null) throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        MedicalRecord r = new MedicalRecord(); r.setPatientId(patientId); r.setUserId(userId);
        r.setVisitDate(req.getVisitDate()); r.setDiagnosis(req.getDiagnosis());
        r.setSymptoms(req.getSymptoms()); r.setTreatmentMethod(req.getTreatmentMethod());
        r.setNotes(req.getNotes()); recordRepository.insert(r); return r;
    }
    public MedicalRecord updateRecord(Long id, Long userId, MedicalRecordRequest req) {
        MedicalRecord r = getRecord(id, userId);
        r.setVisitDate(req.getVisitDate()); r.setDiagnosis(req.getDiagnosis()); r.setSymptoms(req.getSymptoms());
        r.setTreatmentMethod(req.getTreatmentMethod()); r.setNotes(req.getNotes());
        recordRepository.updateById(r); return r;
    }
    public void deleteRecord(Long id, Long userId) { getRecord(id, userId); recordRepository.deleteById(id); }
    public String uploadReport(Long id, Long userId, String filename, byte[] bytes) {
        MedicalRecord r = getRecord(id, userId);
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : ".pdf";
        String name = "reports/" + r.getPatientId() + "/" + UUID.randomUUID() + ext;
        r.setReportFileUrl(name); recordRepository.updateById(r); return name;
    }
    public byte[] exportPatientPdf(Long patientId, Long userId) {
        Patient pat = patientRepository.selectById(patientId);
        if (pat == null) throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        var records = recordRepository.selectList(new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getPatientId, patientId).eq(MedicalRecord::getUserId, userId).orderByDesc(MedicalRecord::getVisitDate));
        var rxs = prescriptionRepository.selectList(new LambdaQueryWrapper<Prescription>().eq(Prescription::getPatientId, patientId).eq(Prescription::getUserId, userId).orderByDesc(Prescription::getCreatedAt));
        User doctor = userRepository.selectById(userId);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf);
            PdfFont font;
            try { font = PdfFontFactory.createFont("fonts/NotoSansCJKsc-Regular.ttf", PdfEncodings.IDENTITY_H); }
            catch (Exception e) { font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED); }
            doc.add(new Paragraph("病人病历档案").setFont(font).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setBold());
            doc.add(new Paragraph(doctor.getClinicName() != null ? doctor.getClinicName() : "").setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("姓名：" + pat.getName() + "  性别：" + (pat.getGender() != null && pat.getGender() == 0 ? "男" : "女") + "  年龄：" + (pat.getAge() != null ? pat.getAge() : "")).setFont(font).setFontSize(9));
            doc.add(new Paragraph("电话：" + (pat.getPhone() != null ? pat.getPhone() : "") + "  住址：" + (pat.getAddress() != null ? pat.getAddress() : "")).setFont(font).setFontSize(9));
            int n = 1;
            for (var r : records) {
                doc.add(new Paragraph("第" + n++ + "次就诊  " + r.getVisitDate().format(FMT)).setFont(font).setFontSize(9).setBold());
                if (r.getDiagnosis() != null) doc.add(new Paragraph("诊断：" + r.getDiagnosis()).setFont(font).setFontSize(9));
                if (r.getSymptoms() != null) doc.add(new Paragraph("症状：" + r.getSymptoms()).setFont(font).setFontSize(9));
            }
            doc.add(new Paragraph("打印日期：" + LocalDateTime.now().format(FMT)).setFont(font).setFontSize(8).setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("医师：" + doctor.getName()).setFont(font).setFontSize(8).setTextAlignment(TextAlignment.RIGHT));
            doc.close(); return baos.toByteArray();
        } catch (Exception e) { throw new BusinessException(ErrorCode.PDF_GENERATION_FAILED); }
    }
}
