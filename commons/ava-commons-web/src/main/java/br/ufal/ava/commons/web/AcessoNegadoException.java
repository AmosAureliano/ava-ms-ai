package br.ufal.ava.commons.web;

/** Usuário sem perfil ou sem posse do recurso: vira HTTP 403. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
