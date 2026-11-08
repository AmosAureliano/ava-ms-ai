package br.ufal.ava.avaliacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AvaliacaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvaliacaoApplication.class, args);
    }
}
