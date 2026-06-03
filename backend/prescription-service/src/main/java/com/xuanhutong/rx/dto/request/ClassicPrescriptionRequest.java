package com.xuanhutong.rx.dto.request;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data
public class ClassicPrescriptionRequest {
    @NotBlank private String name; @NotBlank private String source; private String category;
    @NotBlank private String composition; @NotBlank private String efficacy;
    private String indications; private String usageMethod; private String sourceText; private String notes;
}
