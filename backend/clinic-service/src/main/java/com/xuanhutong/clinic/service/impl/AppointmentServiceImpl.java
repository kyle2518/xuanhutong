package com.xuanhutong.clinic.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.xuanhutong.clinic.dto.request.AppointmentCreateRequest;
import com.xuanhutong.clinic.entity.Appointment;
import com.xuanhutong.common.entity.User;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.clinic.repository.AppointmentRepository;
import com.xuanhutong.clinic.repository.UserRepository;
import com.xuanhutong.clinic.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;

@Service @RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository aptRepo; private final UserRepository userRepo;
    @Value("${app.base-url:http://localhost:5173}") private String baseUrl;

    public Appointment bookAppointment(AppointmentCreateRequest r) {
        var end = r.getAppointmentTime().plusMinutes(30);
        if (aptRepo.selectCount(new LambdaQueryWrapper<Appointment>().eq(Appointment::getUserId, r.getUserId()).eq(Appointment::getStatus, "PENDING").ge(Appointment::getAppointmentTime, r.getAppointmentTime()).lt(Appointment::getAppointmentTime, end)) > 0)
            throw new BusinessException(ErrorCode.APPOINTMENT_TIME_CONFLICT);
        Appointment a = new Appointment(); a.setUserId(r.getUserId()); a.setAppointmentTime(r.getAppointmentTime());
        a.setDurationMinutes(30); a.setStatus("PENDING"); a.setPatientName(r.getPatientName());
        a.setPatientPhone(r.getPatientPhone()); a.setNotes(r.getNotes()); a.setCreatedFrom("WECHAT");
        aptRepo.insert(a); return a;
    }

    public Page<Appointment> listAppointments(Long userId, String date, String status, int page, int size) {
        var w = new LambdaQueryWrapper<Appointment>().eq(Appointment::getUserId, userId);
        if (date != null) { LocalDate d = LocalDate.parse(date); w.between(Appointment::getAppointmentTime, d.atStartOfDay(), d.atTime(LocalTime.MAX)); }
        if (status != null) w.eq(Appointment::getStatus, status);
        w.orderByDesc(Appointment::getAppointmentTime); return aptRepo.selectPage(new Page<>(page, size), w);
    }

    public Appointment getAppointment(Long id, Long userId) {
        var a = aptRepo.selectOne(new LambdaQueryWrapper<Appointment>().eq(Appointment::getId, id).eq(Appointment::getUserId, userId));
        if (a == null) throw new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND); return a;
    }

    public Appointment updateStatus(Long id, Long userId, String status) {
        var a = getAppointment(id, userId); a.setStatus(status); aptRepo.updateById(a); return a;
    }

    public byte[] generateQRCode(Long userId) {
        try {
            User u = userRepo.selectById(userId);
            String url = baseUrl + "/wechat/book?doctorId=" + userId + (u != null && u.getClinicName() != null ? "&clinic=" + u.getClinicName() : "");
            BitMatrix m = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); MatrixToImageWriter.writeToStream(m, "PNG", baos); return baos.toByteArray();
        } catch (Exception e) { throw new BusinessException(5000, "二维码生成失败"); }
    }
}
