package br.ufal.ava.commons.web;

/** Cota de uso esgotada: vira HTTP 429. */
public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String mensagem) {
        super(mensagem);
    }
}
