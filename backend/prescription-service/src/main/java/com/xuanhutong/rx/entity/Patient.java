package com.xuanhutong.rx.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @TableName("patients")
public class Patient {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId; private String name; private String phone;
    private Integer gender; private Integer age; private LocalDate birthDate;
    private String address; private String idCard; private String chiefComplaint;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
