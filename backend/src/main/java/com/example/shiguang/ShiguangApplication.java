package com.example.shiguang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.shiguang.mapper")
public class ShiguangApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiguangApplication.class, args);
    }
}
