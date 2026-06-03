package com.xuanhutong.clinic.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentCreateRequest {
    private Long patientId;
    @NotNull private LocalDateTime appointmentTime;
    @NotBlank private String patientName;
    @NotBlank private String patientPhone;
    private Long userId; private String notes;
}
