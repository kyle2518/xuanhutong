package com.xuanhutong.rx.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.PrescriptionCreateRequest;
import com.xuanhutong.rx.entity.*;
import com.xuanhutong.common.entity.User;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.rx.repository.*;
import com.xuanhutong.rx.service.PdfService;
import com.xuanhutong.rx.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service @RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {
    private final PrescriptionRepository rxRepo;
    private final PrescriptionItemRepository itemRepo;
    private final UserRepository userRepo;
    private final PdfService pdfService;
    private final HerbInventoryRepository inventoryRepo;
    private final PatientRepository patientRepo;

    public Page<Prescription> listPrescriptions(Long patientId, Long userId, int page, int size) {
        var w = new LambdaQueryWrapper<Prescription>().eq(Prescription::getUserId, userId);
        if (patientId != null) w.eq(Prescription::getPatientId, patientId);
        w.orderByDesc(Prescription::getCreatedAt); return rxRepo.selectPage(new Page<>(page, size), w);
    }

    public Map<String, Object> getPrescriptionDetail(Long id, Long userId) {
        var rx = rxRepo.selectOne(new LambdaQueryWrapper<Prescription>().eq(Prescription::getId, id).eq(Prescription::getUserId, userId));
        if (rx == null) throw new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND);
        var items = itemRepo.selectList(new LambdaQueryWrapper<PrescriptionItem>().eq(PrescriptionItem::getPrescriptionId, id).orderByAsc(PrescriptionItem::getSortOrder));
        String patientName = getPatientName(rx.getPatientId());
        Map<String, Object> m = new HashMap<>(); m.put("prescription", rx); m.put("items", items); m.put("patientName", patientName); return m;
    }

    @Transactional
    public Prescription createPrescription(Long userId, PrescriptionCreateRequest r) {
        var rx = new Prescription(); rx.setPatientId(r.getPatientId()); rx.setMedicalRecordId(r.getMedicalRecordId());
        rx.setUserId(userId); rx.setDiagnosis(r.getDiagnosis()); rx.setNotes(r.getNotes());
        rx.setTotalDoses(r.getTotalDoses() != null ? r.getTotalDoses() : 1); rx.setIsSigned(0);
        rxRepo.insert(rx);
        saveItems(rx.getId(), r.getItems());
        deductInventory(userId, r.getItems(), rx.getTotalDoses());
        return rx;
    }

    @Transactional
    public Prescription updatePrescription(Long id, Long userId, PrescriptionCreateRequest r) {
        var rx = rxRepo.selectOne(new LambdaQueryWrapper<Prescription>().eq(Prescription::getId, id).eq(Prescription::getUserId, userId));
        if (rx == null) throw new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND);
        rx.setPatientId(r.getPatientId()); rx.setMedicalRecordId(r.getMedicalRecordId());
        rx.setDiagnosis(r.getDiagnosis()); rx.setNotes(r.getNotes());
        rx.setTotalDoses(r.getTotalDoses() != null ? r.getTotalDoses() : 1); rxRepo.updateById(rx);
        itemRepo.delete(new LambdaQueryWrapper<PrescriptionItem>().eq(PrescriptionItem::getPrescriptionId, id));
        saveItems(id, r.getItems());
        return rx;
    }

    public void deletePrescription(Long id, Long userId) {
        var rx = rxRepo.selectOne(new LambdaQueryWrapper<Prescription>().eq(Prescription::getId, id).eq(Prescription::getUserId, userId));
        if (rx == null) throw new BusinessException(ErrorCode.PRESCRIPTION_NOT_FOUND); rxRepo.deleteById(id);
    }

    public byte[] previewPdf(Long id, Long userId, PrescriptionCreateRequest r) {
        Prescription rx; List<PrescriptionItem> items; String patientName; String sig = null;
        var doctor = userRepo.selectById(userId);
        if (id != null) {
            var detail = getPrescriptionDetail(id, userId);
            rx = (Prescription) detail.get("prescription");
            items = (List<PrescriptionItem>) detail.get("items");
            patientName = (String) detail.getOrDefault("patientName", "病人");
            // For signed prescriptions, use the saved signature text (fallback to doctor name)
            if (rx.getIsSigned() == 1) {
                sig = rx.getSignatureText() != null ? rx.getSignatureText() : doctor.getName();
            }
        } else if (r != null) {
            rx = new Prescription(); rx.setDiagnosis(r.getDiagnosis()); rx.setNotes(r.getNotes());
            rx.setTotalDoses(r.getTotalDoses() != null ? r.getTotalDoses() : 1);
            items = r.getItems().stream().map(dto -> {
                PrescriptionItem item = new PrescriptionItem(); item.setHerbId(dto.getHerbId());
                item.setHerbName(dto.getHerbName()); item.setDosageGrams(dto.getDosageGrams());
                item.setNotes(dto.getNotes()); return item;
            }).toList();
            patientName = getPatientName(r.getPatientId());
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return pdfService.generatePrescriptionPdf(rx, items, doctor, patientName, sig);
    }

    public byte[] signPrescription(Long id, Long userId, String sig) {
        var detail = getPrescriptionDetail(id, userId);
        var rx = (Prescription) detail.get("prescription");
        // Block re-signing
        if (rx.getIsSigned() == 1) throw new BusinessException(400, "该药方已签署，不可重复签署");
        var items = (List<PrescriptionItem>) detail.get("items");
        var doctor = userRepo.selectById(userId);
        String patientName = (String) detail.getOrDefault("patientName", "病人");
        byte[] pdf = pdfService.generatePrescriptionPdf(rx, items, doctor, patientName, sig);
        rx.setPdfUrl("prescriptions/" + id + "/" + UUID.randomUUID() + ".pdf");
        rx.setIsSigned(1); rx.setSignedAt(java.time.LocalDateTime.now());
        rx.setSignatureText(sig);  // Save signature text
        rxRepo.updateById(rx); return pdf;
    }

    private String getPatientName(Long patientId) {
        if (patientId == null) return "未知";
        Patient p = patientRepo.selectById(patientId);
        return p != null ? p.getName() : "未知";
    }

    private void saveItems(Long rxId, List<PrescriptionCreateRequest.PrescriptionItemDto> dtos) {
        int ord = 0;
        for (var dto : dtos) {
            var item = new PrescriptionItem(); item.setPrescriptionId(rxId);
            item.setHerbId(dto.getHerbId() != null ? dto.getHerbId() : 0L); item.setHerbName(dto.getHerbName());
            item.setDosageGrams(dto.getDosageGrams() != null ? dto.getDosageGrams() : BigDecimal.ZERO);
            item.setNotes(dto.getNotes()); item.setSortOrder(ord++); itemRepo.insert(item);
        }
    }

    private void deductInventory(Long userId, List<PrescriptionCreateRequest.PrescriptionItemDto> items, int totalDoses) {
        for (var item : items) {
            if (item.getHerbId() == null || item.getDosageGrams() == null) continue;
            var inv = inventoryRepo.selectOne(new LambdaQueryWrapper<HerbInventory>()
                    .eq(HerbInventory::getUserId, userId).eq(HerbInventory::getHerbId, item.getHerbId()));
            if (inv != null) {
                BigDecimal toDeduct = item.getDosageGrams().multiply(BigDecimal.valueOf(totalDoses));
                BigDecimal newStock = inv.getStockGrams().subtract(toDeduct);
                inv.setStockGrams(newStock.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newStock);
                inventoryRepo.updateById(inv);
                log.info("Deducted {}g of herb {} for user {}, new stock: {}g", toDeduct, item.getHerbName(), userId, inv.getStockGrams());
            }
        }
    }
}
