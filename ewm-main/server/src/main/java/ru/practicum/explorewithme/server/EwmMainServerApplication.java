package ru.practicum.explorewithme.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "ru.practicum.explorewithme.server",
        "ru.practicum.explorewithme.stats.client"  // Явно указываем пакет
})
public class EwmMainServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EwmMainServerApplication.class, args);
    }
}