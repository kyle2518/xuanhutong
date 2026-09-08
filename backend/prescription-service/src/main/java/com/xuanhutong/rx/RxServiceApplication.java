package com.xuanhutong.rx;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.xuanhutong"})
@MapperScan("com.xuanhutong.rx.repository")
public class RxServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RxServiceApplication.class, args);
    }
}
