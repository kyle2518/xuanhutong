package com.xuanhutong.rx.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("herbs")
public class Herb {
    @TableId(type = IdType.AUTO) private Long id;
    private String pinyinName; private String chineseName; private String latinName;
    private String category; private String properties; private String meridianTropism;
    private String efficacy; private String indications; private String dosageRange; private String contraindications;
    @TableLogic private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
