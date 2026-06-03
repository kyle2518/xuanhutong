package com.xuanhutong.clinic.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.clinic.dto.request.AppointmentCreateRequest;
import com.xuanhutong.clinic.entity.Appointment;

public interface AppointmentService {
    Appointment bookAppointment(AppointmentCreateRequest request);
    Page<Appointment> listAppointments(Long userId, String date, String status, int page, int size);
    Appointment getAppointment(Long id, Long userId);
    Appointment updateStatus(Long id, Long userId, String status);
    byte[] generateQRCode(Long userId);
}
