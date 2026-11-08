package br.ufal.ava.commons.events;

import java.time.Instant;

/** Publicado pelo matricula-service quando o aluno conclui uma aula. */
public record AulaConcluidaEvento(Long matriculaId, Long alunoId, Long cursoId, Long aulaId, Instant data) {
}
