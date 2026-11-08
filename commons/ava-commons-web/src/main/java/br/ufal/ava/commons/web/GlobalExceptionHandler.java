package br.ufal.ava.commons.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErro> naoEncontrado(RecursoNaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiErro> regra(RegraNegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ApiErro> negado(AcessoNegadoException e) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ApiErro> naoAutenticado(NaoAutenticadoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<ApiErro> limite(LimiteExcedidoException e) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErro> invalido(MethodArgumentNotValidException e) {
        String campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ApiErro> malFormado(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição mal formada");
    }

    /** Violação de UNIQUE em inserções concorrentes (ex.: duas matrículas simultâneas). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErro> conflito(DataIntegrityViolationException e) {
        return resposta(HttpStatus.CONFLICT, "O registro conflita com outro já existente");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErro> inesperado(Exception e) {
        if (e instanceof ErrorResponse erro) {
            HttpStatus s = HttpStatus.valueOf(erro.getStatusCode().value());
            String detalhe = erro.getBody().getDetail();
            return resposta(s, detalhe != null ? detalhe : s.getReasonPhrase());
        }
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no serviço");
    }

    private ResponseEntity<ApiErro> resposta(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(new ApiErro(s.value(), s.getReasonPhrase(), msg, Instant.now()));
    }
}
