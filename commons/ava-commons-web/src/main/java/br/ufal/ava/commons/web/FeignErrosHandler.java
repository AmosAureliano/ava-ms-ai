package br.ufal.ava.commons.web;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Traduz falhas em chamadas OpenFeign para outro microsserviço. Tem prioridade sobre o
 * {@link GlobalExceptionHandler}, que transformaria essas exceções em 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FeignErrosHandler {

    private static final Logger log = LoggerFactory.getLogger(FeignErrosHandler.class);

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErro> feign(FeignException e) {
        return traduzir(e);
    }

    /** Com o circuit breaker ligado, a falha original chega embrulhada (ou é CallNotPermitted com o circuito aberto). */
    @ExceptionHandler(NoFallbackAvailableException.class)
    public ResponseEntity<ApiErro> semFallback(NoFallbackAvailableException e) {
        return e.getCause() instanceof FeignException f ? traduzir(f) : indisponivel(e);
    }

    private ResponseEntity<ApiErro> traduzir(FeignException e) {
        if (e.status() == HttpStatus.NOT_FOUND.value()) {
            return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado");
        }
        return indisponivel(e);
    }

    private ResponseEntity<ApiErro> indisponivel(Exception e) {
        log.warn("Serviço dependente indisponível: {}", e.toString());
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "Serviço dependente indisponível, tente novamente");
    }

    private ResponseEntity<ApiErro> resposta(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(new ApiErro(s.value(), s.getReasonPhrase(), msg, Instant.now()));
    }
}
