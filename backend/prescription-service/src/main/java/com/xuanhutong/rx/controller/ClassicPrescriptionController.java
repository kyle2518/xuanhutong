package com.xuanhutong.rx.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.ClassicPrescriptionRequest;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.rx.entity.ClassicPrescription;
import com.xuanhutong.rx.service.ClassicPrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/classic-prescriptions") @RequiredArgsConstructor
public class ClassicPrescriptionController {
    private final ClassicPrescriptionService service;
    @GetMapping
    public ApiResponse<Page<ClassicPrescription>> list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source, @RequestParam(defaultValue = "1") int p, @RequestParam(defaultValue = "20") int s) {
        return ApiResponse.success(service.listPrescriptions(keyword, source, p, s));
    }
    @GetMapping("/{id}") public ApiResponse<ClassicPrescription> get(@PathVariable Long id) { return ApiResponse.success(service.getPrescription(id)); }
    @PostMapping public ApiResponse<ClassicPrescription> create(@Valid @RequestBody ClassicPrescriptionRequest r) { return ApiResponse.success(service.createPrescription(r)); }
    @PutMapping("/{id}") public ApiResponse<ClassicPrescription> update(@PathVariable Long id, @Valid @RequestBody ClassicPrescriptionRequest r) { return ApiResponse.success(service.updatePrescription(id, r)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deletePrescription(id); return ApiResponse.success(); }
}
