package com.xuanhutong.rx.dto.request;
import jakarta.validation.constraints.NotEmpty; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal; import java.util.List;

@Data
public class PrescriptionCreateRequest {
    @NotNull private Long patientId; private Long medicalRecordId; private String diagnosis;
    private String notes; private Integer totalDoses;
    @NotEmpty private List<PrescriptionItemDto> items;
    @Data public static class PrescriptionItemDto { private Long herbId; private String herbName; private BigDecimal dosageGrams; private String notes; }
}
