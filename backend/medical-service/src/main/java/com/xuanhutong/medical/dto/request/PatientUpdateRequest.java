package com.xuanhutong.medical.dto.request;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientUpdateRequest {
    private String name; private String phone; private Integer gender;
    private Integer age; private LocalDate birthDate; private String address;
    private String idCard; private String chiefComplaint;
}
