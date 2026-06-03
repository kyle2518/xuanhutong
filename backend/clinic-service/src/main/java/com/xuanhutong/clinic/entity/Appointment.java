package com.xuanhutong.clinic.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("appointments")
public class Appointment {
    @TableId(type = IdType.AUTO) private Long id;
    private Long patientId; private Long userId; private LocalDateTime appointmentTime;
    private Integer durationMinutes; private String status; private String patientName;
    private String patientPhone; private String notes; private String createdFrom;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
