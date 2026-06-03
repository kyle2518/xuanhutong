package com.xuanhutong.clinic.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @TableName("herb_inventory")
public class HerbInventory {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId; private Long herbId; private BigDecimal stockGrams;
    private BigDecimal minStockAlert; private BigDecimal unitPrice; private LocalDate lastRestockDate;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
