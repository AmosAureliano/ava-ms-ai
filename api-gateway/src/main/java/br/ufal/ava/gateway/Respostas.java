package br.ufal.ava.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Erros do gateway no mesmo formato ApiErro dos microsserviços. */
final class Respostas {

    private Respostas() {
    }

    static Mono<Void> erro(ServerWebExchange exchange, HttpStatus status, String mensagem) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(status);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String corpo = "{\"status\":%d,\"erro\":\"%s\",\"mensagem\":\"%s\",\"momento\":\"%s\"}"
                .formatted(status.value(), status.getReasonPhrase(), mensagem, Instant.now());
        return resp.writeWith(Mono.just(resp.bufferFactory().wrap(corpo.getBytes(StandardCharsets.UTF_8))));
    }
}
