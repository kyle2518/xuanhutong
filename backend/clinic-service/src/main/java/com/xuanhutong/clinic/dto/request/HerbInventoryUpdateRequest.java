package com.xuanhutong.clinic.dto.request;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class HerbInventoryUpdateRequest {
    private BigDecimal stockGrams; private BigDecimal minStockAlert; private BigDecimal unitPrice;
    private List<BatchUpdateItem> items;
    @Data public static class BatchUpdateItem { private Long herbId; private BigDecimal stockGrams; }
}
