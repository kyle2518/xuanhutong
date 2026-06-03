package com.xuanhutong.medical.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.medical.dto.request.PatientCreateRequest;
import com.xuanhutong.medical.dto.request.PatientUpdateRequest;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.medical.entity.Patient;
import com.xuanhutong.medical.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @GetMapping
    public ApiResponse<Page<Patient>> list(Authentication auth,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(patientService.listPatients((Long)auth.getPrincipal(), page, size, keyword));
    }

    @PostMapping
    public ApiResponse<Patient> create(Authentication auth, @Valid @RequestBody PatientCreateRequest req) {
        return ApiResponse.success(patientService.createPatient((Long)auth.getPrincipal(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<Patient> get(Authentication auth, @PathVariable Long id) {
        return ApiResponse.success(patientService.getPatient(id, (Long)auth.getPrincipal()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Patient> update(Authentication auth, @PathVariable Long id, @RequestBody PatientUpdateRequest req) {
        return ApiResponse.success(patientService.updatePatient(id, (Long)auth.getPrincipal(), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        patientService.deletePatient(id, (Long)auth.getPrincipal()); return ApiResponse.success();
    }
}
