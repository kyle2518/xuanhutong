package com.xuanhutong.medical.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prescriptions")
public class Prescription {
    @TableId(type = IdType.AUTO) private Long id;
    private Long patientId; private Long medicalRecordId; private Long userId;
    private String diagnosis; private String notes; private Integer totalDoses;
    private String pdfUrl; private Integer isSigned; private LocalDateTime signedAt;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
