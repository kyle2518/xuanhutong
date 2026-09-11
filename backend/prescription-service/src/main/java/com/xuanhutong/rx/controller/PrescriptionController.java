package com.xuanhutong.rx.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.PrescriptionCreateRequest;
import com.xuanhutong.rx.dto.request.PrescriptionSignRequest;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.rx.entity.Prescription;
import com.xuanhutong.rx.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/prescriptions") @RequiredArgsConstructor
public class PrescriptionController {
    private final PrescriptionService service;

    @GetMapping
    public ApiResponse<Page<Prescription>> list(Authentication a, @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int p, @RequestParam(defaultValue = "10") int s) {
        return ApiResponse.success(service.listPrescriptions(patientId, keyword, startDate, endDate, (Long)a.getPrincipal(), p, s));
    }
    @PostMapping
    public ApiResponse<Prescription> create(Authentication a, @Valid @RequestBody PrescriptionCreateRequest r) {
        return ApiResponse.success(service.createPrescription((Long)a.getPrincipal(), r));
    }
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(Authentication a, @PathVariable Long id) {
        return ApiResponse.success(service.getPrescriptionDetail(id, (Long)a.getPrincipal()));
    }
    @PutMapping("/{id}")
    public ApiResponse<Prescription> update(Authentication a, @PathVariable Long id, @Valid @RequestBody PrescriptionCreateRequest r) {
        return ApiResponse.success(service.updatePrescription(id, (Long)a.getPrincipal(), r));
    }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication a, @PathVariable Long id) {
        service.deletePrescription(id, (Long)a.getPrincipal()); return ApiResponse.success();
    }
    @PostMapping("/pdf-preview")
    public ResponseEntity<byte[]> preview(Authentication a, @RequestParam(required = false) Long id, @RequestBody(required = false) PrescriptionCreateRequest r) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(service.previewPdf(id, (Long)a.getPrincipal(), r));
    }
    @PostMapping("/{id}/sign")
    public ResponseEntity<byte[]> sign(Authentication a, @PathVariable Long id, @RequestBody PrescriptionSignRequest r) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(service.signPrescription(id, (Long)a.getPrincipal(), r.getSignatureData()));
    }
}
