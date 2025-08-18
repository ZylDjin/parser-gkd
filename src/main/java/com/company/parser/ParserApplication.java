package com.company.parser;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync       // на будущее: параллельный парсинг/планировщик
@EnableScheduling  // если позже захотим cron
public class ParserApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParserApplication.class, args);
    }
}
