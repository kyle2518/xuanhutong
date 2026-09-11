package com.xuanhutong.medical.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("medical_records")
public class MedicalRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long patientId; private Long userId; private LocalDateTime visitDate;
    private String diagnosis; private String symptoms; private String treatmentMethod;
    private String progress; private String notes; private String reportFileUrl;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
