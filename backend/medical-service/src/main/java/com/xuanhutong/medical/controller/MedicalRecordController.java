package com.xuanhutong.medical.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.medical.dto.request.MedicalRecordRequest;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.medical.entity.MedicalRecord;
import com.xuanhutong.medical.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MedicalRecordController {
    private final MedicalRecordService recordService;

    @GetMapping("/patients/{patientId}/records")
    public ApiResponse<Page<MedicalRecord>> list(Authentication auth, @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(recordService.listRecords(patientId, (Long)auth.getPrincipal(), page, size));
    }

    @PostMapping("/patients/{patientId}/records")
    public ApiResponse<MedicalRecord> create(Authentication auth, @PathVariable Long patientId,
            @Valid @RequestBody MedicalRecordRequest req) {
        return ApiResponse.success(recordService.createRecord(patientId, (Long)auth.getPrincipal(), req));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<MedicalRecord> get(Authentication auth, @PathVariable Long id) {
        return ApiResponse.success(recordService.getRecord(id, (Long)auth.getPrincipal()));
    }

    @PutMapping("/records/{id}")
    public ApiResponse<MedicalRecord> update(Authentication auth, @PathVariable Long id,
            @RequestBody MedicalRecordRequest req) {
        return ApiResponse.success(recordService.updateRecord(id, (Long)auth.getPrincipal(), req));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        recordService.deleteRecord(id, (Long)auth.getPrincipal()); return ApiResponse.success();
    }

    @PostMapping("/records/{id}/upload-report")
    public ApiResponse<String> uploadReport(Authentication auth, @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.success(recordService.uploadReport(id, (Long)auth.getPrincipal(),
                file.getOriginalFilename(), file.getBytes()));
    }

    @GetMapping("/patients/{patientId}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(Authentication auth, @PathVariable Long patientId) {
        byte[] pdf = recordService.exportPatientPdf(patientId, (Long)auth.getPrincipal());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=patient_data.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
