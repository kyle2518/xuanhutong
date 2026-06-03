package com.xuanhutong.medical.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription_items")
public class PrescriptionItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long prescriptionId; private Long herbId; private String herbName;
    private BigDecimal dosageGrams; private String notes; private Integer sortOrder;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
