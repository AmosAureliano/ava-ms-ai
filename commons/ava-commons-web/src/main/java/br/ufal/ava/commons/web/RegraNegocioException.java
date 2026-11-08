package br.ufal.ava.commons.web;

/** Violação de regra de negócio: vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
