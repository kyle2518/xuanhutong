package com.xuanhutong.medical.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MedicalRecordRequest {
    @NotNull(message = "就诊日期不能为空") private LocalDateTime visitDate;
    private String diagnosis; private String symptoms; private String treatmentMethod; private String progress; private String notes;
}
