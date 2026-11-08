package br.ufal.ava.commons.web;

import java.time.Instant;

/** Formato único de erro devolvido por qualquer microsserviço do AVA-MS. */
public record ApiErro(int status, String erro, String mensagem, Instant momento) {
}
