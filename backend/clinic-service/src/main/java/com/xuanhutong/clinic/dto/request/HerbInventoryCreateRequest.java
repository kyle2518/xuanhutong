package com.xuanhutong.clinic.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class HerbInventoryCreateRequest {
    @NotNull(message = "药材不能为空") private Long herbId;
    private BigDecimal stockGrams;
    private BigDecimal minStockAlert;
    private BigDecimal unitPrice;
}
