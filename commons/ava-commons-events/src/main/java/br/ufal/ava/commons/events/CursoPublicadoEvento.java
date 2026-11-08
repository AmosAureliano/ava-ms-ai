package br.ufal.ava.commons.events;

import java.time.Instant;

/** Publicado pelo curso-service quando um curso passa a aceitar matrículas. */
public record CursoPublicadoEvento(Long cursoId, String titulo, Long professorId, Instant data) {
}
