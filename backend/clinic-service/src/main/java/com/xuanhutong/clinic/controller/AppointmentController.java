package com.xuanhutong.clinic.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.clinic.dto.request.AppointmentCreateRequest;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.clinic.entity.Appointment;
import com.xuanhutong.clinic.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService service;
    @PostMapping("/book")
    public ApiResponse<Appointment> book(@Valid @RequestBody AppointmentCreateRequest r) { return ApiResponse.success(service.bookAppointment(r)); }
    @GetMapping
    public ApiResponse<Page<Appointment>> list(Authentication a, @RequestParam(required = false) String date,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") int p, @RequestParam(defaultValue = "10") int s) {
        return ApiResponse.success(service.listAppointments((Long)a.getPrincipal(), date, status, p, s));
    }
    @GetMapping("/{id}")
    public ApiResponse<Appointment> get(Authentication a, @PathVariable Long id) { return ApiResponse.success(service.getAppointment(id, (Long)a.getPrincipal())); }
    @PutMapping("/{id}/status")
    public ApiResponse<Appointment> updateStatus(Authentication a, @PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(service.updateStatus(id, (Long)a.getPrincipal(), status));
    }
    @GetMapping("/generate-qr")
    public ResponseEntity<byte[]> qr(Authentication a) { return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(service.generateQRCode((Long)a.getPrincipal())); }
}
