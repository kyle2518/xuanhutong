package com.xuanhutong.rx.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("classic_prescriptions")
public class ClassicPrescription {
    @TableId(type = IdType.AUTO) private Long id;
    private String name; private String source; private String category; private String composition;
    private String efficacy; private String indications; private String usageMethod; private String sourceText;
    private String notes; private Integer sortOrder;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
