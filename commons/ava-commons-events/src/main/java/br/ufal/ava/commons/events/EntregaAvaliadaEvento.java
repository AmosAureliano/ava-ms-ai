package br.ufal.ava.commons.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Publicado pelo avaliacao-service quando uma entrega recebe nota. */
public record EntregaAvaliadaEvento(Long entregaId, Long atividadeId, String atividadeTitulo, Long cursoId,
                                    Long alunoId, BigDecimal nota, Instant data) {
}
