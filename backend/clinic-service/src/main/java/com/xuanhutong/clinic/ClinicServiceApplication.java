package com.xuanhutong.clinic;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.xuanhutong"})
@MapperScan("com.xuanhutong.clinic.repository")
public class ClinicServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ClinicServiceApplication.class, args); }
}
