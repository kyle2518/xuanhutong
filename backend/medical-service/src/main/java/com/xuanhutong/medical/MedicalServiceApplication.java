package com.xuanhutong.medical;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.xuanhutong"})
@MapperScan("com.xuanhutong.medical.repository")
public class MedicalServiceApplication {
    public static void main(String[] args) { SpringApplication.run(MedicalServiceApplication.class, args); }
}
