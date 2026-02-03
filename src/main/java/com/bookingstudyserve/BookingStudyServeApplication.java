package com.bookingstudyserve;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bookingstudyserve.mapper")
public class BookingStudyServeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingStudyServeApplication.class, args);
    }

}