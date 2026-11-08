package br.ufal.ava.notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** notificacao-service (porta 8085): avisos gerados a partir de eventos, sem acoplamento aos outros serviços. */
@SpringBootApplication
public class NotificacaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificacaoApplication.class, args);
    }
}
