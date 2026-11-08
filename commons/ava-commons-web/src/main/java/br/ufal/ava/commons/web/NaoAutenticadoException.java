package br.ufal.ava.commons.web;

/** Requisição sem identidade válida: vira HTTP 401. */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
