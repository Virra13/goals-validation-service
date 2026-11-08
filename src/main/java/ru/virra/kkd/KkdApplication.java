package ru.virra.kkd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа REST-сервиса контроля качества данных по целям.
 * Запускает Spring Boot приложение, предоставляющее API прогонов,
 * сводной статистики и списка проблем ККД.
 */
@SpringBootApplication
public class KkdApplication {

    /**
     * Запускает REST-сервис ККД.
     *
     * @param args аргументы запуска Spring Boot приложения
     */
    public static void main(String[] args) {
        SpringApplication.run(KkdApplication.class, args);
    }

}
