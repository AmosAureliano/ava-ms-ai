package br.ufal.ava.matricula;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MatriculaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatriculaApplication.class, args);
    }
}
