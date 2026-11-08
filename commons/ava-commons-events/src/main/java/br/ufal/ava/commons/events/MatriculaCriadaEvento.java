package br.ufal.ava.commons.events;

import java.time.Instant;

/** Publicado pelo matricula-service quando um aluno se matricula em um curso. */
public record MatriculaCriadaEvento(Long matriculaId, Long alunoId, Long cursoId, Instant data) {
}
