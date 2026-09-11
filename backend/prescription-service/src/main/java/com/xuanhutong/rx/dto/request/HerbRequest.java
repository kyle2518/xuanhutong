package com.xuanhutong.rx.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HerbRequest {
    @NotBlank(message = "拼音名不能为空") private String pinyinName;
    @NotBlank(message = "中文名不能为空") private String chineseName;
    private String latinName;
    private String category;
    private String properties;
    private String meridianTropism;
    private String efficacy;
    private String indications;
    private String dosageRange;
    private String contraindications;
}
