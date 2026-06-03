package com.xuanhutong.medical.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientCreateRequest {
    @NotBlank(message = "姓名不能为空") private String name;
    @NotBlank(message = "手机号不能为空") private String phone;
    private Integer gender; private Integer age; private LocalDate birthDate;
    private String address; private String idCard; private String chiefComplaint;
}
